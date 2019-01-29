package com.notjuststudio.bashim.loader

import android.net.ParseException
import android.os.Build
import android.text.Html
import android.util.Log
import com.notjuststudio.bashim.App
import com.notjuststudio.bashim.common.Constants.CONNECTION_TIME_OUT
import com.notjuststudio.bashim.common.Link
import com.notjuststudio.bashim.common.Quote
import com.notjuststudio.bashim.helper.DataBaseHelper
import com.notjuststudio.bashim.proto.BaseLoadTask
import com.notjuststudio.bashim.proto.BaseLoader
import java.net.URL
import java.util.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.IOException
import java.net.MalformedURLException
import java.net.UnknownHostException
import javax.inject.Inject


class RegularQuoteLoader(val link: Link, val defaultData: String = "") : BaseLoader() {

    var nextData: String? = defaultData

    @Inject
    lateinit var dbHelper: DataBaseHelper

    init {
        App.instance().component.inject(this)

        taskFactory = LoadTaskFactory(this)
    }

    fun hasNext(): Boolean {
        return nextData != null
    }

    override fun reset() {
        nextData = defaultData
    }

    private class LoadTaskFactory(val loader: RegularQuoteLoader) : BaseLoadTask.Factory {
        override fun create(onLoaded: (List<Quote>) -> Unit,
                            onFirstLoaded: () -> Unit,
                            onDoneLoading: () -> Unit,
                            onFailed: () -> Unit,
                            onNothingToLoad: () -> Unit): BaseLoadTask {
            return LoadTask(
                    loader,
                    onLoaded,
                    onFirstLoaded,
                    onDoneLoading,
                    onFailed,
                    onNothingToLoad)
        }
    }

    private class LoadTask(private val loader: RegularQuoteLoader,
                           onLoaded: (List<Quote>) -> Unit,
                           onFirstLoaded: () -> Unit,
                           onDoneLoading: () -> Unit,
                           onFailed: () -> Unit,
                           onNothingToLoad: () -> Unit) : BaseLoadTask(onLoaded, onFirstLoaded, onDoneLoading, onFailed, onNothingToLoad) {

        val quotes : MutableList<Quote> = mutableListOf()

        override fun doInBackground(vararg params: Unit?): Boolean {
            if (loader.nextData == null) {
                return true
            }

            val builder = StringBuilder()
            builder.append("https://bash.im")

            builder.append(when (loader.link) {
                Link.RANDOM_ONLINE -> "/random"

                Link.BEST_TODAY -> "/best"
                Link.BEST_MONTH -> "/bestmonth"
                Link.BEST_YEAR -> "/bestyear"
                Link.BEST_ALL -> "/byrating"

                Link.ABYSS_NEW -> "/abyss"
                Link.ABYSS_TOP -> "/abysstop"
                Link.ABYSS_BEST -> "/abyssbest"

                else -> ""
            })

            val urlString = when (loader.link) {
                Link.NEW -> if (loader.nextData?.isEmpty() != false) builder.toString() else builder.append("/index/").append(loader.nextData).toString()
                Link.BEST_ALL -> if (loader.nextData?.isEmpty() != false) builder.toString() else builder.append("/").append(loader.nextData).toString()

                Link.RANDOM_ONLINE,
                Link.ABYSS_NEW -> builder.append("?").append(Random().nextLong()).toString()

                Link.BEST_TODAY,
                Link.ABYSS_TOP -> builder.toString()

                Link.BEST_MONTH,
                Link.BEST_YEAR,
                Link.ABYSS_BEST -> builder.append("/").append(loader.nextData).toString()

                Link.SEARCH -> builder.append("/index?text=").append(loader.nextData).toString()

                else -> builder.toString()
            }

//            Log.i("URL", "url=$urlString")

            val url = URL(urlString)

            try {
                val doc = Jsoup.parse(url, CONNECTION_TIME_OUT)

                val index: Int
                loader.nextData = when (loader.link) {
                    Link.NEW -> {
                        index = doc.getElementsByClass("pager")[0].getElementsByClass("current")[0].getElementsByClass("page")[0].attr("value").toInt()
                        if (index == 1) {
                            null
                        } else {
                            (index - 1).toString()
                        }
                    }
                    Link.BEST_ALL -> {
                        val page = doc.getElementsByClass("pager")[0].getElementsByClass("current")[0].getElementsByClass("page")[0]
                        index = page.attr("value").toInt()
                        val max = page.attr("max").toInt()
                        if (max <= index) {
                            null
                        } else {
                            (index + 1).toString()
                        }
                    }
                    else -> {
                        null
                    }

                }

                val quotes = doc.getElementsByAttributeValueMatching("class", "quote")

                val it = quotes.iterator()
                while (it.hasNext()) {
                    val quote = it.next() as Element
                    val quotesTextArray = quote.getElementsByAttributeValue("class", "text")
                    if (quotesTextArray.size != 0) {
                        val quoteId: String?
                        val quoteTextHtml = quotesTextArray.get(0).html()
                        val quoteText: String
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            quoteText = Html.fromHtml(quoteTextHtml, Html.FROM_HTML_MODE_LEGACY).toString()
                        } else {
                            @Suppress("DEPRECATION")
                            quoteText = Html.fromHtml(quoteTextHtml).toString()
                        }
                        val quotesDateArray = quote.getElementsByAttributeValue("class", "date")
                        val quotesAbyssTopDateArray = quote.getElementsByAttributeValue("class", "abysstop-date")
                        var quoteDate = ""
                        if (!quotesDateArray.isEmpty()) {
                            quoteDate = quotesDateArray.get(0).text()
                        } else if (!quotesAbyssTopDateArray.isEmpty()) {
                            quoteDate = quotesAbyssTopDateArray.get(0).text()
                        }
                        val quotesIdArray = quote.getElementsByAttributeValue("class", "id")
                        if (quotesIdArray.size != 0) {
                            quoteId = quotesIdArray.get(0).text().replace("#", "")
                        } else {
                            quoteId = null
                        }
                        val quotesRatingArray = quote.getElementsByAttributeValue("class", "rating")
                        var rating: String? = null
                        if (quotesRatingArray.size != 0) {
                            rating = quotesRatingArray.get(0).text()
                        }
                        val quotesComicsArray = quote.getElementsByAttributeValue("class", "comics")
                        var comics: String? = null
                        if (quotesComicsArray.size != 0) {
                            comics = quotesComicsArray.get(0).attr("href")
                        }
                        val fav: Boolean
                        if (!(loader.link == Link.ABYSS_NEW || loader.link == Link.ABYSS_TOP || loader.link == Link.ABYSS_BEST)) {
                            fav = loader.dbHelper.isFavorite(quoteId?.toInt() ?: 0)
                        } else {
                            fav = false
                        }

                        this.quotes.add(Quote(quoteId, rating, quoteDate, quoteText, fav, comics))
                    }
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
                onFirstLoaded.invoke()
                if (quotes.size <= 0) {
                    onNothingToLoad.invoke()
                } else {
                    onLoaded.invoke(quotes)
                }
                onDoneLoading.invoke()
            } else {
                onDoneLoading.invoke()
                onFailed.invoke()
            }
        }
    }
}