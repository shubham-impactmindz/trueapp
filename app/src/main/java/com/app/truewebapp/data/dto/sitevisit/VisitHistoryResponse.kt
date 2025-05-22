package com.app.accutecherp.data.dto.sitevisit

import com.google.gson.annotations.SerializedName

data class VisitHistoryResponse(
    val status: String,
    //val message:String,
    @SerializedName("message")
    val siteVisitData: List<SiteVisitData>
)