package dev.danielkindl.ocho.data.session

import dev.danielkindl.ocho.data.audio.AudioPlayer
import dev.danielkindl.ocho.data.feedback.FeedbackTrigger
import dev.danielkindl.ocho.domain.engine.WorkoutEngine
import dev.danielkindl.ocho.domain.engine.WorkoutEngineFactory
import dev.danielkindl.ocho.domain.model.PREPARE_COUNTDOWN_SECONDS
import dev.danielkindl.ocho.domain.model.Phase
import dev.danielkindl.ocho.domain.model.SessionCue
import dev.danielkindl.ocho.domain.model.SessionRequest
import dev.danielkindl.ocho.domain.model.SessionSnapshot
import dev.danielkindl.ocho.domain.model.SessionStatus
import dev.danielkindl.ocho.domain.model.WorkoutMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the one workout that can be running at a time.
 *
 * **This class exists because a session must outlive the screen showing it.**
 * Previously each session view model created its own engine on `viewModelScope`,
 * which meant leaving the app could take the workout with it. The session now lives
 * in a singleton on its own scope, and the view models became observers.
 *
 * Everything here is mode-blind. It never learns whether it is running EMOM or
 * Tabata; [WorkoutEngineFactory] resolves that once and this class talks only to the
 * [WorkoutEngine] interface.
 */
@Singleton
class SessionController @Inject constructor(
    private val engineFactory: WorkoutEngineFactory,
    private val feedbackTrigger: FeedbackTrigger,
    private val audioPlayer: AudioPlayer,
    private val serviceLauncher: SessionServiceLauncher,
) {

    /**
     * Deliberately not a `viewModelScope`. That was the bug this release fixes: a
     * scope tied to the UI cancels the workout when the UI goes away.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _snapshot = MutableStateFlow<SessionSnapshot?>(null)

    /** The running session, or null if none has started since launch. */
    val snapshot: StateFlow<SessionSnapshot?> = _snapshot.asStateFlow()

    private var sessionJob: Job? = null
    private var engine: WorkoutEngine? = null

    /**
     * Begins a workout, replacing any session already running.
     *
     * Brings up the foreground service and takes audio focus first, so the session is
     * protected from the moment the pre-start countdown begins rather than only once
     * the engine is ticking.
     */
    fun start(request: SessionRequest) {
        sessionJob?.cancel()
        engine?.stop()

        val newEngine = engineFactory.create(request, scope)
        engine = newEngine

        _snapshot.value = SessionSnapshot(
            mode = request.mode(),
            status = SessionStatus.CountingDown,
            phase = Phase.PREPARE,
            totalDurationMillis = request.totalDurationMillis(),
        )

        audioPlayer.requestAudioFocus()
        serviceLauncher.start()

        sessionJob = scope.launch {
            runPrepareCountdown()

            launch { newEngine.snapshots.collect { snapshot -> _snapshot.value = snapshot } }

            // onSubscription guarantees this collector is registered before the
            // engine starts. Cues have no replay, so starting first would lose the
            // opening phase cue.
            newEngine.cues
                .onSubscription { newEngine.start() }
                .collect(::fireCue)
        }
    }

    /** Freezes the workout. Paused time does not count toward the total. */
    fun pause() {
        engine?.pause()
    }

    /** Resumes a paused workout without losing interval alignment. */
    fun resume() {
        engine?.resume()
    }

    /**
     * Ends the session early.
     *
     * Distinct from finishing: no completion cue, and the snapshot lands on
     * [SessionStatus.Stopped] so the screen navigates away rather than showing a
     * summary.
     */
    fun stop() {
        sessionJob?.cancel()
        sessionJob = null
        engine?.stop()
        engine = null
        _snapshot.update { it?.copy(status = SessionStatus.Stopped) }
        releaseResources()
    }

    /**
     * Counts in the workout, beeping down the last seconds.
     *
     * Lives here rather than in an engine because it is session lifecycle, not
     * workout timing, and both modes count in identically.
     */
    private suspend fun runPrepareCountdown() {
        for (secondsLeft in PREPARE_COUNTDOWN_SECONDS downTo 1) {
            _snapshot.update { it?.copy(countdownSecondsRemaining = secondsLeft) }
            feedbackTrigger.triggerCountdown { audioPlayer.playCountdownBeep() }
            delay(COUNTDOWN_TICK_MS)
        }
    }

    private suspend fun fireCue(cue: SessionCue) {
        when (cue) {
            SessionCue.IntervalBoundary ->
                feedbackTrigger.trigger(isCompletion = false) { audioPlayer.playIntervalBeep() }

            is SessionCue.PhaseChanged -> feedbackTrigger.trigger(isCompletion = false) {
                if (cue.phase == Phase.WORK) {
                    audioPlayer.playWorkStartBeep()
                } else {
                    audioPlayer.playRestStartBeep()
                }
            }

            is SessionCue.Countdown ->
                feedbackTrigger.triggerCountdown { audioPlayer.playCountdownBeep() }

            SessionCue.Completed -> {
                feedbackTrigger.trigger(isCompletion = true) { audioPlayer.playCompletionSound() }
                releaseResources()
            }
        }
    }

    /**
     * Hands back everything the session was holding.
     *
     * Audio focus in particular must be returned, or other apps stay ducked
     * indefinitely after the workout ends.
     */
    private fun releaseResources() {
        audioPlayer.abandonAudioFocus()
        serviceLauncher.stop()
    }

    private fun SessionRequest.mode(): WorkoutMode = when (this) {
        is SessionRequest.Emom -> WorkoutMode.EMOM
        is SessionRequest.Tabata -> WorkoutMode.TABATA
    }

    private fun SessionRequest.totalDurationMillis(): Long = when (this) {
        is SessionRequest.Emom -> config.totalDurationMillis
        is SessionRequest.Tabata -> config.totalDurationMillis
    }

    private companion object {
        const val COUNTDOWN_TICK_MS = 1_000L
    }
}
