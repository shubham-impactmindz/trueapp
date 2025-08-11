package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.dashboard_banners.HomeSlidersResponse
import com.app.truewebapp.data.repository.HomeBannerRepository
import com.app.truewebapp.ui.base.BaseViewModel

class HomeBannersViewModel : BaseViewModel() {

    var homeSlidersResponse= MutableLiveData<HomeSlidersResponse>()

    fun homeBanners(token: String) {
        isLoading.value = true
        HomeBannerRepository.homeBanners({
            homeSlidersResponse.value = it
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