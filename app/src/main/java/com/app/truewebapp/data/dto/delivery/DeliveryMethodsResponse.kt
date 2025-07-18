package com.app.truewebapp.data.dto.delivery

data class DeliveryMethodsResponse(
    val status: Boolean,
    val message: String,
    val delivery_methods: List<DeliveryMethod>
)

data class DeliveryMethod(
    val delivery_method_id: Int,
    val delivery_method_name: String,
    val delivery_method_amount: String,
    val method_status: String,
    val created_at: String,
    val updated_at: String,
)
