package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.wishlist.WishlistRequest
import com.app.truewebapp.data.dto.wishlist.WishlistResponse
import com.app.truewebapp.data.repository.WishlistRepository
import com.app.truewebapp.ui.base.BaseViewModel

class WishlistViewModel : BaseViewModel() {

    var wishlistResponse= MutableLiveData<WishlistResponse>()

    fun wishlist(token: String, wishlistRequest: WishlistRequest) {
        isLoading.value = true
        WishlistRepository.wishlist({
            wishlistResponse.value = it
            isLoading.value = false
        }, {
            apiError.value = it
            isLoading.value = false
        }, {
            onFailure.value = it
            isLoading.value = false
        },wishlistRequest,token)
    }
}