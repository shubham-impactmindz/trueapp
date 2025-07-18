package com.app.truewebapp.data.dto.bank

data class BankDetailResponse(
    val status: Boolean,
    val message: String,
    val bank_detail: BankDetail
)

data class BankDetail(
    val bank_detail_id: Int,
    val account_holder_name: String,
    val sort_code: String,
    val account_number: String,
    val is_active: Boolean,
    val created_at: String,
    val updated_at: String,
)
