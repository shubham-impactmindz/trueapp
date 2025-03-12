package com.app.truewebapp.di.module

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.app.truewebapp.di.ActivityContext
import dagger.Module
import dagger.Provides

@Module
class ActivityModule(private val activity: AppCompatActivity) {

    @ActivityContext
    @Provides
    fun provideContext(): Context {
        return activity
    }



  /*  @Provides
    fun provideAccountTypeViewModel(accountTypeRepository: AccountTypeRepository):AccountTypeViewModel{
        return ViewModelProvider(activity,
        ViewModelProviderFactory(AccountTypeViewModel::class){
            AccountTypeViewModel(accountTypeRepository)
        })[AccountTypeViewModel::class.java]
    }*/

   /* @Provides
    fun provideDashboardViewModel(dashboardRepository: DashboardRepository):DashboardViewModel{
        return ViewModelProvider(activity,
        ViewModelProviderFactory(DashboardViewModel::class){
            DashboardViewModel(dashboardRepository)
        })[DashboardViewModel::class.java]
    }*/

  /*  @Provides
    fun provideAddClientViewModel(addClientRepository: AddClientRepository):AddClientViewModel{
        return ViewModelProvider(activity,
            ViewModelProviderFactory(AddClientViewModel::class){
                AddClientViewModel(addClientRepository)
            })[AddClientViewModel::class.java]
    }*/

   // @Provides
   // fun provideAccountTypeAdapter() = AccountTypeAdapter(ArrayList())

}