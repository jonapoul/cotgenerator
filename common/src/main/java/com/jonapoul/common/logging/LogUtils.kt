package com.jonapoul.common.logging

import android.os.Build
import com.jonapoul.common.di.IBuildResources
import timber.log.Timber

object LogUtils {
    fun startFileLogging(buildResources: IBuildResources) {
        /* Plant the tree, activate file output */
        Timber.plant(FileLoggingTree())

        /* Start the feed by printing app and device info, in case it matters at all */
        Timber.i("Starting file logging!")
        Timber.i("--------------APP--------------")
        Timber.i("Platform = ${buildResources.platform}")
        Timber.i("Build timestamp = ${buildResources.buildTimestamp}")
        Timber.i("App ID = ${buildResources.appId}")
        Timber.i("Version = ${buildResources.versionName}")
        Timber.i("Debug = ${buildResources.isDebug}")
        Timber.i("------------DEVICE------------")
        Timber.i("OS Version = ${System.getProperty("os.version")}")
        Timber.i("API Level = ${Build.VERSION.SDK_INT}")
        Timber.i("Manufacturer = ${Build.MANUFACTURER}")
        Timber.i("Device = ${Build.DEVICE}")
        Timber.i("Model = ${Build.MODEL}")
        Timber.i("Product = ${Build.PRODUCT}")
        Timber.i("Display = ${Build.DISPLAY}")
        Timber.i("Board = ${Build.BOARD}")
        Timber.i("Brand = ${Build.BRAND}")
        Timber.i("Is Emulator = ${System.getProperty("ro.kernel.qemu", Build.UNKNOWN)}")
        Timber.i("Hardware = ${Build.HARDWARE}")
        Timber.i("------------VERSION------------")
        Timber.i("Incremental = ${Build.VERSION.INCREMENTAL}")
        Timber.i("Release = ${Build.VERSION.RELEASE}")
//        Timber.i("Base OS = ${Build.VERSION.BASE_OS}")
        Timber.i("Codename = ${Build.VERSION.CODENAME}")
        Timber.i("-------REGULAR LOGGING--------")
    }

    fun stopFileLogging() {
        Timber.i("Stopping file logging!")
        Timber.forest().forEach {
            if (it is FileLoggingTree) {
                Timber.uproot(it)
            }
        }
    }
}