package com.app.aestudio.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.aestudio.ui.state.EffectPreset
import com.app.aestudio.ui.state.SEARCH_TARGET_KEY
import com.app.aestudio.ui.state.WorkspaceState
import com.app.aestudio.ui.theme.AeColors
import com.app.aestudio.utils.simClickable
import com.app.aestudio.utils.simTarget

private enum class ItemKind { Folder, Composition, Image, Audio }

/** Left column: Project assets tree on top, Effects & Presets search below. */
@Composable
fun LeftPanel(state: WorkspaceState, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.background(AeColors.Panel)) {
        val minEffects = 110.dp
        val maxProject = maxHeight - minEffects
        val projectHeight = state.projectPanelHeight.coerceIn(
            WorkspaceState.MIN_PROJECT_HEIGHT,
            maxOf(WorkspaceState.MIN_PROJECT_HEIGHT, maxProject)
        )
        Column(Modifier.fillMaxSize()) {
            ProjectPanel(state, Modifier.fillMaxWidth().height(projectHeight))
            ResizerDivider(
                orientation = DividerOrientation.Horizontal,
                simulator = state.simulator,
                key = "divider.project",
                onDelta = { state.resizeProjectPanel(it, maxProject) }
            )
            EffectsPanel(state, Modifier.fillMaxWidth().weight(1f))
        }
    }
}

@Composable
private fun PanelHeader(title: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(22.dp)
            .background(AeColors.PanelHeader)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = AeColors.TextPrimary
        )
    }
}

// ---- Project ---------------------------------------------------------------------------

@Composable
private fun ProjectPanel(state: WorkspaceState, modifier: Modifier) {
    val scroll = rememberScrollState()
    Column(modifier.background(AeColors.Panel)) {
        PanelHeader("Project")
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
        ) {
            TreeRow(state, "assets", "Assets", ItemKind.Folder, 0, expanded = state.assetsExpanded) {
                state.assetsExpanded = !state.assetsExpanded
            }
            if (state.assetsExpanded) {
                TreeRow(state, "Main_Comp", "Main_Comp", ItemKind.Composition, 1) { state.selectProjectItem("Main_Comp") }
                TreeRow(state, "Logo.png", "Logo.png", ItemKind.Image, 1) { state.selectProjectItem("Logo.png") }
                TreeRow(state, "Audio.mp3", "Audio.mp3", ItemKind.Audio, 1) { state.selectProjectItem("Audio.mp3") }
            }
        }
    }
}

@Composable
private fun TreeRow(
    state: WorkspaceState,
    id: String,
    name: String,
    kind: ItemKind,
    depth: Int,
    expanded: Boolean? = null,
    onClick: () -> Unit,
) {
    val key = "project.$id"
    val selected = state.selectedProjectItem == id
    val hovered = state.simulator.hoveredKey == key
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .background(
                when {
                    selected -> AeColors.AccentSoft
                    hovered -> AeColors.PanelRaised
                    else -> Color.Transparent
                }
            )
            .simClickable(state.simulator, key, onClick = onClick)
            .padding(start = (6 + depth * 14).dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (expanded != null) Chevron(expanded) else Spacer(Modifier.width(10.dp))
        Spacer(Modifier.width(4.dp))
        ItemGlyph(kind)
        Spacer(Modifier.width(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = AeColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun Chevron(expanded: Boolean) {
    val rotation by animateFloatAsState(if (expanded) 90f else 0f, label = "chevron")
    Canvas(
        Modifier
            .size(10.dp)
            .graphicsLayer { rotationZ = rotation }
    ) {
        val path = Path().apply {
            moveTo(size.width * 0.3f, size.height * 0.2f)
            lineTo(size.width * 0.75f, size.height * 0.5f)
            lineTo(size.width * 0.3f, size.height * 0.8f)
            close()
        }
        drawPath(path, AeColors.TextSecondary)
    }
}

@Composable
private fun ItemGlyph(kind: ItemKind) {
    Canvas(Modifier.size(13.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.2.dp.toPx()
        when (kind) {
            ItemKind.Folder -> {
                drawRoundRect(
                    color = AeColors.FolderTint,
                    topLeft = Offset(0f, h * 0.1f),
                    size = Size(w * 0.45f, h * 0.25f),
                    cornerRadius = CornerRadius(w * 0.08f)
                )
                drawRoundRect(
                    color = AeColors.FolderTint,
                    topLeft = Offset(0f, h * 0.25f),
                    size = Size(w, h * 0.65f),
                    cornerRadius = CornerRadius(w * 0.1f)
                )
            }
            ItemKind.Composition -> {
                drawRoundRect(
                    color = AeColors.LayerText,
                    topLeft = Offset(0f, h * 0.1f),
                    size = Size(w, h * 0.8f),
                    cornerRadius = CornerRadius(w * 0.12f),
                    style = Stroke(stroke)
                )
                drawLine(AeColors.LayerText, Offset(w * 0.25f, h * 0.4f), Offset(w * 0.75f, h * 0.4f), stroke)
                drawLine(AeColors.LayerText, Offset(w * 0.25f, h * 0.65f), Offset(w * 0.6f, h * 0.65f), stroke)
            }
            ItemKind.Image -> {
                drawRoundRect(
                    color = AeColors.LayerImage,
                    topLeft = Offset(0f, h * 0.1f),
                    size = Size(w, h * 0.8f),
                    cornerRadius = CornerRadius(w * 0.12f)
                )
                drawCircle(Color.White.copy(alpha = 0.85f), radius = w * 0.13f, center = Offset(w * 0.3f, h * 0.38f))
                val hill = Path().apply {
                    moveTo(w * 0.1f, h * 0.85f)
                    lineTo(w * 0.45f, h * 0.5f)
                    lineTo(w * 0.7f, h * 0.72f)
                    lineTo(w * 0.8f, h * 0.62f)
                    lineTo(w * 0.95f, h * 0.85f)
                    close()
                }
                drawPath(hill, Color.White.copy(alpha = 0.6f))
            }
            ItemKind.Audio -> {
                val heights = floatArrayOf(0.35f, 0.8f, 0.55f, 0.95f)
                heights.forEachIndexed { i, fraction ->
                    val barW = w * 0.16f
                    val x = i * (w / 4f) + w * 0.04f
                    drawRoundRect(
                        color = AeColors.AudioTint,
                        topLeft = Offset(x, h * (1f - fraction) / 2f + h * 0.05f),
                        size = Size(barW, h * fraction),
                        cornerRadius = CornerRadius(barW / 2f)
                    )
                }
            }
        }
    }
}

// ---- Effects & Presets -------------------------------------------------------------------

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun EffectsPanel(state: WorkspaceState, modifier: Modifier) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    // The studio has its own mechanical keyboard, so the system keyboard stays hidden.
    LaunchedEffect(state.searchFocused) {
        if (state.searchFocused) {
            runCatching { focusRequester.requestFocus() }
        } else {
            focusManager.clearFocus()
        }
    }

    val results = state.filteredEffects()
    Column(modifier.background(AeColors.Panel)) {
        PanelHeader("Effects & Presets")
        OutlinedTextField(
            value = state.effectsQuery,
            onValueChange = { state.effectsQuery = it },
            readOnly = true,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
            placeholder = {
                Text(
                    "Search effects",
                    style = MaterialTheme.typography.bodySmall,
                    color = AeColors.TextDisabled
                )
            },
            suffix = if (state.searchFocused) ({ BlinkingCaret() }) else null,
            shape = RoundedCornerShape(4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = AeColors.TextPrimary,
                unfocusedTextColor = AeColors.TextPrimary,
                focusedContainerColor = AeColors.Window,
                unfocusedContainerColor = AeColors.Window,
                focusedBorderColor = AeColors.Accent,
                unfocusedBorderColor = AeColors.Hairline,
                cursorColor = AeColors.Accent,
            ),
            modifier = Modifier
                .padding(6.dp)
                .fillMaxWidth()
                .height(48.dp)
                .focusRequester(focusRequester)
                .onFocusChanged {
                    state.searchFocused = it.isFocused
                    if (it.isFocused) keyboard?.hide()
                }
                .simTarget(
                    state.simulator,
                    SEARCH_TARGET_KEY,
                    onTap = { state.searchFocused = true }
                )
        )
        if (results.isEmpty()) {
            Text(
                text = "No effects match \"${state.effectsQuery}\". Press Backspace to edit the search.",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                color = AeColors.TextSecondary
            )
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                items(results, key = { it.name }) { EffectRow(state, it) }
            }
        }
    }
}

@Composable
private fun EffectRow(state: WorkspaceState, effect: EffectPreset) {
    val key = "effect.${effect.name}"
    val hovered = state.simulator.hoveredKey == key
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
            .background(if (hovered) AeColors.PanelRaised else Color.Transparent)
            .simClickable(state.simulator, key) { state.applyEffect(effect) }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = effect.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = AeColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (LocalConfiguration.current.screenWidthDp >= 600) Text(
            text = effect.category,
            style = MaterialTheme.typography.labelSmall,
            color = AeColors.TextDisabled,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BlinkingCaret() {
    val transition = rememberInfiniteTransition(label = "caret")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "caretAlpha"
    )
    Box(
        Modifier
            .width(1.5.dp)
            .height(14.dp)
            .alpha(alpha)
            .background(AeColors.Accent)
    )
}
