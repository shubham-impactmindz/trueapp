package com.app.truewebapp.data.dto.company_address

data class CompanyAddressRequest(
    val user_company_name: String,
    val company_address1: String,
    val company_address2: String,
    val company_city: String,
    val company_country: String,
    val company_postcode: String,
    val user_company_address_id: String? = null // Optional parameter
)