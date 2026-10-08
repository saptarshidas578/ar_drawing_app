# Privacy Policy for TraceAR

**Last Updated: October 8, 2026**

TraceAR ("we", "our", or "the app") is committed to respecting and protecting your privacy. This Privacy Policy explains how TraceAR handles your information when you use our mobile application.

---

## 1. Summary: 100% On-Device & Offline
- **We do not collect, store, transmit, or sell any personal information.**
- **TraceAR does not connect to the internet.** The app has no network access permissions (`android.permission.INTERNET` is not included in the application).
- All image processing, camera tracking, and augmented reality computations happen exclusively on your device.

---

## 2. Camera Usage
- **Camera Permission (`android.permission.CAMERA`)**:
  - TraceAR requires camera access solely to project your reference image onto your physical paper via Augmented Reality (ARCore).
  - The live camera feed is processed in real-time in your device's memory for paper edge detection and spatial positioning.
  - Camera frames are discarded immediately after processing. Camera data is never saved, uploaded, recorded, or transmitted to any server.

---

## 3. Storage & User Content
- **Reference Images & Projects**:
  - When you import an image to trace, a local copy is stored within your device's private app sandbox storage (`context.filesDir`).
  - Project configurations (opacity, scale, rotation, grid settings, and drawing guide lines) are saved locally on your device.
  - You can delete any project or its files at any time directly from the app's Home screen.
- **Photo Captures & Timelapse Videos**:
  - When you choose to export a photo or timelapse video of your artwork, the file is saved directly to your device's local media gallery (`Pictures/TraceAR`) using Android's standard MediaStore API.
  - If you choose to share an image or video, TraceAR invokes your device's native system share sheet (`FileProvider`). The file is transferred only to the app or recipient you explicitly select.

---

## 4. Third-Party Services, Analytics & Advertising
- TraceAR contains **no third-party tracking libraries**, **no analytics SDKs**, **no advertising networks**, and **no crash reporting services**.
- We do not use cookies or tracking identifiers.

---

## 5. Children's Privacy
Because TraceAR does not collect, store, or share any personal information from anyone, it complies with global children's privacy regulations including the Children's Online Privacy Protection Act (COPPA) and the General Data Protection Regulation (GDPR).

---

## 6. Permissions Summary
| Permission | Why It Is Needed |
|---|---|
| `android.permission.CAMERA` | To display the camera view and anchor the drawing overlay to your paper via Augmented Reality. |

---

## 7. Changes to This Policy
If we make changes to this Privacy Policy in future updates, we will update the "Last Updated" date at the top and include the new policy within the app and on our store listing.

---

## 8. Contact Us
If you have any questions or feedback regarding this Privacy Policy, please contact the developer via the email listed on our Google Play Store page.
