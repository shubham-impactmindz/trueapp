package com.app.truewebapp.utils;

import android.content.Context;
import android.util.Log;

import com.app.truewebapp.R;


public class ApiFailureTypes {
    public String getFailureMessage(Throwable error, Context context) {

        Log.e("error-",error.getMessage());
        Log.e("error-",error.getLocalizedMessage());
        String message;

        //when throwable is null
        if (error == null) {
            message = context.getResources().getString(R.string.failure_time_out_error);

        }
        //when throwable message is null
        else if (error.getLocalizedMessage() == null) {
            message = context.getResources().getString(R.string.failure_time_out_error);

        }
        //when device disconnected after requesting failure
        else if (error.getLocalizedMessage().toUpperCase().contains("ETIMEDOUT")) {
            message = context.getResources().getString(R.string.failure_internet_connection);

        }
        //when device internet connection connected and disconnected
        else if (error.getLocalizedMessage().toUpperCase().contains("ECONNRESET")) {
            message = context.getResources().getString(R.string.failure_internet_connection);
        }
        //when server is not responding
        else if (error.getLocalizedMessage().toUpperCase().contains("FAILED TO CONNECT TO")) {
            message = context.getResources().getString(R.string.failure_server_not_responding);
        }
        //
        else {
            message = context.getResources().getString(R.string.failure_something_went_wrong);
        }

        return message;
    }
}