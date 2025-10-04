package com.haodustudio.DailyNotes.view.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.haodustudio.DailyNotes.BaseApplication
import com.haodustudio.DailyNotes.R
import com.haodustudio.DailyNotes.databinding.ActivityAboutSoftwareBinding
import com.haodustudio.DailyNotes.helper.makeToast
import com.haodustudio.DailyNotes.utils.ViewUtils
import com.haodustudio.DailyNotes.view.activities.base.BaseActivity
import com.haodustudio.DailyNotes.viewModel.viewModels.GlobalViewModel


class AboutSoftware : BaseActivity() {
    private val binding by lazy { ActivityAboutSoftwareBinding.inflate(layoutInflater) }
    private val appViewModel = ViewModelProvider(
        BaseApplication.instance,
        ViewModelProvider.AndroidViewModelFactory.getInstance(BaseApplication.instance)
    )[GlobalViewModel::class.java]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.openSourceText.setOnClickListener {
            makeToast("视频加载中\n改编自：@我家邻居全是猫")
            val mIntent = Intent(this, ViewImage::class.java)
            val videoUrl = BaseApplication.buildServerUrl("/static/about_eggshell_video.mp4")
            if (videoUrl == null) {
                makeToast("无网络连接")
            } else {
                mIntent.putExtra("isVideo", true)
                mIntent.putExtra("path", videoUrl)
                startActivity(mIntent)
            }
        }

        binding.artPeopleInfo.setOnClickListener {
            val mIntent = Intent(this, ViewImage::class.java)
            val imageUrl = BaseApplication.buildServerUrl("/static/about_thanks_img.png")
            if (imageUrl == null) {
                makeToast("无网络连接")
            } else {
                mIntent.putExtra("path", imageUrl)
                mIntent.putExtra("zoomEnabled", false)
                startActivity(mIntent)
            }
        }

        binding.haodusWindow.setOnClickListener {
            makeToast("诶哟你干嘛 (")
        }

        binding.mengxisWindow.setOnClickListener {
            makeToast("WebStorm，启动！")
        }

        val verName = packageManager.
            getPackageInfo(packageName, 0).versionName
        binding.versionInfo.text = "Version $verName"

        // tot = 2.5, ge ge ge (?
        ViewUtils.fadeIn(binding.aboutForeground, 1000) {
            binding.aboutForeground.postDelayed({
                ViewUtils.fadeOut(binding.aboutForeground, 500) {
                    startMainPage()
                }
            }, 1000)
        }
    }

    private fun startMainPage() {
        binding.videoView.visibility = View.GONE
        binding.aboutForeground.visibility = View.GONE
        binding.aboutRoot.visibility = View.VISIBLE
//        Glide.with(this).asGif().load(R.drawable.ic_cute_loading).into(binding.userPpImg)
        val policyUrl = BaseApplication.buildServerUrl("/static/about_user_privacy_policy.png")
        if (policyUrl == null) {
            makeToast("无网络连接")
        } else {
            Glide
                .with(this)
                .load(policyUrl)
                .placeholder(R.drawable.ic_cute_loading)
                .skipMemoryCache(true) // 不使用内存缓存
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(binding.userPpImg)
        }
    }
}