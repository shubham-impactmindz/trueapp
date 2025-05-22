package com.app.truewebapp.data.repository

import android.util.Log
import com.app.truewebapp.data.api.ApiHelper
import com.app.truewebapp.data.dto.wishlist.WishlistRequest
import com.app.truewebapp.data.dto.wishlist.WishlistResponse
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object WishlistRepository {
    private val webService = ApiHelper.createService()
     fun wishlist(
         successHandler: (WishlistResponse) -> Unit,
         failureHandler: (String) -> Unit,
         onFailure: (Throwable) -> Unit,
         wishlistRequest: WishlistRequest,
         token: String,
    ) {
        webService.fetchWishlist(token,wishlistRequest,)
            .enqueue(object : Callback<WishlistResponse> {
                override fun onResponse(
                    call: Call<WishlistResponse>,
                    response: Response<WishlistResponse>
                ) {
                    try {
                        Log.d("API_RESPONSE_CODE", response.code().toString())

                        val raw = response.errorBody()?.string()
                        Log.d("API_RAW_RESPONSE", raw ?: "null")

                        if (response.isSuccessful) {
                            val body = response.body()
                            Log.d("API_SUCCESS_BODY", body.toString())

                            body?.let {
                                successHandler(it)
                            } ?: run {
                                failureHandler("Empty response body")
                            }
                        } else {
                            val jsonString = raw ?: return failureHandler("No error body")

                            try {
                                val jsonObj = JSONObject(jsonString)

                                // Check if it's wrapped as string
                                if (jsonString.trim().startsWith("\"{")) {
                                    val unwrapped = JSONObject(jsonObj.getString("response"))
                                    Log.d("API_UNWRAPPED_JSON", unwrapped.toString())
                                }

                                failureHandler(jsonObj.optString("message", "Something went wrong"))
                            } catch (e: Exception) {
                                failureHandler("Failed to parse error body")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("API_EXCEPTION", e.message ?: "Unknown error")
                        failureHandler("Unexpected error")
                    }
                }


                override fun onFailure(call: Call<WishlistResponse>, t: Throwable) {
                    onFailure(t)
                }
            })
    }
}