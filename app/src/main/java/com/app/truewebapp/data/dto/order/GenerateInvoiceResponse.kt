package com.app.truewebapp.data.dto.order

data class GenerateInvoiceResponse(
    val status: Boolean,
    val message: String,
    val cdnURL: String?,
    val invoice_pdf: String?
)


