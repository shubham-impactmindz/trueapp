package com.app.truewebapp.data.dto.delivery

data class DeliverySettingsResponse(
    val status: Boolean,
    val message: String,
    val min_order_free_delivery: String,
)
