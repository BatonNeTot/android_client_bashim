package com.notjuststudio.bashim.helper

import com.notjuststudio.bashim.proto.BaseActivity

class ActivityProvider() {

    private val activities : MutableSet<BaseActivity> = mutableSetOf()

    fun onCreate(activity: BaseActivity) {
        activities.add(activity)
    }

    fun onDestroy(activity: BaseActivity) {
        activities.remove(activity)
    }

    fun get() : BaseActivity? {
        return activities.find { true }
    }

}