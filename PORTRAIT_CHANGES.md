# Portrait mode: sirf ye changes karo
Har jagah: file kholo, **PURANA** code dhoondo, usse **NAYA** se replace karo.

---
## `app/src/main/AndroidManifest.xml`
**PURANA:**
```xml
android:screenOrientation="landscape"
```
**NAYA:**
```xml
android:screenOrientation="portrait"
```

---
## `app/src/main/java/com/app/aestudio/ui/state/WorkspaceState.kt`
**PURANA:**
```kotlin
val DEFAULT_LEFT_WIDTH = 190.dp
```
**NAYA:**
```kotlin
val DEFAULT_LEFT_WIDTH = 120.dp
```
**PURANA:**
```kotlin
val DEFAULT_TIMELINE_HEIGHT = 110.dp
```
**NAYA:**
```kotlin
val DEFAULT_TIMELINE_HEIGHT = 130.dp
```
**PURANA:**
```kotlin
val MIN_LEFT_WIDTH = 130.dp
```
**NAYA:**
```kotlin
val MIN_LEFT_WIDTH = 90.dp
```
**PURANA:**
```kotlin
    var capsOn by mutableStateOf(false)
        private set
```
**NAYA:**
```kotlin
    var capsOn by mutableStateOf(false)
        private set

    /** Portrait shows the main key block only; turn on for nav cluster + numpad. */
    var fullKeyboard by mutableStateOf(false)
```

---
## `app/src/main/java/com/app/aestudio/MainActivity.kt`
**PURANA:**
```kotlin
val maxLeft = maxWidth - 240.dp
```
**NAYA:**
```kotlin
val maxLeft = maxWidth - 150.dp
```

---
## `app/src/main/java/com/app/aestudio/ui/components/TopMenuBar.kt`
**PURANA:**
```kotlin
import androidx.compose.ui.text.font.FontWeight
```
**NAYA:**
```kotlin
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
```
**PURANA:**
```kotlin
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
```
**NAYA:**
```kotlin
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
```
**PURANA:**
```kotlin
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(menu.title,
```
**NAYA:**
```kotlin
            .padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(menu.title,
```

---
## `app/src/main/java/com/app/aestudio/ui/components/LeftPanel.kt`
**PURANA:**
```kotlin
import androidx.compose.ui.platform.LocalFocusManager
```
**NAYA:**
```kotlin
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
```
**PURANA:**
```kotlin
        Text(
            text = effect.category,
```
**NAYA:**
```kotlin
        if (LocalConfiguration.current.screenWidthDp >= 600) Text(
            text = effect.category,
```

---
## `app/src/main/java/com/app/aestudio/ui/components/CenterComposition.kt`
**PURANA:**
```kotlin
import androidx.compose.ui.layout.onSizeChanged
```
**NAYA:**
```kotlin
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
```
**PURANA:**
```kotlin
private val NameColumnWidth = 132.dp
```
**NAYA:**
```kotlin

```
**PURANA:**
```kotlin
.width(NameColumnWidth)
```
**NAYA:**
```kotlin
.width(if (LocalConfiguration.current.screenWidthDp >= 600) 132.dp else 84.dp)
```
**PURANA:**
```kotlin
        Text(
            "frame ${state.frame} of ${state.totalFrames} at $TIMELINE_FPS fps",
            style = MaterialTheme.typography.labelSmall,
            color = AeColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.weight(1f))
```
**NAYA:**
```kotlin
        Text(
            "frame ${state.frame} of ${state.totalFrames}",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            color = AeColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
```
**PURANA:**
```kotlin
            Spacer(Modifier.weight(1f))
            Text(
                text = "Main_Comp  1920 x 1080",
                style
```
**NAYA:**
```kotlin
            Text(
                text = "Main_Comp  1920 x 1080",
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                style
```

---
## `app/src/main/java/com/app/aestudio/ui/components/VirtualTrackpad.kt`
**PURANA:**
```kotlin
import androidx.compose.ui.platform.LocalDensity
```
**NAYA:**
```kotlin
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
```
**PURANA:**
```kotlin
text = "Trackpad: drag to move, tap to click, hold then drag to press",
```
**NAYA:**
```kotlin
text = "Drag: move, Tap: click, Hold: drag",
```
**PURANA:**
```kotlin
        Text(
            text = "Last key: ${lastKey ?: "none"}",
            style = MaterialTheme.typography.labelSmall,
            color = AeColors.TextSecondary,
            maxLines = 1
        )
        Spacer(Modifier.width(12.dp))
```
**NAYA:**
```kotlin
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
```

---
## `app/src/main/java/com/app/aestudio/ui/components/MechanicalKeyboard.kt`
**PURANA:**
```kotlin
/** One keycap. Positions and sizes are in key units (1.0 = a standard 1u key). */
```
**NAYA:**
```kotlin
private const val MAIN_BLOCK_UNITS = 15f

/** One keycap. Positions and sizes are in key units (1.0 = a standard 1u key). */
```
**PURANA:**
```kotlin
        val unit = minOf(maxWidth / AnsiLayout.WIDTH_UNITS, maxHeight / AnsiLayout.HEIGHT_UNITS)
        val gap = (unit * 0.1f).coerceAtLeast(1.dp)
        Box(Modifier.size(unit * AnsiLayout.WIDTH_UNITS, unit * AnsiLayout.HEIGHT_UNITS)) {
            AnsiLayout.keys.forEach { spec -> Keycap(spec, unit, gap, state) }
        }
```
**NAYA:**
```kotlin
        val full = state.fullKeyboard
        val widthUnits = if (full) AnsiLayout.WIDTH_UNITS else MAIN_BLOCK_UNITS
        val keys = if (full) AnsiLayout.keys else AnsiLayout.keys.filter { it.x + it.w <= MAIN_BLOCK_UNITS }
        val unit = minOf(maxWidth / widthUnits, maxHeight / AnsiLayout.HEIGHT_UNITS)
        val gap = (unit * 0.1f).coerceAtLeast(1.dp)
        Box(Modifier.size(unit * widthUnits, unit * AnsiLayout.HEIGHT_UNITS)) {
            keys.forEach { spec -> Keycap(spec, unit, gap, state) }
        }
```
