package com.app.accutecherp.data.dto.orders

import com.google.gson.annotations.SerializedName

data class salesProductResponse(
    val status: String,
    val message: String,
    @SerializedName("data")
    val saleProductData: saleProductData
)

data class saleProductData(
    val product_list: List<productList>
)
