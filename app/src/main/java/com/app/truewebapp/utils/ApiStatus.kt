package com.app.truewebapp.utils

import com.google.gson.annotations.SerializedName

data class ApiStatus(@SerializedName("result") val status: Boolean, @SerializedName("message") val message: String)