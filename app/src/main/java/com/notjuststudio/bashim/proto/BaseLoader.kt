package com.notjuststudio.bashim.proto

import android.os.AsyncTask
import com.notjuststudio.bashim.common.Quote
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

abstract class BaseLoader {

    private lateinit var task: BaseLoadTask
    protected lateinit var taskFactory: BaseLoadTask.Factory

    companion object {
        private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    }

    fun loadQuotes(onLoaded: (List<Quote>) -> Unit = {},
                   onFirstLoaded: () -> Unit = {},
                   onDoneLoading: () -> Unit = {},
                   onFailed: () -> Unit = {},
                   onNothingToLoad: () -> Unit = {}) {
        if (::taskFactory.isInitialized) {
            task = taskFactory.create(
                    onLoaded,
                    onFirstLoaded,
                    onDoneLoading,
                    onFailed,
                    onNothingToLoad)
            task.executeOnExecutor(executor)
        }
    }

    abstract fun reset()

    fun cancel() {
        if (::task.isInitialized && task.status != AsyncTask.Status.FINISHED) {
            do {
                task.cancel(true)
            } while (!task.isCancelled)
        }
    }

}

abstract class BaseLoadTask(protected val onLoaded: (List<Quote>) -> Unit,
                            protected val onFirstLoaded: () -> Unit,
                            protected val onDoneLoading: () -> Unit,
                            protected val onFailed: () -> Unit,
                            protected val onNothingToLoad: () -> Unit) : AsyncTask<Unit, Unit, Boolean>() {

    interface Factory {

        fun create(onLoaded: (List<Quote>) -> Unit,
                   onFirstLoaded: () -> Unit,
                   onDoneLoading: () -> Unit,
                   onFailed: () -> Unit,
                   onNothingToLoad: () -> Unit) : BaseLoadTask

    }

}