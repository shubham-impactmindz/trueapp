package com.app.truewebapp.di.component

import com.app.truewebapp.di.module.ApplicationModule
import com.app.truewebapp.TrueWebApplication
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [ApplicationModule::class])
interface ApplicationComponent {

    fun inject(application: TrueWebApplication)
//    @ApplicationContext
//    fun getContext(): Context
//
//    fun getLoginRepository(): LoginRepository
//
//
//    fun getDashboardRepository(): DashboardRepository
//
//    fun getAddClientRepository(): AddClientRepository

}