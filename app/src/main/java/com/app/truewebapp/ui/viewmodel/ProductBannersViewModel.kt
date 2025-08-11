package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.dashboard_banners.ProductSlidersResponse
import com.app.truewebapp.data.repository.ProductBannerRepository
import com.app.truewebapp.ui.base.BaseViewModel

class ProductBannersViewModel : BaseViewModel() {

    var productSlidersResponse= MutableLiveData<ProductSlidersResponse>()

    fun productBanners(token: String) {
        isLoading.value = true
        ProductBannerRepository.productBanners({
            productSlidersResponse.value = it
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