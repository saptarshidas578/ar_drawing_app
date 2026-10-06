---
name: persistence-rules
description: Use when saving or loading projects, settings, images, or exports.
---

- Store only paper-normalized coordinates and settings, never world coordinates or anchors.
- Copy the reference image into the app's own storage when a project is created so it survives gallery deletion.
- Use a versioned schema. When the format changes, add a migration and keep old projects loadable.
- Settings go in DataStore (or the existing mechanism). Projects use Room or JSON, whichever the project already uses.
- Autosave on leaving the screen and when the app goes to the background. Writes happen off the main thread.
- Handle missing files, corrupted data, and low storage with friendly messages. Never crash on bad data.
- Exports (photos and videos) use MediaStore or the share sheet with FileProvider. Do not request legacy storage permissions on modern Android.
