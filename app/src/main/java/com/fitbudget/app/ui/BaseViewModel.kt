package com.fitbudget.app.ui

import androidx.lifecycle.ViewModel
import com.fitbudget.app.util.DateTimeUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

/**
 * Shared plumbing: one-off user messages (snackbars) and a ticking "current day" flow so screens
 * that are left open across midnight roll over on their own.
 */
abstract class BaseViewModel : ViewModel() {

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    protected fun notifyUser(text: String) {
        _messages.tryEmit(text)
    }

    protected fun notifyError(error: Throwable, fallback: String = "Something went wrong.") {
        notifyUser(error.message?.takeIf { it.isNotBlank() } ?: fallback)
    }

    companion object {
        /** Emits the current epoch day, re-checked every minute. */
        @OptIn(ExperimentalCoroutinesApi::class)
        fun currentDayFlow(): Flow<Long> = flow {
            while (true) {
                emit(DateTimeUtils.todayEpochDay())
                delay(60_000)
            }
        }.distinctUntilChanged()
    }
}
