# Troubleshooting Guide — TraceAR (`ar_drawing_app`)

This document provides solutions for common issues encountered during setup, calibration, and tracing.

---

## 1. Surface Detection & Calibration Issues

### Problem: "Looking for surface..." banner never disappears / No surface detected
- **Cause**: Google ARCore cannot find enough visual contrast points to detect a plane.
- **Fixes**:
  1. **Add Texture**: Avoid plain, solid-white tables or glossy glass desks. Place a patterned cloth, cutting mat, wooden board, or a few pencils/books on the table to provide visual feature points.
  2. **Improve Lighting**: Ensure the room is well-lit with diffuse overhead light. If the area is dim, tap the **Torch** icon in the app top bar.
  3. **Slow Down Phone Motion**: Slowly translate the phone in a gentle circular or side-to-side motion (about 10–20 cm) so ARCore's visual-inertial odometry can estimate depth.

### Problem: App selects the floor instead of the desk
- **Cause**: The camera saw the floor before the desk plane was detected.
- **Fixes**:
  1. TraceAR includes automatic **floor rejection** that ignores planes significantly below chest/desk height.
  2. Tilt the camera upward slightly so only the desk surface fills the bottom half of the camera view.
  3. Tap the screen reticle directly on the table surface.

### Problem: Projected overlay is slanted or tilted relative to the paper
- **Cause**: One or more paper corners were tapped slightly off-target during calibration.
- **Fixes**:
  1. Open the **Transform** sheet (📐) and tap **Straighten** to level the image.
  2. Use the **Nudge** arrows (1 mm step) or fine rotation buttons (`-0.1°`, `+0.1°`).
  3. Tap the 3-dot overflow menu and select **"Re-calibrate Paper"** to tap the 4 corners again.

---

## 2. Tracking & Stability Issues

### Problem: "⚠️ Tracking lost — point camera at paper"
- **Cause**: The camera was blocked, moved too rapidly, or pointed away from the calibrated desk.
- **Fixes**:
  1. Point the camera back toward the calibrated desk area. ARCore will automatically re-localize within 1–2 seconds.
  2. Ensure your drawing hand or sleeve is not covering the phone camera lens.

### Problem: Overlay drifts slowly over a long drawing session
- **Cause**: Sensor integration drift in mobile IMUs and optical feature fatigue over extended sessions (>30 minutes).
- **Fixes**:
  1. **Turn on Paper Lock**: Ensure Paper Lock is set to **Following** in the top bar. Paper Lock uses optical edge verification and ORB template matching to cancel out ARCore drift.
  2. **Tape Your Paper**: Use masking tape on all 4 corners of your paper so physical movement is eliminated.
  3. **Tap to Re-align**: When the yellow banner *"Paper moved? Tap to re-align"* appears, tap it to snap the overlay back to the current paper position.

### Problem: Paper Lock is not following the paper when slid
- **Cause**: High-glare lighting washing out paper edges or the paper was moved faster than the tracking filter allows (>1 cm per frame).
- **Fixes**:
  1. Move the paper smoothly and gently.
  2. Ensure there is contrast between the paper and desk (e.g. white paper on a darker wooden desk).
  3. Check the Paper Lock status chip in the top bar: if it says **Paused**, hold the paper still for 1 second to resume tracking.

---

## 3. Hardware, Battery & Performance

### Problem: Device gets warm or frame rate drops after 20 minutes
- **Cause**: Continuous 60 FPS camera streaming, ARCore SLAM, Filament 3D rendering, and computer vision can generate significant heat on some chipsets.
- **Fixes**:
  1. In the app **Settings** (⚙️), enable **Battery Saver Mode**. This throttles background paper tracking loops from 10 Hz to 4 Hz and reduces CPU load by ~40%.
  2. Lower the phone screen brightness slightly.
  3. Take off thick phone cases that trap thermal heat during long drawing sessions.

### Problem: App crashes on launch with "Google Play Services for AR required"
- **Cause**: The device lacks Google ARCore or Google Play Services for AR is out of date.
- **Fixes**:
  1. Check if your phone is officially supported on the [ARCore Supported Devices List](https://developers.google.com/ar/devices).
  2. Open the Google Play Store, search for **Google Play Services for AR**, and tap **Update** or **Install**.

### Problem: Camera permission denied
- **Cause**: The user denied camera permission when prompted on first launch.
- **Fixes**:
  1. Open Android System Settings > Apps > **TraceAR** > Permissions > **Camera** > Select **"Allow only while using the app"**.
  2. Relaunch the app.
