package com.notjuststudio.bashim.helper

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.util.Log
import android.webkit.*
import com.notjuststudio.bashim.R
import com.notjuststudio.bashim.common.Rating

class RateHelper {

    companion object {

        private const val URL_FOR_JS = "https://bash.im/quote/1"

    }

    private var browser: WebView? = null
    private var wasLoaded = false
    private var lastRateQuery: (() -> Unit)? = null

    fun setupBrowser(activity: Activity) {
        browser = activity.findViewById(R.id.browser)

        if (browser != null) {
            @SuppressLint("SetJavaScriptEnabled")
            browser?.settings?.javaScriptEnabled = true
            browser?.webViewClient = object : WebViewClient() {

                private var wasError = false
                private var currentUrl: String? = null

                override fun onPageFinished(view: WebView?, url: String?) {
//                    Log.i("WebView", "Finished $url")
                    if (wasError) {
                        wasError = false
                    } else {
                        wasLoaded = true
                        if (lastRateQuery != null) {
                            lastRateQuery?.invoke()
                            lastRateQuery = null
                        }
                    }
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    currentUrl = url
//                    Log.i("WebView", "Started $url")
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
//                    Log.i("WebView", "Error: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) error?.description else error.toString()}; " +
//                            "Request: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) request?.url else request.toString()}")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        if (request?.url?.toString() == currentUrl)
                            wasError = true
                }

                override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
//                    Log.i("WebView", "ErrorResponse: $errorResponse; Request: $request")
                }

                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
//                    Log.i("WebView", "ErrorSsl: $error; Handler: $handler")
                }
            }
            browser?.webChromeClient = object : WebChromeClient() {
                override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
//                    Log.i("JS", "Alert")
                    return true
                }

                override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
//                    Log.i("JS", "Confirm")
                    return true
                }

                override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult?): Boolean {
//                    Log.i("JS", "Prompt")
                    return true
                }
            }
            browser?.loadUrl(URL_FOR_JS)
        }
    }

    fun destroyBrowser() {
        browser = null
        wasLoaded = false
    }

    private fun execute(script: String) {
        if (browser != null && wasLoaded) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                browser?.evaluateJavascript(script, null)
            } else {
                browser?.loadUrl("javascript:$script")
            }
        }
    }

    fun rateQuote(quoteId: String, rating: Rating, isAbyss: Boolean = false, onDone: () -> Unit = {}) {
        if (browser != null) {
            val rate = {
                execute("v('$quoteId', ${rating.id}, ${if (isAbyss) 1 else 0})")
                onDone()
            }
            if (wasLoaded) {
                rate.invoke()
            } else {
                lastRateQuery = rate
                browser?.loadUrl(URL_FOR_JS)
            }
        }
    }

}