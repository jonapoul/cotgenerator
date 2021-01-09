package com.jonapoul.cotbeacon.service

import android.content.IntentFilter
import android.os.PowerManager
import androidx.annotation.RequiresApi
import com.jonapoul.common.prefs.getBooleanFromPair
import com.jonapoul.common.repositories.IDeviceUidRepository
import com.jonapoul.common.service.CotService
import com.jonapoul.common.utils.MinimumVersions.IGNORE_BATTERY_OPTIMISATIONS
import com.jonapoul.common.utils.MinimumVersions.NOTIFICATION_CHANNELS
import com.jonapoul.common.utils.VersionUtils
import com.jonapoul.cotbeacon.BeaconApplication
import com.jonapoul.cotbeacon.R
import com.jonapoul.cotbeacon.cot.ChatCursorOnTarget
import com.jonapoul.cotbeacon.cot.EmergencyType
import com.jonapoul.cotbeacon.prefs.BeaconPrefs
import com.jonapoul.cotbeacon.repositories.IChatRepository
import com.jonapoul.cotbeacon.service.chat.ChatThreadManager
import com.jonapoul.cotbeacon.service.emergency.EmergencyThreadManager
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject


@AndroidEntryPoint
class BeaconCotService : CotService() {
    @Inject
    lateinit var chatRepository: IChatRepository

    @Inject
    lateinit var deviceUidRepository: IDeviceUidRepository

    private val chatThreadManager by lazy {
        ChatThreadManager(
                prefs = prefs,
                errorListener = this,
                chatRepository = chatRepository,
                deviceUidRepository = deviceUidRepository,
                socketRepository = socketRepository,
        )
    }

    private val emergencyThreadManager by lazy {
        EmergencyThreadManager(
                prefs = prefs,
                errorListener = this,
                deviceUidRepository = deviceUidRepository,
                socketRepository = socketRepository,
                gpsRepository = gpsRepository
        )
    }

    private lateinit var dozeReceiver: DozeReceiver

    override fun onCreate() {
        super.onCreate()
        Timber.d("onCreate")
        if (VersionUtils.isAtLeast(NOTIFICATION_CHANNELS)) {
            notificationGenerator.createNotificationChannel(R.string.chat_notification_title)
        }
        observeChatMessages()

        if (VersionUtils.isAtLeast(IGNORE_BATTERY_OPTIMISATIONS)) {
            initialiseDozeReceiver()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")
        if (VersionUtils.isAtLeast(IGNORE_BATTERY_OPTIMISATIONS)) {
            unregisterReceiver(dozeReceiver)
        }
    }

    override fun start() {
        super.start()
        Timber.d("start")
        if (prefs.getBooleanFromPair(BeaconPrefs.ENABLE_CHAT)) {
            chatThreadManager.start()
        }
    }

    override fun shutdown() {
        super.shutdown()
        Timber.d("shutdown")
        chatThreadManager.shutdown()
    }

    private fun observeChatMessages() {
        Timber.d("observeChatMessages")
        chatRepository.getLatestChat().observe(this) {
            Timber.d("Observed new chat message from repository")
            if (it.isIncoming && !BeaconApplication.chatFragmentIsVisible) {
                Timber.d("Showing notification")
                /* Only show a notification if this isn't a self-message, and if the chat fragment
                * isn't already open. */
                notificationGenerator.showNotification(
                        iconId = R.drawable.chat,
                        title = "Message from ${it.callsign}",
                        subtitle = "${it.start.shortLocalTimestamp()}: ${it.message}"
                )
            }
        }
    }

    @RequiresApi(IGNORE_BATTERY_OPTIMISATIONS)
    private fun initialiseDozeReceiver() {
        registerReceiver(
                DozeReceiver().also { dozeReceiver = it },
                IntentFilter().apply {
                    addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED)
                }
        )
        // Tell it to grab the current "doze mode" value
        dozeReceiver.postDozeModeValue(this, gpsRepository)
        Timber.i("Doze receiver initialised")
    }

    fun sendChat(chatMessage: ChatCursorOnTarget) {
        chatThreadManager.sendChat(chatMessage)
    }

    fun sendEmergency(emergencyType: EmergencyType) {
        emergencyThreadManager.sendEmergency(emergencyType)
    }
}