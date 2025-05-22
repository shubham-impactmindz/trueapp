package com.app.truewebapp.data.api


import com.app.truewebapp.BASE_URL
import com.app.truewebapp.SOMETHING_WENT_WRONG
import com.app.truewebapp.utils.ApiStatus
import com.app.truewebapp.utils.ErrorResponse
import com.app.truewebapp.utils.Status
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiHelper {
    private var mRetrofit: Retrofit

    //private var okHttpClient = OkHttpClient.Builder()
    // Creating Retrofit Object
    init {
        val gson = GsonBuilder()
            .setLenient()
            .create()
        mRetrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(getClient())
            .build()

    }
    // Creating OkHttpclient Object
    private fun getClient(): OkHttpClient {
        val interceptor = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
        return OkHttpClient().newBuilder().connectTimeout(1, TimeUnit.MINUTES)
            .readTimeout(1, TimeUnit.MINUTES)
            .writeTimeout(1, TimeUnit.MINUTES)
            .addInterceptor(interceptor)
//            .addInterceptor(ChuckInterceptor(Application.getContext()))
            /*.addNetworkInterceptor(AddHeaderInterceptor())*/
            .build()
    }
    // Creating OkHttpclient Object
    private fun getAppClient(): OkHttpClient {
        val interceptor = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
        return OkHttpClient().newBuilder().connectTimeout(1, TimeUnit.MINUTES)
            .readTimeout(1, TimeUnit.MINUTES)
            .writeTimeout(1, TimeUnit.MINUTES)
            .addInterceptor(interceptor)
//            .addInterceptor(ChuckInterceptor(Application.getContext()))
            //.addNetworkInterceptor(AddHeaderInterceptor())
            .build()
    }
    //val apiService: WebService = mRetrofit.create(WebService::class.java)
    //Creating service class for calling the web services
    fun createService(): WebService {
        return mRetrofit.create(WebService::class.java)
    }

    // Handling error messages returned by Apis
    fun handleApiError(body: ResponseBody?): String {
        var errorMsg = SOMETHING_WENT_WRONG
        try {
            val errorConverter: Converter<ResponseBody, ApiStatus> =
                mRetrofit.responseBodyConverter(Status::class.java, arrayOfNulls(0))
            val error: ApiStatus = errorConverter.convert(body)!!
            errorMsg = error.message
        } catch (e: Exception) {
        }

        return errorMsg
    }

    // Handling error messages returned by Apis
    fun handleAuthenticationError(body: ResponseBody?): String {
        val errorConverter: Converter<ResponseBody, ErrorResponse> =
            mRetrofit.responseBodyConverter(ErrorResponse::class.java, arrayOfNulls(0))
        val errorResponse: ErrorResponse = errorConverter.convert(body)!!
        return errorResponse.message!!
    }
}