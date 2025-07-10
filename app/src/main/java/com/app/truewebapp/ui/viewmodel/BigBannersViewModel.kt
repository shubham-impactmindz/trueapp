package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.dashboard_banners.BigBannersResponse
import com.app.truewebapp.data.repository.BigBannerRepository
import com.app.truewebapp.ui.base.BaseViewModel

class BigBannersViewModel : BaseViewModel() {

    var bigBannersModel= MutableLiveData<BigBannersResponse>()

    fun bigBanners(token: String) {
        isLoading.value = true
        BigBannerRepository.bigBanners({
            bigBannersModel.value = it
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