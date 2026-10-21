package com.app.aestudio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.app.aestudio.ui.state.LayerKind
import com.app.aestudio.ui.state.TIMELINE_DURATION_SECONDS
import com.app.aestudio.ui.state.TIMELINE_FPS
import com.app.aestudio.ui.state.TimelineLayer
import com.app.aestudio.ui.state.Tool
import com.app.aestudio.ui.state.WorkspaceState
import com.app.aestudio.ui.theme.AeColors
import com.app.aestudio.ui.theme.AeMono
import com.app.aestudio.utils.simClickable
import com.app.aestudio.utils.simTarget
import kotlin.math.min
import kotlin.math.roundToInt

private const val COMP_W = 1920f
private const val COMP_H = 1080f
private val RowHeight = 22.dp
private val RulerHeight = 20.dp

private fun LayerKind.color(): Color = when (this) {
    LayerKind.Text -> AeColors.LayerText
    LayerKind.Image -> AeColors.LayerImage
    LayerKind.Solid -> AeColors.LayerSolid
}

/** Center column: composition viewport on top, timeline at the bottom, draggable divider between. */
@Composable
fun CenterComposition(state: WorkspaceState, modifier: Modifier = Modifier) {
    // Playback clock: advances the playhead by real frame time while playing.
    LaunchedEffect(state.isPlaying) {
        if (state.isPlaying) {
            var last = androidx.compose.runtime.withFrameNanos { it }
            while (true) {
                val now = androidx.compose.runtime.withFrameNanos { it }
                state.advance((now - last) / 1_000_000_000f)
                last = now
            }
        }
    }

    BoxWithConstraints(modifier) {
        val maxTimeline = maxHeight - 90.dp
        val timelineHeight = state.timelineHeight.coerceIn(
            WorkspaceState.MIN_TIMELINE_HEIGHT,
            maxOf(WorkspaceState.MIN_TIMELINE_HEIGHT, maxTimeline)
        )
        Column(Modifier.fillMaxSize()) {
            CompositionViewport(state, Modifier.weight(1f).fillMaxWidth())
            ResizerDivider(
                orientation = DividerOrientation.Horizontal,
                simulator = state.simulator,
                key = "divider.timeline",
                onDelta = { state.resizeTimeline(it, maxTimeline) }
            )
            TimelinePanel(state, Modifier.fillMaxWidth().height(timelineHeight))
        }
    }
}

// ---- Viewport --------------------------------------------------------------------------

private fun fitScale(size: Size): Float =
    min((size.width - 24f) / COMP_W, (size.height - 24f) / COMP_H).coerceAtLeast(0.01f)

private fun compOrigin(size: Size, scale: Float, pan: Offset): Offset =
    Offset((size.width - COMP_W * scale) / 2f + pan.x, (size.height - COMP_H * scale) / 2f + pan.y)

private fun easeInOut(t: Float): Float = t * t * (3f - 2f * t)

/** Layer bounds in composition space (1920x1080), or null when the layer is not on screen at [time]. */
private fun layerFrame(layers: List<TimelineLayer>, layer: TimelineLayer, time: Float): Rect? {
    if (time < layer.inPoint || time > layer.outPoint) return null
    val ordinal = layers.filter { it.kind == layer.kind }.indexOf(layer).coerceAtLeast(0)
    return when (layer.kind) {
        LayerKind.Solid -> Rect(0f, 0f, COMP_W, COMP_H)
        LayerKind.Image -> {
            val p = easeInOut((time / TIMELINE_DURATION_SECONDS).coerceIn(0f, 1f))
            val cx = 520f + (1400f - 520f) * p
            val cy = 400f + ordinal * 70f
            Rect(cx - 140f, cy - 140f, cx + 140f, cy + 140f)
        }
        LayerKind.Text -> {
            val cy = 800f + ordinal * 130f
            Rect(360f, cy - 60f, 1560f, cy + 60f)
        }
    }
}

private fun layerText(layer: TimelineLayer): String =
    if (layer.id == 1) "Hello, Motion" else layer.name

private fun DrawScope.drawLayerContent(
    layer: TimelineLayer,
    frame: Rect,
    origin: Offset,
    scale: Float,
    time: Float,
    measurer: TextMeasurer,
) {
    val topLeft = origin + Offset(frame.left * scale, frame.top * scale)
    val sz = Size(frame.width * scale, frame.height * scale)
    when (layer.kind) {
        LayerKind.Solid -> drawRect(
            brush = Brush.verticalGradient(listOf(Color(0xFF1B2748), Color(0xFF0E1428))),
            topLeft = topLeft,
            size = sz
        )
        LayerKind.Image -> {
            val c = origin + frame.center * scale
            val r = sz.width / 2f
            drawCircle(
                brush = Brush.linearGradient(listOf(AeColors.LayerImage, AeColors.Accent)),
                radius = r,
                center = c
            )
            drawCircle(AeColors.CompBackground, radius = r * 0.55f, center = c)
            drawCircle(Color.White.copy(alpha = 0.9f), radius = r * 0.18f, center = c)
        }
        LayerKind.Text -> {
            val alpha = ((time - layer.inPoint) / 0.6f).coerceIn(0f, 1f)
            val style = TextStyle(
                fontSize = (96f * scale).toSp(),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
            val layout = measurer.measure(layerText(layer), style)
            val c = origin + frame.center * scale
            drawText(
                textLayoutResult = layout,
                color = Color.White,
                topLeft = Offset(c.x - layout.size.width / 2f, c.y - layout.size.height / 2f),
                alpha = alpha
            )
        }
    }
}

private fun DrawScope.drawSelection(frame: Rect, origin: Offset, scale: Float) {
    val tl = origin + Offset(frame.left * scale, frame.top * scale)
    val sz = Size(frame.width * scale, frame.height * scale)
    drawRect(AeColors.Accent, tl, sz, style = Stroke(1.5.dp.toPx()))
    val h = 4.dp.toPx()
    val xs = floatArrayOf(tl.x, tl.x + sz.width / 2f, tl.x + sz.width)
    val ys = floatArrayOf(tl.y, tl.y + sz.height / 2f, tl.y + sz.height)
    for (xi in xs.indices) for (yi in ys.indices) {
        if (xi == 1 && yi == 1) continue
        val p = Offset(xs[xi] - h, ys[yi] - h)
        drawRect(Color.White, p, Size(h * 2, h * 2))
        drawRect(AeColors.Accent, p, Size(h * 2, h * 2), style = Stroke(1.dp.toPx()))
    }
}

private fun handleViewportTap(state: WorkspaceState, local: Offset, viewport: Size) {
    when (state.activeTool) {
        Tool.Zoom -> state.zoomBy(1.25f)
        Tool.Type -> state.addLayer(LayerKind.Text)
        Tool.Hand -> state.message("Hand tool: drag to pan")
        else -> {
            val scale = fitScale(viewport) * state.compZoom
            val origin = compOrigin(viewport, scale, state.viewportPan)
            val p = Offset((local.x - origin.x) / scale, (local.y - origin.y) / scale)
            val t = state.playhead
            val hit = state.layers.firstOrNull { l ->
                l.kind != LayerKind.Solid && layerFrame(state.layers, l, t)?.contains(p) == true
            } ?: state.layers.firstOrNull { l ->
                l.kind == LayerKind.Solid && layerFrame(state.layers, l, t)?.contains(p) == true
            }
            state.selectLayer(hit?.id)
        }
    }
}

@Composable
private fun CompositionViewport(state: WorkspaceState, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    var viewport by remember { mutableStateOf(IntSize.Zero) }

    fun tapAt(local: Offset) = handleViewportTap(state, local, viewport.toSize())

    Box(
        modifier = modifier
            .background(AeColors.Pasteboard)
            .clipToBounds()
            .onSizeChanged { viewport = it }
            .simTarget(
                simulator = state.simulator,
                key = "viewport",
                onTap = { tapAt(it) },
                onDrag = { delta, _ -> if (state.activeTool == Tool.Hand) state.panBy(delta) }
            )
            .pointerInput(Unit) { detectTapGestures { tapAt(it) } }
            .pointerInput(Unit) {
                detectDragGestures { change, delta ->
                    if (state.activeTool == Tool.Hand) {
                        change.consume()
                        state.panBy(delta)
                    }
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val scale = fitScale(size) * state.compZoom
            val origin = compOrigin(size, scale, state.viewportPan)
            val compSize = Size(COMP_W * scale, COMP_H * scale)
            val t = state.playhead
            val layers = state.layers

            drawRect(AeColors.CompBackground, topLeft = origin, size = compSize)
            clipRect(origin.x, origin.y, origin.x + compSize.width, origin.y + compSize.height) {
                for (i in layers.indices.reversed()) {
                    val layer = layers[i]
                    val frame = layerFrame(layers, layer, t) ?: continue
                    drawLayerContent(layer, frame, origin, scale, t, measurer)
                }
            }
            drawRect(AeColors.Hairline, topLeft = origin, size = compSize, style = Stroke(1.dp.toPx()))

            val selected = layers.firstOrNull { it.id == state.selectedLayerId }
            val selectedFrame = selected?.let { layerFrame(layers, it, t) }
            if (selectedFrame != null) drawSelection(selectedFrame, origin, scale)
        }

        // Footer: fit button, zoom readout, composition info, timecode
        val percent = (fitScale(viewport.toSize()) * state.compZoom * 100f).roundToInt()
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(22.dp)
                .background(AeColors.PanelHeader.copy(alpha = 0.92f))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val fitKey = "viewport.fit"
            Text(
                text = "Fit",
                modifier = Modifier
                    .background(if (state.simulator.hoveredKey == fitKey) AeColors.PanelRaised else Color.Transparent)
                    .simClickable(state.simulator, fitKey) { state.fitViewport() }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = AeColors.Accent
            )
            Spacer(Modifier.width(8.dp))
            Text("$percent%", style = AeMono.copy(fontSize = 10.sp), color = AeColors.TextSecondary)
            Text(
                text = "Main_Comp  1920 x 1080",
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                style = MaterialTheme.typography.labelSmall,
                color = AeColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(10.dp))
            Text(state.timecode(), style = AeMono.copy(fontSize = 10.sp), color = AeColors.Accent)
        }
    }
}

// ---- Timeline --------------------------------------------------------------------------

@Composable
private fun TimelinePanel(state: WorkspaceState, modifier: Modifier) {
    val scroll = rememberScrollState()
    Column(modifier.background(AeColors.Panel)) {
        TransportBar(state)
        Row(Modifier.fillMaxWidth().weight(1f)) {
            Column(
                Modifier
                    .width(if (LocalConfiguration.current.screenWidthDp >= 600) 132.dp else 84.dp)
                    .fillMaxHeight()
                    .background(AeColors.PanelHeader)
            ) {
                Box(
                    Modifier.fillMaxWidth().height(RulerHeight).padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text("Layer name", style = MaterialTheme.typography.labelSmall, color = AeColors.TextDisabled)
                }
                Column(Modifier.weight(1f).verticalScroll(scroll)) {
                    state.layers.forEach { LayerNameCell(state, it) }
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                Column(Modifier.fillMaxSize()) {
                    TimeRuler(state)
                    Column(Modifier.weight(1f).verticalScroll(scroll)) {
                        state.layers.forEach { LayerTrackRow(state, it) }
                    }
                }
                PlayheadOverlay(state)
            }
        }
    }
}

@Composable
private fun TransportBar(state: WorkspaceState) {
    val sim = state.simulator
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .background(AeColors.PanelHeader)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TransportButton(state, "timeline.start", TransportGlyph.Start) { state.seek(0f) }
        Spacer(Modifier.width(4.dp))
        TransportButton(
            state, "timeline.play",
            if (state.isPlaying) TransportGlyph.Pause else TransportGlyph.Play
        ) { state.togglePlay() }
        Spacer(Modifier.width(8.dp))
        Text(state.timecode(), style = AeMono, color = AeColors.Accent)
        Spacer(Modifier.width(8.dp))
        Text(
            "frame ${state.frame} of ${state.totalFrames}",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            color = AeColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "${state.layers.size} layers",
            style = MaterialTheme.typography.labelSmall,
            color = AeColors.TextSecondary
        )
    }
}

private enum class TransportGlyph { Start, Play, Pause }

@Composable
private fun TransportButton(state: WorkspaceState, key: String, glyph: TransportGlyph, onClick: () -> Unit) {
    val hovered = state.simulator.hoveredKey == key
    Box(
        Modifier
            .size(22.dp)
            .background(if (hovered) AeColors.PanelRaised else Color.Transparent)
            .simClickable(state.simulator, key, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(10.dp)) {
            val w = size.width
            val h = size.height
            when (glyph) {
                TransportGlyph.Play -> drawPath(
                    Path().apply { moveTo(w * 0.15f, 0f); lineTo(w, h / 2f); lineTo(w * 0.15f, h); close() },
                    AeColors.TextPrimary
                )
                TransportGlyph.Pause -> {
                    drawRect(AeColors.TextPrimary, Offset(w * 0.12f, 0f), Size(w * 0.28f, h))
                    drawRect(AeColors.TextPrimary, Offset(w * 0.6f, 0f), Size(w * 0.28f, h))
                }
                TransportGlyph.Start -> {
                    drawRect(AeColors.TextPrimary, Offset(0f, 0f), Size(w * 0.2f, h))
                    drawPath(
                        Path().apply { moveTo(w, 0f); lineTo(w * 0.25f, h / 2f); lineTo(w, h); close() },
                        AeColors.TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun LayerNameCell(state: WorkspaceState, layer: TimelineLayer) {
    val key = "layer.name.${layer.id}"
    val selected = state.selectedLayerId == layer.id
    val hovered = state.simulator.hoveredKey == key
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .background(
                when {
                    selected -> AeColors.AccentSoft
                    hovered -> AeColors.PanelRaised
                    else -> Color.Transparent
                }
            )
            .simClickable(state.simulator, key) { state.selectLayer(layer.id) }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).background(layer.kind.color()))
        Spacer(Modifier.width(6.dp))
        Text(
            text = layer.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = AeColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (layer.effects.isNotEmpty()) {
            Text("fx", style = MaterialTheme.typography.labelSmall, color = AeColors.Accent)
        }
    }
}

@Composable
private fun LayerTrackRow(state: WorkspaceState, layer: TimelineLayer) {
    val selected = state.selectedLayerId == layer.id
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .background(if (selected) AeColors.AccentSoft else Color.Transparent)
            .simClickable(state.simulator, "layer.track.${layer.id}") { state.selectLayer(layer.id) }
    ) {
        val x0 = size.width * layer.inPoint / TIMELINE_DURATION_SECONDS
        val x1 = size.width * layer.outPoint / TIMELINE_DURATION_SECONDS
        drawRoundRect(
            color = layer.kind.color().copy(alpha = if (selected) 0.9f else 0.55f),
            topLeft = Offset(x0, 3.dp.toPx()),
            size = Size((x1 - x0).coerceAtLeast(2f), size.height - 6.dp.toPx()),
            cornerRadius = CornerRadius(3.dp.toPx())
        )
        drawLine(AeColors.Border, Offset(0f, size.height), Offset(size.width, size.height), 1f)
    }
}

@Composable
private fun TimeRuler(state: WorkspaceState) {
    val measurer = rememberTextMeasurer()
    var width by remember { mutableFloatStateOf(1f) }
    fun seekTo(x: Float) = state.seek(x / width * TIMELINE_DURATION_SECONDS)

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(RulerHeight)
            .background(AeColors.PanelHeader)
            .onSizeChanged { width = it.width.toFloat().coerceAtLeast(1f) }
            .simTarget(
                simulator = state.simulator,
                key = "timeline.ruler",
                priority = 3,
                onTap = { seekTo(it.x) },
                onDragStart = { seekTo(it.x) },
                onDrag = { _, local -> seekTo(local.x) }
            )
            .pointerInput(Unit) { detectTapGestures { seekTo(it.x) } }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    seekTo(change.position.x)
                }
            }
    ) {
        val steps = (TIMELINE_DURATION_SECONDS * 4).toInt()
        val labelStyle = AeMono.copy(fontSize = 9.sp)
        for (i in 0..steps) {
            val x = size.width * i / steps
            val major = i % 4 == 0
            drawLine(
                color = if (major) AeColors.TextSecondary else AeColors.Hairline,
                start = Offset(x, size.height * (if (major) 0.45f else 0.7f)),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
            if (major && i < steps) {
                val label = measurer.measure("${i / 4}s", labelStyle)
                drawText(label, color = AeColors.TextSecondary, topLeft = Offset(x + 3.dp.toPx(), 1.dp.toPx()))
            }
        }
    }
}

@Composable
private fun PlayheadOverlay(state: WorkspaceState) {
    Canvas(Modifier.fillMaxSize()) {
        val x = size.width * (state.playhead / TIMELINE_DURATION_SECONDS).coerceIn(0f, 1f)
        drawLine(AeColors.Playhead, Offset(x, 0f), Offset(x, size.height), 1.5.dp.toPx())
        val head = Path().apply {
            moveTo(x - 5.dp.toPx(), 0f)
            lineTo(x + 5.dp.toPx(), 0f)
            lineTo(x, 8.dp.toPx())
            close()
        }
        drawPath(head, AeColors.Playhead)
    }
}
