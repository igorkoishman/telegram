# Claude Code Agent Kit — Quickstart

## What was generated
- `CLAUDE.md`: project context (commands, conventions)
- `.claude/agents/*.md`: reusable agent instructions
- This file: how to use them

## Recommended usage (Claude Code CLI)
From repo root:

### 1) Orchestrated workflow (tests → run → fix → review → PR text)
In Claude Code, say:

```
Read CLAUDE.md and .claude/agents/orchestrator.md.
Help me with the current change in my git working tree.
Follow the orchestrator process to:
1. Write/update tests
2. Run tests
3. Fix any failures
4. Review changes
5. Draft PR text
```

### 2) Only write tests
```
Read CLAUDE.md and .claude/agents/test_writer.md.
Look at my current git diff and write/update tests accordingly.
Use the Read and Edit tools as specified.
```

### 3) Run tests + summarize
```
Read CLAUDE.md and .claude/agents/test_runner.md.
Run the test command from CLAUDE.md and summarize failures.
```

### 4) Fix failures
```
Read CLAUDE.md and .claude/agents/fixer.md.
Based on the failing test output, propose minimal patches using the Edit tool.
```

### 5) Review changes
```
Read CLAUDE.md and .claude/agents/reviewer.md.
Review my current git diff like a senior engineer.
```

### 6) PR text
```
Read CLAUDE.md and .claude/agents/pr_writer.md.
Draft a PR title and body based on the current changes and test results.
```

## Using with Claude Code Agent tool
Claude Code has a built-in Agent tool for delegating to specialist agents. You can:

```
Use the general-purpose agent to help with [task].
The agent should read .claude/agents/test_writer.md and follow those instructions.
```

## Notes
- If your test/lint commands are wrong, edit CLAUDE.md and Claude will pick up changes.
- To overwrite generated files next time: run bootstrap with `OVERWRITE=1`.
- Claude Code automatically follows CLAUDE.md conventions when it exists.
