package com.haodustudio.DailyNotes.api

import com.haodustudio.DailyNotes.model.models.Weather
import retrofit2.Call
import retrofit2.http.GET

interface WeatherServer {

    @GET("/")
    fun getWeather(): Call<Weather>
}