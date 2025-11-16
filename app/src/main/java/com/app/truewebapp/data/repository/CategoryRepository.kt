package com.app.truewebapp.data.repository

import android.util.Log
import com.app.truewebapp.data.api.ApiHelper
import com.app.truewebapp.data.dto.browse.CategoriesResponse
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object CategoryRepository {
    private val webService = ApiHelper.createService()
     fun categories(
         successHandler: (CategoriesResponse) -> Unit,
         failureHandler: (String) -> Unit,
         onFailure: (Throwable) -> Unit,
         request: String,
         token: String,
         filters: String,
         wishlist: Boolean? = null,
    ) {
        webService.fetchCategories(request,token,filters,wishlist)
            .enqueue(object : Callback<CategoriesResponse> {
                override fun onResponse(
                    call: Call<CategoriesResponse>,
                    response: Response<CategoriesResponse>
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
                }

                override fun onFailure(call: Call<CategoriesResponse>, t: Throwable) {
                    Log.e("TAG", "onResponse: "+call.request().url)
                    Log.e("TAG", "onResponse: "+call.request().headers)
                    Log.e("TAG", "onResponse: "+call.request().body)
                    onFailure(t)
                }
            })
    }
}