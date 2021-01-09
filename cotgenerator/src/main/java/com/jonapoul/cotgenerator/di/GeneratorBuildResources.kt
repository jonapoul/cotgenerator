package com.jonapoul.cotgenerator.di

import android.content.Context
import com.jonapoul.common.di.IBuildResources
import com.jonapoul.cotgenerator.BuildConfig
import com.jonapoul.cotgenerator.R
import com.jonapoul.cotgenerator.service.GeneratorCotService
import java.text.SimpleDateFormat
import java.util.*

class GeneratorBuildResources(context: Context) : IBuildResources {
    override val appName = context.getString(R.string.app_name)
    override val platform = context.getString(R.string.app_name_all_caps)
    override val buildTimestamp: String = SimpleDateFormat("HH:mm:ss dd MMM yyyy z", Locale.ENGLISH).format(BuildConfig.BUILD_TIME)
    override val appId = BuildConfig.APPLICATION_ID
    override val versionName = BuildConfig.VERSION_NAME
    override val isDebug = BuildConfig.DEBUG
    override val serviceClass = GeneratorCotService::class.java
}
