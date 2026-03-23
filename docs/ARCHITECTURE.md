# Architecture

## System Overview

```
                                    ┌─────────────────────────────────────┐
                                    │           PostgreSQL                │
  HTTP Request                      │  ┌───────────┐  ┌───────────────┐  │
       │                            │  │  public    │  │ org_acme_com  │  │
       ▼                            │  │  schema    │  │    schema     │  │
  ┌─────────┐   ┌─────────┐   ┌────┤  │           │  │               │  │
  │JAX-RS   │──▶│Resources│──▶│Svc │──▶│ orgs table│  │ users         │  │
  │Filters  │   │(Jersey) │   │    │  │           │  │ monitors      │  │
  └─────────┘   └─────────┘   └────┤  └───────────┘  │ incidents     │  │
                                ▲   │                 │ monitor_runs  │  │
                                │   │  ┌───────────┐  │ invites       │  │
                            ┌───┴─┐ │  │org_foo_io │  │ status_codes  │  │
                            │DAO  │ │  │  schema   │  │ monitor_audits│  │
                            │(JDBC│ │  │  (same    │  └───────────────┘  │
                            │)    │ │  │  tables)  │                     │
                            └─────┘ │  └───────────┘                     │
                                    └─────────────────────────────────────┘
```

## Request Lifecycle

Every HTTP request follows this path:

```
Client → JAX-RS Filter → Resource → Service → DAO → PostgreSQL
```

1. **Filter** (`AdminAuthFilter` or `OperatorAuthFilter`):
   - JAX-RS `ContainerRequestFilter` implementations bound via `@NameBinding` annotations (`@AdminAuth`, `@OperatorAuth`) applied at the resource class level
   - Extracts `token` cookie via `containerRequestContext.getCookies().get("token")`
   - Validates via `TokenManager.isValid(token)` (checks existence + 1-hour TTL)
   - Retrieves `User` from `TokenManager.getUser(token)`
   - Checks role (`admin` only, or `operator || admin`)
   - Sets `containerRequestContext.setProperty("user", user)` for downstream access
   - Aborts unauthorized requests with `containerRequestContext.abortWith(Response.status(403)...)` returning `{"error": "Access denied"}`

2. **Resource** (e.g., `MonitorResource`):
   - JAX-RS resource classes annotated with `@Path("/")` and method-level `@Path` annotations
   - Reads parameters via `@QueryParam` (GET) and `@FormParam` (POST/PUT/DELETE)
   - Accesses user from filter via `containerRequestContext.getProperty("user")` (injected via `@Context ContainerRequestContext`)
   - Delegates to the appropriate Service
   - Returns `javax.ws.rs.core.Response` — Jersey+Jackson auto-serializes to JSON
   - Five resource classes: `AuthResource`, `AdminResource`, `MonitorResource`, `IncidentResource`, `PublicResource`

3. **Service** (e.g., `MonitorService`):
   - Opens a JDBC connection via `DBManager.getConnection(organization)`
   - Manages transactions: `setAutoCommit(false)` → DAO calls → `commit()` (or `rollback()` on error)
   - Orchestrates multiple DAOs within a single transaction
   - DAOs are `static final` fields, instantiated once

4. **DAO** (e.g., `MonitorDao`):
   - Receives `Connection` as a method parameter
   - Executes SQL via `PreparedStatement` with `?` placeholders
   - Uses **unqualified table names** — schema resolution handled by connection's `search_path`
   - Maps `ResultSet` rows to model POJOs

## Multi-Tenancy

Data isolation is achieved through **PostgreSQL schemas** — one schema per organization.

### Schema Layout

| Schema | Tables | Purpose |
|--------|--------|---------|
| `public` | `organizations` | Registry of all orgs and their schema names |
| `org_example_com` | `users`, `monitors`, `incidents`, `monitor_runs`, `invites`, `status_codes`, `monitor_audits` | All data for example.com |
| `org_acme_io` | (same tables) | All data for acme.io |

### How It Works

1. **Schema creation** — When the first user registers with `user@example.com`, `SchemaManager.createOrgSchema()` creates the `org_example_com` schema and all its tables.

2. **Schema naming** — `SchemaManager.toSchemaName("example.com")` → `"org_example_com"` (lowercase, non-alphanumeric replaced with `_`, prefixed with `org_`).

3. **Connection routing** — `DBManager.getConnection("example.com")` returns a connection with:
   ```sql
   SET search_path TO "org_example_com", public
   ```
   This means all unqualified table names (e.g., `SELECT * FROM users`) resolve to the org's schema first, then fall back to public.

4. **DAO simplicity** — DAOs never reference schema names. They use plain SQL like `INSERT INTO monitors ...` and the connection's `search_path` routes to the correct schema.

### Public Monitors

Monitors have an `is_public` boolean field. The public status endpoint (`/api/status`) only returns monitors where `is_public = true`, allowing organizations to control which monitors are visible on the public status page.

### Adding New Tables

When adding a new table to the per-org schema:
1. Add the `CREATE TABLE` statement to `SchemaManager.initOrgTables()`
2. Use unqualified table names in DAOs
3. Always get connections via `DBManager.getConnection(organization)`

## Authentication & Token Management

### Flow

```
Login → TokenManager.createToken(user) → UUID token → HTTP-only cookie ("token")
  ↓
Subsequent requests → Filter reads cookie → TokenManager.isValid(token) → Allow/Deny
```

### Token Storage

- In-memory `ConcurrentHashMap<String, User>` in `TokenManager`
- Keys: UUID token strings
- Values: `User` objects (with `loggedIn` timestamp set at creation)
- **Not persisted** — all tokens lost on server restart

### Expiration

Lazy expiration: tokens are checked on access, not cleaned up proactively.

```java
// Token is valid if: creationTime + 1 hour > now
if (user.getLoggedIn() + (1_000 * 3_600) > System.currentTimeMillis()) {
    return true;
}
// Otherwise, remove and return false
```

### Role Hierarchy

| Role | Admin endpoints | Operator endpoints | Public endpoints |
|------|:-:|:-:|:-:|
| `admin` | ✅ | ✅ | ✅ |
| `operator` | ❌ | ✅ | ✅ |
| `viewer` | ❌ | ❌ | ✅ |

Filters set `containerRequestContext.setProperty("user", user)` for downstream resource access.

## Jersey Configuration

`JerseyConfig` extends `ResourceConfig` with `@ApplicationPath("/api")`. It scans two packages:
- `org.example.systemuptimemonitor.resources` — JAX-RS resource classes
- `org.example.systemuptimemonitor.filter` — ContainerRequestFilter implementations

All endpoints are served under the `/api` path prefix. Jersey automatically discovers and registers all annotated classes in these packages.

## Background Monitor Execution

`MonitorExecutor` is a `@WebListener` (implements `ServletContextListener`) that runs periodic health checks.

### Lifecycle

```
App Start (contextInitialized)
  ├── DBManager.initSchema()           # Create public.organizations table
  ├── Load all monitors from all orgs
  └── Schedule each enabled monitor    # ScheduledExecutorService, fixed-rate

App Stop (contextDestroyed)
  └── Graceful shutdown (100s timeout, then force)
```

### Per-Monitor Job

Each monitor runs on a fixed schedule (`monitor.checkInterval` seconds):

```
Job Tick
  ├── hasUnresolvedIncident? → skip (don't pile up incidents)
  ├── HTTP GET → monitor.targetUrl (5s connect + read timeout)
  │
  ├── Expected status code?
  │   ├── Record MonitorRun (success=true)
  │   ├── Auto-resolve last incident
  │   └── Reset fail counter
  │
  └── Unexpected status or IOException?
      ├── Record MonitorRun (success=false, status_code=0 for network errors)
      ├── Decrement fail counter
      └── Counter reached 0?
          ├── Create Incident
          └── Reset counter to monitor.failureCount
```

### Key Design Decisions

- **Composite keys**: Internal maps use `"organization:monitorId"` to avoid ID collisions across orgs.
- **Fail threshold**: `monitor.failureCount` consecutive failures required before an incident is created (e.g., if set to 3, the first 2 failures are tolerated).
- **Auto-resolution**: When a previously-failing monitor returns an expected status, the last unresolved incident is automatically resolved.
- **Dynamic management**: `addMonitor()` / `removeMonitor()` called from `MonitorService` when monitors are created/updated/deleted — no restart needed.

## Connection Management

### DBManager

Two connection modes:

| Method | Use Case | Search Path |
|--------|----------|-------------|
| `getConnection()` | Public schema (org registry) | Default (public) |
| `getConnection(organization)` | Org-scoped operations | `"org_[sanitized]", public` |

### Configuration

Environment variables (loaded from `.env` or system env):

| Variable | Required | Default | Example |
|----------|----------|---------|---------|
| `DB_HOST` | Yes | — | `localhost` |
| `DB_PORT` | No | `5432` | `5432` |
| `DB_NAME` | Yes | — | `sysuptimemonitor` |
| `DB_USERNAME` | Yes | — | `postgres` |
| `DB_PASSWORD` | Yes | — | `secret` |

The JDBC URL is constructed as: `jdbc:postgresql://{host}:{port}/{dbName}`

### Transaction Pattern

All services follow this pattern:

```java
Connection connection = DBManager.getConnection(organization);
try {
    connection.setAutoCommit(false);

    // Multiple DAO calls within the same transaction
    dao1.operation(connection, ...);
    dao2.operation(connection, ...);

    connection.commit();
} catch (Exception e) {
    connection.rollback();
    throw e;
} finally {
    connection.setAutoCommit(true);
    connection.close();
}
```

## Utilities

| Class | Purpose |
|-------|---------|
| `DBManager` | Connection factory with schema-aware `search_path` routing |
| `SchemaManager` | Creates/manages per-org PostgreSQL schemas and tables |
| `TokenManager` | In-memory UUID token store with 1-hour lazy expiration |
| `MonitorExecutor` | `@WebListener` that schedules periodic HTTP health checks |
| `RequestBodyParser` | Parses URL-encoded bodies for PUT/DELETE (legacy, unused with Jersey) |
| `ErrorResponse` | Sends standardized `{"error": "..."}` JSON responses with proper escaping |
