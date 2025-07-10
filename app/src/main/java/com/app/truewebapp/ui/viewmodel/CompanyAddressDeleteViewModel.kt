package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.change_password.ChangePasswordResponse
import com.app.truewebapp.data.dto.company_address.CompanyAddressDeleteRequest
import com.app.truewebapp.data.repository.CompanyAddressDeleteRepository
import com.app.truewebapp.ui.base.BaseViewModel

class CompanyAddressDeleteViewModel : BaseViewModel() {

    var companyAddressResponse= MutableLiveData<ChangePasswordResponse>()

    fun deleteCompanyAddress(token: String,deleteRequest: CompanyAddressDeleteRequest) {
        isLoading.value = true
        CompanyAddressDeleteRepository.deleteCompanyAddress({
            companyAddressResponse.value = it
            isLoading.value = false
        }, {
            apiError.value = it
            isLoading.value = false
        }, {
            onFailure.value = it
            isLoading.value = false
        },token,deleteRequest)
    }
}