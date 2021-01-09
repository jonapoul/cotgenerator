package com.jonapoul.cotgenerator.di

import android.content.Context
import android.content.SharedPreferences
import com.jonapoul.common.di.IBuildResources
import com.jonapoul.common.di.IUiResources
import com.jonapoul.common.repositories.IBatteryRepository
import com.jonapoul.common.repositories.IDeviceUidRepository
import com.jonapoul.common.repositories.IGpsRepository
import com.jonapoul.common.service.CotFactory
import com.jonapoul.common.ui.main.SettingsFragment
import com.jonapoul.cotgenerator.service.GeneratorCotFactory
import com.jonapoul.cotgenerator.ui.GeneratorSettingsFragment
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(ActivityComponent::class)
abstract class BindsActivityModule {
    @Binds
    abstract fun bindsSettingsFragment(fragment: GeneratorSettingsFragment): SettingsFragment
}

@Module
@InstallIn(ApplicationComponent::class)
class ProvidesApplicationModule {
    @Singleton
    @Provides
    fun provideBuildResources(@ApplicationContext context: Context): IBuildResources {
        return GeneratorBuildResources(context)
    }

    @Singleton
    @Provides
    fun bindActivityResources(): IUiResources {
        return GeneratorUiResources()
    }

    @Provides
    fun provideCotFactory(
            prefs: SharedPreferences,
            buildResources: IBuildResources,
            deviceUidRepository: IDeviceUidRepository,
            gpsRepository: IGpsRepository,
            batteryRepository: IBatteryRepository
    ): CotFactory {
        return GeneratorCotFactory(
                prefs,
                buildResources,
                deviceUidRepository,
                gpsRepository,
                batteryRepository
        )
    }
}
