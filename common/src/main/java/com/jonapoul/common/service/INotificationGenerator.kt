package com.jonapoul.common.service

import android.app.Notification
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes

interface INotificationGenerator {
    @RequiresApi(Build.VERSION_CODES.O)
    fun createForegroundChannel()

    @RequiresApi(Build.VERSION_CODES.O)
    fun createErrorChannel()

    @RequiresApi(Build.VERSION_CODES.O)
    fun createNotificationChannel(@StringRes titleId: Int)

    fun getForegroundNotification(): Notification
    fun showErrorNotification(errorMessage: String?)
    fun showNotification(@DrawableRes iconId: Int, title: String, subtitle: String)
}
