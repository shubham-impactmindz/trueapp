package com.app.truewebapp.data.dto.wishlist

data class WishlistResponse(
    val status: Boolean,
    val message: String,
    val item: Item?,
)

data class Item(
    val mvariant_id: String,
    val updated_at: String,
    val created_at: String,
    val wishlist_id: Int,
)
//@WishlistResponse.kt has been updated and i think we need to update the logic in @ShopFragment.kt @DashboardFragment.kt and in @CartFragment.kt please fix
