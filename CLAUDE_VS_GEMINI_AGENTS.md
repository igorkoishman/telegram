# Claude Code vs Gemini CLI - Agent Kit Comparison

## Side-by-Side Feature Comparison

| Feature | Claude Code | Gemini CLI | Winner |
|---------|-------------|------------|--------|
| **File Editing** | Direct (Read/Edit/Write tools) | Unified diffs (manual apply) | 🏆 Claude |
| **Agent Delegation** | Built-in Agent tool | Manual prompt switching | 🏆 Claude |
| **Task Tracking** | TaskCreate/TaskUpdate system | Manual tracking | 🏆 Claude |
| **Setup Required** | Claude Code CLI | Any terminal | 🏆 Gemini |
| **Portability** | Claude-specific | Universal | 🏆 Gemini |
| **Prompt Verbosity** | Conversational | Template-based | 🏆 Claude |
| **Cost** | Claude API pricing | Gemini API pricing | Tie |
| **Context Awareness** | Auto-reads CLAUDE.md | Manual prompt | 🏆 Claude |
| **Multi-file Edits** | Native support | Multiple diffs | 🏆 Claude |
| **Iteration Speed** | Interactive | Copy/paste cycles | 🏆 Claude |

## Prompt Style Comparison

### Writing Tests

**Claude Code:**
```
Read .claude/agents/test_writer.md and write tests for my current changes
```

**Gemini CLI:**
```
Read GEMINI.md.
Follow .gemini/agents/test_writer.md.
Look at my current git diff and write/update tests accordingly.
Output unified diffs.
```

### Full Orchestrated Workflow

**Claude Code:**
```
Read .claude/agents/orchestrator.md and help me with my current changes
```

**Gemini CLI:**
```
Read GEMINI.md first.
Use .gemini/agents/orchestrator.md as your system instruction.
Goal: Help me with the current change in my git working tree.
- Start by reading git diff (if available) and relevant files.
- Then follow the orchestrator process.
```

## Workflow Comparison

### Bug Fix Flow

#### Claude Code (Interactive)
```
You: Fix the login bug and add tests

Claude:
1. [Reads CLAUDE.md and test_writer.md]
2. [Uses Grep to find login code]
3. [Uses Read to examine files]
4. [Uses Edit to fix bug]
5. [Uses Write to create test]
6. [Uses Bash to run tests]
7. "✅ Bug fixed and test added. All tests pass."
```

#### Gemini CLI (Template-based)
```
You: [Paste orchestrator prompt]
    Follow .gemini/agents/orchestrator.md
    Fix the login bug and add tests
    
Gemini:
1. Reading git diff...
2. Here's a unified diff to fix the bug:
   [shows diff]
3. Here's a test:
   [shows test diff]
4. Apply these and run: mvn -q test

You: [Apply diffs manually]
You: [Run tests]
```

## Use Case Recommendations

### Use Claude Code When:
✅ You have Claude Code CLI installed
✅ You want interactive, hands-off workflow
✅ You're making complex multi-file changes
✅ You want direct file editing
✅ You prefer conversational interaction
✅ You need task tracking
✅ You're in active development mode

### Use Gemini CLI When:
✅ You don't have Claude Code installed
✅ You want more control over each step
✅ You prefer reviewing diffs before applying
✅ You're in a restricted environment
✅ You want maximum portability
✅ You're working on a shared machine
✅ You want to learn the changes step-by-step

## Integration Patterns

### Pattern 1: Claude for Dev, Gemini for Review
```
Development → Claude Code orchestrator
Code Review → Gemini reviewer agent
PR Draft → Either tool
```

### Pattern 2: Local vs Remote
```
Local dev → Claude Code (full IDE integration)
Remote server → Gemini CLI (terminal only)
```

### Pattern 3: Hybrid Workflow
```
Quick fixes → Claude Code test_runner + fixer
Feature dev → Claude Code orchestrator
Final review → Gemini reviewer (second opinion)
```

## Performance Characteristics

### Claude Code
- **Startup**: ~2s (tool initialization)
- **Per-file edit**: ~3-5s (read + edit)
- **Test run**: Actual test time + 2s overhead
- **Full orchestration**: 3-7 minutes
- **Iteration**: Instant (same session)

### Gemini CLI
- **Startup**: ~1s (prompt parsing)
- **Per-file diff**: ~5-10s (analysis + diff generation)
- **Test run**: Actual test time + 5s parsing
- **Full orchestration**: 5-10 minutes (manual steps)
- **Iteration**: 10-30s (new prompt submission)

## Agent Capabilities Matrix

| Agent | Claude Code Features | Gemini CLI Features |
|-------|---------------------|---------------------|
| **Orchestrator** | Task tracking, parallel agents, auto-retry | Sequential prompts, manual coordination |
| **Test Writer** | Direct file creation, pattern matching | Diff generation, template suggestions |
| **Test Runner** | Live output, error parsing, file navigation | Output summary, error excerpts |
| **Fixer** | Surgical edits, multi-file fixes, verification | Unified diffs, single-fix suggestions |
| **Reviewer** | In-line comments, file navigation, auto-checks | Structured review, security checklist |
| **PR Writer** | Git integration, auto-context, template generation | Manual context, structured format |

## Context File Differences

### CLAUDE.md (Additional Guidelines)
```markdown
## Tool Usage
- Always read files before modifying them.
- Use Read, Edit, Write tools instead of bash cat/sed/awk.
- Use Glob and Grep instead of find/grep.
- Use TaskCreate/TaskUpdate for tracking.
```

### GEMINI.md (Simpler)
```markdown
## Guidelines
- Keep diffs small and focused.
- Output unified diffs.
```

## Cost Comparison (Estimated)

Based on typical orchestrator workflow:

**Claude Code:**
- Input tokens: ~15,000 (file reads, context)
- Output tokens: ~5,000 (edits, responses)
- Cost: ~$0.30 per workflow (Sonnet pricing)

**Gemini CLI:**
- Input tokens: ~10,000 (prompts, manual context)
- Output tokens: ~8,000 (diffs, explanations)
- Cost: ~$0.15 per workflow (Gemini 1.5 Pro pricing)

*Note: Actual costs vary by model choice and workflow complexity*

## Switching Between Tools

### From Claude to Gemini
```bash
# Extract context from Claude session
git diff > /tmp/current_changes.diff

# Use in Gemini
Read GEMINI.md.
Follow .gemini/agents/fixer.md.
Here are the current changes: [paste diff]
Fix the failing tests.
```

### From Gemini to Claude
```bash
# Apply Gemini diffs first
git apply gemini_suggestions.diff

# Continue with Claude
Read .claude/agents/reviewer.md and review the changes
```

## Best Practices by Tool

### Claude Code Best Practices
1. Let it read files (don't paste code)
2. Use agent delegation for complex tasks
3. Trust the Edit tool (review via git diff after)
4. Use task tracking for multi-step work
5. Leverage CLAUDE.md auto-awareness

### Gemini CLI Best Practices
1. Be explicit in prompts (it can't read GEMINI.md automatically)
2. Review all diffs before applying
3. Keep sessions focused (one agent at a time)
4. Save useful prompts in templates
5. Use copy-paste friendly formats

## Limitations

### Claude Code Limitations
- Requires installation
- CLI-only (for now)
- Can't run on remote servers easily
- Opinionated workflows

### Gemini CLI Limitations
- Manual diff application
- No direct file editing
- More verbose prompts required
- No task tracking
- Manual git integration

## Migration Guide

### Moving Existing Workflows to Agent Kit

**Before (Manual):**
```bash
# Write test manually
vim src/test/java/MyTest.java
# Run test
mvn test
# Fix manually
vim src/main/java/MyClass.java
# Review manually
git diff
# Write PR manually
vim PR_DESCRIPTION.md
```

**After (Claude Code):**
```bash
Read .claude/agents/orchestrator.md and help with my current changes
# Wait 3-5 minutes
# Everything done ✅
```

**After (Gemini CLI):**
```bash
# Step 1: Tests
Read GEMINI.md. Follow .gemini/agents/test_writer.md. [task]
# Apply diffs

# Step 2: Run
Follow .gemini/agents/test_runner.md
# Check output

# Step 3: Fix
Follow .gemini/agents/fixer.md
# Apply diffs

# Step 4: Review
Follow .gemini/agents/reviewer.md
# Read feedback

# Step 5: PR
Follow .gemini/agents/pr_writer.md
# Copy PR text
```

## Summary

**Claude Code = Autopilot Mode**
- Best for: Active development, complex tasks, speed
- Trade-off: Less control, requires installation

**Gemini CLI = Co-pilot Mode**
- Best for: Learning, control, portability
- Trade-off: More manual, slower iterations

**Both = Maximum Flexibility**
- Use the right tool for each situation
- Same underlying agent architecture
- Seamless switching via unified context files

**Recommendation:**
- Primary development: Claude Code
- Learning/teaching: Gemini CLI
- CI/CD: Gemini CLI (scripting)
- Code review: Either (try both!)
