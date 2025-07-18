package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.delivery.DeliverySettingsResponse
import com.app.truewebapp.data.repository.DeliverySettingsRepository
import com.app.truewebapp.ui.base.BaseViewModel

class DeliverySettingsViewModel : BaseViewModel() {

    var deliverySettingsResponse= MutableLiveData<DeliverySettingsResponse>()

    fun deliverySetting(token: String) {
        isLoading.value = true
        DeliverySettingsRepository.deliverySetting({
            deliverySettingsResponse.value = it
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