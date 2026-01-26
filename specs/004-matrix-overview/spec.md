# Feature Specification: Competency Matrix Table Overview

**Feature Branch**: `004-matrix-overview`
**Created**: 2026-01-26
**Status**: Draft
**Input**: User description: "An overview page is available in which all skills are listed alfabetically underneath each other (e.g. vertically as rows), and all roles are listed horizontally (e.g. as columns) and their level is listed in this table. E.g. the competence matrix is displayed as a matrix/table. When one hoovers over the level the text is shown for that level."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - View Complete Matrix Overview (Priority: P1)

As a professional exploring career options, I want to see a comprehensive matrix showing all skills as rows and all roles as columns so I can quickly compare competency requirements across different roles.

**Why this priority**: This is the core value proposition - providing a bird's-eye view of the entire competency landscape allows users to make informed career decisions at a glance. This is the foundation that all other functionality builds upon.

**Independent Test**: Can be fully tested by navigating to the matrix overview page and verifying that all skills are displayed alphabetically as rows and all roles are displayed as columns with proficiency levels shown in each cell. Delivers immediate value by showing the complete competency landscape.

**Acceptance Scenarios**:

1. **Given** I am on any page in the application, **When** I click the matrix overview navigation link, **Then** I see a table with all skills listed alphabetically as rows and all roles listed as columns
2. **Given** I am viewing the matrix, **When** I look at any cell, **Then** I see the required proficiency level (Basic, Decent, Good, Excellent) for that skill-role combination
3. **Given** I am viewing the matrix, **When** I scan down a column, **Then** I can see all competency requirements for a specific role
4. **Given** I am viewing the matrix, **When** I scan across a row, **Then** I can see how a specific skill's required proficiency varies across different roles

---

### User Story 2 - View Proficiency Details on Hover (Priority: P2)

As a user viewing the matrix, I want to hover over a proficiency level to see the detailed description so I can understand what that level means for that specific skill without leaving the matrix view.

**Why this priority**: After seeing the matrix overview (P1), users need to understand what the proficiency levels actually mean in practical terms. Hover functionality provides this context efficiently without disrupting the overview experience.

**Independent Test**: Can be tested by hovering over any proficiency level badge in the matrix and verifying that a tooltip or popover displays the corresponding skill description for that proficiency level. Delivers value by providing actionable detail without navigation.

**Acceptance Scenarios**:

1. **Given** I am viewing the matrix, **When** I hover over a proficiency level badge (e.g., "Good"), **Then** a tooltip appears showing the skill description for that proficiency level
2. **Given** I am hovering over a proficiency level, **When** the tooltip is displayed, **Then** it shows the skill name, proficiency level name, and the detailed description text
3. **Given** I am hovering over a proficiency level, **When** I move my mouse away, **Then** the tooltip disappears
4. **Given** I am viewing the matrix on a touch device, **When** I tap on a proficiency level, **Then** the tooltip appears and remains visible until I tap elsewhere

---

### User Story 3 - Navigate to Detailed Views (Priority: P3)

As a user who has identified an interesting role or skill in the matrix, I want to click on a role name or skill name to navigate to the detailed view so I can explore that role or skill in more depth.

**Why this priority**: This connects the matrix overview to the existing detailed views from the base feature (001-competency-matrix), creating a cohesive navigation experience. However, the matrix must function independently first.

**Independent Test**: Can be tested by clicking on role names and skill names in the matrix and verifying that the user is navigated to the appropriate detailed view page. Delivers value by enabling deep exploration from the overview.

**Acceptance Scenarios**:

1. **Given** I am viewing the matrix, **When** I click on a role name in the column header, **Then** I am navigated to the detailed view for that role showing all its competency categories and skills
2. **Given** I am viewing the matrix, **When** I click on a skill name in the row header, **Then** I am navigated to a detailed view showing that skill's descriptions across all proficiency levels
3. **Given** I have navigated to a detailed view from the matrix, **When** I want to return to the matrix, **Then** I can use the browser back button or click the matrix overview navigation link

---

### User Story 4 - Filter and Sort Matrix (Priority: P4)

As a user viewing a large matrix, I want to filter skills by category and sort by different criteria so I can focus on the most relevant information for my career planning.

**Why this priority**: This enhances the usability of the matrix when dealing with many skills and roles, but the basic matrix functionality (P1-P3) must work first.

**Independent Test**: Can be tested by applying category filters and sort options and verifying that the matrix updates to show only filtered/sorted content. Delivers value by making large matrices more manageable.

**Acceptance Scenarios**:

1. **Given** I am viewing the matrix, **When** I select a category from the dropdown filter (e.g., "Java"), **Then** the matrix shows only skills within that category
2. **Given** I have filtered by one category, **When** I select a different category from the dropdown, **Then** the matrix updates to show only skills from the newly selected category
3. **Given** I am viewing the matrix with or without filters, **When** I observe the skill order, **Then** skills are displayed in alphabetical order
4. **Given** I am viewing a filtered matrix, **When** I clear the filter (e.g., select "All categories"), **Then** the matrix returns to showing all skills and roles

---

### Edge Cases

- What happens when a skill has no required proficiency for a particular role? Leave the cell blank (empty) to maintain readability.
- What happens when there are many roles and the table is too wide for the viewport? Allow horizontal scrolling with fixed row headers (skill names).
- What happens when there are many skills and the table is very long? Implement sticky column headers (role names) that remain visible when scrolling vertically.
- What happens when a user hovers over an empty cell (no proficiency requirement)? No tooltip is displayed.
- What happens when the tooltip content is very long? Limit tooltip to 90% of viewport width and height with scrolling enabled for overflow content.
- What happens when a skill or role name is very long? Truncate with ellipsis and show full name in tooltip.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST display a table with all skills listed alphabetically as rows
- **FR-002**: System MUST display all roles as columns in the table, grouped by role family and ordered by seniority within each group
- **FR-003**: System MUST show the required proficiency level (Basic, Decent, Good, Excellent) in each cell for the corresponding skill-role combination
- **FR-004**: System MUST retrieve skill, role, and proficiency requirement data from the database
- **FR-005**: System MUST display a tooltip when user hovers over a proficiency level badge showing the skill name, proficiency level, and detailed description text
- **FR-006**: System MUST support touch interactions for tooltips on mobile/touch devices (tap to show, tap elsewhere to hide)
- **FR-007**: System MUST allow users to click on role names to navigate to the role's detailed view
- **FR-008**: System MUST allow users to click on skill names to navigate to the skill's detailed view
- **FR-009**: System MUST leave cells empty when a skill has no proficiency requirement for a particular role (no text or indicator displayed)
- **FR-010**: System MUST provide horizontal scrolling when the table width exceeds the viewport, with fixed skill name column
- **FR-011**: System MUST provide sticky column headers (role names) that remain visible when scrolling vertically
- **FR-012**: System MUST allow users to filter skills by competency category using a single-select dropdown (one category at a time)
- **FR-013**: System MUST display skills in alphabetical order by default
- **FR-014**: System MUST provide a way to clear filters and return to the full matrix view
- **FR-015**: System MUST truncate long skill and role names with ellipsis and show full names in tooltips
- **FR-016**: System MUST limit tooltip size to a maximum of 90% of viewport width and height, enabling scrolling for content that exceeds these dimensions
- **FR-017**: System MUST provide a top-level navigation link to the matrix overview page, visible on all pages in the application

### Key Entities

- **Skill**: A specific competency with a name and descriptions for each proficiency level (Basic, Decent, Good, Excellent). Skills are organized into categories and displayed as rows in the matrix.
- **Role**: A seniority-specific career position that references required skills at specific proficiency levels. Roles are displayed as columns in the matrix.
- **Proficiency Level**: A fixed 4-tier mastery scale (Basic, Decent, Good, Excellent) with skill-specific descriptions for each level.
- **Role-Skill Requirement**: The link between a role and a skill specifying which proficiency level is required. This data populates the matrix cells.
- **Competency Category**: A grouping of related skills used for filtering the matrix view.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can view the complete competency matrix within 2 seconds of page load
- **SC-002**: Users can compare proficiency requirements across all roles for any skill in a single view without scrolling horizontally (for up to 6 roles)
- **SC-003**: Users can view proficiency level descriptions via hover tooltips within 0.5 seconds of hovering
- **SC-004**: 95% of users can successfully identify the required proficiency level for any skill-role combination on first attempt
- **SC-005**: Users can navigate from the matrix overview to detailed role or skill views within 2 clicks
- **SC-006**: The matrix remains usable and responsive with up to 100 skills and 20 roles
- **SC-007**: 90% of users find the matrix overview helpful for comparing career paths (user satisfaction metric)

## Clarifications

### Session 2026-01-26

- Q: How should empty cells (no proficiency requirement) be displayed to balance clarity with readability? → A: Leave them blank. There is plenty of other data on the page and we don't want to reduce readability by displaying an abundance of N/A.
- Q: How should role columns be ordered in the matrix - by seniority across all roles or grouped by role family? → A: Group by role family then order by seniority within each group (Junior Java Dev, Medior Java Dev, Senior Java Dev, Architect, then Junior AI Specialist, etc.)
- Q: Should category filtering allow single or multiple category selection? → A: Single-select dropdown (user can filter by one category at a time)
- Q: What should be the maximum size and overflow handling for tooltips with long content? → A: Responsive maximum: 90% of viewport width/height, with scrolling
- Q: How should users navigate to the matrix overview page within the application? → A: Top-level navigation link visible on all pages (primary navigation item)

## Assumptions

- The matrix overview page is a new page/route in the existing application, accessible via a top-level navigation link visible on all pages
- Proficiency level descriptions are the same as defined in the base feature (001-competency-matrix): skill-specific text for Basic, Decent, Good, Excellent
- The matrix displays all roles and all skills regardless of category, with optional category filtering
- Role ordering in columns is grouped by role family (e.g., all Java Developer roles together, all AI Specialist roles together), with each group ordered by seniority progression (Junior → Medior → Senior → Architect/Lead/Specialist)
- Tooltip display mechanism will use standard web technologies (CSS hover states or JavaScript for touch support)
- The matrix is read-only and does not allow inline editing of proficiency requirements
- Navigation to detailed views reuses existing role and skill detail pages from the base feature
- The application will be accessed via modern web browsers supporting CSS Grid or Flexbox for table layout
- Skills and roles use the existing data model from feature 001-competency-matrix
- All skill-role proficiency requirements are fully defined in the database (no missing data scenarios)
