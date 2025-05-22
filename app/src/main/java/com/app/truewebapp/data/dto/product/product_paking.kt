package com.app.accutecherp.data.dto.product

data class product_paking(
    val ProductId: String,
    val ProductPackingId: String,
    val PackingId: String,
    val Packing: String,
    val MRP: String,
    val SaleRate: String?,
    val PurRate: String
) {
    override fun toString(): String = Packing
}
