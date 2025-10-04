package com.haodustudio.DailyNotes.view.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.MotionEvent
import com.bumptech.glide.Glide
import com.haodustudio.DailyNotes.BaseApplication
import com.haodustudio.DailyNotes.R
import com.haodustudio.DailyNotes.databinding.ActivityGuideBinding
import com.haodustudio.DailyNotes.helper.makeToast
import com.haodustudio.DailyNotes.model.models.GuideImgList
import com.haodustudio.DailyNotes.utils.NetworkUtils
import com.haodustudio.DailyNotes.view.activities.base.BaseActivity
import com.haodustudio.DailyNotes.view.adapters.BannerAdapter
import com.haodustudio.DailyNotes.viewModel.repositories.NetworkRepository
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GuideActivity : BaseActivity() {
    private val binding by lazy { ActivityGuideBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        Glide.with(this).load(R.drawable.ic_cute_loading).into(binding.progressBar)

        NetworkUtils.isNetworkOnline(object : Handler() {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                if (msg.arg1 != 0) {
                    makeToast("无网络连接")
                    finish()
                }else {
                    loadBanner()
                }
            }
        })
    }

    private fun loadBanner() {
        val call = NetworkRepository.getGuideImgListCall()
        if (call == null) {
            makeToast("无网络连接")
            finish()
            return
        }
        call.enqueue(object : Callback<GuideImgList> {
            override fun onResponse(call: Call<GuideImgList>, response: Response<GuideImgList>) {
                try {
                    val list = response.body()!!.getList().mapNotNull { BaseApplication.buildServerUrl(it) }
                    if (list.isEmpty()) {
                        makeToast("无网络连接")
                        finish()
                        return
                    }
                    val adapter = BannerAdapter(this@GuideActivity, list)
                    binding.banner.adapter = adapter
                }catch (e: Exception) {
                    onFailure(call, e)
                }
            }

            override fun onFailure(call: Call<GuideImgList>, t: Throwable) {
                t.printStackTrace()
                makeToast("无网络连接")
                finish()
            }

        })
    }
}