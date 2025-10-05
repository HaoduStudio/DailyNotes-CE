package com.haodustudio.DailyNotes.view.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.haodustudio.DailyNotes.BaseApplication
import com.haodustudio.DailyNotes.R
import com.haodustudio.DailyNotes.databinding.ActivityCongratulationsBinding
import com.haodustudio.DailyNotes.helper.makeToast
import com.haodustudio.DailyNotes.utils.NetworkUtils
import com.haodustudio.DailyNotes.utils.PlayMediaUtils
import com.haodustudio.DailyNotes.view.activities.base.BaseActivity

class CongratulationsActivity : BaseActivity() {
    private val binding by lazy { ActivityCongratulationsBinding.inflate(layoutInflater) }
    private lateinit var ringReceiver: RINGReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        Glide.with(this).load(R.drawable.ic_cute_loading).into(binding.progressBar)
        binding.loadingContainer.isVisible = true

        NetworkUtils.isNetworkOnline(object : Handler() {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                if (msg.arg1 != 0) {
                    makeToast("无网络连接")
                    finish()
                }else {
                    loadContent()
                }
            }
        })

        val intentFilter = IntentFilter()
        intentFilter.addAction("com.xtc.alarmclock.action.ALARM_VIEW_SHOWING")
        intentFilter.addAction("com.xtc.videochat.start")
        intentFilter.addAction("android.intent.action.PHONE_STATE")
        ringReceiver = RINGReceiver()
        registerReceiver(ringReceiver,intentFilter)
    }

    private fun loadContent() {
        val imgUrl = BaseApplication.buildServerUrl("/static/congratulations_img.webp")
        val audioUrl = BaseApplication.buildServerUrl("/static/congratulations_audio.mp3")
        if (imgUrl == null || audioUrl == null) {
            makeToast("无网络连接")
            finish()
            return
        }
        Glide.with(this)
            .load(imgUrl)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.loadingContainer.isVisible = false
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: com.bumptech.glide.load.DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.loadingContainer.isVisible = false
                    return false
                }
            })
            .into(binding.conImg)

        PlayMediaUtils.play(audioUrl) { error ->
            Log.e("CongratsActivity", "Failed to play congrats audio from $audioUrl", error)
            binding.loadingContainer.isVisible = false
            binding.root.post {
                if (!isFinishing) {
                    makeToast("音频暂时不可用")
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(ringReceiver)
        PlayMediaUtils.stop()
    }

    inner class RINGReceiver : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("PlayRecord","Play interrupted.")
            finish()
        }
    }
}