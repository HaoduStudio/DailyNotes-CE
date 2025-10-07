package com.haodustudio.DailyNotes.view.activities

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.haodustudio.DailyNotes.BaseApplication
import com.haodustudio.DailyNotes.R
import com.haodustudio.DailyNotes.databinding.ActivityBackgroundChooserBinding
import com.haodustudio.DailyNotes.helper.PrivacySettingsManager
import com.haodustudio.DailyNotes.helper.makeToast
import com.haodustudio.DailyNotes.model.models.BackgroundList
import com.haodustudio.DailyNotes.utils.FileUtils
import com.haodustudio.DailyNotes.view.activities.base.BaseActivity
import com.haodustudio.DailyNotes.viewModel.repositories.NetworkRepository
import com.haodustudio.DailyNotes.viewModel.viewModels.GlobalViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BackgroundChooser : BaseActivity(noShot = true) {

    private val binding by lazy { ActivityBackgroundChooserBinding.inflate(layoutInflater) }
    private val appViewModel by lazy {
        ViewModelProvider(
            BaseApplication.instance,
            ViewModelProvider.AndroidViewModelFactory.getInstance(BaseApplication.instance)
        )[GlobalViewModel::class.java]
    }
    private val inflater by lazy { LayoutInflater.from(this) }
    private val remoteItems = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        try {
            renderLocalBackgrounds()
            loadRemoteBackgroundsIfNeeded()
        } catch (error: Exception) {
            error.printStackTrace()
            makeToast("Load background failure")
            finish()
        }
    }

    private fun renderLocalBackgrounds() {
        val locationEnabled = PrivacySettingsManager.isLocationEnabled()
        val locals = mutableListOf<LocalBackground>()

        if (locationEnabled) {
            val weatherAssets = assets.list(
                FileUtils.removePathSlashAtLast(BaseApplication.ASSETS_WEATHER_BACKGROUND_PATH)
            )?.sorted()
            val weatherItem = weatherAssets?.firstOrNull()
            if (weatherItem != null) {
                locals.add(
                    LocalBackground(
                        assetPath = BaseApplication.ASSETS_WEATHER_BACKGROUND_PATH + weatherItem,
                        isWeather = true,
                        displayName = getString(R.string.privacy_weather_option_label)
                    )
                )
            }
        } else {
            makeToast(getString(R.string.privacy_location_disabled_hint))
        }

        val defaultAssets = assets.list(
            FileUtils.removePathSlashAtLast(BaseApplication.ASSETS_DEFAULT_BACKGROUND_PATH)
        )?.filter { it.isNotEmpty() && !it.startsWith(".") }
            ?.sorted()
            ?.distinct()
        
        defaultAssets?.forEachIndexed { index, assetName ->
            locals.add(
                LocalBackground(
                    assetPath = BaseApplication.ASSETS_DEFAULT_BACKGROUND_PATH + assetName,
                    isWeather = false,
                    displayName = getString(R.string.privacy_background_option_label, index + 1)
                )
            )
        }

        // 使用 distinctBy 确保没有重复的路径
        locals.distinctBy { it.assetPath }.forEach { addAssetBackground(it) }
    }

    private fun addAssetBackground(item: LocalBackground) {
        val view = inflater.inflate(R.layout.app_background_item, binding.listFather, false)
        val imageView = view.findViewById<ImageView>(R.id.background_image)
        val nameView = view.findViewById<TextView>(R.id.background_name)

        Glide.with(this)
            .load("file:///android_asset/${item.assetPath}")
            .into(imageView)

        nameView.text = item.displayName

        view.setOnClickListener {
            setBackgroundAndFinish(item.isWeather, "file:///android_asset/${item.assetPath}")
        }

        binding.listFather.addView(view)
    }

    private fun setBackgroundAndFinish(isWeather: Boolean, path: String) {
        try {
            appViewModel.setAppBackground(isWeather, path)
        } catch (error: Exception) {
            error.printStackTrace()
            makeToast("Change failure")
        } finally {
            finish()
        }
    }

    private fun loadRemoteBackgroundsIfNeeded() {
        if (!PrivacySettingsManager.isCloudResourcesEnabled()) {
            makeToast(getString(R.string.privacy_cloud_disabled_hint))
            return
        }

        makeToast("加载网络背景")
        val call = NetworkRepository.getBackgroundListCall()
        if (call == null) {
            makeToast("无网络连接")
            return
        }

        call.enqueue(object : Callback<BackgroundList> {
            override fun onResponse(call: Call<BackgroundList>, response: Response<BackgroundList>) {
                try {
                    val list = response.body()?.getList()?.mapNotNull { BaseApplication.buildServerUrl(it) }
                    if (list.isNullOrEmpty()) {
                        makeToast("无网络连接")
                        return
                    }
                    FileUtils.makeRootDirectory(BaseApplication.BACKGROUND_DOWNLOAD_FROM_URI_PATH)
                    makeToast("获取成功")
                    list.forEach { downloadRemoteBackground(it) }
                } catch (error: Exception) {
                    onFailure(call, error)
                }
            }

            override fun onFailure(call: Call<BackgroundList>, t: Throwable) {
                t.printStackTrace()
                makeToast("无网络连接")
            }
        })
    }

    private fun downloadRemoteBackground(uri: String) {
        Glide.with(this).asBitmap().load(uri)
            .into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    try {
                        val fileName = extractFileName(uri)
                        if (!remoteItems.add(fileName)) {
                            return
                        }
                        val cachePath = "${cacheDir.absolutePath}/$fileName"
                        FileUtils.saveBitmap(cachePath, resource)
                        addRemoteBackgroundView(cachePath, fileName)
                    } catch (error: Exception) {
                        error.printStackTrace()
                        makeToast("Save bitmap failure")
                    }
                }
            })
    }

    private fun addRemoteBackgroundView(cachePath: String, fileName: String) {
        val view = inflater.inflate(R.layout.app_background_item, binding.listFather, false)
        val imageView = view.findViewById<ImageView>(R.id.background_image)
        val nameView = view.findViewById<TextView>(R.id.background_name)

        Glide.with(this).load(cachePath).into(imageView)
        nameView.text = getString(R.string.privacy_background_online_label)

        view.setOnClickListener {
            try {
                val targetDir = BaseApplication.BACKGROUND_DOWNLOAD_FROM_URI_PATH
                if (!FileUtils.exists(targetDir)) {
                    FileUtils.makeRootDirectory(targetDir)
                }
                val targetPath = targetDir + fileName
                FileUtils.copyFile(cachePath, targetPath)
                appViewModel.setAppBackground(false, targetPath)
            } catch (error: Exception) {
                error.printStackTrace()
                makeToast("Setting error")
            } finally {
                finish()
            }
        }

        binding.listFather.addView(view)
    }

    private fun extractFileName(uri: String): String {
        val base = uri.substringAfterLast('/')
        val clean = base.substringBefore('?').substringBefore('#')
        return if (clean.isNotEmpty()) clean else "background_${System.currentTimeMillis()}.png"
    }

    private data class LocalBackground(
        val assetPath: String,
        val isWeather: Boolean,
        val displayName: String
    )
}