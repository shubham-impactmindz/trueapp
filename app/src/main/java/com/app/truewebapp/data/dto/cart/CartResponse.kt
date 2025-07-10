package com.app.truewebapp.data.dto.cart

import com.app.truewebapp.data.dto.browse.Product

data class CartResponse(
    val status: Boolean,
    val message: String,
    val cdnURL: String,
    val cart_item: List<CartItem>
)

data class CartItem(
    val cart_item_id: Int,
    val mvariant_id: Int,
    val quantity: Int,
    val status: String,
    val product: Product
)
