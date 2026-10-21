# AE Studio for Android

A native Kotlin + Jetpack Compose "After Effects"-style studio with a **virtual trackpad cursor**
and a **106-key mechanical keyboard** (104 ANSI keys + Calc and Mute).

Best used in landscape on a large phone or tablet. Hide the dock from the top bar ("Hide keys") if you need more room.

## How it works

- **Trackpad** (the dark surface around the keyboard): drag to move the cursor, tap to click,
  hold then drag to press-and-drag (resize dividers, scrub the timeline, pan with the Hand tool).
- **Keyboard**: tool letters V H Z W C Q G T switch tools. Space plays, Home/End jump, arrows step frames,
  Del removes the selected layer. Click the Effects search field and type to filter.
- **PointerSimulator**: UI elements register as targets (`simTarget` / `simClickable`).
  Cursor taps go to the topmost target; if none handles it, a real `MotionEvent` is injected at the cursor.
- Real touch also works everywhere, so you can use the app without the trackpad.

## Requirements

- Android Studio Koala (2024.1) or newer, JDK 17
- Android SDK 34

## Open in Android Studio

```bash
git clone <your-repo-url> ae-studio-android
cd ae-studio-android
# Android Studio: File > Open > select the ae-studio-android folder, wait for Gradle sync, press Run
```

## Initialize Git and push to GitHub

```bash
cd ae-studio-android
git init
git add .
git commit -m "Initial commit: AE Studio Android"
git branch -M main
git remote add origin https://github.com/<your-username>/ae-studio-android.git
git push -u origin main
```

Or with the GitHub CLI:

```bash
gh repo create ae-studio-android --private --source=. --remote=origin --push
```

## Command-line build

The Gradle wrapper script and jar are not included. Generate them once (needs Gradle 8.7+ installed), or let Android Studio sync:

```bash
gradle wrapper --gradle-version 8.7
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```
