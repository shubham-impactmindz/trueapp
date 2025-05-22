package com.app.accutecherp.data.dto.product

import com.google.gson.annotations.SerializedName

data class productGrpResponse(
    val status: String,
    val message: String,
    @SerializedName("data")
    val data1: data1
)

data class data1(
    val product_group: ArrayList<product_group>
)