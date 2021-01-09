package com.jonapoul.common.logging

import android.annotation.SuppressLint
import android.util.Log
import timber.log.Timber

class ReleaseTree : Timber.Tree() {

    @SuppressLint("LogNotTimber")
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val logMessage = "$tag: $message"
        when (priority) {
            Log.VERBOSE -> return
            Log.DEBUG -> Log.d(TAG, logMessage)
            Log.INFO -> Log.i(TAG, logMessage)
            Log.WARN -> Log.w(TAG, logMessage)
            Log.ERROR -> Log.e(TAG, logMessage)
        }
    }
    private companion object {
        val TAG = ReleaseTree::class.java.simpleName
    }
}