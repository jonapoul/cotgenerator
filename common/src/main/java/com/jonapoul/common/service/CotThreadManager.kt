package com.jonapoul.common.service

import android.content.SharedPreferences
import com.jonapoul.common.repositories.ISocketRepository
import timber.log.Timber
import java.util.concurrent.Executors

internal class CotThreadManager(
        prefs: SharedPreferences,
        private val cotFactory: CotFactory,
        errorListener: IThreadErrorListener,
        private val socketRepository: ISocketRepository,
) : ThreadManager(prefs, errorListener), SharedPreferences.OnSharedPreferenceChangeListener {

    private var thread: BaseThread? = null

    init {
        prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun start() {
        synchronized(lock) {
            Timber.d("start")
            thread = BaseThread.fromPrefs(prefs, socketRepository, cotFactory).apply {
                setUncaughtExceptionHandler { _, t: Throwable -> errorListener.onThreadError(t) }
                start()
            }
        }
    }

    override fun shutdown() {
        synchronized(lock) {
            Timber.d("shutdown")
            Executors.newSingleThreadExecutor().execute {
                synchronized(lock) {
                    thread?.shutdown()
                    thread = null
                }
            }
        }
    }

    override fun restart() {
        Timber.d("restart")
        Executors.newSingleThreadExecutor().execute {
            synchronized(lock) {
                thread?.shutdown()
                start()
            }
        }
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        /* If any preferences are changed, kill the thread and instantly reload with the new settings */
        Timber.d("onSharedPreferenceChanged %s", key)
        synchronized(lock) {
            if (isRunning()) {
                cotFactory.clear()
                restart()
            }
        }
    }

    override fun isRunning(): Boolean {
        synchronized(lock) {
            return (thread?.isRunning() ?: false).also {
                Timber.d("isRunning %s", it)
            }
        }
    }
}
