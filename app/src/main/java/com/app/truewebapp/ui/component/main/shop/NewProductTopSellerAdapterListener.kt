package com.app.truewebapp.ui.component.main.shop

interface NewProductTopSellerAdapterListener {
    fun onUpdateWishlist(productId: String, type:String)
    fun onUpdateCart(totalItems: Int, productId: Int,)
}