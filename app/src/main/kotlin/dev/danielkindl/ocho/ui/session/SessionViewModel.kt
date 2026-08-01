package dev.danielkindl.ocho.ui.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.danielkindl.ocho.core.format.sessionProgress
import dev.danielkindl.ocho.data.audio.AudioPlayer
import dev.danielkindl.ocho.data.feedback.FeedbackTrigger
import dev.danielkindl.ocho.domain.engine.TimerEngineFactory
import dev.danielkindl.ocho.domain.model.SessionStatus
import dev.danielkindl.ocho.domain.model.TimerConfig
import dev.danielkindl.ocho.domain.model.TimerEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Everything the EMOM session screen renders.
 *
 * @property status where the session is in its lifecycle.
 * @property countdownSecondsRemaining seconds left in the pre-start countdown;
 *   meaningful only while [status] is [SessionStatus.CountingDown].
 * @property currentRound 1-indexed interval in progress.
 * @property totalRounds intervals this workout will run.
 * @property elapsedMillis time worked, excluding time spent paused.
 * @property remainingInIntervalMillis time until the next beep — the large numeral.
 * @property totalDurationMillis the configured workout length.
 */
data class SessionUiState(
    val status: SessionStatus = SessionStatus.CountingDown,
    val countdownSecondsRemaining: Int = COUNTDOWN_START_SECONDS,
    val currentRound: Int = 1,
    val totalRounds: Int = 0,
    val elapsedMillis: Long = 0L,
    val remainingInIntervalMillis: Long = 0L,
    val totalDurationMillis: Long = 0L,
) {
    /** Overall completion from 0f to 1f, for the progress bar. */
    val progressFraction: Float
        get() = sessionProgress(elapsedMillis, totalDurationMillis)
}

private const val COUNTDOWN_START_SECONDS = 3
private const val COUNTDOWN_TICK_MS = 1_000L

/**
 * Runs one EMOM session: owns the engine, translates its events into UI state, and
 * fires sound and vibration at the right moments.
 *
 * Configuration arrives through `SavedStateHandle` from the navigation route, so a
 * rotation mid-workout rebuilds the screen without disturbing the running engine.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val timerEngineFactory: TimerEngineFactory,
    private val feedbackTrigger: FeedbackTrigger,
    private val audioPlayer: AudioPlayer,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val totalDurationMillis: Long =
        checkNotNull(savedStateHandle["totalDurationMillis"])
    private val intervalMillis: Long =
        checkNotNull(savedStateHandle["intervalMillis"])

    private val timerEngine = timerEngineFactory.create(viewModelScope)

    private val _uiState = MutableStateFlow(
        SessionUiState(totalDurationMillis = totalDurationMillis)
    )

    /** Current session state, driven by engine events. */
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

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
        timerEngine.start(
            TimerConfig(
                intervalMillis = intervalMillis,
                totalDurationMillis = totalDurationMillis,
            )
        )
    }

    private fun observeEvents() {
        viewModelScope.launch {
            timerEngine.events.collect { event ->
                when (event) {
                    is TimerEvent.Tick -> _uiState.update {
                        it.copy(
                            status = SessionStatus.Running,
                            elapsedMillis = event.elapsedMillis,
                            remainingInIntervalMillis = event.remainingInInterval,
                            currentRound = event.currentInterval,
                            totalRounds = event.totalIntervals,
                        )
                    }

                    is TimerEvent.IntervalCompleted -> feedbackTrigger.trigger(isCompletion = false) {
                        audioPlayer.playIntervalBeep()
                    }

                    is TimerEvent.WorkoutCompleted -> {
                        feedbackTrigger.trigger(isCompletion = true) { audioPlayer.playCompletionSound() }
                        _uiState.update { it.copy(status = SessionStatus.Completed) }
                    }
                }
            }
        }
    }

    /** Freezes the workout. Paused time does not count toward the total. */
    fun pauseSession() {
        timerEngine.pause()
        _uiState.update { it.copy(status = SessionStatus.Paused) }
    }

    /** Resumes from [pauseSession] without losing interval alignment. */
    fun resumeSession() {
        timerEngine.resume()
        _uiState.update { it.copy(status = SessionStatus.Running) }
    }

    /**
     * Ends the session early. Distinct from completing it: no completion feedback,
     * and the screen navigates away instead of showing the summary.
     */
    fun stopSession() {
        countdownJob?.cancel()
        timerEngine.stop()
        _uiState.update { it.copy(status = SessionStatus.Stopped) }
    }

    override fun onCleared() {
        super.onCleared()
        timerEngine.stop()
        audioPlayer.release()
    }
}
