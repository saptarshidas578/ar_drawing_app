---
name: plan-first-workflow
description: Use before making any change. Forces the agent to read, think, plan, and ask before coding, then implement in small verified slices.
---

1. **Read first.** Open `ar-tracer-project-context`, then every file that the task touches. Never guess what code does.
2. **Summarize current behavior** in plain English, naming the files and functions involved.
3. **Find the cause or design.** For a bug, identify the likely root cause with evidence from the code (see `debugging-protocol`). For a feature, describe the design and where it plugs in.
4. **Write a plan** before any code: files to change or create, ordered steps, risks, what could break, and how it will be tested. Keep it short and concrete.
5. **Ask only if truly blocked.** At most 3 questions, each with a recommended default. If not blocked, proceed on clearly stated assumptions.
6. **Implement in small slices.** After each slice, make sure the project still builds, then commit (see `git-safety-and-scope`).
7. **Self-review** the finished work with `review-and-report`.
8. **Be honest.** Never claim something works unless it was built or tested. The agent cannot run ARCore on a phone, so list exactly what could not be verified and must be checked on a real device.

Think about the simplest solution that satisfies the requirements. Do not add features that were not requested.
