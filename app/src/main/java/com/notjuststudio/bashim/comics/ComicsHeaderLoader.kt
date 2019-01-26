package com.notjuststudio.bashim.comics

import android.net.ParseException
import android.os.AsyncTask
import com.notjuststudio.bashim.common.*
import com.notjuststudio.bashim.common.Constants.CONNECTION_TIME_OUT
import org.jsoup.Jsoup
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.net.UnknownHostException
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class ComicsHeaderLoader {

    private lateinit var task: LoadTask

    companion object {
        private val executor: Executor = Executors.newSingleThreadExecutor()

        const val DEFAULT_REF = "https://bash.im/comics-calendar"

        private const val LEFT_LINK_STRING = "←"

        private const val YEAR_LEFT = "Комиксы за "
        private const val YEAR_RIGHT = " год"
    }

    fun loadHeaders(ref: String, onLoaded: (ComicsYear, String?) -> Unit = {_,_->},
                   onFailed: () -> Unit = {}) {
        task = LoadTask(ref, onLoaded, onFailed)
        task.executeOnExecutor(executor)
    }

    private inner class LoadTask(private val ref: String, private val onLoaded: (ComicsYear, String?) -> Unit, private val onFailed: () -> Unit) : AsyncTask<Unit, Unit, Boolean>() {

        lateinit var yearUnit: ComicsYear

        var nextRef: String? = null

        override fun doInBackground(vararg params: Unit?): Boolean {

            val url = URL(ref)

            try {
                val doc = Jsoup.parse(url, CONNECTION_TIME_OUT)

                val body = doc.getElementById("body")
                val h2 = body.getElementsByTag("h2")[0]

                val yearName = h2.text().substringBetween(YEAR_LEFT, YEAR_RIGHT)

                val ref = h2.getElementsByClass("arr")[0]
                if (ref.text() == LEFT_LINK_STRING) {
                    nextRef = "https://bash.im${ref.attr("href")}"
                } else {
                    nextRef = null
                }

                val headersBlocks = body.getElementById("calendar").allElements

                val yearHeaders = mutableListOf<ComicsMonth>()
                var monthName = ""
                val monthHeaders = mutableListOf<ComicsUnit>()

                for (html in headersBlocks) {
                    when (html.tagName()) {
                        "h3" -> {
                            monthName = html.text()
                        }
                        "a" -> {
                            val comicsId = html.attr("href")
                            val headerUrl = html.allElements[1].attr("src")
                            monthHeaders.add(0, ComicsUnit(ComicsHeader(comicsId, headerUrl)))
                        }
                        "div" -> {
                            if (html.className() == "clear" && monthName != "") {
                                yearHeaders.add(0, ComicsMonth(monthName, monthHeaders.toList()))
                            }
                            monthHeaders.clear()
                        }
                    }
                }

                yearUnit = ComicsYear(yearName, yearHeaders.toList())

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
                onLoaded.invoke(yearUnit, nextRef)
            } else {
                onFailed.invoke()
            }
        }
    }
}