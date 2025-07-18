package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.delivery.DeliveryMethodsResponse
import com.app.truewebapp.data.repository.DeliveryMethodsRepository
import com.app.truewebapp.ui.base.BaseViewModel

class DeliveryMethodsViewModel : BaseViewModel() {

    var deliveryMethodsResponse= MutableLiveData<DeliveryMethodsResponse>()

    fun deliveryMethods(token: String) {
        isLoading.value = true
        DeliveryMethodsRepository.deliveryMethods({
            deliveryMethodsResponse.value = it
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