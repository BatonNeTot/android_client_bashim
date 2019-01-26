package com.notjuststudio.bashim.helper

import android.content.Context
import android.support.annotation.LayoutRes
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater



class InflaterHelper(private val context: Context) {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    fun inflate(@LayoutRes resourceId: Int, root: ViewGroup?, attachToRoot: Boolean = false): View {
        val view = inflater.inflate(resourceId, root, false)
        if (attachToRoot) {
            root?.addView(view)
        }
        return view
    }

}