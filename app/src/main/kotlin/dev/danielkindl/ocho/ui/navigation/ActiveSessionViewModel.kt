package dev.danielkindl.ocho.ui.navigation

import androidx.lifecycle.ViewModel
import dev.danielkindl.ocho.data.session.SessionController
import dev.danielkindl.ocho.domain.model.SessionRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Tells the navigation graph whether to open straight into a running session.
 *
 * Needed because a session now outlives the screen that started it. Tapping the
 * ongoing notification, or simply reopening the app, would otherwise land on the
 * home screen with a workout still running and no way to reach it.
 */
@HiltViewModel
class ActiveSessionViewModel @Inject constructor(
    private val sessionController: SessionController,
) : ViewModel() {

    /**
     * The route of the session in progress, or null if none is running.
     *
     * Read once when the graph is first composed, not observed. Navigating on every
     * change would drag the user back to the session screen whenever they tried to
     * leave it.
     */
    fun activeSessionRoute(): String? {
        if (sessionController.snapshot.value?.isActive != true) return null
        return when (val request = sessionController.activeRequest) {
            is SessionRequest.Emom -> sessionRoute(
                request.config.totalDurationMillis,
                request.config.intervalMillis,
            )

            is SessionRequest.Tabata -> tabataSessionRoute(
                request.config.totalDurationMillis,
                request.config.workMillis,
                request.config.restMillis,
            )

            null -> null
        }
    }
}
