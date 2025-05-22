package com.app.accutecherp.data.dto.city

import com.google.gson.annotations.SerializedName

data class cityResponse(

    val status: String,
    val message: String,
    @SerializedName("data")
    val data1: data1
)

data class data1(
    val city_data: List<city_data>
)