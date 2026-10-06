---
name: debugging-protocol
description: Use when something does not work, crashes, drifts, looks wrong, or fails to build.
---

1. **Reproduce or inspect** before changing anything. Read the full error text and the relevant code.
2. List 2 to 4 plausible root causes and what evidence would confirm each.
3. Add temporary targeted logging with one consistent log tag, or use the debug panel, to confirm the cause.
4. Fix the root cause with the smallest change, not the symptom. Never silence errors or warnings to make the build pass.
5. Verify the fix, remove temporary logging, and make sure nearby features still work.
6. Explain in plain English what was wrong and why the fix works.

For build errors: fix the first error first, because later ones often follow from it. Never fix errors by deleting features.
