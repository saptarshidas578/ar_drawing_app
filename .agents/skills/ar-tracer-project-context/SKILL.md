---
name: ar-tracer-project-context
description: Use at the start of EVERY task on the AR tracing app. Explains what the app is, its coordinate spaces, the rules that must never break, and the inventory of features already built.
---

## What the app is
An Android app where the user picks a reference image, the phone camera looks at a sheet of paper on a table, and the image is drawn over the paper, anchored in the real world. The user watches the screen and traces with a pencil on the real paper. The overlay must stay locked to the paper as the phone moves closer, farther, tilts, or rotates.

## Stack
Kotlin, Jetpack Compose, ARCore, SceneView, OpenCV for Android, local storage for projects and settings. Confirm the real versions and libraries by reading `build.gradle` files. Do not assume.

## Coordinate spaces (never mix them up)
1. **World space:** ARCore's meters. Never saved to disk, because ARCore sessions do not persist between runs.
2. **Anchor-local space:** positions relative to the single overlay anchor on the locked plane. Used while the session runs.
3. **Paper-normalized space (u, v), each from 0 to 1:** position across the 4 marked paper corners, origin at the top-left corner. This is the ONLY space used for saving data to disk.

## Invariants (never change without the user's explicit permission)
1. Calibration flow: find and lock the table plane, then mark 4 paper corners (tap or Detect paper).
2. The table plane is the working plane. The paper itself is never detected as a plane.
3. Corner taps become camera rays and are intersected mathematically with the locked plane's infinite plane (ray-plane intersection), not ARCore hit tests.
4. One anchor for the overlay; corners and image transform stored in its local space.
5. The image is a quad mesh with 4 vertices and correct UVs stretched between the corners. Fit modes: Fill, Fit, Stretch.
6. Saved data uses paper-normalized coordinates only.
7. The camera view is the main thing on screen. UI is a slim top bar, a compact bottom dock, and small bottom sheets (sheet at most 30% of screen height; UI under 15% of the screen when no sheet is open).
8. Lock/unlock: locked means image gestures are disabled (and pinch zooms the view); unlocked means gestures edit the image.

## Features already built (do not break any of these)
- Foundation: gallery image picker, camera permission, ARCore support check, 4-corner calibration with markers and undo, quad overlay, opacity slider, change image, keep screen awake, portrait and landscape.
- Placement: corner auto-sorting, draggable corner handles, fit modes, rotate/scale/move by gesture, rotation slider, 90 degree buttons, flips, reset image, lock/unlock, full re-calibration reset, debug outline.
- Zoom and sections: view zoom 1x to 4x with pan and reset, section grid 2x2 to 6x6, active-section highlight with dimming, focus section, next/previous in snake order, mark done with progress, confirmation before re-calibrating, high-resolution image loading and tiling.
- Detection: crosshair surface selection, floor rejection, "Use this surface" lock, OpenCV Detect paper with stability check, fallbacks (depth, instant placement, scan quality, tips), debug panel.
- Quality: overlay smoothing, tracking status chip, fade when tracking is lost, image adjustments (brightness, contrast, invert, threshold), torch, full-brightness option, low-light hint.
- Projects: lines-only mode (sensitivity, thickness, color), project list, save and resume, autosave.
- UI: camera-first layout with top bar, bottom dock, sheets, Hold to Peek, Focus mode, left-handed mode, auto line color.
- Precision: paper size presets, real-world size, ruler, fine nudge, snap rotation, undo/redo, crop.

Maintain a `CHANGELOG.md` in the project root. At the end of every task, append what changed and which features now exist. Do not edit this skill file to record progress.

## About the user
The user is a beginner. Explain in plain English, define any jargon the first time, give exact steps for anything they must do by hand, and say clearly what they should test on their phone.
