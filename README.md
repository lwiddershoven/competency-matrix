# Career Competency Matrix

Career Competency Matrix is a web application that helps professionals plan their career development by visualizing the skills and proficiency levels required for different roles. Users can browse roles (e.g., Junior Developer, Senior Developer, Staff Engineer), view required competencies organized by category, compare skill requirements between roles, and discover career progression paths—all through a simple, fast, server-rendered interface.

## Prerequisites

- Java 25
- Docker (for PostgreSQL)
- Maven (or use the included `./mvnw` wrapper)

## Build & Run Locally

### 1. Start PostgreSQL

```bash
docker-compose up -d
```

This starts PostgreSQL 18.1 on port 5432 with the database `competencymatrix`.

### 2. Run the Application

```bash
./mvnw quarkus:dev
```

Or build and run the JAR:

```bash
./mvnw clean package -DskipTests
java -jar target/quarkus-app/quarkus-run.jar
```

### With colima on a Mac

To run the testcontainers I had to link the colima docker socket to a place that is known within the
Colima VM so Ryuk can access docker (from within the container).

Check https://github.com/testcontainers/testcontainers-java/issues/6450
```zsh 
docker context ls # will show you the socket
ln -s ~/.colima/default/docker.sock ~/.docker/run/docker.sock
```

### 3. Access the Application

- **Web Application**: http://localhost:8080
- **Health Check**: http://localhost:9000/health
- **Liveness Probe**: http://localhost:9000/health/live
- **Readiness Probe**: http://localhost:9000/health/ready
- **Prometheus Metrics**: http://localhost:9000/metrics

## Running Tests

### Unit and Integration Tests

```bash
./mvnw test
```

Tests use Testcontainers to spin up a PostgreSQL instance automatically.

### End-to-End Tests (Playwright)

The E2E tests require Playwright browsers to be installed:

```bash
# Install Playwright browsers (first time only)
./mvnw exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"

# Run E2E tests
./mvnw test -Dtest='BrowseCompetenciesTest'
```

## Docker Build

Build and run the application in Docker:

```bash
# Build the image
docker build -t competency-matrix .

# Run with docker-compose (includes PostgreSQL)
docker-compose -f docker-compose.yaml up
```

## Features

- **Browse Roles**: View all available roles and their descriptions
- **View Competencies**: See required skills grouped by category for each role
- **Skill Details**: Click on any skill to see proficiency level descriptions (Basic, Decent, Good, Excellent)
- **Compare Roles**: Side-by-side comparison of skill requirements between two roles
- **Career Progression**: Discover which roles you can progress to from your current position
- **Theme Toggle**: Switch between light and dark modes

## Technology Stack

- **Backend**: Java 25, Quarkus 3.30.6, JAX-RS, Panache JDBC
- **Frontend**: Qute Templates, htmx, Pico CSS
- **Database**: PostgreSQL 18.1, Flyway
- **Testing**: JUnit 5, Quarkus Test, Testcontainers, REST Assured, Playwright
- **Observability**: Micrometer, SmallRye Health, Prometheus metrics

### PostgreSQL 18+ Data Directory

PostgreSQL 18 changed the default data directory location:

| Version | Data Directory |
|---------|---------------|
| PostgreSQL 17 and earlier | `/var/lib/postgresql/data` |
| PostgreSQL 18+ | `/var/lib/postgresql` |

The `docker-compose.yaml` in this project uses the new PostgreSQL 18+ location. If you're migrating from an older PostgreSQL version or using a different setup, ensure your volume mounts point to the correct directory.

## Project Structure

```
src/
├── main/
│   ├── java/nl/leonw/competencymatrix/
│   │   ├── config/          # DataSeeder
│   │   ├── resource/        # JAX-RS Resources (HomeResource, RoleResource, CompareResource)
│   │   ├── model/           # Role, Skill, Category, etc.
│   │   ├── repository/      # Repositories with Panache patterns
│   │   └── service/         # CompetencyService
│   └── resources/
│       ├── application.properties
│       ├── db/migration/    # Flyway migrations
│       ├── seed/            # Initial data (split YAML files)
│       │   ├── categories/  # Category files (programming.yaml, etc.)
│       │   ├── roles/       # Role files (junior-developer.yaml, etc.)
│       │   └── progressions.yaml
│       ├── templates/       # Qute templates
│       └── static/css/      # Custom CSS
└── test/
    ├── java/nl/leonw/competencymatrix/
    │   ├── resource/        # REST Assured resource tests
    │   ├── repository/      # Repository tests
    │   ├── service/         # Service tests
    │   ├── validation/      # Migration validation tests
    │   └── e2e/             # Playwright E2E tests
    └── resources/
```

## Data Structure

### Competency Data Files

The competency data is organized in multiple YAML files for maintainability:

```
src/main/resources/seed/
├── categories/                    # One file per competency category
│   ├── programming.yaml           # Programming skills (Java, Python, SQL, etc.)
│   ├── software-design.yaml       # Design Patterns, Architecture, API Design
│   ├── devops-infrastructure.yaml # CI/CD, Kubernetes, Cloud, Docker
│   ├── quality-testing.yaml       # Unit Testing, Integration Testing, etc.
│   ├── soft-skills.yaml           # Communication, Mentoring, Problem Solving
│   └── architecture-frameworks.yaml # TOGAF, ArchiMate, iSAQB
├── roles/                         # One file per role definition
│   ├── junior-developer.yaml
│   ├── medior-developer.yaml
│   ├── senior-developer.yaml
│   ├── specialist-developer.yaml
│   ├── lead-developer.yaml
│   ├── lead-developer-software-architect.yaml
│   ├── software-architect.yaml
│   ├── solution-architect.yaml
│   └── devops-engineer.yaml
└── progressions.yaml              # Career progression paths
```

### Editing Competency Data

Each category file contains skills and their proficiency levels:

```yaml
name: "Programming"
displayOrder: 1
skills:
  - name: "Java"
    levels:
      basic: "Kan basis Java code schrijven..."
      decent: "Schrijft schone code, begrijpt OOP..."
      good: "Ontwikkelt onderhoudbare systemen..."
      excellent: "Beheerst geavanceerde patronen..."
```

Each role file contains requirements and skill levels:

```yaml
name: "Junior Developer"
description: "Entry-level software developer..."
roleFamily: "Development"
seniorityOrder: 1
requirements:
  - skillName: "Java"
    categoryName: "Programming"
    level: "basic"
```

For detailed editing instructions, see `specs/006-competencies-split/quickstart.md`.

## Configuration

Key configuration options in `application.properties`:

| Property | Default | Description |
|----------|---------|-------------|
| `quarkus.http.port` | 8080 | Main application port |
| `quarkus.management.port` | 9000 | Management/metrics port |
| `quarkus.datasource.jdbc.url` | jdbc:postgresql://localhost:5432/competencymatrix | Database URL |
| `quarkus.flyway.migrate-at-start` | true | Run migrations on startup |
