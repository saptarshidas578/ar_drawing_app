---
name: arcore-rendering-rules
description: Use for anything involving ARCore, SceneView, anchors, planes, tracking, camera frames, depth, or the overlay mesh.
---

- **Lifecycle:** create, resume, and pause the ARCore session with the Activity lifecycle. Handle camera permission denial, ARCore not installed or outdated, and session exceptions without crashing.
- **Tracking state:** every AR action must check `TrackingState.TRACKING`. When tracking is lost, fade the overlay and show guidance. Do not place or move anything while tracking is not good.
- **Planes:** use only horizontal upward-facing planes. Lock the chosen plane and stop switching. Reject the floor (distance and normal checks with named constants).
- **Anchors:** one overlay anchor on the locked plane. Do not create or recreate anchors every frame. Detach and dispose old anchors on re-calibration.
- **Hit testing and taps:** after the plane is locked, convert screen taps to camera rays and intersect with the plane mathematically. Handle ray parallel to plane, intersection behind the camera, and implausibly far points.
- **Depth and instant placement:** enable depth AUTOMATIC only if the device supports it. Instant Placement is a fallback and must be labeled less stable.
- **Camera images:** any CPU image acquired from a frame must be closed promptly. Do not hold frames across threads.
- **Per-frame code:** the render and frame callback must not allocate objects, decode bitmaps, run OpenCV, or touch disk. Reuse matrices and arrays.
- **Threads:** heavy work goes on background coroutines. Only small results return to the render thread.
- **Never save world coordinates or anchors.** Save paper-normalized values only.
- **Unit-test** pure geometry (see `math-and-testing-rules`).
