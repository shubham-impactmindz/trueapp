package com.app.truewebapp.data.repository

import android.util.Log
import com.app.truewebapp.data.api.ApiHelper
import com.app.truewebapp.data.dto.brands.BrandsResponse
import com.app.truewebapp.httpCodes
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object BrandsRepository {
    private val webService = ApiHelper.createService()
     fun brands(
         successHandler: (BrandsResponse) -> Unit,
         failureHandler: (String) -> Unit,
         onFailure: (Throwable) -> Unit,
         token: String,
         userId: String?,
    ) {
        webService.fetchBrands(token, userId.toString())
            .enqueue(object : Callback<BrandsResponse> {
                override fun onResponse(
                    call: Call<BrandsResponse>,
                    response: Response<BrandsResponse>
                ) {
                    response.body()?.let {
                        successHandler(it)
                    }
                    Log.e("TAG", "onResponse: "+call.request().url)
                    Log.e("TAG", "onResponse: "+call.request().headers)

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

                override fun onFailure(call: Call<BrandsResponse>, t: Throwable) {
                    Log.e("TAG", "onFailure: "+call.request().headers)
                    Log.e("TAG", "onFailure: "+call.request().url)
                    Log.e("TAG", "onFailure: "+call.request().body)
                    onFailure(t)
                }
            })
    }
}