# Application Screenshots & Media Assets

This directory contains real application screenshots and graphic assets illustrating **TraceAR** (`ar_drawing_app`) in action on physical Android devices.

---

## Included Screenshots

1. **`01_surface_detection.png`**
   - **Feature**: AR Surface Detection & Plane Calibration.
   - **Description**: The phone camera detects the flat table/desk surface using Google ARCore, displays a green reticle crosshair, and prompts: *"Table surface found! Tap 'Use this surface' below"*.

2. **`02_tracing_transform.png`**
   - **Feature**: Optical Tracing & Transform Bottom Sheet.
   - **Description**: Reference artwork (Goddess Durga) projected directly over paper in real time with the **Transform** sheet open (Nudge 1mm, 90° rotation, flip horizontal/vertical, straighten, scale, opacity).

3. **`03_lines_section_grid.png`**
   - **Feature**: Lines-Only Extraction & Section Grid Navigation.
   - **Description**: Real-time OpenCV Canny edge extraction converting reference art to clean cyan line art, with a 3x3 section grid numbered in snake order (1 to 9) for systematic block-by-block tracing.

4. **`04_adjust_filters.png`**
   - **Feature**: Real-Time Image Adjustments.
   - **Description**: Tracing screen with white edge outlines and the **Adjust** sheet open, showing real-time sliders for overlay smoothing, brightness, contrast, color inversion, and black & white threshold.

5. **`05_tonal_layers.png`**
   - **Feature**: Tonal Value Shading & Bilateral Filtering.
   - **Description**: Advanced tonal segmentation decomposing reference art into 4 discrete value planes (Highlights to Deep Shadows) with edge smoothing and sequential Stages Mode for portrait artists.

---

## Recommended Additional Captures (Future / Store)

- [ ] **`06_guides_proportion_grid.png`**: Proportion grid active with chessboard coordinate badges (A1, B2) and Rule of Thirds guidelines.
- [ ] **`07_settings_screen.png`**: App settings screen showing persistent preferences (default opacity, line color chips, left-handed mode, battery saver mode).
- [ ] **`demo.gif`**: Short 5–10 second screen recording showing paper lock following a gently sliding sheet of paper.
