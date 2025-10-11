package com.app.truewebapp.data.dto.register

data class RegisterRequest(
    val first_name: String,
    val last_name: String,
    val email: String,
    val password: String,
    val mobile: String,
    val rep_code: String,
    val company_name: String,
    val address1: String,
    val address2: String,
    val city: String,
    val country: String,
    val postcode: String,
    val referral_code: String
)
