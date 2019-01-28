package com.notjuststudio.bashim.activity

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.AdapterView
import kotlinx.android.synthetic.main.settings_activity.*
import android.content.DialogInterface
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.SeekBar
import com.notjuststudio.bashim.*
import com.notjuststudio.bashim.common.Link
import com.notjuststudio.bashim.custom.TriggerOnClickListener
import com.notjuststudio.bashim.helper.*
import com.notjuststudio.bashim.loader.CountQuoteLoader
import com.notjuststudio.bashim.proto.BaseActivity
import com.notjuststudio.bashim.service.QuoteSaverController
import kotlinx.android.synthetic.main.offline_dialog_layout.view.*
import java.lang.Math.max
import java.lang.Math.min
import javax.inject.Inject


class SettingsActivity : BaseActivity() {

    companion object {

        const val LOAD_COUNT = 1000

        const val MAX_PROGRESS = 1000

        const val LAST_THEME_WAS_DARK = "lastThemeWasDark"
        const val LAST_TEXT_SIZE = "lastTextSize"
        const val SCROLL_POSITION = "scrollPosition"

    }

    @Inject
    lateinit var resourceHelper: ResourceHelper

    @Inject
    lateinit var inflaterHelper: InflaterHelper

    @Inject
    lateinit var dbHelper: DataBaseHelper

    @Inject
    lateinit var quoteSaverController: QuoteSaverController

    @Inject
    lateinit var billingHelper: BillingHelper

    @Inject
    lateinit var quoteHelper: QuoteHelper

    private var lastThemeWasDark: Boolean = false
    private var lastTextSize: Float = 15f

    @SuppressLint("PrivateResource")
    override fun onCreate(savedInstanceState: Bundle?) {
        app.component.inject(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        setSupportActionBar(toolbar)

        val currentThemeIsDark = sharedPrefHelper.isDarkTheme()
        lastThemeWasDark = intent?.extras?.getBoolean(LAST_THEME_WAS_DARK, currentThemeIsDark) ?: currentThemeIsDark

        addSheludePost{
            scroll.scrollTo(0, intent?.extras?.getInt(SCROLL_POSITION, 0) ?: 0)
        }

        val textSizeInit = sharedPrefHelper.getQuoteTextSize()
        lastTextSize = intent?.extras?.getFloat(LAST_TEXT_SIZE, textSizeInit) ?: textSizeInit
        val textSizeMin = resourceHelper.int(R.integer.quote_text_min_size)
        val textSizeMax = resourceHelper.int(R.integer.quote_text_max_size)

        quoteHelper.saveUpdateTheme(lastTextSize != textSizeInit || lastThemeWasDark != currentThemeIsDark)

        textSizeBarExample.textSize = textSizeInit
        textSizeBar.max = MAX_PROGRESS
        textSizeBar.progress = (MAX_PROGRESS * (textSizeInit - textSizeMin) / (textSizeMax - textSizeMin)).toInt()
        textSizeBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val textSize = (textSizeMax - textSizeMin).toFloat() * progress / MAX_PROGRESS + textSizeMin
                sharedPrefHelper.setQuoteTextSize(textSize)
                textSizeBarExample.textSize = textSize

                quoteHelper.saveUpdateTheme(lastTextSize != textSize || lastThemeWasDark != currentThemeIsDark)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {}

        })

        isDarkTheme.isChecked = currentThemeIsDark
        isDarkTheme.setOnCheckedChangeListener { _, flag ->
            sharedPrefHelper.setIsDarkTheme(flag)
            intent
                    .putExtra(LAST_THEME_WAS_DARK, lastThemeWasDark)
                    .putExtra(LAST_TEXT_SIZE, textSizeBarExample.textSize)
                    .putExtra(SCROLL_POSITION, scroll.scrollY)
            finish()
            startActivity(intent)
            overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out)
        }

        spinnerFavorite.adapter = ArrayAdapter<String>(this, R.layout.spinner_item, resourceHelper.stringArray(R.array.preferred_links))
        spinnerFavorite.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                sharedPrefHelper.saveFavorite(Link.fromInt(position))
            }

        }

        spinnerFavorite.setSelection(sharedPrefHelper.loadFavorite().id)

        downloadRandom.setOnClickListener{ it ->
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

            @SuppressLint("InflateParams")
            val root = inflaterHelper.inflate(R.layout.loading_layout, null)

            val dialog = AlertDialog.Builder(this, R.style.Dialog)
                    .setView(root)
                    .setCancelable(true)
                    .create()

            dialog.show()

            CountQuoteLoader.loadQuote(onLoaded = { count ->
                dialog.dismiss()
                val offlineQuotes = dbHelper.countQuotes()
                val available = count - offlineQuotes

                if (available <= 0) {
                    App.info(R.string.download_fat)
                    return@loadQuote
                }

                @Suppress("NAME_SHADOWING")
                @SuppressLint("InflateParams")
                val root = inflaterHelper.inflate(R.layout.offline_dialog_layout, null)

                @Suppress("NAME_SHADOWING")
                val dialog = AlertDialog.Builder(this, R.style.Dialog)
                        .setView(root)
                        .setCancelable(true)
                        .setPositiveButton(R.string.date_picker_ok){ dialog, _->
                            val count = root.count.text.toString().toInt()
                            dialog.dismiss()

                            if (count <= 0) {
                                return@setPositiveButton
                            }

                            disableButtons()

                            quoteSaverController.setOnConnected {
                                quoteSaverController.addOnDoneListener {
                                    enableButtons()
                                }

                                quoteSaverController.setOnUpdateListener { _ ->
                                    randomTitle.text = getString(R.string.random_title, dbHelper.countQuotes())
                                }
                            }

                            quoteSaverController.startLoading(count)
                        }
                        .setNegativeButton(R.string.date_picker_cancel){ dialog, _->
                            dialog.dismiss()
                        }
                        .setTitle(R.string.download_count)
                        .create()

                root.count.setText(LOAD_COUNT.toString())
                root.count.addTextChangedListener(object : TextWatcher{

                    var skip = false

                    override fun afterTextChanged(text: Editable) {
                        if (skip)
                            return

                        val value = try {
                            text.toString().toInt()
                        } catch (e: Throwable) {
                            0
                        }

                        skip = true
                        text.clear()
                        text.insert(0, max(0, min(available, value)).toString())
                        skip = false
                    }

                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                })

                dialog.show()

            }, onFailed = {
                App.error(R.string.quotes_load_error)
                dialog.dismiss()
            })

        }

        clearRandom.setOnClickListener{
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

            AlertDialog.Builder(this, R.style.Dialog)
                    .setTitle(R.string.clear_random)
                    .setMessage(R.string.clear_random_text)
                    .setCancelable(true)
                    .setPositiveButton(R.string.clear_random_confirm) {
                        _ : DialogInterface, _ : Int ->
                        dbHelper.clearQuotes()
                        randomTitle.text = getString(R.string.random_title, 0)
                    }
                    .setNegativeButton(R.string.clear_random_cancel) {
                        _ : DialogInterface, _ : Int ->
                    }
                    .show()
        }

        favoriteTitle.text = getString(R.string.favorite_title, dbHelper.countFavorites())
        clearFavorite.setOnClickListener{
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

            AlertDialog.Builder(this, R.style.Dialog)
                    .setTitle(R.string.clear_favorite)
                    .setMessage(R.string.clear_random_text)
                    .setCancelable(true)
                    .setPositiveButton(R.string.clear_favorite_confirm) {
                        _ : DialogInterface, _ : Int ->
                        dbHelper.clearFavorites()
                        favoriteTitle.text = getString(R.string.favorite_title, 0)
                    }
                    .setNegativeButton(R.string.clear_favorite_cancel) {
                        _ : DialogInterface, _ : Int ->
                    }
                    .show()
        }

        donateButton.setOnClickListener(object : TriggerOnClickListener() {
            override fun triggerOnClick(v: View?) {
                billingHelper.purchasePie(this@SettingsActivity, this::consume)
            }

        })
    }

    private fun disableButtons() {
        downloadRandom.isEnabled = false
        clearRandom.isEnabled = false
    }

    private fun enableButtons() {
        downloadRandom.isEnabled = true
        clearRandom.isEnabled = true
    }

    override fun onResume() {
        super.onResume()

        randomTitle.text = getString(R.string.random_title, dbHelper.countQuotes())

        quoteSaverController.setOnConnected {
            if (quoteSaverController.isLoading()) {
                disableButtons()
                quoteSaverController.addOnDoneListener {
                    enableButtons()
                }
                quoteSaverController.setOnUpdateListener {
                    randomTitle.text = getString(R.string.random_title, dbHelper.countQuotes())
                }
            } else {
                enableButtons()
            }
        }
    }

    override fun onPause() {
        super.onPause()

        quoteSaverController.clearOnDoneListeners()
        quoteSaverController.setOnUpdateListener(null)
    }

    override fun onStart() {
        super.onStart()
        quoteSaverController.connectToService()
    }

    override fun onStop() {
        super.onStop()
        quoteSaverController.disconnectFromService()
    }

}