package com.app.truewebapp.ui.component.main.shop

interface ProductAdapterListener {
    fun onUpdateWishlist(productId: String)
    fun onUpdateCart(totalItems: Int, productId: Int)
}