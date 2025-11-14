package com.app.truewebapp.data.dto.stripe

data class StripeConfigResponse(
    val status: Boolean,
    val message: String,
    val stripe_config: StripeConfig?
)

data class StripeConfig(
    val publishable_key: String?,
    val client_secret: String?,
    val payment_intent_id: String?
)

