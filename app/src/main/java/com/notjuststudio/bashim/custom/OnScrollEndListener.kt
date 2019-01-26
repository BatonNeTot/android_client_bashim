package com.notjuststudio.bashim.custom

import android.support.v7.widget.RecyclerView

abstract class OnScrollEndListener : RecyclerView.OnScrollListener() {

    abstract fun onScrollEnd(recyclerView: RecyclerView)

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        if (!recyclerView.canScrollVertically(1)) {
            onScrollEnd(recyclerView)
        }
    }

}