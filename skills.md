# AR Tracer: Antigravity Skills

This file holds 12 skills for the AR tracing app (Android, Kotlin, ARCore).
Every prompt in `prompts.md` tells the agent which skills to apply.

**How Antigravity loads skills:** each skill is a folder `.agent/skills/<name>/SKILL.md` inside the project (workspace scope), starting with YAML frontmatter containing `name` and `description`. The agent sees only the name and description at first, then reads the full skill when it looks relevant. You can also force a skill by naming it in your prompt.

**Easiest setup:** put this file in your project root and run **Prompt 0** from `prompts.md`. It splits this file into the 12 skill folders for you. Restart the agent session afterwards so Antigravity re-detects them.

---

# SKILL 1: ar-tracer-project-context

name: ar-tracer-project-context
description: Use at the start of EVERY task on the AR tracing app. Explains what the app is, its coordinate spaces, the rules that must never break, and the inventory of features already built.

## What the app is
An Android app where the user picks a reference image, the phone camera looks at a sheet of paper on a table, and the image is drawn over the paper, anchored in the real world. The user watches the screen and traces with a pencil on the real paper. The overlay must stay locked to the paper as the phone moves closer, farther, tilts, or rotates.

## Stack
Kotlin, Jetpack Compose, ARCore, SceneView, OpenCV for Android, local storage for projects and settings. Confirm the real versions and libraries by reading `build.gradle` files. Do not assume.

## Coordinate spaces (never mix them up)
1. **World space:** ARCore's meters. Never saved to disk, because ARCore sessions do not persist between runs.
2. **Anchor-local space:** positions relative to the single overlay anchor on the locked plane. Used while the session runs.
3. **Paper-normalized space (u, v), each from 0 to 1:** position across the 4 marked paper corners, origin at the top-left corner. This is the ONLY space used for saving data to disk.

## Invariants (never change without the user's explicit permission)
1. Calibration flow: find and lock the table plane, then mark 4 paper corners (tap or Detect paper).
2. The table plane is the working plane. The paper itself is never detected as a plane.
3. Corner taps become camera rays and are intersected mathematically with the locked plane's infinite plane (ray-plane intersection), not ARCore hit tests.
4. One anchor for the overlay; corners and image transform stored in its local space.
5. The image is a quad mesh with 4 vertices and correct UVs stretched between the corners. Fit modes: Fill, Fit, Stretch.
6. Saved data uses paper-normalized coordinates only.
7. The camera view is the main thing on screen. UI is a slim top bar, a compact bottom dock, and small bottom sheets (sheet at most 30% of screen height; UI under 15% of the screen when no sheet is open).
8. Lock/unlock: locked means image gestures are disabled (and pinch zooms the view); unlocked means gestures edit the image.

## Features already built (do not break any of these)
- Foundation: gallery image picker, camera permission, ARCore support check, 4-corner calibration with markers and undo, quad overlay, opacity slider, change image, keep screen awake, portrait and landscape.
- Placement: corner auto-sorting, draggable corner handles, fit modes, rotate/scale/move by gesture, rotation slider, 90 degree buttons, flips, reset image, lock/unlock, full re-calibration reset, debug outline.
- Zoom and sections: view zoom 1x to 4x with pan and reset, section grid 2x2 to 6x6, active-section highlight with dimming, focus section, next/previous in snake order, mark done with progress, confirmation before re-calibrating, high-resolution image loading and tiling.
- Detection: crosshair surface selection, floor rejection, "Use this surface" lock, OpenCV Detect paper with stability check, fallbacks (depth, instant placement, scan quality, tips), debug panel.
- Quality: overlay smoothing, tracking status chip, fade when tracking is lost, image adjustments (brightness, contrast, invert, threshold), torch, full-brightness option, low-light hint.
- Projects: lines-only mode (sensitivity, thickness, color), project list, save and resume, autosave.
- UI: camera-first layout with top bar, bottom dock, sheets, Hold to Peek, Focus mode, left-handed mode, auto line color.
- Precision: paper size presets, real-world size, ruler, fine nudge, snap rotation, undo/redo, crop.

Maintain a `CHANGELOG.md` in the project root. At the end of every task, append what changed and which features now exist. Do not edit this skill file to record progress.

## About the user
The user is a beginner. Explain in plain English, define any jargon the first time, give exact steps for anything they must do by hand, and say clearly what they should test on their phone.

---

# SKILL 2: plan-first-workflow

name: plan-first-workflow
description: Use before making any change. Forces the agent to read, think, plan, and ask before coding, then implement in small verified slices.

1. **Read first.** Open `ar-tracer-project-context`, then every file that the task touches. Never guess what code does.
2. **Summarize current behavior** in plain English, naming the files and functions involved.
3. **Find the cause or design.** For a bug, identify the likely root cause with evidence from the code (see `debugging-protocol`). For a feature, describe the design and where it plugs in.
4. **Write a plan** before any code: files to change or create, ordered steps, risks, what could break, and how it will be tested. Keep it short and concrete.
5. **Ask only if truly blocked.** At most 3 questions, each with a recommended default. If not blocked, proceed on clearly stated assumptions.
6. **Implement in small slices.** After each slice, make sure the project still builds, then commit (see `git-safety-and-scope`).
7. **Self-review** the finished work with `review-and-report`.
8. **Be honest.** Never claim something works unless it was built or tested. The agent cannot run ARCore on a phone, so list exactly what could not be verified and must be checked on a real device.

Think about the simplest solution that satisfies the requirements. Do not add features that were not requested.

---

# SKILL 3: git-safety-and-scope

name: git-safety-and-scope
description: Use for every task that changes files. Protects working code with checkpoints, branches, and strict scope control.

- Run `git status`. If there is no repository, run `git init` and make a baseline commit.
- Before starting, commit a checkpoint: `checkpoint before <feature>`. Work on a branch named `feature/<short-name>`.
- Commit after each working slice with a clear message. Never rewrite history or force-push.
- Change only what the task requires. No unrelated refactors, renames, or formatting sweeps.
- Never delete an existing feature, file, or setting without saying so and getting permission.
- Do not upgrade Gradle, AGP, Kotlin, ARCore, SceneView, OpenCV, or other dependencies unless the task needs it. If you must, say why and which version.
- Never commit secrets: keystores, passwords, API keys. Make sure `.gitignore` covers them.
- At the end, tell the user how to undo the whole task (the exact git command or branch to go back to).

---

# SKILL 4: arcore-rendering-rules

name: arcore-rendering-rules
description: Use for anything involving ARCore, SceneView, anchors, planes, tracking, camera frames, depth, or the overlay mesh.

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

---

# SKILL 5: compose-ui-rules

name: compose-ui-rules
description: Use for any Jetpack Compose or screen layout change. Keeps the camera view dominant and the UI consistent and accessible.

- The camera/AR view is the hero. Keep the layout from `ar-tracer-project-context`: slim top bar, bottom dock, small sheets. Nothing new may take permanent screen space.
- New controls go into an existing sheet or a new sheet in the dock, not onto the main screen.
- State lives in ViewModels with immutable UI state. Composables are small and reusable.
- Never recreate or restart the AR view because of UI recomposition. Keep the AR composable stable and keep its inputs minimal.
- Touch targets at least 48 dp. Icons have labels or content descriptions. Support dark and light themes, portrait and landscape, and small and large screens.
- No hardcoded user-facing text: use `strings.xml`.
- Sheets use semi-transparent backgrounds, scroll internally, and close on outside tap, swipe down, or after about 6 seconds of no use.
- Respect left-handed mode for any new buttons.
- Friendly plain-language messages, shown as small temporary banners that do not cover the paper.

---

# SKILL 6: image-processing-rules

name: image-processing-rules
description: Use for OpenCV, bitmaps, edge detection, thresholding, tonal layers, textures, tiling, or any pixel work.

- All image processing runs on a background dispatcher. Never on the main or render thread.
- Downscale large inputs before processing (longest side about 2048 px for processing; keep the original for display).
- Release memory: call `Mat.release()` on every OpenCV Mat, recycle or reuse bitmaps, and avoid holding several full-size copies.
- Debounce slider-driven processing (about 150 ms) and cache the last result. Show a small loading indicator.
- Output overlays as ARGB bitmaps with a transparent background so only the lines or tones appear on the paper.
- Textures must respect the GPU maximum texture size. Use tiles or downscaling when needed.
- Handle failures (decode errors, out of memory) with a friendly message and a safe fallback to the previous result.

---

# SKILL 7: persistence-rules

name: persistence-rules
description: Use when saving or loading projects, settings, images, or exports.

- Store only paper-normalized coordinates and settings, never world coordinates or anchors.
- Copy the reference image into the app's own storage when a project is created so it survives gallery deletion.
- Use a versioned schema. When the format changes, add a migration and keep old projects loadable.
- Settings go in DataStore (or the existing mechanism). Projects use Room or JSON, whichever the project already uses.
- Autosave on leaving the screen and when the app goes to the background. Writes happen off the main thread.
- Handle missing files, corrupted data, and low storage with friendly messages. Never crash on bad data.
- Exports (photos and videos) use MediaStore or the share sheet with FileProvider. Do not request legacy storage permissions on modern Android.

---

# SKILL 8: math-and-testing-rules

name: math-and-testing-rules
description: Use for geometry, coordinate conversion, filtering, undo stacks, and any logic that can be tested without a phone.

- Put math in pure Kotlin functions with no Android or ARCore dependencies so they can be unit tested.
- Required test areas when touched: ray-plane intersection, corner sorting, normalized-to-world conversion, homography or crop mapping, fit-mode mapping, unit conversion (cm to meters), preset snapping, undo/redo stack, smoothing filters, serialization round trips.
- Test edge cases: zero and negative values, parallel rays, degenerate or self-intersecting corner sets, extreme zoom, very large and very small images, empty input.
- Use tolerances for floating-point comparisons. Name constants and thresholds so they are easy to tune.
- Run the unit tests and report results. If a test cannot be run, say so.

---

# SKILL 9: debugging-protocol

name: debugging-protocol
description: Use when something does not work, crashes, drifts, looks wrong, or fails to build.

1. **Reproduce or inspect** before changing anything. Read the full error text and the relevant code.
2. List 2 to 4 plausible root causes and what evidence would confirm each.
3. Add temporary targeted logging with one consistent log tag, or use the debug panel, to confirm the cause.
4. Fix the root cause with the smallest change, not the symptom. Never silence errors or warnings to make the build pass.
5. Verify the fix, remove temporary logging, and make sure nearby features still work.
6. Explain in plain English what was wrong and why the fix works.

For build errors: fix the first error first, because later ones often follow from it. Never fix errors by deleting features.

---

# SKILL 10: review-and-report

name: review-and-report
description: Use at the end of every task. Self-review checklist and the required final report format.

## Self-review (do this before reporting)
- Does it build with no errors? Any new warnings that matter?
- Are all existing features still reachable and working?
- Null cases, lifecycle (pause/resume, rotation), permission denial, low memory, tracking lost?
- Any per-frame allocation or main-thread work that should not be there?
- Strings in `strings.xml`, accessibility labels, dark and light themes?
- Do the unit tests pass? Were tests added for new pure logic?
- Is the code commented in plain English where it matters?

## Final report format
1. **Summary:** what was done, in plain English.
2. **Changed files:** the list, with one line each.
3. **How to undo:** the git branch or command.
4. **Known limits:** anything not verified or imperfect.
5. **Manual test checklist:** a numbered list the user can follow on a real phone, with the expected result for each step.
6. **CHANGELOG.md** updated.

---

# SKILL 11: performance-and-battery

name: performance-and-battery
description: Use when a change could affect frame rate, heat, battery, memory, or startup.

- Target a smooth AR view. Do not add work to the render loop. Process at a lower rate than the camera when possible, and skip frames if the previous job is still running.
- Cancel background jobs when the screen closes or inputs change.
- Avoid large allocations in loops. Reuse buffers.
- Respect Battery Saver mode where it exists.
- Release camera, torch, and AR session promptly when leaving the tracing screen.
- For anything heavy, add a way to measure it (a debug readout or log of milliseconds per run) and report the numbers.

---

# SKILL 12: privacy-and-release

name: privacy-and-release
description: Use for permissions, data handling, release builds, store listing, privacy policy, and anything about publishing the app.

- The app uses the camera and processes images locally. Do not add networking, analytics, ads, or crash reporting without telling the user clearly, because that changes the privacy policy and store forms.
- The manifest must declare the camera permission and mark the app as requiring ARCore (camera AR feature and the ARCore metadata), so the store only offers it to supported phones.
- Release builds use R8 or ProGuard with the keep rules needed by OpenCV, ARCore, and SceneView. Test the release build, not just debug.
- Play Store uploads use a signed Android App Bundle. The signing key and passwords are never committed and the user must back them up safely.
- Keep `versionCode` and `versionName` updated for each release.
- Do not claim store requirements from memory. Tell the user to check the Play Console's current rules for target API level, testing requirements for new developer accounts, the Data safety form, and content rating.
