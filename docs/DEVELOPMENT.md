# Developer Guide — TraceAR (`ar_drawing_app`)

This document outlines environment requirements, build commands, test execution, coding conventions, and architectural safety rules for developers working on **TraceAR**.

---

## 1. Prerequisites & Environment Setup

- **Operating System**: Windows, macOS, or Linux.
- **Android Studio**: Android Studio Ladybug (2024.2.1) or newer.
- **JDK**: Java Development Kit 17 (recommended: Eclipse Temurin 17 or Android Studio bundled JDK).
- **Android SDK Components**:
  - Android SDK Platform 35 (Android 15)
  - Android SDK Build-Tools 35.0.0
  - Android NDK (if building native C++ components, though prebuilt OpenCV AAR is integrated)
- **Physical Test Device**:
  - ARCore requires a physical Android device certified for Google Play Services for AR ([Supported Devices List](https://developers.google.com/ar/devices)).
  - _Note_: Android Studio Virtual Devices (Emulators) cannot simulate ARCore camera depth and feature point tracking accurately. Always test on physical hardware.

---

## 2. Build & Verification Commands

All commands can be executed using the Gradle wrapper in the project root:

### Build Debug APK

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Install Debug APK Directly to Connected Phone

```bash
./gradlew installDebug
```

### Run Unit Tests

```bash
./gradlew testDebugUnitTest
```

To run a specific test suite (e.g. math tests):

```bash
./gradlew testDebugUnitTest --tests "com.tracear.app.ar.MathUtilsTest"
```

### Run Android Lint

```bash
./gradlew lintDebug
```

Lint reports are generated at `app/build/reports/lint-results-debug.html`.

### Build Release APK & Bundle (Local Verification)

```bash
./gradlew assembleRelease
./gradlew bundleRelease
```

---

## 3. Coding Conventions & Best Practices

1. **Pure Logic Decoupling**:
   - Math, geometry, coordinate transforms, filter logic, and serialization models must remain in pure Kotlin classes inside `com.tracear.app.ar` or `com.tracear.app.data`.
   - Never import `android.view.*`, `com.google.ar.core.*`, or `androidx.compose.*` into pure math files. This allows fast JVM unit testing (< 5 seconds) without needing Robolectric or a connected device.
2. **Immutability & State Hoisting**:
   - UI state classes (`TracingUiState`, `PaperLockState`, `GridState`) should be immutable `data class`es.
   - Mutate state through ViewModel methods or StateFlow updates.
3. **OpenCV Memory Management**:
   - OpenCV `Mat` objects hold native C++ memory that is not garbage collected by the JVM.
   - Always call `.release()` on temporary `Mat` instances in a `finally` block or use `.use { }` extension patterns.
4. **Jetpack Compose Performance**:
   - Keep composables lightweight. Heavy calculations (filtering, scaling, OpenCV processing) must run on `Dispatchers.Default` with a debounce timer (150 ms) to avoid dropping UI frames.
5. **KDoc Documentation**:
   - Document all public math and AR functions with coordinate space tags: `[World Space (m)]`, `[Anchor-Local (m)]`, or `[Paper-Normalized (0..1)]`.

---

## 4. How to Add a Feature Safely

When adding features, adhere to the established project safety workflow:

1. **Check Invariants**:
   - Does this feature touch the paper plane? If yes, ensure it operates in **paper-normalized coordinates** (`u, v ∈ [0, 1]`).
   - Never modify the single AR table anchor or world-space pose.
2. **Create a Feature Branch**:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **Write Pure Logic & Unit Tests First**:
   - Implement your algorithm in pure Kotlin and add corresponding tests in `app/src/test/java/com/tracear/app/`.
4. **Wire into UI**:
   - Place controls in existing sheets in [`TracingCategorySheets.kt`](../app/src/main/java/com/tracear/app/ui/tracing/TracingCategorySheets.kt) or create a new compact category sheet.
   - Ensure touch targets are at least 48 dp.
5. **Verify No Regressions**:
   ```bash
   ./gradlew testDebugUnitTest
   ./gradlew assembleDebug
   ```

---

## 5. Working with AI Prompts & Skills (`docs/ai-workflow`)

This repository includes custom agent skills located in `.agent/skills/` and documentation in `docs/ai-workflow/`:

- **`ar-tracer-project-context`**: Invariant rules and coordinate space definitions.
- **`plan-first-workflow`**: Instructions for planning before coding.
- **`image-processing-rules`**: Memory rules for OpenCV and Bitmaps.
- **`compose-ui-rules`**: Mobile UI layout rules keeping camera view dominant.

To use an AI assistant with this repository, refer to [`docs/ai-workflow/prompts.md`](ai-workflow/prompts.md) for examples of structured milestone prompts.
