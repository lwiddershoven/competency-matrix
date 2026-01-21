# Migration Runbook: Spring Boot → Quarkus

**Date**: 2026-01-21
**Feature**: Platform Modernization to Quarkus 3.30.6
**Status**: Ready for execution

## Overview

This runbook provides step-by-step instructions for migrating the Career Competency Matrix from Spring Boot 4.0.1 to Quarkus 3.30.6. Follow sequentially to ensure data integrity and minimize risk.

## Pre-Migration Checklist

### 1. Backup & Baseline

- [ ] **Database backup**: `pg_dump competencymatrix > backup-$(date +%Y%m%d).sql`
- [ ] **Code backup**: Ensure current branch is committed and pushed
- [ ] **Tag current version**: `git tag pre-quarkus-migration`
- [ ] **Document current metrics**:
  - [ ] Run performance baseline tests
  - [ ] Record startup time
  - [ ] Document memory footprint
  - [ ] Capture current test coverage percentage

###2. Validation Checks

- [ ] **All tests passing**: `./mvnw test` → 100% pass rate
- [ ] **No uncommitted changes**: `git status` → clean working tree
- [ ] **Database schema documented**: `pg_dump --schema-only` → save for comparison
- [ ] **Row counts recorded**:
  ```sql
  SELECT 'rolename' as table, COUNT(*) FROM rolename UNION ALL
  SELECT 'skill', COUNT(*) FROM skill UNION ALL
  SELECT 'competency_category', COUNT(*) FROM competency_category UNION ALL
  SELECT 'role_skill_requirement', COUNT(*) FROM role_skill_requirement UNION ALL
  SELECT 'role_progression', COUNT(*) FROM role_progression;
  ```

## Migration Steps

### Phase 1: Project Setup

#### Step 1.1: Update pom.xml

Replace Spring Boot parent with Quarkus BOM:

**Remove**:
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.1</version>
</parent>
```

**Add**:
```xml
<properties>
    <quarkus.version>3.30.6</quarkus.version>
    <java.version>25</java.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.quarkus.platform</groupId>
            <artifactId>quarkus-bom</artifactId>
            <version>${quarkus.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

#### Step 1.2: Replace Dependencies

**Remove Spring Boot dependencies**:
```xml
<!-- REMOVE -->
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-thymeleaf</artifactId></dependency>
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jdbc</artifactId></dependency>
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-liquibase</artifactId></dependency>
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-actuator</artifactId></dependency>
<dependency><groupId>io.github.wimdeblauwe</groupId><artifactId>htmx-spring-boot-thymeleaf</artifactId></dependency>
```

**Add Quarkus extensions**:
```xml
<!-- Core Quarkus -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-resteasy-reactive-qute</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-jdbc-postgresql</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-flyway</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>

<!-- Observability -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-micrometer-registry-prometheus</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-health</artifactId>
</dependency>

<!-- Testing -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-junit5</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-test-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<!-- Keep Playwright and Testcontainers -->
```

#### Step 1.3: Update Build Plugins

**Remove**:
```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
</plugin>
```

**Add**:
```xml
<plugin>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-maven-plugin</artifactId>
    <version>${quarkus.version}</version>
    <executions>
        <execution>
            <goals>
                <goal>build</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Phase 2: Configuration Migration

#### Step 2.1: Convert application.yaml → application.properties

Create `src/main/resources/application.properties`:

```properties
# Application
quarkus.application.name=competency-matrix

# Database
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/competencymatrix
quarkus.datasource.username=competency
quarkus.datasource.password=competency

# HTTP
quarkus.http.port=8080

# Management (Actuator equivalent)
quarkus.management.enabled=true
quarkus.management.port=9000

# Health checks
quarkus.smallrye-health.root-path=/health

# Metrics
quarkus.micrometer.export.prometheus.path=/metrics

# Flyway
quarkus.flyway.migrate-at-start=true
quarkus.flyway.locations=db/migration

# Logging
quarkus.log.category."nl.leonw.competencymatrix".level=DEBUG
quarkus.log.category."org.jboss.resteasy.reactive".level=DEBUG
```

**Delete**: `src/main/resources/application.yaml`

#### Step 2.2: Migrate Liquibase to Flyway

1. **Create migration directory**:
   ```bash
   mkdir -p src/main/resources/db/migration
   ```

2. **Convert Liquibase changelog**:
   - Read `src/main/resources/db/changelog/001-initial-schema.sql`
   - Copy SQL content to `src/main/resources/db/migration/V1__initial_schema.sql`
   - Ensure naming: `V{version}__{description}.sql` (two underscores)

3. **Delete Liquibase files**:
   ```bash
   rm -rf src/main/resources/db/changelog/
   ```

### Phase 3: Code Migration

#### Step 3.1: Update Entity Classes

**For each entity** (`Role`, `Skill`, etc.):

1. **Remove Spring Data annotations**:
   - Delete `@Table`, `@Id`, `@Column` imports
   - Keep record structure unchanged
   - Keep business logic methods (e.g., `getDescriptionForLevel`)

2. **Example diff**:
   ```java
   // BEFORE
   import org.springframework.data.annotation.Id;
   import org.springframework.data.relational.core.mapping.Table;

   @Table("rolename")
   public record Role(@Id Integer id, String name, String description) {}

   // AFTER
   public record Role(Integer id, String name, String description) {}
   ```

#### Step 3.2: Migrate Repositories

**For each repository**:

1. **Delete Spring Data interface**:
   ```java
   // DELETE
   public interface RoleRepository extends CrudRepository<Role, Integer> {}
   ```

2. **Create CDI repository class**:
   ```java
   import jakarta.enterprise.context.ApplicationScoped;
   import jakarta.inject.Inject;
   import javax.sql.DataSource;

   @ApplicationScoped
   public class RoleRepository {
       @Inject
       DataSource dataSource;

       public List<Role> findAllOrderByName() {
           String sql = "SELECT id, name, description FROM rolename ORDER BY name";
           try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
               // Manual mapping
           }
       }
   }
   ```

3. **Update package**: `repository/` (no change needed)

#### Step 3.3: Migrate Services

**Update annotations**:
- `@Service` → `@ApplicationScoped`
- `@Autowired` → `@Inject`
- `import org.springframework.stereotype.Service;` → `import jakarta.enterprise.context.ApplicationScoped;`
- `import org.springframework.beans.factory.annotation.Autowired;` → `import jakarta.inject.Inject;`

**Update transactions**:
- `import org.springframework.transaction.annotation.Transactional;` → `import jakarta.transaction.Transactional;`

#### Step 3.4: Migrate Controllers → Resources

**Rename directory**: `controller/` → `resource/`

**For each controller**:

1. **Update annotations**:
   ```java
   // BEFORE
   @Controller
   @RequestMapping("/roles")
   public class RoleController {}

   // AFTER
   @Path("/roles")
   public class RoleResource {}
   ```

2. **Update method annotations**:
   - `@GetMapping` → `@GET`
   - `@PostMapping` → `@POST`
   - `@PathVariable` → `@PathParam`
   - `@RequestParam` → `@QueryParam`
   - Add `@Blocking` to all methods
   - Add `@Produces(MediaType.TEXT_HTML)`

3. **Update template handling**:
   ```java
   // BEFORE
   @GetMapping("/{id}")
   public String getRole(@PathVariable Integer id, Model model) {
       model.addAttribute("role", service.findById(id));
       return "role";
   }

   // AFTER
   @Inject
   Template role;

   @GET
   @Path("{id}")
   @Produces(MediaType.TEXT_HTML)
   @Blocking
   public TemplateInstance getRole(@PathParam("id") Integer id) {
       return role.data("role", service.findById(id));
   }
   ```

#### Step 3.5: Migrate Configuration Classes

**DataSeeder** (ApplicationRunner → StartupEvent):

```java
// BEFORE
@Component
public class DataSeeder implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) {
        // Seed logic
    }
}

// AFTER
@ApplicationScoped
public class DataSeeder {
    void onStart(@Observes StartupEvent event) {
        // Seed logic
    }
}
```

### Phase 4: Template Migration

#### Step 4.1: Migrate Each Template

For each `.html` file in `src/main/resources/templates/`:

1. **Remove Thymeleaf namespace**: Delete `xmlns:th="http://www.thymeleaf.org"`
2. **Convert syntax** (see `contracts/template-migration.md`):
   - `${var}` → `{var}`
   - `th:each` → `{#for}...{/for}`
   - `th:if` → `{#if}...{/if}`
   - `th:href="@{...}"` → `href="..."`
3. **Test rendering** after each template

#### Step 4.2: Migrate Static Resources

Move files:
```bash
mv src/main/resources/static/* src/main/resources/META-INF/resources/
```

### Phase 5: Test Migration

#### Step 5.1: Update Test Annotations

**For each test class**:

```java
// BEFORE
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class RoleControllerTest {}

// AFTER
@QuarkusTest
public class RoleResourceTest {}
```

#### Step 5.2: Update Test Assertions

- REST Assured usage remains similar
- Update imports: `org.springframework.test.*` → `io.quarkus.test.*`
- Remove `@Autowired`, use CDI `@Inject` if needed

#### Step 5.3: Configure DevServices

**Test properties** (`src/test/resources/application.properties`):

```properties
# DevServices will auto-start PostgreSQL container
quarkus.datasource.devservices.enabled=true
```

### Phase 6: Validation

#### Step 6.1: Build & Test

```bash
# Clean build
./mvnw clean compile

# Run tests
./mvnw test

# Expected: 100% pass rate (M test cases from baseline)
```

#### Step 6.2: Data Integrity Check

```bash
# Start application
./mvnw quarkus:dev

# Verify row counts (must match baseline)
psql competencymatrix -c "
SELECT 'rolename' as table, COUNT(*) FROM rolename UNION ALL
SELECT 'skill', COUNT(*) FROM skill UNION ALL
SELECT 'competency_category', COUNT(*) FROM competency_category UNION ALL
SELECT 'role_skill_requirement', COUNT(*) FROM role_skill_requirement UNION ALL
SELECT 'role_progression', COUNT(*) FROM role_progression;
"
```

#### Step 6.3: URL Validation

Test all endpoints:

```bash
curl http://localhost:8080/
curl http://localhost:8080/roles/1
curl http://localhost:8080/compare?from=1&to=2
curl http://localhost:9000/health
curl http://localhost:9000/health/live
curl http://localhost:9000/health/ready
curl http://localhost:9000/metrics
```

#### Step 6.4: Performance Benchmarking

Compare with baseline metrics:

```bash
# Startup time
time ./mvnw quarkus:dev

# Page load (should be <2s per SC-003)
curl -w "@curl-format.txt" -o /dev/null -s http://localhost:8080/roles/1

# Memory usage
ps aux | grep quarkus
```

#### Step 6.5: E2E Tests (Playwright)

```bash
./mvnw test -Dtest=*E2E*
```

Expected: All user workflows complete successfully (VC-004).

### Phase 7: Rollback Procedure

**If critical issues detected**:

1. **Stop application**: `Ctrl+C` or `kill <pid>`
2. **Restore database**:
   ```bash
   psql competencymatrix < backup-YYYYMMDD.sql
   ```
3. **Revert code**:
   ```bash
   git reset --hard pre-quarkus-migration
   ```
4. **Restart Spring Boot**:
   ```bash
   git checkout main
   ./mvnw spring-boot:run
   ```

**Rollback time target**: <5 minutes (SC-009)

## Post-Migration Checklist

- [ ] **All tests passing**: 100% pass rate maintained
- [ ] **Data integrity verified**: Row counts match baseline
- [ ] **URLs functional**: All bookmarks resolve correctly
- [ ] **Performance validated**: Page loads <2s, no regressions
- [ ] **Observability working**: Health/metrics endpoints responding
- [ ] **E2E tests passing**: User workflows complete successfully
- [ ] **No errors in logs**: Clean startup, no exceptions during usage
- [ ] **Pixel-perfect rendering**: Side-by-side comparison with screenshots
- [ ] **Database schema unchanged**: `pg_dump --schema-only` matches baseline

## Final Steps

1. **Tag successful migration**: `git tag quarkus-migration-success`
2. **Update documentation**: README.md with Quarkus-specific instructions
3. **Update CLAUDE.md**: Remove Spring Boot references, add Quarkus context
4. **Monitor production**: 24-hour observation period (SC-007: 99.9% availability)

## Troubleshooting

### Issue: Tests fail with "No datasource found"

**Solution**: Ensure `quarkus-test-testcontainers` dependency added, DevServices enabled in test properties.

### Issue: Templates not rendering

**Solution**: Verify templates in `src/main/resources/templates/`, check `@Inject Template` declarations, ensure `@Produces(MediaType.TEXT_HTML)`.

### Issue: URLs return 404

**Solution**: Check `@Path` annotations, verify JAX-RS resource class is in scanned package (`nl.leonw.competencymatrix`).

### Issue: Database migration fails

**Solution**: Check Flyway migration file naming (`V1__description.sql`), verify SQL syntax, ensure `quarkus.flyway.migrate-at-start=true`.

## Success Criteria Verification

After completing all phases:

- ✅ **SC-001**: All features functional (manual testing + E2E tests)
- ✅ **SC-002**: Critical workflows error-free (E2E test suite)
- ✅ **SC-003**: Page loads <2s (performance benchmarks)
- ✅ **SC-004**: Data complete/accurate (row count verification)
- ✅ **SC-005**: Zero data loss (database comparison)
- ✅ **SC-006**: Concurrent user capacity (load testing)
- ✅ **SC-007**: 99.9% availability (24-hour monitoring)
- ✅ **SC-008**: Bookmarks work (URL validation)
- ✅ **SC-009**: 5min rollback (rollback procedure tested)
- ✅ **SC-010**: Resource consumption (memory/CPU monitoring)

## Estimated Duration

- **Preparation**: 1 hour (backup, baseline, documentation)
- **Phase 1-2**: 2 hours (pom.xml, configuration)
- **Phase 3**: 4 hours (code migration - entities, repos, services, resources)
- **Phase 4**: 3 hours (template migration)
- **Phase 5**: 2 hours (test migration)
- **Phase 6**: 2 hours (validation, verification)

**Total**: 12-14 hours (staged over 2-3 days for safety)
