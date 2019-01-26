package android.support.v4.view

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.notjuststudio.bashim.App

class PagerDateStrip(context: Context, attrs: AttributeSet?) : PagerTabStrip(context, attrs) {
    constructor(context: Context) : this(context, null)

    private var onTabListener: (() -> Unit)? = null

    init{
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O)
            mCurrText.focusable = View.FOCUSABLE

        mCurrText.setOnClickListener {
            onTabListener?.invoke()
        }
    }

    fun setOnTabListener(listener: (() -> Unit)?) {
        onTabListener = listener
    }

    fun getOnTabListener() : (() -> Unit)? {
        return onTabListener
    }

    override fun setTabIndicatorColor(color: Int) {
        super.setTabIndicatorColor(color)
        this.mNextText.setTextColor(color)
        this.mCurrText.setTextColor(color)
        this.mPrevText.setTextColor(color)
    }
}