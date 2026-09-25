# Agent Bot Config API

Per-agent configuration for the Telegram bot server (brand name, admin IDs,
support handles, bank details). Replaces the hardcoded per-agent config map —
`agentsData[agentId]` becomes `await getAgentConfig(agentId)` with no field
mapping.

## Authentication

All endpoints require the internal access-token header (same mechanism as
`GET /api/v1/agents/active`):

```
X-Access-Token: <server-configured token>
```

Missing/invalid token → `401`:

```json
{ "statusCode": 401, "success": false, "message": "Invalid or missing access token" }
```

## Standard envelope

All responses use the standard wrapper:

```json
{
  "success": true,
  "statusCode": 200,
  "message": "…",
  "error": null,
  "errors": null,
  "path": "/api/v1/agents/2/bot-config",
  "data": { },
  "timestamp": "2026-09-22T09:00:00Z"
}
```

---

## 1. Bot-facing: get config

```
GET /api/v1/agents/{agentId}/bot-config
```

| | |
|---|---|
| Auth | `X-Access-Token` header |
| Path param | `agentId` (integer) — the agent's `id` |

### 200 response

Field names are **contractual** — consume them exactly as returned.

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Agent bot config retrieved successfully",
  "error": null,
  "errors": null,
  "path": "/api/v1/agents/2/bot-config",
  "data": {
    "agentId": 2,
    "name": "Awash Bingo",
    "adminIds": "1961597377,312661397",
    "logoName": "logo_abex.png",
    "supportContact": "",
    "supportUsername": "AbexSupportBot",
    "supportChannel": "abexbingo",
    "bankDetails": {
      "telebirr":  { "recieverName": "bezawite tadele zenebe", "phoneNumber": "251902493104" },
      "cbeonline": { "accountName": "Bezawit Tadele Zenebe", "accountNumber": "1000210696354" }
    },
    "themeKey": null,
    "hideName": false
  },
  "timestamp": "2026-09-22T09:00:00Z"
}
```

### 404 response (agent or config not found)

```json
{
  "success": false,
  "statusCode": 404,
  "message": "Agent config not found for agent ID: 99",
  "error": "Not Found",
  "errors": null,
  "path": "/api/v1/agents/99/bot-config",
  "data": null,
  "timestamp": "2026-09-22T09:00:00Z"
}
```

### Field reference (`data`)

| Field | Type | Notes |
|---|---|---|
| `agentId` | number | Agent's `id` |
| `name` | string \| null | Brand display name used in bot messages ("Awash Bingo") |
| `adminIds` | string \| null | **CSV string** of Telegram user IDs — split on `,` to get a list |
| `logoName` | string \| null | Static logo filename served by the bot app |
| `supportContact` | string \| null | Free-form contact (email, phone); may be `""` |
| `supportUsername` | string \| null | Telegram username, **no `@`** |
| `supportChannel` | string \| null | Telegram channel/group handle, **no `@`** |
| `bankDetails` | object \| null | **Free-form map keyed by payment method** — do not assume a fixed schema; new methods can appear |
| `themeKey` | string \| null | UI theme palette key; `null` = client "default" palette |
| `hideName` | boolean | When `true`, hide player display names for this agent |

`bankDetails` per-method shape is defined by the client contract, e.g.:

```json
{
  "telebirr":  { "recieverName": "…", "phoneNumber": "…" },
  "cbeonline": { "accountName": "…", "accountNumber": "…" }
}
```

> **`recieverName` is intentionally misspelled** — it is part of the existing
> client contract. Do not "fix" it.

### Caching guidance

- Config changes are rare — **safe to cache briefly** (e.g. in-memory `Map` with
  a TTL of 1–5 minutes).
- **Re-fetch on 404**: a 404 means the agent or its config row doesn't exist;
  do not cache the 404, retry the fetch on next use.
- On fetch failure, prefer serving the last cached value over failing the bot flow.

---

## 2. Admin: fetch config for editing

```
GET /api/v1/admin/agents/{agentId}/config
```

| | |
|---|---|
| Auth | `X-Access-Token` header |
| Path param | `agentId` (integer) |

Response body is identical in shape to the bot-facing endpoint above
(`data` = same `AgentConfigDto`). `404` if the agent or config doesn't exist.

## 3. Admin: upsert config

```
PUT /api/v1/admin/agents/{agentId}/config
```

| | |
|---|---|
| Auth | `X-Access-Token` header |
| Path param | `agentId` (integer) |
| Body | `AgentConfigUpdateDto` (JSON) |

Full upsert: creates the row if missing, otherwise replaces all fields with the
provided values. `404` if the agent doesn't exist.

### Request body

```json
{
  "name": "Awash Bingo",
  "adminIds": "1961597377,312661397",
  "logoName": "logo_abex.png",
  "supportContact": "",
  "supportUsername": "AbexSupportBot",
  "supportChannel": "abexbingo",
  "bankDetails": {
    "telebirr":  { "recieverName": "bezawite tadele zenebe", "phoneNumber": "251902493104" },
    "cbeonline": { "accountName": "Bezawit Tadele Zenebe", "accountNumber": "1000210696354" }
  },
  "themeKey": null,
  "hideName": false
}
```

All fields are optional in the payload; omitted fields are stored as `null`
(this is a replace, not a patch).

### 200 response

Returns the saved config in the same `AgentConfigDto` shape as the GET.

## 4. Admin: create agent

```
POST /api/v1/admin/agents
```

| | |
|---|---|
| Auth | `X-Access-Token` header |
| Body | `AgentCreateDto` (JSON) |

Creates the agent **and** an empty `agent_config` row in the same transaction,
so the config can be filled in later via the PUT endpoint.

```json
{
  "name": "New Agent",
  "code": "AGT-004",
  "phoneNumber": "+2519…",
  "email": "agent@example.com",
  "contactName": "…",
  "isMaster": false,
  "isActive": true,
  "commissionRate": 10.0,
  "botToken": "…",
  "botUsername": "…",
  "contactAddress": "…"
}
```

`name`, `code`, `phoneNumber`, `email` are required. `201` → `AgentDto` in `data`.

---

## Notes for integrators

- **Never expose** `adminIds` or `bankDetails` in public agent responses — these
  fields are intentionally absent from `GET /api/v1/agents/{id}`.
- `bankDetails` keys are payment-method identifiers (`telebirr`, `cbeonline`, …);
  iterate keys rather than hardcoding them.
