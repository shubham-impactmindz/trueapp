package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.service.RegisterInterestRequest
import com.app.truewebapp.data.dto.service.RegisterInterestResponse
import com.app.truewebapp.data.repository.RegisterInterestRepository
import com.app.truewebapp.ui.base.BaseViewModel

class RegisterInterestViewModel : BaseViewModel() {

    var registerInterestResponse = MutableLiveData<RegisterInterestResponse>()

    fun registerInterest(token: String, registerInterestRequest: RegisterInterestRequest) {
        isLoading.value = true
        RegisterInterestRepository.registerInterest(
            {
                registerInterestResponse.value = it
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
            registerInterestRequest
        )
    }
}







