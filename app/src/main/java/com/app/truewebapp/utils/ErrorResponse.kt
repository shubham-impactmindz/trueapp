package com.app.truewebapp.utils

import com.google.gson.annotations.SerializedName


data class ErrorResponse(

        @field:SerializedName("message")
        val message: String? = null,

        @field:SerializedName("result")
        val errors: Boolean? = false
)