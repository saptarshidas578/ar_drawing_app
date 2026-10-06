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
