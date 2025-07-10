package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.dashboard_banners.DealsBannersResponse
import com.app.truewebapp.data.repository.DealsBannerRepository
import com.app.truewebapp.ui.base.BaseViewModel

class DealsBannersViewModel : BaseViewModel() {

    var dealsBannersModel= MutableLiveData<DealsBannersResponse>()

    fun dealsBanners(token: String) {
        isLoading.value = true
        DealsBannerRepository.dealsBanners({
            dealsBannersModel.value = it
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