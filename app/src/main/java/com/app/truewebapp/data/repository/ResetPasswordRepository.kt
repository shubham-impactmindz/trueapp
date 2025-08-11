package com.app.truewebapp.data.repository

import com.app.truewebapp.data.api.ApiHelper
import com.app.truewebapp.data.dto.reset_password.ResetPasswordRequest
import com.app.truewebapp.data.dto.reset_password.ResetPasswordResponse
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object ResetPasswordRepository {
    private val webService = ApiHelper.createService()
     fun resetPassword(
         successHandler: (ResetPasswordResponse) -> Unit,
         failureHandler: (String) -> Unit,
         onFailure: (Throwable) -> Unit,
         resetPasswordRequest: ResetPasswordRequest
    ) {
        webService.fetchResetPassword(resetPasswordRequest)
            .enqueue(object : Callback<ResetPasswordResponse> {
                override fun onResponse(
                    call: Call<ResetPasswordResponse>,
                    response: Response<ResetPasswordResponse>
                ) {
                    response.body()?.let {
                        successHandler(it)
                    }

                    if (response.code() != 200) {
                        val jsonObj = JSONObject(response.errorBody()!!.charStream().readText())
                        failureHandler(jsonObj.getString("message"))
                    }
                }

                override fun onFailure(call: Call<ResetPasswordResponse>, t: Throwable) {
                    onFailure(t)
                }
            })
    }
}