package dev.danielkindl.ocho.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dev.danielkindl.ocho.BuildConfig
import dev.danielkindl.ocho.core.Clock
import dev.danielkindl.ocho.core.SystemClock
import dev.danielkindl.ocho.data.audio.AudioPlayer
import dev.danielkindl.ocho.data.audio.ToneAudioPlayer
import dev.danielkindl.ocho.data.repository.PresetRepositoryImpl
import dev.danielkindl.ocho.data.repository.SettingsRepositoryImpl
import dev.danielkindl.ocho.data.repository.TabataPresetRepositoryImpl
import dev.danielkindl.ocho.data.update.UpdateRepositoryImpl
import dev.danielkindl.ocho.domain.engine.DefaultTabataEngineFactory
import dev.danielkindl.ocho.domain.engine.DefaultTimerEngineFactory
import dev.danielkindl.ocho.domain.engine.TabataEngineFactory
import dev.danielkindl.ocho.domain.engine.TimerEngineFactory
import dev.danielkindl.ocho.domain.model.SemVer
import dev.danielkindl.ocho.domain.model.UpdateChannel
import dev.danielkindl.ocho.domain.model.UpdateConfig
import dev.danielkindl.ocho.domain.repository.PresetRepository
import dev.danielkindl.ocho.domain.repository.SettingsRepository
import dev.danielkindl.ocho.domain.repository.TabataPresetRepository
import dev.danielkindl.ocho.domain.repository.UpdateRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindPresetRepository(impl: PresetRepositoryImpl): PresetRepository

    @Binds
    @Singleton
    abstract fun bindTabataPresetRepository(impl: TabataPresetRepositoryImpl): TabataPresetRepository

    @Binds
    @Singleton
    abstract fun bindAudioPlayer(impl: ToneAudioPlayer): AudioPlayer

    @Binds
    @Singleton
    abstract fun bindUpdateRepository(impl: UpdateRepositoryImpl): UpdateRepository

    companion object {

        @Provides
        @Singleton
        fun provideClock(): Clock = SystemClock()

        @Provides
        @Singleton
        fun provideTimerEngineFactory(clock: Clock): TimerEngineFactory =
            DefaultTimerEngineFactory(clock)

        @Provides
        @Singleton
        fun provideTabataEngineFactory(clock: Clock): TabataEngineFactory =
            DefaultTabataEngineFactory(clock)

        @Provides
        @Singleton
        fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            context.dataStore

        /**
         * The only place `BuildConfig` is read.
         *
         * Keeps the generated Android class out of the domain and data layers, which
         * work with the plain [UpdateConfig] value instead. The fields are set per
         * build type in `app/build.gradle.kts`.
         */
        @Provides
        @Singleton
        fun provideUpdateConfig(): UpdateConfig = UpdateConfig(
            repoSlug = BuildConfig.UPDATE_REPO,
            channel = UpdateChannel.fromId(BuildConfig.UPDATE_CHANNEL),
            installedVersion = SemVer.parse(BuildConfig.VERSION_NAME),
        )
    }
}
