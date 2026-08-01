package dev.danielkindl.ocho.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Guards against losing a session in progress, whether by the STOP button or a
 * stray back gesture — easy to trigger with sweaty hands mid-workout.
 */
@Composable
fun ExitConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exit workout?") },
        text = { Text("Your progress in this session will be lost.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Exit", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
