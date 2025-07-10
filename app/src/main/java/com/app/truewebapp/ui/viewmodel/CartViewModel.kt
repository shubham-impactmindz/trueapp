package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.cart.CartRequest
import com.app.truewebapp.data.dto.change_password.ChangePasswordResponse
import com.app.truewebapp.data.repository.CartRepository
import com.app.truewebapp.ui.base.BaseViewModel

class CartViewModel : BaseViewModel() {

    var changePasswordResponse= MutableLiveData<ChangePasswordResponse>()

    fun cart(token:String, cartRequest: CartRequest) {
        isLoading.value = true
        CartRepository.cart({
            changePasswordResponse.value = it
            isLoading.value = false
        }, {
            apiError.value = it
            isLoading.value = false
        }, {
            onFailure.value = it
            isLoading.value = false
        },token, cartRequest)
    }
}