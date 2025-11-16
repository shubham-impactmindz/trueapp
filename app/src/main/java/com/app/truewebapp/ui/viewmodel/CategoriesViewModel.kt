package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.browse.CategoriesResponse
import com.app.truewebapp.data.repository.CategoryRepository
import com.app.truewebapp.ui.base.BaseViewModel

class CategoriesViewModel : BaseViewModel() {

    var categoriesModel= MutableLiveData<CategoriesResponse>()

    fun categories(request: String, token: String, filters: String, wishlist: Boolean? = null) {
        isLoading.value = true
        CategoryRepository.categories({
            categoriesModel.value = it
            isLoading.value = false
        }, {
            apiError.value = it
            isLoading.value = false
        }, {
            onFailure.value = it
            isLoading.value = false
        },request,token,filters,wishlist)
    }
}