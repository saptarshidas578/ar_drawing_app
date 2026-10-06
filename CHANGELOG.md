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
  - **Empty & Error States:** Welcoming dark-glass card for empty project list; missing image file detection on project open with recovery choices ("Pick New Image" / "Delete Project"); storage capacity checking and full disk error alerts.
  - **Adaptive App Identity:** App label configured as `TraceAR: AR Drawing & Tracing` with custom vector adaptive launcher icon (`ic_launcher`) featuring paper, pencil line, and AR corner brackets.
  - **Performance & Battery:** Dedicated battery saver mode throttling background paper detection and tracking loops to 4 Hz (250 ms); `FLAG_KEEP_SCREEN_ON` lifecycle cleanup restoring screen timeout when leaving tracing screen.
