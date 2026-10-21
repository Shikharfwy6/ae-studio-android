package com.app.aestudio.ui.state

import android.view.View
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.aestudio.utils.PointerSimulator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import kotlin.math.roundToInt

const val TIMELINE_DURATION_SECONDS = 6f
const val TIMELINE_FPS = 30
const val SEARCH_TARGET_KEY = "effects.search"

enum class Tool(val shortcut: Char, val displayName: String) {
    Selection('V', "Selection"),
    Hand('H', "Hand"),
    Zoom('Z', "Zoom"),
    Rotation('W', "Rotation"),
    Camera('C', "Camera"),
    Shape('Q', "Shape"),
    Pen('G', "Pen"),
    Type('T', "Type"),
}

enum class LayerKind { Text, Image, Solid }

data class TimelineLayer(
    val id: Int,
    val name: String,
    val kind: LayerKind,
    val inPoint: Float,
    val outPoint: Float,
    val effects: List<String> = emptyList(),
)

data class EffectPreset(val name: String, val category: String)

val EffectCatalog = listOf(
    EffectPreset("Gaussian Blur", "Blur & Sharpen"),
    EffectPreset("Fast Box Blur", "Blur & Sharpen"),
    EffectPreset("Directional Blur", "Blur & Sharpen"),
    EffectPreset("Glow", "Stylize"),
    EffectPreset("Drop Shadow", "Perspective"),
    EffectPreset("Fill", "Generate"),
    EffectPreset("Gradient Ramp", "Generate"),
    EffectPreset("Tint", "Color Correction"),
    EffectPreset("Curves", "Color Correction"),
    EffectPreset("Levels", "Color Correction"),
    EffectPreset("Echo", "Time"),
    EffectPreset("Fractal Noise", "Noise & Grain"),
    EffectPreset("Wiggle Position", "Animation Presets"),
    EffectPreset("Fade In", "Animation Presets"),
)

/** Single source of truth for panel sizes, cursor, tools, timeline and keyboard state. */
@Stable
class WorkspaceState {

    val simulator = PointerSimulator()

    // ---- Panel dimensions -------------------------------------------------------------
    var leftPanelWidth by mutableStateOf(DEFAULT_LEFT_WIDTH)
    var projectPanelHeight by mutableStateOf(DEFAULT_PROJECT_HEIGHT)
    var timelineHeight by mutableStateOf(DEFAULT_TIMELINE_HEIGHT)
    var dockHeight by mutableStateOf(DEFAULT_DOCK_HEIGHT)
    var dockVisible by mutableStateOf(true)

    fun resizeLeftPanel(delta: Dp, max: Dp) {
        leftPanelWidth = (leftPanelWidth + delta).coerceIn(MIN_LEFT_WIDTH, maxOf(MIN_LEFT_WIDTH, max))
    }

    fun resizeProjectPanel(delta: Dp, max: Dp) {
        projectPanelHeight =
            (projectPanelHeight + delta).coerceIn(MIN_PROJECT_HEIGHT, maxOf(MIN_PROJECT_HEIGHT, max))
    }

    /** Dragging the divider down shrinks the timeline. */
    fun resizeTimeline(delta: Dp, max: Dp) {
        timelineHeight = (timelineHeight - delta).coerceIn(MIN_TIMELINE_HEIGHT, maxOf(MIN_TIMELINE_HEIGHT, max))
    }

    /** Dragging the divider down shrinks the dock. */
    fun resizeDock(delta: Dp, max: Dp) {
        dockHeight = (dockHeight - delta).coerceIn(MIN_DOCK_HEIGHT, maxOf(MIN_DOCK_HEIGHT, max))
    }

    fun initDock(windowHeight: Dp) {
        dockHeight = (windowHeight * 0.42f).coerceIn(MIN_DOCK_HEIGHT, 300.dp)
    }

    fun resetLayout() {
        leftPanelWidth = DEFAULT_LEFT_WIDTH
        projectPanelHeight = DEFAULT_PROJECT_HEIGHT
        timelineHeight = DEFAULT_TIMELINE_HEIGHT
        dockHeight = DEFAULT_DOCK_HEIGHT
        dockVisible = true
        fitViewport()
        message("Workspace layout reset")
    }

    // ---- Virtual cursor ---------------------------------------------------------------
    var cursor by mutableStateOf(Offset(200f, 200f))
        private set
    var cursorBounds by mutableStateOf(Rect.Zero)
        private set
    var cursorVisible by mutableStateOf(true)
    var cursorPressed by mutableStateOf(false)
        private set
    var clickPulse by mutableIntStateOf(0)
        private set
    var sensitivity by mutableFloatStateOf(1.8f)
    private var cursorPlaced = false

    /** The cursor is confined to the studio area (everything above the dock). */
    fun updateCursorBounds(bounds: Rect) {
        cursorBounds = bounds
        if (bounds.isEmpty) return
        if (!cursorPlaced) {
            cursor = bounds.center
            cursorPlaced = true
        } else {
            cursor = clampToBounds(cursor)
        }
    }

    private fun clampToBounds(p: Offset): Offset {
        val b = cursorBounds
        if (b.isEmpty) return p
        return Offset(p.x.coerceIn(b.left, b.right), p.y.coerceIn(b.top, b.bottom))
    }

    fun moveCursor(delta: Offset) {
        cursor = clampToBounds(cursor + delta * sensitivity)
        simulator.updateHover(cursor)
    }

    /** Tap-to-click: dispatches a click at the cursor position. */
    fun click(hostView: View?) {
        clickPulse++
        val hit = simulator.tap(cursor, hostView)
        if (hit?.key != SEARCH_TARGET_KEY) searchFocused = false
    }

    fun beginPress() {
        cursorPressed = true
        simulator.press(cursor)
    }

    fun dragCursorBy(delta: Offset) {
        val before = cursor
        moveCursor(delta)
        simulator.dragBy(cursor - before, cursor)
    }

    fun endPress() {
        simulator.release()
        cursorPressed = false
    }

    // ---- Status / menus ---------------------------------------------------------------
    var statusMessage by mutableStateOf("Ready")
        private set
    var openMenu by mutableStateOf<String?>(null)
        private set
    var openMenuAnchor by mutableStateOf(Offset.Zero)
        private set

    fun message(text: String) { statusMessage = text }

    fun toggleMenu(title: String, anchor: Offset) {
        if (openMenu == title) {
            openMenu = null
        } else {
            openMenu = title
            openMenuAnchor = anchor
        }
    }

    fun closeMenu() { openMenu = null }

    // ---- Tools ------------------------------------------------------------------------
    var activeTool by mutableStateOf(Tool.Selection)
        private set

    fun selectTool(tool: Tool) {
        activeTool = tool
        message("${tool.displayName} tool")
    }

    // ---- Project + effects ------------------------------------------------------------
    var assetsExpanded by mutableStateOf(true)
    var selectedProjectItem by mutableStateOf<String?>("Main_Comp")
    var effectsQuery by mutableStateOf("")
    var searchFocused by mutableStateOf(false)

    fun selectProjectItem(id: String) { selectedProjectItem = id }

    fun filteredEffects(): List<EffectPreset> {
        val q = effectsQuery.trim()
        return if (q.isEmpty()) EffectCatalog
        else EffectCatalog.filter {
            it.name.contains(q, ignoreCase = true) || it.category.contains(q, ignoreCase = true)
        }
    }

    fun applyEffect(effect: EffectPreset) {
        val index = layers.indexOfFirst { it.id == selectedLayerId }
        if (index < 0) {
            message("Select a layer before applying ${effect.name}")
            return
        }
        val layer = layers[index]
        layers[index] = layer.copy(effects = layer.effects + effect.name)
        message("Applied ${effect.name} to ${layer.name}")
    }

    fun applyEffectByName(name: String) {
        EffectCatalog.firstOrNull { it.name == name }?.let { applyEffect(it) }
    }

    // ---- Viewport ---------------------------------------------------------------------
    var compZoom by mutableFloatStateOf(1f)
        private set
    var viewportPan by mutableStateOf(Offset.Zero)
        private set

    fun zoomBy(factor: Float) {
        compZoom = (compZoom * factor).coerceIn(0.25f, 6f)
        message("Zoom ${(compZoom * 100).roundToInt()}% of fit")
    }

    fun panBy(delta: Offset) { viewportPan += delta }

    fun fitViewport() {
        compZoom = 1f
        viewportPan = Offset.Zero
    }

    // ---- Timeline ---------------------------------------------------------------------
    var playhead by mutableFloatStateOf(1.2f)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var selectedLayerId by mutableStateOf<Int?>(1)
    private var nextLayerId = 4

    val layers = mutableStateListOf(
        TimelineLayer(1, "Text Layer", LayerKind.Text, 0.4f, 5.6f),
        TimelineLayer(2, "Logo.png", LayerKind.Image, 0f, 6f),
        TimelineLayer(3, "Solid 1", LayerKind.Solid, 0f, 6f),
    )

    val frame: Int get() = (playhead * TIMELINE_FPS).roundToInt()
    val totalFrames: Int get() = (TIMELINE_DURATION_SECONDS * TIMELINE_FPS).roundToInt()

    fun timecode(): String {
        val f = frame
        val fps = TIMELINE_FPS
        return String.format(Locale.US, "0:%02d:%02d:%02d", f / fps / 60, f / fps % 60, f % fps)
    }

    fun seek(seconds: Float) { playhead = seconds.coerceIn(0f, TIMELINE_DURATION_SECONDS) }
    fun stepFrames(count: Int) { seek(playhead + count / TIMELINE_FPS.toFloat()) }
    fun togglePlay() { isPlaying = !isPlaying }

    /** Called every frame while playing; loops at the end of the composition. */
    fun advance(deltaSeconds: Float) {
        var t = playhead + deltaSeconds
        if (t > TIMELINE_DURATION_SECONDS) t -= TIMELINE_DURATION_SECONDS
        playhead = t
    }

    fun selectLayer(id: Int?) {
        selectedLayerId = id
        layers.firstOrNull { it.id == id }?.let { message("Selected ${it.name}") }
    }

    fun addLayer(kind: LayerKind) {
        val n = layers.count { it.kind == kind } + 1
        val name = when (kind) {
            LayerKind.Text -> "Text Layer $n"
            LayerKind.Image -> "Image $n"
            LayerKind.Solid -> "Solid $n"
        }
        val layer = TimelineLayer(nextLayerId++, name, kind, 0f, TIMELINE_DURATION_SECONDS)
        layers.add(0, layer)
        selectedLayerId = layer.id
        message("Added $name")
    }

    fun duplicateSelected() {
        val index = layers.indexOfFirst { it.id == selectedLayerId }
        if (index < 0) { message("Select a layer to duplicate"); return }
        val src = layers[index]
        val copy = src.copy(id = nextLayerId++, name = "${src.name} 2")
        layers.add(index, copy)
        selectedLayerId = copy.id
        message("Duplicated ${src.name}")
    }

    fun deleteSelected() {
        val index = layers.indexOfFirst { it.id == selectedLayerId }
        if (index < 0) { message("No layer selected"); return }
        val removed = layers.removeAt(index)
        selectedLayerId = layers.firstOrNull()?.id
        message("Deleted ${removed.name}")
    }

    // ---- Keyboard ---------------------------------------------------------------------
    var shiftOn by mutableStateOf(false)
        private set
    var capsOn by mutableStateOf(false)
        private set

    /** Portrait shows the main key block only; turn on for nav cluster + numpad. */
    var fullKeyboard by mutableStateOf(false)

    private val _lastKey = MutableStateFlow<String?>(null)
    val lastKey: StateFlow<String?> = _lastKey.asStateFlow()

    /**
     * Entry point for every keycap press. [text] is the character the key produces with the
     * current Shift/Caps state (null for non-printing keys).
     */
    fun onKeyStroke(code: String, text: String?) {
        _lastKey.value = code
        when (code) {
            "ShiftLeft", "ShiftRight" -> { shiftOn = !shiftOn; return }
            "CapsLock" -> { capsOn = !capsOn; return }
            "ControlLeft", "ControlRight", "AltLeft", "AltRight",
            "MetaLeft", "MetaRight", "ContextMenu", "NumLock", "Fn" -> return
        }
        if (searchFocused) handleSearchKey(code, text) else handleShortcut(code)
        if (text != null) shiftOn = false
    }

    private fun handleSearchKey(code: String, text: String?) {
        when (code) {
            "Backspace" -> effectsQuery = effectsQuery.dropLast(1)
            "Escape" -> searchFocused = false
            "Enter", "NumpadEnter" -> {
                val first = filteredEffects().firstOrNull()
                if (first != null) applyEffect(first) else message("No effect matches \"$effectsQuery\"")
            }
            else -> if (!text.isNullOrEmpty()) effectsQuery += text
        }
    }

    private fun handleShortcut(code: String) {
        if (code.startsWith("Key") && code.length == 4) {
            val letter = code[3]
            Tool.entries.firstOrNull { it.shortcut == letter }?.let { selectTool(it); return }
        }
        when (code) {
            "Space" -> togglePlay()
            "Home" -> seek(0f)
            "End" -> seek(TIMELINE_DURATION_SECONDS)
            "ArrowLeft" -> stepFrames(if (shiftOn) -10 else -1)
            "ArrowRight" -> stepFrames(if (shiftOn) 10 else 1)
            "Delete", "Backspace" -> deleteSelected()
            "Escape" -> closeMenu()
            "NumpadAdd" -> zoomBy(1.25f)
            "NumpadSubtract" -> zoomBy(0.8f)
            else -> Unit
        }
    }

    companion object {
        val DEFAULT_LEFT_WIDTH = 120.dp
        val DEFAULT_PROJECT_HEIGHT = 110.dp
        val DEFAULT_TIMELINE_HEIGHT = 130.dp
        val DEFAULT_DOCK_HEIGHT = 190.dp
        val MIN_LEFT_WIDTH = 90.dp
        val MIN_PROJECT_HEIGHT = 60.dp
        val MIN_TIMELINE_HEIGHT = 70.dp
        val MIN_DOCK_HEIGHT = 100.dp
    }
}
