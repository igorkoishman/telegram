#!/usr/bin/env bash
# === Gemini Project Agent Kit Bootstrapper ===
# Copy-paste into your terminal at the ROOT of your repo.
# It will:
# - inspect your project (Java/Maven/Gradle, Python/pytest, Node/jest, etc.)
# - create GEMINI.md (project context)
# - create a set of useful Gemini agents (tests, runner, fixer, reviewer, PR writer, etc.)
# - create a small "orchestrator prompt" you can paste into Gemini CLI any time
#
# Safe by default: it will NOT overwrite existing files unless you set OVERWRITE=1.

set -euo pipefail

OVERWRITE="${OVERWRITE:-0}"
ROOT="$(pwd)"

write_file() {
  local path="$1"
  local content="$2"
  local dir
  dir="$(dirname "$path")"
  mkdir -p "$dir"

  if [ -f "$path" ] && [ "$OVERWRITE" != "1" ]; then
    echo "SKIP (exists): $path  (set OVERWRITE=1 to overwrite)"
    return 0
  fi

  printf "%s" "$content" > "$path"
  echo "WROTE: $path"
}

exists_any() {
  for f in "$@"; do
    [ -e "$f" ] && return 0
  done
  return 1
}

# --- Detect project type(s) and commands ---
proj_langs=()
build_hint=""
test_cmd=""
lint_cmd=""
fmt_cmd=""
type_hints=()

# Java
has_maven=0
has_gradle=0
if exists_any "pom.xml"; then has_maven=1; fi
if exists_any "build.gradle" "build.gradle.kts" "settings.gradle" "settings.gradle.kts"; then has_gradle=1; fi

if [ "$has_maven" = "1" ] || [ "$has_gradle" = "1" ]; then
  proj_langs+=("java")
  type_hints+=("Java")
  if [ "$has_gradle" = "1" ]; then
    build_hint="Gradle"
    if [ -x "./gradlew" ]; then
      test_cmd="./gradlew test"
      lint_cmd="./gradlew check"
      fmt_cmd=""
    else
      test_cmd="gradle test"
      lint_cmd="gradle check"
      fmt_cmd=""
    fi
  else
    build_hint="Maven"
    test_cmd="mvn -q test"
    lint_cmd="mvn -q -DskipTests=true verify"
    fmt_cmd=""
  fi
fi

# Python
if exists_any "pyproject.toml" "requirements.txt" "setup.py" "Pipfile"; then
  proj_langs+=("python")
  type_hints+=("Python")
  if command -v pytest >/dev/null 2>&1 || exists_any "pytest.ini" "conftest.py"; then
    [ -z "$test_cmd" ] && test_cmd="pytest -q"
  else
    [ -z "$test_cmd" ] && test_cmd="python -m unittest"
  fi
  [ -z "$lint_cmd" ] && lint_cmd="python -m compileall ."
  [ -z "$fmt_cmd" ] && fmt_cmd=""
fi

# Node
if exists_any "package.json"; then
  proj_langs+=("node")
  type_hints+=("Node")
  if [ -z "$test_cmd" ]; then
    test_cmd="npm test"
  fi
  if [ -z "$lint_cmd" ]; then
    lint_cmd="npm run lint"
  fi
  if [ -z "$fmt_cmd" ]; then
    fmt_cmd="npm run format"
  fi
fi

# If nothing detected, still create generic kit
if [ "${#proj_langs[@]}" -eq 0 ]; then
  type_hints=("Generic")
  [ -z "$test_cmd" ] && test_cmd="(set your test command here)"
  [ -z "$lint_cmd" ] && lint_cmd="(set your lint command here)"
  [ -z "$fmt_cmd" ] && fmt_cmd="(set your format command here)"
fi

proj_name="$(basename "$ROOT")"
langs_line="$(IFS=", "; echo "${type_hints[*]}")"

# --- Create GEMINI.md ---
gemini_md_content="# $proj_name — Gemini Project Context

This file is the **single source of truth** for how Gemini should work in this repo.

## Project type
Detected: **$langs_line**${build_hint:+ (Build: $build_hint)}

## How to build / test / lint / format
- Test: \`$test_cmd\`
- Lint/Check: \`$lint_cmd\`
- Format: \`${fmt_cmd:-N/A}\`

> If these are wrong, edit them here. Agents must follow this file.

## Repo rules
- Keep diffs small and focused.
- Prefer adding tests for bug fixes & new behavior.
- Don't change public APIs unless requested.
- Don't introduce new dependencies without asking.

## Testing conventions
- Add tests close to existing testing patterns in the repo.
- Keep tests deterministic (no sleeps, no network).
- Prefer black-box tests via public API.

## PR conventions
- PR must include: summary, motivation, what changed, test plan, risks, follow-ups.
- Include screenshots/logs if relevant.

## Commands (copy/paste)
- Run tests: \`$test_cmd\`
- Run checks: \`$lint_cmd\`
"

write_file "GEMINI.md" "$gemini_md_content"

# --- Create agent prompts ---
# We'll use a project-local folder. Gemini CLI conventions vary across versions,
# but this folder works well as a reusable prompt library.
agent_dir=".gemini/agents"

common_header="You are a specialist agent working inside a real software repository.

GLOBAL RULES:
- Follow GEMINI.md as the source of truth for commands and conventions.
- Keep changes minimal.
- If you need context, request specific files or use repo search; don't guess.
- Output should be actionable: patches, commands, and short explanations.
"

junit_writer="$common_header
ROLE: TestWriter

Goal:
- Write or update tests for the current change.
- Prefer the repo's test framework and patterns.

Rules:
- Only add/modify test files, unless a tiny refactor is necessary for testability.
- Tests must be deterministic and fast.
- Do not weaken tests to make them pass.

Output format:
1) Files to change
2) Unified diff patches
3) Brief intent per test
"

test_runner="$common_header
ROLE: TestRunner

Goal:
- Run the repo test command and summarize failures.

Rules:
- Do not modify code.
- Use the exact Test command from GEMINI.md unless user overrides.
- If tests fail: list failing tests first, then root cause guesses with file/line.

Output format:
- Command executed
- Failing tests list
- Key error excerpts (short)
- Suggested next action
"

fixer="$common_header
ROLE: Fixer

Goal:
- Fix compilation/test failures with minimal diff.

Rules:
- Prefer fixing production code over weakening tests.
- Don't change behavior unless required by tests or user requirements.
- If you must refactor: smallest safe refactor.

Output format:
1) Diagnosis
2) Patch (unified diff)
3) What to re-run
"

reviewer="$common_header
ROLE: Reviewer

Goal:
- Review changes like a senior engineer.

Checklist:
- Correctness, edge cases, error handling
- Security basics (injection, traversal, secrets)
- Performance regressions
- Readability & maintainability
- Tests coverage

Output format:
- APPROVE or REQUEST_CHANGES
- Bullet list of issues with file/line
- Optional patch suggestions
"

pr_writer="$common_header
ROLE: PRWriter

Goal:
- Draft PR title and body for the current change.

Rules:
- Make it easy for reviewers.
- Include a test plan that matches GEMINI.md commands.

Output format:
- PR Title
- PR Body (Markdown) with:
  - Summary
  - Motivation/Context
  - Changes
  - Test Plan
  - Risks
  - Follow-ups
"

orchestrator="$common_header
ROLE: Orchestrator

Goal:
Coordinate specialist agents to support the developer's workflow.

Process:
1) Understand the goal and constraints.
2) Identify touched files (based on git diff if available).
3) Call TestWriter to add/update tests.
4) Call TestRunner to run tests.
5) If failures: call Fixer, then TestRunner again (max 3 iterations).
6) Call Reviewer to validate.
7) Call PRWriter to draft PR text.

Hard limits:
- Max 12 total steps
- Max 3 fix iterations
- Stop and ask for developer input if requirements are ambiguous.

Output format:
- Current plan step
- Delegation (which agent, why)
- Results summary
- Next action
"

write_file "$agent_dir/test_writer.md" "$junit_writer"
write_file "$agent_dir/test_runner.md" "$test_runner"
write_file "$agent_dir/fixer.md" "$fixer"
write_file "$agent_dir/reviewer.md" "$reviewer"
write_file "$agent_dir/pr_writer.md" "$pr_writer"
write_file "$agent_dir/orchestrator.md" "$orchestrator"

# --- Create a quickstart prompt you can paste into Gemini CLI ---
quickstart=".gemini/QUICKSTART.md"
quickstart_content="# Gemini Agent Kit — Quickstart

## What was generated
- \`GEMINI.md\`: project context (commands, conventions)
- \`.gemini/agents/*.md\`: reusable agent instructions
- This file: how to use them

## Recommended usage (Gemini CLI)
From repo root:

### 1) Orchestrated workflow (tests → run → fix → review → PR text)
Paste this into Gemini CLI:

\`\`\`
Read GEMINI.md first.
Use .gemini/agents/orchestrator.md as your system instruction.
Goal: Help me with the current change in my git working tree.
- Start by reading git diff (if available) and relevant files.
- Then follow the orchestrator process.
\`\`\`

### 2) Only write tests
\`\`\`
Read GEMINI.md.
Follow .gemini/agents/test_writer.md.
Look at my current git diff and write/update tests accordingly. Output unified diffs.
\`\`\`

### 3) Run tests + summarize
\`\`\`
Read GEMINI.md.
Follow .gemini/agents/test_runner.md.
Run the Test command from GEMINI.md and summarize failures.
\`\`\`

### 4) Fix failures
\`\`\`
Read GEMINI.md.
Follow .gemini/agents/fixer.md.
Based on the failing output, propose minimal patches to make tests pass.
\`\`\`

### 5) PR text
\`\`\`
Read GEMINI.md.
Follow .gemini/agents/pr_writer.md.
Draft a PR title and body based on the current changes and test results.
\`\`\`

## Notes
- If your test/lint commands are wrong, edit GEMINI.md and rerun prompts.
- To overwrite generated files next time: run bootstrap with \`OVERWRITE=1\`.
"
write_file "$quickstart" "$quickstart_content"

# --- Optional: create a "repo scan" helper prompt ---
repo_scan=".gemini/REPO_SCAN_PROMPT.txt"
repo_scan_content="Read GEMINI.md.
Scan the repo to confirm:
- correct test/lint commands,
- test framework (JUnit4/5, pytest, jest),
- code style tools (spotless, checkstyle, black, eslint),
- CI commands (GitHub Actions, Jenkins, etc.)
Then suggest updates to GEMINI.md (only edits, no guesses)."
write_file "$repo_scan" "$repo_scan_content"

echo ""
echo "✅ Gemini Agent Kit created."
echo "Next:"
echo "1) Open GEMINI.md and confirm test/lint commands."
echo "2) Use .gemini/QUICKSTART.md prompts in Gemini CLI."
echo ""
echo "Tip: To re-run and overwrite existing generated files:"
echo "  OVERWRITE=1 bash -c '<paste the script again>'"
