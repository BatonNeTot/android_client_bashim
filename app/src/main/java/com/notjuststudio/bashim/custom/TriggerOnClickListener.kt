package com.notjuststudio.bashim.custom

import android.view.HapticFeedbackConstants
import android.view.View

abstract class TriggerOnClickListener : View.OnClickListener {

    private var trigger = false

    abstract fun triggerOnClick(v: View?)

    override fun onClick(v: View?) {
        v?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

        if (!trigger) {
            trigger = true
            triggerOnClick(v)
        }
    }

    fun consume() {
        trigger = false
    }

}