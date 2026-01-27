# Quickstart: Matrix Overview Implementation

**Feature**: 004-matrix-overview
**Date**: 2026-01-26
**Audience**: Developers implementing this feature

## Prerequisites

- Java 25 installed
- Maven 3.9+
- PostgreSQL 18.1 running locally (or via Docker)
- Quarkus CLI (optional but recommended)

## Overview

This feature adds a matrix/table overview page showing all skills as rows and all roles as columns. Implementation follows TDD principles from the Constitution.

## Development Workflow

```
1. Run database migration (V2__add_role_grouping.sql)
2. Update seed data (competencies.yaml)
3. Write failing tests (TDD)
4. Implement DTOs (MatrixViewModel, MatrixRow, MatrixCell)
5. Implement service methods (CompetencyService.buildMatrixViewModel)
6. Implement resource endpoint (MatrixOverviewResource)
7. Create Qute templates (matrix-overview.html, matrix-tooltip.html)
8. Add CSS styles (matrix.css)
9. Add JavaScript (matrix.js for tooltips)
10. Verify tests pass
11. Manual testing (browser)
```

## Step 1: Database Migration

Create migration file:

```bash
# File: src/main/resources/db/migration/V2__add_role_grouping.sql
```

```sql
-- Add role grouping fields
ALTER TABLE rolename ADD COLUMN role_family VARCHAR(50);
ALTER TABLE rolename ADD COLUMN seniority_order INTEGER;

-- Populate existing roles
UPDATE rolename SET role_family = 'Developer', seniority_order = 1
  WHERE name = 'Junior Developer';
UPDATE rolename SET role_family = 'Developer', seniority_order = 2
  WHERE name = 'Medior Developer';
UPDATE rolename SET role_family = 'Developer', seniority_order = 3
  WHERE name = 'Senior Developer';
UPDATE rolename SET role_family = 'Developer', seniority_order = 4
  WHERE name = 'Specialist Developer';
UPDATE rolename SET role_family = 'Developer', seniority_order = 5
  WHERE name = 'Lead Developer';
UPDATE rolename SET role_family = 'Architect', seniority_order = 1
  WHERE name = 'Lead Developer / Software Architect';
UPDATE rolename SET role_family = 'Architect', seniority_order = 2
  WHERE name = 'Software Architect';
UPDATE rolename SET role_family = 'Architect', seniority_order = 3
  WHERE name = 'Solution Architect';
UPDATE rolename SET role_family = 'Operations', seniority_order = 1
  WHERE name = 'DevOps Engineer';

-- Add constraints
ALTER TABLE rolename ALTER COLUMN role_family SET NOT NULL;
ALTER TABLE rolename ALTER COLUMN seniority_order SET NOT NULL;
```

Run migration:

```bash
# Quarkus dev mode will auto-apply
./mvnw quarkus:dev

# Or manually via Flyway
./mvnw flyway:migrate
```

## Step 2: Update Seed Data

Edit `src/main/resources/seed/competencies.yaml`:

```yaml
roles:
  - name: "Junior Developer"
    roleFamily: "Developer"
    seniorityOrder: 1
    description: "Entry-level software developer"

  - name: "Medior Developer"
    roleFamily: "Developer"
    seniorityOrder: 2
    description: "Mid-level software developer"

  # ... add roleFamily and seniorityOrder to all roles
```

## Step 3: Write Failing Tests (TDD)

Create test file:

```bash
# File: src/test/java/nl/leonw/competencymatrix/resource/MatrixOverviewResourceTest.java
```

```java
@QuarkusTest
class MatrixOverviewResourceTest {

    @Test
    void matrixOverviewPageReturnsAllSkillsAndRoles() {
        given()
            .when().get("/matrix")
            .then()
            .statusCode(200)
            .body(containsString("matrix-table"))
            .body(containsString("Junior Developer"))
            .body(containsString("Java"));
    }

    @Test
    void matrixOverviewSupportsC ategoryFiltering() {
        given()
            .queryParam("category", 1)
            .when().get("/matrix")
            .then()
            .statusCode(200)
            .body(containsString("matrix-table"));
    }

    @Test
    void skillTooltipReturnsDescriptionForLevel() {
        given()
            .queryParam("level", "DECENT")
            .when().get("/matrix/tooltips/skill/1")
            .then()
            .statusCode(200)
            .body(containsString("tooltip-content"))
            .body(containsString("DECENT"));
    }
}
```

Run tests (they will fail):

```bash
./mvnw test
```

## Step 4: Implement DTOs

Create DTO package:

```bash
mkdir -p src/main/java/nl/leonw/competencymatrix/dto
```

Implement DTOs (see data-model.md for full code):
- `MatrixViewModel.java`
- `MatrixRow.java`
- `MatrixCell.java`
- `MatrixColumnHeader.java`

## Step 5: Extend CompetencyService

Edit `src/main/java/nl/leonw/competencymatrix/service/CompetencyService.java`:

```java
public MatrixViewModel buildMatrixViewModel(Optional<Integer> categoryId) {
    // 1. Load all entities
    List<Skill> skills = categoryId
        .map(id -> skillRepository.findByCategoryId(id))
        .orElse(skillRepository.findAllOrderByName());

    List<Role> roles = roleRepository.findAllOrderByFamilyAndSeniority();
    List<RoleSkillRequirement> requirements = requirementRepository.findAll();
    List<CompetencyCategory> categories = categoryRepository.findAllOrderByName();

    // 2. Group roles by family
    Map<String, List<MatrixColumnHeader>> rolesByFamily = roles.stream()
        .collect(Collectors.groupingBy(
            Role::roleFamily,
            TreeMap::new,
            Collectors.mapping(
                MatrixColumnHeader::from,
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    list -> list.stream()
                        .sorted(Comparator.comparingInt(h -> h.role().seniorityOrder()))
                        .toList()
                )
            )
        ));

    // 3. Build matrix rows
    List<MatrixRow> rows = skills.stream()
        .map(skill -> buildMatrixRow(skill, roles, requirements))
        .toList();

    // 4. Return view model
    return categoryId
        .map(id -> MatrixViewModel.filtered(rows, rolesByFamily, categories, String.valueOf(id)))
        .orElse(MatrixViewModel.unfiltered(rows, rolesByFamily, categories));
}

private MatrixRow buildMatrixRow(Skill skill, List<Role> roles, List<RoleSkillRequirement> allRequirements) {
    Map<Integer, ProficiencyLevel> requirementMap = allRequirements.stream()
        .filter(req -> req.skill().id().equals(skill.id()))
        .collect(Collectors.toMap(
            req -> req.role().id(),
            RoleSkillRequirement::requiredLevel
        ));

    List<MatrixCell> cells = roles.stream()
        .map(role -> requirementMap.containsKey(role.id())
            ? MatrixCell.withLevel(skill.id(), role.id(), requirementMap.get(role.id()))
            : MatrixCell.empty(skill.id(), role.id()))
        .toList();

    return new MatrixRow(skill, cells);
}
```

## Step 6: Implement Resource Endpoint

Create `src/main/java/nl/leonw/competencymatrix/resource/MatrixOverviewResource.java`:

```java
@Path("/matrix")
@Produces(MediaType.TEXT_HTML)
public class MatrixOverviewResource {

    @Inject
    CompetencyService competencyService;

    @GET
    public TemplateInstance getMatrixOverview(@QueryParam("category") Optional<Integer> categoryId) {
        MatrixViewModel viewModel = competencyService.buildMatrixViewModel(categoryId);
        return Templates.matrixOverview(viewModel);
    }

    @GET
    @Path("/tooltips/skill/{skillId}")
    public TemplateInstance getSkillTooltip(
        @PathParam("skillId") Long skillId,
        @QueryParam("level") String level
    ) {
        Skill skill = competencyService.getSkillById(skillId);
        String description = skill.getDescriptionForLevel(level);

        return Templates.skillTooltip(skill, level, description);
    }
}
```

## Step 7: Create Qute Templates

### Main Template

Create `src/main/resources/templates/matrix-overview.html`:

```html
{#include layout.html}
{#title}Matrix Overview{/title}

{#content}
<article>
    <header>
        <h1>Competency Matrix Overview</h1>
        <p>Compare skill requirements across all roles</p>
    </header>

    <!-- Category Filter -->
    <div class="filter-controls">
        <label for="category-filter">Filter by category:</label>
        <select
            id="category-filter"
            name="category"
            hx-get="/matrix"
            hx-target="body"
            hx-push-url="true">
            <option value="">All Skills</option>
            {#for category in viewModel.categories}
            <option value="{category.id}"
                    {#if category.id == viewModel.selectedCategoryId}selected{/if}>
                {category.name}
            </option>
            {/for}
        </select>
    </div>

    <!-- Matrix Table -->
    <div class="matrix-container">
        <table class="matrix-table">
            <thead>
                <tr>
                    <th>Skills / Roles</th>
                    {#for entry in viewModel.rolesByFamily}
                    {#for header in entry.value}
                    <th title="{header.role.name}">
                        {header.abbreviation}
                    </th>
                    {/for}
                    {/for}
                </tr>
            </thead>
            <tbody>
                {#for row in viewModel.rows}
                <tr>
                    <th>{row.skill.name}</th>
                    {#for cell in row.cells}
                    <td>
                        {#if !cell.isEmpty}
                        <button
                            class="level-badge {cell.getLevelCssClass()}"
                            popovertarget="tooltip-{cell.skillId}-{cell.roleId}"
                            hx-get="/matrix/tooltips/skill/{cell.skillId}?level={cell.requiredLevel.name()}"
                            hx-trigger="mouseenter once, focus once"
                            hx-target="#tooltip-{cell.skillId}-{cell.roleId}"
                            hx-swap="innerHTML">
                            {cell.getDisplayText()}
                        </button>
                        <div popover="manual"
                             id="tooltip-{cell.skillId}-{cell.roleId}"
                             role="tooltip"
                             class="tooltip-popover">
                            Loading...
                        </div>
                        {/if}
                    </td>
                    {/for}
                </tr>
                {/for}
            </tbody>
        </table>
    </div>
</article>
{/content}
{/include}
```

### Tooltip Template

Create `src/main/resources/templates/fragments/matrix-tooltip.html`:

```html
<div class="tooltip-content">
    <h5>{skill.name}</h5>
    <p><strong class="level-badge level-{level.toLowerCase()}">{level}</strong></p>
    <p>{description}</p>
</div>
```

## Step 8: Add CSS Styles

Create `src/main/resources/META-INF/resources/css/matrix.css`:

```css
/* Matrix Container */
.matrix-container {
    max-height: calc(100vh - 250px);
    overflow: auto;
    border: 1px solid var(--pico-border-color);
    border-radius: var(--pico-border-radius);
}

/* Matrix Table */
.matrix-table {
    width: 100%;
    border-collapse: separate;
    border-spacing: 0;
    font-size: 0.9rem;
}

/* Sticky Column Headers */
.matrix-table thead th {
    position: sticky;
    top: 0;
    background-color: var(--pico-card-background-color);
    z-index: 3;
    font-weight: 600;
    text-align: center;
    padding: 1rem 0.75rem;
    border-bottom: 2px solid var(--pico-primary);
}

/* Fixed Row Headers */
.matrix-table tbody th {
    position: sticky;
    left: 0;
    background-color: var(--pico-card-background-color);
    z-index: 2;
    font-weight: 500;
    text-align: left;
    padding: 0.75rem;
    border-right: 2px solid var(--pico-border-color);
}

/* Top-left corner */
.matrix-table thead th:first-child {
    left: 0;
    z-index: 4;
}

/* Table cells */
.matrix-table td {
    padding: 0.5rem;
    min-width: 120px;
    text-align: center;
    border: 1px solid var(--pico-border-color);
}

/* Level badges */
.level-badge {
    display: inline-block;
    padding: 0.25rem 0.75rem;
    border-radius: 4px;
    font-size: 0.875rem;
    font-weight: 500;
    border: none;
    cursor: pointer;
}

.level-basic { background-color: #e3f2fd; color: #1976d2; }
.level-decent { background-color: #fff3e0; color: #f57c00; }
.level-good { background-color: #e8f5e9; color: #388e3c; }
.level-excellent { background-color: #f3e5f5; color: #7b1fa2; }

/* Tooltips */
[popover].tooltip-popover {
    max-width: min(90vw, 500px);
    max-height: 90vh;
    overflow-y: auto;
    padding: 1rem;
    border: 1px solid var(--pico-card-border-color);
    background: var(--pico-card-background-color);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}
```

Include in layout.html:

```html
<link rel="stylesheet" href="/css/matrix.css">
```

## Step 9: Add JavaScript

Create `src/main/resources/META-INF/resources/js/matrix.js`:

```javascript
// Tooltip positioning
document.addEventListener('beforetoggle', (e) => {
    if (e.target.matches('[popover].tooltip-popover') && e.newState === 'open') {
        const trigger = document.querySelector(`[popovertarget="${e.target.id}"]`);
        if (!trigger) return;

        const triggerRect = trigger.getBoundingClientRect();
        const tooltipRect = e.target.getBoundingClientRect();
        const spacing = 8;

        let top = triggerRect.bottom + spacing;
        let left = triggerRect.left;

        // Flip vertically if needed
        if (top + tooltipRect.height > window.innerHeight - spacing) {
            top = triggerRect.top - tooltipRect.height - spacing;
        }

        // Shift horizontally if needed
        if (left + tooltipRect.width > window.innerWidth - spacing) {
            left = window.innerWidth - tooltipRect.width - spacing;
        }
        if (left < spacing) {
            left = spacing;
        }

        e.target.style.top = `${top}px`;
        e.target.style.left = `${left}px`;
    }
});
```

Include in layout.html:

```html
<script src="/js/matrix.js"></script>
```

## Step 10: Update Navigation

Edit `src/main/resources/templates/layout.html`:

```html
<nav>
    <ul>
        <li><a href="/" class="brand"><strong>Competency Matrix</strong></a></li>
        <li><a href="/matrix">Matrix Overview</a></li> <!-- NEW -->
    </ul>
    <!-- ... theme toggle -->
</nav>
```

## Step 11: Run Tests

```bash
# Run all tests
./mvnw test

# Run specific test
./mvnw test -Dtest=MatrixOverviewResourceTest
```

## Step 12: Manual Testing

Start dev server:

```bash
./mvnw quarkus:dev
```

Open browser:
- Main page: http://localhost:8080/matrix
- With filter: http://localhost:8080/matrix?category=1

Test checklist:
- [ ] All skills displayed alphabetically
- [ ] Roles grouped by family (Developer, Architect, Operations)
- [ ] Roles within families ordered by seniority
- [ ] Hover over proficiency badge shows tooltip
- [ ] Tooltip displays on tap (mobile simulation in DevTools)
- [ ] Category filter updates matrix
- [ ] Sticky headers work when scrolling
- [ ] Empty cells are blank (no N/A text)

## Troubleshooting

### Migration doesn't run
- Check `application.properties`: `quarkus.flyway.migrate-at-start=true`
- Delete `flyway_schema_history` table and restart

### Tests fail with "table rolename has no column role_family"
- Migration not applied to test database
- Check test resources: `src/test/resources/application.properties`

### Tooltips don't show
- Check browser console for JavaScript errors
- Verify Popover API support (Chrome 114+, Safari 17+)
- Check htmx is loaded (`<script src="https://unpkg.com/htmx.org@2.0.4"></script>`)

### Sticky headers don't stick
- Verify `border-collapse: separate` on table
- Check z-index values in CSS
- Ensure container has `overflow: auto` and explicit height

## Next Steps

After implementation:
1. Run `/speckit.tasks` to generate task breakdown
2. Follow TDD workflow: test → implement → refactor
3. Create pull request with Constitution compliance checklist
4. Request code review

## Related Documentation

- [spec.md](spec.md) - Feature specification
- [plan.md](plan.md) - Implementation plan
- [data-model.md](data-model.md) - Data structures
- [research.md](research.md) - Technical decisions
- [contracts/matrix-api.yaml](contracts/matrix-api.yaml) - API contract
