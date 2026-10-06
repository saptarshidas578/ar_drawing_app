---
name: privacy-and-release
description: Use for permissions, data handling, release builds, store listing, privacy policy, and anything about publishing the app.
---

- The app uses the camera and processes images locally. Do not add networking, analytics, ads, or crash reporting without telling the user clearly, because that changes the privacy policy and store forms.
- The manifest must declare the camera permission and mark the app as requiring ARCore (camera AR feature and the ARCore metadata), so the store only offers it to supported phones.
- Release builds use R8 or ProGuard with the keep rules needed by OpenCV, ARCore, and SceneView. Test the release build, not just debug.
- Play Store uploads use a signed Android App Bundle. The signing key and passwords are never committed and the user must back them up safely.
- Keep `versionCode` and `versionName` updated for each release.
- Do not claim store requirements from memory. Tell the user to check the Play Console's current rules for target API level, testing requirements for new developer accounts, the Data safety form, and content rating.
