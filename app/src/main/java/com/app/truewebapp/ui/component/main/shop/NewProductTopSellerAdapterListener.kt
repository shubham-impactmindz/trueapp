package com.app.truewebapp.ui.component.main.shop

interface NewProductTopSellerAdapterListener {
    fun onUpdateWishlist(mvariant_id: String, type:String)
    fun onUpdateCart(totalItems: Int, productId: Int,)
}