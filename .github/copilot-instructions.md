# Copilot Instructions — WebsiteHealthMonitor

## Architecture

Java Servlet 4.0 WAR application (Java 8) for HTTP endpoint uptime monitoring. Three-tier layered architecture:

```
Filters (auth) → Servlets (one per endpoint) → Services (connection + tx) → DAOs (JDBC) → MySQL
```

- **Servlets** (`servlets/`): One class per HTTP endpoint, annotated with `@WebServlet`. Read input via `req.getParameter()`, delegate to services, write responses via `PrintWriter`.
- **Services** (`services/`): Own the JDBC `Connection` lifecycle. Open connections via `DriverManager.getConnection()`, manage transactions (`setAutoCommit(false)` / `commit()` / `rollback()`), call DAOs. Instantiate DAOs as `static final` fields.
- **DAOs** (`dao/`): Receive `Connection` as a method parameter. Use `PreparedStatement` for all SQL. Map `ResultSet` to model POJOs.
- **Models** (`model/`): Plain POJOs with getters/setters, no annotations or Lombok.
- **Filters** (`filter/`): `@WebFilter`-based role checks. `AdminAuthenticationFilter` guards admin routes; `OperatorAuthenticationFilter` guards operator routes.

## Authentication & Roles

- Token-based auth using in-memory `ConcurrentHashMap<String, User>` in `util/TokenManager`.
- Tokens are UUIDs stored in HTTP-only secure cookies named `"token"`, expire after 1 hour.
- Three roles: `admin`, `operator`, `viewer`. Filters check exact role match — admins do **not** pass operator filters.
- Filters set `req.getSession().getAttribute("user")` for downstream servlets.

## Servlet URL Map

| Pattern | Servlet | Method | Auth |
|---------|---------|--------|------|
| `/login` | `Login` | GET | None |
| `/register` | `RegisterUser` | POST | None |
| `/create` | `CreateUser` | POST | None (invite link) |
| `/generate_invitelink` | `GenerateInviteLink` | GET | Admin |
| `/delete_user` | `DeleteUser` | DELETE | Admin |
| `/create_monitor`, `/delete_monitor`, `/update_monitor` | CRUD servlets | POST/DELETE/PUT | Operator |
| `/monitors`, `/incidents` | Get servlets | GET | Operator |
| `/create_incident`, `/resolve_incident` | Incident servlets | POST/PUT | Operator |
| `/status` | `ViewAll` | GET | None (public) |

## Database & Connection Patterns

- PostgreSQL at `jdbc:postgresql://localhost:5432/sysuptimemonitor`.
- Credentials are loaded from a `.env` file (or `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` environment variables) by `util/DBManager`.
- All services and DAOs obtain connections via `DBManager.getConnection()` — **never** use `DriverManager.getConnection()` directly.
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

- **Input:** Always `req.getParameter()` (form-encoded), never Jackson deserialization.
- **Output:** Manual `PrintWriter` string concatenation — Jackson is declared in `pom.xml` but unused.
- When adding new JSON output, follow the existing manual pattern **or** refactor to use `ObjectMapper` if the scope warrants it.

## Background Monitor Execution

`util/MonitorExecutor` implements `ServletContextListener` (`@WebListener`). On startup, loads all enabled monitors and schedules `ScheduledExecutorService` fixed-rate tasks. Each job:
1. HTTP GET to `monitor.getTargetUrl()` with 5s timeout.
2. Records `MonitorRun` (status code, response time, success).
3. On unexpected status: decrements `AtomicInteger` fail counter → creates `Incident` when threshold reached.
4. On expected status: auto-resolves the last unresolved incident.

Dynamic add/remove via `addMonitor()`/`removeMonitor()` called from `MonitorService`.

## Key Domain Flows

- **Registration:** First user for an org (derived from email domain via `split("@")[1]`) becomes `admin`. Subsequent users join via invite links (role set by inviter).
- **Invite links** expire after 30 seconds and are single-use.
- **Organization scoping:** Monitors and incidents are scoped to an organization. `ViewAll` (`/status`) is the exception — it's public and shows all orgs.

## Known Issues to Be Aware Of

- `hasUnresolvedIncident()` naming is inverted — returns `true` when there are **no** unresolved incidents.
- `IncidentDao.updateIncident()` SQL is missing a comma between SET clauses.
- `UserDao.deleteUser()` uses `WHERE id=?` but binds an email string.
- Uptime calculation in `ViewAll` uses integer division (always yields 0 or 100).
- JSON output has trailing commas and occasional quoting issues.

## Build & Run

```bash
./mvnw clean package         # Build WAR
# Deploy target/WebsiteHealthMonitor-1.0-SNAPSHOT.war to Tomcat
# Place .env file in Tomcat's working directory (catalina.base) or set DB_URL/DB_USERNAME/DB_PASSWORD env vars
```

Dependencies: `javax.servlet-api` (provided), `postgresql` (JDBC driver), `jackson-core` (unused), `jbcrypt` (password hashing), JUnit 5 (test).
