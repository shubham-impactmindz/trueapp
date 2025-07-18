package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.bank.BankDetailResponse
import com.app.truewebapp.data.repository.BankDetailRepository
import com.app.truewebapp.ui.base.BaseViewModel

class BankDetailViewModel : BaseViewModel() {

    var bankDetailResponse= MutableLiveData<BankDetailResponse>()

    fun bankDetails(token: String) {
        isLoading.value = true
        BankDetailRepository.bankDetails({
            bankDetailResponse.value = it
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