package com.jonapoul.common.di

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.google.android.gms.location.LocationCallback
import com.jonapoul.common.presets.DatabaseMigrations
import com.jonapoul.common.presets.IPresetDao
import com.jonapoul.common.presets.PresetDatabase
import com.jonapoul.common.repositories.*
import com.jonapoul.common.repositories.impl.*
import com.jonapoul.common.service.GpsLocationCallback
import com.jonapoul.common.service.INotificationGenerator
import com.jonapoul.common.service.NotificationGenerator
import com.jonapoul.common.service.SocketFactory
import com.jonapoul.common.utils.Constants
import com.jonapoul.common.utils.MinimumVersions
import com.jonapoul.common.utils.VersionUtils
import com.jonapoul.common.versioncheck.IGithubApi
import com.jonapoul.common.versioncheck.UpdateChecker
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton


@Module
@InstallIn(ApplicationComponent::class)
abstract class BindsApplicationModule {
    @Singleton
    @Binds
    abstract fun bindGps(repository: GpsRepository): IGpsRepository

    @Singleton
    @Binds
    abstract fun bindStatus(repository: StatusRepository): IStatusRepository
}

@InstallIn(ServiceComponent::class)
@Module
class ProvidesServiceModule {
    @Provides
    fun provideNotificationGenerator(
            @ApplicationContext context: Context,
            prefs: SharedPreferences,
            buildResources: IBuildResources,
    ): INotificationGenerator {
        return NotificationGenerator(context, prefs, buildResources)
    }

    @Provides
    fun provideLocationCallback(gpsRepository: IGpsRepository): LocationCallback {
        return GpsLocationCallback(gpsRepository)
    }
}

@InstallIn(ApplicationComponent::class)
@Module
class ProvidesApplicationModule {
    @Provides
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @Singleton
    @Provides
    fun provideBattery(@ApplicationContext context: Context): IBatteryRepository {
        return BatteryRepository(context)
    }

    @Singleton
    @Provides
    fun provideDeviceUid(@ApplicationContext context: Context): IDeviceUidRepository {
        return DeviceUidRepository(context)
    }

    @Singleton
    @Provides
    fun providePreset(@ApplicationContext context: Context, presetDao: IPresetDao): IPresetRepository {
        return PresetRepository(context, presetDao)
    }

    @Singleton
    @Provides
    fun provideSocketFactory(prefs: SharedPreferences, presetRepository: IPresetRepository): SocketFactory {
        return SocketFactory(prefs, presetRepository)
    }

    @Singleton
    @Provides
    fun provideSocketRepository(socketFactory: SocketFactory): ISocketRepository {
        return SocketRepository(socketFactory)
    }

    @Provides
    fun providePresetDao(appDatabase: PresetDatabase): IPresetDao {
        return appDatabase.presetDao()
    }

    @Singleton
    @Provides
    fun providePresetDatabase(@ApplicationContext context: Context): PresetDatabase {
        return Room.databaseBuilder(context, PresetDatabase::class.java, PresetDatabase.FILENAME)
                .addMigrations(*DatabaseMigrations.allMigrations)
                .fallbackToDestructiveMigration()
                .build()
    }

    @Provides
    fun provideRetrofitClient(): Retrofit? {
        return if (VersionUtils.isAtLeast(MinimumVersions.OKHTTP_SSL)) {
            return Retrofit.Builder()
                    .baseUrl("https://api.github.com")
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build()
        } else {
            null
        }
    }

    @Provides
    fun provideGithubApi(retrofit: Retrofit?): IGithubApi? {
        return if (VersionUtils.isAtLeast(MinimumVersions.OKHTTP_SSL)) {
            retrofit?.create(IGithubApi::class.java)
        } else {
            null
        }
    }

    @Provides
    fun provideUpdateChecker(githubApi: IGithubApi?, buildResources: IBuildResources): UpdateChecker {
        return UpdateChecker(githubApi, buildResources)
    }
}
