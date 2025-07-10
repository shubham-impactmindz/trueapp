package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.dashboard_banners.SmallBannersResponse
import com.app.truewebapp.data.repository.SmallBannerRepository
import com.app.truewebapp.ui.base.BaseViewModel

class SmallBannersViewModel : BaseViewModel() {

    var smallBannersModel= MutableLiveData<SmallBannersResponse>()

    fun smallBanners(token: String) {
        isLoading.value = true
        SmallBannerRepository.smallBanners({
            smallBannersModel.value = it
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