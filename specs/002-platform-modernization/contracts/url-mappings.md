# URL Mappings: Spring MVC → JAX-RS

**Date**: 2026-01-21
**Status**: Complete

## Overview

This document maps Spring MVC controller endpoints to Quarkus JAX-RS resource endpoints. All URLs must remain identical to preserve bookmarks and external links (FR-002).

## Endpoint Mappings

### Home Controller → Home Resource

| Method | Spring MVC URL | JAX-RS URL | Notes |
|--------|---------------|------------|-------|
| GET | `/` | `/` | Home page with role list |
| POST | `/theme` | `/theme` | Toggle light/dark theme |

**Spring MVC**:
```java
@Controller
public class HomeController {
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("roles", competencyService.getAllRoles());
        return "index";
    }

    @PostMapping("/theme")
    public String toggleTheme(@CookieValue("theme") String currentTheme,
                              HttpServletResponse response) {
        // Cookie manipulation
    }
}
```

**JAX-RS (Quarkus)**:
```java
@Path("/")
public class HomeResource {
    @Inject Template index;
    @Inject CompetencyService competencyService;

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Blocking
    public TemplateInstance home() {
        return index.data("roles", competencyService.getAllRoles());
    }

    @POST
    @Path("theme")
    @Produces(MediaType.TEXT_HTML)
    @Blocking
    public Response toggleTheme(@CookieParam("theme") String currentTheme) {
        String newTheme = "dark".equals(currentTheme) ? "light" : "dark";
        NewCookie cookie = new NewCookie.Builder("theme")
            .value(newTheme)
            .path("/")
            .maxAge(60 * 60 * 24 * 365)
            .build();
        return Response.seeOther(URI.create("/")).cookie(cookie).build();
    }
}
```

---

### Role Controller → Role Resource

| Method | Spring MVC URL | JAX-RS URL | Notes |
|--------|---------------|------------|-------|
| GET | `/roles/{id}` | `/roles/{id}` | Role detail page |
| GET | `/roles/{id}/progression` | `/roles/{id}/progression` | Career progression |

**Spring MVC**:
```java
@Controller
@RequestMapping("/roles")
public class RoleController {
    @GetMapping("/{id}")
    public String getRole(@PathVariable Integer id, Model model) {
        model.addAttribute("role", competencyService.getRoleWithSkills(id));
        return "role";
    }

    @GetMapping("/{id}/progression")
    public String getProgression(@PathVariable Integer id, Model model) {
        model.addAttribute("progressions", competencyService.getProgressions(id));
        return "progression";
    }
}
```

**JAX-RS (Quarkus)**:
```java
@Path("/roles")
public class RoleResource {
    @Inject Template role;
    @Inject Template progression;
    @Inject CompetencyService competencyService;

    @GET
    @Path("{id}")
    @Produces(MediaType.TEXT_HTML)
    @Blocking
    public TemplateInstance getRole(@PathParam("id") Integer id) {
        return role.data("role", competencyService.getRoleWithSkills(id));
    }

    @GET
    @Path("{id}/progression")
    @Produces(MediaType.TEXT_HTML)
    @Blocking
    public TemplateInstance getProgression(@PathParam("id") Integer id) {
        return progression.data("progressions", competencyService.getProgressions(id));
    }
}
```

---

### Compare Controller → Compare Resource

| Method | Spring MVC URL | JAX-RS URL | Notes |
|--------|---------------|------------|-------|
| GET | `/compare?from={fromId}&to={toId}` | `/compare?from={fromId}&to={toId}` | Role comparison |

**Spring MVC**:
```java
@Controller
public class CompareController {
    @GetMapping("/compare")
    public String compare(@RequestParam Integer from, @RequestParam Integer to, Model model) {
        model.addAttribute("fromRole", competencyService.getRoleWithSkills(from));
        model.addAttribute("toRole", competencyService.getRoleWithSkills(to));
        return "compare";
    }
}
```

**JAX-RS (Quarkus)**:
```java
@Path("/compare")
public class CompareResource {
    @Inject Template compare;
    @Inject CompetencyService competencyService;

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Blocking
    public TemplateInstance compare(@QueryParam("from") Integer from, @QueryParam("to") Integer to) {
        return compare
            .data("fromRole", competencyService.getRoleWithSkills(from))
            .data("toRole", competencyService.getRoleWithSkills(to));
    }
}
```

---

## Static Resources

| Spring Boot Path | Quarkus Path | Notes |
|-----------------|--------------|-------|
| `/static/css/*` | `/css/*` | CSS files auto-served from `META-INF/resources/css/` |
| `/static/js/*` | `/js/*` | JavaScript files auto-served from `META-INF/resources/js/` |

**Migration**: Move `/src/main/resources/static/` → `/src/main/resources/META-INF/resources/`

---

## Health & Metrics Endpoints

| Spring Boot Actuator | Quarkus Endpoint | Notes |
|---------------------|------------------|-------|
| `/actuator/health` | `/q/health` | Overall health |
| `/actuator/health/liveness` | `/q/health/live` | Liveness probe |
| `/actuator/health/readiness` | `/q/health/ready` | Readiness probe |
| `/actuator/prometheus` | `/q/metrics` | Prometheus metrics |

**URL Compatibility**: To preserve `/actuator/*` URLs, configure:
```properties
quarkus.http.non-application-root-path=/actuator
```

Then endpoints become:
- `/actuator/health`
- `/actuator/health/live`
- `/actuator/health/ready`
- `/actuator/metrics` (Prometheus)

---

## Key Annotation Mappings

| Spring MVC | JAX-RS |
|------------|--------|
| `@Controller` | `@Path("/")` |
| `@GetMapping("/path")` | `@GET @Path("path")` |
| `@PostMapping("/path")` | `@POST @Path("path")` |
| `@PathVariable` | `@PathParam` |
| `@RequestParam` | `@QueryParam` |
| `@CookieValue` | `@CookieParam` |
| `@RequestHeader` | `@HeaderParam` |
| `Model model` | `Template.data("key", value)` |
| `return "templateName";` | `return template.instance();` |

---

## htmx Header Detection

**Spring Boot** (with `spring-boot-htmx-thymeleaf`):
```java
@GetMapping("/roles/{id}")
public String getRole(@PathVariable Integer id,
                      @HxRequest boolean isHtmx,
                      Model model) {
    if (isHtmx) {
        return "role-partial";
    }
    return "role-full";
}
```

**Quarkus** (manual header check):
```java
@GET
@Path("roles/{id}")
@Produces(MediaType.TEXT_HTML)
@Blocking
public TemplateInstance getRole(@PathParam("id") Integer id,
                                 @HeaderParam("HX-Request") String hxRequest) {
    if (hxRequest != null) {
        return rolePartial.data("role", service.getRoleWithSkills(id));
    }
    return roleFull.data("role", service.getRoleWithSkills(id));
}
```

---

## Summary

- **All URLs preserved**: No breaking changes to bookmarks or external links
- **Template return values**: `String` (Spring) → `TemplateInstance` (Quarkus)
- **Model data**: `Model.addAttribute()` → `Template.data()`
- **Path variables/params**: Annotation changes only (`@PathVariable` → `@PathParam`)
- **Cookies**: `HttpServletResponse.addCookie()` → `Response.cookie(NewCookie)`
