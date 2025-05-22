package com.app.accutecherp.data.dto.orders

data class productList(
    val ProductId: String,
    val ProductName: String
) {
    override fun toString(): String = ProductName
}
