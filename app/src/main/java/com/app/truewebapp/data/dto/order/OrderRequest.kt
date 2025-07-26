package com.app.truewebapp.data.dto.order

data class OrderRequest(
    val wallet_discount: String,
    val coupon_discount: String,
    val user_company_address_id: String,
    val delivery_method_id: String,
    val delivery_instructions: String,
    val couponId: String,
)
