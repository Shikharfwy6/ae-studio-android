package com.app.aestudio.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.app.aestudio.ui.state.WorkspaceState
import com.app.aestudio.ui.theme.AeColors
import com.app.aestudio.ui.theme.AeMono
import kotlin.math.roundToInt

private enum class TrackpadOutcome { Tap, Move, Cancel }

/**
 * Touch surface that frames the keyboard.
 *  - drag: move the virtual cursor (delta * sensitivity)
 *  - tap: click at the cursor
 *  - hold, then drag: mouse-down drag (resize dividers, scrub the timeline, pan with Hand tool)
 * Touches that land on a keycap are consumed by the key and never reach this surface.
 */
@Composable
fun VirtualTrackpad(
    state: WorkspaceState,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val hostView = LocalView.current
    val lastKey by state.lastKey.collectAsState()
    val shape = RoundedCornerShape(10.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(AeColors.DockTop, AeColors.DockBottom)))
            .border(
                BorderStroke(1.dp, if (state.cursorPressed) AeColors.Accent else AeColors.Hairline),
                shape
            )
            .pointerInput(hostView) {
                val slop = viewConfiguration.touchSlop
                val longPressMs = viewConfiguration.longPressTimeoutMillis
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = true)
                    var total = Offset.Zero

                    // Decide within the long-press window: tap, move, or (on timeout) press-drag.
                    val outcome: TrackpadOutcome? = withTimeoutOrNull(longPressMs) {
                        var result: TrackpadOutcome? = null
                        while (result == null) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                            result = if (change == null || change.isConsumed) {
                                TrackpadOutcome.Cancel
                            } else if (change.changedToUp()) {
                                change.consume()
                                TrackpadOutcome.Tap
                            } else {
                                total += change.positionChange()
                                if (total.getDistance() > slop) TrackpadOutcome.Move else null
                            }
                        }
                        result
                    }

                    when (outcome) {
                        TrackpadOutcome.Tap -> state.click(hostView)
                        TrackpadOutcome.Cancel -> Unit
                        TrackpadOutcome.Move -> {
                            state.moveCursor(total)
                            trackPointer(down.id, pressed = false, state = state)
                        }
                        null -> {
                            state.beginPress()
                            try {
                                trackPointer(down.id, pressed = true, state = state)
                            } finally {
                                state.endPress()
                            }
                        }
                    }
                }
            }
    ) {
        TrackpadStrip(state, lastKey)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            content = content
        )
    }
}

private suspend fun AwaitPointerEventScope.trackPointer(
    id: PointerId,
    pressed: Boolean,
    state: WorkspaceState,
) {
    while (true) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == id } ?: return
        if (change.changedToUp() || !change.pressed) {
            change.consume()
            return
        }
        val delta = change.positionChange()
        if (delta != Offset.Zero) {
            if (pressed) state.dragCursorBy(delta) else state.moveCursor(delta)
            change.consume()
        }
    }
}

@Composable
private fun TrackpadStrip(state: WorkspaceState, lastKey: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Drag: move, Tap: click, Hold: drag",
            style = MaterialTheme.typography.labelSmall,
            color = AeColors.TextDisabled,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        if (LocalConfiguration.current.screenWidthDp >= 600) {
            Text(
                text = "Last key: ${lastKey ?: "none"}",
                style = MaterialTheme.typography.labelSmall,
                color = AeColors.TextSecondary,
                maxLines = 1
            )
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = if (state.fullKeyboard) "Main" else "Full",
            modifier = Modifier
                .clickable { state.fullKeyboard = !state.fullKeyboard }
                .padding(horizontal = 6.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = AeColors.Accent
        )
        SpeedButton("-") { state.sensitivity = (state.sensitivity - 0.2f).coerceAtLeast(0.6f) }
        Text(
            text = "x${"%.1f".format(state.sensitivity)}",
            modifier = Modifier.width(34.dp),
            style = AeMono,
            color = AeColors.TextPrimary,
            textAlign = TextAlign.Center
        )
        SpeedButton("+") { state.sensitivity = (state.sensitivity + 0.2f).coerceAtMost(4f) }
    }
}

@Composable
private fun SpeedButton(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier = Modifier
            .size(18.dp)
            .border(1.dp, AeColors.Hairline, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = AeMono, color = AeColors.TextPrimary)
    }
}

/**
 * The floating cursor: a ring centred on the cursor coordinates, drawn above everything
 * (zIndex 999). It has no pointer handlers, so it never blocks touches.
 */
@Composable
fun VirtualCursorOverlay(state: WorkspaceState) {
    val size = 36.dp
    val halfPx = with(LocalDensity.current) { size.toPx() / 2f }
    val pulse = remember { Animatable(1f) }

    LaunchedEffect(state.clickPulse) {
        if (state.clickPulse > 0) {
            pulse.snapTo(0f)
            pulse.animateTo(1f, tween(300, easing = LinearOutSlowInEasing))
        }
    }

    Canvas(
        Modifier
            .offset {
                IntOffset(
                    (state.cursor.x - halfPx).roundToInt(),
                    (state.cursor.y - halfPx).roundToInt()
                )
            }
            .size(size)
            .zIndex(999f)
    ) {
        if (!state.cursorVisible) return@Canvas
        val c = center
        val pressed = state.cursorPressed
        val r = 9.dp.toPx() * (if (pressed) 0.8f else 1f)
        drawCircle(
            color = if (pressed) AeColors.Accent.copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.28f),
            radius = r,
            center = c
        )
        drawCircle(
            color = if (pressed) AeColors.Accent else Color.White.copy(alpha = 0.95f),
            radius = r,
            center = c,
            style = Stroke(1.5.dp.toPx())
        )
        drawCircle(Color.White, radius = 2.dp.toPx(), center = c)

        val p = pulse.value
        if (p < 1f) {
            drawCircle(
                color = AeColors.Accent.copy(alpha = (1f - p) * 0.8f),
                radius = lerp(r, 17.dp.toPx(), p),
                center = c,
                style = Stroke(2.dp.toPx())
            )
        }
    }
}
