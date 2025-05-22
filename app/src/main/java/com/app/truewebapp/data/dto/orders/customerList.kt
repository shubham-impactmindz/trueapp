package com.app.accutecherp.data.dto.orders



data class customerList(
    val AccountId: String,
    val Account: String,
    val AccountTypeId: String,
    val PaymentModeId: String,
    val GSTIN: String,
    val CrDays: String,

    val address: address?=null
) {
    override fun toString(): String = Account
}

data class address(
    val PlotNo: String,
    val Sector_Street: String,
    val Locality: String,
    val Landmark: String,
    val Zip: String,
    val City: String,
    val State: String,
    val StateId:String,
    val District: String,
    val Country: String
)
