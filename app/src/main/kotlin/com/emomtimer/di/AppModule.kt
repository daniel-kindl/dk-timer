package com.emomtimer.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.emomtimer.core.Clock
import com.emomtimer.core.SystemClock
import com.emomtimer.data.audio.AudioPlayer
import com.emomtimer.data.audio.ToneAudioPlayer
import com.emomtimer.data.repository.PresetRepositoryImpl
import com.emomtimer.data.repository.SettingsRepositoryImpl
import com.emomtimer.data.repository.TabataPresetRepositoryImpl
import com.emomtimer.data.update.UpdateRepositoryImpl
import com.emomtimer.domain.engine.DefaultTabataEngineFactory
import com.emomtimer.domain.engine.DefaultTimerEngineFactory
import com.emomtimer.domain.engine.TabataEngineFactory
import com.emomtimer.domain.engine.TimerEngineFactory
import com.emomtimer.domain.repository.PresetRepository
import com.emomtimer.domain.repository.SettingsRepository
import com.emomtimer.domain.repository.TabataPresetRepository
import com.emomtimer.domain.repository.UpdateRepository
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
    }
}
