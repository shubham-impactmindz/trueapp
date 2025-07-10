package com.app.truewebapp.data.dto.register

data class VerifyRepResponse(
    val status: Boolean,
    val data: Data,
)

data class Data(
    val rep_id: Int,
    val user_id: Int,
    val rep_code: String,
    val commission_percent: String,
    val created_at: String,
    val updated_at: String,
)


