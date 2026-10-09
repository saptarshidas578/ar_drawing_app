# Google Play Store Listing Draft — TraceAR

## App Title

`TraceAR: AR Drawing & Tracing` _(30 characters)_

---

## Short Description

_(Must be 80 characters or fewer)_
`Trace & sketch any image onto paper with precision augmented reality.` _(72 characters)_

---

## Full Description

_(Max 4,000 characters)_

**Learn to draw, sketch, and shade with augmented reality!**

TraceAR turns your Android phone into an optical drawing projector. Place your phone on a stand, point your camera at a sheet of paper, and trace any reference photo onto paper with pencil-point accuracy.

Whether you are an aspiring artist learning proportions or a seasoned illustrator looking for fast, accurate transfer, TraceAR keeps your reference image rock-solid on your paper surface so you can draw with confidence.

---

### 🌟 Key Features

#### 📐 Augmented Reality Table Lock

- **True AR Projection**: Uses Google ARCore to anchor your drawing overlay directly to your physical paper plane.
- **Smart Paper Detection**: Detects paper edges automatically via OpenCV or let you tap 4 corners manually.
- **Dynamic Paper Tracking**: Slides and rotates your drawing overlay in real time if your paper shifts on your desk.

#### 🧭 Classic Drawing Guides (On Paper)

- **Proportion Grid**: Break complex subjects into manageable blocks. Toggle by Count (2x2 up to 20x20) or by Real Size in centimeters.
- **Chessboard Coordinates**: Clear letter-number labels (A1, B2) that fade automatically as you zoom in.
- **Composition Guides**: Golden Ratio lines, Rule of Thirds, Center Crosshair, and Corner Diagonals.

#### 🎨 Tonal Layers & Value Shading

- **Luminance Quantization**: Separate any photo into 2 to 6 tonal value planes (Highlights to Deep Shadows) using edge-preserving bilateral filtering.
- **Solo & Step-by-Step Tracing**: Trace one tone at a time with adjustable contrast ramps and opacity.
- **Interactive Value Eyedropper**: Tap anywhere on your reference image or physical paper to inspect exact tone values.

#### 🔍 Precision Image Controls

- **Lines-Only Mode**: Instant Canny edge extraction converting photos to clean line art.
- **Image Adjustments**: Invert colors, adjust brightness & contrast, and apply black & white thresholds.
- **Section Grid & Zoom**: Pan and zoom from 1x to 4x. Mark completed sections in snake order.
- **Transform & Crop**: Nudge, rotate, scale, flip horizontally/vertically, or crop precisely to the area you want to draw.

#### 📸 Capture, Rectify & Timelapse

- **Photo Export**: Save clean drawing photos or full AR tracing captures directly to your gallery via MediaStore.
- **Rectified Paper Scan**: Generate a flat, top-down perspective-corrected scan of your finished artwork.
- **Hardware-Accelerated Timelapse**: Record your drawing session as a smooth 1080p/720p MP4 video at 12–30 fps.

---

### 🔒 Privacy & Offline First

- **Zero Internet Permissions**: TraceAR contains no network permissions (`android.permission.INTERNET` is not included).
- **100% On-Device**: All AR calculations, edge detections, and photos remain on your phone.
- **No Ads, No Accounts, No Tracking**: Just open the app and draw.

---

### 📋 Requirements

- An ARCore-compatible Android device running Android 7.0 (Nougat) or newer.
- A stable phone stand, tripod, or tall cup to hold your phone 40–60 cm above your drawing paper.
