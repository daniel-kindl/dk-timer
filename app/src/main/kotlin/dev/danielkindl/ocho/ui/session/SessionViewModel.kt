package dev.danielkindl.ocho.ui.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.danielkindl.ocho.data.session.SessionController
import dev.danielkindl.ocho.domain.model.Phase
import dev.danielkindl.ocho.domain.model.SessionRequest
import dev.danielkindl.ocho.domain.model.SessionSnapshot
import dev.danielkindl.ocho.domain.model.SessionStatus
import dev.danielkindl.ocho.domain.model.TimerConfig
import dev.danielkindl.ocho.domain.model.WorkoutMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Presents the EMOM session screen.
 *
 * Deliberately thin. It no longer owns an engine: the session lives in
 * [SessionController], on a scope that outlives this view model, which is what lets
 * a workout survive the screen being destroyed or the app being backgrounded.
 *
 * Note the absence of `onCleared`. Releasing the session there would defeat the whole
 * arrangement, so a session ends only on an explicit stop or on completion.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionController: SessionController,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val totalDurationMillis: Long =
        checkNotNull(savedStateHandle["totalDurationMillis"])
    private val intervalMillis: Long =
        checkNotNull(savedStateHandle["intervalMillis"])

    /** Current session state, seeded so the screen has something to draw immediately. */
    val uiState: StateFlow<SessionSnapshot> = sessionController.snapshot
        .filterNotNull()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SessionSnapshot(
                mode = WorkoutMode.EMOM,
                status = SessionStatus.CountingDown,
                phase = Phase.PREPARE,
                totalDurationMillis = totalDurationMillis,
            ),
        )

    init {
        // Only start if nothing is already running. Returning to this screen while a
        // workout is in progress, from the notification say, must attach to that
        // session rather than restart it from zero.
        if (sessionController.snapshot.value?.isActive != true) {
            sessionController.start(
                SessionRequest.Emom(
                    TimerConfig(
                        intervalMillis = intervalMillis,
                        totalDurationMillis = totalDurationMillis,
                    )
                )
            )
        }
    }

    /** Freezes the workout. Paused time does not count toward the total. */
    fun pauseSession() = sessionController.pause()

    /** Resumes from [pauseSession] without losing interval alignment. */
    fun resumeSession() = sessionController.resume()

    /** Ends the session early, with no completion feedback. */
    fun stopSession() = sessionController.stop()
}
