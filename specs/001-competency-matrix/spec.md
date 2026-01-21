# Feature Specification: Career Competency Matrix

**Feature Branch**: `001-competency-matrix`
**Created**: 2026-01-19
**Status**: Draft
**Input**: User description: "I want to create a web app in which I can easily see what is expected of me in my role as Junior, medior or senior Java dev or AI specialist or other role. The use-case is the planning of my career. By selecting the next step in the role I can see the categories (Java, Spring, Presentation skills, Consultancy skills, Devops skills) and within these skills the core skill and some texts on what is expected of me in each of the seniority levels junior, medior, senior, lead dev, architect or specialist. The required information is provided in a database and the initial version is not personalized."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Browse Competencies by Role (Priority: P1)

As a professional planning my career, I want to select a role (e.g., Java Developer, AI Specialist) so I can see all competency categories and skills expected for that role across different seniority levels.

**Why this priority**: This is the core value proposition - users need to first see what competencies exist for their chosen career path before they can plan their development.

**Independent Test**: Can be fully tested by selecting any role and verifying that all competency categories with their skills are displayed. Delivers immediate value by showing the complete competency landscape.

**Acceptance Scenarios**:

1. **Given** I am on the main page, **When** I select "Java Developer" as my role, **Then** I see all competency categories (Java, Spring, Presentation skills, Consultancy skills, DevOps skills) with their associated skills listed
2. **Given** I have selected a role, **When** I view the competency matrix, **Then** each category shows all core skills within that category
3. **Given** I am viewing competencies, **When** I look at any skill, **Then** I can see descriptions of what is expected at each seniority level (Junior, Medior, Senior, Lead Dev, Architect, Specialist)

---

### User Story 2 - View Competency Details by Seniority Level (Priority: P2)

As a professional at a specific seniority level, I want to see detailed expectations for each skill at my current level and the next level so I can identify gaps and plan my growth.

**Why this priority**: After seeing the competency landscape (P1), users need to drill down into specific skills to understand exactly what is expected at each level.

**Independent Test**: Can be tested by selecting any skill and verifying that detailed expectations are displayed for all seniority levels. Delivers value by providing actionable growth targets.

**Acceptance Scenarios**:

1. **Given** I have selected a role and category, **When** I click on a specific skill (e.g., "Spring Boot"), **Then** I see the expectations text for Junior, Medior, Senior, Lead Dev, Architect, and Specialist levels
2. **Given** I am viewing a skill's details, **When** I compare two adjacent seniority levels, **Then** I can clearly see the progression and additional expectations at the higher level
3. **Given** I am viewing skill expectations, **When** I read the description for any level, **Then** the text clearly describes what proficiency or knowledge is expected

---

### User Story 3 - Compare Seniority Levels Side-by-Side (Priority: P3)

As a professional planning a career step, I want to compare what is expected at my current seniority level versus the next level so I can identify specific skills to develop.

**Why this priority**: This enhances the core browsing experience by making gap analysis easier, but the basic viewing functionality (P1, P2) must work first.

**Independent Test**: Can be tested by selecting any two seniority levels and verifying that expectations are displayed side-by-side for easy comparison. Delivers value by simplifying career planning decisions.

**Acceptance Scenarios**:

1. **Given** I am viewing a role's competency matrix, **When** I select my current level (e.g., "Medior") and target level (e.g., "Senior"), **Then** I see a comparison view highlighting differences in expectations
2. **Given** I am in comparison mode, **When** I look at any skill category, **Then** I can see both levels' expectations displayed in a way that makes differences clear
3. **Given** I am viewing a role comparison table, **When** I click on a skill name or proficiency level badge (e.g., "Decent", "Good"), **Then** a modal opens displaying the full skill description and all proficiency level expectations for that skill

---

### User Story 4 - Navigate Between Different Roles (Priority: P4)

As a professional considering different career paths, I want to easily switch between roles (e.g., from Java Developer to AI Specialist) so I can compare what different paths require.

**Why this priority**: Role switching extends the core functionality and allows exploration of alternative career paths.

**Independent Test**: Can be tested by switching between roles and verifying that the competency matrix updates accordingly with role-specific categories and skills.

**Acceptance Scenarios**:

1. **Given** I am viewing the Java Developer competency matrix, **When** I select "AI Specialist" from the role selector, **Then** the view updates to show AI Specialist competencies and categories
2. **Given** I switch roles, **When** I view the new role's matrix, **Then** I see categories and skills specific to that role (which may differ from the previous role)

---

### Edge Cases

- What happens when a role has no competencies defined yet? Display a message indicating "No competencies have been defined for this role yet."
- What happens when a skill has no description for a particular seniority level? Display "No specific requirements defined" for that level.
- What happens when a user accesses the app without selecting a role? Show a landing page prompting them to select a role to begin.
- How does the system handle roles with different applicable seniority levels? Display only the seniority levels that apply to that role.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST display a list of available roles (e.g., Java Developer, AI Specialist, and other configured roles)
- **FR-002**: System MUST allow users to select a role to view its competency matrix
- **FR-003**: System MUST display competency categories for the selected role (e.g., Java, Spring, Presentation skills, Consultancy skills, DevOps skills)
- **FR-004**: System MUST display core skills within each competency category
- **FR-005**: System MUST display skill descriptions for each proficiency level (Basic, Decent, Good, Excellent) showing what that level means for each skill
- **FR-006**: System MUST retrieve all role, category, skill, and expectation data from a database
- **FR-007**: System MUST allow users to switch between different roles without losing navigation context within the app
- **FR-008**: System MUST provide a clear visual hierarchy showing Role > Category > Skill > Required Proficiency Level
- **FR-009**: System MUST display meaningful messages when data is not available (no competencies for role, no description for level)
- **FR-010**: System MUST allow users to compare skill requirements between two roles
- **FR-011**: System MUST display "next roles" links on each role's page showing possible career progression paths (e.g., Junior Java Dev → Medior Java Dev; Senior Java Dev → Architect, Lead Dev, or Specialist)
- **FR-012**: System MUST allow users to click on skill names and proficiency level badges in the role comparison view to open a modal displaying detailed skill descriptions and proficiency level expectations

### Key Entities

- **Role**: A seniority-specific career position (e.g., "Junior Java Dev", "Senior AI Specialist", "Architect"). The seniority is embedded in the role name. A role references required skills at specific proficiency levels and links to possible "next roles" in the career progression. Roles follow a hierarchy: Junior → Medior → Senior → (Architect | Lead Dev | Specialist).
- **Competency Category**: A grouping of related skills (e.g., Java, Spring, DevOps skills, Consultancy skills). Categories organize skills for display purposes.
- **Skill**: A specific competency within a category (e.g., "Spring Boot", "Communication"). Contains a name and four descriptions—one for each proficiency level (Basic, Decent, Good, Excellent)—explaining what that level means for this specific skill. Skill descriptions are shared across all roles (not role-specific).
- **Proficiency Level**: A fixed 4-tier mastery scale: Basic → Decent → Good → Excellent. Each skill defines what these levels mean in its context.
- **Role-Skill Requirement**: The link between a role and a skill, specifying which proficiency level is required (e.g., Architect requires "excellent" consultancy skills, Specialist requires "decent").

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can find and view expectations for any skill within 3 clicks from the landing page (select role, select category/skill, view expectations)
- **SC-002**: Users can identify the differences between their current level and target level within 30 seconds of viewing a skill
- **SC-003**: System displays all competency data for a selected role within 2 seconds of selection
- **SC-004**: 90% of users can successfully navigate to view skill expectations on their first attempt without guidance
- **SC-005**: Users can switch between roles and view updated competencies in under 3 seconds
- **SC-006**: System accurately displays all database-stored competency information without data loss or corruption

## Clarifications

### Session 2026-01-19

- Q: How does initial competency data enter the system? → A: YAML seed files loaded on startup
- Q: How do skills relate to roles? → A: Skills have level-based descriptions that are shared across roles. Roles are seniority-specific (e.g., "Junior Java Dev", "Senior Java Dev"). A role references required skills at specific proficiency levels. Role names embed seniority for now; future versions may separate role and level.
- Q: What proficiency levels exist for skills? → A: 4-level scale: Basic, Decent, Good, Excellent. Each skill has a description for each level explaining what that proficiency means for that specific skill.
- Q: How should role progression be displayed? → A: Show "next roles" links on each role's page to guide career planning.
- Q: Is this for a specific organization or generic? → A: Specific organization's career framework (single tenant).

## Assumptions

- Single-tenant application for a specific organization's career framework (no multi-tenancy)
- The initial version is read-only and does not require user authentication or personalization
- Competency data (roles, categories, skills, proficiency descriptions) will be pre-populated via YAML seed files loaded on application startup
- Proficiency levels are fixed: Basic, Decent, Good, Excellent
- Role names embed seniority (e.g., "Junior Java Dev", "Senior Java Dev") rather than separating role and level
- Skill proficiency descriptions are shared across all roles (not role-specific)
- The application will be accessed via modern web browsers (desktop and mobile)
