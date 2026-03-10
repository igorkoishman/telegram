# AI Agent Kit — Universal Guide for Claude & Gemini

This repo has **dual AI agent support** - you can use either Claude Code or Gemini depending on where you're working.

## Quick Reference

| Tool | Context File | Agent Dir | Quickstart |
|------|-------------|-----------|------------|
| **Claude Code** | `CLAUDE.md` | `.claude/agents/` | `.claude/QUICKSTART.md` |
| **Gemini CLI** | `GEMINI.md` | `.gemini/agents/` | `.gemini/QUICKSTART.md` |

Both tools detected: **Java (Maven)** project

## Common Commands (Both Tools)
- **Run tests**: `mvn -q test`
- **Lint/Check**: `mvn -q -DskipTests=true verify`
- **Format**: N/A

---

## Using Claude Code

### Option 1: Orchestrated Workflow (Recommended)
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

### Option 2: Individual Agents

**Write/Update Tests:**
```
Read CLAUDE.md and .claude/agents/test_writer.md.
Look at my current git diff and write/update tests accordingly.
```

**Run Tests:**
```
Read CLAUDE.md and .claude/agents/test_runner.md.
Run the test command and summarize failures.
```

**Fix Failures:**
```
Read CLAUDE.md and .claude/agents/fixer.md.
Fix the failing tests with minimal changes.
```

**Review Changes:**
```
Read CLAUDE.md and .claude/agents/reviewer.md.
Review my current git diff like a senior engineer.
```

**Draft PR:**
```
Read CLAUDE.md and .claude/agents/pr_writer.md.
Draft a PR title and body based on the current changes.
```

### Claude Code Advantages
- Direct file editing (Read, Edit, Write tools)
- Built-in Agent tool for delegating to specialist agents
- Task tracking system
- Automatic CLAUDE.md awareness

---

## Using Gemini CLI

### Option 1: Orchestrated Workflow (Recommended)
```
Read GEMINI.md first.
Use .gemini/agents/orchestrator.md as your system instruction.
Goal: Help me with the current change in my git working tree.
- Start by reading git diff (if available) and relevant files.
- Then follow the orchestrator process.
```

### Option 2: Individual Agents

**Write/Update Tests:**
```
Read GEMINI.md.
Follow .gemini/agents/test_writer.md.
Look at my current git diff and write/update tests accordingly.
Output unified diffs.
```

**Run Tests:**
```
Read GEMINI.md.
Follow .gemini/agents/test_runner.md.
Run the Test command from GEMINI.md and summarize failures.
```

**Fix Failures:**
```
Read GEMINI.md.
Follow .gemini/agents/fixer.md.
Based on the failing output, propose minimal patches to make tests pass.
```

**Review Changes:**
```
Read GEMINI.md.
Follow .gemini/agents/reviewer.md.
Review my changes like a senior engineer.
```

**Draft PR:**
```
Read GEMINI.md.
Follow .gemini/agents/pr_writer.md.
Draft a PR title and body based on the current changes.
```

### Gemini CLI Advantages
- Works in any environment
- Lightweight prompts
- Flexible integration

---

## Agent Roles Explained

Both Claude and Gemini have the same 6 specialist agents:

1. **Orchestrator** - Coordinates all other agents in a full workflow
2. **Test Writer** - Writes/updates tests following repo patterns
3. **Test Runner** - Runs tests and diagnoses failures
4. **Fixer** - Fixes compilation/test failures with minimal changes
5. **Reviewer** - Reviews changes like a senior engineer (security, performance, quality)
6. **PR Writer** - Drafts comprehensive PR descriptions

---

## Workflow Examples

### Typical Feature Development Flow

#### With Claude Code:
```
Read .claude/agents/orchestrator.md and CLAUDE.md.
I'm working on [feature description].
Please orchestrate the full workflow:
- Write tests
- Implement the feature
- Run tests and fix failures
- Review the changes
- Draft a PR
```

#### With Gemini CLI:
```
Read GEMINI.md and .gemini/agents/orchestrator.md.
I'm working on [feature description].
Follow the orchestrator process to write tests, implement, test, review, and draft a PR.
```

### Bug Fix Flow

#### With Claude Code:
```
Read .claude/agents/test_writer.md first.
Write a failing test that reproduces this bug: [bug description].
Then read .claude/agents/fixer.md and fix the bug with minimal changes.
```

#### With Gemini CLI:
```
Follow .gemini/agents/test_writer.md to write a failing test for: [bug description].
Then follow .gemini/agents/fixer.md to fix it minimally.
```

### Quick Test Run

#### With Claude Code:
```
Read .claude/agents/test_runner.md and run tests
```

#### With Gemini CLI:
```
Follow .gemini/agents/test_runner.md and run the tests
```

---

## Customization

### Update Test Commands
Edit **CLAUDE.md** or **GEMINI.md** to change:
- Test command
- Lint/check command
- Format command
- Project conventions
- PR requirements

### Update Agent Behavior
Edit individual agent files in:
- `.claude/agents/*.md` for Claude Code
- `.gemini/agents/*.md` for Gemini CLI

### Repo Scan
Both tools have a repo scan prompt to verify/update commands:

**Claude Code:**
```
Read .claude/REPO_SCAN_PROMPT.txt and execute it
```

**Gemini CLI:**
```
Read .gemini/REPO_SCAN_PROMPT.txt and execute it
```

---

## Regenerating Agent Kits

If you need to regenerate or update the agent kits:

```bash
# Regenerate Claude Code kit (will skip existing files)
./bootstrap-claude-agent-kit.sh

# Regenerate Gemini kit (will skip existing files)
./bootstrap-gemini-agent-kit.sh

# Force overwrite existing files
OVERWRITE=1 ./bootstrap-claude-agent-kit.sh
OVERWRITE=1 ./bootstrap-gemini-agent-kit.sh
```

---

## Best Practices

### For Both Tools:
1. **Always read context first** - Start with CLAUDE.md or GEMINI.md
2. **Use orchestrator for complex work** - Let it coordinate multiple agents
3. **Use individual agents for focused tasks** - Faster for specific needs
4. **Keep changes minimal** - Agents are instructed to make small, focused diffs
5. **Run tests before PR** - Both tools will do this if you use the orchestrator

### Claude Code Specific:
- Let Claude use Read/Edit/Write tools instead of suggesting manual edits
- Use the Agent tool delegation for parallel work
- Check task list with TaskList when using orchestrator

### Gemini CLI Specific:
- Copy/paste the full prompt templates from QUICKSTART.md
- Be explicit about which agent prompt to follow
- Request unified diffs for easy application

---

## Troubleshooting

### Tests failing?
**Claude:** `Read .claude/agents/fixer.md and fix the failures`
**Gemini:** `Follow .gemini/agents/fixer.md to fix failures`

### Wrong test command?
Edit CLAUDE.md or GEMINI.md and rerun commands

### Need to review before committing?
**Claude:** `Read .claude/agents/reviewer.md and review my changes`
**Gemini:** `Follow .gemini/agents/reviewer.md to review`

### Want a PR description?
**Claude:** `Read .claude/agents/pr_writer.md and draft a PR`
**Gemini:** `Follow .gemini/agents/pr_writer.md for PR text`

---

## Contributing

When adding new conventions or patterns:
1. Update CLAUDE.md and GEMINI.md
2. Consider updating relevant agent prompts
3. Test with both tools if possible
4. Keep prompts tool-agnostic where possible

---

## Summary Table

| Task | Claude Code Prompt | Gemini CLI Prompt |
|------|-------------------|-------------------|
| Full Workflow | `Read .claude/agents/orchestrator.md and help with current changes` | `Read GEMINI.md. Follow .gemini/agents/orchestrator.md` |
| Write Tests | `Read .claude/agents/test_writer.md and write tests` | `Read GEMINI.md. Follow .gemini/agents/test_writer.md` |
| Run Tests | `Read .claude/agents/test_runner.md and run tests` | `Read GEMINI.md. Follow .gemini/agents/test_runner.md` |
| Fix Failures | `Read .claude/agents/fixer.md and fix failures` | `Read GEMINI.md. Follow .gemini/agents/fixer.md` |
| Review Code | `Read .claude/agents/reviewer.md and review changes` | `Read GEMINI.md. Follow .gemini/agents/reviewer.md` |
| Draft PR | `Read .claude/agents/pr_writer.md and draft PR` | `Read GEMINI.md. Follow .gemini/agents/pr_writer.md` |

**Choose your tool, copy the prompt, and go!**
