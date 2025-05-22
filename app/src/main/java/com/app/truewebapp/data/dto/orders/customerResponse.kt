package com.app.accutecherp.data.dto.orders

import com.google.gson.annotations.SerializedName

data class customerResponse(
    val status: String,
    val message: String,
    @SerializedName("data")
    val customerData: customerData
)

data class customerData(
    @SerializedName("employee_customer_list")
    val customerList: MutableList<customerList>
)
