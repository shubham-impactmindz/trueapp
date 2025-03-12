package com.app.truewebapp.ui.component.main.shop

interface ProductAdapterListener {
    fun onAddToCartClicked(totalItems: Int, productName: String)
}