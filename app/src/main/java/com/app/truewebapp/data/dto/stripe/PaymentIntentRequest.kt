package com.app.truewebapp.data.dto.stripe

data class PaymentIntentRequest(
    val amount: Double // Amount in the smallest currency unit (e.g., pence for GBP, cents for USD)
)

