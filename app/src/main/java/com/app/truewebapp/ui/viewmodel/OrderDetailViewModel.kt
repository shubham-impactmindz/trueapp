package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.app.truewebapp.data.dto.order.OrderDetailResponse
import com.app.truewebapp.data.repository.OrderDetailRepository

class OrderDetailViewModel : ViewModel() {
    private val orderDetailResponse = MutableLiveData<OrderDetailResponse>()
    val orderDetailResponseLiveData: LiveData<OrderDetailResponse> = orderDetailResponse

    private val apiError = MutableLiveData<String>()
    val apiErrorLiveData: LiveData<String> = apiError

    private val onFailure = MutableLiveData<Throwable>()
    val onFailureLiveData: LiveData<Throwable> = onFailure

    private val isLoading = MutableLiveData<Boolean>()
    val isLoadingLiveData: LiveData<Boolean> = isLoading

    fun orderDetail(orderId: Int, token: String) {
        isLoading.postValue(true)
        OrderDetailRepository.orderDetail(
            successHandler = { response ->
                orderDetailResponse.postValue(response)
                isLoading.postValue(false)
            },
            failureHandler = { error ->
                apiError.postValue(error)
                isLoading.postValue(false)
            },
            onFailure = { throwable ->
                onFailure.postValue(throwable)
                isLoading.postValue(false)
            },
            orderId = orderId,
            token = token
        )
    }
}

