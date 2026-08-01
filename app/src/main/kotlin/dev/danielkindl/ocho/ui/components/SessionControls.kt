package dev.danielkindl.ocho.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * Height of the primary session control.
 *
 * 96dp rather than the usual 48dp because this is pressed mid-effort, one-handed,
 * without looking. It is contractual in the design system, not a preference.
 */
private val PRIMARY_CONTROL_HEIGHT = 96.dp

/** Floor for every other interactive target. */
private val MIN_TOUCH_TARGET = 48.dp

/** Comfortably above [MIN_TOUCH_TARGET] without competing with the primary control. */
private val SECONDARY_CONTROL_HEIGHT = 56.dp

/** `radius-2` from the token scale: controls, buttons, inputs. */
private val ControlShape = RoundedCornerShape(6.dp)

/** Glyph size in controls, per the iconography rules. */
private val CONTROL_ICON_SIZE = 20.dp

/** Gap between a control's glyph and its label. */
private val ICON_LABEL_GAP = 10.dp

/** Hairline border, matching the card treatment. */
private val CONTROL_BORDER_WIDTH = 1.dp

/** Fill opacity for a control sitting on a phase plate. */
private const val ON_PLATE_FILL = 0.18f

/** Border opacity for an outlined control on a phase plate. */
private const val ON_PLATE_BORDER = 0.35f

/**
 * The pause/resume control: the one button a user hits without looking.
 *
 * Filled rather than outlined, and [PRIMARY_CONTROL_HEIGHT] tall, so it is the
 * largest target on the screen by a wide margin.
 */
@Composable
fun PrimarySessionControl(
    label: String,
    icon: Painter,
    onPlate: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(PRIMARY_CONTROL_HEIGHT),
        shape = ControlShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = onPlate.copy(alpha = ON_PLATE_FILL),
            contentColor = onPlate,
        ),
    ) {
        Icon(painter = icon, contentDescription = null, modifier = Modifier.size(CONTROL_ICON_SIZE))
        Spacer(Modifier.width(ICON_LABEL_GAP))
        Text(text = label, style = MaterialTheme.typography.titleLarge)
    }
}

/**
 * A secondary session control — stop, done, dismiss.
 *
 * Outlined rather than filled so it never competes with the primary control for the
 * glance, while still clearing the 48dp minimum target.
 */
@Composable
fun SecondarySessionControl(
    label: String,
    onPlate: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(minHeight = MIN_TOUCH_TARGET)
            .height(SECONDARY_CONTROL_HEIGHT),
        shape = ControlShape,
        border = BorderStroke(CONTROL_BORDER_WIDTH, SolidColor(onPlate.copy(alpha = ON_PLATE_BORDER))),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = onPlate),
    ) {
        if (icon != null) {
            Icon(painter = icon, contentDescription = null, modifier = Modifier.size(CONTROL_ICON_SIZE))
            Spacer(Modifier.width(ICON_LABEL_GAP))
        }
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}

/** Gap between adjacent session controls, from the spacing scale. */
private val CONTROL_GAP = 12.dp

/** Lays out session controls with the spacing the token scale calls for. */
@Composable
fun SessionControlRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CONTROL_GAP),
    ) {
        content()
    }
}
