# Architecture Decision Records (ADRs) — TraceAR (`ar_drawing_app`)

This document records the foundational technical and architectural decisions made in **TraceAR**, the rationale behind each choice, and alternatives considered.

---

## ADR 1: Anchor to the Table Plane Instead of Detecting Paper as a Plane

- **Decision**: Calibrate against the physical table/desk horizontal plane, rather than treating the paper itself as an AR plane.
- **Context**: A sheet of paper is typically thin (~0.1 mm) and feature-poor (blank white surface). ARCore often fails to detect small, thin paper sheets as distinct planes, or creates unstable, jittery plane estimates that oscillate in elevation.
- **Rationale**: A tabletop provides a large, rigid, visually textured physical plane that ARCore detects with high confidence. Because paper rests directly flat on the tabletop, anchoring to the tabletop plane provides sub-millimeter vertical stability and eliminates floating or z-fighting artifacts.

---

## ADR 2: Corner Marking via Ray-Plane Intersection Rather than ARCore Hit Tests

- **Decision**: When the user taps a screen point to place or adjust a corner, compute an optical camera ray $( \mathbf{P}_{cam}, \mathbf{d}_{ray} )$ and mathematically intersect it with the locked infinite 3D table plane $P = (\mathbf{p}_0, \hat{\mathbf{n}})$.
- **Context**: ARCore's `frame.hitTest(x, y)` only returns hits if the ray intersects an actively detected ARCore point-cloud feature or plane polygon at that precise instant. If the user taps a corner near the edge of the paper where feature points are sparse, ARCore hit tests frequently fail or return points at erratic depths.
- **Rationale**: An analytical ray-plane intersection always yields an exact mathematical point on the calibrated drawing plane, regardless of whether ARCore has tracking features under the user's finger.

---

## ADR 3: Paper-Normalized Coordinate Space (`u, v ∈ [0, 1]`) for Persistent Storage

- **Decision**: Store all saved project data (drawing boundaries, crops, section grids, proportion lines) strictly in paper-normalized coordinates $[0, 1]$, where $(0,0)$ is top-left and $(1,1)$ is bottom-right.
- **Context**: ARCore world space (meters) is non-deterministic and ephemeral; reopening the app or moving the phone reinitializes the world origin. Anchor-local coordinates depend on the exact position of the initial hit point.
- **Rationale**: Paper-normalized coordinates are completely invariant to camera distance, phone orientation, table height, and real-world scale. When a project is saved and resumed later, the user only needs to mark the 4 paper corners again, and all artwork, guides, and section states restore with 100% geometric accuracy.

---

## ADR 4: Single Physical Anchor per Overlay

- **Decision**: Maintain exactly one ARCore `Anchor` per tracing session, positioning all 4 corners and the quad mesh relative to this anchor in local space.
- **Context**: An alternative design would create 4 independent ARCore anchors—one for each paper corner.
- **Rationale**: ARCore continuously refines individual anchors independently as it gathers sensor data. Having 4 separate anchors causes the paper quad to stretch, skew, and deform non-rigidly over time, ruining drawing proportions. A single anchor guarantees that the 4-corner geometry remains rigid and physically true to the real sheet of paper.

---

## ADR 5: Paper Lock as a 2D Rigid Transform on the Plane

- **Decision**: Model physical paper movement as a 3-DOF rigid planar transformation $(\Delta x, \Delta z, \Delta \theta)$ on the table plane, rather than a full 6-DOF 3D pose.
- **Context**: Paper is constrained by gravity and friction to remain flat on the table surface. Allowing 6 degrees of freedom (including pitch, roll, and elevation $y$) introduces out-of-plane tilting and floating bugs.
- **Rationale**: Constraining motion to 2D translation and in-plane yaw on the table plane prevents numerical instability, guarantees that the overlay never lifts off the table, and allows fast, lightweight pose fusion between edge unprojection and ORB feature matching.

---

## ADR 6: Zero Network Access & 100% On-Device Processing

- **Decision**: Omit `android.permission.INTERNET` entirely and bundle all computer vision algorithms locally via OpenCV for Android.
- **Context**: Many tracing and drawing apps rely on cloud image processing, telemetry, or remote ads.
- **Rationale**: Artists use TraceAR in workshops, outdoors, and studios where Wi-Fi may be unavailable. Complete offline execution guarantees privacy (drawings and camera feeds never leave the phone), eliminates network latency, and ensures the app functions forever without backend server dependencies.
