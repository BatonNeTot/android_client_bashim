package com.notjuststudio.bashim.dagger

import android.content.Context
import com.notjuststudio.bashim.service.QuoteSaverController
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class QuoteSaverModule {

    @Provides
    @Singleton
    fun provideQuoteSaverController(context: Context) = QuoteSaverController(context)

}