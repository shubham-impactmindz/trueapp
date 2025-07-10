package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.cart.CartResponse
import com.app.truewebapp.data.repository.CartItemsRepository
import com.app.truewebapp.ui.base.BaseViewModel

class CartItemsViewModel : BaseViewModel() {

    var cartResponse= MutableLiveData<CartResponse>()

    fun cart(token:String) {
        isLoading.value = true
        CartItemsRepository.cart({
            cartResponse.value = it
            isLoading.value = false
        }, {
            apiError.value = it
            isLoading.value = false
        }, {
            onFailure.value = it
            isLoading.value = false
        },token)
    }
}