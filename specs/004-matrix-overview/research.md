# Research: Competency Matrix Table Overview

**Date**: 2026-01-26
**Feature**: Matrix overview page with sticky headers, tooltips, and category filtering

## Research Questions

This document resolves technical uncertainties from the Technical Context section of plan.md:

1. How to implement sticky table headers and fixed columns for large matrix (100 skills × 20 roles)?
2. What's the best approach for tooltips with hover/tap interaction and dynamic content loading?
3. How to group roles by family and order by seniority within groups?

---

## 1. Sticky Headers and Fixed Columns

### Decision

**Use HTML `<table>` with CSS `position: sticky`**

### Rationale

- **Semantic & Accessible**: Native `<table>` preserves screen reader compatibility
- **Browser Support**: `position: sticky` fully supported on table elements in all modern browsers (2026)
- **Performance**: CSS-only solution outperforms JavaScript-based alternatives for 2,000 cells (100×20)
- **Maintainability**: Minimal CSS (~30 lines), no JavaScript required for core functionality
- **Consistency**: Project already uses HTML tables (comparison-table.html) with Pico CSS

### Alternatives Considered

- **CSS Grid**: Rejected due to accessibility issues (requires `display: contents` hack) and complexity
- **Flexbox**: Rejected as designed for one-dimensional layouts, not tabular data
- **JavaScript libraries**: Rejected as over-engineered when CSS-only solution works

### Implementation Pattern

```css
/* Scrollable container */
.matrix-container {
    max-height: calc(100vh - 250px);
    overflow: auto;
    position: relative;
}

/* Table with separated borders (required for sticky) */
.matrix-table {
    border-collapse: separate;
    border-spacing: 0;
}

/* Sticky column headers (vertical scroll) */
.matrix-table thead th {
    position: sticky;
    top: 0;
    background-color: var(--pico-card-background-color);
    z-index: 3;
}

/* Fixed row headers (horizontal scroll) */
.matrix-table tbody th {
    position: sticky;
    left: 0;
    background-color: var(--pico-card-background-color);
    z-index: 2;
}

/* Top-left corner (both sticky) */
.matrix-table thead th:first-child {
    left: 0;
    z-index: 4;
}
```

### Key Requirements

1. **`border-collapse: separate`** - Required for `position: sticky` to work
2. **Background colors** - Prevent scrolling content from showing through sticky elements
3. **Z-index layering** - Corner cell (4) > column headers (3) > row headers (2) > regular cells (1)
4. **Explicit dimensions** - Container height prevents rendering all rows initially (performance)

---

## 2. Tooltip Implementation

### Decision

**Use HTML Popover API with htmx for dynamic content loading**

### Rationale

- **Native Browser Support**: Popover API achieved Baseline status (2024-2025), production-ready for 2026
- **Perfect for htmx**: Server-rendered HTML loaded via `hx-get`, browser handles overlay mechanics
- **Accessibility Built-In**: ARIA roles, keyboard support (Tab, ESC), focus management all native
- **Touch Device Support**: Click/tap to open, click outside to dismiss - works identically on mobile and desktop
- **Minimal JavaScript**: Only needed for viewport positioning logic, not core functionality

### Alternatives Considered

- **Pure CSS tooltips**: Rejected due to no touch device support and cannot load dynamic content
- **Floating UI library (3KB)**: Excellent but overkill when Popover API provides features natively
- **Full libraries (Tippy.js)**: Rejected as too heavy (5-10KB+) for vanilla JS + CSS goal
- **Custom JavaScript**: Rejected as reinventing the wheel (Popover API does this better)

### Implementation Pattern

```html
<!-- Trigger element with htmx lazy loading -->
<button
  popovertarget="skill-tooltip-{skillId}"
  hx-get="/matrix/tooltips/skill/{skillId}?level={level}"
  hx-trigger="mouseenter once, focus once"
  hx-target="#skill-tooltip-{skillId}"
  hx-swap="innerHTML"
  aria-describedby="skill-tooltip-{skillId}">
  <span class="level-badge level-{level}">{level}</span>
</button>

<!-- Tooltip popover (content loaded dynamically) -->
<div
  popover="manual"
  id="skill-tooltip-{skillId}"
  role="tooltip"
  class="tooltip-popover">
  <div class="tooltip-loading">Loading...</div>
</div>
```

```css
/* Tooltip styling with viewport constraints */
[popover].tooltip-popover {
  max-width: min(90vw, 500px);
  max-height: 90vh;
  overflow-y: auto;
  padding: 1rem;
  background: var(--pico-card-background-color);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}
```

```javascript
// Vanilla JS for viewport positioning
document.addEventListener('beforetoggle', (e) => {
  if (e.target.matches('[popover].tooltip-popover') && e.newState === 'open') {
    const trigger = document.querySelector(`[popovertarget="${e.target.id}"]`);
    const triggerRect = trigger.getBoundingClientRect();
    const tooltipRect = e.target.getBoundingClientRect();

    // Calculate position (below trigger with viewport boundary detection)
    let top = triggerRect.bottom + 8;
    let left = triggerRect.left;

    // Flip vertically if doesn't fit
    if (top + tooltipRect.height > window.innerHeight - 8) {
      top = triggerRect.top - tooltipRect.height - 8;
    }

    // Shift horizontally to stay in viewport
    if (left + tooltipRect.width > window.innerWidth - 8) {
      left = window.innerWidth - tooltipRect.width - 8;
    }

    e.target.style.top = `${top}px`;
    e.target.style.left = `${left}px`;
  }
});
```

### Server-Side Endpoint

```java
@GET
@Path("/matrix/tooltips/skill/{skillId}")
@Produces(MediaType.TEXT_HTML)
public TemplateInstance getSkillTooltip(
    @PathParam("skillId") Long skillId,
    @QueryParam("level") String level) {

    Skill skill = skillRepository.findById(skillId);
    String description = skill.getDescriptionForLevel(level);

    return Templates.skillTooltip(skill, level, description);
}
```

### Key Features

- **Lazy Loading**: `hx-trigger="mouseenter once"` loads content only when needed, once
- **WCAG Compliance**: 300ms hover delay before showing (prevents accidental triggers)
- **Touch Support**: Tap to open, tap outside to dismiss (native popover behavior)
- **Scrollable**: Long content scrolls within 90vh constraint

---

## 3. Role Grouping and Ordering

### Decision

**Add `role_family` and `seniority_order` fields to database, implement grouping logic in Java**

### Rationale

- **Explicit Intent**: Database field is clearer than string parsing ("Junior Java Dev" → "Java Dev" is fragile)
- **Flexible Business Logic**: Seniority ordering is complex (Junior < Medior < Senior < Lead < Architect); Java Comparators are easier to maintain than SQL CASE expressions
- **Separation of Concerns**: Database handles data storage, Java handles presentation logic
- **Testability**: Java sorting logic is easily unit-testable without database
- **Simplicity First**: Avoids regex parsing and complex SQL; adds two simple fields

### Alternatives Considered

- **String parsing**: Rejected as fragile (breaks with "Lead Developer / Software Architect" or "DevOps Engineer")
- **Pure SQL ORDER BY CASE**: Rejected as complex, hard to read, and buries business logic in SQL
- **Computed column**: Rejected as over-engineering (YAGNI - no performance issues yet)

### Implementation Pattern

#### Database Migration

```sql
-- V2__add_role_grouping.sql
ALTER TABLE rolename ADD COLUMN role_family VARCHAR(50) NOT NULL DEFAULT 'Other';
ALTER TABLE rolename ADD COLUMN seniority_order INTEGER NOT NULL DEFAULT 999;

-- Populate existing data
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
```

#### Updated Role Model

```java
public record Role(
    Integer id,
    String name,
    String description,
    String roleFamily,      // NEW
    Integer seniorityOrder  // NEW
) {
    public Role(String name, String description, String roleFamily, Integer seniorityOrder) {
        this(null, name, description, roleFamily, seniorityOrder);
    }
}
```

#### Java Grouping Logic

```java
// In CompetencyService
public Map<String, List<Role>> getRolesGroupedByFamily() {
    List<Role> allRoles = roleRepository.findAll();

    // Group by family, sort within groups by seniority
    return allRoles.stream()
        .collect(Collectors.groupingBy(
            Role::roleFamily,
            TreeMap::new,  // Sorts families alphabetically
            Collectors.collectingAndThen(
                Collectors.toList(),
                list -> list.stream()
                    .sorted(Comparator.comparingInt(Role::seniorityOrder))
                    .toList()
            )
        ));
}
```

**Result**: `Map<String, List<Role>>` with families as keys (sorted alphabetically), roles within each family sorted by seniority.

### Example Output Structure

```
"Architect" → [Lead Developer / Software Architect, Software Architect, Solution Architect]
"Developer" → [Junior Developer, Medior Developer, Senior Developer, Specialist Developer, Lead Developer]
"Operations" → [DevOps Engineer]
```

### Seed Data Update

Update `competencies.yaml` to include new fields:

```yaml
roles:
  - name: "Junior Developer"
    roleFamily: "Developer"
    seniorityOrder: 1
    description: "..."
  - name: "Medior Developer"
    roleFamily: "Developer"
    seniorityOrder: 2
    description: "..."
```

---

## Summary of Technical Decisions

| Aspect | Technology Choice | Key Benefit |
|--------|------------------|-------------|
| **Table Layout** | HTML `<table>` + CSS `position: sticky` | Native semantics, accessibility, zero JS |
| **Tooltips** | Popover API + htmx + vanilla JS positioning | Built-in accessibility, touch support, lazy loading |
| **Role Grouping** | Database fields + Java Streams | Explicit intent, testable, flexible |
| **Filtering** | htmx `hx-get` with query params | Server-side rendering, no client-side state |
| **Styling** | Pico CSS + custom matrix.css | Consistent with existing app, minimal CSS |

---

## Next Steps (Phase 1)

1. Create data model DTOs (MatrixViewModel, MatrixRow, MatrixCell)
2. Define API contract for `/matrix` endpoint
3. Write quickstart guide for developers
4. Update agent context with new technologies
