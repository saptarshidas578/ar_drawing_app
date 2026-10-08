# Privacy Policy — TraceAR (`ar_drawing_app`)

**Last updated:** October 8, 2026

TraceAR is built with a strict **offline-first, zero-telemetry, privacy-by-design** philosophy.

---

## 1. Zero Network Access Guarantee

- **No Internet Permission**: The application does not request the `android.permission.INTERNET` permission in its Android Manifest. It is technically impossible for the application to send or receive data over the internet or communicate with remote servers.
- **No Third-Party Analytics or Tracking**: We do not integrate any analytics SDKs (such as Firebase Analytics, Mixpanel, or Adjust), crash-reporting services (such as Firebase Crashlytics or Sentry), or advertising networks.
- **No User Accounts**: You do not need to create an account, log in, or provide personal details (name, email, phone number) to use the app.

---

## 2. Camera Usage

- **Real-Time AR Projection**: TraceAR requests runtime access to your device camera (`android.permission.CAMERA`) strictly to detect the tabletop surface via Google ARCore and project drawing guides over your paper.
- **Ephemeral RAM Processing**: Camera frames are processed strictly in temporary device memory (RAM) for AR pose estimation and computer vision edge detection. Frames are discarded immediately and are never saved to disk without your direct action.
- **No Video Surveillance**: The camera operates only while the tracing screen is active and ceases immediately when the screen is minimized or closed.

---

## 3. Local Data Storage

All data created or modified by the app remains exclusively on your physical device:
- **Saved Projects**: When you save a tracing project, project metadata and reference images are saved to the app's private sandbox directory (`Context.filesDir`).
- **User Preferences**: Settings such as default opacity, line colors, grid dimensions, and left-handed mode are stored locally via `SharedPreferences`.
- **Photo & Timelapse Exports**: When you explicitly tap **Capture Photo** or **Export Timelapse**, files are written to your device's public photo gallery using modern Android `MediaStore` APIs (`Pictures/TraceAR` and `Movies/TraceAR`).
- **Data Deletion**: Uninstalling the application or selecting "Clear Data" in Android System Settings completely deletes all saved projects and settings.

---

## 4. Verification

This policy has been verified directly against the application source code:
- Manifest: No network permissions declared.
- Dependencies: Only Google ARCore, SceneView, Filament, and OpenCV native libraries are linked. No network clients (such as OkHttp, Retrofit, or Ktor) are bundled.

---

## 5. Contact

For questions regarding privacy, security, or this application, please open an issue on GitHub:
[https://github.com/saptarshidas578/ar_drawing_app/issues](https://github.com/saptarshidas578/ar_drawing_app/issues)
