package com.app.truewebapp.data.dto.stripe

data class PaymentIntentResponse(
    val status: Boolean? = null, // Payment status (true = success, false = failed)
    val message: String? = null, // Status message
    val client_secret: String? = null, // PaymentIntent client secret
    val payment_intent_id: String? = null // Payment Intent ID
)

