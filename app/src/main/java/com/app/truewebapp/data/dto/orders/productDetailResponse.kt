package com.app.accutecherp.data.dto.orders

import com.google.gson.annotations.SerializedName

data class productDetailResponse(
    val status:String,
    @SerializedName("data")
    val productData: product_data
)
data class product_data(
    val product_detail:product_detail
)