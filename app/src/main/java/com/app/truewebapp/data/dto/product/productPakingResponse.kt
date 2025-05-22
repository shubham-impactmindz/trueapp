package com.app.accutecherp.data.dto.product

import com.google.gson.annotations.SerializedName

data class productPakingResponse(
    val status: String,
    val message: String,
    @SerializedName("data")
    val pakingData: pakingData
)

data class pakingData(
    val product_packing_list: List<product_paking>
)
