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

/**
 * Application-wide bindings.
 *
 * The `@Binds` half maps each domain interface to its `data/` implementation, which
 * is what keeps `domain/` and `ui/` from ever naming an Android-facing class. The
 * `@Provides` half builds the things that have no injectable constructor.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    /** Binds the DataStore-backed settings store. */
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    /** Binds the DataStore-backed EMOM preset store. */
    @Binds
    @Singleton
    abstract fun bindPresetRepository(impl: PresetRepositoryImpl): PresetRepository

    /** Binds the DataStore-backed Tabata preset store. */
    @Binds
    @Singleton
    abstract fun bindTabataPresetRepository(impl: TabataPresetRepositoryImpl): TabataPresetRepository

    /** Binds the `ToneGenerator` player. Singleton because it owns a native audio handle. */
    @Binds
    @Singleton
    abstract fun bindAudioPlayer(impl: ToneAudioPlayer): AudioPlayer

    /** Binds the GitHub Releases update source. */
    @Binds
    @Singleton
    abstract fun bindUpdateRepository(impl: UpdateRepositoryImpl): UpdateRepository

    companion object {

        /** The real system clock. Tests substitute a fake to make engine timing deterministic. */
        @Provides
        @Singleton
        fun provideClock(): Clock = SystemClock()

        /** Factory for EMOM engines; each session gets one scoped to its view model. */
        @Provides
        @Singleton
        fun provideTimerEngineFactory(clock: Clock): TimerEngineFactory =
            DefaultTimerEngineFactory(clock)

        /** Factory for Tabata engines; each session gets one scoped to its view model. */
        @Provides
        @Singleton
        fun provideTabataEngineFactory(clock: Clock): TabataEngineFactory =
            DefaultTabataEngineFactory(clock)

        /** The single preferences store shared by settings and both preset repositories. */
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
