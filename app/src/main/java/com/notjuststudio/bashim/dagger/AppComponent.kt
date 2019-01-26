package com.notjuststudio.bashim.dagger

import com.notjuststudio.bashim.*
import com.notjuststudio.bashim.activity.ComicsActivity
import com.notjuststudio.bashim.activity.MainActivity
import com.notjuststudio.bashim.activity.SettingsActivity
import com.notjuststudio.bashim.comics.ComicsBodyPagerAdapter
import com.notjuststudio.bashim.comics.ComicsHeaderAdapter
import com.notjuststudio.bashim.comics.ComicsHeaderLoader
import com.notjuststudio.bashim.comics.ComicsHeaderPagerAdapter
import com.notjuststudio.bashim.loader.FavoriteQuotesLoader
import com.notjuststudio.bashim.loader.OfflineQuotesLoader
import com.notjuststudio.bashim.loader.RegularQuoteLoader
import com.notjuststudio.bashim.loader.SingleQuoteLoader
import com.notjuststudio.bashim.proto.BaseActivity
import com.notjuststudio.bashim.service.QuoteSaverService
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component (modules = [AppModule::class, HelperModule::class, QuoteSaverModule::class])
interface AppComponent {

    fun inject(activity: BaseActivity)

    fun inject(activity: MainActivity)
    fun inject(activity: ComicsActivity)
    fun inject(activity: SettingsActivity)

    fun inject(task: QuoteRater.Companion.RateTask)
    fun inject(adapter: QuoteAdapter)
    fun inject(adapter: QuotePagerAdapter)

    fun inject(loader: RegularQuoteLoader)
    fun inject(loader: FavoriteQuotesLoader)
    fun inject(task: OfflineQuotesLoader)

    fun inject(task: SingleQuoteLoader.LoadTask)
    fun inject(task: QuoteSaverService.SaveTask)

    fun inject(loader: ComicsHeaderLoader)
    fun inject(adapter: ComicsHeaderPagerAdapter)
    fun inject(adapter: ComicsHeaderAdapter)
    fun inject(adapter: ComicsBodyPagerAdapter)

}