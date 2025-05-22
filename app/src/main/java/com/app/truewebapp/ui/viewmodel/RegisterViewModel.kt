package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.register.RegisterRequest
import com.app.truewebapp.data.dto.register.RegisterResponse
import com.app.truewebapp.data.repository.RegisterRepository
import com.app.truewebapp.ui.base.BaseViewModel

class RegisterViewModel : BaseViewModel() {

    var registerResponse= MutableLiveData<RegisterResponse>()

    fun register(registerRequest: RegisterRequest) {
        isLoading.value = true
        RegisterRepository.register({
            registerResponse.value = it
            isLoading.value = false
        }, {
            apiError.value = it
            isLoading.value = false
        }, {
            onFailure.value = it
            isLoading.value = false
        }, registerRequest)
    }
}