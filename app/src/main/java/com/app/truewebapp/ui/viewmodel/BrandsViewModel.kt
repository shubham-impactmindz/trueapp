package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.brands.BrandsResponse
import com.app.truewebapp.data.repository.BrandsRepository
import com.app.truewebapp.ui.base.BaseViewModel

class BrandsViewModel : BaseViewModel() {

    var brandsResponse= MutableLiveData<BrandsResponse>()

    fun brands(token: String, userId: String?) {
        isLoading.value = true
        BrandsRepository.brands({
            brandsResponse.value = it
            isLoading.value = false
        }, {
            apiError.value = it
            isLoading.value = false
        }, {
            onFailure.value = it
            isLoading.value = false
        },token,userId)
    }
}