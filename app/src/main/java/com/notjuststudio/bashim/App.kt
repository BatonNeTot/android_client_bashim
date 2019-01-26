package com.notjuststudio.bashim

import android.app.Activity
import android.app.Application
import android.os.AsyncTask
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import com.notjuststudio.bashim.dagger.*
import es.dmoral.toasty.Toasty

val Activity.app: App
    get() = application as App

class App : Application() {

    companion object {
        private lateinit var app: App

        fun instance() : App {
            return app
        }

        fun success(text: String) = Toasty.success(app.applicationContext, text).show()
        fun info(text: String) = Toasty.info(app.applicationContext, text).show()
        fun warning(text: String) = Toasty.warning(app.applicationContext, text).show()
        fun error(text: String) = Toasty.error(app.applicationContext, text).show()

        fun success(@StringRes resourceId: Int) = success(app.applicationContext.getString(resourceId))
        fun info(@StringRes resourceId: Int) = info(app.applicationContext.getString(resourceId))
        fun warning(@StringRes resourceId: Int) = warning(app.applicationContext.getString(resourceId))
        fun error(@StringRes resourceId: Int) = error(app.applicationContext.getString(resourceId))
    }

    lateinit var component: AppComponent

    override fun onCreate() {
        super.onCreate()

        component = DaggerAppComponent.builder()
                .appModule(AppModule(this))
                .build()

        val color = ContextCompat.getColor(applicationContext, R.color.toastColor)
        Toasty.Config.getInstance()
                .setSuccessColor(color)
                .setInfoColor(color)
                .setWarningColor(color)
                .setErrorColor(color)
                .apply()

        app = this
    }

}