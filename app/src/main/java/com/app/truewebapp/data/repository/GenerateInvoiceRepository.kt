package com.app.truewebapp.data.repository

import android.util.Log
import com.app.truewebapp.data.api.ApiHelper
import com.app.truewebapp.data.dto.order.GenerateInvoiceRequest
import com.app.truewebapp.data.dto.order.GenerateInvoiceResponse
import com.app.truewebapp.httpCodes
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object GenerateInvoiceRepository {
    private val webService = ApiHelper.createService()
    
    fun generateInvoice(
        successHandler: (GenerateInvoiceResponse) -> Unit,
        failureHandler: (String) -> Unit,
        onFailure: (Throwable) -> Unit,
        token: String,
        generateInvoiceRequest: GenerateInvoiceRequest
    ) {
        webService.generateInvoice(token, generateInvoiceRequest)
            .enqueue(object : Callback<GenerateInvoiceResponse> {
                override fun onResponse(
                    call: Call<GenerateInvoiceResponse>,
                    response: Response<GenerateInvoiceResponse>
                ) {
                    response.body()?.let {
                        successHandler(it)
                    }
                    Log.d("GenerateInvoice", "onResponse: ${response.body()}")
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

                override fun onFailure(call: Call<GenerateInvoiceResponse>, t: Throwable) {
                    onFailure(t)
                }
            })
    }
}


