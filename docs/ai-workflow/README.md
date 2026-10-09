# AI Workflow & Pair-Programming Documentation

This folder documents how **TraceAR** (`ar_drawing_app`) was designed, implemented, and stabilized using **Google Antigravity**—an advanced agentic AI coding assistant.

---

## Overview

The entire application was built iteratively by combining:

1. **Domain-Specific Architectural Skills**: 12 modular instruction sets stored in `.agent/skills/` that enforce strict invariants (e.g., coordinate spaces, OpenCV memory safety, Jetpack Compose UI patterns, battery management, and test-first math logic).
2. **Sequential Implementation Prompts**: A series of milestone prompts (archived in [`prompts.md`](prompts.md)) guiding the agent through feature increments:
   - **Foundation & Core Calibration**: 4-corner paper quad mapping on an ARCore detected table plane.
   - **Paper Lock**: Dynamic 2D rigid tracking fusing OpenCV edge detection and ORB feature template matching to follow sliding paper without drifting.
   - **Drawing Guides**: On-paper proportion grids (count & cm modes), chessboard cell coordinates (A1, B2), and composition construction lines (Golden Ratio, Rule of Thirds).
   - **Tonal Layers & Shading**: Edge-preserving bilateral filter and luminance quantization splitting reference photos into discrete transparent tonal planes.
   - **Photo Capture & Timelapse Video**: MediaStore integration, OpenCV perspective rectification, and hardware-accelerated `MediaCodec` H.264 video encoding.
   - **Pre-Release Audit & Release Hardening**: R8 minification, memory leak fixes, lifecycle torch shutoff, and ProGuard keep rules.

---

## Files in this Directory

- [`skills.md`](skills.md): The aggregated source of all 12 agent skills used across the project.
- [`prompts.md`](prompts.md): The prompt sequence executed during development.
- `.agent/skills/` (in root): The active skills loaded by the Antigravity agent during pair programming.

_(Note: This folder is maintained for engineering documentation and can be archived or deleted if desired without affecting the Android application runtime.)_
