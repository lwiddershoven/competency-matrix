<!--
  SYNC IMPACT REPORT
  ==================
  Version change: N/A (initial) → 1.0.0
  Modified principles: None (initial creation)
  Added sections:
    - Core Principles (5 principles)
    - Quality Standards
    - Development Workflow
    - Governance
  Removed sections: None (initial)
  Templates requiring updates:
    - .specify/templates/plan-template.md: ✅ Compatible (Constitution Check section exists)
    - .specify/templates/spec-template.md: ✅ Compatible (no changes needed)
    - .specify/templates/tasks-template.md: ✅ Compatible (TDD workflow supported)
  Follow-up TODOs: None
-->

# Competency Matrix Constitution

## Core Principles

### I. Simplicity First

All design decisions MUST favor the simplest solution that meets requirements.

- Start with the minimum viable implementation; add complexity only when proven necessary
- YAGNI (You Aren't Gonna Need It): Do not build features for hypothetical future needs
- Prefer standard library solutions over external dependencies
- Every abstraction MUST justify its existence with a concrete problem it solves
- If a simpler alternative exists that meets requirements, it MUST be chosen

**Rationale**: Complexity is the primary source of bugs, maintenance burden, and project delays.
A simple codebase is easier to understand, test, and extend.

### II. Test-First Development (NON-NEGOTIABLE)

Tests MUST be written before implementation code.

- Red-Green-Refactor cycle: Write failing test → Implement minimum code to pass → Refactor
- No implementation code may be committed without corresponding tests
- Tests define the contract; implementation satisfies the contract
- Test coverage MUST include happy paths, edge cases, and error conditions
- Integration tests MUST cover user-facing scenarios from the specification

**Rationale**: TDD ensures every feature is testable by design, prevents scope creep during
implementation, and provides living documentation of expected behavior.

### III. Monolith First Architecture

The application MUST be built as a single deployable unit.

- One backend service, one frontend application, one database
- No microservices, message queues, or distributed systems until proven necessary
- Shared code lives in modules/packages within the monolith, not separate services
- Database access MUST go through a single, well-defined data layer
- Deployment MUST be achievable with a single command or CI pipeline

**Rationale**: Distributed systems add network complexity, debugging difficulty, and operational
overhead. A monolith is simpler to develop, test, and deploy for most applications.

### IV. Data Integrity

Competency data MUST be accurate, consistent, and reliably stored.

- All data modifications MUST be validated before persistence
- Database schema MUST enforce referential integrity via constraints
- No data loss is acceptable; failed operations MUST be rolled back cleanly
- Empty or missing data MUST display meaningful messages, not errors
- Data model changes MUST use versioned migrations with rollback capability

**Rationale**: The core value proposition depends on accurate competency information.
Users make career decisions based on this data; it MUST be trustworthy.

### V. User-Centric Design

Every feature MUST serve a clear user need from the specification.

- Navigation MUST follow the 3-click rule: any content reachable in 3 clicks from landing
- Response times MUST meet specification criteria (2 seconds for data load)
- UI MUST provide clear feedback for loading, errors, and empty states
- Features MUST be independently testable from a user perspective
- Accessibility basics MUST be followed (semantic HTML, keyboard navigation, contrast)

**Rationale**: The application exists to help users plan their careers. Technical excellence
means nothing if users cannot easily find and understand the information they need.

## Quality Standards

### Code Quality

- Code MUST pass linting and formatting checks before commit
- Functions MUST have single responsibility; classes MUST have cohesive purpose
- Magic numbers and strings MUST be extracted to named constants
- Error messages MUST be actionable and user-friendly
- No commented-out code in committed files

### Documentation

- Public APIs MUST have clear documentation (parameters, return values, errors)
- Complex business logic MUST have inline comments explaining the "why"
- README MUST contain setup instructions that work on first attempt
- Architecture decisions MUST be recorded when non-obvious choices are made

### Performance

- Database queries MUST be optimized (indexes, no N+1 queries)
- Frontend MUST not block on unnecessary synchronous operations
- Large data sets MUST be paginated or virtualized
- Performance regressions MUST be caught by monitoring or tests

## Development Workflow

### Feature Development

1. Specification written and approved (spec.md)
2. Implementation plan created (plan.md)
3. Tasks generated and prioritized (tasks.md)
4. Tests written first (failing)
5. Implementation to make tests pass
6. Code review and merge

### Code Review Requirements

- All changes require review before merge to main branch
- Reviewer MUST verify tests exist and pass
- Reviewer MUST verify Constitution principles are followed
- Reviewer MUST be able to run the code locally

### Branch Strategy

- `main` branch MUST always be deployable
- Feature branches use pattern: `NNN-feature-name`
- No direct commits to main; all changes via pull request

## Governance

This Constitution is the supreme authority for development practices in this project.

- All code reviews MUST verify compliance with Constitution principles
- Violations MUST be corrected before merge
- Amendments require:
  1. Written proposal documenting the change and rationale
  2. Update to this document with version increment
  3. Review and approval before taking effect
- Complexity that violates Simplicity First MUST be documented in plan.md Complexity Tracking
- When Constitution and convenience conflict, Constitution wins

**Version**: 1.0.0 | **Ratified**: 2026-01-19 | **Last Amended**: 2026-01-19
