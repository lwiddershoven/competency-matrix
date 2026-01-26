# Claude Code Instructions

## Before Starting ANY Work

1. **Read Constitution**: `.specify/memory/constitution.md` (SUPREME AUTHORITY for all decisions)
2. **Verify Branch/Spec Alignment**: Branch `NNN-feature-name` MUST match `specs/NNN-feature-name/`
3. **Read Feature Context** (in order):
   - `specs/NNN-feature-name/spec.md` - requirements
   - `specs/NNN-feature-name/plan.md` - approach (if exists)
   - `specs/NNN-feature-name/tasks.md` - work items (if exists)

## Spec-Kit Workflow Detection

This project uses [spec-kit (specify)](https://github.com/specify-kit/specify) for structured feature development.

**WARN user to use spec-kit when they request:**
- "Add/build/implement a new feature..."
- "Let's create..."
- "I want to add..."
- Work affecting multiple components or user-facing behavior

**Warning template:**
```
⚠️ This sounds like a new feature requiring spec-kit workflow:

Use: /specify → /plan → /tasks → /implement

This ensures specification-driven development per Constitution.
Proceed with structured approach, or is this adhoc work?
```

**Adhoc work is OK for:**
- Bug fixes, documentation updates, refactoring within current feature
- Investigation, questions, test runs, git operations

## Branch Strategy

Convention: `NNN-feature-name` (e.g., `002-platform-modernization`)
- Must match spec directory
- See [spec-kit documentation](https://github.com/specify-kit/specify) for workflow details

**Validate before work:**
```bash
git branch --show-current                      # Check current
ls specs/$(git branch --show-current)/spec.md  # Verify match
# If mismatch → WARN USER
```

## Context7 MCP

Always use Context7 MCP for library/API documentation without asking user first.

## Current Technology Stack

- Java 25, Quarkus 3.30.6 (JAX-RS, Qute, Panache JDBC, Flyway, SmallRye Health, Micrometer)
- PostgreSQL 18.1, htmx, Pico CSS
- Testing: JUnit 5, Quarkus Test, REST Assured, Testcontainers, Playwright

## Quick Reference

| User Says | Response |
|-----------|----------|
| "Add feature X" | ⚠️ Warn: use `/specify` |
| "Fix bug Y" | ✅ TDD: test first |
| "How does Z work?" | ✅ Read/explain |
| "Refactor..." | ✅ Tests first |

---
**Last Updated**: 2026-01-22 | **Constitution**: 1.0.0

## Active Technologies
- Java 25 + Quarkus 3.30.6 (JAX-RS, Qute, Panache JDBC), SnakeYAML, SLF4J (003-competencies-sync)
- PostgreSQL 18.1 (via Panache JDBC and Flyway migrations) (003-competencies-sync)
- Java 25 + Quarkus 3.30.6 (REST, Qute templating), htmx 2.0.4, Pico CSS 2 (004-matrix-overview)
- PostgreSQL 18.1 (via Panache JDBC) (004-matrix-overview)

## Recent Changes
- 003-competencies-sync: Added Java 25 + Quarkus 3.30.6 (JAX-RS, Qute, Panache JDBC), SnakeYAML, SLF4J
