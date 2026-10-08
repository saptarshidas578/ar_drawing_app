# Pre-Release & Google Play Checklist — TraceAR

Before uploading your build to Google Play Console or sharing it publicly, run through this comprehensive pre-release checklist.

---

## 1. Google Play Console Account Verification (IMPORTANT)

> [!WARNING]
> **Check current Google Play Console rules directly in the Console**:
> Google regularly updates requirements for new developer accounts. Do **not** rely solely on memory or older guides:
> - **Testing Requirements**: Personal developer accounts created after November 13, 2023 are required to run a **Closed Test with at least 20 testers opted in for at least 14 days** before applying for production access. Check your Play Console dashboard to see if your account requires this.
> - **Identity Verification**: Verify your government ID, residential address, and phone number as requested in the Play Console.
> - **Organization Accounts**: If publishing under an organization, ensure your **D-U-N-S number** and legal entity verification documents are ready.
> - **Registration Fee**: Google charges a one-time $25 registration fee.
> - **Target API Requirements**: Google Play mandates target SDK levels each year (typically Android 14/15 or API 34/35). Always check the latest Play Console policy notices.

---

## 2. Testing On Physical Hardware

### Test on at least Two Physical Devices
ARCore and camera hardware differ substantially between device manufacturers:
- [ ] **Device 1 (Primary Test Phone)**: Verify plane detection, paper lock, edge detection, and drawing guides work smoothly.
- [ ] **Device 2 (Secondary / Different OEM)**: Verify camera aspect ratio, touch coordinates, and performance on another phone (e.g. Samsung vs. Pixel vs. Xiaomi).
- [ ] **Battery & Thermal Check**: Trace for 15–20 minutes continuously. Confirm device does not overheat or thermal throttle ARCore frame rate below usable levels.

### Fresh Install & Onboarding Experience
- [ ] **Clean Uninstall First**: Uninstall any existing debug APK (`adb uninstall com.tracear.app`) to simulate a first-time user.
- [ ] **Camera Permission Flow**: Verify the runtime permission prompt appears cleanly. If user grants it, camera stream opens immediately without app crash.
- [ ] **Permission Denial Handling**: If camera permission is denied, verify an informative UI explains why the camera is required and offers a button to open App Settings.
- [ ] **First-Time Tutorial / Help**: Verify tutorial tips or guided hints appear correctly on fresh install and do not crash.

### Unsupported / Non-ARCore Device Behavior
- [ ] **ARCore Availability Check**: If run on a device without ARCore or where ARCore is not installed, verify that Google Play Services for AR prompt appears (or a graceful error dialog explains the device lacks ARCore support).

### Release vs. Debug Validation
- [ ] **Release Build Tested**: Never publish without testing the exact **release build** (`assembleRelease`). R8 minification and resource shrinking can strip classes or methods that worked fine in debug.
- [ ] **No Debug UI Visible**: Verify that debug bounding boxes, FPS counters, or diagnostic overlay test buttons are hidden by default in production.
- [ ] **No Test Data or Placeholders**: Ensure sample images load cleanly and no hardcoded test strings or dummy URLs are present.

---

## 3. Core Feature Regression Check

- [ ] **Paper Detection & Lock**: Tap 4 corners or auto-detect. Confirm paper quad locks and tracks.
- [ ] **Drawing Guides**: Toggle Proportion Grid (Count mode & Real Size mode). Check chessboard badges (A1, B2).
- [ ] **Tonal Layers**: Load an image, open Tonal Layers, adjust tone count (2 to 6), solo a tone, and verify overlay colors contrast with paper.
- [ ] **Lines-Only & Adjustments**: Toggle Canny edge detection, invert mode, and brightness/contrast sliders.
- [ ] **Export & MediaStore**:
  - [ ] Tap Capture Photo: verify photo appears in phone Gallery.
  - [ ] Tap Rectified Scan: verify flat top-down photo is saved.
  - [ ] Record 10-second Timelapse: verify smooth MP4 playback in Gallery.
- [ ] **Lifecycle (Background / Foreground)**:
  - [ ] Press Home button while tracking; reopen app: session resumes without crash.
  - [ ] Lock screen and unlock: camera reinitializes cleanly.

---

## 4. Keystore & Security Verification

- [ ] `keystore.properties` is listed in `.gitignore` and **NOT** committed to git.
- [ ] Release `.jks` / `.keystore` file is safely stored in an offline backup (e.g., password manager, encrypted USB, or cloud vault).
- [ ] Keystore password, key alias, and key password are documented securely.
- [ ] ProGuard / R8 mapping file (`app/build/outputs/mapping/release/mapping.txt`) is retained or uploaded to Play Console for de-obfuscating crash stack traces.
