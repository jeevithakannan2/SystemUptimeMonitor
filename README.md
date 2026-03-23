# SystemUptimeMonitor

A multi-tenant HTTP endpoint uptime monitoring application. Organizations can register monitors for their web services, track uptime, and manage incidents — all scoped to their own isolated data.

## Quick Start

### Docker (Recommended)

```bash
# 1. Configure environment
cp .env.example .env
# Edit .env with your database credentials:
#   DB_USERNAME=postgres
#   DB_PASSWORD=secret
#   DB_HOST=localhost
#   DB_PORT=5432
#   DB_NAME=sysuptimemonitor

# 2. Build and run
./mvnw clean package
docker-compose up --build

# App available at http://localhost:8080
```

### Manual Setup

Requires: Java 8+, PostgreSQL 16, Apache Tomcat 9

```bash
# 1. Create PostgreSQL database
createdb sysuptimemonitor

# 2. Configure environment
cp .env.example .env
# Edit .env with your database credentials

# 3. Build
./mvnw clean package

# 4. Deploy WAR to Tomcat
cp target/SystemUptimeMonitor-1.0-SNAPSHOT.war $CATALINA_HOME/webapps/ROOT.war

# 5. Start Tomcat (ensure .env is in Tomcat's working directory, or set env vars)
$CATALINA_HOME/bin/startup.sh
```

### Database Management

```bash
scripts/reinit_db.sh history   # Clear incidents, monitor runs, audit logs
scripts/reinit_db.sh all       # Clear ALL data including users and monitors
```

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 8 |
| Web Framework | Java Servlet 4.0 (no Spring) |
| Database | PostgreSQL 16 |
| Password Hashing | BCrypt (jbcrypt) |
| Build Tool | Maven (via Maven Wrapper) |
| App Server | Apache Tomcat 9 |
| Containerization | Docker + Docker Compose |
| Tests | JUnit 5 |

## How It Works

1. **Register** — The first user from an email domain (e.g., `@example.com`) creates the organization and becomes its admin.
2. **Invite** — Admins generate time-limited invite links to onboard operators and viewers.
3. **Monitor** — Operators create HTTP monitors with target URLs, expected status codes, and check intervals.
4. **Auto-check** — A background scheduler periodically pings each monitor's URL and records results.
5. **Incidents** — After a configurable number of consecutive failures, an incident is automatically created. It auto-resolves when the service recovers.
6. **Status Page** — A public endpoint shows all monitors and their uptime for any organization.

## Project Structure

```
src/main/java/org/example/systemuptimemonitor/
├── dao/            # Data access (JDBC + PreparedStatement)
├── exceptions/     # Domain-specific exceptions
├── filter/         # Authentication filters (@WebFilter)
├── model/          # POJOs (User, Monitor, Incident, etc.)
├── services/       # Business logic + transaction management
├── servlets/       # HTTP endpoints (@WebServlet)
└── util/           # DBManager, TokenManager, MonitorExecutor, SchemaManager

src/main/webapp/
├── *.html          # Frontend pages (login, dashboard, admin, status)
├── css/style.css   # Styles
├── js/app.js       # API client and session management
└── WEB-INF/web.xml

scripts/            # Database management scripts
```

## Documentation

- **[API Reference](docs/API.md)** — All endpoints with parameters, responses, and error codes
- **[Architecture](docs/ARCHITECTURE.md)** — Multi-tenancy, request lifecycle, background execution
- **[Data Models](docs/MODELS.md)** — Java models, database schema, entity relationships

## Build Commands

```bash
./mvnw clean package                      # Build WAR
./mvnw test                               # Run all tests
./mvnw test -Dtest=ClassName              # Run a single test class
./mvnw test -Dtest=ClassName#methodName   # Run a single test method
```
