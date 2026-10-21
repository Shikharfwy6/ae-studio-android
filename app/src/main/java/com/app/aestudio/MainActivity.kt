package com.app.aestudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.app.aestudio.ui.components.CenterComposition
import com.app.aestudio.ui.components.DividerOrientation
import com.app.aestudio.ui.components.LeftPanel
import com.app.aestudio.ui.components.MechanicalKeyboard
import com.app.aestudio.ui.components.MenuDropdownOverlay
import com.app.aestudio.ui.components.ResizerDivider
import com.app.aestudio.ui.components.Toolbar
import com.app.aestudio.ui.components.TopMenuBar
import com.app.aestudio.ui.components.VirtualCursorOverlay
import com.app.aestudio.ui.components.VirtualTrackpad
import com.app.aestudio.ui.state.WorkspaceState
import com.app.aestudio.ui.theme.AeColors
import com.app.aestudio.ui.theme.AeStudioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Immersive: the studio needs every pixel; system bars return on edge swipe.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            AeStudioTheme {
                AeStudioApp()
            }
        }
    }
}

@Composable
fun AeStudioApp() {
    val state = remember { WorkspaceState() }

    BoxWithConstraints(Modifier.fillMaxSize().background(AeColors.Window)) {
        val windowHeight = maxHeight
        LaunchedEffect(Unit) { state.initDock(windowHeight) }

        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            // Everything above the dock. The virtual cursor is confined to this region.
            StudioArea(
                state = state,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onGloballyPositioned { state.updateCursorBounds(it.boundsInRoot()) }
            )

            if (state.dockVisible) {
                val maxDock = windowHeight - 170.dp
                val dockHeight = state.dockHeight.coerceIn(
                    WorkspaceState.MIN_DOCK_HEIGHT,
                    maxOf(WorkspaceState.MIN_DOCK_HEIGHT, maxDock)
                )
                ResizerDivider(
                    orientation = DividerOrientation.Horizontal,
                    simulator = state.simulator,
                    key = "divider.dock",
                    onDelta = { state.resizeDock(it, maxDock) }
                )
                VirtualTrackpad(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dockHeight)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    MechanicalKeyboard(state, Modifier.fillMaxSize())
                }
            }
        }

        MenuDropdownOverlay(state)
        VirtualCursorOverlay(state)
    }
}

@Composable
private fun StudioArea(state: WorkspaceState, modifier: Modifier = Modifier) {
    Column(modifier.background(AeColors.Panel)) {
        TopMenuBar(state)
        Toolbar(state)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val maxLeft = maxWidth - 150.dp
            val leftWidth = state.leftPanelWidth.coerceIn(
                WorkspaceState.MIN_LEFT_WIDTH,
                maxOf(WorkspaceState.MIN_LEFT_WIDTH, maxLeft)
            )
            Row(Modifier.fillMaxSize()) {
                LeftPanel(state, Modifier.width(leftWidth).fillMaxHeight())
                ResizerDivider(
                    orientation = DividerOrientation.Vertical,
                    simulator = state.simulator,
                    key = "divider.left",
                    onDelta = { state.resizeLeftPanel(it, maxLeft) }
                )
                CenterComposition(state, Modifier.weight(1f).fillMaxSize())
            }
        }
    }
}
