package com.app.truewebapp.data.dto.change_password

data class ChangePasswordRequest (
    val current_password: String,
    val new_password: String,
    val new_password_confirmation: String,
)