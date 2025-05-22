package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.login.LoginRequest
import com.app.truewebapp.data.dto.login.LoginResponse
import com.app.truewebapp.data.repository.LoginRepository
import com.app.truewebapp.ui.base.BaseViewModel

class LoginViewModel : BaseViewModel() {

    var loginResponse= MutableLiveData<LoginResponse>()

    fun login(loginRequest: LoginRequest) {
        isLoading.value = true
        LoginRepository.login({
            loginResponse.value = it
            isLoading.value = false
        }, {
            apiError.value = it
            isLoading.value = false
        }, {
            onFailure.value = it
            isLoading.value = false
        }, loginRequest)
    }
}