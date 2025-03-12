package com.app.truewebapp.di.component

import com.app.truewebapp.di.component.ApplicationComponent
import com.app.truewebapp.di.ActivityScope
import com.app.truewebapp.di.module.ActivityModule
import dagger.Component

@ActivityScope
@Component(dependencies = [ApplicationComponent::class], modules = [ActivityModule::class])
interface ActivityComponent {



}