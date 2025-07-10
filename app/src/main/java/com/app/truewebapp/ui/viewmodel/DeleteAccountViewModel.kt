package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.change_password.ChangePasswordResponse
import com.app.truewebapp.data.repository.DeleteAccountRepository
import com.app.truewebapp.ui.base.BaseViewModel

class DeleteAccountViewModel : BaseViewModel() {

    var changePasswordResponse= MutableLiveData<ChangePasswordResponse>()

    fun deleteAccount(token:String) {
        isLoading.value = true
        DeleteAccountRepository.deleteAccount({
            changePasswordResponse.value = it
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