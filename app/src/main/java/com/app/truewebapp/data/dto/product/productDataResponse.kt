package com.app.accutecherp.data.dto.product

import com.google.gson.annotations.SerializedName

data class productDataResponse(
    val status: String,
    val message: String,
    @SerializedName("data")
    val productData: productData
)

data class productData(
    val product_data: ArrayList<product_data>
)
