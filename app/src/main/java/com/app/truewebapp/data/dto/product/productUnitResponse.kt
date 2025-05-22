package com.app.accutecherp.data.dto.product

import com.google.gson.annotations.SerializedName

data class productUnitResponse(
    val status: String,
    val message: String,
    @SerializedName("data")
    val unitData: unitData,
)

data class unitData(

    val product_unit_list: List<product_unit>
)
