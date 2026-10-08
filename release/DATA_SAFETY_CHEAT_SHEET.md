# Google Play Console Data Safety Form Cheat Sheet — TraceAR

This document gives you exact, question-by-question answers for the **Data Safety** section in Google Play Console, verified against the actual TraceAR codebase.

---

## Codebase Audit Summary
- **Internet Permission (`android.permission.INTERNET`)**: **NOT PRESENT**. The app cannot make network calls.
- **Third-party SDKs**: Only Google ARCore, SceneView, Filament, and OpenCV are used. No advertising, analytics, crash-reporting, or telemetry SDKs are present.
- **Camera Permission (`android.permission.CAMERA`)**: Used solely for real-time local augmented reality tracking and local photo capture. Camera frames are processed strictly in RAM and never transmitted.
- **Storage / Photos**: User explicitly selects reference photos via system photo picker (`ActivityResultContracts.GetContent`). Exported drawings and timelapses are written to public MediaStore gallery.

---

## 1. Overview Questions

### Does your app collect or share any of the required user data types?
- **Answer**: **No**
- *Explanation*: The app does not collect any user data off the device, and does not share any user data with third parties.

> [!NOTE]
> Under Google Play's definition:
> - **Collection** means transmitting data off the device to a server. Processing camera frames or sensor data ephemerally in RAM on the device is **not** considered collection.
> - **Sharing** means transferring data off the device to a third party.

### Is all of the user data collected by your app encrypted in transit?
- *If asked*: **Not applicable** (or "Yes" if forced by the form, noting that no data leaves the device).

### Do you provide a way for users to request that their data be deleted?
- **Answer**: **No** (or **Yes, via app uninstall / local data deletion**)
- *Explanation*: Since all data (projects, settings, cache) is stored strictly in the local app sandbox on the device, users can delete all data at any time by clearing app data in Android Settings or uninstalling the app.

---

## 2. Data Types Breakdown

If Google Play prompts you through specific categories, answer **"Not collected"** for all of them:

| Category | Real Code Behavior | Form Answer |
| :--- | :--- | :--- |
| **Location** (Approximate or Precise) | No location permissions declared or used. | **Not collected** |
| **Personal Info** (Name, Email, Phone, Address) | No accounts, sign-in, or forms. | **Not collected** |
| **Financial Info** (Credit card, bank, purchase history) | No payments or in-app billing. | **Not collected** |
| **Health and Fitness** | None. | **Not collected** |
| **Messages** (SMS, Chat) | None. | **Not collected** |
| **Photos and Videos** | Handled locally on-device. Photos selected by the user stay in app memory/sandbox. Exported images/timelapses are saved to MediaStore. None are sent to a server. | **Not collected** (Ephemeral local processing only) |
| **Audio Files** | Microphone is not requested or accessed. | **Not collected** |
| **Files and Docs** | Local project files only. | **Not collected** |
| **Calendar** | None. | **Not collected** |
| **Contacts** | None. | **Not collected** |
| **App Activity** (Page views, clicks, search history) | No analytics SDKs (no Firebase, no Mixpanel). | **Not collected** |
| **Web Browsing** | No web views, no browsers. | **Not collected** |
| **App Info and Performance** (Crash logs, diagnostics) | No remote crash reporting. | **Not collected** |
| **Device or other IDs** (Advertising ID, IMEI, Android ID) | No Advertising ID permission (`AD_ID`), no device IDs collected. | **Not collected** |

---

## 3. Permissions Declarations (Play Console "App Content" Section)

### Camera Permission
- **Purpose**: Real-time Augmented Reality tracking and live camera overlay for drawing.
- **Disclosure**: "TraceAR uses the camera solely to project drawing guides and reference images onto your paper in real time. Camera frames are processed strictly on your device and are never recorded without user request or sent over the internet."

### Target Audience & Content
- **Target Age**: 13 and older (Recommended: "13-15", "16-17", "18 and over").
  *Tip: Selecting ages under 13 triggers Google Play Families Policy compliance requirements, which require additional COPPA privacy disclosures.*
- **Contains Ads**: **No** (TraceAR does not contain ads).

---

## 4. Privacy Policy URL
- In the Play Console **App Content > Privacy Policy** field, enter a public URL where you host [PRIVACY_POLICY.md](file:///c:/Users/lenovo/ar_drawing_app/release/PRIVACY_POLICY.md).
- *Easy options for hosting*:
  1. A GitHub Pages site or raw GitHub markdown URL in your repository.
  2. A Notion public page.
  3. A free static page on Google Sites or Cloudflare Pages.
