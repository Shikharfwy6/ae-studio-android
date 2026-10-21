package com.app.aestudio.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.aestudio.ui.state.Tool
import com.app.aestudio.ui.state.WorkspaceState
import com.app.aestudio.ui.theme.AeColors

private const val MAIN_BLOCK_UNITS = 15f

/** One keycap. Positions and sizes are in key units (1.0 = a standard 1u key). */
data class KeySpec(
    val code: String,
    val label: String,
    val x: Float,
    val y: Float,
    val w: Float = 1f,
    val h: Float = 1f,
    val topLabel: String? = null,
    val char: String? = null,
    val shiftChar: String? = null,
    val isLetter: Boolean = false,
    val isModifier: Boolean = false,
)

/**
 * Full-size ANSI layout: the 104 standard keys plus two utility keys (Calc, Mute) = 106.
 * Width is 23u (main 15u, nav 3u, numpad 4u, with 0.5u gaps); height is 6.5u.
 */
object AnsiLayout {
    const val WIDTH_UNITS = 23f
    const val HEIGHT_UNITS = 6.5f
    val keys: List<KeySpec> by lazy { buildKeys() }
}

private fun buildKeys(): List<KeySpec> {
    val out = ArrayList<KeySpec>(106)

    fun add(
        code: String, label: String, x: Float, y: Float,
        w: Float = 1f, h: Float = 1f,
        mod: Boolean = false, top: String? = null,
        char: String? = null, shift: String? = null, letter: Boolean = false,
    ) {
        out += KeySpec(code, label, x, y, w, h, top, char, shift, letter, mod)
    }

    // Row 0: Esc, F1-F12, PrtSc/ScrLk/Pause, two utility keys above the numpad
    add("Escape", "Esc", 0f, 0f, mod = true)
    val fx = floatArrayOf(2f, 3f, 4f, 5f, 6.5f, 7.5f, 8.5f, 9.5f, 11f, 12f, 13f, 14f)
    fx.forEachIndexed { i, x -> add("F${i + 1}", "F${i + 1}", x, 0f, mod = true) }
    add("PrintScreen", "PrtSc", 15.5f, 0f, mod = true)
    add("ScrollLock", "ScrLk", 16.5f, 0f, mod = true)
    add("Pause", "Pause", 17.5f, 0f, mod = true)
    add("Calc", "Calc", 19f, 0f, mod = true)
    add("Mute", "Mute", 20f, 0f, mod = true)

    // Row 1: number row
    val numCodes = listOf(
        "Backquote", "Digit1", "Digit2", "Digit3", "Digit4", "Digit5", "Digit6",
        "Digit7", "Digit8", "Digit9", "Digit0", "Minus", "Equal"
    )
    val numPairs = listOf(
        "`" to "~", "1" to "!", "2" to "@", "3" to "#", "4" to "\$", "5" to "%", "6" to "^",
        "7" to "&", "8" to "*", "9" to "(", "0" to ")", "-" to "_", "=" to "+"
    )
    numPairs.forEachIndexed { i, (base, shifted) ->
        add(numCodes[i], base, i.toFloat(), 1.5f, top = shifted, char = base, shift = shifted)
    }
    add("Backspace", "Bksp", 13f, 1.5f, w = 2f, mod = true)
    add("Insert", "Ins", 15.5f, 1.5f, mod = true)
    add("Home", "Home", 16.5f, 1.5f, mod = true)
    add("PageUp", "PgUp", 17.5f, 1.5f, mod = true)
    add("NumLock", "Num", 19f, 1.5f, mod = true)
    add("NumpadDivide", "/", 20f, 1.5f, char = "/")
    add("NumpadMultiply", "*", 21f, 1.5f, char = "*")
    add("NumpadSubtract", "-", 22f, 1.5f, char = "-")

    // Row 2: QWERTY
    add("Tab", "Tab", 0f, 2.5f, w = 1.5f, mod = true)
    "QWERTYUIOP".forEachIndexed { i, c ->
        add("Key$c", c.toString(), 1.5f + i, 2.5f, char = c.lowercase(), letter = true)
    }
    add("BracketLeft", "[", 11.5f, 2.5f, top = "{", char = "[", shift = "{")
    add("BracketRight", "]", 12.5f, 2.5f, top = "}", char = "]", shift = "}")
    add("Backslash", "\\", 13.5f, 2.5f, w = 1.5f, top = "|", char = "\\", shift = "|")
    add("Delete", "Del", 15.5f, 2.5f, mod = true)
    add("End", "End", 16.5f, 2.5f, mod = true)
    add("PageDown", "PgDn", 17.5f, 2.5f, mod = true)
    add("Numpad7", "7", 19f, 2.5f, char = "7")
    add("Numpad8", "8", 20f, 2.5f, char = "8")
    add("Numpad9", "9", 21f, 2.5f, char = "9")
    add("NumpadAdd", "+", 22f, 2.5f, h = 2f, char = "+")

    // Row 3: home row
    add("CapsLock", "Caps", 0f, 3.5f, w = 1.75f, mod = true)
    "ASDFGHJKL".forEachIndexed { i, c ->
        add("Key$c", c.toString(), 1.75f + i, 3.5f, char = c.lowercase(), letter = true)
    }
    add("Semicolon", ";", 10.75f, 3.5f, top = ":", char = ";", shift = ":")
    add("Quote", "'", 11.75f, 3.5f, top = "\"", char = "'", shift = "\"")
    add("Enter", "Enter", 12.75f, 3.5f, w = 2.25f, mod = true)
    add("Numpad4", "4", 19f, 3.5f, char = "4")
    add("Numpad5", "5", 20f, 3.5f, char = "5")
    add("Numpad6", "6", 21f, 3.5f, char = "6")

    // Row 4: shift row
    add("ShiftLeft", "Shift", 0f, 4.5f, w = 2.25f, mod = true)
    "ZXCVBNM".forEachIndexed { i, c ->
        add("Key$c", c.toString(), 2.25f + i, 4.5f, char = c.lowercase(), letter = true)
    }
    add("Comma", ",", 9.25f, 4.5f, top = "<", char = ",", shift = "<")
    add("Period", ".", 10.25f, 4.5f, top = ">", char = ".", shift = ">")
    add("Slash", "/", 11.25f, 4.5f, top = "?", char = "/", shift = "?")
    add("ShiftRight", "Shift", 12.25f, 4.5f, w = 2.75f, mod = true)
    add("ArrowUp", "\u2191", 16.5f, 4.5f, mod = true)
    add("Numpad1", "1", 19f, 4.5f, char = "1")
    add("Numpad2", "2", 20f, 4.5f, char = "2")
    add("Numpad3", "3", 21f, 4.5f, char = "3")
    add("NumpadEnter", "Enter", 22f, 4.5f, h = 2f, mod = true)

    // Row 5: bottom row
    add("ControlLeft", "Ctrl", 0f, 5.5f, w = 1.25f, mod = true)
    add("MetaLeft", "Win", 1.25f, 5.5f, w = 1.25f, mod = true)
    add("AltLeft", "Alt", 2.5f, 5.5f, w = 1.25f, mod = true)
    add("Space", "", 3.75f, 5.5f, w = 6.25f, char = " ")
    add("AltRight", "Alt", 10f, 5.5f, w = 1.25f, mod = true)
    add("MetaRight", "Win", 11.25f, 5.5f, w = 1.25f, mod = true)
    add("ContextMenu", "Menu", 12.5f, 5.5f, w = 1.25f, mod = true)
    add("ControlRight", "Ctrl", 13.75f, 5.5f, w = 1.25f, mod = true)
    add("ArrowLeft", "\u2190", 15.5f, 5.5f, mod = true)
    add("ArrowDown", "\u2193", 16.5f, 5.5f, mod = true)
    add("ArrowRight", "\u2192", 17.5f, 5.5f, mod = true)
    add("Numpad0", "0", 19f, 5.5f, w = 2f, char = "0")
    add("NumpadDecimal", ".", 21f, 5.5f, char = ".")

    return out
}

/**
 * The keyboard scales to whatever space it is given. It is meant to sit inside [VirtualTrackpad],
 * whose surface frames it; taps on keycaps are consumed here, everything else moves the cursor.
 */
@Composable
fun MechanicalKeyboard(state: WorkspaceState, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val full = state.fullKeyboard
        val widthUnits = if (full) AnsiLayout.WIDTH_UNITS else MAIN_BLOCK_UNITS
        val keys = if (full) AnsiLayout.keys else AnsiLayout.keys.filter { it.x + it.w <= MAIN_BLOCK_UNITS }
        val unit = minOf(maxWidth / widthUnits, maxHeight / AnsiLayout.HEIGHT_UNITS)
        val gap = (unit * 0.1f).coerceAtLeast(1.dp)
        Box(Modifier.size(unit * widthUnits, unit * AnsiLayout.HEIGHT_UNITS)) {
            keys.forEach { spec -> Keycap(spec, unit, gap, state) }
        }
    }
}

@Composable
private fun Keycap(spec: KeySpec, unit: Dp, gap: Dp, state: WorkspaceState) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, tween(60), label = "keyScale")

    val latched = (spec.code == "CapsLock" && state.capsOn) ||
        (spec.code.startsWith("Shift") && state.shiftOn)
    val isToolKey = spec.isLetter && Tool.entries.any { it.shortcut.toString() == spec.label }
    val toolActive = isToolKey && state.activeTool.shortcut.toString() == spec.label
    val lit = latched || toolActive

    val topColor = when {
        pressed -> AeColors.KeyPressedTop
        lit -> AeColors.KeyLitTop
        spec.isModifier -> AeColors.KeyModTop
        else -> AeColors.KeyTop
    }
    val bottomColor = when {
        pressed -> AeColors.KeyPressedBottom
        lit -> AeColors.KeyLitBottom
        spec.isModifier -> AeColors.KeyModBottom
        else -> AeColors.KeyBottom
    }
    val legendColor = if (isToolKey) AeColors.Accent else AeColors.KeyLegend
    val fontSize = (unit.value * 0.34f).coerceIn(6f, 13f).sp

    Box(
        modifier = Modifier
            .offset(unit * spec.x, unit * spec.y)
            .size(unit * spec.w - gap, unit * spec.h - gap)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .drawBehind {
                val radius = CornerRadius(minOf(size.width, size.height) * 0.16f)
                val inset = minOf(size.width, size.height) * 0.07f
                val capTopLeft = Offset(inset, inset * 0.5f)
                val capSize = Size(size.width - inset * 2f, size.height - inset * 2.4f)
                drawRoundRect(AeColors.KeyEdge, cornerRadius = radius)
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(topColor, bottomColor)),
                    topLeft = capTopLeft,
                    size = capSize,
                    cornerRadius = radius
                )
                drawLine(
                    color = Color.White.copy(alpha = if (pressed) 0.05f else 0.14f),
                    start = Offset(capTopLeft.x + radius.x, capTopLeft.y + 0.5.dp.toPx()),
                    end = Offset(capTopLeft.x + capSize.width - radius.x, capTopLeft.y + 0.5.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
                if (lit) {
                    drawRoundRect(
                        color = AeColors.Accent,
                        topLeft = capTopLeft,
                        size = capSize,
                        cornerRadius = radius,
                        style = Stroke(1.2.dp.toPx())
                    )
                }
                if (latched) {
                    drawCircle(
                        AeColors.Accent, radius = 2.dp.toPx(),
                        center = Offset(size.width - inset - 5.dp.toPx(), 5.dp.toPx())
                    )
                }
            }
            .pointerInput(spec.code) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        state.onKeyStroke(spec.code, resolveText(spec, state))
                        tryAwaitRelease()
                        pressed = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            spec.topLabel?.let {
                Text(
                    text = it,
                    color = AeColors.KeyLegendDim,
                    fontSize = fontSize * 0.85f,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                    lineHeight = fontSize
                )
            }
            if (spec.label.isNotEmpty()) {
                Text(
                    text = spec.label,
                    color = if (spec.isModifier) AeColors.KeyLegendDim else legendColor,
                    fontSize = fontSize,
                    fontWeight = if (spec.isLetter) FontWeight.SemiBold else FontWeight.Normal,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                    lineHeight = fontSize,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/** Character produced by [spec] given the current Shift and Caps Lock state. */
private fun resolveText(spec: KeySpec, state: WorkspaceState): String? {
    val c = spec.char ?: return null
    return when {
        spec.isLetter -> if (state.shiftOn xor state.capsOn) c.uppercase() else c
        state.shiftOn -> spec.shiftChar ?: c
        else -> c
    }
}
