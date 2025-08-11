package com.app.truewebapp.data.dto.profile

data class UserProfileResponse(
    val status: Boolean,
    val message: String,
    val user_detail: UserDetail?,
    val rep_details: RepDetails? // or RepDetails? if you later define it
)

data class UserDetail(
    val id: Int,
    val rep_id: Int?,
    val name: String,
    val email: String,
    val email_verified_at: String?,
    val mobile: String,
    val company_name: String,
    val address1: String,
    val address2: String,
    val city: String,
    val country: String,
    val postcode: String,
    val admin_approval: String,
    val created_at: String,
    val updated_at: String
)

data class RepDetails(
    val rep_id: Int,
    val name: String,
    val email: String,
    val mobile: String,
    val rep_code: String,
    val commission_percent: String,
)