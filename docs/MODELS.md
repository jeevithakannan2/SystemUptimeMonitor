# Data Models & Database Schema

## Java Models

All models are plain POJOs in `src/main/java/org/example/systemuptimemonitor/model/`.

### User

Represents a registered user within an organization.

| Field | Type | Description |
|-------|------|-------------|
| `id` | `int` | Auto-generated primary key |
| `email` | `String` | Unique email address |
| `password` | `String` | BCrypt-hashed password |
| `role` | `String` | `admin`, `operator`, or `viewer` |
| `organization` | `String` | Organization name (derived from email domain) |
| `loggedIn` | `long` | Timestamp of last login (used for token expiration) |

**Constructors:**
- `User()` — no-arg constructor (required for Jersey/Jackson deserialization)
- `User(int id, String email, String password, String role, String organization)` — full (from DB)
- `User(String email, String password, String role, String organization)` — new user (no ID)

### Monitor

Represents an HTTP endpoint being monitored.

| Field | Type | Description |
|-------|------|-------------|
| `id` | `int` | Auto-generated primary key |
| `name` | `String` | Display name |
| `targetUrl` | `String` | URL to check |
| `checkInterval` | `int` | Seconds between checks |
| `createdTime` | `long` | Creation timestamp |
| `createdBy` | `int` | ID of the user who created it |
| `failureCount` | `int` | Consecutive failures required before creating an incident |
| `organization` | `String` | Organization name |
| `enabled` | `boolean` | Whether background checks are active |
| `isPublic` | `boolean` | Whether this monitor appears on the public status page (default: false) |
| `statusCodes` | `ArrayList<Integer>` | Expected HTTP status codes (loaded separately from `status_codes` table) |

**Constructors:**
- `Monitor(int id, String name, String targetUrl, int checkInterval, long createdTime, int createdBy, int failureCount, String organization, boolean enabled, boolean isPublic)` — full
- `Monitor(String name, String targetUrl, int checkInterval, long createdTime, int createdBy, int failureCount, String organization, boolean enabled, boolean isPublic)` — new (no ID)

**Notable method:**
- `isExpectedStatusCode(int statusCode)` — checks if a status code is in the expected list

### MonitorRun

A single health check result.

| Field | Type | Description |
|-------|------|-------------|
| `id` | `int` | Auto-generated primary key |
| `monitor_id` | `int` | FK to the monitor |
| `time` | `long` | Timestamp of the check |
| `response_time` | `int` | Response time in milliseconds |
| `status_code` | `int` | HTTP status received (0 for network errors) |
| `success` | `boolean` | Whether the status code was expected |

**Constructors:**
- `MonitorRun(int id, int monitor_id, long time, int response_time, int status_code, boolean success)` — full
- `MonitorRun(int monitor_id, long time, int response_time, int status_code)` — from successful check
- `MonitorRun(int monitor_id, long time, int status_code)` — from failed check (no response time)
- `MonitorRun(int monitor_id, long time)` — network error (no status code or response time)

### Incident

Represents a downtime event for a monitor.

| Field | Type | Description |
|-------|------|-------------|
| `id` | `int` | Auto-generated primary key |
| `monitorId` | `int` | The monitor this incident belongs to |
| `monitorRunId` | `int` | FK to the monitor run that triggered this incident |
| `downTime` | `long` | Timestamp when the incident started |
| `resolvedTime` | `long` | Timestamp when resolved (0 if unresolved) |
| `statusCode` | `int` | The unexpected HTTP status code |
| `expectedStatusCodes` | `String` | CSV of expected codes (e.g., `"200,301"`) |
| `resolved` | `boolean` | Whether the incident has been resolved |
| `notes` | `String` | Resolution notes (max 256 chars, set on manual resolve) |

**Constructors:**
- `Incident(int id, int monitorId, int monitorRunId, long downTime, long resolvedTime, int statusCode, boolean resolved, String expectedStatusCodes)` — full
- `Incident(int id, int monitorRunId, long downTime, long resolvedTime, int statusCode, boolean resolved)` — from DB (partial)
- `Incident(int monitorRunId, int monitorId, long downTime, int statusCode, String expectedStatusCodes)` — new incident

### InviteLink

A time-limited, single-use invitation to join an organization.

| Field | Type | Description |
|-------|------|-------------|
| `id` | `int` | Auto-generated primary key |
| `createdBy` | `int` | User ID of the admin who created it |
| `createdTime` | `long` | Creation timestamp |
| `expired` | `boolean` | Whether the link has been used or manually expired |
| `url` | `String` | The invite code (currently a timestamp string) |
| `role` | `String` | Role assigned to the new user (`operator` or `viewer`) |

**Constructors:**
- `InviteLink(int id, int createdBy, long createdTime, boolean expired, String url, String role)` — full
- `InviteLink(int createdBy, long createdTime, boolean expired, String url, String role)` — new

**TTL:** 30 seconds from `createdTime`. Validated in `UserService.createUserFromLink()`.

### MonitorAudit

Immutable audit log entry for monitor lifecycle events.

| Field | Type | Description |
|-------|------|-------------|
| `id` | `int` | Auto-generated primary key (final) |
| `monitorId` | `int` | Monitor ID (final, no FK — survives monitor deletion) |
| `operation` | `String` | `CREATE`, `UPDATE`, or `DELETE` (final) |
| `time` | `long` | Timestamp of the operation (final) |

**Constructor:**
- `MonitorAudit(int id, int monitorId, String operation, long time)` — all fields final, getters only

---

## Database Schema

PostgreSQL with schema-per-organization multi-tenancy.

### Public Schema

Contains only the organization registry:

```sql
CREATE TABLE organizations (
    id          SERIAL       PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    schema_name VARCHAR(255) NOT NULL UNIQUE
);
```

> `OrganizationDao` queries the `public.organizations` table to list all orgs. It has no dedicated model class — results are returned as `Map<String, Object>` entries.

### Per-Organization Schema

Each org gets an isolated schema (e.g., `org_example_com`) with these tables:

#### users

```sql
CREATE TABLE users (
    id           SERIAL       PRIMARY KEY,
    email        VARCHAR(255) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    role         VARCHAR(50)  NOT NULL,
    organization VARCHAR(255) NOT NULL
);
```

#### invites

```sql
CREATE TABLE invites (
    id           SERIAL       PRIMARY KEY,
    created_by   INTEGER      NOT NULL REFERENCES users(id),
    created_time TIMESTAMP    NOT NULL,
    expired      BOOLEAN      NOT NULL DEFAULT FALSE,
    url          VARCHAR(255) NOT NULL UNIQUE,
    role         VARCHAR(50)  NOT NULL
);
```

#### monitors

```sql
CREATE TABLE monitors (
    id             SERIAL        PRIMARY KEY,
    name           VARCHAR(255)  NOT NULL,
    target_url     VARCHAR(2048) NOT NULL,
    check_interval INTEGER       NOT NULL,
    created_time   TIMESTAMP     NOT NULL,
    created_by     INTEGER       NOT NULL REFERENCES users(id),
    failure_count  INTEGER       NOT NULL DEFAULT 0,
    organization   VARCHAR(255)  NOT NULL,
    enabled        BOOLEAN       NOT NULL DEFAULT TRUE,
    is_public      BOOLEAN       NOT NULL DEFAULT FALSE
);
```

#### monitor_runs

```sql
CREATE TABLE monitor_runs (
    id            SERIAL    PRIMARY KEY,
    monitor_id    INTEGER   NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,
    time          TIMESTAMP NOT NULL,
    response_time INTEGER   NOT NULL,
    status_code   INTEGER   NOT NULL,
    success       BOOLEAN   NOT NULL
);
```

#### incidents

```sql
CREATE TABLE incidents (
    id                    SERIAL    PRIMARY KEY,
    monitor_run_id        INTEGER   NOT NULL REFERENCES monitor_runs(id) ON DELETE CASCADE,
    down_time             TIMESTAMP NOT NULL,
    resolved_time         TIMESTAMP,
    status_code           INTEGER   NOT NULL,
    expected_status_codes VARCHAR(255),
    resolved              BOOLEAN   NOT NULL DEFAULT FALSE,
    notes                 TEXT
);
```

#### status_codes

```sql
CREATE TABLE status_codes (
    monitor_id  INTEGER NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,
    status_code INTEGER NOT NULL,
    PRIMARY KEY (monitor_id, status_code)
);
```

#### monitor_audits

```sql
CREATE TABLE monitor_audits (
    id         SERIAL      PRIMARY KEY,
    monitor_id INTEGER     NOT NULL,
    operation  VARCHAR(50) NOT NULL,
    time       TIMESTAMP   NOT NULL
);
```

> `monitor_audits` intentionally has **no foreign key** to `monitors` — audit records survive monitor deletion.

---

## Entity Relationships

```
organizations (public)
    │
    └── [per-org schema]
            │
            ├── users
            │     ├──< invites        (created_by → users.id)
            │     └──< monitors       (created_by → users.id)
            │             ├──< status_codes     (monitor_id, ON DELETE CASCADE)
            │             ├──< monitor_audits   (monitor_id, no FK)
            │             └──< monitor_runs     (monitor_id, ON DELETE CASCADE)
            │                     └──< incidents (monitor_run_id, ON DELETE CASCADE)
            │
            └── [All tables isolated per org via PostgreSQL search_path]
```

### Cascade Behavior

| Parent | Child | On Delete |
|--------|-------|-----------|
| `monitors` | `monitor_runs` | CASCADE |
| `monitors` | `status_codes` | CASCADE |
| `monitor_runs` | `incidents` | CASCADE |
| `monitors` | `monitor_audits` | No FK (audit preserved) |

Deleting a monitor cascades through `monitor_runs` → `incidents` and `status_codes`, but `monitor_audits` records are preserved for historical tracking.

---

## Exceptions

All exceptions are `RuntimeException` subclasses with no-arg constructors, defined in `src/main/java/org/example/systemuptimemonitor/exceptions/`.

| Exception | Thrown When |
|-----------|------------|
| `MissingUserException` | User not found by email in `UserDao.getUserByEmail()` |
| `MissingMonitorException` | Monitor not found by ID |
| `MissingIncidentException` | Incident not found by ID during resolution |
| `IncidentAlreadyResolvedException` | Attempting to resolve an already-resolved incident |
| `MonitorAlreadyExistsException` | Creating a monitor with a `target_url` that already exists in the org |
| `UserAlreadyExistsException` | Creating a user with an email that already exists (via invite link) |
| `InviteLinkExpiredException` | Invite code is expired (>30 seconds old) or already used |
| `RoleMissingException` | Role parameter missing or invalid during registration |
