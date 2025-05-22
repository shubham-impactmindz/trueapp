package com.app.accutecherp.data.dto.orders

import com.google.gson.annotations.SerializedName

data class paymentModeResponse(
    val status: String,
    val message: String,
    @SerializedName("data")
    val paymentData: paymentData

)

data class paymentData(
    @SerializedName("payment_mode")
    val paymentMode: List<paymentMode>
)
