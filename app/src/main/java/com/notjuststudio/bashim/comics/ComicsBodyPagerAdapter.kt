package com.notjuststudio.bashim.comics

import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import com.notjuststudio.bashim.App
import com.notjuststudio.bashim.R
import com.notjuststudio.bashim.activity.ComicsActivity
import com.notjuststudio.bashim.helper.*
import kotlinx.android.synthetic.main.comics_item.view.*
import javax.inject.Inject

class ComicsBodyPagerAdapter(private val activity: ComicsActivity) : PagerAdapter() {

    inner class ViewHolder(collection: ViewGroup, position: Int) {

        val container: View

        init {
            container = inflaterHelper.inflate(R.layout.comics_item, collection)
            container.id = position
            if (position + 1 == comicsHelper.getComicsCount() && comicsHelper.getYearCount() < comicsHelper.getRefsCount()) {
                comicsHelper.loadYear(comicsHelper.getRef(comicsHelper.getRefsCount() - 1), onDone = {
                    this@ComicsBodyPagerAdapter.notifyDataSetChanged()
                })
            }
            comicsHelper.loadBody(position, {
                comicsUrl, quoteId, authorName, authorUrl ->
                container.backlink.visibility = View.VISIBLE

                container.author.text = authorName
                if (authorUrl != null) {
                    container.author.paintFlags = (container.quoteId.paintFlags or Paint.UNDERLINE_TEXT_FLAG)
                    container.author.setOnClickListener {
                        it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        activity.startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(authorUrl)))
                    }
                } else {
                    container.author.setTextColor(resourceHelper.colorFromAttr(R.attr.textColor))
                }

                if (quoteId != null) {
                    container.quoteId.setText(resourceHelper.string(R.string.comics_bottom_quote, quoteId))
                    container.quoteId.paintFlags = (container.quoteId.paintFlags or Paint.UNDERLINE_TEXT_FLAG)
                    container.quoteId.setOnClickListener{
                        it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        quoteHelper.goToQuote(quoteId, true)
                    }
                } else {
                    container.quoteContainer.visibility = View.GONE
                }

                imageLoaderHelper.loadImage(comicsUrl, {
                    bitmap ->
                    container.comics.setImageBitmap(bitmap)
                    container.loader.visibility = View.GONE

                    container.comicsShare.setImageResource(resourceHelper.drawableIdFromAttr(R.attr.quoteShare))
                    container.comicsShare.setOnClickListener {
                        it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        interactionHelper.share(activity, bitmap!!)
                    }
                    container.comicsShare.visibility = View.VISIBLE
                })
            }, {
                App.error(R.string.download_error)
                activity.finish()
            })
        }

    }

    @Inject
    lateinit var inflaterHelper: InflaterHelper

    @Inject
    lateinit var resourceHelper: ResourceHelper

    @Inject
    lateinit var quoteHelper: QuoteHelper

    @Inject
    lateinit var comicsHelper: ComicsHelper

    @Inject
    lateinit var interactionHelper: InteractionHelper

    @Inject
    lateinit var imageLoaderHelper: ImageLoaderHelper

    init {
        App.instance().component.inject(this)
    }

    override fun instantiateItem(collection: ViewGroup, position: Int): Any {
        val holder = ViewHolder(collection, position)
        (collection as ViewPager).addView(holder.container)
        return holder.container
    }

    override fun destroyItem(collection: ViewGroup, position: Int, view: Any) {
        (collection as ViewPager).removeView(view as View)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean = (view == `object`)

    override fun getCount(): Int = comicsHelper.getComicsCount()

}