package com.jonapoul.common.service

import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import androidx.lifecycle.LifecycleService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.jonapoul.common.repositories.IGpsRepository
import com.jonapoul.common.repositories.ISocketRepository
import com.jonapoul.common.repositories.IStatusRepository
import com.jonapoul.common.utils.MinimumVersions.NOTIFICATION_CHANNELS
import com.jonapoul.common.utils.VersionUtils
import timber.log.Timber
import javax.inject.Inject

abstract class CotService : LifecycleService(),
        IThreadErrorListener {
    @Inject
    lateinit var prefs: SharedPreferences

    @Inject
    lateinit var cotFactory: CotFactory

    @Inject
    lateinit var notificationGenerator: INotificationGenerator

    @Inject
    lateinit var gpsRepository: IGpsRepository

    @Inject
    lateinit var statusRepository: IStatusRepository

    @Inject
    lateinit var socketRepository: ISocketRepository

    @Inject
    lateinit var locationCallback: LocationCallback

    inner class ServiceBinder : Binder() {
        val service = this@CotService
    }

    private val binder: IBinder = ServiceBinder()

    private var updateRateSeconds = 0
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationRequest: LocationRequest? = null

    private val threadManager by lazy {
        CotThreadManager(
                prefs = prefs,
                cotFactory = cotFactory,
                errorListener = this,
                socketRepository = socketRepository
        )
    }


    override fun onBind(intent: Intent): IBinder? {
        Timber.d("onBind")
        super.onBind(intent)
        return binder
    }

    override fun onCreate() {
        Timber.d("onCreate")
        super.onCreate()
        if (VersionUtils.isAtLeast(NOTIFICATION_CHANNELS)) {
            notificationGenerator.createForegroundChannel()
            notificationGenerator.createErrorChannel()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")
        unregisterGpsUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("onStartCommand %s", intent?.action)
        val result = super.onStartCommand(intent, flags, startId)
        return when (intent?.action) {
            STOP_SERVICE -> {
                shutdown()
                result
            }
            START_SERVICE -> {
                start()
                START_STICKY
            }
            else -> {
                result
            }
        }
    }

    fun initialiseFusedLocationClient() {
        Timber.d("initialiseFusedLocationClient")
        try {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this).apply {
                lastLocation.addOnSuccessListener {
                    if (it != null) {
                        gpsRepository.setLocation(it)
                    }
                }
            }
            initialiseLocationRequest()
        } catch (e: SecurityException) {
            error(e)
        }
    }

    open fun start() {
        Timber.d("start")
        statusRepository.postStatus(ServiceState.RUNNING)
        threadManager.start()
        startForegroundService()
    }

    open fun shutdown() {
        Timber.d("shutdown")
        statusRepository.postStatus(ServiceState.STOPPED)
        threadManager.shutdown()
        stopForegroundService()
    }

    private fun error(throwable: Throwable) {
        Timber.d("error")
        Timber.e(throwable)
        notificationGenerator.showErrorNotification(throwable.message)
        shutdown()
        ServiceState.errorMessage = throwable.message
        statusRepository.postStatus(ServiceState.ERROR)
    }

    fun updateGpsPeriod(newPeriodSeconds: Int) {
        Timber.d("updateGpsPeriod %d secs", newPeriodSeconds)
        updateRateSeconds = newPeriodSeconds
        initialiseLocationRequest()
    }

    private fun startForegroundService() {
        Timber.d("startForegroundService")
        startForeground(3, notificationGenerator.getForegroundNotification())
    }

    private fun stopForegroundService() {
        Timber.d("stopForegroundService")
        stopForeground(true)
        stopSelf()
    }

    private fun initialiseLocationRequest() {
        Timber.d("initialiseLocationRequest")
        unregisterGpsUpdates()
        locationRequest = LocationRequest.create()
                .setInterval(updateRateSeconds * 1000.toLong())
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        registerGpsUpdates()
    }

    private fun registerGpsUpdates() {
        Timber.d("registerGpsUpdates")
        try {
            fusedLocationClient?.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            error(e)
        }
    }

    private fun unregisterGpsUpdates() {
        Timber.d("unregisterGpsUpdates")
        fusedLocationClient?.removeLocationUpdates(locationCallback)
    }

    override fun onThreadError(throwable: Throwable) {
        Timber.d("onThreadError")
        /* Get an error from a thread, so pass the message down to our activity and show the user */
        error(throwable)
    }

    companion object {
        private const val BASE_INTENT_ID = "com.jonapoul.common.CotService."
        const val STOP_SERVICE = "${BASE_INTENT_ID}.STOP"
        const val START_SERVICE = "${BASE_INTENT_ID}.START"
    }
}
