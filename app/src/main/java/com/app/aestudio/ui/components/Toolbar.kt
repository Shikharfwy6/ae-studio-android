package com.app.aestudio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.aestudio.ui.state.Tool
import com.app.aestudio.ui.state.WorkspaceState
import com.app.aestudio.ui.theme.AeColors
import com.app.aestudio.ui.theme.AeMono
import com.app.aestudio.utils.simClickable

/** Tool strip: V H Z W C Q G T. Works with real touch, virtual cursor, and keyboard shortcuts. */
@Composable
fun Toolbar(state: WorkspaceState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(AeColors.PanelRaised)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Tool.entries.forEach { tool ->
            ToolButton(state, tool)
            Spacer(Modifier.width(4.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${state.activeTool.displayName} tool",
            style = MaterialTheme.typography.labelMedium,
            color = AeColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ToolButton(state: WorkspaceState, tool: Tool) {
    val key = "tool.${tool.shortcut}"
    val active = state.activeTool == tool
    val hovered = state.simulator.hoveredKey == key
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = Modifier
            .size(26.dp)
            .background(
                when {
                    active -> AeColors.AccentSoft
                    hovered -> AeColors.Panel
                    else -> Color.Transparent
                },
                shape
            )
            .border(
                BorderStroke(1.dp, if (active) AeColors.Accent else Color.Transparent),
                shape
            )
            .semantics { contentDescription = "${tool.displayName} tool" }
            .simClickable(state.simulator, key) { state.selectTool(tool) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = tool.shortcut.toString(),
            style = AeMono,
            color = if (active) AeColors.Accent else AeColors.TextPrimary
        )
    }
}
