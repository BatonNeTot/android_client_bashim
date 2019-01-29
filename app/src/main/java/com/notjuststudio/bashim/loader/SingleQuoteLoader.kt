package com.notjuststudio.bashim.loader

import android.net.ParseException
import android.os.AsyncTask
import android.os.Build
import android.text.Html
import com.notjuststudio.bashim.App
import com.notjuststudio.bashim.common.Constants.CONNECTION_TIME_OUT
import com.notjuststudio.bashim.common.Quote
import com.notjuststudio.bashim.helper.DataBaseHelper
import org.jsoup.Jsoup
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.net.UnknownHostException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Inject

class SingleQuoteLoader {

    companion object {

        private const val THREAD_COUNT = 10

        private val executor: Executor = Executors.newFixedThreadPool(THREAD_COUNT)

        fun loadQuote(id: String,
                      onLoaded: (Quote) -> Unit = {},
                      onFailed: () -> Unit = {}) {
            LoadTask(id, onLoaded, onFailed).executeOnExecutor(executor)
        }

    }

    class LoadTask(val id: String, private val onLoaded: (Quote) -> Unit, private val onFailed: () -> Unit) : AsyncTask<Unit, Unit, Boolean>() {

        lateinit var quote: Quote

        @Inject
        lateinit var dbHelper: DataBaseHelper

        override fun onPreExecute() {
            App.instance().component.inject(this)
        }

        override fun doInBackground(vararg params: Unit?): Boolean {
            val url = URL(StringBuilder().append("https://bash.im/quote/").append(id).toString())

            try {
                val doc = Jsoup.parse(url, CONNECTION_TIME_OUT)

                val quoteElement = doc.getElementsByAttributeValueMatching("class", "quote").get(0)

                val quotesTextArray = quoteElement.getElementsByAttributeValue("class", "text")

                val quoteTextHtml = quotesTextArray.get(0).html()
                val quoteText: String
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    quoteText = Html.fromHtml(quoteTextHtml, Html.FROM_HTML_MODE_LEGACY).toString()
                } else {
                    @Suppress("DEPRECATION")
                    quoteText = Html.fromHtml(quoteTextHtml).toString()
                }
                val quotesDateArray = quoteElement.getElementsByAttributeValue("class", "date")
                val quoteDate = quotesDateArray.get(0).text()

                val quotesIdArray = quoteElement.getElementsByAttributeValue("class", "id")
                val quoteId = quotesIdArray.get(0).text().replace("#", "")

                val quotesRatingArray = quoteElement.getElementsByAttributeValue("class", "rating")
                var rating: String? = null
                if (quotesRatingArray.size != 0) {
                    rating = quotesRatingArray.get(0).text()
                }
                val quotesComicsArray = quoteElement.getElementsByAttributeValue("class", "comics")
                var comics: String? = null
                if (quotesComicsArray.size != 0) {
                    comics = quotesComicsArray.get(0).attr("href")
                }

                val fav = dbHelper.isFavorite(quoteId.toInt())

                quote = Quote(quoteId, rating, quoteDate, quoteText, fav, comics)


                return true

            } catch (e: RuntimeException) {
                e.printStackTrace()
                return false
            } catch (e: UnknownHostException) {
                e.printStackTrace()
                return false
            } catch (e: MalformedURLException) {
                e.printStackTrace()
                return false
            } catch (e: IOException) {
                e.printStackTrace()
                return false
            } catch (e: ParseException) {
                e.printStackTrace()
                return false
            }
        }

        override fun onPostExecute(result: Boolean?) {
            if (result == true) {
                onLoaded.invoke(quote)
            } else {
                onFailed.invoke()
            }
        }

    }

}