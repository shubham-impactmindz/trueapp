package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.browse.BannerResponse
import com.app.truewebapp.data.repository.BannerRepository
import com.app.truewebapp.ui.base.BaseViewModel

class BannersViewModel : BaseViewModel() {

    var bannersModel= MutableLiveData<BannerResponse>()

    fun banners(token: String) {
        isLoading.value = true
        BannerRepository.banners({
            bannersModel.value = it
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