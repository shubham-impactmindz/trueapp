package com.app.truewebapp.di.module

import android.content.Context
import com.app.truewebapp.TrueWebApplication
import com.app.truewebapp.di.ApplicationContext
import com.app.truewebapp.di.BaseUrl
import dagger.Module
import dagger.Provides

@Module
class ApplicationModule(private val application: TrueWebApplication) {

    @ApplicationContext
    @Provides
    fun provideContext(): Context {
        return application
    }

    @BaseUrl
    @Provides
    fun provideBaseUrl(): String = "https://app.imunim.com/"

}