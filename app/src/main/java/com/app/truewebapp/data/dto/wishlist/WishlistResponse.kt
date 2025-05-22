package com.app.truewebapp.data.dto.wishlist

data class WishlistResponse(
    val status: Boolean,
    val message: String,
    val item: Item?,
)

data class Item(
    val user_id: String,
    val mproduct_id: String,
    val updated_at: String,
    val created_at: String,
    val wishlist_id: Int,
)
