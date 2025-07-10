package com.app.truewebapp.ui.component.main.shop

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

data class CartData(val totalItems: Int, val productId: Int, val totalPrice: Double)

class ShopViewModel : ViewModel() {
    private val _cartLiveData = MutableLiveData<CartData>()
    val cartLiveData: LiveData<CartData> get() = _cartLiveData

    fun updateCart(count: Int, productId: Int) {
        if (count > 0) {
            val totalPrice = count * 10.0 // Assuming each product costs $10
            _cartLiveData.value = CartData(count, productId, totalPrice)
        } else {
            _cartLiveData.value = CartData(0, 0, 0.0) // Reset cart if empty
        }
    }
}
