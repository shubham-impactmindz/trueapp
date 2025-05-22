package com.app.accutecherp.data.dto.state

import com.google.gson.annotations.SerializedName

data class stateResponse(
    val status: String,
    val message: String,
    @SerializedName("data")
    val data1: data1
)

data class data1(
    val state_data: List<state_data>
)