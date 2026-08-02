package dev.danielkindl.ocho.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.danielkindl.ocho.core.format.formatElapsed
import dev.danielkindl.ocho.domain.model.Phase
import dev.danielkindl.ocho.domain.model.WorkoutPlan
import dev.danielkindl.ocho.ui.theme.phaseTheme

/** Height of the timeline strip. */
private val TIMELINE_HEIGHT = 44.dp

/** `radius-2` from the token scale. */
private val SegmentShape = RoundedCornerShape(6.dp)

/** Gap between segments, from the spacing scale. */
private val SEGMENT_GAP = 2.dp

/**
 * A single stretch of the planned session.
 *
 * @property phase which colour the segment takes.
 * @property millis how long it runs, which sets its proportional width.
 */
data class RunSegment(val phase: Phase, val millis: Long)

/**
 * Proportional preview of a configured workout, so its shape is visible before it
 * starts: an amber prepare segment, alternating work and rest, then a violet cap.
 *
 * Uses the same [phaseTheme] colours as the session screen, so the strip is a
 * literal preview of the colours the user will see rather than a separate
 * decorative palette. Segments are weighted by duration, which makes a lopsided
 * work/rest ratio visible at a glance.
 *
 * @param segments in running order. Zero-length segments are dropped, since a
 *   zero-weight child would fail to lay out.
 * @param patternLabel a short description of the structure, e.g. `8 × (20s work / 10s rest)`.
 * @param totalMillis the session length, rendered as the end time.
 */
@Composable
fun RunTimeline(
    segments: List<RunSegment>,
    patternLabel: String,
    totalMillis: Long,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val drawable = segments.filter { it.millis > 0 }
    if (drawable.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TIMELINE_HEIGHT),
            horizontalArrangement = Arrangement.spacedBy(SEGMENT_GAP),
        ) {
            drawable.forEach { segment ->
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .weight(segment.millis.toFloat())
                        .fillMaxHeight()
                        .clip(SegmentShape)
                        .background(phaseTheme(segment.phase, dark).plate)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TimelineCaption("0:00")
            TimelineCaption(patternLabel)
            TimelineCaption(totalMillis.formatElapsed())
        }
    }
}

@Composable
private fun TimelineCaption(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Draws a planned workout as the strip the setup screen previews.
 *
 * The preview used to rebuild the workout's structure itself, from a copy of the
 * engine's rules that was correct only for as long as nobody edited either side.
 * Reading the plan the session will actually run makes a preview that lies about its
 * workout unrepresentable rather than merely unlikely.
 *
 * Adds the two segments that are drawing concerns rather than workout structure: the
 * amber prepare lead, which the session controller counts in, and the violet cap that
 * gives the finish somewhere to be.
 *
 * @param prepareMillis length of the pre-start countdown.
 */
fun WorkoutPlan.toRunSegments(prepareMillis: Long): List<RunSegment> = buildList {
    add(RunSegment(Phase.PREPARE, prepareMillis))
    segments.forEach { add(RunSegment(it.phase, it.durationMillis)) }
    add(RunSegment(Phase.COMPLETE, completeCapMillis(totalDurationMillis)))
}

/**
 * Width of the trailing complete segment.
 *
 * Complete is an instant, not a duration, so it has no natural width. It gets a
 * small fixed share of the run so the violet cap stays visible on a long workout
 * without distorting the work/rest proportions on a short one.
 */
private fun completeCapMillis(totalMillis: Long): Long =
    (totalMillis / COMPLETE_CAP_DIVISOR).coerceAtLeast(MIN_COMPLETE_CAP_MILLIS)

private const val COMPLETE_CAP_DIVISOR = 24
private const val MIN_COMPLETE_CAP_MILLIS = 5_000L
