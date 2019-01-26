package com.notjuststudio.bashim.loader

import android.util.Log
import com.notjuststudio.bashim.App
import com.notjuststudio.bashim.common.Quote
import com.notjuststudio.bashim.helper.DataBaseHelper
import com.notjuststudio.bashim.proto.BaseLoadTask
import com.notjuststudio.bashim.proto.BaseLoader
import javax.inject.Inject

class FavoriteQuotesLoader(offset: Int = 0) : BaseLoader() {

    private var nextOffset: Int = offset

    companion object {
        const val LOAD_COUNT = 50
    }

    @Inject
    lateinit var dbHelper: DataBaseHelper

    init {
        App.instance().component.inject(this)

        taskFactory = LoadTaskFactory(this)
    }

    fun getOffset() : Int {
        return nextOffset
    }

    override fun reset() {
        nextOffset = 0
    }

    fun incrementOffset() {
        nextOffset++
    }

    fun decrementOffset() {
        nextOffset--
    }

    private class LoadTaskFactory(val loader: FavoriteQuotesLoader) : BaseLoadTask.Factory {
        override fun create(onLoaded: (List<Quote>) -> Unit,
                            onFirstLoaded: () -> Unit,
                            onDoneLoading: () -> Unit,
                            onFailed: () -> Unit,
                            onNothingToLoad: () -> Unit): BaseLoadTask {
            return LoadTask(
                    loader,
                    onLoaded,
                    onFirstLoaded,
                    onDoneLoading,
                    onFailed,
                    onNothingToLoad)
        }
    }

    private class LoadTask(private val loader: FavoriteQuotesLoader,
                           onLoaded: (List<Quote>) -> Unit,
                           onFirstLoaded: () -> Unit,
                           onDoneLoading: () -> Unit,
                           onFailed: () -> Unit,
                           onNothingToLoad: () -> Unit) : BaseLoadTask(onLoaded, onFirstLoaded, onDoneLoading, onFailed, onNothingToLoad) {

        private var counter = 0
        private var sum = 0

        private var alreadyAlarmed = false

        override fun doInBackground(vararg params: Unit?): Boolean {
            val ids = loader.dbHelper.getFavorites(LOAD_COUNT, loader.nextOffset)
            sum = ids.size

            if (sum == 0) {
                return true
            }

            for (id in ids) {
                SingleQuoteLoader.loadQuote(id, onLoaded = {
                    if (counter == 0) {
                        onFirstLoaded.invoke()
                    }
                    counter++
                    onLoaded.invoke(listOf(it))
                    Log.i("Loader", "Done loading, thread: ${Thread.currentThread().name}")
                    if (counter == sum) {
                        loader.nextOffset += counter
                        onDoneLoading.invoke()
                    }
                }, onFailed = {
                    if (!alreadyAlarmed) {
                        alreadyAlarmed = true
                        loader.nextOffset += counter
                        onDoneLoading.invoke()
                        onFailed.invoke()
                    }
                })
            }

            return false
        }

        // true means that nothing to load
        override fun onPostExecute(result: Boolean?) {
            if (result == true) {
                onFirstLoaded.invoke()
                onDoneLoading.invoke()
                onNothingToLoad.invoke()
            }
        }

    }

}