# Changelog

All notable changes and inventory of features for the AR Tracer app.

## Existing Features

- **Foundation:** gallery image picker, camera permission, ARCore support check, 4-corner calibration with markers and undo, quad overlay, opacity slider, change image, keep screen awake, portrait and landscape.
- **Placement:** corner auto-sorting, draggable corner handles, fit modes, rotate/scale/move by gesture, rotation slider, 90 degree buttons, flips, reset image, lock/unlock, full re-calibration reset, debug outline.
- **Zoom and sections:** view zoom 1x to 4x with pan and reset, section grid 2x2 to 6x6, active-section highlight with dimming, focus section, next/previous in snake order, mark done with progress, confirmation before re-calibrating, high-resolution image loading and tiling.
- **Detection:** crosshair surface selection, floor rejection, "Use this surface" lock, OpenCV Detect paper with stability check, fallbacks (depth, instant placement, scan quality, tips), debug panel.
- **Quality:** overlay smoothing, tracking status chip, fade when tracking is lost, image adjustments (brightness, contrast, invert, threshold), torch, full-brightness option, low-light hint.
- **Projects:** lines-only mode (sensitivity, thickness, color), project list, save and resume, autosave.
- **UI:** camera-first layout with top bar, bottom dock, sheets, Hold to Peek, Focus mode, left-handed mode, auto line color.
- **Precision:** paper size presets, real-world size, ruler, fine nudge, snap rotation, undo/redo, crop.
- **Paper Lock (Dynamic Paper Tracking):**
  - Real-time 2D rigid tracking $(\Delta x, \Delta z, \Delta \theta)$ on the locked table plane keeping the tracing overlay locked when paper slides or rotates.
  - Zero vertex buffer churn: composed dynamically in SceneView Filament GPU node poses without modifying saved normalized data or undo stack.
  - Measurement A (Edge ray-plane unprojection with 5% side length validation) & Measurement B (Rectified 512px grayscale template with ORB feature matching and RANSAC outlier rejection).
  - Fusion engine with temporal gating (3-cycle confirmation for large jumps >1cm), deadband ($0.5\text{ mm}$, $0.1^\circ$), and slow template blending during stationary drawing (3s still).
  - Background coroutine loop at 5–10 Hz with immediate CPU camera image closing, battery-saver down-throttling, and tracking pause protection.
  - Top bar status chip ("Paper: Following / Searching / Paused / Off"), View sheet toggles (Lock on/off, Freeze paper, Sensitivity, Re-align), non-blocking banners ("Paper moved? / Paper found"), and debug panel with quad outlines (cyan detected, yellow predicted) and "Simulate shift (2cm)" test button.
- **UI/UX Polish & Onboarding:**
  - **First-run tutorial:** Interactive 5-step card walkthrough for first launch (stand setup, lighting/paper, camera motion, corner placement, tracing). Features animated dot indicators, Next/Back/Skip buttons, and re-launchable from Settings and HomeScreen.
  - **Friendly guidance banners:** Replaced technical jargon with plain actionable instructions ("☀️ Too dark — turn on torch or brighten room", "🐌 Moving too fast — slow down phone", "🔍 Low texture — point camera at paper", "⚠️ Tracking lost — point camera at paper"). Floating non-blocking temporary banners that keep the camera view dominant.
  - **Comprehensive Settings Screen:** Centralized persistent preferences via SharedPreferences (`tracear_app_settings`) for default opacity, default fit mode, default grid size, default line color chip, smoothing mode, left-handed mode toggle, keep screen on toggle, battery saver mode, show debug info, "How to use" tutorial launcher, and "Reset all settings" confirmation dialog.
  - **Adaptive App Identity:** App label configured as `TraceAR: AR Drawing & Tracing` with custom vector adaptive launcher icon (`ic_launcher`) featuring paper, pencil line, and AR corner brackets.
  - **Performance & Battery:** Dedicated battery saver mode throttling background paper detection and tracking loops to 4 Hz (250 ms); `FLAG_KEEP_SCREEN_ON` lifecycle cleanup restoring screen timeout when leaving tracing screen.
- **Classic Drawing Guides:**
  - **Proportion Grid:** On-paper scaffolding grid with two modes: "By Count" (2 to 20 columns/rows) and "Real Size (cm)" (physical centimeter cell spacing when paper dimensions are known, with helpful preset guidance).
  - **Chessboard Cell Labels:** Chessboard coordinate labels (columns A–Z, rows 1–9) rendered with high-contrast text and shadows; smoothly fades out at zoom scale > 2.5x and hides completely by 4.0x to avoid obstructing fine pencil tracing.
  - **Construction Lines:** Independent on-paper toggles for Horizontal Center (`─`), Vertical Center (`│`), Diagonals (`✕`), Rule of Thirds ($1/3, 2/3$), and Golden Ratio ($\Phi \approx 0.382, 0.618$).
  - **Style & Placement Controls:** Dedicated "Guides" category (🧭) in the compact bottom dock with palette color chips, auto-contrast adaptation against paper and ambient lighting, line thickness slider (0.8–4.0 dp), opacity slider (10%–100%), and "Move guides with image" toggle (allowing guides to stay locked to the physical paper quad or transform with the image).
  - **State & Persistence:** Isolated from Section Grid (`GridState`); serialized to and restored from project data (`NormalizedGuides`) in `ProjectModel.kt`.
  - **Tonal Layers & Value Shading:**
  - **Luminance Quantization:** Converts reference image to grayscale, applies edge-preserving bilateral filtering (`Imgproc.bilateralFilter`) to remove photographic noise while protecting sharp facial contours, and quantizes luminance into $N$ (2 to 6, default 4) tonal planes (Highlights $\to$ Shadows).
  - **Individual Layer Controls:** Transparent overlay per tone with independent show/hide (👁), solo toggle, color picker chips (slate contrast ramps, cyan, warm sepia, red, yellow), and opacity sliders. Includes integrated Outline Layer (Canny edge detection) rendered in the same hierarchy.
  - **Stages Mode:** Classical portrait drawing stepper displaying one layer at a time in sequence (Outline first $\to$ Highlights $\to$ Midtones $\to$ Deep Shadows) with step badge chip and Prev/Next buttons.
  - **Value Picker & Eyedropper:** Pixel-accurate interactive tap preview inside the dock sheet plus "Pick on Paper" AR overlay tap mode, reporting brightness %, tone number, hex color swatch, and tonal name.
  - **Two-Tier High Performance Pipeline:** OpenCV segmentation runs asynchronously in background with a 150 ms slider debounce and LRU memory caching; real-time compositing operates purely in-memory ($< 15\text{ ms}$), maintaining smooth 60 FPS camera frame rates with zero ARCore stutter.
- **Photo Export & Timelapse Video Recording:**
  - **Photo Capture (PixelCopy):** Hardware-accelerated GPU surface capture saving photos directly to device gallery via modern MediaStore (`Pictures/TraceAR`, zero legacy permissions required). Offers both "With Overlay" (complete AR tracing composite) and "Drawing Only" (clean real drawing without overlay).
  - **Rectified Paper Scan:** Perspective warping via OpenCV (`Imgproc.getPerspectiveTransform` and `warpPerspective`) that projects the 4 tracked paper corners into a flat, top-down rectangular scan matching paper aspect ratios.
  - **Direct System Sharing:** Native Android share sheet (`Intent.ACTION_SEND`) via `FileProvider` with content URIs for instant sharing of photos and videos.
  - **Timelapse Session Recording:** Background timer capturing downscaled 720p frames at adjustable intervals (1 to 30s, default 5s). Enforces a 600-frame cap and 150 MB storage ceiling with a 100 MB free space guard.
  - **Hardware-Accelerated MP4 Video Export:** Native `MediaCodec` (H.264 / `video/avc`) + `MediaMuxer` pipeline rendering frames through an EGL input surface at adjustable playback speeds (10 to 30 fps, default 24 fps) with real-time encoding progress, cancel support, and automatic temporary file cleanup.
  - **On-Screen Recording Indicator & Lifecycle:** Sleek blinking on-screen recording chip (`🔴 REC 00:35 • 7f`) that opens controls on tap, and automatically pauses on app minimize (`ON_PAUSE` / `ON_STOP`) and resumes on `ON_RESUME`.
  - **Unit Testing:** Comprehensive test suite in `ExportModelTest.kt` verifying frame naming, duration math, file formatting, even dimension constraints, and storage caps.
- **Pre-Release Audit & Production Stabilization:**
  - **Manifest & Store Readiness:** Declared `<uses-feature android:name="android.hardware.camera" android:required="true" />` in `AndroidManifest.xml` to fix Google Play / ChromeOS hardware compatibility requirements (resolving critical lint failure).
  - **Memory Leak Detection:** Added LeakCanary 2.14 (`debugImplementation`) to debug builds for continuous runtime memory leak detection.
  - **ARCore Lifecycle Resilience:** Implemented `onSessionFailed` callback in `ARSceneView` with user error dialog and graceful return navigation; added `ON_RESUME` camera permission revocation detection to prevent unhandled `SecurityException`s.
  - **VRAM & Node Cleanup:** Created `TracingOverlayNode` extending `AnchorNode` with explicit Filament GPU resource destruction (`engine.destroyTexture`, `destroyVertexBuffer`, `destroyIndexBuffer`, `destroyMaterialInstance`); added `it.destroy()` invocation on removed nodes during sceneview updates, screen disposal (`onRelease`), and surface re-calibration.
  - **Bitmap Allocation Optimization:** Added immediate `finalBitmap.recycle()` after GPU texture upload in `prepareBitmap` pipeline, preventing multi-megabyte heap retention during slider adjustments.
  - **Battery & Thermal Management:** Automatically shut off ARCore LED torch on `Lifecycle.Event.ON_PAUSE` to eliminate hardware battery drain and overheating when minimizing the app or turning off the screen.
  - **Pure Logic Test Suite Expansion:** Added comprehensive edge-case unit tests in `MathUtilsTest.kt` verifying behavior with empty corner lists, fewer than 4 corners, collinear points, singular matrix inversion, and camera pixel unprojection behind the lens.


