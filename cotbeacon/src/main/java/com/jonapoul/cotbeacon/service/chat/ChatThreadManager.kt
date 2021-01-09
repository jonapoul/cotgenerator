package com.jonapoul.cotbeacon.service.chat

import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import com.jonapoul.cotbeacon.cot.ChatCursorOnTarget
import com.jonapoul.common.prefs.getBooleanFromPair
import com.jonapoul.common.repositories.IDeviceUidRepository
import com.jonapoul.common.repositories.ISocketRepository
import com.jonapoul.common.service.IThreadErrorListener
import com.jonapoul.common.service.ThreadManager
import com.jonapoul.cotbeacon.prefs.BeaconPrefs
import com.jonapoul.cotbeacon.repositories.IChatRepository
import com.jonapoul.cotbeacon.service.chat.runnables.ChatListenRunnable
import com.jonapoul.cotbeacon.service.chat.runnables.ChatSendRunnable
import timber.log.Timber
import java.util.concurrent.Executors

class ChatThreadManager(
        prefs: SharedPreferences,
        errorListener: IThreadErrorListener,
        chatRepository: IChatRepository,
        private val deviceUidRepository: IDeviceUidRepository,
        socketRepository: ISocketRepository,
) : ThreadManager(prefs, errorListener), IThreadErrorListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var listeningExecutor = Executors.newSingleThreadExecutor()
    private var sendingExecutor = Executors.newSingleThreadExecutor()
    private var listenRunnable: ChatListenRunnable? = null
    private var sendRunnable: ChatSendRunnable? = null

    private val runnableFactory = ChatRunnableFactory(
            prefs = prefs,
            threadErrorListener = this,
            socketRepository = socketRepository,
            chatRepository = chatRepository
    )

    init {
        prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun start() {
        synchronized(lock) {
            Timber.d("start")
            if (listeningExecutor.isShutdown) {
                listeningExecutor = Executors.newSingleThreadExecutor()
            }
            listenRunnable = runnableFactory.getListenRunnable(deviceUidRepository)
            listeningExecutor.execute(listenRunnable)
        }
    }

    override fun shutdown() {
        synchronized(lock) {
            Timber.d("shutdown")
            listenRunnable?.close()
            listenRunnable = null
            listeningExecutor.shutdownNow()
            sendingExecutor.shutdownNow()
        }
    }

    override fun restart() {
        synchronized(lock) {
            Timber.d("restart")
            shutdown()
            if (chatIsEnabled()) {
                start()
            }
        }
    }

    override fun isRunning(): Boolean {
        synchronized(lock) {
            return (listenRunnable != null).also {
                Timber.d("isRunning %s", it)
            }
        }
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        synchronized(lock) {
            Timber.d("onSharedPreferenceChanged %s", key)
            if (isRunning()) {
                /* If any preferences are changed during runtime, kill the threads and reload with the new settings */
                restart()
            } else if (key == BeaconPrefs.ENABLE_CHAT.key && chatIsEnabled()) {
                /* If chat was previously disabled (threads weren't running) and now it's enabled, start it up */
                start()
            }
        }
    }

    fun sendChat(chatMessage: ChatCursorOnTarget) {
        synchronized(lock) {
            Timber.d("sendChat")
            if (sendingExecutor.isShutdown) {
                sendingExecutor = Executors.newSingleThreadExecutor()
            }
            sendRunnable = runnableFactory.getSendRunnable(chatMessage)
            sendingExecutor.execute(sendRunnable)
        }
    }

    override fun onThreadError(throwable: Throwable) {
        mainHandler.post {
            errorListener.onThreadError(throwable)
        }
    }

    private fun chatIsEnabled(): Boolean {
        return prefs.getBooleanFromPair(BeaconPrefs.ENABLE_CHAT)
    }
}