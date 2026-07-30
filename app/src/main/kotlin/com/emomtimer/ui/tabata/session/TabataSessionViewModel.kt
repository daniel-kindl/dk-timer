package com.emomtimer.ui.tabata.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emomtimer.core.format.sessionProgress
import com.emomtimer.data.audio.AudioPlayer
import com.emomtimer.data.feedback.FeedbackTrigger
import com.emomtimer.domain.engine.TabataEngineFactory
import com.emomtimer.domain.model.SessionStatus
import com.emomtimer.domain.model.TabataConfig
import com.emomtimer.domain.model.TabataEvent
import com.emomtimer.domain.model.TabataPhase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TabataSessionUiState(
    val status: SessionStatus = SessionStatus.CountingDown,
    val countdownSecondsRemaining: Int = COUNTDOWN_START_SECONDS,
    val phase: TabataPhase = TabataPhase.Work,
    val remainingInPhaseMillis: Long = 0L,
    val elapsedMillis: Long = 0L,
    val totalDurationMillis: Long = 0L,
    val currentRound: Int = 1,
    val totalRounds: Int = 0,
) {
    val progressFraction: Float
        get() = sessionProgress(elapsedMillis, totalDurationMillis)
}

private const val COUNTDOWN_START_SECONDS = 3
private const val COUNTDOWN_TICK_MS = 1_000L

@HiltViewModel
class TabataSessionViewModel @Inject constructor(
    private val engineFactory: TabataEngineFactory,
    private val feedbackTrigger: FeedbackTrigger,
    private val audioPlayer: AudioPlayer,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val totalDurationMillis: Long = checkNotNull(savedStateHandle["totalDurationMillis"])
    private val workMillis: Long = checkNotNull(savedStateHandle["workMillis"])
    private val restMillis: Long = checkNotNull(savedStateHandle["restMillis"])

    private val engine = engineFactory.create(viewModelScope)

    private val _uiState = MutableStateFlow(
        TabataSessionUiState(totalDurationMillis = totalDurationMillis)
    )
    val uiState: StateFlow<TabataSessionUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    init {
        observeEvents()
        countdownJob = viewModelScope.launch {
            for (secondsLeft in COUNTDOWN_START_SECONDS - 1 downTo 0) {
                delay(COUNTDOWN_TICK_MS)
                _uiState.update { it.copy(countdownSecondsRemaining = secondsLeft) }
            }
            beginSession()
        }
    }

    private fun beginSession() {
        _uiState.update { it.copy(status = SessionStatus.Running) }
        engine.start(TabataConfig(workMillis, restMillis, totalDurationMillis))
    }

    private fun observeEvents() {
        viewModelScope.launch {
            engine.events.collect { event ->
                when (event) {
                    is TabataEvent.Tick -> _uiState.update {
                        it.copy(
                            status = SessionStatus.Running,
                            phase = event.phase,
                            remainingInPhaseMillis = event.remainingInPhaseMillis,
                            elapsedMillis = event.elapsedMillis,
                            currentRound = event.currentRound,
                            totalRounds = event.totalRounds,
                        )
                    }

                    is TabataEvent.WorkStarted -> feedbackTrigger.trigger(
                        isCompletion = false,
                        playSound = { audioPlayer.playWorkStartBeep() },
                    )

                    is TabataEvent.RestStarted -> feedbackTrigger.trigger(
                        isCompletion = false,
                        playSound = { audioPlayer.playRestStartBeep() },
                    )

                    is TabataEvent.WorkoutCompleted -> {
                        feedbackTrigger.trigger(
                            isCompletion = true,
                            playSound = { audioPlayer.playCompletionSound() },
                        )
                        _uiState.update { it.copy(status = SessionStatus.Completed) }
                    }
                }
            }
        }
    }

    fun pauseSession() {
        engine.pause()
        _uiState.update { it.copy(status = SessionStatus.Paused) }
    }

    fun resumeSession() {
        engine.resume()
        _uiState.update { it.copy(status = SessionStatus.Running) }
    }

    fun stopSession() {
        countdownJob?.cancel()
        engine.stop()
        _uiState.update { it.copy(status = SessionStatus.Stopped) }
    }

    override fun onCleared() {
        super.onCleared()
        engine.stop()
        audioPlayer.release()
    }
}
