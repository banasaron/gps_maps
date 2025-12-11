package com.app.gps_maps.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Url

interface ApiService {
    @GET
    fun getWeather(
        @Url url: String
    ): Call<WeatherResponse>
}