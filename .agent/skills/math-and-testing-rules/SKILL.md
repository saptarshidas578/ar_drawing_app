---
name: math-and-testing-rules
description: Use for geometry, coordinate conversion, filtering, undo stacks, and any logic that can be tested without a phone.
---

- Put math in pure Kotlin functions with no Android or ARCore dependencies so they can be unit tested.
- Required test areas when touched: ray-plane intersection, corner sorting, normalized-to-world conversion, homography or crop mapping, fit-mode mapping, unit conversion (cm to meters), preset snapping, undo/redo stack, smoothing filters, serialization round trips.
- Test edge cases: zero and negative values, parallel rays, degenerate or self-intersecting corner sets, extreme zoom, very large and very small images, empty input.
- Use tolerances for floating-point comparisons. Name constants and thresholds so they are easy to tune.
- Run the unit tests and report results. If a test cannot be run, say so.
