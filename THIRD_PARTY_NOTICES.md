# Third-Party Software Notices & Licenses

TraceAR (`ar_drawing_app`) incorporates open-source software libraries. This document lists all direct dependencies declared in `app/build.gradle.kts`, their versions, licenses, links, and attribution requirements.

---

## Direct Dependencies

| Library | Version | License | Official Project Link | Notes / Attribution |
| :--- | :--- | :--- | :--- | :--- |
| **Google ARCore** (`com.google.ar:core`) | `1.47.0` | Apache 2.0 | [Google ARCore SDK](https://github.com/google-ar/arcore-android-sdk) | Required for 6-DOF tracking and plane detection. Uses Google Play Services for AR. |
| **SceneView AR** (`io.github.sceneview:arsceneview`) | `2.2.1` | Apache 2.0 | [SceneView Android](https://github.com/SceneView/sceneview-android) | ARSceneView surface and 3D node lifecycle. Bundles Google Filament (Apache 2.0). |
| **OpenCV for Android** (`org.opencv:opencv`) | `4.9.0` | Apache 2.0 | [OpenCV Official](https://opencv.org/) | Computer vision algorithms (Canny edge, perspective warp, contour detection, bilateral filter). *(Licensed under Apache 2.0 since OpenCV 4.5.0)*. |
| **AndroidX Jetpack Compose** (`androidx.compose.*`) | `2024.12.01` (BOM) | Apache 2.0 | [AndroidX Compose](https://developer.android.com/jetpack/compose) | Declarative UI toolkit and Material 3 components. |
| **AndroidX Core KTX** (`androidx.core:core-ktx`) | `1.15.0` | Apache 2.0 | [AndroidX Core](https://developer.android.com/jetpack/androidx/releases/core) | Kotlin extensions for Android framework APIs. |
| **AndroidX Activity Compose** (`androidx.activity:activity-compose`) | `1.9.3` | Apache 2.0 | [AndroidX Activity](https://developer.android.com/jetpack/androidx/releases/activity) | Compose integration with Activity lifecycle and result contracts. |
| **AndroidX Lifecycle** (`androidx.lifecycle:*`) | `2.8.7` | Apache 2.0 | [AndroidX Lifecycle](https://developer.android.com/jetpack/androidx/releases/lifecycle) | Coroutine lifecycle scopes and Compose lifecycle awareness. |
| **Coil Compose** (`io.coil-kt:coil-compose`) | `2.7.0` | Apache 2.0 | [Coil Image Loader](https://coil-kt.github.io/coil/) | Asynchronous image loading for gallery imports. |
| **Accompanist Permissions** (`com.google.accompanist:accompanist-permissions`) | `0.36.0` | Apache 2.0 | [Accompanist](https://github.com/google/accompanist) | Compose permission request wrappers. |
| **LeakCanary** (`com.squareup.leakcanary:leakcanary-android`) | `2.14` | Apache 2.0 | [Square LeakCanary](https://square.github.io/leakcanary/) | Runtime memory leak detection *(debug builds only)*. |
| **JUnit 4** (`junit:junit`) | `4.13.2` | EPL 1.0 | [JUnit.org](https://junit.org/junit4/) | Unit testing framework *(test implementation only)*. |
| **JSON in Java** (`org.json:json`) | `20240303` | Public Domain / JSON License | [JSON-java](https://github.com/stleary/JSON-java) | Unit test serialization mock *(test implementation only)*. |

---

## Full License Texts

### Apache License, Version 2.0
Used by: Google ARCore, SceneView, OpenCV 4.9.0, AndroidX, Coil, Accompanist, LeakCanary.

```
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

### Eclipse Public License - v 1.0 (EPL 1.0)
Used by: JUnit 4.13.2.
Full text available at: [https://www.eclipse.org/legal/epl-v10.html](https://www.eclipse.org/legal/epl-v10.html)
