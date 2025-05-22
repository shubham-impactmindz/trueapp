package com.app.accutecherp.data.dto.clientstatus

import com.google.gson.annotations.SerializedName

data class clientStatusResponse(
    val status: String,
    val message: String,
    @SerializedName("data")
    val clientData: client_data

)

data class client_data(
    val client_status: List<client_status>
)

