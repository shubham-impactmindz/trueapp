package com.app.truewebapp

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.app.truewebapp.di.component.ApplicationComponent
//import com.app.accutecherp.di.component.DaggerApplicationComponent

class TrueWebApplication : Application() {
    lateinit var applicationComponent: ApplicationComponent
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

    override fun onCreate() {
        super.onCreate()
        injectDependencies()
    }

    @Synchronized


    private fun injectDependencies() {
//        applicationComponent = DaggerApplicationComponent
//            .builder()
//            .applicationModule(ApplicationModule(this))
//            .build()
//        applicationComponent.inject(this)
    }

    fun getip(): String? {
        val preferences = getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        return preferences.getString("ip", null)
    }

    fun setCartId(cartId: String?) {
        val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
        preferences.edit().putString("cartId", cartId).commit()
    }
}