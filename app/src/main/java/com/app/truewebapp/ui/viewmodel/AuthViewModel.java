package com.app.truewebapp.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AuthViewModel extends ViewModel {

    public MutableLiveData<String> email = new MutableLiveData<>();
    public MutableLiveData<String> password = new MutableLiveData<>();
    private MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>();

    public void onLoginClick() {
        if (email.getValue() != null && password.getValue() != null) {
            loginSuccess.setValue(true);  // Simulate login success
        } else {
            loginSuccess.setValue(false);
        }
    }

    public LiveData<Boolean> getLoginSuccess() {
        return loginSuccess;
    }
}
