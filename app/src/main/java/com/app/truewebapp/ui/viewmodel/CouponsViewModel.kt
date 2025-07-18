package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.coupons.CouponsResponse
import com.app.truewebapp.data.repository.CouponsRepository
import com.app.truewebapp.ui.base.BaseViewModel

class CouponsViewModel : BaseViewModel() {

    var couponsResponse= MutableLiveData<CouponsResponse>()

    fun coupons(token: String) {
        isLoading.value = true
        CouponsRepository.coupons({
            couponsResponse.value = it
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