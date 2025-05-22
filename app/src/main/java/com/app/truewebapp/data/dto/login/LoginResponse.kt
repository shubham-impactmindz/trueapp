package com.app.truewebapp.data.dto.login

data class LoginResponse(
    val status: Boolean,
    val message: String,
    val token: String,
    val token_type: String,
    val user: User,
    val expires_in: Long,
)

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val email_verified_at: String,
    val mobile: String,
    val company_name: String,
    val address1: String,
    val address2: String,
    val city: String,
    val country: String,
    val postcode: String,
    val rep_code: String,
    val admin_approval: String,
    val created_at: String,
    val updated_at: String,
)
