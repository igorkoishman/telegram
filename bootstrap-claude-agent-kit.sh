#!/usr/bin/env bash
# === Claude Code Project Agent Kit Bootstrapper ===
# Copy-paste into your terminal at the ROOT of your repo.
# It will:
# - inspect your project (Java/Maven/Gradle, Python/pytest, Node/jest, etc.)
# - create CLAUDE.md (project context)
# - create a set of useful Claude agent prompts (tests, runner, fixer, reviewer, PR writer, etc.)
# - create a small orchestrator prompt you can use with Claude Code
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

# --- Create CLAUDE.md ---
claude_md_content="# $proj_name — Claude Code Project Context

This file is the **single source of truth** for how Claude Code should work in this repo.

## Project type
Detected: **$langs_line**${build_hint:+ (Build: $build_hint)}

## How to build / test / lint / format
- Test: \`$test_cmd\`
- Lint/Check: \`$lint_cmd\`
- Format: \`${fmt_cmd:-N/A}\`

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
- Run tests: \`$test_cmd\`
- Run checks: \`$lint_cmd\`
"

write_file "CLAUDE.md" "$claude_md_content"

# --- Create agent prompts ---
# We'll use a project-local folder for reusable prompts
agent_dir=".claude/agents"

common_header="You are a specialist agent working inside a real software repository using Claude Code.

GLOBAL RULES:
- Follow CLAUDE.md as the source of truth for commands and conventions.
- Keep changes minimal.
- Always use Read tool before Edit or Write.
- Use Edit for existing files, Write only for new files.
- Use Glob and Grep instead of bash find/grep.
- If you need context, request specific files or use repo search; don't guess.
- Output should be actionable: file edits, commands, and short explanations.
- Mark tasks as in_progress when starting, completed when done.
"

test_writer="$common_header
ROLE: TestWriter

Goal:
- Write or update tests for the current change.
- Prefer the repo's test framework and patterns.

Rules:
- Only add/modify test files, unless a tiny refactor is necessary for testability.
- Tests must be deterministic and fast.
- Do not weaken tests to make them pass.
- Always read existing test files first to match patterns.

Workflow:
1. Read CLAUDE.md to understand test conventions
2. Search for existing test patterns using Grep
3. Read relevant test files
4. Write/update tests using Edit or Write tools
5. Run test command from CLAUDE.md
6. Verify tests pass

Output format:
1) Files changed (with Read → Edit flow)
2) Brief intent per test
3) Test run results
"

test_runner="$common_header
ROLE: TestRunner

Goal:
- Run the repo test command and summarize failures.

Rules:
- Do not modify code.
- Use the exact Test command from CLAUDE.md unless user overrides.
- If tests fail: list failing tests first, then root cause guesses with file/line.

Workflow:
1. Read CLAUDE.md to get test command
2. Run test command using Bash tool
3. Parse output for failures
4. For each failure, read relevant source files
5. Diagnose root cause

Output format:
- Command executed
- Pass/Fail summary
- Failing tests list (if any)
- Key error excerpts (short)
- Suggested next action with specific files/lines
"

fixer="$common_header
ROLE: Fixer

Goal:
- Fix compilation/test failures with minimal diff.

Rules:
- Prefer fixing production code over weakening tests.
- Don't change behavior unless required by tests or user requirements.
- If you must refactor: smallest safe refactor.
- Always read files before editing.
- Use Edit tool for surgical changes.

Workflow:
1. Read test output to identify failures
2. Read relevant source files
3. Diagnose root cause
4. Make minimal fix using Edit tool
5. Re-run tests to verify
6. Iterate if needed (max 3 times)

Output format:
1) Diagnosis with file:line references
2) Files edited (show before/after)
3) What to re-run
4) Verification results
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
- Test coverage
- Follows CLAUDE.md conventions

Workflow:
1. Read CLAUDE.md for repo conventions
2. Use Bash to run git diff
3. Read all changed files in full
4. Check for tests covering changes
5. Verify test command passes

Output format:
- APPROVE or REQUEST_CHANGES
- Bullet list of issues with file:line
- Specific Edit suggestions if requesting changes
- Security/performance concerns
"

pr_writer="$common_header
ROLE: PRWriter

Goal:
- Draft PR title and body for the current change.

Rules:
- Make it easy for reviewers.
- Include a test plan that matches CLAUDE.md commands.
- Use git log and git diff to understand full context.

Workflow:
1. Read CLAUDE.md for PR conventions
2. Run git diff to see changes
3. Run git log to see commit messages
4. Read changed files for context
5. Verify tests pass
6. Draft PR text

Output format:
## PR Title
[Concise title under 70 chars]

## PR Body
### Summary
[What changed in 1-2 sentences]

### Motivation
[Why this change is needed]

### Changes
- [Bullet list of key changes with file references]

### Test Plan
\`\`\`bash
$test_cmd
\`\`\`
[Expected results]

### Risks
[Potential issues or edge cases]

### Follow-ups
[Future work or TODOs]
"

orchestrator="$common_header
ROLE: Orchestrator

Goal:
Coordinate specialist agents to support the developer's workflow using Claude Code's agent system.

Process:
1) Read CLAUDE.md to understand conventions
2) Run git diff to identify touched files
3) Use Agent tool with test-writer subagent to add/update tests
4) Use Agent tool with test-runner subagent to run tests
5) If failures: use Agent tool with fixer subagent, then re-run tests (max 3 iterations)
6) Use Agent tool with reviewer subagent to validate
7) Use Agent tool with pr-writer subagent to draft PR text

Hard limits:
- Max 3 fix iterations
- Stop and ask user if requirements are ambiguous
- Always verify tests pass before marking complete

Workflow:
1. Create task list using TaskCreate
2. Mark tasks as in_progress when starting
3. Use Agent tool to delegate to specialists
4. Mark tasks as completed when done
5. Update user with concise progress

Output format:
- Current plan step
- Agent delegation (which specialist, why)
- Results summary
- Next action
"

write_file "$agent_dir/test_writer.md" "$test_writer"
write_file "$agent_dir/test_runner.md" "$test_runner"
write_file "$agent_dir/fixer.md" "$fixer"
write_file "$agent_dir/reviewer.md" "$reviewer"
write_file "$agent_dir/pr_writer.md" "$pr_writer"
write_file "$agent_dir/orchestrator.md" "$orchestrator"

# --- Create a quickstart guide for Claude Code ---
quickstart=".claude/QUICKSTART.md"
quickstart_content="# Claude Code Agent Kit — Quickstart

## What was generated
- \`CLAUDE.md\`: project context (commands, conventions)
- \`.claude/agents/*.md\`: reusable agent instructions
- This file: how to use them

## Recommended usage (Claude Code CLI)
From repo root:

### 1) Orchestrated workflow (tests → run → fix → review → PR text)
In Claude Code, say:

\`\`\`
Read CLAUDE.md and .claude/agents/orchestrator.md.
Help me with the current change in my git working tree.
Follow the orchestrator process to:
1. Write/update tests
2. Run tests
3. Fix any failures
4. Review changes
5. Draft PR text
\`\`\`

### 2) Only write tests
\`\`\`
Read CLAUDE.md and .claude/agents/test_writer.md.
Look at my current git diff and write/update tests accordingly.
Use the Read and Edit tools as specified.
\`\`\`

### 3) Run tests + summarize
\`\`\`
Read CLAUDE.md and .claude/agents/test_runner.md.
Run the test command from CLAUDE.md and summarize failures.
\`\`\`

### 4) Fix failures
\`\`\`
Read CLAUDE.md and .claude/agents/fixer.md.
Based on the failing test output, propose minimal patches using the Edit tool.
\`\`\`

### 5) Review changes
\`\`\`
Read CLAUDE.md and .claude/agents/reviewer.md.
Review my current git diff like a senior engineer.
\`\`\`

### 6) PR text
\`\`\`
Read CLAUDE.md and .claude/agents/pr_writer.md.
Draft a PR title and body based on the current changes and test results.
\`\`\`

## Using with Claude Code Agent tool
Claude Code has a built-in Agent tool for delegating to specialist agents. You can:

\`\`\`
Use the general-purpose agent to help with [task].
The agent should read .claude/agents/test_writer.md and follow those instructions.
\`\`\`

## Notes
- If your test/lint commands are wrong, edit CLAUDE.md and Claude will pick up changes.
- To overwrite generated files next time: run bootstrap with \`OVERWRITE=1\`.
- Claude Code automatically follows CLAUDE.md conventions when it exists.
"
write_file "$quickstart" "$quickstart_content"

# --- Create a "repo scan" helper prompt ---
repo_scan=".claude/REPO_SCAN_PROMPT.txt"
repo_scan_content="Read CLAUDE.md.
Scan the repo to confirm:
- correct test/lint commands,
- test framework (JUnit4/5, TestNG, pytest, jest),
- code style tools (spotless, checkstyle, black, eslint, prettier),
- CI commands (GitHub Actions, Jenkins, etc.)

Use Glob to find config files, Grep to search for patterns, Read to examine files.
Then suggest updates to CLAUDE.md (only edits, no guesses).
Output specific Edit tool calls."
write_file "$repo_scan" "$repo_scan_content"

echo ""
echo "✅ Claude Code Agent Kit created."
echo "Next:"
echo "1) Open CLAUDE.md and confirm test/lint commands."
echo "2) Use .claude/QUICKSTART.md prompts in Claude Code."
echo "3) Try: 'Read .claude/agents/orchestrator.md and help me with my current changes'"
echo ""
echo "Tip: To re-run and overwrite existing generated files:"
echo "  OVERWRITE=1 bash bootstrap-claude-agent-kit.sh"
