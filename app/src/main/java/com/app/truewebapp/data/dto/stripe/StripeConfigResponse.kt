package com.app.truewebapp.data.dto.stripe

data class StripeConfigResponse(
    val status: Boolean,
    val message: String,
    val data: StripeConfig?
)

data class StripeConfig(
    val provider: String?,
    val test_mode: Boolean?,
    val publishable_key: String?,
    val secret_key: String?,
    val client_secret: String? // PaymentIntent client secret (optional, can be provided by backend)
)

