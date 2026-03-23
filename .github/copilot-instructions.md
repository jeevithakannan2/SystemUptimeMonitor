# Copilot Instructions — SystemUptimeMonitor

## Architecture

Java Servlet 4.0 WAR application (Java 8) for HTTP endpoint uptime monitoring. Three-tier layered architecture:

```
Filters (auth) → Servlets (one per endpoint) → Services (connection + tx) → DAOs (JDBC) → PostgreSQL
```

- **Servlets** (`servlets/`): One class per HTTP endpoint, annotated with `@WebServlet`. Read input via `req.getParameter()` (GET/POST) or `RequestBodyParser.parse()` (PUT/DELETE). Delegate to services, write responses via `PrintWriter`.
- **Services** (`services/`): Own the JDBC `Connection` lifecycle. Open connections via `DBManager.getConnection(organization)` (org-scoped) or `DBManager.getConnection()` (public schema). Manage transactions (`setAutoCommit(false)` / `commit()` / `rollback()`), call DAOs. Instantiate DAOs as `static final` fields.
- **DAOs** (`dao/`): Receive `Connection` as a method parameter. Use `PreparedStatement` for all SQL. Map `ResultSet` to model POJOs. Use **unqualified table names** — schema resolution is handled by the connection's `search_path`.
- **Models** (`model/`): Plain POJOs with getters/setters, no annotations or Lombok.
- **Filters** (`filter/`): `@WebFilter`-based role checks. `AdminAuthenticationFilter` guards admin-only routes; `OperatorAuthenticationFilter` guards operator routes (allows both `operator` and `admin` roles).

## Multi-Tenancy via PostgreSQL Schemas

Each organization gets an isolated PostgreSQL schema. This is the core data isolation mechanism:

- **Public schema:** Contains only the `organizations` registry table.
- **Per-org schemas** (e.g., `org_example_com`): Contain all org-scoped tables (users, monitors, incidents, invites, etc.). Created by `SchemaManager.createOrgSchema()` when the first user registers for a new org.
- **Schema naming:** `SchemaManager.toSchemaName(organization)` converts org names (e.g., `"example.com"`) → schema names (e.g., `"org_example_com"`) by lowercasing and replacing non-alphanumeric chars with underscores, prefixed with `"org_"`.
- **Connection routing:** `DBManager.getConnection(organization)` returns a connection with `search_path` set to `"org_schema_name", public` — so all unqualified table names in DAOs resolve to the correct org schema.

When adding new tables or queries, **never hardcode schema names** in DAOs. Always use unqualified table names and let the connection's `search_path` handle routing.

## Authentication & Roles

- Token-based auth using in-memory `ConcurrentHashMap<String, User>` in `util/TokenManager`.
- Tokens are UUIDs stored in HTTP-only secure cookies named `"token"`, with lazy expiration after 1 hour.
- Three roles: `admin`, `operator`, `viewer`.
- `AdminAuthenticationFilter` requires `role == "admin"`.
- `OperatorAuthenticationFilter` allows `role == "operator" || role == "admin"`.
- Filters set `req.getSession().setAttribute("user", user)` for downstream servlets.

## Servlet URL Map

| Pattern | Servlet | Method | Auth |
|---------|---------|--------|------|
| `/login` | `Login` | GET | None |
| `/register` | `RegisterUser` | POST | None |
| `/create` | `CreateUser` | POST | None (invite link) |
| `/generate_invitelink` | `GenerateInviteLink` | GET | Admin |
| `/delete_user` | `DeleteUser` | DELETE | Admin |
| `/create_monitor` | `CreateMonitor` | POST | Operator |
| `/delete_monitor` | `DeleteMonitor` | DELETE | Operator |
| `/update_monitor` | `UpdateMonitor` | PUT | Operator |
| `/monitors` | `GetMonitors` | GET | Operator |
| `/monitor_history` | `GetMonitorHistory` | GET | Operator |
| `/incidents` | `GetIncidents` | GET | Operator |
| `/create_incident` | `CreateIncident` | POST | Operator |
| `/resolve_incident` | `ResolveIncident` | PUT | Operator |
| `/status` | `ViewAll` | GET | None (public) |

## Database & Connection Patterns

- PostgreSQL, with connection URL built from individual env vars: `DB_HOST`, `DB_PORT` (default 5432), `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`.
- Credentials loaded from `.env` file (or environment variables) by `util/DBManager`.
- **Always** obtain connections via `DBManager.getConnection(organization)` for org-scoped operations or `DBManager.getConnection()` for public schema — never use `DriverManager.getConnection()` directly.
- `.env` is gitignored; copy `.env.example` to `.env` and fill in credentials for local development.
- All SQL uses `PreparedStatement` with `?` placeholders.
- Transaction pattern in services:
  ```java
  connection.setAutoCommit(false);
  // DAO calls...
  connection.commit();
  // catch: rollback(); finally: setAutoCommit(true);
  ```

## JSON I/O Convention

- **Input:** `req.getParameter()` for GET/POST. For PUT/DELETE, use `RequestBodyParser.parse(req)` which reads and URL-decodes the request body (Tomcat doesn't auto-parse these methods).
- **Output:** Manual `PrintWriter` string concatenation — `jackson-core` is in `pom.xml` but unused (`jackson-databind` is not included, so `ObjectMapper` is not available).
- **Errors:** Use `ErrorResponse.sendJsonError(resp, statusCode, message)` for standardized JSON error responses.

## Background Monitor Execution

`util/MonitorExecutor` implements `ServletContextListener` (`@WebListener`). On startup, calls `DBManager.initSchema()`, loads all enabled monitors, and schedules `ScheduledExecutorService` fixed-rate tasks. Each job:
1. HTTP GET to `monitor.getTargetUrl()` with 5s timeout.
2. Records `MonitorRun` (status code, response time, success).
3. On unexpected status: decrements `AtomicInteger` fail counter → creates `Incident` when counter reaches 0, then resets counter.
4. On expected status: auto-resolves the last unresolved incident.

Keys in the internal maps use `"organization:monitorId"` format to avoid ID collisions across orgs. Dynamic add/remove via `addMonitor()`/`removeMonitor()` called from `MonitorService`.

## Key Domain Flows

- **Registration:** First user for an org (derived from email domain via `split("@")[1]`) becomes `admin` and triggers per-org schema creation. Subsequent users join via invite links (role set by inviter).
- **Invite links** expire after 30 seconds and are single-use.
- **Organization scoping:** Monitors and incidents are scoped to an organization. `ViewAll` (`/status`) is the exception — it's public and shows all orgs.

## Known Issues

- `MonitorService.hasUnresolvedIncident()` has inverted logic — returns `true` when there are **no** unresolved incidents (`incident == null`).
- JSON output in some servlets has trailing commas and inconsistent timestamp quoting.

## Build & Run

```bash
./mvnw clean package                    # Build WAR
./mvnw test                             # Run all tests
./mvnw test -Dtest=ClassName            # Run a single test class
./mvnw test -Dtest=ClassName#methodName # Run a single test method

# Docker (reads .env for DB credentials):
docker-compose up --build               # Start app + PostgreSQL

# Database management:
scripts/reinit_db.sh history            # Clear incidents, monitor runs, audit logs
scripts/reinit_db.sh all                # Clear ALL data including users and monitors
```

Deploy `target/SystemUptimeMonitor-1.0-SNAPSHOT.war` to Tomcat 9. Place `.env` file in Tomcat's working directory or set `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USERNAME`/`DB_PASSWORD` as environment variables.

Dependencies: `javax.servlet-api` (provided), `postgresql` (JDBC driver), `jackson-core` (unused), `jbcrypt` (password hashing), JUnit 5 (test).
