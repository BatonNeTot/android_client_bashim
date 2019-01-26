package com.notjuststudio.bashim.common

import android.view.View
import android.view.ViewGroup

fun String.substringBetween(first: String, second: String) : String {
    val start = this.indexOf(first, ignoreCase = true) + first.length
    val end = this.indexOf(second, ignoreCase = true)
    return this.substring(start, end)
}

fun View.replace(view: View) {
    val parent = this.parent as ViewGroup?
    val viewParent = view.parent as ViewGroup?
    if (parent == null)// || parent == viewParent)
        return

    val index = parent.indexOfChild(this)
    parent.removeView(this)

    if (viewParent != null)
        viewParent.removeView(view)
    parent.addView(view, index)
}