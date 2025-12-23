package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.stripe.StripeConfigResponse
import com.app.truewebapp.data.repository.StripeConfigRepository
import com.app.truewebapp.ui.base.BaseViewModel

class StripeConfigViewModel : BaseViewModel() {

    var stripeConfigResponse = MutableLiveData<StripeConfigResponse>()

    fun stripeConfig(token: String) {
        isLoading.value = true
        StripeConfigRepository.stripeConfig(
            {
                stripeConfigResponse.value = it
                isLoading.value = false
            },
            {
                apiError.value = it
                isLoading.value = false
            },
            {
                onFailure.value = it
                isLoading.value = false
            },
            token
        )
    }
}







