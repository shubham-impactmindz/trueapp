package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.order.OrdersResponse
import com.app.truewebapp.data.repository.OrdersRepository
import com.app.truewebapp.ui.base.BaseViewModel

class OrdersViewModel : BaseViewModel() {

    var ordersResponse= MutableLiveData<OrdersResponse>()

    fun orders(page:String,perPage:String,token:String) {
        isLoading.value = true
        OrdersRepository.orders({
            ordersResponse.value = it
            isLoading.value = false
        }, {
            apiError.value = it
            isLoading.value = false
        }, {
            onFailure.value = it
            isLoading.value = false
        },page,perPage,token)
    }
}