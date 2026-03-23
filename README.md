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
#   DB_NAME=sysuptimemonitor

# 2. Build and run
docker-compose up --build

# UI available at http://localhost:5173
# API available at http://localhost:9090 (direct, optional)
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

### Email Notifications (Optional)

```bash
# Add SMTP credentials to .env for email notifications:
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=alerts@example.com
SMTP_PASSWORD=app-password
SMTP_FROM=alerts@example.com
```

Email notifications are optional. If `SMTP_HOST` is not set, the app runs normally without sending emails. Users can enable notifications per-user and subscribe to individual monitors.

### Database Management

```bash
scripts/reinit_db.sh history   # Clear incidents, monitor runs, audit logs
scripts/reinit_db.sh all       # Clear ALL data including users and monitors
```

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 8 |
| Web Framework | Jersey 2.39.1 (JAX-RS on Servlet 4.0) |
| Database | PostgreSQL 16 |
| Frontend | React 18 + TypeScript + Vite |
| UI Components | Shadcn/ui + Tailwind CSS v4 |
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
6. **Status Page** — A public status page shows monitors marked as **public** and their uptime for any organization. An organization listing page lets visitors discover all registered organizations.

## Project Structure

```
src/main/java/org/example/systemuptimemonitor/
├── config/         # JerseyConfig (@ApplicationPath("/api"))
├── dao/            # Data access (JDBC + PreparedStatement)
├── exceptions/     # Domain-specific exceptions
├── filter/         # JAX-RS ContainerRequestFilters (@AdminAuth, @OperatorAuth)
├── model/          # POJOs (User, Monitor, Incident, etc.)
├── resources/      # JAX-RS resource classes (Auth, Admin, Monitor, Incident, Public)
├── services/       # Business logic + transaction management
└── util/           # DBManager, TokenManager, MonitorExecutor, SchemaManager

src/main/webapp/
├── index.html      # SPA entry point (built from frontend/)
├── assets/         # Built JS/CSS bundles and fonts
└── WEB-INF/web.xml

frontend/               # React SPA source
├── src/
│   ├── components/     # UI components (GlassCard, NavRail, shadcn/ui)
│   ├── hooks/          # useAuth, useSidebar
│   ├── pages/          # Login, Register, Dashboard, Admin, Status, Invite
│   ├── services/       # API client (Axios)
│   ├── types/          # TypeScript interfaces
│   └── index.css       # Solstice glassmorphic theme
├── package.json
└── vite.config.ts      # Proxy to Tomcat, builds to src/main/webapp/

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

## Frontend Development

```bash
cd frontend
npm install                # Install dependencies
npm run dev                # Start dev server on :5173 (proxies API to :9090)
npm run build              # Production build → src/main/webapp/
npm run lint               # Lint with ESLint
```

The Vite dev server proxies `/api/*` requests to `localhost:9090` (Tomcat), keeping the `/api` prefix intact (Jersey serves at `/api/*`).
