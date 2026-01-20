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
./mvnw spring-boot:run
```

Or build and run the JAR:

```bash
./mvnw clean package -DskipTests
java -jar target/competency-matrix-0.0.1-SNAPSHOT.jar
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
- **Health Check**: http://localhost:9000/actuator/health
- **Prometheus Metrics**: http://localhost:9000/actuator/prometheus

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
./mvnw test -Dtest=*E2E*
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

- **Backend**: Java 25, Spring Boot 4, Spring Data JDBC
- **Frontend**: Thymeleaf, htmx, Pico CSS
- **Database**: PostgreSQL 18.1, Liquibase
- **Testing**: JUnit Jupiter, Testcontainers, Playwright

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
│   │   ├── CompetencyMatrixApplication.java
│   │   ├── config/          # DataSeeder
│   │   ├── controller/      # HomeController, RoleController, CompareController
│   │   ├── model/           # Role, Skill, Category, etc.
│   │   ├── repository/      # Spring Data JDBC repositories
│   │   └── service/         # CompetencyService
│   └── resources/
│       ├── application.yaml
│       ├── db/changelog/    # Liquibase migrations
│       ├── seed/            # Initial data (competencies.yaml)
│       ├── templates/       # Thymeleaf templates
│       └── static/css/      # Custom CSS
└── test/
    ├── java/nl/leonw/competencymatrix/
    │   ├── controller/      # Controller tests
    │   ├── repository/      # Repository tests
    │   ├── service/         # Service tests
    │   └── e2e/             # Playwright E2E tests
    └── resources/
```

## Configuration

Key configuration options in `application.yaml`:

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | 8080 | Main application port |
| `management.server.port` | 9000 | Actuator/metrics port |
| `spring.datasource.url` | jdbc:postgresql://localhost:5432/competencymatrix | Database URL |
