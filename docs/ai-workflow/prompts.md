# AR Tracer: Antigravity Prompts (Phase 2 and beyond)

Copy each prompt (the text inside a block) into Antigravity, one at a time, in this order.
Every prompt starts by invoking skills from `skills.md`.

## Rules for using these prompts
1. Put `skills.md` in the project root and run **Prompt 0** first (one time).
2. Run **one prompt at a time**. Let it finish and build, then test it on your phone with the checklist it gives you.
3. Each prompt makes the agent commit a checkpoint first. If something breaks, ask Antigravity to revert to the last checkpoint.
4. If a test fails, tell Antigravity exactly what you see (screenshot or description, plus any error text) and ask it to fix it using the `debugging-protocol` skill.
5. Prompts 14 and 13 are optional: run them only if testing shows you need them.

## Order and dependencies

| # | Prompt | Needs |
|---|--------|-------|
| 0 | Install skills (one time) | skills.md |
| 8 | Polish: tutorial, messages, settings, icon, battery saver | Prompts 1 to 7 |
| 9 | Proportion grid and construction lines | 7 (paper size) |
| 10 | Tonal layers, stages, value picker | 3 (lines-only) |
| 11 | Reference preview and accuracy check (beta) | 9, 10 |
| 12 | Timelapse and photo export | 8 |
| 13 | Printed marker fallback (optional) | 4 |
| 14 | Hand occlusion (optional, experimental) | 4 |
| 15 | Stability and hardening audit | all above |
| 16 | Release build and Play Store preparation | 15 |
| 17 | Languages: English, Tamil, Hindi (optional) | 8 |

---

## Prompt 0: install the skills (run once)

```text
I added a file called skills.md in the project root. It contains 12 skills for this project, each starting with a heading like "# SKILL 1: ar-tracer-project-context" followed by "name:" and "description:" lines and a body.

Task: split skills.md into Antigravity workspace skills. For each of the 12 skills, create the folder .agent/skills/<name>/ and a SKILL.md inside it, where the file starts with YAML frontmatter:

---
name: <the name>
description: <the description>
---

followed by the skill body, copied exactly (do not shorten or reword it). Keep skills.md in place as the master copy. Also create CHANGELOG.md in the project root with a first entry listing the features that already exist (use the feature list in the ar-tracer-project-context skill).

Then list the 12 folders you created and tell me, in simple steps, how to restart the agent session so the skills are detected. Do not change any app code.
```

---

## Prompt 8: polish (tutorial, friendly messages, settings, icon, battery saver)

```text
USE SKILLS: ar-tracer-project-context, plan-first-workflow, git-safety-and-scope, compose-ui-rules, performance-and-battery, review-and-report.

GOAL
Make the app easy for a first-time user without changing how the AR works. Do not change AR, calibration, tracking, or rendering logic.

BEFORE CODING
Follow plan-first-workflow: read the code, summarize the current flow from app launch to tracing screen, list files you will change, risks, and any questions.

REQUIREMENTS
1. First-run tutorial (5 steps, shown only the first time, reopenable from Settings > "How to use"):
   (1) put the phone on a stand 40 to 60 cm above the paper, (2) use a textured surface and good light, (3) point the crosshair at the paper and move the phone slowly side to side, (4) tap "Use this surface", then mark the 4 corners or use Detect paper, (5) adjust the image, lock it, and trace. Each step has an icon or simple illustration, one short sentence, Back/Next/Skip, and a progress indicator.
2. Plain-language guidance banners (small, temporary, never covering the paper) for: tracking lost, too dark, moving too fast, no surface found after a while, camera permission denied (button to open app settings), ARCore missing or unsupported (explanation and an install/update button where possible).
3. Settings screen from the top bar menu: default opacity, default fit mode, default grid size, default line color, smoothing level, left-handed mode, keep screen on, show debug info, How to use, Reset all settings, About (app name and version). Save persistently.
4. Empty and error states: friendly empty project list; clear messages for missing image files and full storage.
5. App identity: a proper app name and a simple adaptive launcher icon (a paper sheet with a pencil line).
6. Battery saver option: when on, lower the overlay processing and update rate, and make sure the screen and camera are released properly when leaving the tracing screen.

ACCEPTANCE CRITERIA
- A fresh install shows the tutorial once; skipping it works; it can be reopened from Settings.
- Every message in the list appears in the right situation and disappears on its own.
- Settings persist after closing and reopening the app.
- The tracing screen has no new permanent UI.

OUT OF SCOPE
New drawing features, AR changes, languages other than English.

DELIVERABLES
Final report per review-and-report, including a manual test checklist that starts with a fresh install.
```

---

## Prompt 9: proportion grid and construction lines

```text
USE SKILLS: ar-tracer-project-context, plan-first-workflow, git-safety-and-scope, arcore-rendering-rules, compose-ui-rules, math-and-testing-rules, persistence-rules, review-and-report.

GOAL
Add classic drawing guides that sit on the paper (not on the screen) and help the user place shapes accurately.

BEFORE CODING
Follow plan-first-workflow. Read how the section grid, quad mesh, paper size presets, ruler, and the transform sheet work today. Explain how guides will be drawn in paper-normalized (u, v) space so they stay glued to the paper, and how they relate to the existing section grid (they must stay separate features).

REQUIREMENTS
1. Proportion grid: a line grid drawn over the marked paper area. Two modes: by count (2 to 20 columns and rows) or by real size in cm (only when a paper size is known, otherwise disable that mode with a short hint). Cells are labeled like a chessboard (columns A, B, C... and rows 1, 2, 3...), with labels shown only when the grid is on and fading when zoomed in a lot.
2. The grid appears only on the paper overlay in this task. Do not add a grid to any reference thumbnail yet (that comes later with the picture-in-picture preview in Prompt 11).
3. Construction lines (each one a separate toggle): horizontal and vertical center lines, both diagonals, rule of thirds, and golden-ratio lines.
4. Style controls: line color, thickness, and opacity, with a "contrast with paper" auto option reusing the existing auto line color logic.
5. All guides rotate, scale, and follow the paper correctly when the user changes the fit mode or when zoomed. They are not affected by image rotation unless the user turns on "Move guides with image".
6. Guides are included in save and resume (stored in normalized units) and are included in undo only if they were changed through the transform controls (do not add them to the image undo stack unless it is trivial).
7. Controls live in a new "Guides" sheet in the bottom dock (or inside the existing View sheet if the dock is already full). Add short plain-English labels.

ACCEPTANCE CRITERIA
- With the grid on, grid lines stay on the same paper positions while the phone moves and when zooming.
- Changing the count changes the cells immediately without lag.
- Real-size mode shows exact cm cells for A4 and Custom paper sizes.
- Everything still works with lines-only mode, the section grid, lock/unlock, and Hold to Peek (guides hide during Hold to Peek too).

OUT OF SCOPE
Tonal layers, reference preview, any AR or calibration changes.

DELIVERABLES
Unit tests for the grid math (cell counts, labels, cm to normalized conversion). Final report per review-and-report.
```

---

## Prompt 10: tonal layers, step-by-step stages, value picker

```text
USE SKILLS: ar-tracer-project-context, plan-first-workflow, git-safety-and-scope, image-processing-rules, compose-ui-rules, persistence-rules, performance-and-battery, math-and-testing-rules, review-and-report.

GOAL
Help the user draw shading and portraits by splitting the reference image into tonal layers they can trace one at a time.

BEFORE CODING
Follow plan-first-workflow. Read how lines-only mode, image adjustments, the texture pipeline, and save/resume work. Propose the processing pipeline and explain how results are cached.

REQUIREMENTS
1. Tonal layers: convert the image to grayscale, smooth it (for example a bilateral or median filter so regions are clean), then split it into N tones (N adjustable from 2 to 6; default 4) using luminance quantization or k-means on luminance. Each tone becomes its own transparent layer.
2. Layer controls: show/hide each tone, solo a tone, and set each tone's overlay color and opacity (defaults: light to dark ramp that contrasts with typical paper). Add "Outline layer" (the lines-only result) as an extra layer in the same list.
3. Stages mode: a simple stepper that shows one layer at a time in a recommended order (outline first, then mid tones, then darks). The user can jump between stages, and the current stage name is shown in a small chip.
4. Value picker: tap a point on the reference image preview (or enter "Pick" mode and tap the overlay) to show that point's brightness as a percentage and as a tone number, and its color as a swatch and hex code. This must not interfere with gestures when locked.
5. Processing runs on a background thread with a loading indicator, is debounced while sliders move, and results are cached per (image, settings).
6. Everything aligns exactly with the paper using the existing quad and uv mapping (same fit mode, rotation, scale, crop, and flips as the normal image).
7. Settings and layer choices are saved with the project.
8. New controls go in a new "Tones" sheet in the dock.

ACCEPTANCE CRITERIA
- A portrait photo produces clearly separated tones, with each layer traceable on its own.
- Changing N or smoothing updates within about a second on a typical phone and never freezes the AR view.
- The layers line up with the outline layer and with the paper corners.
- No memory crash on a 12 MP photo (images are downscaled for processing).

OUT OF SCOPE
Color painting guides, any AR or calibration change.

DELIVERABLES
Unit tests for the quantization and mapping logic. Report processing time per run in the final report. Final report per review-and-report.
```

---

## Prompt 11: reference preview and accuracy check (beta)

```text
USE SKILLS: ar-tracer-project-context, plan-first-workflow, git-safety-and-scope, arcore-rendering-rules, image-processing-rules, compose-ui-rules, math-and-testing-rules, performance-and-battery, review-and-report.

GOAL
Help the user keep the big picture in view and check how close their drawing is to the reference.

BEFORE CODING
Follow plan-first-workflow. Read how camera frames, the quad mapping, and the paper corners are available in code. Explain how you will rectify the camera view of the paper into paper-normalized space, and how you will keep it cheap and off the render thread.

REQUIREMENTS
1. Reference picture-in-picture (PiP): a small draggable, resizable thumbnail of the full reference image (with a rectangle showing the currently visible region when zoomed). It sits in a corner, has opacity control, can be hidden, and respects left-handed mode. It never blocks the dock or sheets.
2. Blink compare: a "Blink" button that automatically flashes the overlay on and off about once per second while pressed or toggled, so the user can compare the drawing with the lines. It stops on release or toggle off.
3. Accuracy check (label it Beta): a button that takes the current camera frame, uses the four paper corners to rectify the paper into a flat top-down image (perspective warp), and compares what the user has drawn against the reference lines. Show the result as a transparent heat overlay: green where drawn lines are close to the reference, red where reference lines are still missing or far off. The check runs on a background thread, only when the user presses the button, and takes a still snapshot (it does not run continuously).
4. Handle limits honestly: if the hand covers part of the paper or lighting is poor, show a note that results may be inaccurate. Include a short "How accurate is this?" explanation.
5. Show a simple numeric score ("about 72% matched") with a clear explanation that it is approximate.
6. Controls live in a new "Check" sheet, plus the PiP toggle in the View sheet.

ACCEPTANCE CRITERIA
- PiP shows the right region and updates with zoom.
- Blink does not disturb tracking or the lock state.
- The accuracy overlay lines up with the paper and clearly differs between an empty paper and a mostly traced one.
- The check never freezes the AR view and can be cancelled.

OUT OF SCOPE
Automatic correction of the user's drawing, any change to calibration or tracking.

DELIVERABLES
Unit tests for the rectification mapping and the scoring function. Final report per review-and-report, with timing numbers.
```

---

## Prompt 12: timelapse and photo export

```text
USE SKILLS: ar-tracer-project-context, plan-first-workflow, git-safety-and-scope, image-processing-rules, persistence-rules, compose-ui-rules, performance-and-battery, privacy-and-release, review-and-report.

GOAL
Let the user save and share photos of the finished drawing and a timelapse video of the tracing session.

BEFORE CODING
Follow plan-first-workflow. Read how the camera view, overlay, and projects work. Compare the realistic options for capturing frames (for example PixelCopy of the AR surface, or the CPU camera image) and choose the simplest one that is reliable. Explain the choice and its limits (for example, whether the overlay is included).

REQUIREMENTS
1. Photo export: a "Capture" button that saves a photo to the gallery (MediaStore, no legacy storage permission on modern Android). Offer two options: with overlay and without overlay. A share button opens the system share sheet (FileProvider).
2. Rectified paper photo: optional export of a flat, top-down image of the paper made with the four corners (perspective warp), so the result looks like a scan.
3. Timelapse recording: a Record toggle that captures one frame every N seconds (N adjustable from 1 to 30, default 5) while the tracing screen is open. Frames are stored temporarily in app storage with a cap on count and size.
4. Timelapse export: encode the frames into an MP4 (for example with MediaCodec and MediaMuxer) at an adjustable speed (for example 10 to 30 fps playback), save to the gallery, and offer sharing. Show progress and allow cancel. Clean up temporary frames afterward.
5. A small recording indicator chip on screen while recording, and automatic pause when the app goes to the background.
6. Show clear messages for low storage and failures.
7. Everything is stored locally. Do not add any networking.

ACCEPTANCE CRITERIA
- A photo appears in the gallery with the correct orientation and content.
- A one-minute test session produces a playable video.
- Recording does not noticeably reduce AR smoothness (report frame-time impact).
- Temporary files are removed after export or cancel.

OUT OF SCOPE
Cloud sharing, accounts, editing videos.

DELIVERABLES
Unit tests for any pure logic (frame scheduling, file naming, size caps). Final report per review-and-report.
```

---

## Prompt 13: printed marker fallback (optional)

```text
USE SKILLS: ar-tracer-project-context, plan-first-workflow, git-safety-and-scope, arcore-rendering-rules, compose-ui-rules, math-and-testing-rules, debugging-protocol, review-and-report.

GOAL
Give users a very stable fallback when plane detection is unreliable: a printed marker image placed next to the paper, tracked with ARCore Augmented Images.

BEFORE CODING
Follow plan-first-workflow. Read the current calibration flow and explain how a marker-based plane will plug into it without changing the existing flow for users who do not use markers.

REQUIREMENTS
1. Marker asset: include a high-contrast, feature-rich marker image in the app, with its real printed width defined as a constant (for example 10 cm). Add an "Export marker" action that saves or shares the image for printing and shows printing instructions (print at 100% size, do not scale, measure the printed width). Verify the image quality using ARCore's image database: if adding the image throws an insufficient-quality error, say so clearly in the plan and pick another image.
2. Marker mode in the calibration flow: a toggle "Use printed marker". When on, create an Augmented Image database with the marker and track it with full tracking.
3. When the marker is tracked, use its pose as the working plane (the marker lies flat on the same table). Then the user marks the 4 paper corners (tap or Detect paper) and the existing ray-plane intersection is used with this plane. Create the single overlay anchor from the marker's pose, and keep using the same corner and transform logic.
4. Keep the marker plane stable: use the marker's pose while it is visible, and keep the last good anchor if the marker leaves the view (do not require the marker to stay visible afterward). Show a status chip: Marker found, Marker lost (still tracking), Tracking lost.
5. If the marker is not found after a while, show tips (more light, flat marker, closer or farther).
6. Provide a clear "Back to normal mode" switch that restores the existing plane flow.

ACCEPTANCE CRITERIA
- With the marker on a blank white desk (a surface where normal plane detection fails), calibration works and the overlay stays aligned.
- Normal mode behaves exactly as before.
- Re-calibration fully resets marker mode state with no leaks.

OUT OF SCOPE
Multiple markers, marker-size calibration UI.

DELIVERABLES
Unit tests for any pure math. Final report per review-and-report, including how to print and test the marker.
```

---

## Prompt 14: hand occlusion (optional, experimental)

```text
USE SKILLS: ar-tracer-project-context, plan-first-workflow, git-safety-and-scope, arcore-rendering-rules, performance-and-battery, debugging-protocol, review-and-report.

GOAL
Hide the overlay where the user's hand and pencil are in front of the paper, so the lines do not draw on top of the hand. This is experimental.

BEFORE CODING
Follow plan-first-workflow. This is a high-risk change. In the plan, compare two approaches and recommend one with reasons:
(A) ARCore Depth: use the current, non-deprecated depth image API (only on devices that support depth), and hide overlay pixels that are clearly closer to the camera than the paper plane.
(B) A hand mask from an on-device hand landmark or segmentation model, used to cut out the overlay.
Report the realistic quality, device support, performance cost, and how you will keep AR smooth. Ask me before choosing option B, because it adds a large dependency.

REQUIREMENTS
1. Feature toggle "Hand occlusion (beta)" in the View sheet. Default off. Hidden or disabled with a short reason on devices that do not support the chosen method.
2. When on, the overlay is hidden (or strongly faded) wherever the hand is detected in front of the paper, with a soft edge to avoid flicker. Smooth over a few frames.
3. A threshold control ("Sensitivity") so the user can tune it.
4. Automatic safety: if the frame time gets too high, turn the feature off and tell the user why.
5. Hold to Peek must keep working and remain the reliable fallback.

ACCEPTANCE CRITERIA
- On a supported phone, the overlay disappears behind a hand held clearly above the paper and returns when the hand moves away.
- With the toggle off there is zero performance impact and no behavior change.
- No crashes on unsupported devices.

OUT OF SCOPE
Pencil-tip tracking, any calibration or tracking changes.

DELIVERABLES
Measured frame-time impact and a candid statement of quality limits. Final report per review-and-report.
```

---

## Prompt 15: stability and hardening audit

```text
USE SKILLS: ar-tracer-project-context, plan-first-workflow, git-safety-and-scope, arcore-rendering-rules, image-processing-rules, persistence-rules, performance-and-battery, math-and-testing-rules, debugging-protocol, review-and-report.

GOAL
Find and fix hidden problems before release. This is an audit-and-fix task: do not add features.

BEFORE CODING
Follow plan-first-workflow. Audit the whole project and write findings as a list ranked by severity (crash, data loss, wrong behavior, performance, minor), each with file names, the evidence from the code, and the proposed fix. Show me the list before fixing anything, then fix the critical and high items and re-check.

CHECK AT LEAST THESE AREAS
1. ARCore session lifecycle: pause/resume, app switching, screen off, permission revoked while running, session exceptions.
2. Anchors and calibration reset: leaks after many re-calibrations; all old anchors, nodes, and bitmaps released.
3. Memory: bitmaps, OpenCV Mats, textures, tiles, timelapse frames. Look for leaks and large copies. Add LeakCanary to debug builds only if it is not already present.
4. Threads and coroutines: main-thread work, unscoped coroutines, jobs that continue after leaving the screen.
5. Rotation and process death: state restore for the tracing screen and projects.
6. Save/load: corrupt data, missing files, schema versions, migration from older saves.
7. Per-frame allocations and hot paths in the render loop.
8. Battery and heat: update rates, torch and wake lock handling.
9. UI: touch targets, small and large screens, dark and light themes, accessibility labels, left-handed mode, text that is cut off.
10. Android Lint warnings that indicate real problems, and Gradle warnings.
11. Unit test coverage for the pure logic listed in math-and-testing-rules. Add missing tests.

ACCEPTANCE CRITERIA
- A written audit list with severity and status (fixed, not fixed with reason).
- All critical and high items fixed, and the project builds cleanly with all unit tests passing.
- A "30 to 60 minute real session" test plan I can run on my phone, with specific things to watch (drift, heat, battery, memory, re-calibrate 10 times, save and resume, rotate the phone, background and return).

OUT OF SCOPE
New features, visual redesign.

DELIVERABLES
Final report per review-and-report, with the audit list included.
```

---

## Prompt 16: release build and Play Store preparation

```text
USE SKILLS: ar-tracer-project-context, plan-first-workflow, git-safety-and-scope, privacy-and-release, review-and-report.

GOAL
Prepare the app for sharing and, if I choose, publishing on Google Play. Do not add features.

BEFORE CODING
Follow plan-first-workflow. Read the manifest, Gradle files, dependencies, and permissions. Summarize what data the app collects or sends (it should be none beyond local storage and the camera) and list what needs to be done, in order.

REQUIREMENTS
1. Manifest check: camera permission, AR Required settings (camera AR feature and ARCore metadata), correct app name, icon, minimum SDK, and target SDK. Tell me to confirm the current required target API level in the Play Console instead of guessing it.
2. Release build configuration: enable R8 minification and resource shrinking with the keep rules needed for OpenCV, ARCore, and SceneView. Build a release version and tell me how to test it on my phone, because release builds can behave differently from debug.
3. Signing: explain, in beginner steps, how to create an upload keystore in Android Studio, how to configure signing safely without committing the keystore or passwords, and where to back them up. Make sure .gitignore covers them.
4. Versioning: set versionCode and versionName, and explain how to increase them for each update.
5. Build outputs: an Android App Bundle (.aab) for the Play Store, and a signed APK I can send directly to friends for testing.
6. Documents to generate in a /release folder: PRIVACY_POLICY.md (plain language, local-only processing, camera use, no data collected or shared; update it if any networking or analytics exists), a Play Store listing draft (app name, short description under 80 characters, full description, feature list), screenshot suggestions (list which screens to capture), and a "Data safety form answers" cheat sheet based on the real code.
7. A pre-release checklist: tested on at least two phones, tutorial verified on fresh install, release build tested, ARCore unsupported device behavior checked, no debug UI visible by default.
8. Tell me to check the Play Console's current rules for new developer accounts (testing requirements, verification, fees) instead of stating them from memory.

ACCEPTANCE CRITERIA
- A signed release APK and AAB build successfully.
- The release build runs on my phone with AR working (R8 did not break anything).
- The /release documents exist and match what the app really does.

OUT OF SCOPE
Adding analytics, ads, accounts, or any networking.

DELIVERABLES
Final report per review-and-report, with step-by-step instructions for every manual step I must do in Android Studio and the Play Console.
```

---

## Prompt 17: languages, English, Tamil, Hindi (optional)

```text
USE SKILLS: ar-tracer-project-context, plan-first-workflow, git-safety-and-scope, compose-ui-rules, review-and-report.

GOAL
Add Tamil and Hindi translations of all user-facing text.

BEFORE CODING
Follow plan-first-workflow. Search the project for any hardcoded text that is not in strings.xml and list it. Move it into strings.xml first (English), then translate.

REQUIREMENTS
1. All visible text comes from strings.xml, including tutorial text, banners, buttons, settings, errors, content descriptions, and notification text.
2. Add values-ta (Tamil) and values-hi (Hindi) resource folders with natural, simple translations. Use correct plural forms and placeholders. Mark any string you are unsure about with a translator comment so I can have it reviewed by a native speaker.
3. The app follows the phone's language by default. Add a language option in Settings (System default, English, Tamil, Hindi) using the platform's per-app language support.
4. Check layouts with longer text and with these scripts: no cut-off or overlapping text in the dock, sheets, tutorial, and dialogs. Use fonts that render Tamil and Hindi correctly.
5. Do not translate technical log messages or debug panel text.

ACCEPTANCE CRITERIA
- Switching the language updates all screens, including the tutorial.
- No hardcoded text remains.
- The build passes Lint without missing-translation errors.

OUT OF SCOPE
Right-to-left languages, voice guidance.

DELIVERABLES
Final report per review-and-report, with a list of strings that need native-speaker review.
```

---

## After the last prompt

Run a full, real tracing session of at least 30 minutes, write down what annoys you, and use those notes to decide the next round of features. Good candidates that are not in these prompts: multiple reference images on one paper, text and shape overlays, color-painting guides, and an iPhone version (a separate project).
