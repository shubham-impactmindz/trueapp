package com.app.accutecherp.data.dto.account_type

data class accountTypeResponse(
    val status: String,
    val message: String,
    val data: data
)

data class data(
    val account_type: List<account_type>
)


