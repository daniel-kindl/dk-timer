package dev.danielkindl.ocho.ui.tabata.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.danielkindl.ocho.data.session.SessionController
import dev.danielkindl.ocho.domain.model.Phase
import dev.danielkindl.ocho.domain.model.SessionRequest
import dev.danielkindl.ocho.domain.model.SessionSnapshot
import dev.danielkindl.ocho.domain.model.SessionStatus
import dev.danielkindl.ocho.domain.model.TabataConfig
import dev.danielkindl.ocho.domain.model.WorkoutMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Presents the Tabata session screen.
 *
 * Identical in shape to the EMOM view model, and for the same reason: the session
 * lives in [SessionController], outside this screen's lifecycle. The only difference
 * between the two is which [SessionRequest] they submit.
 */
@HiltViewModel
class TabataSessionViewModel @Inject constructor(
    private val sessionController: SessionController,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val totalDurationMillis: Long = checkNotNull(savedStateHandle["totalDurationMillis"])
    private val workMillis: Long = checkNotNull(savedStateHandle["workMillis"])
    private val restMillis: Long = checkNotNull(savedStateHandle["restMillis"])

    /** Current session state, seeded so the screen has something to draw immediately. */
    val uiState: StateFlow<SessionSnapshot> = sessionController.snapshot
        .filterNotNull()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SessionSnapshot(
                mode = WorkoutMode.TABATA,
                status = SessionStatus.CountingDown,
                phase = Phase.PREPARE,
                totalDurationMillis = totalDurationMillis,
            ),
        )

    init {
        // Attach to a running session rather than restarting it. See SessionViewModel.
        if (sessionController.snapshot.value?.isActive != true) {
            sessionController.start(
                SessionRequest.Tabata(
                    TabataConfig(
                        workMillis = workMillis,
                        restMillis = restMillis,
                        totalDurationMillis = totalDurationMillis,
                    )
                )
            )
        }
    }

    /** Freezes the workout. Paused time does not count toward the total. */
    fun pauseSession() = sessionController.pause()

    /** Resumes from [pauseSession] without losing phase alignment. */
    fun resumeSession() = sessionController.resume()

    /** Ends the session early, with no completion feedback. */
    fun stopSession() = sessionController.stop()
}
