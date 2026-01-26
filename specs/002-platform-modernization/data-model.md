# Data Model: Entity Migration Mapping

**Date**: 2026-01-21
**Feature**: Platform Modernization to Quarkus
**Status**: Complete

## Overview

This document maps Spring Data JDBC entities to Quarkus JDBC entities. Since we're using plain JDBC (not Hibernate/JPA), entities remain simple POJOs/records without ORM annotations. The database schema is unchanged.

## Entity Migration Strategy

**Approach**: Records with no framework annotations

- Spring Data JDBC annotations (`@Id`, `@Table`, `@Column`) **removed**
- Plain Java records for immutability and simplicity
- Mapping handled in repository layer (manual ResultSet → Record conversion)
- No JPA/Hibernate entities (maintains current architecture)

## Entity Mappings

### 1. Role

**Current (Spring Boot)**:
```java
@Table("rolename")
public record Role(
    @Id Integer id,
    String name,
    String description
) {}
```

**Target (Quarkus)**:
```java
// No annotations - plain record
public record Role(
    Integer id,
    String name,
    String description
) {
    public Role(String name, String description) {
        this(null, name, description);
    }
}
```

**Database Table**: `rolename` (unchanged)
**Columns**: `id` (PK), `name`, `description`

---

### 2. Skill

**Current (Spring Boot)**:
```java
@Table("skill")
public record Skill(
    @Id Integer id,
    String name,
    @Column("category_id") Integer categoryId,
    @Column("basic_description") String basicDescription,
    @Column("decent_description") String decentDescription,
    @Column("good_description") String goodDescription,
    @Column("excellent_description") String excellentDescription
) {}
```

**Target (Quarkus)**:
```java
// No annotations - plain record
public record Skill(
    Integer id,
    String name,
    Integer categoryId,
    String basicDescription,
    String decentDescription,
    String goodDescription,
    String excellentDescription
) {
    public Skill(String name, Integer categoryId, String basicDescription,
                 String decentDescription, String goodDescription, String excellentDescription) {
        this(null, name, categoryId, basicDescription, decentDescription, goodDescription, excellentDescription);
    }

    public String getDescriptionForLevel(ProficiencyLevel level) {
        return switch (level) {
            case BASIC -> basicDescription;
            case DECENT -> decentDescription;
            case GOOD -> goodDescription;
            case EXCELLENT -> excellentDescription;
        };
    }
}
```

**Database Table**: `skill` (unchanged)
**Columns**: `id` (PK), `name`, `category_id` (FK), `basic_description`, `decent_description`, `good_description`, `excellent_description`

**Note**: Column name mapping (camelCase → snake_case) handled in repository ResultSet mapping.

---

### 3. CompetencyCategory

**Current (Spring Boot)**:
```java
@Table("competency_category")
public record CompetencyCategory(
    @Id Integer id,
    String name
) {}
```

**Target (Quarkus)**:
```java
// No annotations - plain record
public record CompetencyCategory(
    Integer id,
    String name
) {
    public CompetencyCategory(String name) {
        this(null, name);
    }
}
```

**Database Table**: `competency_category` (unchanged)
**Columns**: `id` (PK), `name`

---

### 4. ProficiencyLevel

**Current (Spring Boot)**:
```java
public enum ProficiencyLevel {
    BASIC, DECENT, GOOD, EXCELLENT
}
```

**Target (Quarkus)**:
```java
// No changes - enums are framework-independent
public enum ProficiencyLevel {
    BASIC, DECENT, GOOD, EXCELLENT
}
```

**Database Representation**: Stored as integer or string in `role_skill_requirement` table

---

### 5. RoleSkillRequirement

**Current (Spring Boot)**:
```java
@Table("role_skill_requirement")
public record RoleSkillRequirement(
    @Id Integer id,
    @Column("role_id") Integer roleId,
    @Column("skill_id") Integer skillId,
    @Column("proficiency_level") ProficiencyLevel proficiencyLevel
) {}
```

**Target (Quarkus)**:
```java
// No annotations - plain record
public record RoleSkillRequirement(
    Integer id,
    Integer roleId,
    Integer skillId,
    ProficiencyLevel proficiencyLevel
) {
    public RoleSkillRequirement(Integer roleId, Integer skillId, ProficiencyLevel proficiencyLevel) {
        this(null, roleId, skillId, proficiencyLevel);
    }
}
```

**Database Table**: `role_skill_requirement` (unchanged)
**Columns**: `id` (PK), `role_id` (FK), `skill_id` (FK), `proficiency_level`

---

### 6. RoleProgression

**Current (Spring Boot)**:
```java
@Table("role_progression")
public record RoleProgression(
    @Id Integer id,
    @Column("from_role_id") Integer fromRoleId,
    @Column("to_role_id") Integer toRoleId
) {}
```

**Target (Quarkus)**:
```java
// No annotations - plain record
public record RoleProgression(
    Integer id,
    Integer fromRoleId,
    Integer toRoleId
) {
    public RoleProgression(Integer fromRoleId, Integer toRoleId) {
        this(null, fromRoleId, toRoleId);
    }
}
```

**Database Table**: `role_progression` (unchanged)
**Columns**: `id` (PK), `from_role_id` (FK), `to_role_id` (FK)

---

## Repository Layer Pattern

### Current (Spring Data JDBC)

```java
public interface RoleRepository extends CrudRepository<Role, Integer> {
    List<Role> findAllByOrderByName();
}
```

### Target (Quarkus Plain JDBC)

```java
@ApplicationScoped
public class RoleRepository {

    @Inject
    DataSource dataSource;

    public List<Role> findAllOrderByName() {
        String sql = "SELECT id, name, description FROM rolename ORDER BY name";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            List<Role> roles = new ArrayList<>();
            while (rs.next()) {
                roles.add(new Role(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description")
                ));
            }
            return roles;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch roles", e);
        }
    }

    @Transactional
    public Role save(Role role) {
        if (role.id() == null) {
            return insert(role);
        } else {
            return update(role);
        }
    }

    private Role insert(Role role) {
        String sql = "INSERT INTO rolename (name, description) VALUES (?, ?) RETURNING id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, role.name());
            stmt.setString(2, role.description());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Role(rs.getInt(1), role.name(), role.description());
                }
                throw new SQLException("Insert failed, no ID returned");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert role", e);
        }
    }
}
```

## Validation Annotations Migration

### Current (Spring Validation)

```java
import org.springframework.validation.annotation.Validated;
import javax.validation.constraints.*;

@Validated
public record CreateRoleRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 500) String description
) {}
```

### Target (Jakarta Validation)

```java
import jakarta.validation.constraints.*;

// @Validated not needed in Quarkus - automatic validation
public record CreateRoleRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 500) String description
) {}
```

**Change**: `javax.validation.*` → `jakarta.validation.*` (package rename only)

## Transaction Management

### Current (Spring)

```java
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleService {
    @Transactional
    public Role createRole(String name, String description) {
        // ...
    }
}
```

### Target (Quarkus)

```java
import jakarta.transaction.Transactional;

@ApplicationScoped
public class RoleService {
    @Transactional
    public Role createRole(String name, String description) {
        // ...
    }
}
```

**Changes**:
- `@Service` → `@ApplicationScoped` (CDI instead of Spring)
- `org.springframework.transaction.annotation.Transactional` → `jakarta.transaction.Transactional`

## Summary of Changes

| Aspect | Spring Boot | Quarkus |
|--------|-------------|---------|
| **Entity Annotations** | `@Table`, `@Id`, `@Column` | None (plain records) |
| **Repository Pattern** | `CrudRepository` interface | `@ApplicationScoped` class with manual JDBC |
| **Dependency Injection** | `@Autowired` | `@Inject` |
| **Bean Scope** | `@Service`, `@Component` | `@ApplicationScoped`, `@RequestScoped` |
| **Validation** | `javax.validation.*` | `jakarta.validation.*` |
| **Transactions** | `org.springframework.transaction.*` | `jakarta.transaction.Transactional` |

## Database Schema

**No changes to database schema**. All tables, columns, constraints, and indexes remain identical. This migration is purely a framework change at the application layer.

## Next Steps

1. ✅ Entity mappings documented
2. → Generate `contracts/` with URL and template mappings
3. → Generate `quickstart.md` with step-by-step migration runbook
