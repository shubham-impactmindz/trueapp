package com.app.truewebapp.data.dto.cart

data class CartRequest (
    val cart: List<Cart>,
)

data class Cart (
    val mvariant_id: String,
    val quantity: String,
)