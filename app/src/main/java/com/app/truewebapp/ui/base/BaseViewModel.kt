package com.app.truewebapp.ui.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

open class BaseViewModel:ViewModel() {
    var apiError = MutableLiveData<String>()
    var onFailure = MutableLiveData<Throwable>()
    var isLoading = MutableLiveData<Boolean>()

}