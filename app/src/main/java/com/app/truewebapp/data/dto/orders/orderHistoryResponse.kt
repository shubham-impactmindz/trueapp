package com.app.accutecherp.data.dto.orders

import com.google.gson.annotations.SerializedName

data class orderHistoryResponse(
    val status: String,
    val message: String,
    @SerializedName("data")
    val orderData: OrderData
)

data class OrderData(

    @SerializedName("so_data")
    val orderHistoryData: List<OrderHistoryData>
)
