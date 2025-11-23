package com.app.truewebapp.data.dto.order

data class OrderRequest(
    // Basic order information
    val user_company_address_id: String, // ID of the selected delivery address
    val delivery_method_id: String, // ID of the selected delivery method
    val coupon_discount: Double?, // Amount discounted from coupon (nullable)
    val wallet_discount: Double?, // Amount discounted from wallet (nullable)
    val delivery_instructions: String?, // Optional delivery notes/instructions
    val coupon_id: String?, // Optional coupon ID (nullable)
    val pay_by_bank: Boolean, // Payment method flag (true for bank transfer, false for card)
    val payment_status: String?, // Payment status (nullable) paid for stripe payment, pending for bank transfer
    
    // Payment provider information
    val payment_provider: String?, // Payment provider (nullable) bank_transfer for bank, stripe for stripe
    val payment_reference: String?, // Payment reference (nullable)
    
    // Stripe payment information
    val payment_intent_id: String, // Stripe Payment Intent ID (required, empty string for bank payments)
    val payment_method_id: String?, // Stripe Payment Method ID (nullable)
    val customer_id: String?, // Stripe Customer ID (nullable)
    val currency: String, // Payment currency (required)
    val amount: Int, // Payment amount in cents (required)
    val status: String, // Payment status (required) pending for bank transfer, succeeded for stripe
    val receipt_email: String?, // Receipt email (nullable)
    val description: String?, // Payment description (nullable)
    val metadata: String?, // Additional metadata (nullable)
    val raw_payload: String? // Raw payment payload (nullable)
)
