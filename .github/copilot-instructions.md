# Copilot Instructions — SystemUptimeMonitor

## Architecture

Jersey 2.39.1 JAX-RS (javax.ws.rs) WAR application (Java 8) for HTTP endpoint uptime monitoring. Three-tier layered architecture:

```
JAX-RS Filters (auth) → Resources (Jersey) → Services (connection + tx) → DAOs (JDBC) → PostgreSQL
```

- **Resources** (`resources/`): Five JAX-RS resource classes annotated with `@Path`. Read input via `@FormParam` for POST/PUT/DELETE and `@QueryParam` for GET. Return `javax.ws.rs.core.Response`. Jersey auto-serializes POJOs to JSON via jackson-databind.
- **Services** (`services/`): Own the JDBC `Connection` lifecycle. Open connections via `DBManager.getConnection(organization)` (org-scoped) or `DBManager.getConnection()` (public schema). Manage transactions (`setAutoCommit(false)` / `commit()` / `rollback()`), call DAOs. Instantiate DAOs as `static final` fields.
- **DAOs** (`dao/`): Receive `Connection` as a method parameter. Use `PreparedStatement` for all SQL. Map `ResultSet` to model POJOs. Use **unqualified table names** — schema resolution is handled by the connection's `search_path`.
- **Models** (`model/`): Plain POJOs with getters/setters, no annotations or Lombok.
- **Filters** (`filter/`): JAX-RS `ContainerRequestFilter` implementations with custom `@NameBinding` annotations (`@AdminAuth`, `@OperatorAuth`). Applied to resource classes, not URL patterns.
- **Config** (`config/JerseyConfig.java`): Extends `ResourceConfig` with `@ApplicationPath("/api")`, scans `resources` and `filter` packages.

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
- `AdminAuthFilter` implements `ContainerRequestFilter`, annotated with `@AdminAuth` (custom `@NameBinding`). Requires `role == "admin"`.
- `OperatorAuthFilter` implements `ContainerRequestFilter`, annotated with `@OperatorAuth` (custom `@NameBinding`). Allows `role == "operator" || role == "admin"`.
- Filters set `containerRequestContext.setProperty("user", user)` — resources access via `crc.getProperty("user")`.

## API Endpoint Map

All paths are under `/api` (set by `@ApplicationPath("/api")` in `JerseyConfig`).

| Pattern | Resource | Method | Auth |
|---------|----------|--------|------|
| `/api/login` | `AuthResource` | GET | None |
| `/api/register` | `AuthResource` | POST | None |
| `/api/create` | `AuthResource` | POST | None (invite link) |
| `/api/generate_invitelink` | `AdminResource` | GET | Admin |
| `/api/delete_user` | `AdminResource` | DELETE | Admin |
| `/api/users` | `AdminResource` | GET | Admin |
| `/api/update_role` | `AdminResource` | PUT | Admin |
| `/api/create_monitor` | `MonitorResource` | POST | Operator |
| `/api/delete_monitor` | `MonitorResource` | DELETE | Operator |
| `/api/update_monitor` | `MonitorResource` | PUT | Operator |
| `/api/monitors` | `MonitorResource` | GET | Operator |
| `/api/monitor_history` | `MonitorResource` | GET | Operator |
| `/api/incidents` | `IncidentResource` | GET | Operator |
| `/api/create_incident` | `IncidentResource` | POST | Operator |
| `/api/resolve_incident` | `IncidentResource` | PUT | Operator |
| `/api/status` | `PublicResource` | GET | None (public) |
| `/api/organizations` | `PublicResource` | GET | None (public) |
| `/api/notification_preference` | `NotificationResource` | PUT | Operator |
| `/api/subscribe_monitor` | `NotificationResource` | POST | Operator |
| `/api/unsubscribe_monitor` | `NotificationResource` | DELETE | Operator |
| `/api/subscriptions` | `NotificationResource` | GET | Operator |

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

- **Input:** `@FormParam` for POST/PUT/DELETE form-encoded params, `@QueryParam` for GET query params. Jersey natively handles parsing for all HTTP methods — no manual body parsing needed.
- **Output:** Jersey + Jackson auto-serialization. Resources return `Response.ok(pojoOrMap).build()` and Jackson serializes to JSON automatically. No manual `PrintWriter` string concatenation.
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
- **Organization scoping:** Monitors and incidents are scoped to an organization. `PublicResource` (`/api/status`) is the exception — it's public and shows all orgs' monitors where `is_public = true`.
- **Public monitors:** The `Monitor` model has an `is_public` boolean field. When true, the monitor appears on the public status page (`/api/status?org=X`). Only public monitors are returned by `PublicResource`.
- **Organization listing:** `/api/organizations` returns a list of all registered organizations.
- **User management:** `/api/users` returns all users in the authenticated admin's org. `/api/update_role` allows admins to change user roles within their org.

## Email Notifications

Optional SMTP-based email notifications for incident events.

- **Configuration:** `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM` env vars. If `SMTP_HOST` is empty, emails are silently disabled.
- **Per-user toggle:** `email_notifications` boolean on the `users` table. Toggled via `PUT /api/notification_preference`.
- **Per-monitor subscription:** `monitor_subscriptions` join table (user_id, monitor_id). Users subscribe/unsubscribe via `POST /api/subscribe_monitor` and `DELETE /api/unsubscribe_monitor`.
- **Trigger:** `IncidentService` sends emails after creating or resolving an incident. Queries subscribed users with `email_notifications = true` via `MonitorSubscriptionDao.getSubscribedEmails()`.
- **Async:** `EmailService` uses a single-thread `ExecutorService` — email failures never block incident processing.

## Known Issues

- `MonitorService.hasUnresolvedIncident()` has inverted logic — returns `true` when there are **no** unresolved incidents (`incident == null`).

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

Dependencies: `javax.servlet-api` (provided), `postgresql` (JDBC driver), `jersey-container-servlet`, `jersey-media-json-jackson`, `jersey-hk2`, `jackson-databind`, `jbcrypt` (password hashing), `javax.mail` (JavaMail 1.6.2), JUnit 5 (test).

## Frontend (React SPA)

The frontend lives in `frontend/` and is a React 18 + TypeScript + Vite SPA using Tailwind CSS v4 and Shadcn/ui components with a Solstice warm glassmorphic design system.

### Stack
- React 18 + TypeScript + Vite
- Tailwind CSS v4 (`@tailwindcss/vite` plugin — no `tailwind.config` needed)
- Shadcn/ui (new-york style) with Radix primitives
- Lucide React icons, Framer Motion, next-themes, Sonner toasts
- React Router v6 (client-side routing)
- Axios for API calls

### Structure
```
frontend/src/
├── components/       # Reusable components (GlassCard, NavRail, StatusBadge, etc.)
│   └── ui/           # Shadcn/ui generated components
├── hooks/            # useAuth (AuthContext), useSidebar
├── pages/            # Login, Register, Invite, Dashboard, Admin, Status
├── services/api.ts   # Axios client with all backend endpoints
├── types/index.ts    # TypeScript interfaces (Monitor, Incident, User, etc.)
├── lib/utils.ts      # cn() utility for Tailwind class merging
└── index.css         # Solstice theme (OKLch CSS variables, glass utilities)
```

### Dev Workflow
```bash
cd frontend
npm install            # Install dependencies
npm run dev            # Start dev server on :5173 (proxies /api/* → :9090)
npm run build          # Production build → src/main/webapp/
```

### API Proxy
Vite dev server proxies `/api/*` to `localhost:9090`, keeping the `/api` prefix intact. Jersey serves at `/api/*` via `@ApplicationPath("/api")`, so requests pass through unchanged (e.g., `api.get('/api/monitors')` → `GET /api/monitors` on Tomcat).

### Auth Pattern
Cookie-based auth using the existing backend token system. `useAuth` hook provides `user`, `isAuthenticated`, `isAdmin`, `isOperator`, `logout`. Protected routes are guarded in `App.tsx` router config.

### Adding Shadcn Components
```bash
cd frontend
npx shadcn@latest add <component-name>
# Then move from @/components/ui/ to src/components/ui/ if shadcn creates a literal @/ directory
```
