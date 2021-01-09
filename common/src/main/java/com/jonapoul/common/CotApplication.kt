package com.jonapoul.common

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import com.jonapoul.common.di.IBuildResources
import com.jonapoul.common.logging.*
import com.jonapoul.common.prefs.CommonPrefs
import com.jonapoul.sharedprefs.getBooleanFromPair
import timber.log.Timber
import javax.inject.Inject

open class CotApplication : MultiDexApplication() {
    @Inject
    lateinit var prefs: SharedPreferences

    @Inject
    lateinit var buildResources: IBuildResources

    override fun onCreate() {
        super.onCreate()

        /* Set night mode */
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        if (BuildConfig.DEBUG) {
            /* Debug builds only */
            Timber.plant(DebugTree())
            Timber.d("Planted debug tree")
        } else {
            /* Release builds */
            Timber.plant(ReleaseTree())
            Timber.d("Planted debug tree")
        }
        if (prefs.getBooleanFromPair(CommonPrefs.LOG_TO_FILE)) {
            /* If the user has configured file logging */
            LogUtils.startFileLogging(buildResources)
            Timber.d("Planted file logging tree")
        }
    }
}