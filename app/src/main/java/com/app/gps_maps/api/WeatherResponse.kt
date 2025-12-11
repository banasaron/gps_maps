package com.app.gps_maps.api

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @Expose @SerializedName("hourly")
    val hourly: HourlyData? = null
)