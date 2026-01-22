
Always use Context7 MCP when I need library/API documentation, code generation, setup or configuration steps without me having to explicitly ask.

All development must follow the project constitution at `.specify/memory/constitution.md`. This includes:
- Simplicity First: Favor simplest solutions, YAGNI
- Test-First Development (TDD): Write tests before implementation
- Monolith First: Single deployable unit
- Data Integrity: Validate all data, enforce referential integrity
- User-Centric Design: 3-click rule, 2-second response times

## Active Technologies
- Java 25 (002-platform-modernization)
- Quarkus 3.30.6 (002-platform-modernization)
  - JAX-RS for REST endpoints
  - Qute for server-side templates
  - Panache JDBC for data access patterns
  - Flyway for database migrations
  - SmallRye Health for health checks
  - Micrometer for metrics
- PostgreSQL 18.1 (no changes to schema or data) (002-platform-modernization)
- htmx for client-side interactivity (unchanged)
- Pico CSS for styling (unchanged)

## Testing Stack
- JUnit 5 with Quarkus Test
- REST Assured for HTTP testing
- Testcontainers for PostgreSQL DevServices
- Playwright for E2E tests

## Recent Changes
- 002-platform-modernization: Migrated from Spring Boot to Quarkus 3.30.6
  - Replaced Spring MVC with JAX-RS
  - Replaced Thymeleaf with Qute templates
  - Replaced Liquibase with Flyway
  - Replaced Spring Actuator with SmallRye Health
  - All URLs and functionality preserved
  - All tests passing (49 total: 21 integration, 9 validation, 11 observability, 8 E2E)
