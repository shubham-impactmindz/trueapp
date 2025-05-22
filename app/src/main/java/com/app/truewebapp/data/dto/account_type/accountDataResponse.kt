package com.app.accutecherp.data.dto.account_type

import com.google.gson.annotations.SerializedName

data class accountDataResponse (
    val status:String,
    val message:String,
    @SerializedName("data")
    val data: data1
)
data class data1(
    val employee_party_data: List<employee_party_data>
)