package com.app.accutecherp.data.dto.account_type

data class account_type(
    val AccountTypeId: String,
    val AccountType: String,
    val IsActive: String
){
    override fun toString(): String =AccountType
}
