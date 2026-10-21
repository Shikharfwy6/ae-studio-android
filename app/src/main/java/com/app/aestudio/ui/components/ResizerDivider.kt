package com.app.aestudio.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.aestudio.ui.theme.AeColors
import com.app.aestudio.utils.PointerSimulator
import com.app.aestudio.utils.simTarget

enum class DividerOrientation {
    /** A vertical bar between side-by-side panels; dragging changes widths. */
    Vertical,

    /** A horizontal bar between stacked panels; dragging changes heights. */
    Horizontal,
}

private val HitThickness = 10.dp

/**
 * Drag-to-resize handle. Reports the drag delta along its axis in dp, from either a real finger
 * ([pointerInput] + [detectDragGestures]) or the virtual trackpad (press-and-drag via [simTarget]).
 */
@Composable
fun ResizerDivider(
    orientation: DividerOrientation,
    simulator: PointerSimulator,
    key: String,
    onDelta: (Dp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val currentOnDelta by rememberUpdatedState(onDelta)
    var dragging by remember { mutableStateOf(false) }
    val vertical = orientation == DividerOrientation.Vertical
    val highlighted = dragging || simulator.hoveredKey == key
    val lineColor by animateColorAsState(
        targetValue = if (highlighted) AeColors.Accent else AeColors.Border,
        label = "dividerColor"
    )

    fun toDp(delta: Offset): Dp = with(density) { (if (vertical) delta.x else delta.y).toDp() }

    val hitModifier =
        if (vertical) Modifier.width(HitThickness).fillMaxHeight()
        else Modifier.height(HitThickness).fillMaxWidth()

    val lineThickness = if (highlighted) 2.dp else 1.dp
    val lineModifier =
        if (vertical) Modifier.width(lineThickness).fillMaxHeight()
        else Modifier.height(lineThickness).fillMaxWidth()

    Box(
        modifier = modifier
            .then(hitModifier)
            .simTarget(
                simulator = simulator,
                key = key,
                priority = 5,
                onDragStart = { dragging = true },
                onDrag = { delta, _ -> currentOnDelta(toDp(delta)) },
                onDragEnd = { dragging = false },
            )
            .pointerInput(orientation) {
                detectDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                ) { change, dragAmount ->
                    change.consume()
                    currentOnDelta(toDp(dragAmount))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(lineModifier.background(lineColor))
    }
}
