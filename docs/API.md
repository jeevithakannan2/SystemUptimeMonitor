# API Reference

All endpoints return JSON (`application/json`) unless otherwise noted. Errors are returned as:

```json
{"error": "Error message"}
```

Authentication is via an HTTP-only cookie named `token` (set by the login endpoint). Auth levels:
- **None** — No authentication required
- **Admin** — Requires `admin` role
- **Operator** — Requires `operator` or `admin` role

---

## Authentication

### Login

Authenticates a user and sets a session token cookie.

```
GET /api/login
```

**Auth:** None

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `email` | string | Yes | User's email address |
| `password` | string | Yes | Plaintext password |

**Success Response (200):**

```json
{
  "role": "admin",
  "email": "user@example.com",
  "organization": "example.com"
}
```

Sets cookie: `token=<UUID>; HttpOnly; Secure; Max-Age=3600`

**Error Responses:**

| Status | Message |
|--------|---------|
| 400 | Email and password are required |
| 400 | Not a valid email |
| 403 | Organization not found |
| 403 | User not found |
| 403 | Wrong password |
| 500 | Server error |

---

## User Management

### Register (First User / Org Creation)

Registers a new user. If the organization (email domain) doesn't exist yet, the user becomes the admin and the org is created.

```
POST /api/register
```

**Auth:** None

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `email` | string | Yes | Work email (domain becomes org name) |
| `password` | string | Yes | Plaintext password (hashed with BCrypt) |
| `role` | string | No | `operator` or `viewer` (ignored for first user, who becomes `admin`) |

**Success Response:** `200 OK` (no body)

**Error Responses:**

| Status | Message |
|--------|---------|
| 400 | Email and password are required |
| 400 | Not a valid email |
| 400 | Role parameter should be either viewer or operator |
| 500 | Cannot create user |

### Create User (Via Invite Link)

Creates a user from an invite link. The role is inherited from the invite.

```
POST /api/create
```

**Auth:** None (validated via invite code)

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `code` | string | Yes | Invite code from the invite link |
| `email` | string | Yes | Email address (must match org domain) |
| `password` | string | Yes | Plaintext password |

**Success Response:** `200 OK`

**Error Responses:**

| Status | Message |
|--------|---------|
| 400 | Not a valid email |
| 400 | Organization not found |
| 400 | Invite link expired |
| 400 | User already exists |
| 403 | Invite code is required |
| 500 | Server error |

### Delete User

Deletes a user by email. Admin only.

```
DELETE /api/delete_user
```

**Auth:** Admin

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `delete_email` | string | Yes | Email of user to delete |

> Parameters sent as `application/x-www-form-urlencoded` body.

**Success Response:** `200 OK`

**Error Responses:**

| Status | Message |
|--------|---------|
| 400 | Not a valid email |
| 500 | Failed to delete user |

### Generate Invite Link

Generates a single-use invite link for onboarding new users. Admin only.

```
GET /api/generate_invitelink
```

**Auth:** Admin

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `role` | string | Yes | `operator` or `viewer` |

**Success Response (200):** `text/plain` — the invite code (a timestamp string).

Users access the invite at: `/invite?code=<code>`

Invite links expire after **30 seconds** and are single-use.

**Error Responses:**

| Status | Message |
|--------|---------|
| 400 | Role should be either operator or viewer |
| 500 | Server error |

### Get Organization Users

Returns all users in the admin's organization.

```
GET /api/users
```

**Auth:** Admin

**Success Response (200):**

```json
{
  "users": [
    {
      "id": 1,
      "email": "admin@example.com",
      "role": "admin",
      "organization": "example.com"
    }
  ]
}
```

**Error Responses:**

| Status | Message |
|--------|---------|
| 500 | Failed to load users |

### Update User Role

Changes a user's role. Admin only.

```
PUT /api/update_role
```

**Auth:** Admin

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `user_id` | string | Yes | ID of the user to update |
| `role` | string | Yes | New role: `operator` or `viewer` |

> Parameters sent as `application/x-www-form-urlencoded` body.

**Success Response:** `200 OK`

**Error Responses:**

| Status | Message |
|--------|---------|
| 400 | user_id and role are required |
| 400 | user_id must be a valid number |
| 400 | Role must be either operator or viewer |
| 500 | Failed to update role |

---

## Monitors

### Create Monitor

Creates a new HTTP monitor.

```
POST /api/create_monitor
```

**Auth:** Operator

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `name` | string | Yes | Display name |
| `target_url` | string | Yes | Full URL to monitor |
| `expected_status_codes` | string | Yes | Comma-separated expected HTTP codes (e.g., `"200,301"`) |
| `check_interval` | string | Yes | Check interval in seconds |
| `enabled` | string | Yes | `"true"` or `"false"` |
| `failure_count` | string | No | Failures before incident (default: 3) |
| `is_public` | string | No | `"true"` or `"false"` (default: `"false"`) — if true, monitor appears on public status page |

**Success Response:** `200 OK`

The monitor is immediately scheduled for background execution if enabled.

**Error Responses:**

| Status | Message |
|--------|---------|
| 400 | All fields are required |
| 400 | Monitor target URL already exists |
| 500 | Server error |

### Get All Monitors

Returns all monitors for the authenticated user's organization.

```
GET /api/monitors
```

**Auth:** Operator

**Success Response (200):**

```json
{
  "monitors": [
    {
      "id": 1,
      "name": "API Server",
      "target_url": "https://api.example.com",
      "check_interval": 60,
      "created_time": "2024-03-23 12:00:00.0",
      "failure_count": 3,
      "organization": "example.com",
      "status_codes": [200, 301],
      "enabled": true,
      "is_public": false
    }
  ]
}
```

**Error Responses:**

| Status | Message |
|--------|---------|
| 500 | Failed to load monitors |

### Update Monitor

Updates an existing monitor. Only provided fields are changed.

```
PUT /api/update_monitor
```

**Auth:** Operator

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `monitor_id` | string | Yes | ID of monitor to update |
| `name` | string | No | New display name |
| `target_url` | string | No | New target URL |
| `expected_status_codes` | string | No | New comma-separated status codes |
| `check_interval` | string | No | New interval in seconds |
| `enabled` | string | No | `"true"` or `"false"` |
| `failure_count` | string | No | New failure threshold |
| `is_public` | string | No | `"true"` or `"false"` — controls public visibility |

> Parameters sent as `application/x-www-form-urlencoded` body.

**Success Response:** `200 OK`

The monitor is rescheduled in the background executor after update.

**Error Responses:**

| Status | Message |
|--------|---------|
| 400 | monitor_id must be a valid number |
| 400 | Not a valid number |
| 400 | Monitor target URL not found |
| 500 | Server error |

### Delete Monitor

Deletes a monitor and all its associated data (runs, incidents, status codes).

```
DELETE /api/delete_monitor
```

**Auth:** Operator

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Monitor ID |

> Parameters sent as `application/x-www-form-urlencoded` body.

**Success Response:** `200 OK`

**Error Responses:**

| Status | Message |
|--------|---------|
| 400 | Monitor ID is required |
| 400 | Not a valid monitor id |
| 400 | Specified monitor not found |
| 500 | Server error |

### Get Monitor History

Returns the audit trail for a specific monitor (create, update, delete events).

```
GET /api/monitor_history
```

**Auth:** Operator

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Monitor ID |

**Success Response (200):**

```json
{
  "history": [
    {
      "id": 1,
      "monitor_id": 1,
      "operation": "CREATE",
      "time": "2024-03-23 12:00:00.0"
    },
    {
      "id": 2,
      "monitor_id": 1,
      "operation": "UPDATE",
      "time": "2024-03-23 13:00:00.0"
    }
  ]
}
```

**Error Responses:**

| Status | Message |
|--------|---------|
| 400 | Missing required parameter: id |
| 400 | Invalid ID format |
| 500 | Failed to load history |

---

## Incidents

### Get All Incidents

Returns all incidents for the authenticated user's organization.

```
GET /api/incidents
```

**Auth:** Operator

**Success Response (200):**

```json
{
  "incidents": [
    {
      "id": 1,
      "monitor_id": 1,
      "monitor_run_id": 5,
      "down_time": "2024-03-23 12:30:00.0",
      "resolved_time": 0,
      "status_code": 500,
      "expected_status_codes": "200,301",
      "resolved": false
    }
  ]
}
```

> `resolved_time` is `0` when the incident is unresolved; a timestamp string when resolved.

**Error Responses:**

| Status | Message |
|--------|---------|
| 500 | Failed to load incidents |

### Create Incident (Manual)

Manually creates an incident for a monitor.

```
POST /api/create_incident
```

**Auth:** Operator

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `monitor_id` | string | Yes | Monitor ID |
| `status_code` | string | Yes | HTTP status code that triggered the incident |

**Success Response:** `200 OK`

Creates both a `MonitorRun` record and an `Incident` record in a single transaction.

**Error Responses:**

| Status | Message |
|--------|---------|
| 400 | monitor_id and status_code are required |
| 400 | monitor_id and status_code not a valid number |
| 500 | Error creating an incident |

### Resolve Incident

Resolves an open incident with optional notes.

```
PUT /api/resolve_incident
```

**Auth:** Operator

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `incident_id` | string | Yes | Incident ID |
| `notes` | string | No | Resolution notes (max 256 characters) |

> Parameters sent as `application/x-www-form-urlencoded` body.

**Success Response:** `200 OK`

**Error Responses:**

| Status | Message |
|--------|---------|
| 400 | incident_id cannot be empty |
| 400 | incident_id must be a positive number |
| 400 | Notes cannot be more than 256 characters |
| 400 | Specified incident not found |
| 400 | Incident already resolved |
| 500 | Error when resolving the incident |

---

## Public Status

### View Organization Status

Returns **public** monitors and their incidents for a given organization. Only monitors with `is_public = true` are included. This is the only endpoint that does not require authentication.

```
GET /api/status
```

**Auth:** None

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `org` | string | Yes | Organization name (e.g., `example.com`) |

**Success Response (200):**

```json
{
  "organization": "example.com",
  "monitors": [
    {
      "id": 1,
      "name": "API Server",
      "target_url": "https://api.example.com",
      "check_interval": 60,
      "created_time": "2024-03-23 12:00:00.0",
      "failure_count": 3,
      "organization": "example.com",
      "enabled": true,
      "is_public": true,
      "incidents": [
        {
          "id": 1,
          "monitor_run_id": 5,
          "down_time": "2024-03-23 12:30:00.0",
          "resolved_time": "2024-03-23 12:45:00.0",
          "status_code": 500
        }
      ],
      "uptime": 99.50
    }
  ]
}
```

The `uptime` field is a percentage calculated as:
```
uptime = ((totalTime - totalDownTime) / totalTime) * 100
```

Where `totalDownTime` is the sum of all incident durations (using current time for unresolved incidents).

**Error Responses:**

| Status | Message |
|--------|---------|
| 400 | Missing required parameter: org |
| 404 | Organization not found |
| 500 | Failed to load status |

### List Organizations

Returns all registered organizations.

```
GET /api/organizations
```

**Auth:** None

**Success Response (200):**

```json
{
  "organizations": [
    {
      "id": 1,
      "name": "example.com",
      "schema_name": "org_example_com"
    }
  ]
}
```

**Error Responses:**

| Status | Message |
|--------|---------|
| 500 | Failed to load organizations |
