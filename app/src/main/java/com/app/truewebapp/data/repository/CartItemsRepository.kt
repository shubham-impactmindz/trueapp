package com.app.truewebapp.data.repository

import android.util.Log
import com.app.truewebapp.data.api.ApiHelper
import com.app.truewebapp.data.dto.cart.CartResponse
import com.app.truewebapp.httpCodes
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object CartItemsRepository {
    private val webService = ApiHelper.createService()
     fun cart(
         successHandler: (CartResponse) -> Unit,
         failureHandler: (String) -> Unit,
         onFailure: (Throwable) -> Unit,
         token: String,
    ) {
        webService.fetchCartItems(token)
            .enqueue(object : Callback<CartResponse> {
                override fun onResponse(
                    call: Call<CartResponse>,
                    response: Response<CartResponse>
                ) {
                    response.body()?.let {
                        successHandler(it)
                    }
                    Log.e("TAG", "onResponse: "+call.request())
                    Log.e("TAG", "onResponse: "+call.request().url)
                    Log.e("TAG", "onResponse: "+call.request().headers)
                    Log.e("TAG", "onResponse: "+call.request().body)
                    if (response.code() == 400) {
                        val jsonObj = JSONObject(response.errorBody()!!.charStream().readText())
                        failureHandler(jsonObj.getString("message"))
                    }
                    if (response.code() == httpCodes.STATUS_API_VALIDATION_ERROR) {
                        val jsonObj = JSONObject(response.errorBody()!!.charStream().readText())
                        failureHandler(jsonObj.getString("message"))

                    } else {
                        response.errorBody()?.let {
                            val jsonObj = JSONObject(response.errorBody()!!.charStream().readText())
                            failureHandler(jsonObj.getString("message"))
                        }
                    }
                }

                override fun onFailure(call: Call<CartResponse>, t: Throwable) {
                    onFailure(t)
                }
            })
    }
}