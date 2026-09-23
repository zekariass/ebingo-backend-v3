# Agent Deposit Config API

Per-agent deposit bonus/lock rules applied when a player's wallet is credited
with a `DEPOSIT` transaction. Each agent can have **at most one** config row;
agents without a row automatically use the global defaults defined server-side
(`application.yml` → `deposit.*`).

## What the config controls

On every deposit of `amount`:

```
bonus = amount * bonusAmount.rate + bonusAmount.fixed
if bonusAmount.max.isCapped and bonus > bonusAmount.max.amount:
    bonus = bonusAmount.max.amount

lock  = (amount + bonus) * lockAmount.rate + lockAmount.fixed
if lockAmount.max.isCapped and lock > lockAmount.max.amount:
    lock = lockAmount.max.amount
```

- `bonus` is added to the wallet's `depositBonus` and `totalAvailableBalance`.
- `lock` is added to the wallet's `lockedAmount` (funds withheld from withdrawal).

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
  "path": "/api/v1/agents/2/deposit-config",
  "data": { },
  "timestamp": "2026-09-22T09:00:00Z"
}
```

## Config object shape (`AgentDepositConfigDto`)

Used identically as the GET response `data` and the PUT request body.

```json
{
  "agentId": 2,
  "bonusAmount": {
    "rate": 0.1,
    "fixed": 0,
    "max": { "isCapped": true, "amount": 500 }
  },
  "lockAmount": {
    "rate": 0.5,
    "fixed": 0,
    "max": { "isCapped": false, "amount": 1000 }
  }
}
```

### Field reference

| Field | Type | Required | Notes |
|---|---|---|---|
| `agentId` | number | response only | Ignored in PUT body — the path `agentId` always wins |
| `bonusAmount.rate` | number ≥ 0 | yes | Fraction of the deposit credited as bonus (0.1 = 10%) |
| `bonusAmount.fixed` | number ≥ 0 | yes | Flat bonus added on top of the rate |
| `bonusAmount.max.isCapped` | boolean | yes | Whether the bonus is capped |
| `bonusAmount.max.amount` | number ≥ 0 | yes | Max bonus when `isCapped` is true |
| `lockAmount.rate` | number ≥ 0 | yes | Fraction of `(deposit + bonus)` locked from withdrawal |
| `lockAmount.fixed` | number ≥ 0 | yes | Flat lock added on top of the rate |
| `lockAmount.max.isCapped` | boolean | yes | Whether the lock amount is capped |
| `lockAmount.max.amount` | number ≥ 0 | yes | Max lock when `isCapped` is true |

All fields are **required** in a PUT body — this is a full replace, not a patch.
Omitting a field fails validation (`400`).

---

## Agent self-service endpoints

### 1. Get own deposit config

```
GET /api/v1/agents/{agentId}/deposit-config
```

| | |
|---|---|
| Auth | `X-Access-Token` header |
| Path param | `agentId` (integer) — the agent's `id` |

Returns the **effective** config: the stored row when one exists, otherwise the
global defaults. Always returns `200` with a full config object for an existing
agent — a missing row is not an error.

#### 200 response

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Deposit config retrieved successfully",
  "error": null,
  "errors": null,
  "path": "/api/v1/agents/2/deposit-config",
  "data": {
    "agentId": 2,
    "bonusAmount": { "rate": 0.1, "fixed": 0, "max": { "isCapped": true, "amount": 500 } },
    "lockAmount":  { "rate": 0.5, "fixed": 0, "max": { "isCapped": false, "amount": 1000 } }
  },
  "timestamp": "2026-09-22T09:00:00Z"
}
```

#### 404 response (agent not found)

```json
{
  "success": false,
  "statusCode": 404,
  "message": "Agent not found with ID: 99",
  "error": "Not Found",
  "errors": null,
  "path": "/api/v1/agents/99/deposit-config",
  "data": null,
  "timestamp": "2026-09-22T09:00:00Z"
}
```

### 2. Create or replace own deposit config

```
PUT /api/v1/agents/{agentId}/deposit-config
```

| | |
|---|---|
| Auth | `X-Access-Token` header |
| Path param | `agentId` (integer) |
| Body | `AgentDepositConfigDto` (JSON, all fields required) |

Upsert semantics: creates the row if the agent has none, otherwise replaces all
fields. `404` if the agent doesn't exist. `400` on validation failure
(missing fields, negative numbers).

#### Request body

```json
{
  "bonusAmount": { "rate": 0.15, "fixed": 5, "max": { "isCapped": true, "amount": 300 } },
  "lockAmount":  { "rate": 0.4,  "fixed": 0, "max": { "isCapped": false, "amount": 0 } }
}
```

#### 200 response

Returns the saved config in the same shape as the GET.

### 3. Delete own deposit config

```
DELETE /api/v1/agents/{agentId}/deposit-config
```

| | |
|---|---|
| Auth | `X-Access-Token` header |
| Path param | `agentId` (integer) |

Removes the agent's config row. Afterwards the agent falls back to the global
defaults. `404` if the agent doesn't exist; deleting a non-existent row is a
no-op success.

#### 200 response

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Deposit config deleted; global defaults now apply",
  "error": null,
  "errors": null,
  "path": "/api/v1/agents/2/deposit-config",
  "data": null,
  "timestamp": "2026-09-22T09:00:00Z"
}
```

---

## Admin endpoints

Identical operations under the admin namespace — same bodies, same responses:

```
GET    /api/v1/admin/agents/{agentId}/deposit-config
PUT    /api/v1/admin/agents/{agentId}/deposit-config
DELETE /api/v1/admin/agents/{agentId}/deposit-config
```

---

## Notes for integrators

- **One config per agent** — enforced by the DB primary key; PUT always
  overwrites, never duplicates.
- **GET never 404s for a missing config row** — it returns the global defaults
  instead. A 404 means the *agent* doesn't exist.
- **No partial updates** — PUT is a full replace; send the complete object
  (GET → modify → PUT round-trip is the intended flow).
- **Changes apply to future deposits only** — already-credited bonuses/locks
  are not recalculated.
- **`isCapped` spelling** — the JSON field is `isCapped` (note the double `p`);
  the legacy `application.yml` key `isCaped` is unrelated to this API.
- **Numbers are decimals** — send/receive plain JSON numbers (`0.1`, `500`),
  not strings.
