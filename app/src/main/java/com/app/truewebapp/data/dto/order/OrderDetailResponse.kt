package com.app.truewebapp.data.dto.order

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class OrderDetailResponse(
    val status: Boolean,
    val message: String,
    val cdnURL: String,
    val order: OrderDetail
)

@Parcelize
data class OrderDetail(
    val order_id: Int,
    val user: Users,
    val order_date: String,
    val units: Int,
    val payment_status: String,
    val invoice_pdf: String?,
    val tracking_number: String?,
    val track_your_order: String?,
    val fulfillment_status: String,
    val skus: Int,
    val delivery: Delivery,
    val coupon: Coupon?,
    val delivery_instructions: String?,
    val summary: Summary,
    val items: List<Items>
) : Parcelable


