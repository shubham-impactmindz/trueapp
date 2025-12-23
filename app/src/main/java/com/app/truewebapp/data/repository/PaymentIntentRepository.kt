package com.app.truewebapp.data.repository

import android.util.Log
import com.app.truewebapp.data.api.ApiHelper
import com.app.truewebapp.data.dto.stripe.PaymentIntentRequest
import com.app.truewebapp.data.dto.stripe.PaymentIntentResponse
import com.app.truewebapp.httpCodes
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object PaymentIntentRepository {
    private val webService = ApiHelper.createService()
    
    fun createPaymentIntent(
        successHandler: (PaymentIntentResponse) -> Unit,
        failureHandler: (String) -> Unit,
        onFailure: (Throwable) -> Unit,
        token: String,
        paymentIntentRequest: PaymentIntentRequest
    ) {
        webService.createPaymentIntent(token, paymentIntentRequest)
            .enqueue(object : Callback<PaymentIntentResponse> {
                override fun onResponse(
                    call: Call<PaymentIntentResponse>,
                    response: Response<PaymentIntentResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        response.body()?.let {
                            successHandler(it)
                        }
                        Log.d("PaymentIntent", "onResponse: " + response.body())
                    } else {
                        if (response.code() == 400) {
                            try {
                                val jsonObj = JSONObject(response.errorBody()!!.charStream().readText())
                                failureHandler(jsonObj.getString("message"))
                            } catch (e: Exception) {
                                failureHandler("Bad request")
                            }
                        } else if (response.code() == httpCodes.STATUS_API_VALIDATION_ERROR) {
                            response.errorBody()?.let {
                                val error = ApiHelper.handleAuthenticationError(response.errorBody()!!)
                                failureHandler(error)
                            }
                        } else {
                            response.errorBody()?.let {
                                val error = ApiHelper.handleApiError(response.errorBody()!!)
                                failureHandler(error)
                            } ?: run {
                                failureHandler("Unknown error occurred")
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<PaymentIntentResponse>, t: Throwable) {
                    onFailure(t)
                }
            })
    }
}



