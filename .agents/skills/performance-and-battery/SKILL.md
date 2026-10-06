---
name: performance-and-battery
description: Use when a change could affect frame rate, heat, battery, memory, or startup.
---

- Target a smooth AR view. Do not add work to the render loop. Process at a lower rate than the camera when possible, and skip frames if the previous job is still running.
- Cancel background jobs when the screen closes or inputs change.
- Avoid large allocations in loops. Reuse buffers.
- Respect Battery Saver mode where it exists.
- Release camera, torch, and AR session promptly when leaving the tracing screen.
- For anything heavy, add a way to measure it (a debug readout or log of milliseconds per run) and report the numbers.
