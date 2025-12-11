package com.app.gps_maps.api

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

//nominatim manual
//open meteo
data class HourlyData (
    @Expose @SerializedName("time")
    val time: List<String>? = null,

    @Expose @SerializedName("temperature_2m")
    val temperature: List<Float>? = null
): Serializable