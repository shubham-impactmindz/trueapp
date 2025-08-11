package com.app.truewebapp.data.repository

import com.app.truewebapp.data.api.ApiHelper
import com.app.truewebapp.data.dto.dashboard_banners.HomeSlidersResponse
import com.app.truewebapp.httpCodes
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object HomeBannerRepository {
    private val webService = ApiHelper.createService()
     fun homeBanners(
         successHandler: (HomeSlidersResponse) -> Unit,
         failureHandler: (String) -> Unit,
         onFailure: (Throwable) -> Unit,
         token: String,
    ) {
        webService.fetchHomeBanner(token)
            .enqueue(object : Callback<HomeSlidersResponse> {
                override fun onResponse(
                    call: Call<HomeSlidersResponse>,
                    response: Response<HomeSlidersResponse>
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

                override fun onFailure(call: Call<HomeSlidersResponse>, t: Throwable) {
                    onFailure(t)
                }
            })
    }
}