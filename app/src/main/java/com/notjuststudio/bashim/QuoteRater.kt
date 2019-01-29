package com.notjuststudio.bashim

import android.os.AsyncTask
import android.util.Log
import com.notjuststudio.bashim.common.Quote
import com.notjuststudio.bashim.common.Rating
import com.notjuststudio.bashim.helper.QuoteHelper
import com.notjuststudio.bashim.helper.RateHelper
import com.squareup.okhttp.MediaType
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.RequestBody
import org.json.JSONException
import org.json.JSONObject
import java.lang.Exception
import java.lang.RuntimeException
import java.net.UnknownHostException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Inject


class QuoteRater {

    companion object {

        private val executor: Executor = Executors.newSingleThreadExecutor()

        private val okHttp = OkHttpClient()

        val MEDIA_TYPE = MediaType.parse("application/json")

        class RateTask(val quote: Quote, val rating: Rating, val afterRated: () -> Unit, val onFailed: () -> Unit) : AsyncTask<Unit, Unit, Boolean>() {

            @Inject
            lateinit var rateHelper: RateHelper

            override fun doInBackground(vararg params: Unit?): Boolean {
                val builder = StringBuilder()

                builder.append("https://bash.im/quote/").append(quote.id).append("/").append(rating.code)

                val postdata = JSONObject()
                try {
                    postdata.put("quote", quote.id)
                    postdata.put("act", rating.code)
                } catch (e: JSONException) {
                    e.printStackTrace()
                    return false
                }


                val body = RequestBody.create(MEDIA_TYPE, postdata.toString())

                val request = Request.Builder()
                        .url(builder.toString())
                        .post(body)
                        .addHeader("Content-Type", "application/json")
                        .build()

                try {
                    val response = okHttp.newCall(request).execute()

                    Log.i("Rating", "Response -> code = ${response.code()}; message = ${response.message()}")
                    return response.code() == 200 && response.message() == "OK"

                } catch (e : UnknownHostException) {
                    return false
                } catch (e : Exception) {
                    return false
                }
            }

            override fun onPostExecute(result: Boolean?) {
                App.instance().component.inject(this)

                if (result == true) {
                    rateHelper.rateQuote(quoteId = quote.id ?: "", rating = rating) {
                        quote.lastRating = rating
                        afterRated()
                        App.success(R.string.quote_rating_success)
                    }
                } else {
                    onFailed()
                    App.error(R.string.quote_rating_error)
                }
            }

        }

        fun rate(quote: Quote, rating: Rating, afterRated: () -> Unit = {}, onFailed: () -> Unit = {}) {
            RateTask(quote, rating, afterRated, onFailed).executeOnExecutor(executor)
        }
    }

}