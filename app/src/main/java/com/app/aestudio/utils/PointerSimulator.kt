package com.app.aestudio.utils

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * A UI region the virtual cursor can interact with. Bounds are in root coordinates,
 * the same space the cursor overlay lives in.
 */
class SimTarget {
    var key: String = ""
    var priority: Int = 0
    var bounds: Rect = Rect.Zero
    var onTap: ((local: Offset) -> Unit)? = null
    var onDragStart: ((local: Offset) -> Unit)? = null
    var onDrag: ((delta: Offset, local: Offset) -> Unit)? = null
    var onDragEnd: (() -> Unit)? = null

    val isDraggable: Boolean get() = onDrag != null || onDragStart != null
}

/**
 * Maps the virtual cursor position to UI actions.
 *
 * Two paths:
 *  1. Registered [SimTarget]s (via [simTarget]/[simClickable]) get taps, press-drags and hover.
 *  2. If no target handles a tap, a real [MotionEvent] DOWN/UP pair is injected at the cursor
 *     position into the host view, so ordinary Compose pointer handlers still receive it.
 */
class PointerSimulator {
    private val targets = ArrayList<SimTarget>()
    private var activeDrag: SimTarget? = null

    /** Key of the target currently under the cursor, for hover styling. */
    var hoveredKey: String? by mutableStateOf(null)
        private set

    val isDragging: Boolean get() = activeDrag != null

    fun register(target: SimTarget) { if (!targets.contains(target)) targets.add(target) }

    fun unregister(target: SimTarget) {
        targets.remove(target)
        if (activeDrag === target) activeDrag = null
    }

    /** Highest priority wins; ties go to the smallest (innermost) region. */
    fun hitTest(position: Offset, predicate: (SimTarget) -> Boolean = { true }): SimTarget? =
        targets.asSequence()
            .filter { !it.bounds.isEmpty && it.bounds.contains(position) && predicate(it) }
            .sortedWith(
                compareByDescending<SimTarget> { it.priority }
                    .thenBy { it.bounds.width * it.bounds.height }
            )
            .firstOrNull()

    fun updateHover(position: Offset) {
        hoveredKey = hitTest(position)?.key
    }

    /** Tap-to-click. Returns the target that was hit (if any). */
    fun tap(position: Offset, hostView: View?): SimTarget? {
        val target = hitTest(position)
        val handler = target?.onTap
        if (target != null && handler != null) {
            handler(position - target.bounds.topLeft)
        } else if (hostView != null) {
            injectTap(hostView, position)
        }
        return target
    }

    /** Mouse-down. Starts a drag on the topmost draggable target under the cursor. */
    fun press(position: Offset): SimTarget? {
        val target = hitTest(position) { it.isDraggable }
        activeDrag = target
        target?.onDragStart?.invoke(position - target.bounds.topLeft)
        return target
    }

    fun dragBy(delta: Offset, position: Offset) {
        val target = activeDrag ?: return
        target.onDrag?.invoke(delta, position - target.bounds.topLeft)
    }

    /** Mouse-up. */
    fun release() {
        activeDrag?.onDragEnd?.invoke()
        activeDrag = null
    }

    /** Injects a DOWN/UP touch pair at [position] (host-view coordinates) on the next frame. */
    fun injectTap(host: View, position: Offset) {
        host.post {
            val downTime = SystemClock.uptimeMillis()
            dispatch(host, MotionEvent.ACTION_DOWN, downTime, downTime, position)
            dispatch(host, MotionEvent.ACTION_UP, downTime, downTime + 30L, position)
        }
    }

    private fun dispatch(host: View, action: Int, downTime: Long, eventTime: Long, p: Offset) {
        val event = MotionEvent.obtain(downTime, eventTime, action, p.x, p.y, 0)
        event.source = InputDevice.SOURCE_TOUCHSCREEN
        host.dispatchTouchEvent(event)
        event.recycle()
    }
}

/** Registers this element as a virtual-cursor target. Callback offsets are local to the element. */
fun Modifier.simTarget(
    simulator: PointerSimulator,
    key: String,
    priority: Int = 0,
    onTap: ((Offset) -> Unit)? = null,
    onDragStart: ((Offset) -> Unit)? = null,
    onDrag: ((delta: Offset, local: Offset) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
): Modifier = composed {
    val target = remember { SimTarget() }
    SideEffect {
        target.key = key
        target.priority = priority
        target.onTap = onTap
        target.onDragStart = onDragStart
        target.onDrag = onDrag
        target.onDragEnd = onDragEnd
    }
    DisposableEffect(simulator, target) {
        simulator.register(target)
        onDispose { simulator.unregister(target) }
    }
    Modifier.onGloballyPositioned { target.bounds = it.boundsInRoot() }
}

/** Real touch click plus virtual-cursor click on the same element. */
fun Modifier.simClickable(
    simulator: PointerSimulator,
    key: String,
    priority: Int = 0,
    onClick: () -> Unit,
): Modifier = this
    .clickable(onClick = onClick)
    .simTarget(simulator, key, priority, onTap = { onClick() })
