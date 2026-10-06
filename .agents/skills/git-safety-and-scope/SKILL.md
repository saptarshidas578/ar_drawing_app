---
name: git-safety-and-scope
description: Use for every task that changes files. Protects working code with checkpoints, branches, and strict scope control.
---

- Run `git status`. If there is no repository, run `git init` and make a baseline commit.
- Before starting, commit a checkpoint: `checkpoint before <feature>`. Work on a branch named `feature/<short-name>`.
- Commit after each working slice with a clear message. Never rewrite history or force-push.
- Change only what the task requires. No unrelated refactors, renames, or formatting sweeps.
- Never delete an existing feature, file, or setting without saying so and getting permission.
- Do not upgrade Gradle, AGP, Kotlin, ARCore, SceneView, OpenCV, or other dependencies unless the task needs it. If you must, say why and which version.
- Never commit secrets: keystores, passwords, API keys. Make sure `.gitignore` covers them.
- At the end, tell the user how to undo the whole task (the exact git command or branch to go back to).
