package com.notjuststudio.bashim.helper

import android.app.Activity

class ActivityProvider() {

    private val activities : MutableSet<Activity> = mutableSetOf()

    fun onCreate(activity: Activity) {
        activities.add(activity)
    }

    fun onDestroy(activity: Activity) {
        activities.remove(activity)
    }

    fun get() : Activity? {
        return activities.find { true }
    }

}