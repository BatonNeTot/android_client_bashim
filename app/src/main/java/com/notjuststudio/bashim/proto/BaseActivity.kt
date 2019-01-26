package com.notjuststudio.bashim.proto

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.notjuststudio.bashim.R
import com.notjuststudio.bashim.app
import com.notjuststudio.bashim.helper.ActivityProvider
import com.notjuststudio.bashim.helper.RateHelper
import com.notjuststudio.bashim.helper.SharedPrefHelper
import javax.inject.Inject

abstract class BaseActivity : AppCompatActivity() {

    @Inject
    lateinit var activityProvider: ActivityProvider

    @Inject
    lateinit var sharedPrefHelper: SharedPrefHelper

    @Inject
    lateinit var rateHelper: RateHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        app.component.inject(this)

        setTheme(if (sharedPrefHelper.isDarkTheme()) R.style.GeneralDark_WithStatusBar else R.style.GeneralLight_WithStatusBar)
        app.setTheme(if (sharedPrefHelper.isDarkTheme()) R.style.GeneralDark_WithStatusBar else R.style.GeneralLight_WithStatusBar)

        window.decorView.viewTreeObserver.addOnPreDrawListener {
            var flag = true
            if (!onPostCreatedListeners.isEmpty()) {
                for (listener in onPostCreatedListeners) {
                    listener.invoke()
                }
                onPostCreatedListeners.clear()
                flag = false
            }
            onPostCreatedListeners.addAll(onPostCreatedListenersBuffer)
            onPostCreatedListenersBuffer.clear()
            return@addOnPreDrawListener flag
        }

        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        activityProvider.onCreate(this)
        rateHelper.setupBrowser(this)
    }

    override fun onPause() {
        super.onPause()
        activityProvider.onDestroy(this)
        rateHelper.destroyBrowser()
    }

    private val onPostCreatedListenersBuffer: MutableSet<() -> Unit> = mutableSetOf()
    private val onPostCreatedListeners: MutableSet<() -> Unit> = mutableSetOf()

    fun addSheludePost(listener: () -> Unit) {
        onPostCreatedListenersBuffer.add(listener)
    }

}