# User Guide — TraceAR (`ar_drawing_app`)

Welcome to **TraceAR**! This guide walks you through setting up your drawing area, calibrating your paper, and using all the features of the app to create physical drawings on paper.

---

## 1. Setting Up Your Drawing Space

For the best augmented reality experience, follow these physical setup tips:

1. **Use a Phone Stand or Rig**:
   - You need both hands free to draw. Mount your phone in an adjustable gooseneck stand, desktop tripod, or place it on top of a sturdy tall mug/box ~40–60 cm above your drawing paper.
   - Point the camera downward at a ~45° to 60° angle facing your paper.
2. **Textured Desk / Table Surface**:
   - Google ARCore needs visual features (wood grain, tablecloth patterns, or desk texture) to track motion.
   - _Avoid_: Plain, featureless white tables, glass desks, or glossy reflective surfaces.
3. **Good, Even Lighting**:
   - Use warm, diffuse room lighting. Avoid harsh shadows or pointing the camera directly into a desk lamp.
   - If drawing in dim conditions, tap the **Torch (Flashlight)** icon in the app's top bar.
4. **Paper Placement**:
   - Place a sheet of paper (A4, A3, or sketchbook) flat on the table.
   - _Tip_: Taping the four corners of your paper to the desk with masking tape gives rock-solid stability.

---

## 2. Step-by-Step Drawing Workflow

### Step 1: Start a New Project

1. Open TraceAR and tap **"Start New Drawing"** (or pick a recent sketch from the gallery).
2. Choose a photo or reference sketch from your phone's photo library.

### Step 2: Detect & Lock the Table Surface

1. Point your camera at the desk next to your paper. Move the phone slightly side-to-side so ARCore detects the surface.
2. A green reticle and dots will appear flat on your desk. When the banner says **"Table surface found!"**, tap **"Use this surface"**.

### Step 3: Calibrate the 4 Paper Corners

- **Manual Mode**: Tap the 4 corners of your physical paper in sequence on the screen (Top-Left, Top-Right, Bottom-Right, Bottom-Left). Green pins will mark each corner.
- **Auto-Detect**: Tap **"Detect Paper"** to let OpenCV find the paper boundary automatically.
- Once 4 corners are set, tap **"Done"**. Your reference image will immediately project onto the paper!

### Step 4: Adjust Artwork Placement

- Open the **Transform** bottom sheet (📐):
  - **Nudge**: Tap Left/Up/Down/Right to move the overlay in 1 mm increments.
  - **Rotate**: Tap 90° buttons or use fine ±0.1° rotation buttons.
  - **Straighten**: Instantly align the image with paper edges.
  - **Scale**: Zoom image by +0.5% or +10%.
  - **Flip**: Mirror horizontally or vertically.
  - **Fit Modes**: Choose _Fit_, _Fill_, or _Stretch_ to match paper proportions.

### Step 5: Trace!

Look through your phone screen and use your real pencil to trace the projected lines directly onto the real paper.

---

## 3. Features Breakdown

### 🧭 Drawing Guides (Proportion & Composition)

Open the **Guides** sheet (🧭) to render classic art guides directly on your paper:

- **Proportion Grid**:
  - _By Count_: Divide paper into 2x2 up to 20x20 equal blocks.
  - _Real Size (cm)_: Draw grid lines with exact physical spacing in centimeters (e.g., 2 cm grid squares).
  - _Chessboard Labels_: Coordinate markers (A1, B2) appear at cell corners and fade out automatically when you zoom in.
- **Composition Lines**:
  - Center Crosshair (`─`, `│`), Diagonals (`✕`), Rule of Thirds ($1/3, 2/3$), and Golden Ratio ($\Phi \approx 0.618$).

### ✏️ Lines-Only Mode

Open the **Lines** sheet (✏️):

- Toggles OpenCV Canny edge extraction, turning any photo into clean line art.
- Adjust **Detail / Sensitivity** slider to extract fine details or bold outlines.
- Choose outline colors (Cyan, Yellow, Orange, Green, Blue, White, Black).

### 🎨 Tonal Layers (Value Shading)

Open the **Tones** sheet (🎭):

- Automatically quantizes photo brightness into **2 to 6 tonal planes** (Highlights $\to$ Midtones $\to$ Deep Shadows) using edge-preserving bilateral filtering.
- **Solo a Tone**: Isolate deep shadows or highlights to shade one value at a time.
- **Stages Mode**: Step through tones sequentially (Outline first $\to$ Tone 1 $\to$ Tone 2 $\to$ Tone 3).
- **Eyedropper**: Tap anywhere on the reference photo or physical paper to inspect brightness percentage.

### 🎨 Image Adjustments

Open the **Adjust** sheet (🎨):

- **Overlay Smoothing**: Low/High temporal filtering to eliminate camera noise.
- **Brightness & Contrast**: Boost contrast to make faint lines clearly visible against paper.
- **Invert**: Invert dark lines to glowing white lines (great for dark paper or low-light tracing).
- **Black & White Threshold**: Convert grayscale gradients into high-contrast binary silhouettes.

### 🔍 Section Grid & 4x View Zoom

- Pinch with two fingers to zoom into intricate details (from 1x up to 4x).
- Enable the **Section Grid** (3x3 to 6x6) in the Sections sheet (▦) to trace complex drawings block by block. Numbered in snake order with progress tracking.

### 📸 Capture, Rectify & Timelapse Export

Open the **Export** dialog (📸):

- **Capture Photo**: Saves a clean high-resolution photo to your gallery (`Pictures/TraceAR`).
- **Rectified Paper Scan**: Perspective-warps the 4 tracked paper corners into a flat, top-down rectangular scan of your finished artwork.
- **Timelapse Video**: Automatically records your tracing session as a smooth 1080p/720p MP4 video saved to `Movies/TraceAR`.
