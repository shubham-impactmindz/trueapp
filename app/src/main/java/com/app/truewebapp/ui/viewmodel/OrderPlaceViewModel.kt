package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.order.OrderPlaceResponse
import com.app.truewebapp.data.dto.order.OrderRequest
import com.app.truewebapp.data.repository.OrderPlaceRepository
import com.app.truewebapp.ui.base.BaseViewModel

class OrderPlaceViewModel : BaseViewModel() {

    var orderPlaceResponse= MutableLiveData<OrderPlaceResponse>()

    fun orderPlace(token:String, orderRequest: OrderRequest) {
        isLoading.value = true
        OrderPlaceRepository.orderPlace({
            orderPlaceResponse.value = it
            isLoading.value = false
        }, {
            apiError.value = it
            isLoading.value = false
        }, {
            onFailure.value = it
            isLoading.value = false
        },token, orderRequest)
    }
}