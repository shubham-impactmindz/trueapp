package com.app.truewebapp.data.dto.profile

data class ProfileRequest(
    val name: String,
    val email: String,
    val mobile: String,
    val company_name: String,
    val address1: String,
    val address2: String,
    val city: String,
    val country: String,
    val postcode: String,
)
