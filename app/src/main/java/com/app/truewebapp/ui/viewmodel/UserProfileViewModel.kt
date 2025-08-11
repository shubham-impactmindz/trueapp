package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.profile.UserProfileResponse
import com.app.truewebapp.data.repository.UserProfileRepository
import com.app.truewebapp.ui.base.BaseViewModel

class UserProfileViewModel : BaseViewModel() {

    var userProfileResponse= MutableLiveData<UserProfileResponse>()

    fun userProfile(token:String) {
        isLoading.value = true
        UserProfileRepository.userProfile({
            userProfileResponse.value = it
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