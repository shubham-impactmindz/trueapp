package com.app.truewebapp.data.repository

import com.app.truewebapp.data.api.ApiHelper
import com.app.truewebapp.data.dto.login.LoginRequest
import com.app.truewebapp.data.dto.login.LoginResponse
import com.app.truewebapp.httpCodes
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object LoginRepository {
    private val webService = ApiHelper.createService()
     fun login(
         successHandler: (LoginResponse) -> Unit,
         failureHandler: (String) -> Unit,
         onFailure: (Throwable) -> Unit,
         loginRequest: LoginRequest,
    ) {
        webService.fetchLogin(loginRequest)
            .enqueue(object : Callback<LoginResponse> {
                override fun onResponse(
                    call: Call<LoginResponse>,
                    response: Response<LoginResponse>
                ) {
                    response.body()?.let {
                        successHandler(it)
                    }

                    if (response.code() == 400) {
                        val jsonObj = JSONObject(response.errorBody()!!.charStream().readText())
                        failureHandler(jsonObj.getString("message"))
                    }
                    if (response.code() == httpCodes.STATUS_API_VALIDATION_ERROR) {
                        response.errorBody()?.let {
                            val error = ApiHelper.handleAuthenticationError(response.errorBody()!!)
                            failureHandler(error)
                        }

                    } else {
                        response.errorBody()?.let {
                            val error = ApiHelper.handleApiError(response.errorBody()!!)
                            failureHandler(error)
                        }
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    onFailure(t)
                }
            })
    }
}