package com.app.aestudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.aestudio.ui.state.LayerKind
import com.app.aestudio.ui.state.WorkspaceState
import com.app.aestudio.ui.theme.AeColors
import com.app.aestudio.utils.simClickable
import com.app.aestudio.utils.simTarget
import kotlin.math.roundToInt

private class MenuItemDef(
    val label: String,
    val shortcut: String? = null,
    val action: WorkspaceState.() -> Unit,
)

private class MenuDef(val title: String, val items: List<MenuItemDef>)

private val Menus = listOf(
    MenuDef("File", listOf(
        MenuItemDef("New Project", "Ctrl+Alt+N") { message("New project created") },
        MenuItemDef("Save", "Ctrl+S") { message("Project saved") },
        MenuItemDef("Add to Render Queue", "Ctrl+M") { message("Main_Comp added to the render queue") },
    )),
    MenuDef("Edit", listOf(
        MenuItemDef("Duplicate Layer", "Ctrl+D") { duplicateSelected() },
        MenuItemDef("Delete Layer", "Del") { deleteSelected() },
    )),
    MenuDef("Composition", listOf(
        MenuItemDef("Play / Pause", "Space") { togglePlay() },
        MenuItemDef("Go to Start", "Home") { seek(0f) },
        MenuItemDef("Fit Viewport") { fitViewport() },
    )),
    MenuDef("Layer", listOf(
        MenuItemDef("New Text Layer") { addLayer(LayerKind.Text) },
        MenuItemDef("New Solid", "Ctrl+Y") { addLayer(LayerKind.Solid) },
        MenuItemDef("New Image Layer") { addLayer(LayerKind.Image) },
    )),
    MenuDef("Effect", listOf(
        MenuItemDef("Position") { applyEffectByName("Position") },
        MenuItemDef("Gaussian Blur") { applyEffectByName("Gaussian Blur") },
        MenuItemDef("Glow") { applyEffectByName("Glow") },
        MenuItemDef("Drop Shadow") { applyEffectByName("Drop Shadow") },
    )),
    MenuDef("Window", listOf(
        MenuItemDef("Keyboard & Trackpad") { dockVisible = !dockVisible },
        MenuItemDef("Reset Layout") { resetLayout() },
    )),
)

private const val PRIORITY_MENU_TITLE = 95
private const val PRIORITY_MENU_SCRIM = 90
private const val PRIORITY_MENU_ITEM = 100

/** Dark menu bar: File, Edit, Composition, Layer, Effect, Window. */
@Composable
fun TopMenuBar(state: WorkspaceState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(AeColors.PanelHeader),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .padding(horizontal = 8.dp)
                .size(18.dp)
                .background(AeColors.Logo, RoundedCornerShape(3.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Ae", color = AeColors.LogoText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Menus.forEach { MenuTitle(state, it) }
        // Status text and the keys toggle only fit on wide screens (use Window menu in portrait).
        if (LocalConfiguration.current.screenWidthDp >= 600) {
            Text(
                text = state.statusMessage,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                style = MaterialTheme.typography.labelSmall,
                color = AeColors.TextSecondary,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            DockToggle(state)
        }
    }
}

@Composable
private fun MenuTitle(state: WorkspaceState, menu: MenuDef) {
    val key = "menu.${menu.title}"
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val open = state.openMenu == menu.title
    val hovered = state.simulator.hoveredKey == key
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .onGloballyPositioned { bounds = it.boundsInRoot() }
            .background(
                when {
                    open -> AeColors.AccentSoft
                    hovered -> AeColors.PanelRaised
                    else -> Color.Transparent
                }
            )
            .simClickable(state.simulator, key, PRIORITY_MENU_TITLE) {
                state.toggleMenu(menu.title, Offset(bounds.left, bounds.bottom))
            }
            .padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(menu.title, style = MaterialTheme.typography.labelMedium, color = AeColors.TextPrimary)
    }
}

@Composable
private fun DockToggle(state: WorkspaceState) {
    val key = "menu.dockToggle"
    val hovered = state.simulator.hoveredKey == key
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .background(if (hovered) AeColors.PanelRaised else Color.Transparent)
            .simClickable(state.simulator, key, PRIORITY_MENU_TITLE) { state.dockVisible = !state.dockVisible }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (state.dockVisible) "Hide keys" else "Show keys",
            style = MaterialTheme.typography.labelMedium,
            color = AeColors.Accent
        )
    }
}

/**
 * Drawn at the root (not in a Popup) so both the real finger and the virtual cursor can reach it.
 * The scrim only registers with the simulator; it does not intercept real touches, so the
 * trackpad keeps working while a menu is open.
 */
@Composable
fun MenuDropdownOverlay(state: WorkspaceState) {
    val openTitle = state.openMenu ?: return
    val menu = Menus.firstOrNull { it.title == openTitle } ?: return
    val shape = RoundedCornerShape(4.dp)

    Box(
        Modifier
            .fillMaxSize()
            .simTarget(state.simulator, "menu.scrim", PRIORITY_MENU_SCRIM, onTap = { state.closeMenu() })
    )
    Column(
        modifier = Modifier
            .offset {
                IntOffset(
                    state.openMenuAnchor.x.roundToInt(),
                    state.openMenuAnchor.y.roundToInt()
                )
            }
            .width(220.dp)
            .shadow(10.dp, shape)
            .background(AeColors.PanelRaised, shape)
            .border(1.dp, AeColors.Hairline, shape)
            .padding(vertical = 4.dp)
    ) {
        menu.items.forEach { MenuRow(state, menu.title, it) }
    }
}

@Composable
private fun MenuRow(state: WorkspaceState, menuTitle: String, item: MenuItemDef) {
    val key = "menu.$menuTitle.${item.label}"
    val hovered = state.simulator.hoveredKey == key
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(if (hovered) AeColors.Accent else Color.Transparent)
            .simClickable(state.simulator, key, PRIORITY_MENU_ITEM) {
                item.action(state)
                state.closeMenu()
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = if (hovered) Color.White else AeColors.TextPrimary,
            maxLines = 1
        )
        item.shortcut?.let {
            Spacer(Modifier.width(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = if (hovered) Color.White else AeColors.TextSecondary
            )
        }
    }
}
