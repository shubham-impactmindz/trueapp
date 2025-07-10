package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.register.VerifyRepResponse
import com.app.truewebapp.data.repository.VerifyRepRepository
import com.app.truewebapp.ui.base.BaseViewModel

class VerifyRepViewModel : BaseViewModel() {

    var verifyRepResponse= MutableLiveData<VerifyRepResponse>()

    fun verify(userName: String) {
        isLoading.value = true
        VerifyRepRepository.verify({
            verifyRepResponse.value = it
            isLoading.value = false
        }, {
            apiError.value = it
            isLoading.value = false
        }, {
            onFailure.value = it
            isLoading.value = false
        }, userName)
    }
}