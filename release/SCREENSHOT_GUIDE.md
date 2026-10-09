# Google Play Store Screenshot & Graphic Asset Guide — TraceAR

This guide defines the visual assets, screenshot compositions, and Play Console specifications required for publishing **TraceAR**.

---

## 1. Technical Requirements (Google Play)

| Asset Type                    | Dimensions                             | Aspect Ratio            | Format                  | Max File Size | Requirement              |
| :---------------------------- | :------------------------------------- | :---------------------- | :---------------------- | :------------ | :----------------------- |
| **App Icon**                  | 512 x 512 px                           | 1:1                     | 32-bit PNG (with alpha) | 1,024 KB      | Mandatory                |
| **Feature Graphic**           | 1024 x 500 px                          | 1024:500 (~2:1)         | JPEG or 24-bit PNG      | 15 MB         | Mandatory                |
| **Phone Screenshots**         | Min 1080 x 1920 px (or 1080 x 2400 px) | 9:16 Portrait (or 16:9) | 24-bit PNG or JPEG      | 8 MB each     | Min 2, Recommended 6–8   |
| **7-inch Tablet (Optional)**  | Min 1200 x 1920 px                     | 9:16 or 16:9            | PNG or JPEG             | 8 MB each     | Optional but recommended |
| **10-inch Tablet (Optional)** | Min 1600 x 2560 px                     | 9:16 or 16:9            | PNG or JPEG             | 8 MB each     | Optional but recommended |

> [!IMPORTANT]
> Google Play requires at least **4 phone screenshots** (minimum resolution 1080px on shortest side, max 7680px) and a **1024x500 Feature Graphic** before you can submit an app for review.

---

## 2. Recommended Screenshot Storyboard (8 Screens)

Each screenshot should feature a **bold headline banner at the top** (e.g., in a clean sans-serif typeface like Outfit or Inter), a **phone mockup frame**, and a clear view of the app in action.

### Screenshot 1: Hero — AR Optical Tracing

- **Banner Headline**: _Trace Any Image Directly Onto Paper_
- **Sub-headline**: _Rock-solid AR projection keeps artwork locked to your desk_
- **Visual Content**:
  - Live AR camera view pointed at real drawing paper on a desk.
  - Semi-transparent reference sketch (e.g., a lion or anime character) projected onto the paper.
  - Hand with pencil actively tracing the projected lines.
- **Why it converts**: Immediately communicates the primary function of the app in 1 second.

### Screenshot 2: Paper Detection & Table Lock

- **Banner Headline**: _Smart Paper Calibration in Seconds_
- **Sub-headline**: _Auto-detects paper corners or lock manually with 4 taps_
- **Visual Content**:
  - Camera view showing green glowing polygon contour around a physical A4 sheet.
  - "Paper Locked" badge and surface stability indicator.
- **Why it converts**: Proves the app is intelligent and will not slip or float arbitrarily in the air.

### Screenshot 3: Classic Drawing & Proportion Grids

- **Banner Headline**: _Master Proportions With On-Paper Grids_
- **Sub-headline**: _Coordinate labels (A1, B2) & Rule of Thirds stay glued to paper_
- **Visual Content**:
  - AR view showing the 4x4 or 8x8 proportion grid rendered directly over the paper frame.
  - Chessboard coordinate badges clearly legible.
  - Grid mode chip active in bottom toolbar.
- **Why it converts**: Appeals directly to art students, hobbyists, and teachers who practice the grid method.

### Screenshot 4: Tonal Layers & Value Shading

- **Banner Headline**: _Split Reference Photos Into Tonal Layers_
- **Sub-headline**: _Learn shading step-by-step from highlights to deep shadows_
- **Visual Content**:
  - Tonal Layer bottom sheet open showing 4 discrete value layers (Light, Midtone, Shadow, Dark).
  - One tone soloed in high-contrast blue/slate overlay on the paper.
  - Eyedropper tool active picking a tone value.
- **Why it converts**: Differentiates TraceAR from basic tracing apps; highlights advanced portrait and shading assistance.

### Screenshot 5: Instant Edge Extraction (Lines-Only)

- **Banner Headline**: _Convert Any Photo to Clean Line Art_
- **Sub-headline**: _Canny edge filter & high-contrast inverted tracing_
- **Visual Content**:
  - Before/after or clean line extraction of an intricate photo (e.g., flowers, architecture).
  - Invert switch toggled to white-on-black or dark lines on light background.
- **Why it converts**: Shows that users do not need pre-made coloring outlines; any camera photo works.

### Screenshot 6: Section Navigation & High-Precision Zoom

- **Banner Headline**: _Zoom In Up to 4x for Fine Details_
- **Sub-headline**: _Section grid navigates complex drawings block by block_
- **Visual Content**:
  - Magnified 2.5x view of an intricate eye or architectural detail.
  - Mini-map / section indicator in corner showing active block.
- **Why it converts**: Reassures artists that complex, detailed drawings are achievable without hand fatigue.

### Screenshot 7: Rectified Scans & Timelapse Recording

- **Banner Headline**: _Capture, Rectify & Share Your Process_
- **Sub-headline**: _Top-down perspective scan & hardware timelapse export_
- **Visual Content**:
  - Rectified paper preview screen showing the physical paper flattened top-down without perspective distortion.
  - Timelapse video export card showing playback controls and resolution options (1080p).
- **Why it converts**: Shows the complete workflow from blank page to social-media-ready export.

### Screenshot 8: 100% Offline & Private

- **Banner Headline**: _No Ads. No Accounts. 100% On-Device._
- **Sub-headline**: _Your camera feed and drawings never leave your phone_
- **Visual Content**:
  - Clean project manager grid showing saved sketches and zero clutter.
  - Trust badge / Privacy guarantee callout.
- **Why it converts**: Critical selling point in an app store flooded with ad-heavy clone apps.

---

## 3. Best Practices for Capturing Screenshots

1. **Clean Real World Desk**: Place a fresh, crisp sheet of white paper (A4 or Letter) on a wooden desk with good daylight or overhead lighting.
2. **Realistic Drawings**: Trace an attractive, recognizable subject (e.g., botanical illustration, animal portrait, or architectural sketch).
3. **Hide Debug Overlays**: Ensure `DEBUG_OVERLAYS` and any FPS counters are turned off in settings.
4. **Use High Resolution**: Take screenshots on a device with at least 1080 x 2400 resolution. Use ADB screencap for pixel-perfect captures:
   ```bash
   adb shell screencap -p /sdcard/screen1.png
   adb pull /sdcard/screen1.png .
   ```
5. **Localization**: If launching in multiple languages in the future, translate the banner headlines accordingly.
