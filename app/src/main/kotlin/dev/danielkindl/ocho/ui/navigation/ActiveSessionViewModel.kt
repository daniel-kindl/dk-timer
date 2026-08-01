package dev.danielkindl.ocho.ui.navigation

import androidx.lifecycle.ViewModel
import dev.danielkindl.ocho.data.session.SessionController
import dev.danielkindl.ocho.domain.model.SessionRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Tells the navigation graph whether to open straight into a running session.
 *
 * Needed because a session outlives the screen that started it. Tapping the ongoing
 * notification, or simply reopening the app, would otherwise land on the home screen
 * with a workout still running and no way to reach it.
 */
@HiltViewModel
class ActiveSessionViewModel @Inject constructor(
    private val sessionController: SessionController,
) : ViewModel() {

    /**
     * The request behind the session in progress, or null if none is running.
     *
     * Returns the request rather than a route so this class stays free of route
     * strings, which belong to the graph. Read once when the graph is first composed
     * rather than observed: navigating on every change would drag the user back to
     * the session screen whenever they tried to leave it.
     */
    fun activeSessionRequest(): SessionRequest? {
        if (sessionController.snapshot.value?.isActive != true) return null
        return sessionController.activeRequest
    }
}
