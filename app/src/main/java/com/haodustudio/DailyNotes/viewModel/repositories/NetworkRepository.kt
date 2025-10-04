package com.haodustudio.DailyNotes.viewModel.repositories

import com.haodustudio.DailyNotes.BaseApplication
import com.haodustudio.DailyNotes.api.RetrofitInstance
import com.haodustudio.DailyNotes.api.DailyServer

object NetworkRepository {
    private val mengCalls = RetrofitInstance.getInstance(BaseApplication.BASE_SERVER_URI).create(DailyServer::class.java)

    fun getWeatherCall() = mengCalls.getWeather()
    fun getStickerListCall() = mengCalls.getStickerList()
    fun getTemplateListCall() = mengCalls.getTemplateList()
    fun getBackgroundListCall() = mengCalls.getBackgroundList()
    fun getGuideImgListCall() = mengCalls.getGuideImgList()
}