package com.app.accutecherp.data.dto.product

data class product_unit(
    val Packing:String,
    val PackingId:String,
    val DecimalPoints: String,
    val UnitAbbr: String,
    val Unit: String,
    val UnitId: String,
    val CoversionUnitId: String,
    val ConversionFactor: String
) {
    override fun toString(): String = Unit
}
