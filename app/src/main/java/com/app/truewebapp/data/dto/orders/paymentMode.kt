package com.app.accutecherp.data.dto.orders

data class paymentMode(
    val PaymentModeId: String,
    val PaymentMode: String
) {
    override fun toString(): String = PaymentMode
}
