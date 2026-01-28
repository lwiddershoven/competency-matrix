# Quick Start Guide: Editing Competency Files

**Audience**: Business content editors (non-technical)
**Last Updated**: 2026-01-28

## Overview

Competency data is now split into smaller, manageable files instead of one large 678-line file. This guide shows you how to edit categories and roles.

---

## Finding the Right File

### For Category Edits (Skills and Proficiency Levels)

**Location**: `src/main/resources/seed/categories/`

| Category Name | Filename |
|---------------|----------|
| Programming | `programming.yaml` |
| Software Design | `software-design.yaml` |
| DevOps & Infrastructure | `devops-infrastructure.yaml` |
| Quality & Testing | `quality-testing.yaml` |
| Soft Skills | `soft-skills.yaml` |
| Architecture Frameworks | `architecture-frameworks.yaml` |

**Example**: To edit the Java skill proficiency levels, open `programming.yaml`.

### For Role Edits (Role Requirements and Descriptions)

**Location**: `src/main/resources/seed/roles/`

| Role Name | Filename |
|-----------|----------|
| Junior Developer | `junior-developer.yaml` |
| Medior Developer | `medior-developer.yaml` |
| Senior Developer | `senior-developer.yaml` |
| Specialist Developer | `specialist-developer.yaml` |
| Lead Developer | `lead-developer.yaml` |
| Lead Developer / Software Architect | `lead-developer-software-architect.yaml` |
| Software Architect | `software-architect.yaml` |
| Solution Architect | `solution-architect.yaml` |
| DevOps Engineer | `devops-engineer.yaml` |

**Example**: To change the requirements for a Senior Developer, open `senior-developer.yaml`.

---

## Editing a Category File

### Example: Adding a New Skill to Programming Category

**File**: `categories/programming.yaml`

```yaml
name: "Programming"
displayOrder: 1
skills:
  # Existing skills...

  # Add a new skill here:
  - name: "Kotlin"
    levels:
      basic: "Kan basis Kotlin code schrijven en syntax begrijpen"
      decent: "Schrijft idiomatische Kotlin code, gebruikt extension functions"
      good: "Ontwikkelt productie-ready Kotlin applicaties, begrijpt coroutines"
      excellent: "Architecteert complexe Kotlin systemen, bijdragen aan open source"
```

**Important**:
- Keep indentation consistent (2 spaces per level)
- Include ALL four proficiency levels (basic, decent, good, excellent)
- Use double quotes around text values

### Example: Editing an Existing Skill's Proficiency Levels

**File**: `categories/programming.yaml`

Find the skill you want to edit and update the level descriptions:

```yaml
  - name: "Java"
    levels:
      basic: "Updated basic description here"
      decent: "Updated decent description here"
      good: "Updated good description here"
      excellent: "Updated excellent description here"
```

---

## Editing a Role File

### Example: Adding a Skill Requirement to a Role

**File**: `roles/junior-developer.yaml`

```yaml
name: "Junior Developer"
description: "Entry-level software developer learning foundational skills"
roleFamily: "Development"
seniorityOrder: 1
requirements:
  # Existing requirements...

  # Add a new requirement:
  - skillName: "Docker"
    categoryName: "DevOps & Infrastructure"
    level: "basic"
```

**Important**:
- `skillName` must match a skill name in one of the category files
- `categoryName` must match a category name (e.g., "Programming", "DevOps & Infrastructure")
- `level` must be one of: basic, decent, good, excellent

### Example: Changing a Requirement's Proficiency Level

**File**: `roles/senior-developer.yaml`

Find the requirement and change the `level` value:

```yaml
requirements:
  - skillName: "Java"
    categoryName: "Programming"
    level: "excellent"  # Changed from "good" to "excellent"
```

---

## YAML Syntax Reminders

### Indentation (CRITICAL)
- Use 2 spaces per indentation level
- DO NOT use tabs (your editor should convert tabs to spaces)
- Keep indentation consistent throughout the file

**Good Example**:
```yaml
name: "Programming"
skills:
  - name: "Java"
    levels:
      basic: "Description"
```

**Bad Example** (inconsistent indentation):
```yaml
name: "Programming"
skills:
  - name: "Java"
      levels:
    basic: "Description"
```

### Quotes
- Use double quotes for text values containing special characters or colons
- Simple text without special characters can be unquoted, but quotes are safer

**Safe Approach**:
```yaml
basic: "This is safe with quotes"
decent: "Also safe: with colons and commas"
```

### Lists
- Lists start with a hyphen and space (`- `)
- Items must be indented consistently

```yaml
skills:
  - name: "Skill 1"
    levels: {...}
  - name: "Skill 2"
    levels: {...}
```

### Common Mistakes to Avoid

❌ **Mixing tabs and spaces**
- Stick to 2 spaces everywhere

❌ **Missing proficiency levels**
- All four levels (basic, decent, good, excellent) are required

❌ **Typos in skill or category names**
- Role requirements must reference exact skill and category names
- Check spelling and capitalization

❌ **Duplicate category or role names**
- Each category name must be unique across all category files
- Each role name must be unique across all role files

---

## Testing Your Changes

### 1. Save Your File
After editing, save the file in your text editor.

### 2. Restart the Application
The application reads these files on startup, so you need to restart it to see changes.

```bash
# From the project root directory:
./mvnw quarkus:dev
```

### 3. Check for Errors
Watch the application startup logs. If there are YAML syntax errors, you'll see:

```
ERROR: Failed to parse programming.yaml at line 42: mapping values are not allowed here
  File: src/main/resources/seed/categories/programming.yaml
```

**What to do**:
- Open the file mentioned in the error
- Go to the line number indicated
- Check indentation, quotes, and syntax around that line
- Fix the error and restart again

### 4. Verify Changes in the UI
Once the application starts successfully:
- Navigate to the competency matrix view
- Verify your changes appear correctly
- Check that skills show the updated proficiency levels
- Confirm roles have the correct requirements

---

## Quick Reference Card

| Task | File Location | What to Edit |
|------|---------------|--------------|
| Add/edit a skill | `categories/{category-name}.yaml` | Add to `skills:` list or edit existing skill |
| Change proficiency levels | `categories/{category-name}.yaml` | Update `levels:` under the skill |
| Add a skill to a role | `roles/{role-name}.yaml` | Add to `requirements:` list |
| Change role requirements | `roles/{role-name}.yaml` | Update `level:` for a requirement |
| Edit role description | `roles/{role-name}.yaml` | Update `description:` field |

---

## Getting Help

**Syntax Errors**: If you see an error about "mapping values" or "unexpected character", it's usually an indentation or quote issue. Check:
1. Indentation is 2 spaces (not tabs)
2. Text values with colons are in quotes
3. Lists start with `- ` (hyphen + space)

**Validation Errors**: If the application starts but shows a validation error, it means:
- A required field is missing (e.g., missing a proficiency level)
- A skill/category reference doesn't exist (typo in a requirement)
- Duplicate names detected

**File Not Found**: If you can't find a file:
- Check the filename matches the transformation rule (lowercase, hyphens)
- Example: "DevOps & Infrastructure" → `devops-infrastructure.yaml`
- Look in the correct directory (`categories/` vs `roles/`)

**Need Technical Support**: Contact the development team with:
- Which file you were editing
- What change you were trying to make
- The exact error message you received

---

## Appendix: File Naming Reference

**Transformation Rule**: Display Name → Filename

1. Convert to lowercase
2. Replace spaces with hyphens
3. Remove special characters (& / etc.)
4. Add `.yaml` extension

**Examples**:
- "Software Design" → `software-design.yaml`
- "Quality & Testing" → `quality-testing.yaml`
- "Lead Developer / Software Architect" → `lead-developer-software-architect.yaml`
