package com.app.truewebapp.data.dto.order

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class OrdersResponse(
    val status: Boolean,
    val message: String,
    val cdnURL: String,
    val orders: List<Orders>,
    val meta: Meta
)

@Parcelize
data class Orders(
    val order_id: Int,
    val user: Users,
    val order_date: String,
    val units: Int,
    val payment_status: String,
    val fulfillment_status: String,
    val skus: Int,
    val delivery: Delivery,
    val coupon: Coupon?,
    val delivery_instructions: String?,
    val summary: Summary,
    val items: List<Items>
) : Parcelable

@Parcelize
data class Users(
    val id: Int,
    val name: String,
    val email: String
) : Parcelable

@Parcelize
data class Delivery(
    val method_id: Int,
    val method: String,
    val address_id: Int,
    val address: String
) : Parcelable

@Parcelize
data class Coupon(
    val coupon_id: Int,
    val code: String,
    val discount_type: String,
    val discount_value: String,
    val expires_at: String?,
    val usage_limit: Int,
    val per_user_limit: Int,
    val min_cart_value: String
) : Parcelable

@Parcelize
data class Summary(
    val subtotal: Double,
    val wallet_discount: Double,
    val coupon_discount: Double,
    val delivery_cost: Double,
    val vat: Double,
    val payment_total: Double
) : Parcelable

@Parcelize
data class Items(
    val order_item_id: Int,
    val mvariant_id: Int,
    val quantity: Int,
    val unit_price: Double,
    val variant: Variant,
    val product: Product
) : Parcelable

@Parcelize
data class Variant(
    val sku: String,
    val image: String?,
    val price: Double,
    val compare_price: Double,
    val cost_price: Double,
    val options: List<String>,
    val option_value: Map<String, String>
) : Parcelable

@Parcelize
data class Product(
    val mproduct_id: Int,
    val mproduct_title: String,
    val mproduct_slug: String,
    val mproduct_image: String
) : Parcelable

data class Meta(
    val current_page: Int,
    val per_page: Int,
    val total: Int,
    val last_page: Int,
    val from: Int,
    val to: Int,
)
