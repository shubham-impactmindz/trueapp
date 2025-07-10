package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.dashboard_banners.RoundBannersResponse
import com.app.truewebapp.data.repository.RoundBannerRepository
import com.app.truewebapp.ui.base.BaseViewModel

class RoundBannersViewModel : BaseViewModel() {

    var roundBannersModel= MutableLiveData<RoundBannersResponse>()

    fun roundBanners(token: String) {
        isLoading.value = true
        RoundBannerRepository.roundBanners({
            roundBannersModel.value = it
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