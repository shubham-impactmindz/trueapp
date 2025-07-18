package com.app.truewebapp.data.repository

import android.util.Log
import com.app.truewebapp.data.api.ApiHelper
import com.app.truewebapp.data.dto.order.OrderPlaceResponse
import com.app.truewebapp.data.dto.order.OrderRequest
import com.app.truewebapp.httpCodes
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object OrderPlaceRepository {
    private val webService = ApiHelper.createService()
     fun orderPlace(
         successHandler: (OrderPlaceResponse) -> Unit,
         failureHandler: (String) -> Unit,
         onFailure: (Throwable) -> Unit,
         token: String,
         orderRequest: OrderRequest
    ) {
        webService.fetchOrderPlace(token,orderRequest)
            .enqueue(object : Callback<OrderPlaceResponse> {
                override fun onResponse(
                    call: Call<OrderPlaceResponse>,
                    response: Response<OrderPlaceResponse>
                ) {
                    response.body()?.let {
                        successHandler(it)
                    }
                    Log.e("TAG", "onResponse: "+response.body())
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

                override fun onFailure(call: Call<OrderPlaceResponse>, t: Throwable) {
                    onFailure(t)
                }
            })
    }
}