package com.notjuststudio.bashim.loader

import com.notjuststudio.bashim.App
import com.notjuststudio.bashim.common.Quote
import com.notjuststudio.bashim.helper.DataBaseHelper
import com.notjuststudio.bashim.proto.BaseLoadTask
import com.notjuststudio.bashim.proto.BaseLoader
import javax.inject.Inject

class OfflineQuotesLoader : BaseLoader() {

    companion object {
        const val LOAD_COUNT = 50
    }

    @Inject
    lateinit var db: DataBaseHelper

    init {
        App.instance().component.inject(this)

        taskFactory = LoadTaskFactory(this)
    }

    override fun reset() {}

    private class LoadTaskFactory(val loader: OfflineQuotesLoader) : BaseLoadTask.Factory {
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

    private class LoadTask(private val loader: OfflineQuotesLoader,
                           onLoaded: (List<Quote>) -> Unit,
                           onFirstLoaded: () -> Unit,
                           onDoneLoading: () -> Unit,
                           onFailed: () -> Unit,
                           onNothingToLoad: () -> Unit) : BaseLoadTask(onLoaded, onFirstLoaded, onDoneLoading, onFailed, onNothingToLoad) {

        lateinit var quotes: List<Quote>

        override fun doInBackground(vararg params: Unit?): Boolean {
            quotes = loader.db.getQuotes(LOAD_COUNT)

            return quotes.isNotEmpty()
        }

        override fun onPostExecute(result: Boolean?) {
            if (result == true) {
                onFirstLoaded.invoke()
                onLoaded.invoke(quotes)
                onDoneLoading.invoke()
            } else {
                onDoneLoading.invoke()
                onNothingToLoad.invoke()
            }
        }

    }
}