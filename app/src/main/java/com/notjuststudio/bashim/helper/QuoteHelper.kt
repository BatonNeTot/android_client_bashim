package com.notjuststudio.bashim.helper

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.ImageView
import com.notjuststudio.bashim.App
import com.notjuststudio.bashim.QuoteRater
import com.notjuststudio.bashim.R
import com.notjuststudio.bashim.activity.ComicsActivity
import com.notjuststudio.bashim.activity.MainActivity
import com.notjuststudio.bashim.loader.SingleQuoteLoader
import com.notjuststudio.bashim.common.*
import com.notjuststudio.bashim.loader.CountQuoteLoader
import kotlinx.android.synthetic.main.date_dialog_layout.view.*
import kotlinx.android.synthetic.main.header_bottom.view.*
import kotlinx.android.synthetic.main.quote_body.view.*
import kotlinx.android.synthetic.main.quote_header.view.*
import kotlinx.android.synthetic.main.quote_layout.view.*
import kotlinx.android.synthetic.main.search_dialog_layout.view.*
import java.net.URLEncoder
import java.util.*

class QuoteHelper(private val inflaterHelper: InflaterHelper,
                  private val resourceHelper: ResourceHelper,
                  private val interactionHelper: InteractionHelper,
                  private val dbHelper: DataBaseHelper,
                  private val activityProvider: ActivityProvider,
                  private val sharedPrefHelper: SharedPrefHelper) {private val months: Array<String> = resourceHelper.stringArray(R.array.date_month).asList().toTypedArray()

    companion object {
        const val FIRST_YEAR = 2004
        const val FIRST_MONTH = Calendar.AUGUST
    }

    private var lastDialog: AlertDialog? = null

    fun dateDialog(lastDay: Calendar, currentDate: Calendar, firstDay: Calendar, dataType: Int, onTrue: (Int) -> Unit) {
        @SuppressLint("InflateParams")
        val root = inflaterHelper.inflate(R.layout.date_dialog_layout, null)

        lastDialog = AlertDialog.Builder(activityProvider.get(), R.style.Dialog)
                .setView(root)
                .setCancelable(true)
                .setPositiveButton(R.string.date_picker_ok){ dialog, _->
                    when(dataType) {
                        Calendar.YEAR -> {
                            onTrue(calculatePosition(lastDay,
                                    root.yearPicker.value))
                        }
                        Calendar.MONTH -> {
                            onTrue(calculatePosition(lastDay,
                                    root.yearPicker.value,
                                    root.monthPicker.value))
                        }
                        Calendar.DAY_OF_MONTH -> {
                            onTrue(calculatePosition(lastDay,
                                    root.yearPicker.value,
                                    root.monthPicker.value,
                                    root.dayPicker.value))
                        }
                    }
                    lastDialog = null
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.date_picker_cancel){ dialog, _->
                    lastDialog = null
                    dialog.dismiss()
                }
                .setOnCancelListener{
                    lastDialog = null
                }
                .setTitle(R.string.date_picker_title)
                .create()

        val setupDay = {
            day: Int ->
            root.dayPicker.value = day
        }

        val setupMonth = {
            month: Int ->
            root.monthPicker.value = month

            if (dataType != Calendar.MONTH) {
                root.dayPicker.apply {
                    val prev = value
                    val year = root.yearPicker.value

                    if (year == firstDay.get(Calendar.YEAR) && month == firstDay.get(Calendar.MONTH)) {
                        minValue = firstDay.get(Calendar.DAY_OF_MONTH)
                        maxValue = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH)
                    } else if (year == lastDay.get(Calendar.YEAR) && month == lastDay.get(Calendar.MONTH)) {
                        minValue = 1
                        maxValue = lastDay.get(Calendar.DAY_OF_MONTH)
                    } else {
                        val cal = Calendar.getInstance()
                        cal.set(Calendar.MONTH, month)
                        minValue = 1
                        maxValue = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    }
                    setupDay(Math.max(minValue, Math.min(maxValue, prev)))
                }
            }
        }

        val setupYear = {
            year: Int ->
            root.yearPicker.value = year


            if (dataType != Calendar.YEAR) {
                root.monthPicker.apply {
                    val prev = value
                    when (year) {
                        firstDay.get(Calendar.YEAR) -> {
                            displayedValues = months
                            minValue = firstDay.get(Calendar.MONTH)
                            maxValue = 11
                            value = Math.max(minValue, Math.min(maxValue, value))
                            displayedValues = months.copyOfRange(firstDay.get(Calendar.MONTH), 12)
                        }
                        lastDay.get(Calendar.YEAR) -> {
                            displayedValues = months
                            minValue = 0
                            maxValue = lastDay.get(Calendar.MONTH)
                            value = Math.max(minValue, Math.min(maxValue, value))
                            displayedValues = months.copyOfRange(0, lastDay.get(Calendar.MONTH) + 1)
                        }
                        else -> {
                            displayedValues = months
                            minValue = 0
                            maxValue = 11
                            value = Math.max(minValue, Math.min(maxValue, value))
                        }
                    }
                    setupMonth(Math.max(minValue, Math.min(maxValue, prev)))
                }
            }
        }

        root.yearPicker.apply {
            minValue = firstDay.get(Calendar.YEAR)
            maxValue = lastDay.get(Calendar.YEAR)
        }
        root.monthPicker.apply {
            minValue = firstDay.get(Calendar.MONTH)
            maxValue = lastDay.get(Calendar.MONTH)
        }
        root.dayPicker.apply {
            minValue = firstDay.get(Calendar.DAY_OF_MONTH)
            maxValue = lastDay.get(Calendar.DAY_OF_MONTH)
        }

        when(dataType) {
            Calendar.YEAR -> {
                root.monthPicker.visibility = View.GONE
                root.dayPicker.visibility = View.GONE
            }
            Calendar.MONTH -> {
                root.yearPicker.setOnValueChangedListener { _, _, value ->
                    setupYear(value)
                }
                root.monthPicker.setOnValueChangedListener { _, _, value ->
                    setupMonth(value)
                }
                root.dayPicker.visibility = View.GONE
            }
            Calendar.DAY_OF_MONTH -> {
                root.yearPicker.setOnValueChangedListener { _, _, value ->
                    setupYear(value)
                }
                root.monthPicker.setOnValueChangedListener { _, _, value ->
                    setupMonth(value)
                }
                root.dayPicker.setOnValueChangedListener { _, _, value ->
                    setupDay(value)
                }
            }
        }

        setupYear(currentDate.get(Calendar.YEAR))
        setupMonth(currentDate.get(Calendar.MONTH))
        setupDay(currentDate.get(Calendar.DAY_OF_MONTH))

        lastDialog?.show()
    }

    fun searchQuoteDialog(activity: MainActivity) {
        @SuppressLint("InflateParams")
        val root = inflaterHelper.inflate(R.layout.search_dialog_layout, null)

        lastDialog = AlertDialog.Builder(activity, R.style.Dialog)
                .setView(root)
                .setCancelable(true)
                .setPositiveButton(R.string.link_search_ok){ dialog, _->
                    lastDialog = null
                    dialog.dismiss()

                    val searchText = root.query.text
                    val searchKeys = searchText?.split(" ")?.filter { it.isNotEmpty() }

                    if (searchKeys != null && searchKeys.isNotEmpty()) {
                        val search = {
                            discardCache()

                            val title = searchKeys.map{
                                resourceHelper.string(R.string.link_search, it)
                            }.joinToString(", ")
                            val query = URLEncoder.encode(searchKeys.joinToString("+"), "cp1251")

                            setupTitleName(title)
                            setupLoaderData(query)

                            activity.loadQuotes(Link.SEARCH)
                        }

                        val quoteId = searchKeys.get(0).toIntOrNull()

                        if (searchKeys.size == 1 && quoteId != null) {
                            goToQuote(quoteId.toString(), mayBeWrongId = true, onWrongId = search, onNonexistentId = search)
                        } else {
                            search()
                        }
                    }
                }
                .setNegativeButton(R.string.link_search_cancel){ dialog, _->
                    lastDialog = null
                    dialog.dismiss()
                }
                .setOnCancelListener{
                    lastDialog = null
                }
                .setTitle(R.string.link_search_title)
                .create()

        lastDialog?.show()
    }

    private var lastDialogId: String? = null
    private var lastActivityName: String = ""

    fun restoreQuoteDialog(activity: Activity) {
        if (lastActivityName == activity.localClassName && lastDialogId != null) {
            goToQuote(lastDialogId!!)
        } else {
            lastDialogId = null
            lastActivityName = ""
        }
    }

    fun goToQuote(id: String, mayBeWrongId: Boolean = false,
                  onWrongId: () -> Unit = {}, onNonexistentId: () -> Unit = {}) {
        if (lastDialog != null) {
            try {
                lastDialog?.dismiss()
            } catch (e: Exception) {}
        }

        val activity = activityProvider.get()

        lastDialogId = id
        lastActivityName = activity?.localClassName ?: ""
        @SuppressLint("InflateParams")
        val root = inflaterHelper.inflate(R.layout.quote_layout, null)

        val clearDialog = {
            lastDialog = null
            lastDialogId = null
            lastActivityName = ""
        }


        lastDialog = AlertDialog.Builder(activity, R.style.Dialog)
                .setView(root)
                .setCancelable(true)
                .setOnCancelListener {
                    clearDialog()
                }
                .create()

        val closeDialog = {
            lastDialog?.dismiss()
            clearDialog()
        }

        val onError = {
            App.error(R.string.quotes_load_error)
            closeDialog()
        }

        val showQuote = {
            Log.i("QuoteDialog", "Quote showed = $id")
            SingleQuoteLoader.loadQuote(id, onLoaded = {
                if (id != it.id) {
                    onNonexistentId()
                    closeDialog()
                    return@loadQuote
                }
                setupQuote(0, QuoteType.SINGLE, root.container, it)
                root.loading.visibility = View.GONE
                root.container.visibility = View.VISIBLE
            }, onFailed = onError)
        }

        if (!mayBeWrongId) {
            lastDialog?.show()
            showQuote()
        } else {
            val intId = id.toInt()
            if (intId < 1) {
                onWrongId()
                return
            }

            lastDialog?.show()
            CountQuoteLoader.loadQuote(onLoaded = {
                Log.i("QuoteDialog", "Quote count = $it")
                Log.i("QuoteDialog", "Quote current = $intId or $id")
                if (intId > it) {
                    onWrongId()
                    closeDialog()
                } else {
                    showQuote()
                }
            }, onFailed = onError)
        }

    }

    fun goToComics(index: Int) {
        val activity = activityProvider.get()

        val intent = Intent(activity, ComicsActivity::class.java)
        intent.putExtra(ComicsActivity.COMICS_ID, index)
        activity?.startActivityForResult(intent, MainActivity.COMICS)
    }

    private inner class Rate(val container: View, val quote: Quote, val rating: com.notjuststudio.bashim.common.Rating) : () -> Unit {
        override fun invoke() {
            var isNumber = true

            try {
                quote.value?.toInt()
            } catch(e: NumberFormatException) {
                isNumber = false
            }

            if (quote.value != null && isNumber) {
                quote.value = (quote.value!!.toInt() + when(rating){
                    Rating.RULEZ -> 1
                    Rating.BAYAN -> 0
                    Rating.SUX -> -1
                }).toString()
                container.quoteRatingValue.text = quote.value
            } else {
                quote.value = resourceHelper.string(when(rating){
                    Rating.RULEZ -> R.string.quote_rating_unknown_plus
                    Rating.BAYAN,
                    Rating.SUX -> R.string.quote_rating_unknown_minus
                })
                container.quoteRatingValue.text = quote.value
            }

            if (rating != Rating.RULEZ) {
                container.quoteRatingPlus.setImageResource(resourceHelper.drawableIdFromAttr(R.attr.ratingPlusUnable))
            }
            if (rating != Rating.SUX) {
                container.quoteRatingMinus.setImageResource(resourceHelper.drawableIdFromAttr(R.attr.ratingMinusUnable))
            }
            if (rating != Rating.BAYAN) {
                container.quoteRatingBayan.setImageResource(resourceHelper.drawableIdFromAttr(R.attr.ratingBayanUnable))
            }

            quote.isVoting = false
        }
    }

    private class RestoreQuote(val quote: Quote) : () -> Unit  {
        override fun invoke() {
            quote.isVoting = false
        }
    }

    private val countToNextSmiley = resourceHelper.intArray(R.array.counts_to_next_smiley)
    private val smileyArrayLeft = resourceHelper.stringArray(R.array.quote_rating_smiley_left)
    private val smileyArrayRight = resourceHelper.stringArray(R.array.quote_rating_smiley_right)

    private fun alreadyRated(container: View, quote: Quote, rating: com.notjuststudio.bashim.common.Rating) {
        quote.countOfClicking++

        if (quote.currentSmiley < countToNextSmiley.size - 1 && quote.countOfClicking >= countToNextSmiley[quote.currentSmiley + 1]) {
            quote.countOfClicking = 0
            quote.currentSmiley++
        }

        if (quote.currentSmiley >= 0) {
            if (rating == Rating.RULEZ) {
                quote.value = smileyArrayLeft[quote.currentSmiley]
            } else {
                quote.value = smileyArrayRight[quote.currentSmiley]
            }
            container.quoteRatingValue.text = quote.value
        }
    }

    fun setupQuote(index: Int, type: QuoteType, root: View, quote: Quote,
                   onFavorite: (id: String) -> Unit = {},
                   onUnfavorite: (id: String) -> Unit = {}) {
        val activity = activityProvider.get()

        root.tag = index

        root.quoteDate.text = quote.date
        root.quoteText.text = quote.text

        root.quoteShare.setImageResource(resourceHelper.drawableIdFromAttr(R.attr.quoteShare))
        root.quoteShare.setOnClickListener{
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            if (activity != null) {
                val shareText = resourceHelper.string(R.string.quote_share_form,
                        if (type.canLink) {
                            resourceHelper.string(R.string.quote_id_form, quote.id)
                        } else {
                            resourceHelper.string(R.string.quote_share_abyss)
                        }, quote.text)
                interactionHelper.share(activity, shareText)
            }
        }

        root.quoteCopy.setImageResource(resourceHelper.drawableIdFromAttr(R.attr.quoteCopy))
        root.quoteCopy.setOnClickListener{
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            interactionHelper.toClipBoard(quote.text)
            App.info(R.string.quote_copy_success)
        }

        root.quoteText.textSize = sharedPrefHelper.getQuoteTextSize()
        if (type.canLink) {
            root.quoteText.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                goToQuote(quote.id ?: "")
            }
        }

        if (type.canFavorite) {
            root.quoteFavorite.visibility = View.VISIBLE
            root.quoteFavorite.setImageResource(if (quote.favorite)
                resourceHelper.drawableIdFromAttr(R.attr.quoteFavorite) else resourceHelper.drawableIdFromAttr(R.attr.quoteUnfavorite)
            )
            root.quoteFavorite.contentDescription = resourceHelper.string(if (quote.favorite) R.string.quote_unfavorite else R.string.quote_favorite)
            root.quoteFavorite.setOnClickListener{
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                if (quote.favorite) {
                    dbHelper.removeFavorite(quote.id?.toInt() ?: 0)
                    onUnfavorite(quote.id ?: "0")
                } else {
                    dbHelper.addFavorite(quote.id?.toInt() ?: 0)
                    onFavorite(quote.id ?: "0")
                }
                quote.favorite = !quote.favorite
                (it as ImageView).apply {
                    setImageResource(if (quote.favorite)
                        resourceHelper.drawableIdFromAttr(R.attr.quoteFavorite) else resourceHelper.drawableIdFromAttr(R.attr.quoteUnfavorite)
                    )
                    contentDescription = resourceHelper.string(if (quote.favorite) R.string.quote_unfavorite else R.string.quote_favorite)
                }
            }
        } else {
            root.quoteFavorite.visibility = View.GONE
        }

        if (type.needTop) {
            root.quoteId.text = resourceHelper.string(R.string.quote_top_form, index + 1)
            root.quoteId.setTextColor(resourceHelper.colorFromAttr(R.attr.textColor))
            root.quoteId.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            root.quoteId.setOnClickListener {}
        } else {
            root.quoteId.text = resourceHelper.string(R.string.quote_id_form, quote.id)
            root.quoteId.setTypeface(Typeface.DEFAULT, Typeface.NORMAL)
            if (type.canLink) {
                root.quoteId.setTextColor(resourceHelper.colorFromAttr(R.attr.linkColor))
                root.quoteId.paintFlags = (root.quoteId.paintFlags or Paint.UNDERLINE_TEXT_FLAG)
                root.quoteId.setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    goToQuote(quote.id ?: "")
                }
            } else {
                root.quoteId.setTextColor(resourceHelper.colorFromAttr(R.attr.textColor))
                root.quoteId.paintFlags = (root.quoteId.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv())
                root.quoteId.setOnClickListener {}
            }
        }

        if (type.canVote) {
            root.quoteRatingMinus.visibility = View.VISIBLE
            root.quoteRatingMinus.setImageResource(
                    when (quote.lastRating) {
                        Rating.SUX,
                        null -> resourceHelper.drawableIdFromAttr(R.attr.ratingMinus)
                        else -> resourceHelper.drawableIdFromAttr(R.attr.ratingMinusUnable)
                    }
            )
            root.quoteRatingPlus.visibility = View.VISIBLE
            root.quoteRatingPlus.setImageResource(
                    when (quote.lastRating) {
                        Rating.RULEZ,
                        null -> resourceHelper.drawableIdFromAttr(R.attr.ratingPlus)
                        else -> resourceHelper.drawableIdFromAttr(R.attr.ratingPlusUnable)
                    }
            )
            root.quoteRatingBayan.visibility = View.VISIBLE
            root.quoteRatingBayan.setImageResource(
                    when (quote.lastRating) {
                        Rating.BAYAN,
                        null -> resourceHelper.drawableIdFromAttr(R.attr.ratingBayan)
                        else -> resourceHelper.drawableIdFromAttr(R.attr.ratingBayanUnable)
                    }
            )

            root.quoteRatingValue.visibility = View.VISIBLE

            if (quote.value != null) {
                root.quoteRatingValue.text = quote.value
            } else {
                root.quoteRatingValue.setText(R.string.quote_rating_unknown_value)
            }

            root.quoteRatingMinus.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                if (!quote.isVoting) {
                    if (quote.isVoted) {
                        alreadyRated(root, quote, Rating.SUX)
                    } else {
                        quote.isVoting = true
                        QuoteRater.rate(quote, Rating.SUX,
                                Rate(root, quote, Rating.SUX), RestoreQuote(quote))
                    }
                }
            }
            root.quoteRatingPlus.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                if (!quote.isVoting) {
                    if (quote.isVoted) {
                        alreadyRated(root, quote, Rating.RULEZ)
                    } else {
                        quote.isVoting = true
                        QuoteRater.rate(quote, Rating.RULEZ,
                                Rate(root, quote, Rating.RULEZ), RestoreQuote(quote))
                    }
                }
            }
            root.quoteRatingBayan.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                if (!quote.isVoting) {
                    if (quote.isVoted) {
                        alreadyRated(root, quote, Rating.BAYAN)
                    } else {
                        quote.isVoting = true
                        QuoteRater.rate(quote, Rating.BAYAN,
                                Rate(root, quote, Rating.BAYAN), RestoreQuote(quote))
                    }
                }
            }
        } else {
            root.quoteRatingMinus.visibility = View.GONE
            root.quoteRatingPlus.visibility = View.GONE
            root.quoteRatingBayan.visibility = View.GONE

            root.quoteRatingValue.visibility = View.GONE

            root.quoteRatingMinus.setOnClickListener {}
            root.quoteRatingPlus.setOnClickListener {}
            root.quoteRatingBayan.setOnClickListener {}
        }
    }

    private var needUpdateTheme = false

    fun isNeedUpdateTheme() : Boolean {
        return needUpdateTheme
    }

    fun saveUpdateTheme(updateTheme: Boolean) {
        needUpdateTheme = updateTheme
    }

    private var titleName = ""

    fun setupTitleName(title: String) {
        titleName = title
    }

    fun getTitleName() : String {
        val title = titleName
        titleName = ""
        return title
    }

    private var cachedLink = Link.NONE
    private var cachedPagerIndex = 0
    private var cachedPagerPos = 0
    private var cachedPagerOffset = 0

    fun setupPosition(link: Link, pagerIndex: Int,
                      pagerPos: Int, pagerOffset: Int) {
        cachedLink = link
        cachedPagerIndex = pagerIndex
        cachedPagerPos = pagerPos
        cachedPagerOffset = pagerOffset
    }

    fun getLink() : Link {
        val link = cachedLink
        cachedLink = Link.NONE
        return link
    }

    fun getPagerIndex() : Int {
        val index = cachedPagerIndex
        cachedPagerIndex = 0
        return index
    }

    fun getPagerPos() : Int {
        val pos = cachedPagerPos
        cachedPagerPos = 0
        return pos
    }

    fun getPagerOffset() : Int {
        val offset = cachedPagerOffset
        cachedPagerOffset = 0
        return offset
    }

    private var cachedQuotes = listOf<Quote>()

    fun saveQuotes(quotes: List<Quote>) {
        cachedQuotes = quotes
    }

    fun restoreQuotes() : List<Quote> {
        val quotes = cachedQuotes
        cachedQuotes = listOf()
        return quotes
    }

    private var cachedDate: Calendar? = null

    fun setupDate(referenceDate: Calendar) {
        cachedDate = referenceDate
    }

    fun getDate() : Calendar? {
        val referenceDate = cachedDate
        cachedDate = null
        return referenceDate
    }

    private var cachedDefaultData = ""
    private var cachedNextData = ""

    fun setupLoaderData(defData: String, nextData: String? = null) {
        cachedDefaultData = defData
        cachedNextData = if (nextData != null) nextData else defData
    }

    fun getDefData() : String {
        val data = cachedDefaultData
        cachedDefaultData = ""
        return data
    }

    fun getNextData() : String {
        val data = cachedNextData
        cachedNextData = ""
        return data
    }

    private var cachedFavoriteOffset = 0

    fun setupFavoriteOffset(offset: Int) {
        cachedFavoriteOffset = offset
    }

    fun getFavoriteOffset() : Int {
        val offset = cachedFavoriteOffset
        cachedFavoriteOffset = 0
        return offset
    }

    fun discardCache() {
        getLink()
        getPagerIndex()
        getPagerPos()
        getPagerOffset()

        restoreQuotes()

        getDefData()
        getNextData()

        getFavoriteOffset()
    }

    fun calculateDate(link: Link, referenceDate: Calendar, position: Int) : String {
        val date = when(link.title) {
            TitleType.BEST_MONTH -> {
                val cal = Calendar.getInstance()
                cal.time = referenceDate.time
                cal.add(Calendar.MONTH, -position)

                val year = cal.get(Calendar.YEAR)
                val month = cal.get(Calendar.MONTH) + 1

                "$year/$month"
            }
            TitleType.BEST_YEAR -> {
                (referenceDate.get(Calendar.YEAR) - position).toString()
            }
            TitleType.ABYSS_BEST -> {
                val cal = Calendar.getInstance()
                cal.time = referenceDate.time
                cal.add(Calendar.DATE, -position)

                val year = cal.get(Calendar.YEAR)
                val month = cal.get(Calendar.MONTH) + 1
                val day = cal.get(Calendar.DAY_OF_MONTH)

                String.format("%04d%02d%02d", year, month, day)
            }
            else -> ""
        }
        return date
    }

    fun calculatePosition(lastDay: Calendar, year: Int, month: Int? = null, day: Int? = null) : Int {
        val index = when {
            month == null -> {
                lastDay.get(Calendar.YEAR) - year
            }
            day == null -> {
                val mYear = lastDay.get(Calendar.YEAR) - year
                val mMonth = lastDay.get(Calendar.MONTH) - month

                val index = mYear * 12 + mMonth

                index
            }
            else -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, day)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)

                lastDay.set(Calendar.HOUR_OF_DAY, 0)
                lastDay.set(Calendar.MINUTE, 0)
                lastDay.set(Calendar.SECOND, 0)
                lastDay.set(Calendar.MILLISECOND, 0)

                val diff = (lastDay.timeInMillis - cal.timeInMillis) / (24 * 60 * 60 * 1000)

                diff.toInt()
            }
        }

        return index
    }

}