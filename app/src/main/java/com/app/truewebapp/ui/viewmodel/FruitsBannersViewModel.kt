package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.dashboard_banners.FruitsBannersResponse
import com.app.truewebapp.data.repository.FruitsBannerRepository
import com.app.truewebapp.ui.base.BaseViewModel

class FruitsBannersViewModel : BaseViewModel() {

    var fruitsBannersModel= MutableLiveData<FruitsBannersResponse>()

    fun fruitsBanners(token: String) {
        isLoading.value = true
        FruitsBannerRepository.fruitsBanners({
            fruitsBannersModel.value = it
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