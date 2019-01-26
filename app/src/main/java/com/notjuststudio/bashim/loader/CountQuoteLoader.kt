package com.notjuststudio.bashim.loader

import android.net.ParseException
import android.os.AsyncTask
import com.notjuststudio.bashim.common.Constants.CONNECTION_TIME_OUT
import org.jsoup.Jsoup
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.net.UnknownHostException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CountQuoteLoader {

    companion object {
        private val executor: ExecutorService = Executors.newCachedThreadPool()

        fun loadQuote(onLoaded: (Int) -> Unit = {},
                      onFailed: () -> Unit = {}) {
            LoadTask(onLoaded, onFailed).executeOnExecutor(executor)
        }
    }

    private class LoadTask(private val onLoaded: (Int) -> Unit, private val onFailed: () -> Unit) : AsyncTask<Unit, Unit, Boolean>() {

        var count = 0

        override fun doInBackground(vararg params: Unit?): Boolean {
            val builder = StringBuilder()
            builder.append("https://bash.im")

            val url = URL("https://bash.im")

            try {
                val doc = Jsoup.parse(url, CONNECTION_TIME_OUT)

                val quote = doc.getElementsByAttributeValueMatching("class", "quote").get(0)

                val quotesIdArray = quote.getElementsByAttributeValue("class", "id")
                count = quotesIdArray.get(0).text().replace("#", "").toInt()

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
                onLoaded.invoke(count)
            } else {
                onFailed.invoke()
            }
        }
    }
}