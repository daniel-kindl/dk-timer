package dev.danielkindl.ocho.ui.components

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.danielkindl.ocho.domain.model.SessionStatus

/**
 * Shared session-screen lifecycle: keeps the screen on for as long as this composable is in
 * composition, navigates away once [status] reaches [SessionStatus.Stopped], and intercepts the
 * back gesture (while a session is running or paused) behind an exit-confirmation dialog.
 *
 * [content] receives `onRequestExit`, which the screen should wire to its own STOP controls
 * so they share the same confirmation flow as the back gesture.
 */
@Composable
fun SessionLifecycleScaffold(
    status: SessionStatus,
    onSessionFinished: () -> Unit,
    onStopSession: () -> Unit,
    content: @Composable (onRequestExit: () -> Unit) -> Unit,
) {
    val activity = LocalContext.current as Activity
    DisposableEffect(Unit) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Navigate back only on an explicit stop; Completed shows its own summary first
    LaunchedEffect(status) {
        if (status == SessionStatus.Stopped) {
            onSessionFinished()
        }
    }

    var showExitConfirm by rememberSaveable { mutableStateOf(false) }
    val canExit = status == SessionStatus.Running || status == SessionStatus.Paused

    BackHandler(enabled = canExit) { showExitConfirm = true }

    if (showExitConfirm) {
        ExitConfirmDialog(
            onConfirm = {
                showExitConfirm = false
                onStopSession()
            },
            onDismiss = { showExitConfirm = false },
        )
    }

    content { showExitConfirm = true }
}
