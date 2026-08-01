package dev.danielkindl.ocho.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Asks for notification permission once, when a session screen opens.
 *
 * Requested here rather than at launch because that is where it can be justified:
 * the user has just started a workout, and the notification is how they will control
 * it from the lock screen.
 *
 * **Denial is not an error.** The foreground service still runs, the wake lock is
 * still held, and timing is still exact. The user simply loses the notification, so
 * nothing here blocks, retries, or nags. There is deliberately no rationale dialog:
 * a workout is already starting and interrupting it to argue about a permission
 * would be worse than going without.
 *
 * No-op below Android 13, where the permission is granted at install time.
 */
@Composable
fun RequestNotificationPermission() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Granted or not, the session is unaffected. */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@LaunchedEffect
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
