package com.notjuststudio.bashim.dagger

import com.notjuststudio.bashim.App
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppModule(private val app: App) {

    @Provides
    @Singleton
    fun provideContext() = app.applicationContext

}