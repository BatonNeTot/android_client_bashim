package com.notjuststudio.bashim.helper

import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Message
import android.util.Log
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.lang.Exception
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class ImageLoaderHelper() {

    private val picasso = Picasso.get()

    private val executor: Executor = Executors.newCachedThreadPool()

    private class PicassoHandler : Handler() {
        override fun handleMessage(msg: Message) {
            (msg.obj as PicassoCallback).invoke()
        }
    }

    private class PicassoCallback(private val bitmap: Bitmap?, private val onLoad: (Bitmap?) -> Unit) {
        fun invoke() {
            onLoad(bitmap)
        }
    }

    private class PicassoRunnable(private val picasso: Picasso, private val uri: Uri, private val uiHandler: Handler, private val onLoad: (Bitmap?) -> Unit) : Runnable {
        override fun run() {
            uiHandler.sendMessage(uiHandler.obtainMessage(0, PicassoCallback(picasso.load(uri).get(), onLoad)))
        }

    }

    private val uiHandler = PicassoHandler()

    private var onError: ((Uri, Exception) -> Unit)? = { uri, exception -> Log.e("Picasso", "Can't get \"${uri}; Cause: ${exception.message}", exception) }

    fun setOnError(onError: ((Uri, Exception) -> Unit)?) {
        this.onError = onError
    }

    private var errorFlag = false

    fun resetErrorFlag() {
        errorFlag = false
    }

    fun loadImage(path: String, onLoad: (Bitmap?) -> Unit, onFail: () -> Unit = {}) {
        val uri = Uri.parse(path)
        picasso.load(uri).fetch(object : Callback {
            override fun onSuccess() {
                executor.execute(PicassoRunnable(picasso, uri, uiHandler, onLoad))
            }

            override fun onError(e: Exception) {
                onFail()
                if (!errorFlag) {
                    errorFlag = true
                    onError?.invoke(uri, e)
                }
            }

        })
    }

}