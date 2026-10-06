---
name: review-and-report
description: Use at the end of every task. Self-review checklist and the required final report format.
---

## Self-review (do this before reporting)
- Does it build with no errors? Any new warnings that matter?
- Are all existing features still reachable and working?
- Null cases, lifecycle (pause/resume, rotation), permission denial, low memory, tracking lost?
- Any per-frame allocation or main-thread work that should not be there?
- Strings in `strings.xml`, accessibility labels, dark and light themes?
- Do the unit tests pass? Were tests added for new pure logic?
- Is the code commented in plain English where it matters?

## Final report format
1. **Summary:** what was done, in plain English.
2. **Changed files:** the list, with one line each.
3. **How to undo:** the git branch or command.
4. **Known limits:** anything not verified or imperfect.
5. **Manual test checklist:** a numbered list the user can follow on a real phone, with the expected result for each step.
6. **CHANGELOG.md** updated.
