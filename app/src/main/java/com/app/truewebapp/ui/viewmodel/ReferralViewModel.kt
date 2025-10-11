package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.referral.ReferralRequest
import com.app.truewebapp.data.dto.referral.ReferralResponse
import com.app.truewebapp.data.repository.ReferralRepository
import com.app.truewebapp.ui.base.BaseViewModel

class ReferralViewModel : BaseViewModel() {

    var referralModel= MutableLiveData<ReferralResponse>()

    fun sendReferral(token: String, referralRequest: ReferralRequest) {
        isLoading.value = true
        ReferralRepository.sendReferral({
            referralModel.value = it
            isLoading.value = false
        }, {
            apiError.value = it
            isLoading.value = false
        }, {
            onFailure.value = it
            isLoading.value = false
        }, token, referralRequest)
    }
}