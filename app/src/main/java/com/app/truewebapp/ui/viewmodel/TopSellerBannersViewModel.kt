package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.dashboard_banners.TopSellerBannersResponse
import com.app.truewebapp.data.repository.TopSellerBannerRepository
import com.app.truewebapp.ui.base.BaseViewModel

class TopSellerBannersViewModel : BaseViewModel() {

    var topSellerBannersResponse= MutableLiveData<TopSellerBannersResponse>()

    fun topSellerBanners(token: String) {
        isLoading.value = true
        TopSellerBannerRepository.topSellerBanners({
            topSellerBannersResponse.value = it
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