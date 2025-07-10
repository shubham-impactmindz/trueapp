package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.company_address.CompanyAddressResponse
import com.app.truewebapp.data.repository.CompanyAddressRepository
import com.app.truewebapp.ui.base.BaseViewModel

class CompanyAddressViewModel : BaseViewModel() {

    var companyAddressResponse= MutableLiveData<CompanyAddressResponse>()

    fun companyAddress(token: String) {
        isLoading.value = true
        CompanyAddressRepository.companyAddress({
            companyAddressResponse.value = it
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