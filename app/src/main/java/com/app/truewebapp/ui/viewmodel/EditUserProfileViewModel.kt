package com.app.truewebapp.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import com.app.truewebapp.data.dto.profile.ProfileRequest
import com.app.truewebapp.data.dto.profile.UserProfileResponse
import com.app.truewebapp.data.repository.EditUserProfileRepository
import com.app.truewebapp.ui.base.BaseViewModel

class EditUserProfileViewModel : BaseViewModel() {

    var userProfileResponse= MutableLiveData<UserProfileResponse>()

    fun editUserProfile(token:String,profileRequest: ProfileRequest) {
        isLoading.value = true
        EditUserProfileRepository.editUserProfile({
            userProfileResponse.value = it
            isLoading.value = false
        }, {
            apiError.value = it
            isLoading.value = false
        }, {
            onFailure.value = it
            isLoading.value = false
        },token,profileRequest)
    }
}