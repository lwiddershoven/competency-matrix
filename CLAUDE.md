# Claude Code Instructions for Competency Matrix Project

## 🚨 CRITICAL: Always Read First

**Before ANY work**, you MUST:

1. **Read the Constitution**: `.specify/memory/constitution.md`
   - This is the SUPREME AUTHORITY for all development decisions
   - All code, tests, and architecture MUST comply with these principles
   - When in doubt, Constitution wins over convenience

2. **Check Current Context**: Identify which feature is being worked on
   - Current branch determines active feature (format: `NNN-feature-name`)
   - Feature specs live in `specs/NNN-feature-name/`
   - Read these files in order:
     - `spec.md` - Feature specification and requirements
     - `plan.md` - Implementation approach (if exists)
     - `tasks.md` - Actionable task list (if exists)

3. **Verify Branch Alignment**:
   - Branch name MUST match spec directory (e.g., `002-platform-modernization` → `specs/002-platform-modernization/`)
   - If branch doesn't match spec, WARN THE USER immediately
   - Never work on features without proper branch/spec alignment

## 📋 Spec-Kit Context Awareness

This project uses **spec-kit (specify)** for structured feature development. You MUST recognize when the user is requesting work that should go through the proper spec-kit workflow.

### When to WARN the User to Use Spec-Kit

⚠️ **WARN when the user requests**:

- "Add a new feature..." → Should use `/specify` skill
- "Let's build..." → Should use `/specify` skill
- "I want to implement..." → Should use `/specify` skill
- "Can you plan out..." → Should use `/plan` skill
- "Break this down into tasks..." → Should use `/tasks` skill
- Work that affects multiple components or user-facing behavior

**Warning Template**:
```
⚠️ This request sounds like a new feature that should be developed using the spec-kit workflow.

I recommend using:
- `/specify` to create a feature specification
- `/plan` to create an implementation plan
- `/tasks` to generate actionable tasks
- `/implement` to execute the tasks

This ensures:
✓ Specification-driven development (Constitution requirement)
✓ Implementation plan before coding
✓ Task tracking and accountability
✓ Test-first development (TDD)

Would you like to proceed with the structured approach, or is this an adhoc fix/investigation?
```

### When Adhoc Work is Acceptable

✅ **Don't warn for**:
- Bug fixes in existing code
- Documentation updates
- Refactoring within current feature
- Investigation/exploration tasks
- Answering questions about the codebase
- Running tests or builds
- Git operations within current feature

## 🏛️ Constitution Principles (ALWAYS FOLLOW)

From `.specify/memory/constitution.md`:

### I. Simplicity First (MANDATORY)
- Favor the simplest solution that meets requirements
- YAGNI: You Aren't Gonna Need It
- Prefer standard library over dependencies
- Every abstraction MUST justify itself
- If simpler alternative exists, MUST choose it

### II. Test-First Development (NON-NEGOTIABLE)
- Tests MUST be written BEFORE implementation
- Red-Green-Refactor cycle
- No code without tests
- Tests define the contract
- Coverage: happy paths, edge cases, errors

### III. Monolith First Architecture
- Single deployable unit
- One backend, one frontend, one database
- No microservices unless proven necessary
- Single data layer for database access

### IV. Data Integrity
- Validate before persistence
- Enforce referential integrity in schema
- No data loss - rollback on failure
- Meaningful messages for empty/missing data
- Versioned migrations with rollback

### V. User-Centric Design
- Every feature serves clear user need
- 3-click rule: any content in 3 clicks
- 2-second response time requirement
- Clear feedback for loading/errors/empty states
- Accessibility basics (semantic HTML, keyboard, contrast)

## 🔧 Development Workflow

### For New Features (Use Spec-Kit)

1. **Specification** (`/specify` skill):
   - Creates `specs/NNN-feature-name/spec.md`
   - Defines user stories, requirements, acceptance criteria
   - Branch: `NNN-feature-name`

2. **Planning** (`/plan` skill):
   - Creates `specs/NNN-feature-name/plan.md`
   - Implementation approach, architecture decisions
   - Complexity tracking
   - Constitution compliance check

3. **Task Generation** (`/tasks` skill):
   - Creates `specs/NNN-feature-name/tasks.md`
   - Actionable, ordered task list
   - Test tasks BEFORE implementation tasks (TDD)

4. **Implementation** (`/implement` skill):
   - Executes tasks from tasks.md
   - Tests first, then implementation
   - Updates task status as work progresses

### For Adhoc Work (Direct Coding)

- Bug fixes: Direct implementation with tests
- Documentation: Update as needed
- Refactoring: Within current feature scope
- Investigation: Read code, run tests, analyze

## 📂 Project Structure

```
competency-matrix/
├── .specify/
│   ├── memory/
│   │   └── constitution.md         ← ALWAYS READ FIRST
│   ├── templates/                   ← Spec-kit templates
│   └── scripts/                     ← Automation scripts
├── specs/
│   ├── 001-competency-matrix/       ← Original feature
│   │   ├── spec.md
│   │   ├── plan.md
│   │   └── tasks.md
│   └── NNN-feature-name/            ← Current feature
│       ├── spec.md                  ← Read for requirements
│       ├── plan.md                  ← Read for approach
│       └── tasks.md                 ← Read for tasks
├── src/
│   ├── main/java/.../
│   │   ├── resource/                ← JAX-RS REST endpoints
│   │   ├── model/                   ← Domain entities
│   │   ├── repository/              ← Data access (Panache JDBC)
│   │   ├── service/                 ← Business logic
│   │   └── config/                  ← Configuration
│   ├── main/resources/
│   │   ├── templates/               ← Qute templates
│   │   ├── db/migration/            ← Flyway migrations
│   │   └── application.properties
│   └── test/java/.../
│       ├── resource/                ← REST Assured tests
│       ├── repository/              ← Repository tests
│       ├── service/                 ← Service tests
│       ├── validation/              ← Migration validation tests
│       └── e2e/                     ← Playwright E2E tests
├── CLAUDE.md                        ← This file (your instructions)
├── README.md                        ← User-facing documentation
├── ROLLBACK.md                      ← Rollback procedures
└── MONITORING.md                    ← Monitoring plans
```

## 🛠️ Active Technology Stack

### Backend
- **Java 25**: Language version
- **Quarkus 3.30.6**: Framework
  - JAX-RS for REST endpoints
  - Qute for server-side templates
  - Panache JDBC for data access patterns
  - Flyway for database migrations
  - SmallRye Health for health checks
  - Micrometer for metrics

### Frontend
- **htmx**: Client-side interactivity (hypermedia-driven)
- **Pico CSS**: Classless semantic CSS framework
- **Qute Templates**: Server-side rendering

### Database
- **PostgreSQL 18.1**: Primary database
- **Flyway**: Migration tool (versioned migrations)

### Testing
- **JUnit 5**: Testing framework
- **Quarkus Test**: Integration testing
- **REST Assured**: HTTP/API testing
- **Testcontainers**: PostgreSQL DevServices (automatic container management)
- **Playwright**: E2E browser testing (Chromium)

### Build & Deploy
- **Maven**: Build tool (`./mvnw`)
- **Docker**: Containerization
- **Docker Compose**: Local development environment

## 🧪 Testing Requirements (TDD)

### Test-First Workflow (MANDATORY)

1. **Write failing test** (Red)
2. **Implement minimum code** to pass (Green)
3. **Refactor** while keeping tests green

### Test Coverage Requirements

- ✅ Happy path scenarios
- ✅ Edge cases
- ✅ Error conditions
- ✅ User-facing workflows (E2E)
- ✅ Database constraints
- ✅ Performance requirements (2s response time)

### Test Commands

```bash
# All tests
./mvnw test

# Specific test
./mvnw test -Dtest=ClassNameTest

# E2E tests
./mvnw test -Dtest='BrowseCompetenciesTest'

# With coverage
./mvnw verify
```

## 🌿 Branch Strategy

### Convention: `NNN-feature-name`

- `NNN`: Zero-padded feature number (001, 002, 003)
- `feature-name`: Kebab-case description
- **MUST match** spec directory: `specs/NNN-feature-name/`

### Examples

- Branch: `001-competency-matrix` → Spec: `specs/001-competency-matrix/spec.md`
- Branch: `002-platform-modernization` → Spec: `specs/002-platform-modernization/spec.md`

### Branch Validation (YOU MUST DO THIS)

```bash
# Check current branch
git branch --show-current

# Verify matching spec exists
ls specs/$(git branch --show-current)/spec.md

# If mismatch → WARN USER IMMEDIATELY
```

### Main Branch Protection

- `main` branch MUST always be deployable
- All features developed in feature branches
- Merge via Pull Request only
- PR requires: tests passing, Constitution compliance, code review

## 📚 Context7 MCP Usage

**Always use Context7 MCP** when you need:
- Library/API documentation
- Code generation examples
- Configuration reference
- Setup instructions

**DO NOT ask user first** - proactively use Context7 for technical documentation.

### Common Contexts

- Quarkus guides and configuration
- JAX-RS API reference
- Qute template syntax
- Flyway migration patterns
- REST Assured testing
- Playwright E2E testing

## ✅ Checklist Before Starting Work

```
[ ] Read .specify/memory/constitution.md
[ ] Verify current branch: git branch --show-current
[ ] Check branch matches spec directory: specs/NNN-feature-name/
[ ] Read specs/NNN-feature-name/spec.md (requirements)
[ ] Read specs/NNN-feature-name/plan.md (if exists - approach)
[ ] Read specs/NNN-feature-name/tasks.md (if exists - tasks)
[ ] If user request sounds like new feature → WARN to use spec-kit
[ ] If adhoc work → Proceed with TDD
```

## 🚫 What NOT to Do

- ❌ Write implementation before tests (violates TDD)
- ❌ Add features not in spec (violates Simplicity First)
- ❌ Skip Constitution check
- ❌ Work without matching branch/spec
- ❌ Commit directly to main
- ❌ Add unnecessary dependencies
- ❌ Build for hypothetical future needs (YAGNI)
- ❌ Create microservices/distributed systems
- ❌ Skip validation on data modifications
- ❌ Let user add features without spec-kit (WARN THEM)

## 🎯 Recent Context

### Current State (as of 2026-01-22)

- **Completed Feature**: 002-platform-modernization
  - Migrated Spring Boot → Quarkus 3.30.6
  - All 49 tests passing
  - PR #2 created and ready for review
  - Branch: `002-platform-modernization`

- **Active Branch**: 002-platform-modernization (merged to main pending)

- **Performance**: All pages 7-145ms (far exceeds 2s requirement)

- **Test Suite**:
  - Total: 49 tests, 0 failures
  - Repository: 6 tests
  - Service: 6 tests
  - Resource: 8 tests
  - E2E: 8 tests
  - Validation: 9 tests
  - Observability: 6 tests
  - Performance: 5 tests
  - Context: 1 test

## 💡 Best Practices

### When User Says...

| User Request | Your Response |
|--------------|---------------|
| "Add feature X" | ⚠️ Warn to use `/specify` → `/plan` → `/tasks` → `/implement` |
| "Fix bug in Y" | ✅ Proceed with TDD (test first, then fix) |
| "How does Z work?" | ✅ Read code, explain, provide examples |
| "Refactor this code" | ✅ Ensure tests exist first, then refactor |
| "Update documentation" | ✅ Update README, code comments as needed |
| "Build a new..." | ⚠️ Warn to use spec-kit workflow |

### Reading Order for New Features

1. `.specify/memory/constitution.md` (principles)
2. `specs/NNN-feature-name/spec.md` (what to build)
3. `specs/NNN-feature-name/plan.md` (how to build)
4. `specs/NNN-feature-name/tasks.md` (ordered work items)

### Constitution Compliance Checks

Before merging any code:
- ✅ Is this the simplest solution? (Principle I)
- ✅ Were tests written first? (Principle II)
- ✅ Does it fit in the monolith? (Principle III)
- ✅ Is data validated and safe? (Principle IV)
- ✅ Does it serve clear user need? (Principle V)

## 🔄 Version

- **Last Updated**: 2026-01-22
- **Constitution Version**: 1.0.0
- **Migration Status**: Quarkus 3.30.6 complete
