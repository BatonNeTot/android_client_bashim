package com.notjuststudio.bashim.service

import android.app.Service
import android.content.Intent
import android.net.ParseException
import android.os.*
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationCompat.BADGE_ICON_NONE
import android.support.v4.app.NotificationManagerCompat
import android.text.Html
import android.util.Log
import com.notjuststudio.bashim.App
import com.notjuststudio.bashim.R
import com.notjuststudio.bashim.common.Constants.CONNECTION_TIME_OUT
import com.notjuststudio.bashim.common.Quote
import com.notjuststudio.bashim.helper.DataBaseHelper
import com.notjuststudio.bashim.helper.NotificationHelper
import com.notjuststudio.bashim.helper.ResourceHelper
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Inject

class QuoteSaverService : Service() {

    companion object {

        private val executor: Executor = Executors.newSingleThreadExecutor()

        const val NOTIFICATION_ID = 1
    }

    private var task: SaveTask? = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val count = intent.getIntExtra(QuoteSaverController.EXTRA_COUNT,0)

        task = SaveTask(count)
        task?.executeOnExecutor(executor)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.i("Service", "Destroyed")
    }

    override fun onBind(intent: Intent?): IBinder {
        return QuoteBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        clearOnDoneListeners()
        setOnUpdateListener(null)
        return true
    }

    fun isLoading() : Boolean {
        return task != null && task?.status == AsyncTask.Status.RUNNING
    }

    private val onDoneListeners: MutableList<() -> Unit> = mutableListOf()
    private var onUpdateListener: ((Int) -> Unit)? = null

    fun addOnDoneListener(listener: () -> Unit) = onDoneListeners.add(listener)

    fun clearOnDoneListeners() = onDoneListeners.clear()

    private fun invokeOnDoneListeners() {
        for (listener in onDoneListeners) {
            listener()
        }
        onDoneListeners.clear()
    }

    fun setOnUpdateListener(listener: ((Int) -> Unit)?) {
        onUpdateListener = listener
    }

    inner class QuoteBinder : Binder() {

        fun service() = this@QuoteSaverService

    }

    inner class SaveTask(val count: Int) : AsyncTask<Unit, Int, Unit>() {

        override fun doInBackground(vararg params: Unit?) {
            val url = URL(StringBuilder().append("https://bash.im/random?").append(Random().nextLong().toString()).toString())

            var needCount = count

            try {
                while (needCount > 0) {
                    val doc = Jsoup.parse(url, CONNECTION_TIME_OUT)

                    val quotes = doc.getElementsByAttributeValueMatching("class", "quote")

                    val resultQuotes = mutableListOf<Quote>()

                    var counter = 0
                    val it = quotes.iterator()
                    while (it.hasNext() && counter < needCount) {
                        counter++

                        val quote = it.next() as Element
                        val quotesTextArray = quote.getElementsByAttributeValue("class", "text")
                        if (quotesTextArray.size != 0) {
                            val quoteTextHtml = quotesTextArray.get(0).html()
                            val quoteText: String
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                quoteText = Html.fromHtml(quoteTextHtml, Html.FROM_HTML_MODE_LEGACY).toString()
                            } else {
                                @Suppress("DEPRECATION")
                                quoteText = Html.fromHtml(quoteTextHtml).toString()
                            }
                            val quotesDateArray = quote.getElementsByAttributeValue("class", "date")
                            val quoteDate = quotesDateArray.get(0).text()

                            val quotesIdArray = quote.getElementsByAttributeValue("class", "id")
                            val quoteId = quotesIdArray.get(0).text().replace("#", "")

                            val quotesRatingArray = quote.getElementsByAttributeValue("class", "rating")
                            var rating: String? = null
                            if (quotesRatingArray.size != 0) {
                                rating = quotesRatingArray.get(0).text()
                            }

                            resultQuotes.add(Quote(quoteId, rating, quoteDate, quoteText))
                        }
                    }

                    val loadedCount = dbHelper.addQuotes(resultQuotes)

                    needCount -= loadedCount
                    update(loadedCount)
                    publishProgress(loadedCount)
                }

                postExecute(true)

            } catch (e: RuntimeException) {
                e.printStackTrace()
                postExecute(false)
            } catch (e: UnknownHostException) {
                e.printStackTrace()
                postExecute(false)
            } catch (e: MalformedURLException) {
                e.printStackTrace()
                postExecute(false)
            } catch (e: IOException) {
                e.printStackTrace()
                postExecute(false)
            } catch (e: ParseException) {
                e.printStackTrace()
                postExecute(false)
            }
        }

        lateinit var noteManager: NotificationManagerCompat
        lateinit var noteBuilder: NotificationCompat.Builder
        var progress = 0

        @Inject
        lateinit var dbHelper: DataBaseHelper

        @Inject
        lateinit var resourceHelper: ResourceHelper

        @Inject
        lateinit var notificationHelper: NotificationHelper

        override fun onPreExecute() {
            App.instance().component.inject(this)

            noteManager = notificationHelper.manager()
            noteBuilder = notificationHelper.builder(NotificationHelper.CHANNEL_ID)
                    .setContentTitle(resourceHelper.string(R.string.download_title))
                    .setContentText(resourceHelper.string(R.string.download_text, 0, count))
                    .setBadgeIconType(BADGE_ICON_NONE)
                    .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setContentIntent(notificationHelper.settingPendingIntent())
                    .apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            setSmallIcon(R.drawable.bash_notify)
                        } else {
                            setSmallIcon(R.drawable.bash_old_notify)
                        }
                    }

            noteBuilder.setProgress(count, progress, false)
            startForeground(NOTIFICATION_ID, noteBuilder.build())
        }

        private fun update(vararg values: Int?) {
            progress += values.get(0) ?: 0

            noteBuilder
                    .setProgress(count, progress, false)
                    .setContentText(resourceHelper.string(R.string.download_text, progress, count))
            noteManager.notify(NOTIFICATION_ID, noteBuilder.build())
        }

        private fun postExecute(result: Boolean) {
            if (result == true) {
                noteBuilder.setContentText(resourceHelper.string(R.string.download_complete))
            } else {
                noteBuilder.setContentText(resourceHelper.string(R.string.download_error))
            }

            stopForeground(true)

            noteBuilder
                    .setProgress(0, 0, false)
                    .setAutoCancel(true)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setContentIntent(notificationHelper.offlineRandomPendingIntent())
            noteManager.notify(NOTIFICATION_ID, noteBuilder.build())
        }

        override fun onProgressUpdate(vararg values: Int?) {
            onUpdateListener?.invoke(values.get(0) ?: 0)
        }

        override fun onPostExecute(result: Unit?) {
            invokeOnDoneListeners()
            stopSelf()
        }

    }

}