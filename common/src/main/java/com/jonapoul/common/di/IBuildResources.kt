package com.jonapoul.common.di

import com.jonapoul.common.service.CotService

interface IBuildResources {
    val appName: String
    val platform: String
    val buildTimestamp: String
    val appId: String
    val versionName: String
    val isDebug: Boolean
    val serviceClass: Class<out CotService>
}
