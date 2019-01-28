package com.notjuststudio.bashim.dagger

import android.content.Context
import com.notjuststudio.bashim.helper.*
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class HelperModule {

    @Provides
    @Singleton
    fun provideSharedPrefHelper(context: Context,
                                resource: ResourceHelper) = SharedPrefHelper(context, resource)

    @Provides
    @Singleton
    fun provideDBHelper(context: Context) = DataBaseHelper(context)

    @Provides
    @Singleton
    fun provideInteractionHelper(context: Context) = InteractionHelper(context)

    @Provides
    @Singleton
    fun provideInflaterHelper(context: Context) = InflaterHelper(context)

    @Provides
    @Singleton
    fun provideResourceHelper(context: Context) = ResourceHelper(context)

    @Provides
    @Singleton
    fun provideNotificationHelper(context: Context) = NotificationHelper(context)

    @Provides
    @Singleton
    fun provideImageLoaderHelper() = ImageLoaderHelper()

    @Provides
    @Singleton
    fun provideQuoteHelper(inflater: InflaterHelper,
                           resource: ResourceHelper,
                           interaction: InteractionHelper,
                           dataBase: DataBaseHelper,
                           activityProvider: ActivityProvider,
                           sharedPref: SharedPrefHelper) = QuoteHelper(inflater, resource, interaction, dataBase, activityProvider, sharedPref)

    @Provides
    @Singleton
    fun provideComicsHelper(imageLoader: ImageLoaderHelper, resource: ResourceHelper) = ComicsHelper(imageLoader, resource)

    @Provides
    @Singleton
    fun provideBillingHelper(context: Context) = BillingHelper(context)

    @Provides
    @Singleton
    fun provideActivity() = ActivityProvider()

    @Provides
    @Singleton
    fun provideRateHelper() = RateHelper()

}