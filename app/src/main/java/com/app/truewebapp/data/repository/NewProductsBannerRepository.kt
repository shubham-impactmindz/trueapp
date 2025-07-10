package com.app.truewebapp.data.repository

import android.util.Log
import com.app.truewebapp.data.api.ApiHelper
import com.app.truewebapp.data.dto.dashboard_banners.NewProductsBannersResponse
import com.app.truewebapp.httpCodes
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object NewProductsBannerRepository {
    private val webService = ApiHelper.createService()
     fun newProductsBanners(
         successHandler: (NewProductsBannersResponse) -> Unit,
         failureHandler: (String) -> Unit,
         onFailure: (Throwable) -> Unit,
         token: String,
    ) {
        webService.fetchNewProductsBanners(token)
            .enqueue(object : Callback<NewProductsBannersResponse> {
                override fun onResponse(
                    call: Call<NewProductsBannersResponse>,
                    response: Response<NewProductsBannersResponse>
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

                override fun onFailure(call: Call<NewProductsBannersResponse>, t: Throwable) {
                    onFailure(t)
                }
            })
    }
}