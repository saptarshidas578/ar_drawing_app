---
name: image-processing-rules
description: Use for OpenCV, bitmaps, edge detection, thresholding, tonal layers, textures, tiling, or any pixel work.
---

- All image processing runs on a background dispatcher. Never on the main or render thread.
- Downscale large inputs before processing (longest side about 2048 px for processing; keep the original for display).
- Release memory: call `Mat.release()` on every OpenCV Mat, recycle or reuse bitmaps, and avoid holding several full-size copies.
- Debounce slider-driven processing (about 150 ms) and cache the last result. Show a small loading indicator.
- Output overlays as ARGB bitmaps with a transparent background so only the lines or tones appear on the paper.
- Textures must respect the GPU maximum texture size. Use tiles or downscaling when needed.
- Handle failures (decode errors, out of memory) with a friendly message and a safe fallback to the previous result.
