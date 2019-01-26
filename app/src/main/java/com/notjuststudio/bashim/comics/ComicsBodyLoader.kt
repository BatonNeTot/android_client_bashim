package com.notjuststudio.bashim.comics

import android.net.ParseException
import android.os.AsyncTask
import com.notjuststudio.bashim.App
import com.notjuststudio.bashim.common.Constants
import com.notjuststudio.bashim.common.Constants.CONNECTION_TIME_OUT
import com.notjuststudio.bashim.common.substringBetween
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.net.UnknownHostException
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ComicsBodyLoader {

    companion object {
        private val executor: ExecutorService = Executors.newCachedThreadPool()

        fun loadComics(comicsLocation: String,
                       onLoaded: (String, String?, String, String?) -> Unit = {_,_,_,_->},
                       onFailed: () -> Unit = {}) {
            LoadTask(comicsLocation, onLoaded, onFailed).executeOnExecutor(executor)
        }
    }

    class LoadTask(private val comicsLocation: String, private val onLoaded: (String, String?, String, String?) -> Unit, private val onFailed: () -> Unit) : AsyncTask<Unit, Unit, Boolean>() {

        lateinit var comics : String

        var quoteId: String? = null

        lateinit var authorName: String
        var authorUrl: String? = null

        override fun doInBackground(vararg params: Unit?): Boolean {

            val url = URL("https://bash.im$comicsLocation")

            try {
                lateinit var doc: Document
                while (true) {
                    try {
                        doc = Jsoup.parse(url, CONNECTION_TIME_OUT)
                    } catch (e: HttpStatusException) {
                        if (e.statusCode == 503)
                            continue

                        e.printStackTrace()
                        return false
                    }
                    break
                }

                comics = doc.getElementById("cm_strip").attr("src")

                val backlink = doc.getElementById("boiler").getElementsByClass("backlink").get(0)
                val refs = backlink.getElementsByTag("a")

                if (refs.size == 1) {
                    val rawId = refs[0].text()
                    val index = rawId.lastIndexOf("#")
                    if (index != -1) {
                        quoteId = rawId.substring(index + 1)

                        authorName = backlink.text().substringBetween("нарисовал ", " по мотивам")
                    } else {

                        authorName = rawId
                        authorUrl = refs[0].attr("href")
                    }
                } else {
                    authorName = refs[0].text()
                    authorUrl = refs[0].attr("href")

                    val rawId = refs[1].text()
                    quoteId = rawId.substring(rawId.lastIndexOf("#") + 1)
                }

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
                onLoaded.invoke(comics, quoteId, authorName, authorUrl)
            } else {
                onFailed.invoke()
            }
        }
    }
}