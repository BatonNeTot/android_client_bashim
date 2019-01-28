package com.notjuststudio.bashim.activity

import android.app.Activity
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.view.View
import com.notjuststudio.bashim.R
import com.notjuststudio.bashim.app
import com.notjuststudio.bashim.comics.ComicsBodyPagerAdapter
import com.notjuststudio.bashim.helper.ComicsHelper
import com.notjuststudio.bashim.helper.QuoteHelper
import com.notjuststudio.bashim.proto.BaseActivity
import kotlinx.android.synthetic.main.comics_activity.*
import kotlinx.android.synthetic.main.comics_item.view.*
import javax.inject.Inject

class ComicsActivity : BaseActivity() {

    companion object {

        const val COMICS_ID = "comicsId"

    }

    @Inject
    lateinit var comicsHelper: ComicsHelper

    @Inject
    lateinit var quoteHelper: QuoteHelper

    var index = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        app.component.inject(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.comics_activity)

        index = intent?.extras?.getInt(COMICS_ID, 0) ?: 0

        intent.putExtra(COMICS_ID, index)
        setResult(Activity.RESULT_OK, intent)

        addSheludePost {
            quoteHelper.restoreQuoteDialog(this)
        }

        comicsPager.adapter = ComicsBodyPagerAdapter(this)
        comicsPager.setCurrentItem(index, false)
        comicsPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener{
            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                intent.putExtra(COMICS_ID, position)
                setResult(Activity.RESULT_OK, intent)

                comicsPager.findViewById<View>(position - 1)?.comics?.resetZoom()
                comicsPager.findViewById<View>(position + 1)?.comics?.resetZoom()
            }

        })
    }

}