# Changelog

All notable changes to the **TraceAR** (`ar_drawing_app`) project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] - 2026-10-08

### Added
- **Core AR Tracing Engine**:
  - Horizontal tabletop plane detection with automatic floor rejection via Google ARCore.
  - Optical camera ray to infinite table plane mathematical unprojection (sub-millimeter corner accuracy without point-cloud dependence).
  - 4-corner calibration with manual tap marker placement and automated OpenCV contour detection (`PaperDetector`).
  - Single rigid AR anchor architecture with GPU textured quad mesh rendered via SceneView and Filament.
  - Paper-normalized coordinate persistence (`u, v ∈ [0, 1]`) ensuring stored projects are invariant to camera orientation, distance, and real-world scale.
  - Interactive transform sheet: 1 mm nudge, ±0.1° and 90° rotation, straighten to paper edge, horizontal/vertical flips, and scale.
  - Section grid (2x2 to 6x6) with snake-order numbering and block completion tracking.
  - Continuous 1x to 4x view zoom with smooth two-finger pan.
- **Dynamic Paper Tracking ("Paper Lock")**:
  - 2D rigid planar pose tracking $(\Delta x, \Delta z, \Delta \theta)$ on the table plane keeping the tracing overlay locked when paper moves or rotates.
  - Dual-measurement fusion: edge ray-plane unprojection (Measurement A) and rectified 512px ORB feature template matching with RANSAC homography (Measurement B).
  - Temporal gating (3-cycle confirmation for large jumps >1 cm), deadband filtering ($0.5\text{ mm}, 0.1^\circ$), and slow template blending.
  - Background tracking loop (5–10 Hz) with Battery Saver down-throttling to 4 Hz.
- **Classic On-Paper Drawing Guides**:
  - Proportion grid with two modes: "By Count" (2 to 20 columns/rows) and "Real Size (cm)" for physical grid squares.
  - Chessboard coordinate cell labels (A1, B2) that fade smoothly when zooming in past 2.5x to preserve pencil visibility.
  - Composition construction lines: Center Crosshair, Diagonals, Rule of Thirds ($1/3, 2/3$), and Golden Ratio ($\Phi \approx 0.618$).
  - Auto-contrast adaptation dynamically adjusting guide line colors against physical paper and ambient lighting.
- **Tonal Layers & Value Shading**:
  - Grayscale conversion with edge-preserving bilateral filtering (`Imgproc.bilateralFilter`) to remove photographic noise while protecting sharp contours.
  - Luminance quantization into $N$ (2 to 6, default 4) discrete transparent value planes.
  - Individual layer controls: solo mode, show/hide, color picker chips, and opacity sliders.
  - Portrait Stages Mode guiding step-by-step shading (Outline $\to$ Highlights $\to$ Midtones $\to$ Deep Shadows).
  - Interactive value eyedropper tool for pixel-accurate tone inspection.
- **Photo Capture & Timelapse Video Export**:
  - Hardware-accelerated GPU surface capture (`PixelCopy`) saving high-resolution captures directly to MediaStore gallery (`Pictures/TraceAR`).
  - Perspective-rectified paper scan generating a flat, top-down rectangular image of completed physical artwork.
  - Native share sheet integration (`Intent.ACTION_SEND`) via `FileProvider`.
  - Hardware-accelerated timelapse video recording using `MediaCodec` (H.264 / `video/avc`) + `MediaMuxer` at 12–30 fps.
- **Onboarding, Settings & UI Polish**:
  - First-run interactive 5-step tutorial walkthrough.
  - Floating non-blocking guidance banners ("Too dark", "Moving too fast", "Low texture", "Tracking lost").
  - Persistent user preferences via `SharedPreferences` (smoothing, left-handed mode, battery saver, line colors).
  - Vector adaptive launcher icon (`ic_launcher`).
- **Production Build & CI Configuration**:
  - R8 code minification and resource shrinking with custom ProGuard keep rules for OpenCV, ARCore, SceneView, and Filament.
  - GitHub Actions CI workflow for automated testing, debug builds, and lint.
  - Comprehensive documentation suite: Architecture, User Guide, Developer Guide, Troubleshooting, ADRs, and Privacy Policy.

### Fixed
- Declared `android.hardware.camera` feature in `AndroidManifest.xml` resolving Google Play hardware filter requirements.
- Implemented `onSessionFailed` fallback in `ARSceneView` with user error dialog and graceful return navigation.
- Added `ON_RESUME` camera permission revocation detection to prevent unhandled `SecurityException`s.
- Created `TracingOverlayNode` with explicit Filament GPU texture, vertex buffer, and material instance destruction to eliminate VRAM leaks.
- Added immediate `finalBitmap.recycle()` call after GPU texture upload in bitmap pipeline.
- Automatically disabled ARCore LED torch on `Lifecycle.Event.ON_PAUSE` to eliminate hardware overheating and battery drain when minimizing the app.

### Security
- 100% offline architecture: zero internet permissions declared (`android.permission.INTERNET` omitted).
- Zero third-party telemetry, analytics, or advertising SDKs.
- Local sandbox storage for private drawings and modern scoped `MediaStore` APIs for public exports.
