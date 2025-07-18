package com.app.truewebapp.data.dto.order

data class OrderPlaceResponse(
    val status: Boolean,
    val message: String,
    val order: Order
)

data class Order(
    val user_id: Int,
    val total_amount: Double,
    val wallet_discount: Double,
    val coupon_discount: Double,
    val status: String,
    val fulfillment_status: String,
    val user_company_address_id: Int,
    val delivery_method_id: Int,
    val vat: Double,
    val product_total_amount: Double,
    val delivery_instructions: String?,
    val updated_at: String,
    val created_at: String,
    val order_id: Int,
    val items: List<OrderItem>,
    val user: User
)

data class OrderItem(
    val order_item_id: Int,
    val order_id: Int,
    val mvariant_id: Int,
    val quantity: Int,
    val unit_price: String,
    val created_at: String,
    val updated_at: String
)

data class User(
    val id: Int,
    val name: String,
    val email: String
)
