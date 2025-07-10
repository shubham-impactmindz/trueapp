package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.change_password.ChangePasswordResponse
import com.app.truewebapp.data.dto.company_address.CompanyAddressRequest
import com.app.truewebapp.data.repository.CompanyAddressCreateRepository
import com.app.truewebapp.ui.base.BaseViewModel

class CompanyAddressUpsertViewModel : BaseViewModel() {

    var companyAddressResponse= MutableLiveData<ChangePasswordResponse>()

    fun companyAddress(token: String,companyAddressRequest: CompanyAddressRequest) {
        isLoading.value = true
        CompanyAddressCreateRepository.createCompanyAddress({
            companyAddressResponse.value = it
            isLoading.value = false
        }, {
            apiError.value = it
            isLoading.value = false
        }, {
            onFailure.value = it
            isLoading.value = false
        },token,companyAddressRequest)
    }
}