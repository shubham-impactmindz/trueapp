package com.app.truewebapp

import android.app.Application
import android.content.SharedPreferences

class TrueWebApplication : Application() {
    lateinit var sharedPreferences: SharedPreferences

    init {
        mInstance = this
    }

    companion object {
        var mInstance: TrueWebApplication? = null
        fun getInstance(): TrueWebApplication {
            return mInstance as TrueWebApplication
        }
    }
}