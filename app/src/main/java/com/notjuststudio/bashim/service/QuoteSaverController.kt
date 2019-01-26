package com.notjuststudio.bashim.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder

class QuoteSaverController(private val context: Context) {

    companion object {
        const val EXTRA_COUNT = "count"

        const val START_LOADING = "START_LOADING"
    }

    private var binder: QuoteSaverService.QuoteBinder? = null
    private var connection = Connection()

    private var onBind: (() -> Unit)? = null

    fun startLoading(count: Int) {
        val service = Intent(context, QuoteSaverService::class.java)
                .setAction(START_LOADING)
                .putExtra(EXTRA_COUNT, count)
        context.startService(service)

        if (binder != null) {
            invokeOnBind()
        }
    }

    fun setOnConnected(listener: (() -> Unit)?) {
        if (binder != null) {
            listener?.invoke()
        } else {
            onBind = listener
        }
    }

    fun isLoading() : Boolean {
        return binder != null && binder?.service()?.isLoading()?: false
    }

    fun addOnDoneListener(listener: () -> Unit) {
        binder?.service()?.addOnDoneListener(listener)
    }

    fun clearOnDoneListeners() {
        binder?.service()?.clearOnDoneListeners()
    }

    fun setOnUpdateListener(listener: ((Int) -> Unit)?) {
        binder?.service()?.setOnUpdateListener(listener)
    }

    fun connectToService() {
        context.bindService(Intent(context, QuoteSaverService::class.java), connection, 0)
    }

    fun disconnectFromService() {
        val tmpBinder = binder
        binder = null
        if (tmpBinder != null)
            context.unbindService(connection)
    }

    private fun invokeOnBind() {
        onBind?.invoke()
        onBind = null
    }

    inner class Connection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            val flag = this@QuoteSaverController.binder == null

            this@QuoteSaverController.binder = binder as QuoteSaverService.QuoteBinder
            if (flag)
                invokeOnBind()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            this@QuoteSaverController.binder = null
        }
    }

}