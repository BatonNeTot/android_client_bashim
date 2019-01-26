package com.notjuststudio.bashim.helper

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import com.notjuststudio.bashim.activity.MainActivity
import com.notjuststudio.bashim.R
import com.notjuststudio.bashim.activity.SettingsActivity
import com.notjuststudio.bashim.common.Link

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "NOTIFICATION_CHANNEL"
    }

    init {
        createNotificationChannel()
    }

    fun manager() = NotificationManagerCompat.from(context)

    fun builder(channelId: String) = NotificationCompat.Builder(context, channelId)

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.download_channel_name)
            val description = context.getString(R.string.download_channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager!!.createNotificationChannel(channel)
        }
    }

    fun emptyPendingIntent(): PendingIntent {
        return PendingIntent.getActivity(context, 0, Intent(), 0)
    }

    fun settingPendingIntent(): PendingIntent {
        return PendingIntent.getActivity(context, 0, Intent(context, SettingsActivity::class.java), 0)
    }

    fun offlineRandomPendingIntent(): PendingIntent {
        return PendingIntent.getActivity(context, 0,
                Intent(context, MainActivity::class.java)
                        .setAction(MainActivity.ACTION_LINK)
                        .putExtra(MainActivity.ACTION_LINK, Link.RANDOM_OFFLINE.id), 0)
    }

}