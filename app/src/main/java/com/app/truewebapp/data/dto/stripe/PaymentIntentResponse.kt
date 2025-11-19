package com.app.truewebapp.data.dto.stripe

data class PaymentIntentResponse(
    val status: Boolean,
    val message: String,
    val data: PaymentIntentData?
)

data class PaymentIntentData(
    val client_secret: String? // PaymentIntent client secret
)

