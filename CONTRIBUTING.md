# Contributing to TraceAR

Thank you for your interest in contributing to **TraceAR** (`ar_drawing_app`)! We welcome bug reports, documentation improvements, and thoughtful feature contributions.

---

## Code of Conduct

All contributors and participants are expected to follow our [Code of Conduct](CODE_OF_CONDUCT.md).

---

## Architectural Guidelines & Invariants

Before writing code, please review [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md). TraceAR relies on strict core invariants:

1. **Coordinate Spaces Must Never Be Mixed**:
   - **World Space (meters)**: Transient ARCore session coordinates. Never saved to disk or persistent state.
   - **Anchor-Local Space**: Rigid coordinates relative to the single physical table plane anchor.
   - **Paper-Normalized Space (`u, v` from 0.0 to 1.0)**: Coordinates normalized to the 4 paper corners. This is the **only** coordinate space serialized to disk.
2. **Paper Lock**: The overlay tracks relative to a 2D rigid transform on the table plane (`dx, dz, dθ`). It does not alter normalized project data or undo history.
3. **Pure Logic Separation**: Geometry, filtering, math, and transformations must remain in pure Kotlin classes (`com.tracear.app.ar.*`) decoupled from Android/ARCore APIs so they can be unit-tested on the JVM without an emulator or physical device.
4. **Camera View Dominance**: The AR camera stream must remain primary. UI dialogs and sheets must be compact and non-blocking.
5. **No Network Access**: The app must remain 100% offline. Do not introduce network dependencies, analytics, or advertising SDKs.

---

## Reporting Bugs

1. Search existing [Issues](https://github.com/saptarshidas578/ar_drawing_app/issues) to verify the bug has not already been reported.
2. If new, open an issue using the **Bug Report Template**.
3. Include your device model, Android OS version, Google Play Services for AR version, step-by-step reproduction instructions, and screenshots if applicable.

---

## Development & Pull Requests

### 1. Environment Setup
- Android Studio Ladybug (2024.2.1) or newer.
- JDK 17.
- A physical Android device with Google Play Services for AR installed (the Android Emulator does not provide realistic AR camera feeds).

### 2. Workflow
1. Fork the repository and create a branch off `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```
2. Make your changes adhering to existing Kotlin and Jetpack Compose conventions.
3. Add unit tests for any new mathematical, algorithmic, or parsing logic in `app/src/test/`.
4. Verify that existing tests pass:
   ```bash
   ./gradlew testDebugUnitTest
   ```
5. Verify lint and debug build:
   ```bash
   ./gradlew assembleDebug
   ./gradlew lintDebug
   ```
6. Commit with clear, descriptive commit messages.
7. Open a Pull Request targeting `main` using our PR template.
