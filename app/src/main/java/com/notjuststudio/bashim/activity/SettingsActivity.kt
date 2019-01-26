package com.notjuststudio.bashim.activity

import android.app.AlertDialog
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.AdapterView
import kotlinx.android.synthetic.main.settings_activity.*
import android.content.DialogInterface
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.CompoundButton
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

        const val LAST_THEME_WAS_DARK = "lastThemeWasDark"
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

    var lastThemeWasDark: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        app.component.inject(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        setSupportActionBar(toolbar)

        val currentThemeIsDark = sharedPrefHelper.isDarkTheme()
        lastThemeWasDark = intent?.extras?.getBoolean(LAST_THEME_WAS_DARK, currentThemeIsDark) ?: currentThemeIsDark
        quoteHelper.saveUpdateTheme(lastThemeWasDark != currentThemeIsDark)

        addSheludePost{
            scroll.scrollTo(0, intent?.extras?.getInt(SCROLL_POSITION, 0) ?: 0)
        }

        isDarkTheme.isChecked = currentThemeIsDark
        isDarkTheme.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(view: CompoundButton, flag: Boolean) {
                sharedPrefHelper.saveIsDarkTheme(flag)
                intent
                        .putExtra(LAST_THEME_WAS_DARK, lastThemeWasDark)
                        .putExtra(SCROLL_POSITION, scroll.scrollY)
                finish()
                startActivity(intent)
                overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out)
            }

        })

        spinnerFavorite.adapter = ArrayAdapter<String>(this, R.layout.spinner_item, resourceHelper.stringArray(R.array.preferred_links))
        spinnerFavorite.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                sharedPrefHelper.saveFavorite(Link.fromInt(position))
            }

        }

        spinnerFavorite.setSelection(sharedPrefHelper.loadFavorite().id)

        downloadRandom.setOnClickListener{
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

            val root = inflaterHelper.inflate(R.layout.loading_layout, null)

            val dialog = AlertDialog.Builder(this, R.style.Dialog)
                    .setView(root)
                    .setCancelable(true)
                    .create()

            dialog.show()

            CountQuoteLoader.loadQuote(onLoaded = {
                dialog.dismiss()
                val offlineQuotes = dbHelper.countQuotes()
                val available = it - offlineQuotes

                if (available <= 0) {
                    App.info(R.string.download_fat)
                    return@loadQuote
                }

                @Suppress("NAME_SHADOWING")
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

                                quoteSaverController.setOnUpdateListener {
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