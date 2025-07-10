package com.app.truewebapp.data.dto.company_address

data class CompanyAddressResponse (
    val status: Boolean,
    val message: String,
    val company_addresses: List<CompanyAddresses>,
    val delivery_methods: List<DeliveryMethods>,
)

data class CompanyAddresses(
    val user_company_address_id: Int,
    val user_id: Int,
    val user_company_name: String,
    val company_address1: String,
    val company_address2: String?,
    val company_city: String,
    val company_country: String,
    val company_postcode: String,
    val created_at: String,
    val updated_at: String,
)

data class DeliveryMethods(
    val delivery_method_id: Int,
    val delivery_method_name: String,
    val delivery_method_amount: String,
    val method_status: String,
    val created_at: String,
    val updated_at: String,
)
