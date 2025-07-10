package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.dashboard_banners.NewProductsBannersResponse
import com.app.truewebapp.data.repository.NewProductsBannerRepository
import com.app.truewebapp.ui.base.BaseViewModel

class NewProductsBannersViewModel : BaseViewModel() {

    var newProductsBannersResponse= MutableLiveData<NewProductsBannersResponse>()

    fun newProductBanners(token: String) {
        isLoading.value = true
        NewProductsBannerRepository.newProductsBanners({
            newProductsBannersResponse.value = it
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