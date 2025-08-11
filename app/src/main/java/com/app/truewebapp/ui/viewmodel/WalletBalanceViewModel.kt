package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.wallet.WalletBalanceResponse
import com.app.truewebapp.data.repository.WalletBalanceRepository
import com.app.truewebapp.ui.base.BaseViewModel

class WalletBalanceViewModel : BaseViewModel() {

    var walletBalanceResponse= MutableLiveData<WalletBalanceResponse>()

    fun walletBalance(token: String) {
        isLoading.value = true
        WalletBalanceRepository.walletBalance({
            walletBalanceResponse.value = it
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