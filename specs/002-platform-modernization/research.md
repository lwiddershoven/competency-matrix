# Technology Research: Quarkus Migration Decisions

**Date**: 2026-01-21
**Feature**: Platform Modernization to Quarkus 3.30.6
**Status**: Complete

## Summary

This document records technology decisions for migrating from Spring Boot 4 to Quarkus 3.30.6. All decisions prioritize simplicity, maintain current functionality, and avoid reactive programming per requirements.

## Decision 1: Database Migration Tool

**Question**: Flyway vs Liquibase for database migrations in Quarkus?

**Decision**: Use **Flyway**

**Rationale**:
- Quarkus provides first-class support for Flyway (`quarkus-flyway` extension)
- Simpler migration model: SQL-based or Java-based migrations
- Liquibase available only via community extension (less maintained)
- Flyway aligns with Simplicity First principle: fewer XML configurations, straightforward versioned SQL files
- Migration path: Convert existing Liquibase changesets to Flyway SQL migrations

**Implementation**:
- Dependency: `io.quarkus:quarkus-flyway` + `org.flywaydb:flyway-database-postgresql`
- Migration files location: `src/main/resources/db/migration/`
- Naming: `V1__initial_schema.sql` (convert from Liquibase 001-initial-schema.sql)
- Configuration: `quarkus.flyway.migrate-at-start=true`

**Alternatives Considered**:
- **Liquibase Quarkus extension**: Community-maintained, less documentation, more complex XML/YAML format
- **Plain JDBC migrations**: No versioning, manual tracking (rejected - violates Data Integrity principle)

## Decision 2: Templating Engine

**Question**: Qute (Quarkus native) vs Thymeleaf extension?

**Decision**: Use **Qute**

**Rationale**:
- Qute is Quarkus-native with build-time template validation and optimization
- Faster performance: compiled at build time, type-safe
- Simpler syntax than Thymeleaf for common patterns
- Better integration with Quarkus dependency injection (CDI)
- Template migration effort comparable to maintaining Thymeleaf compatibility layer

**Implementation**:
- Dependency: `io.quarkus:quarkus-resteasy-reactive-qute` (RESTEasy Reactive in blocking mode)
- Templates location: `src/main/resources/templates/`
- Template syntax migration:
  - Thymeleaf `th:each="item : ${items}"` → Qute `{#for item in items}`
  - Thymeleaf `th:text="${item.name}"` → Qute `{item.name}`
  - Thymeleaf `th:if="${condition}"` → Qute `{#if condition}`
- Template parameters: `@Inject Template myTemplate;` then `myTemplate.data("key", value)`

**Alternatives Considered**:
- **Thymeleaf Quarkus extension**: Available but not Quarkus-optimized, slower, less type-safe
- **Server-side React/Vue**: Over-engineered for server-rendered app (rejected - Simplicity First violation)

## Decision 3: htmx Integration

**Question**: How to integrate htmx without `spring-boot-htmx-thymeleaf` library?

**Decision**: **Direct htmx integration** with manual request/response handling

**Rationale**:
- htmx is framework-agnostic JavaScript library (works with any backend)
- Spring htmx library provided convenience annotations (`@HxRequest`, `@HxTrigger`) but not essential
- Quarkus JAX-RS can detect htmx headers manually: `@HeaderParam("HX-Request")`
- Qute templates can include htmx attributes directly in HTML
- Simpler approach: use htmx as intended (via HTML attributes), handle responses in JAX-RS resources

**Implementation**:
- Include htmx.js in static resources: `src/main/resources/META-INF/resources/js/htmx.min.js`
- Qute templates use htmx attributes:
  ```html
  <button hx-get="/api/roles/{roleId}" hx-target="#content">Load Role</button>
  ```
- JAX-RS resources detect htmx:
  ```java
  @GET
  @Path("/roles/{id}")
  @Produces(MediaType.TEXT_HTML)
  public TemplateInstance getRole(@PathParam("id") Long id, @HeaderParam("HX-Request") String hxRequest) {
      if (hxRequest != null) {
          return rolePartial.data("role", service.findRole(id)); // Return partial
      }
      return rolePage.data("role", service.findRole(id)); // Return full page
  }
  ```

**Alternatives Considered**:
- **Port spring-boot-htmx-thymeleaf to Quarkus**: Unnecessary complexity for small convenience features
- **Vanilla JavaScript**: Loses htmx declarative benefits (rejected)

## Decision 4: REST Framework

**Question**: RESTEasy Classic vs RESTEasy Reactive (blocking mode)?

**Decision**: Use **RESTEasy Reactive in blocking mode**

**Rationale**:
- RESTEasy Reactive is the recommended default for Quarkus 3.x
- Can run entirely in blocking mode using `@Blocking` annotation
- Better performance even in blocking mode compared to RESTEasy Classic
- Future-proof: RESTEasy Classic is in maintenance mode
- No reactive code required: `@Blocking` forces traditional thread-per-request model
- Aligns with user requirement: "no reactive APIs"

**Implementation**:
- Dependency: `io.quarkus:quarkus-resteasy-reactive`
- Mark all resource methods with `@Blocking`:
  ```java
  @Path("/roles")
  public class RoleResource {
      @GET
      @Blocking
      public List<Role> listRoles() {
          return roleRepository.findAll(); // Traditional blocking call
      }
  }
  ```
- Alternative: Use `@RunOnVirtualThread` (Java 21+) for better resource utilization with blocking code

**Alternatives Considered**:
- **RESTEasy Classic**: Older, slower, maintenance mode only
- **Fully reactive with Mutiny**: Requires rewriting all data access code, violates "no reactive" requirement

## Decision 5: Data Access Pattern

**Question**: Panache Active Record vs Panache Repository vs Plain JDBC?

**Decision**: Use **Plain JDBC** with repository pattern (closest to Spring Data JDBC)

**Rationale**:
- Current app uses Spring Data JDBC (not JPA/Hibernate)
- Panache is JPA/Hibernate-based: would force entity annotations, lazy loading, session management
- Plain JDBC maintains current simplicity: explicit SQL, no OR/M overhead
- Repository pattern preserved: Quarkus supports custom repository interfaces with `@ApplicationScoped`
- Avoids JPA complexity (proxies, detached entities, transaction edge cases)

**Implementation**:
- Dependency: `io.quarkus:quarkus-jdbc-postgresql` (no `quarkus-hibernate-orm-panache`)
- Repository pattern with CDI:
  ```java
  @ApplicationScoped
  public class RoleRepository {
      @Inject
      DataSource dataSource;

      public List<Role> findAll() {
          try (Connection conn = dataSource.getConnection();
               PreparedStatement stmt = conn.prepareStatement("SELECT * FROM roles ORDER BY name")) {
              ResultSet rs = stmt.executeQuery();
              List<Role> roles = new ArrayList<>();
              while (rs.next()) {
                  roles.add(mapRole(rs));
              }
              return roles;
          }
      }
  }
  ```
- Transaction management: Quarkus Narayana for `@Transactional` support

**Alternatives Considered**:
- **Panache Active Record**: Requires JPA, adds complexity, changes entity structure (rejected)
- **Panache Repository**: Still JPA-based, auto-generates queries (less control, rejected)
- **jOOQ**: Type-safe SQL DSL but adds dependency and learning curve (overkill for this app)

## Decision 6: Observability & Metrics

**Question**: Micrometer vs SmallRye Metrics for Prometheus?

**Decision**: Use **Micrometer** with Prometheus registry

**Rationale**:
- Current app uses Micrometer - maintains consistency
- Quarkus supports Micrometer via `quarkus-micrometer-registry-prometheus`
- Can preserve `/actuator/prometheus` endpoint path with configuration
- SmallRye Metrics is Quarkus default but uses different API (requires code changes)
- Micrometer has broader ecosystem support

**Implementation**:
- Dependency: `io.quarkus:quarkus-micrometer-registry-prometheus`
- Health checks: `io.quarkus:quarkus-smallrye-health` (separate from metrics)
- Configuration:
  ```properties
  quarkus.micrometer.export.prometheus.path=/q/metrics
  # Optionally map to /actuator/prometheus for compatibility:
  # quarkus.http.non-application-root-path=/actuator
  ```
- Health endpoints: `/q/health`, `/q/health/live`, `/q/health/ready`

**Alternatives Considered**:
- **SmallRye Metrics**: Requires rewriting all custom metrics code (rejected - unnecessary migration cost)
- **No metrics**: Violates observability requirements (rejected)

## Decision 7: Test Infrastructure

**Question**: Quarkus DevServices vs Custom Testcontainers setup?

**Decision**: Use **Quarkus DevServices** with manual Testcontainers fallback for complex scenarios

**Rationale**:
- DevServices automatically starts PostgreSQL container for tests (zero configuration)
- Seamless integration with `@QuarkusTest` annotation
- Testcontainers still available for explicit control (e.g., Playwright E2E tests)
- Simpler than Spring Boot Test slices: single `@QuarkusTest` annotation

**Implementation**:
- Dependency: `io.quarkus:quarkus-junit5`, `io.quarkus:quarkus-test-testcontainers`
- Basic test:
  ```java
  @QuarkusTest
  public class RoleResourceTest {
      @Test
      public void testListRoles() {
          given()
              .when().get("/roles")
              .then().statusCode(200);
      }
  }
  ```
- DevServices auto-starts PostgreSQL (no manual container management)
- E2E tests: Keep explicit Testcontainers for Playwright scenarios

**Alternatives Considered**:
- **Manual Testcontainers everywhere**: More boilerplate, slower (DevServices is optimized)
- **H2 in-memory database**: Different SQL dialect, doesn't test PostgreSQL-specific features (rejected)

## Configuration Migration

**Spring Boot application.yaml → Quarkus application.properties**:

| Spring Boot | Quarkus Equivalent |
|-------------|-------------------|
| `spring.datasource.url` | `quarkus.datasource.jdbc.url` |
| `spring.datasource.username` | `quarkus.datasource.username` |
| `spring.datasource.password` | `quarkus.datasource.password` |
| `server.port` | `quarkus.http.port` |
| `management.server.port` | `quarkus.management.port` |
| `management.endpoints.web.exposure.include` | `quarkus.management.endpoints.web.exposure.include` |
| `logging.level.*` | `quarkus.log.category."*".level` |

## Summary of Technology Stack

**Current (Spring Boot 4)**:
- Spring Web + Spring Data JDBC
- Thymeleaf + htmx-spring-boot-thymeleaf
- Liquibase
- Spring Boot Actuator + Micrometer
- Spring Boot Test + Testcontainers

**Target (Quarkus 3.30.6)**:
- RESTEasy Reactive (blocking mode) + Plain JDBC
- Qute + direct htmx integration
- Flyway
- SmallRye Health + Micrometer
- Quarkus Test (DevServices) + Testcontainers

## Next Steps

1. ✅ Research complete - all technology decisions documented
2. → Phase 1: Generate `data-model.md` (entity annotations mapping)
3. → Phase 1: Generate `contracts/` (URL mapping, template syntax conversion)
4. → Phase 1: Generate `quickstart.md` (step-by-step migration runbook)
