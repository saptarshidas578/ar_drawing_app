---
name: compose-ui-rules
description: Use for any Jetpack Compose or screen layout change. Keeps the camera view dominant and the UI consistent and accessible.
---

- The camera/AR view is the hero. Keep the layout from `ar-tracer-project-context`: slim top bar, bottom dock, small sheets. Nothing new may take permanent screen space.
- New controls go into an existing sheet or a new sheet in the dock, not onto the main screen.
- State lives in ViewModels with immutable UI state. Composables are small and reusable.
- Never recreate or restart the AR view because of UI recomposition. Keep the AR composable stable and keep its inputs minimal.
- Touch targets at least 48 dp. Icons have labels or content descriptions. Support dark and light themes, portrait and landscape, and small and large screens.
- No hardcoded user-facing text: use `strings.xml`.
- Sheets use semi-transparent backgrounds, scroll internally, and close on outside tap, swipe down, or after about 6 seconds of no use.
- Respect left-handed mode for any new buttons.
- Friendly plain-language messages, shown as small temporary banners that do not cover the paper.
