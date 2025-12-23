package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.stripe.PaymentIntentRequest
import com.app.truewebapp.data.dto.stripe.PaymentIntentResponse
import com.app.truewebapp.data.repository.PaymentIntentRepository
import com.app.truewebapp.ui.base.BaseViewModel

class PaymentIntentViewModel : BaseViewModel() {

    var paymentIntentResponse = MutableLiveData<PaymentIntentResponse>()

    fun createPaymentIntent(token: String, paymentIntentRequest: PaymentIntentRequest) {
        isLoading.value = true
        PaymentIntentRepository.createPaymentIntent(
            {
                paymentIntentResponse.value = it
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
            token,
            paymentIntentRequest
        )
    }
}



