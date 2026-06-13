# Gemini Agent Kit — Quickstart

## What was generated
- `GEMINI.md`: project context (commands, conventions)
- `.gemini/agents/*.md`: reusable agent instructions
- This file: how to use them

## Recommended usage (Gemini CLI)
From repo root:

### 1) Orchestrated workflow (tests → run → fix → review → PR text)
Paste this into Gemini CLI:

```
Read GEMINI.md first.
Use .gemini/agents/orchestrator.md as your system instruction.
Goal: Help me with the current change in my git working tree.
- Start by reading git diff (if available) and relevant files.
- Then follow the orchestrator process.
```

### 2) Only write tests
```
Read GEMINI.md.
Follow .gemini/agents/test_writer.md.
Look at my current git diff and write/update tests accordingly. Output unified diffs.
```

### 3) Run tests + summarize
```
Read GEMINI.md.
Follow .gemini/agents/test_runner.md.
Run the Test command from GEMINI.md and summarize failures.
```

### 4) Fix failures
```
Read GEMINI.md.
Follow .gemini/agents/fixer.md.
Based on the failing output, propose minimal patches to make tests pass.
```

### 5) PR text
```
Read GEMINI.md.
Follow .gemini/agents/pr_writer.md.
Draft a PR title and body based on the current changes and test results.
```

## Notes
- If your test/lint commands are wrong, edit GEMINI.md and rerun prompts.
- To overwrite generated files next time: run bootstrap with `OVERWRITE=1`.
