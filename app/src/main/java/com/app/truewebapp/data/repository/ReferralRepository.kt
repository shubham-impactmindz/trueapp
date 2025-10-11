package com.app.truewebapp.data.repository

import android.util.Log
import com.app.truewebapp.data.api.ApiHelper
import com.app.truewebapp.data.dto.referral.ReferralRequest
import com.app.truewebapp.data.dto.referral.ReferralResponse
import com.app.truewebapp.httpCodes
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object ReferralRepository {
    private val webService = ApiHelper.createService()
     fun sendReferral(
         successHandler: (ReferralResponse) -> Unit,
         failureHandler: (String) -> Unit,
         onFailure: (Throwable) -> Unit,
         token: String,
         referralRequest: ReferralRequest
    ) {
        webService.sendReferral(token,referralRequest)
            .enqueue(object : Callback<ReferralResponse> {
                override fun onResponse(
                    call: Call<ReferralResponse>,
                    response: Response<ReferralResponse>
                ) {
                    response.body()?.let {
                        successHandler(it)
                    }
                    Log.e("TAG", "onResponse: "+response.body())
                    Log.e("TAG", "onResponse: "+response.headers())
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

                override fun onFailure(call: Call<ReferralResponse>, t: Throwable) {
                    onFailure(t)
                }
            })
    }
}