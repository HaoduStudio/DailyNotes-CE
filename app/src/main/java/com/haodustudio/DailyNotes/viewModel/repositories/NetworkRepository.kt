package com.haodustudio.DailyNotes.viewModel.repositories

import com.haodustudio.DailyNotes.BaseApplication
import com.haodustudio.DailyNotes.api.DailyServer
import com.haodustudio.DailyNotes.api.RetrofitInstance
import com.haodustudio.DailyNotes.model.models.BackgroundList
import com.haodustudio.DailyNotes.model.models.GuideImgList
import com.haodustudio.DailyNotes.model.models.StickerList
import com.haodustudio.DailyNotes.model.models.TemplateList
import com.haodustudio.DailyNotes.model.models.Weather
import retrofit2.Call

object NetworkRepository {
    @Volatile
    private var service: DailyServer? = null

    private fun ensureService(): DailyServer? {
        val cached = service
        if (cached != null) return cached
        val retrofit = RetrofitInstance.getInstance(BaseApplication.BASE_SERVER_URI) ?: return null
        return retrofit.create(DailyServer::class.java).also { service = it }
    }

    fun getWeatherCall(): Call<Weather>? = ensureService()?.getWeather()
    fun getStickerListCall(): Call<StickerList>? = ensureService()?.getStickerList()
    fun getTemplateListCall(): Call<TemplateList>? = ensureService()?.getTemplateList()
    fun getBackgroundListCall(): Call<BackgroundList>? = ensureService()?.getBackgroundList()
    fun getGuideImgListCall(): Call<GuideImgList>? = ensureService()?.getGuideImgList()
}