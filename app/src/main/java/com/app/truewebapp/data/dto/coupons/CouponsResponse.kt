package com.app.truewebapp.data.dto.coupons

data class CouponsResponse(
    val status: Boolean,
    val message: String,
    val data: List<Data>
)

data class Data(
    val coupon_id: Int,
    val code: String,
    val discount_type: String,
    val expires_at: String,
    val usage_limit: Int,
    val per_user_limit: Int,
    val discount_value: String,
    val min_cart_value: String,
    val is_active: Boolean,
    val created_at: String,
    val updated_at: String,
)
