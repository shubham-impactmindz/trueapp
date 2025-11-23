package com.app.truewebapp.data.dto.stripe

data class PaymentIntentRequest(
    val amount: Double, // Amount in main currency unit (e.g., pounds for GBP, dollars for USD)
    val currency: String = "gbp", // Currency code (default: gbp)
    val automatic_payment_methods: AutomaticPaymentMethods = AutomaticPaymentMethods(enabled = true),
    val metadata: Map<String, String>? = null, // Optional metadata (e.g., orderId, userId)
    val description: String? = null, // Optional description
    val customerId: String? = null, // Optional customer ID
    val receipt_email: String? = null // Optional receipt email
)

data class AutomaticPaymentMethods(
    val enabled: Boolean
)

