# Template Migration: Thymeleaf → Qute

**Date**: 2026-01-21
**Status**: Complete

## Overview

This document provides syntax mappings for migrating Thymeleaf templates to Qute templates. All HTML structure and CSS classes remain unchanged to ensure pixel-perfect rendering (VC-002).

## Syntax Mappings

### 1. Variable Expressions

| Thymeleaf | Qute | Example |
|-----------|------|---------|
| `${variable}` | `{variable}` | Role name: `{role.name}` |
| `${object.property}` | `{object.property}` | Description: `{role.description}` |
| `${object.method()}` | `{object.method}` | Level: `{skill.getDescriptionForLevel(level)}` |

**Example**:
```html
<!-- Thymeleaf -->
<h3 th:text="${role.name}">Role Name</h3>

<!-- Qute -->
<h3>{role.name}</h3>
```

---

### 2. Iteration (th:each → {#for})

| Thymeleaf | Qute |
|-----------|------|
| `th:each="item : ${items}"` | `{#for item in items}...{/for}` |
| `th:each="item, stat : ${items}"` | `{#for item in items}...{item_index}...{/for}` |

**Example**:
```html
<!-- Thymeleaf -->
<a th:each="role : ${roles}"
   th:href="@{/roles/{id}(id=${role.id})}"
   class="role-card">
    <h3 th:text="${role.name}">Role Name</h3>
</a>

<!-- Qute -->
{#for role in roles}
<a href="/roles/{role.id}" class="role-card">
    <h3>{role.name}</h3>
</a>
{/for}
```

**Index Access**:
```html
<!-- Thymeleaf -->
<li th:each="item, stat : ${items}" th:class="${stat.odd} ? 'odd' : 'even'">
    Item #{stat.count}
</li>

<!-- Qute -->
{#for item in items}
<li class="{item_odd ? 'odd' : 'even'}">
    Item #{item_count}
</li>
{/for}
```

*Qute provides implicit variables*:
- `{item_index}` - zero-based index
- `{item_count}` - one-based count
- `{item_odd}` / `{item_even}` - boolean flags
- `{item_hasNext}` - true if not last

---

### 3. Conditionals (th:if → {#if})

| Thymeleaf | Qute |
|-----------|------|
| `th:if="${condition}"` | `{#if condition}...{/if}` |
| `th:unless="${condition}"` | `{#if !condition}...{/if}` or `{#unless condition}...{/unless}` |
| `th:if` + `th:unless` | `{#if}...{#else}...{/if}` |

**Example**:
```html
<!-- Thymeleaf -->
<div th:if="${role.description != null}">
    <p th:text="${role.description}">Description</p>
</div>

<!-- Qute -->
{#if role.description}
<div>
    <p>{role.description}</p>
</div>
{/if}
```

**If/Else**:
```html
<!-- Thymeleaf -->
<span th:if="${skill.level == 'EXCELLENT'}" class="badge excellent">Excellent</span>
<span th:unless="${skill.level == 'EXCELLENT'}" class="badge">{skill.level}</span>

<!-- Qute -->
{#if skill.level == 'EXCELLENT'}
<span class="badge excellent">Excellent</span>
{#else}
<span class="badge">{skill.level}</span>
{/if}
```

---

### 4. URL Construction (th:href → direct URL)

| Thymeleaf | Qute |
|-----------|------|
| `th:href="@{/roles/{id}(id=${role.id})}"` | `href="/roles/{role.id}"` |
| `th:href="@{/compare(from=${from},to=${to})}"` | `href="/compare?from={from}&to={to}"` |
| `th:src="@{/css/style.css}"` | `src="/css/style.css"` |

**Example**:
```html
<!-- Thymeleaf -->
<a th:href="@{/roles/{id}(id=${role.id})}">View Role</a>
<a th:href="@{/compare(from=${fromRole.id},to=${toRole.id})}">Compare</a>

<!-- Qute -->
<a href="/roles/{role.id}">View Role</a>
<a href="/compare?from={fromRole.id}&to={toRole.id}">Compare</a>
```

---

### 5. Layouts & Fragments

**Thymeleaf Layout**:
```html
<!-- layout.html -->
<html xmlns:th="http://www.thymeleaf.org">
<body>
    <div th:fragment="header">Header content</div>
    <div th:replace="${content}">Content placeholder</div>
</body>
</html>

<!-- index.html -->
<html th:replace="~{layout :: layout(content=~{::main-content})}">
<div th:fragment="main-content">
    <h1>Home</h1>
</div>
</html>
```

**Qute Layout**:
```html
<!-- layout.html -->
<html>
<body>
    {#insert header}Default header{/insert}
    {#insert content}Default content{/insert}
</body>
</html>

<!-- index.html -->
{#include layout}
    {#header}
        <div>Header content</div>
    {/header}
    {#content}
        <h1>Home</h1>
    {/content}
{/include}
```

---

### 6. Attributes

| Thymeleaf | Qute |
|-----------|------|
| `th:text="${value}"` | `{value}` (direct output) |
| `th:attr="data-id=${id}"` | `data-id="{id}"` |
| `th:class="${condition} ? 'active' : ''"` | `class="{condition ? 'active' : ''}"` |
| `th:classappend="${condition} ? 'active'"` | `class="base {condition ? 'active' : ''}"` |

**Example**:
```html
<!-- Thymeleaf -->
<div th:class="${active} ? 'card active' : 'card'">

<!-- Qute -->
<div class="card {active ? 'active' : ''}">
```

---

### 7. Form Handling

**Thymeleaf**:
```html
<form th:action="@{/compare}" method="get">
    <select name="from" required>
        <option th:each="role : ${roles}"
                th:value="${role.id}"
                th:text="${role.name}">
        </option>
    </select>
</form>
```

**Qute**:
```html
<form action="/compare" method="get">
    <select name="from" required>
        {#for role in roles}
        <option value="{role.id}">{role.name}</option>
        {/for}
    </select>
</form>
```

---

### 8. Elvis Operator & Null Safety

| Thymeleaf | Qute |
|-----------|------|
| `${value ?: 'default'}` | `{value ?: 'default'}` |
| `${obj?.property}` | `{obj.property}` (null-safe by default) |

**Example**:
```html
<!-- Thymeleaf -->
<h3 th:text="${role.name ?: 'Unknown'}">

<!-- Qute -->
<h3>{role.name ?: 'Unknown'}</h3>
```

---

### 9. htmx Attributes

**No changes required** - htmx attributes are plain HTML:

```html
<!-- Same in both Thymeleaf and Qute -->
<button hx-get="/roles/{role.id}/details"
        hx-target="#details-panel"
        hx-swap="innerHTML">
    Load Details
</button>
```

---

### 10. Template Parameters (Java → Template)

**Spring MVC**:
```java
@GetMapping("/roles/{id}")
public String getRole(@PathVariable Integer id, Model model) {
    model.addAttribute("role", roleService.findById(id));
    model.addAttribute("skills", skillService.findByRoleId(id));
    return "role";
}
```

**Quarkus JAX-RS**:
```java
@Inject Template role;

@GET
@Path("roles/{id}")
@Produces(MediaType.TEXT_HTML)
@Blocking
public TemplateInstance getRole(@PathParam("id") Integer id) {
    return role
        .data("role", roleService.findById(id))
        .data("skills", skillService.findByRoleId(id));
}
```

---

## Complete Template Example

### Thymeleaf (index.html)

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      th:replace="~{layout :: layout(title='Home', content=~{::main-content})}">
<body>
    <div th:fragment="main-content">
        <h1>Career Competency Matrix</h1>

        <div class="roles-grid">
            <a th:each="role : ${roles}"
               th:href="@{/roles/{id}(id=${role.id})}"
               class="role-card">
                <h3 th:text="${role.name}">Role Name</h3>
                <p th:text="${role.description}">Role description</p>
            </a>
        </div>
    </div>
</body>
</html>
```

### Qute (index.html)

```html
{#include layout title="Home"}
    {#content}
        <h1>Career Competency Matrix</h1>

        <div class="roles-grid">
            {#for role in roles}
            <a href="/roles/{role.id}" class="role-card">
                <h3>{role.name}</h3>
                <p>{role.description}</p>
            </a>
            {/for}
        </div>
    {/content}
{/include}
```

---

## Migration Checklist

- [ ] Convert `${...}` → `{...}` for all variables
- [ ] Convert `th:each` → `{#for}...{/for}`
- [ ] Convert `th:if` → `{#if}...{/if}`
- [ ] Convert `th:href="@{...}"` → `href="..."`
- [ ] Convert `th:text` to direct `{value}` output
- [ ] Update layout/fragment system to Qute includes
- [ ] Remove Thymeleaf xmlns declarations
- [ ] Test all templates for identical rendering

---

## Key Differences Summary

| Feature | Thymeleaf | Qute |
|---------|-----------|------|
| **Variable** | `${var}` | `{var}` |
| **Iteration** | `th:each="item : ${items}"` | `{#for item in items}` |
| **Conditional** | `th:if="${cond}"` | `{#if cond}` |
| **URLs** | `@{/path/{id}(id=${val})}` | `/path/{val}` |
| **Layouts** | `th:replace/fragment` | `{#include}/{#insert}` |
| **Null-safe** | `${obj?.prop}` | `{obj.prop}` (always null-safe) |
| **Performance** | Runtime processing | Build-time compilation |
