package dev.danielkindl.ocho.ui.tabata.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.danielkindl.ocho.core.format.sessionProgress
import dev.danielkindl.ocho.data.audio.AudioPlayer
import dev.danielkindl.ocho.data.feedback.FeedbackTrigger
import dev.danielkindl.ocho.domain.engine.TabataEngineFactory
import dev.danielkindl.ocho.domain.model.SessionStatus
import dev.danielkindl.ocho.domain.model.TabataConfig
import dev.danielkindl.ocho.domain.model.TabataEvent
import dev.danielkindl.ocho.domain.model.TabataPhase
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
 * Everything the Tabata session screen renders.
 *
 * @property status where the session is in its lifecycle.
 * @property countdownSecondsRemaining seconds left in the pre-start countdown;
 *   meaningful only while [status] is [SessionStatus.CountingDown].
 * @property phase work or rest; also selects the full-screen background colour.
 * @property remainingInPhaseMillis time until the phase flips — the large numeral.
 * @property elapsedMillis time worked, excluding time spent paused.
 * @property totalDurationMillis the configured workout length.
 * @property currentRound 1-indexed round; a round begins at each work phase.
 * @property totalRounds rounds this workout will run.
 */
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
    /** Overall completion from 0f to 1f, for the progress bar. */
    val progressFraction: Float
        get() = sessionProgress(elapsedMillis, totalDurationMillis)
}

private const val COUNTDOWN_START_SECONDS = 3
private const val COUNTDOWN_TICK_MS = 1_000L

/**
 * Runs one Tabata session. Mirrors `SessionViewModel`, with phase transitions
 * driving both the beep pitch and the background colour.
 */
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
    /** Current session state, driven by engine events. */
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

    /** Freezes the workout. Paused time does not count toward the total. */
    fun pauseSession() {
        engine.pause()
        _uiState.update { it.copy(status = SessionStatus.Paused) }
    }

    /** Resumes from [pauseSession] without losing phase alignment. */
    fun resumeSession() {
        engine.resume()
        _uiState.update { it.copy(status = SessionStatus.Running) }
    }

    /**
     * Ends the session early. Distinct from completing it: no completion feedback,
     * and the screen navigates away instead of showing the summary.
     */
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
