package com.jonapoul.common.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.view.Menu
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jonapoul.common.R
import com.jonapoul.common.di.IBuildResources
import com.jonapoul.common.di.IUiResources
import com.jonapoul.common.logging.LogUtils
import com.jonapoul.common.prefs.CommonPrefs
import com.jonapoul.common.prefs.getBooleanFromPair
import com.jonapoul.common.prefs.getIntFromPair
import com.jonapoul.common.repositories.IStatusRepository
import com.jonapoul.common.service.CotService
import com.jonapoul.common.service.ServiceState
import com.jonapoul.common.ui.IServiceCommunicator
import com.jonapoul.common.ui.StateViewModel
import com.jonapoul.common.utils.GenerateInt
import com.jonapoul.common.utils.MinimumVersions.FINISH_AND_REMOVE_TASK
import com.jonapoul.common.utils.MinimumVersions.IGNORE_BATTERY_OPTIMISATIONS
import com.jonapoul.common.utils.MinimumVersions.OKHTTP_SSL
import com.jonapoul.common.utils.Notify
import com.jonapoul.common.utils.VersionUtils
import com.jonapoul.common.versioncheck.GithubRelease
import com.jonapoul.common.versioncheck.UpdateChecker
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

abstract class MainActivity : AppCompatActivity(),
        EasyPermissions.PermissionCallbacks,
        SharedPreferences.OnSharedPreferenceChangeListener,
        ServiceConnection,
        IServiceCommunicator {

    protected var service: CotService? = null
    protected val viewModel: StateViewModel by viewModels()
    private val compositeDisposable = CompositeDisposable()
    protected val navController: NavController by lazy { findNavController(uiResources.navHostFragmentId) }

    @Inject
    lateinit var updateChecker: UpdateChecker

    @Inject
    lateinit var statusRepository: IStatusRepository

    @Inject
    protected lateinit var prefs: SharedPreferences

    @Inject
    protected lateinit var uiResources: IUiResources

    @Inject
    protected lateinit var buildResources: IBuildResources

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)
        if (!hasPermissions()) {
            Timber.d("Don't have permissions, requesting...")
            EasyPermissions.requestPermissions(
                    this,
                    getString(uiResources.permissionRationaleId),
                    PERMISSIONS_CODE,
                    *PERMISSIONS
            )
        } else if (isBatteryOptimised()) {
            Timber.d("Battery is optimised, requesting to disable...")
            disableBatteryOptimisation()
        } else {
            buildActivity()
        }
    }

    private fun buildActivity() {
        Timber.d("buildActivity")
        if (viewModel.activityIsBuilt) {
            Timber.d("Activity has already been built!")
            return
        }
        setContentView(uiResources.activityLayoutId)
        initialiseToolbar()
        if (VersionUtils.isAtLeast(OKHTTP_SSL)) {
            compositeDisposable.add(
                    updateChecker.fetchReleases()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    { onReleasesFetched(it) },
                                    { onReleaseFetchingFailure(it) }
                            )
            )
        }
        startCotService()
        viewModel.activityIsBuilt = true
    }

    private fun initialiseToolbar() {
        Timber.d("initialiseToolbar")
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
            setDisplayHomeAsUpEnabled(false)
        }

        val appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
        toolbar.setNavigationOnClickListener {
            navController.navigateUp() || super.onSupportNavigateUp()
        }
    }

    override fun onResume() {
        Timber.d("onResume")
        super.onResume()
        prefs.registerOnSharedPreferenceChangeListener(this)

        if (hasPermissions() && isBatteryOptimised()) {
            Timber.d("Have permissions but battery is optimised, requesting to disable...")
            disableBatteryOptimisation()
        }
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        super.onDestroy()
        service?.let {
            Timber.d("Unbinding service")
            service = null
            unbindService(this)
        }
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        compositeDisposable.clear()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Timber.d("onCreateOptionsMenu")
        menuInflater.inflate(uiResources.mainMenuId, menu)
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, grantedPermissions: List<String>) {
        Timber.d("onPermissionsGranted")
        if (grantedPermissions.size == PERMISSIONS.size) {
            Timber.d("All permissions granted!")
            buildActivity()
        } else {
            Timber.d("Not all permissions granted!")
            chastiseUserAndQuit()
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        Timber.d("Some permissions denied!")
        chastiseUserAndQuit()
    }

    @CallSuper
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        Timber.d("onSharedPreferenceChanged %s", key)
        when (key) {
            CommonPrefs.TRANSMISSION_PERIOD.key -> {
                service?.updateGpsPeriod(prefs.getIntFromPair(CommonPrefs.TRANSMISSION_PERIOD))
            }
            CommonPrefs.LOG_TO_FILE.key -> {
                if (prefs.getBooleanFromPair(CommonPrefs.LOG_TO_FILE)) {
                    LogUtils.startFileLogging(buildResources)
                } else {
                    LogUtils.stopFileLogging()
                }
            }

        }
    }

    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        Timber.d("onServiceConnected %s", name)
        service = (binder as CotService.ServiceBinder).service
        service?.initialiseFusedLocationClient()
    }

    override fun onServiceDisconnected(name: ComponentName) {
        Timber.d("onServiceDisconnected %s", name)
        service = null
    }

    override fun startService() {
        Timber.d("startService")
        service?.start()
    }

    override fun stopService() {
        Timber.d("stopService")
        service?.shutdown()
    }

    override fun isServiceNull(): Boolean {
        Timber.d("isServiceNull = %s", service == null)
        return service == null
    }

    override fun isServiceRunning(): Boolean {
        Timber.d("isServiceRunning = %s", viewModel.currentState == ServiceState.RUNNING)
        return viewModel.currentState == ServiceState.RUNNING
    }

    protected fun getRootView(): View {
        return findViewById(android.R.id.content)
    }

    private fun observeServiceStatus() {
        Timber.d("observeServiceStatus")
        statusRepository.getStatus().observe(this) { updateStatus(it) }
    }

    protected open fun updateStatus(newState: ServiceState) {
        Timber.d("Service state changed to %s", newState.name)
        viewModel.currentState = newState
        if (newState == ServiceState.ERROR) {
            Notify.red(getRootView(), "Error: ${ServiceState.errorMessage}")
        }
    }

    private fun startCotService() {
        Timber.d("startCotService")

        /* Start the service and bind to it */
        val intent = Intent(this, buildResources.serviceClass)
        startService(intent)
        bindService(intent, this, BIND_AUTO_CREATE)

        observeServiceStatus()
    }

    private fun chastiseUserAndQuit() {
        Timber.d("chastiseUserAndQuit")
        val err = getString(R.string.permissions_denied_chastisement)
        Timber.e(err)
        Notify.toast(applicationContext, err)
        closeApp()
    }

    private fun onReleasesFetched(releases: List<GithubRelease>) {
        Timber.d("onReleasesFetched")

        val latest = updateChecker.getLatestRelease(releases)
        if (latest != null && updateChecker.isNewerVersion(latest) && updateChecker.releaseIsNotIgnored(latest, prefs)) {
            val msg = "Installed = ${buildResources.versionName}\nLatest = ${latest.name}\n\n" +
                    "Would you like to visit the Github page to download it?\n\n"
            MaterialAlertDialogBuilder(this)
                    .setTitle("Update Available")
                    .setMessage(msg)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(latest.htmlUrl))
                        startActivity(browserIntent)
                    }.setNegativeButton("IGNORE") { dialog, _ ->
                        updateChecker.ignoreRelease(latest, prefs)
                        dialog.dismiss()
                    }.setNeutralButton("LATER", null)
                    .show()
        }
    }

    private fun onReleaseFetchingFailure(throwable: Throwable) {
        Timber.d("onReleaseFetchingFailure")
        Timber.d(throwable)

        if (throwable !is IOException) {
            /* Don't show an error snackbar if the exception was due to network problems.
             * If the user is off-grid I don't want to annoy them. */
            Timber.e(throwable)
            Notify.red(getRootView(), "Failed to check latest version on Github: ${throwable.message}")
        }
    }

    private fun hasPermissions(): Boolean {
        return EasyPermissions.hasPermissions(this, *PERMISSIONS)
    }

    private fun isBatteryOptimised(): Boolean {
        return if (VersionUtils.isAtLeast(IGNORE_BATTERY_OPTIMISATIONS)) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            return !powerManager.isIgnoringBatteryOptimizations(packageName)
        } else false
    }

    @SuppressLint("BatteryLife")
    private fun disableBatteryOptimisation() {
        Timber.d("disableBatteryOptimisation")
        if (!VersionUtils.isAtLeast(IGNORE_BATTERY_OPTIMISATIONS)) {
            Timber.d("Too low version to disable battery optimisations!")
            return
        }
        MaterialAlertDialogBuilder(this)
                .setTitle(R.string.battery_optimisation_dialog_title)
                .setMessage(R.string.battery_optimisation_dialog_message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    // Add a positive button which will take the user to battery settings when pressed.
                    Timber.d("Starting activity for result")
                    startActivityForResult(
                            Intent().apply {
                                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                data = Uri.parse("package:$packageName")
                            },
                            BATTERY_OPTIMISATION_CODE
                    )
                }
                .setCancelable(false)
                .setOnCancelListener { closeApp() }
                .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Timber.d("onActivityResult")
        if (requestCode == BATTERY_OPTIMISATION_CODE && isBatteryOptimised()) {
            // User clicked "DENY", so close the app and let them do it again
            Notify.toast(this, "Battery optimisation should be disabled!")
            closeApp()
        } else {
            buildActivity()
        }
    }

    private fun closeApp() {
        if (VersionUtils.isAtLeast(FINISH_AND_REMOVE_TASK)) {
            Timber.d("finishAndRemoveTask")
            finishAndRemoveTask()
        } else {
            Timber.d("finish")
            finish()
        }
    }

    companion object {
        private val BATTERY_OPTIMISATION_CODE = GenerateInt.next()
        private val PERMISSIONS_CODE = GenerateInt.next()
        val PERMISSIONS = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}
