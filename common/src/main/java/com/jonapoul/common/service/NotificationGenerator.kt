package com.jonapoul.common.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import com.jonapoul.common.R
import com.jonapoul.common.di.IBuildResources
import com.jonapoul.common.prefs.getStringFromPair
import com.jonapoul.common.presets.OutputPreset
import com.jonapoul.common.utils.GenerateInt
import com.jonapoul.common.utils.MinimumVersions.NOTIFICATION_CATEGORY_SERVICE
import com.jonapoul.common.utils.MinimumVersions.NOTIFICATION_CHANNELS
import com.jonapoul.common.utils.MinimumVersions.NOTIFICATION_PRIORITY_MAX
import com.jonapoul.common.utils.Protocol
import com.jonapoul.common.utils.VersionUtils
import javax.inject.Inject

class NotificationGenerator @Inject constructor(
        private val context: Context,
        private val prefs: SharedPreferences,
        private val buildResources: IBuildResources,
) : INotificationGenerator {

    private val notificationManager = context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager

    private val stopServicePendingIntentId = GenerateInt.next()

    private val foregroundChannelId = "${buildResources.appId}.FOREGROUND"
    private val errorChannelId = "${buildResources.appId}.ERROR"
    private val notificationChannelId = "${buildResources.appId}.NOTIFICATION"

    override fun getForegroundNotification(): Notification {
        /* Intent to stop the service when the notification button is tapped */
        val stopPendingIntent = PendingIntent.getService(
                context,
                stopServicePendingIntentId,
                Intent(context, buildResources.serviceClass).setAction(CotService.STOP_SERVICE),
                0
        )
        val channelId = if (VersionUtils.isAtLeast(NOTIFICATION_CHANNELS)) {
            foregroundChannelId
        } else {
            buildResources.appName
        }
        val builder = NotificationCompat.Builder(context, channelId)
                .setOngoing(true)
                .setSmallIcon(R.drawable.target)
                .setContentTitle(buildResources.appName)
                .setContentText(getPresetInfoString(prefs))
                .addAction(R.drawable.stop, context.getString(R.string.notification_stop), stopPendingIntent)

        if (VersionUtils.isAtLeast(NOTIFICATION_CATEGORY_SERVICE)) {
            builder.setCategory(Notification.CATEGORY_SERVICE)
        }
        if (VersionUtils.isLessThan(NOTIFICATION_PRIORITY_MAX)) {
            builder.priority = NotificationCompat.PRIORITY_MAX
        }
        return builder.build()
    }

    override fun showErrorNotification(errorMessage: String?) {
        val message = errorMessage ?: "Null error: check the stack trace for info"
        notificationManager.notify(
                GenerateInt.next(),
                NotificationCompat.Builder(context, errorChannelId)
                        .setOngoing(false)
                        .setSmallIcon(R.drawable.error)
                        .setContentTitle("Error")
                        .setContentText(message)
                        .setStyle(bigText(message))
                        .build()
        )
    }

    override fun showNotification(@DrawableRes iconId: Int, title: String, subtitle: String) {
        notificationManager.notify(
                GenerateInt.next(),
                NotificationCompat.Builder(context, notificationChannelId)
                        .setOngoing(false)
                        .setSmallIcon(iconId)
                        .setContentTitle(title)
                        .setContentText(subtitle)
                        .setStyle(bigText(subtitle))
                        .build()
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun createForegroundChannel() {
        notificationManager.createNotificationChannel(
                NotificationChannel(
                        foregroundChannelId,
                        buildResources.appName,
                        NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    lightColor = Color.BLUE
                    lockscreenVisibility = Notification.VISIBILITY_SECRET
                    enableVibration(false)
                    setSound(null, null)
                    setShowBadge(false)
                }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun createErrorChannel() {
        notificationManager.createNotificationChannel(
                NotificationChannel(
                        errorChannelId,
                        "Error",
                        NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    lightColor = Color.RED
                    lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                    vibrationPattern = longArrayOf(0, 1000) // zero delay, then vibrate for 1 second
                    setSound(null, null)
                    setShowBadge(false)
                }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun createNotificationChannel(@StringRes titleId: Int) {
        notificationManager.createNotificationChannel(
                NotificationChannel(
                        notificationChannelId,
                        context.getString(titleId),
                        NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    lightColor = Color.YELLOW
                    lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                    vibrationPattern = longArrayOf(0, 200) // zero delay, then vibrate for 0.2 seconds
                    setSound(null, null)
                    setShowBadge(true)
                }
        )
    }

    private fun getPresetInfoString(prefs: SharedPreferences): String {
        val protocol = Protocol.fromPrefs(prefs)
        val preset = OutputPreset.fromString(prefs.getStringFromPair(protocol.presetPref))
        return protocol.toString() + if (preset == null) ": Unknown" else ": " + preset.alias
    }

    private fun bigText(text: String?) = NotificationCompat.BigTextStyle().bigText(text)
}