package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.order.GenerateInvoiceRequest
import com.app.truewebapp.data.dto.order.GenerateInvoiceResponse
import com.app.truewebapp.data.repository.GenerateInvoiceRepository
import com.app.truewebapp.ui.base.BaseViewModel

class GenerateInvoiceViewModel : BaseViewModel() {

    var generateInvoiceResponse = MutableLiveData<GenerateInvoiceResponse>()

    fun generateInvoice(token: String, generateInvoiceRequest: GenerateInvoiceRequest) {
        isLoading.value = true
        GenerateInvoiceRepository.generateInvoice({
            generateInvoiceResponse.value = it
            isLoading.value = false
        }, {
            apiError.value = it
            isLoading.value = false
        }, {
            onFailure.value = it
            isLoading.value = false
        }, token, generateInvoiceRequest)
    }
}


