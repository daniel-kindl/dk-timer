package dev.danielkindl.ocho.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.danielkindl.ocho.R
import dev.danielkindl.ocho.ui.theme.ErrorSurface
import dev.danielkindl.ocho.ui.theme.OnErrorSurface

/** `radius-3` from the token scale: callouts and error plates. */
private val PlateShape = RoundedCornerShape(8.dp)

/**
 * The app's only way of showing an error.
 *
 * Every signal colour in this design system is spent on session phases, so red can
 * no longer mean "wrong" on its own. The separation is structural instead of
 * chromatic: a phase colour is only ever a full-bleed background, and an error is
 * only ever this tinted plate sitting *inside* the layout.
 *
 * That rule has a hard consequence worth stating: **a red error plate must never
 * appear over a red work screen.** An error raised mid-session waits for the next
 * rest interval or surfaces in the ongoing notification. The running clock is never
 * covered.
 *
 * No border, and no coloured left-accent bar — the tinted surface does the work.
 *
 * @param message states the cause and the next move. Never "something went wrong".
 * @param actionLabel optional recovery action, phrased as a verb.
 */
@Composable
fun ErrorPlate(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(PlateShape)
            .background(ErrorSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_octagon_alert),
            contentDescription = null,
            tint = OnErrorSurface,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = OnErrorSurface,
            )
            if (actionLabel != null && onAction != null) {
                TextButton(
                    onClick = onAction,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 0.dp,
                        vertical = 4.dp,
                    ),
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = OnErrorSurface,
                    )
                }
            }
        }
    }
}
