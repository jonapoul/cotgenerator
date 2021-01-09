package com.jonapoul.common.service

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener

abstract class ThreadManager(
        protected val prefs: SharedPreferences,
        protected val errorListener: IThreadErrorListener
) : OnSharedPreferenceChangeListener {

    protected val lock = Any()

    abstract fun start()

    abstract fun shutdown()

    abstract fun restart()

    abstract fun isRunning(): Boolean
}
