package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.reset_password.ResetPasswordRequest
import com.app.truewebapp.data.dto.reset_password.ResetPasswordResponse
import com.app.truewebapp.data.repository.ResetPasswordRepository
import com.app.truewebapp.ui.base.BaseViewModel

class ResetPasswordViewModel : BaseViewModel() {

    var resetPasswordResponse= MutableLiveData<ResetPasswordResponse>()

    fun resetPassword(resetPasswordRequest: ResetPasswordRequest) {
        isLoading.value = true
        ResetPasswordRepository.resetPassword({
            resetPasswordResponse.value = it
            isLoading.value = false
        }, {
            apiError.value = it
            isLoading.value = false
        }, {
            onFailure.value = it
            isLoading.value = false
        },resetPasswordRequest)
    }
}