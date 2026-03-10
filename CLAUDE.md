# telegram — Claude Code Project Context

This file is the **single source of truth** for how Claude Code should work in this repo.

## Project type
Detected: **Java** (Build: Maven)

## How to build / test / lint / format
- Test: `mvn -q test`
- Lint/Check: `mvn -q -DskipTests=true verify`
- Format: `N/A`

> If these are wrong, edit them here. All Claude Code agents must follow this file.

## Repo rules
- Keep diffs small and focused.
- Prefer adding tests for bug fixes & new behavior.
- Don't change public APIs unless requested.
- Don't introduce new dependencies without asking.
- Always read files before modifying them.
- Use Read, Edit, Write tools instead of bash cat/sed/awk.

## Testing conventions
- Add tests close to existing testing patterns in the repo.
- Keep tests deterministic (no sleeps, no network).
- Prefer black-box tests via public API.
- All tests must pass before marking work complete.

## PR conventions
- PR must include: summary, motivation, what changed, test plan, risks, follow-ups.
- Include screenshots/logs if relevant.
- Never force push to main/master.
- Create commits with meaningful messages.

## Commands (copy/paste)
- Run tests: `mvn -q test`
- Run checks: `mvn -q -DskipTests=true verify`
