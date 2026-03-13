# OpenSenseMap Edu

An interactive web-based SQL learning tool that uses real-world environmental sensor data from [openSenseMap](https://opensensemap.org). Built as part of a Master's thesis in Geoinformatics and Spatial Data Science at the University of Münster, extending the NRW DatabaseConnector framework used in German schools.

Students learn SQL through narrative-driven scenarios where they explore real sensor networks — querying device locations, filtering by exposure types, joining sensor data with measurements, and analyzing air quality patterns — all on a live-synced dataset from the openSenseMap platform.

---

## Features

### SQL Learning Environment
- **Story-based scenarios** with progressive tasks that guide students from basic SELECT queries to JOINs, GROUP BY, and subqueries
- **Integrated SQL editor** powered by CodeMirror with syntax highlighting, auto-completion, and Ctrl+Enter execution
- **Automatic query validation** that compares student results against sample solutions (row count, column count, data values)
- **Instant feedback** with targeted hints when queries are incorrect
- **SQL Playground** for free-form practice outside of guided scenarios

### Geographic Visualization
- **Interactive map view** with Leaflet.js that automatically detects latitude/longitude columns in query results
- **Custom senseBox markers** color-coded by exposure type (outdoor, indoor, mobile)
- **Marker clustering** for large result sets with animated drop-in effects
- **Styled popups** showing all query result fields for each device
- **Auto-centering** that fits the map to query results

### Progress Tracking
- **User authentication** with role-based access (Student, Teacher, Admin)
- **Per-task completion tracking** with green checkmarks in the sidebar
- **Next task navigation** that prompts students to continue after completing a task
- **Scenario-level progress bars** showing completion percentage on the scenarios list page
- **Completion badges** and status indicators (Not Started → In Progress → Completed)

### Administration
- **Admin dashboard** for managing users, scenarios, and tasks
- **Scenario & task editor** for creating new learning content
- **Student progress monitoring** across all scenarios

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 3.x |
| Security | Spring Security with role-based access |
| Database | PostgreSQL 17 with PostGIS |
| ORM | Spring Data JPA / Hibernate |
| Frontend | Thymeleaf, Bootstrap 5, CodeMirror |
| Maps | Leaflet.js, Leaflet.MarkerCluster |
| Data Source | openSenseMap Production Database (daily sync) |

---

## Database Architecture

The application uses a single PostgreSQL database combining two schemas:

**openSenseMap tables** (synced from production):
- `device` — senseBox stations with location, exposure type, model info
- `sensor` — individual sensors attached to devices
- `measurement` — time-series sensor readings (last 24h synced daily)
- `location`, `device_to_location` — device position history

**Educational tables** (application-managed):
- `edu_user` — students, teachers, admins with authentication
- `scenario` — learning scenarios with stories, objectives, difficulty levels
- `task` — individual SQL tasks with sample solutions and hints
- `query_submission` — student query attempts with correctness tracking
- `user_progress` — per-scenario completion and scoring

---

## Getting Started

### Prerequisites
- Java 21+
- Maven 3.8+
- PostgreSQL 17+
- A local database named `opensensemap_edu`

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/JerryVincent/opensensemap-edu.git
   cd opensensemap-edu
   ```

2. **Configure the database** in `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/opensensemap_edu
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   ```

3. **Run database migrations** — Flyway migrations in `src/main/resources/db/migration/` will auto-run on startup

4. **Build and run**
   ```bash
   mvn spring-boot:run
   ```

5. **Access the application** at `http://localhost:8080`

### Default Accounts
| Username | Password | Role |
|----------|----------|------|
| admin | admin123 | ADMIN |
| student | student123 | STUDENT |

---

## Data Sync

The application syncs real data from the openSenseMap production database daily via a PowerShell script:

- **Small tables** (user, profile, device, sensor): full sync
- **Measurements**: last 24 hours only (~2.6M rows)
- **Locations**: latest position per device only

Sync scripts are located in `C:\osm_sync\` (not committed — they contain database credentials). See `SYNC_SETUP.md` for configuration details.

---

## Project Structure

```
src/main/
├── java/com/opensensemap/edu/
│   ├── controller/          # Web + REST controllers
│   │   ├── HomeController   # Pages: home, scenarios, workspace, playground
│   │   ├── QueryController  # REST API for query execution
│   │   └── AdminController  # Admin dashboard
│   ├── model/entity/        # JPA entities
│   ├── repository/          # Spring Data JPA repositories
│   ├── service/             # Business logic
│   │   ├── QueryExecutionService  # SQL execution + validation
│   │   └── UserService            # Authentication + user management
│   └── config/              # Security, database config
├── resources/
│   ├── templates/           # Thymeleaf HTML templates
│   │   ├── task/workspace.html    # SQL editor + map + results
│   │   ├── scenarios/list.html    # Scenario browser with progress
│   │   └── playground.html        # Free-form SQL playground
│   ├── db/migration/        # Flyway SQL migrations
│   └── application.properties
```

---

## Learning Scenarios

The application ships with three scenarios:

1. **Getting Started with SQL** (Beginner) — SELECT, column selection, WHERE filtering
2. **Air Quality Detective** (Intermediate) — LIKE patterns, JOINs, time-based filtering
3. **Sensor Network Analyst** (Advanced) — COUNT, GROUP BY, AVG, subqueries, ORDER BY

Each scenario contains 3 tasks that progressively build SQL skills using real sensor data.

---

## Screenshots

*Coming soon*

---

## Thesis Context

This project is part of a Master's thesis at the [Institute for Geoinformatics (ifgi)](https://www.uni-muenster.de/Geoinformatics/), University of Münster. The research investigates how real-world spatial data can enhance database education, with a focus on student motivation measured via the SELLMO instrument. A user study with school students in Münster evaluates the tool's effectiveness.

---

## License

This project is developed as academic work. Contact the author for usage permissions.

---

## Acknowledgments

- [openSenseMap](https://opensensemap.org) by [re:edu / Reedu GmbH & Co. KG](https://reedu.de/) for the open environmental sensor data platform
- [ifgi, University of Münster](https://www.uni-muenster.de/Geoinformatics/) for academic supervision
- NRW DatabaseConnector framework for the educational foundation
