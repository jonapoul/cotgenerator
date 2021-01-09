package com.jonapoul.common.logging

import android.util.Log
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import timber.log.Timber


class FileLoggingTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val logMessage = "$tag: $message"
        when (priority) {
            Log.VERBOSE -> return
            Log.DEBUG -> logger.debug(logMessage)
            Log.INFO -> logger.info(logMessage)
            Log.WARN -> logger.warn(logMessage)
            Log.ERROR -> logger.error(logMessage)
        }
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(FileLoggingTree::class.java)
    }
}
