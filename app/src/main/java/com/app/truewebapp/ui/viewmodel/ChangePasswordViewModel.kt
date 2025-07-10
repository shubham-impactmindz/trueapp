package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.change_password.ChangePasswordRequest
import com.app.truewebapp.data.dto.change_password.ChangePasswordResponse
import com.app.truewebapp.data.repository.ChangePasswordRepository
import com.app.truewebapp.ui.base.BaseViewModel

class ChangePasswordViewModel : BaseViewModel() {

    var changePasswordResponse= MutableLiveData<ChangePasswordResponse>()

    fun changePassword(token:String, changePasswordRequest: ChangePasswordRequest) {
        isLoading.value = true
        ChangePasswordRepository.changePassword({
            changePasswordResponse.value = it
            isLoading.value = false
        }, {
            apiError.value = it
            isLoading.value = false
        }, {
            onFailure.value = it
            isLoading.value = false
        },token, changePasswordRequest)
    }
}