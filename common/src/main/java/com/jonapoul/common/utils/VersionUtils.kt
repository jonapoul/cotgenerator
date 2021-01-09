package com.jonapoul.common.utils

import android.os.Build

object VersionUtils {
    fun isAtLeast(sdkMinimum: Int): Boolean {
        return Build.VERSION.SDK_INT >= sdkMinimum
    }

    fun isLessThan(sdkMaximum: Int): Boolean {
        return Build.VERSION.SDK_INT < sdkMaximum
    }
}
