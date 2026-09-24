# Admin Bot Users API

Bulk creation of bot user profiles and their wallets. One call creates `count`
bot users **and** matching wallets in a single database transaction — either
everything is inserted or nothing is.

## Authentication

Requires the internal access-token header (same mechanism as the other admin
endpoints):

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
  "statusCode": 201,
  "message": "…",
  "error": null,
  "errors": null,
  "path": "/api/v1/admin/bot-users",
  "data": { },
  "timestamp": "2026-09-24T00:00:00Z"
}
```

---

## POST /api/v1/admin/bot-users

Creates `count` bot user profiles + wallets.

**ID pattern** (identical to the existing robot seed data):

```
user_profile.id = user_profile.telegram_id = wallet.id = wallet.user_profile_id
              = startId + i        for i in [0, count)
phone_number  = startPhone + i     (Ethiopian format, e.g. 251900017013)
```

Every created profile is `status=ACTIVE`, `role=PLAYER`, `is_bot=true`,
`is_deleted=false`, `password=null`, `referrer_id=null`. Every wallet starts
with `total_available_balance = initialBalance` and all other amounts `0`.

### Request body

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `startId` | integer | yes | — | First id. Used for `user_profile.id`, `telegram_id`, `wallet.id`, `wallet.user_profile_id`. Incremented by 1 per record. Must be > 0. |
| `startPhone` | integer | yes | — | First phone number, Ethiopian format without `+` (e.g. `251900017013`). Incremented by 1 per record. |
| `botRoomId` | integer | yes | — | Room the bots belong to (`user_profile.bot_room_id`). |
| `agentId` | integer | yes | — | Owning agent. Must exist → `404` otherwise. |
| `count` | integer | yes | — | Number of bot users to create. `1–500`. |
| `initialBalance` | number | no | `1000000000` | Starting `total_available_balance` for each wallet. |
| `names` | array | no | built-in pool | Name pool shuffled and dealt to created bots — each bot gets a distinct random name while `count <= pool size`. If omitted, a built-in pool of 500+ Ethiopian names is used. |
| `names[].firstName` | string | yes* | — | Required for each entry when `names` is provided. |
| `names[].lastName` | string | no | `null` | |
| `names[].nickname` | string | no | `null` | |

### Example request

```http
POST /api/v1/admin/bot-users
X-Access-Token: <token>
Content-Type: application/json
```

```json
{
  "startId": 1000017013,
  "startPhone": 251900017013,
  "botRoomId": 16,
  "agentId": 1,
  "count": 3,
  "initialBalance": 1000000000,
  "names": [
    { "firstName": "Lensa", "lastName": "Merga", "nickname": "Lensi" },
    { "firstName": "Hundee", "lastName": "Fufa", "nickname": "Hundee" }
  ]
}
```

This creates:

| id / telegram_id | phone_number | name | wallet.id | balance |
|---|---|---|---|---|
| 1000017013 | 251900017013 | Lensa Merga (Lensi) | 1000017013 | 1000000000 |
| 1000017014 | 251900017014 | Hundee Fufa (Hundee) | 1000017014 | 1000000000 |
| 1000017015 | 251900017015 | random pick from the pool | 1000017015 | 1000000000 |

### 201 response

```json
{
  "success": true,
  "statusCode": 201,
  "message": "Bot users and wallets created successfully",
  "error": null,
  "errors": null,
  "path": "/api/v1/admin/bot-users",
  "data": {
    "createdCount": 3,
    "firstId": 1000017013,
    "lastId": 1000017015,
    "firstPhone": "251900017013",
    "lastPhone": "251900017015",
    "botRoomId": 16,
    "agentId": 1,
    "initialBalance": 1000000000
  },
  "timestamp": "2026-09-24T00:00:00Z"
}
```

### Error responses

| Status | When | Body |
|---|---|---|
| `400` | Validation failure (missing field, `count` out of `1–500`, non-positive `startId`/`startPhone`) | `errors` map of field → message |
| `401` | Missing/invalid `X-Access-Token` | |
| `404` | `agentId` does not exist | `message`: `"Agent not found with ID: X"` |
| `409` | Any `id`, `telegram_id`, `phone_number`, or `wallet.id` in the requested range already exists | `message` includes conflict counts and the offending ranges |
| `500` | DB failure mid-insert — transaction rolled back, nothing persisted | |

`400` example:

```json
{
  "success": false,
  "statusCode": 400,
  "message": "Validation failed",
  "error": "Bad Request",
  "errors": { "count": "count must not exceed 500" },
  "path": "/api/v1/admin/bot-users",
  "data": null,
  "timestamp": "2026-09-24T00:00:00Z"
}
```

`409` example:

```json
{
  "success": false,
  "statusCode": 409,
  "message": "ID/phone range already in use: 2 user_profile and 0 wallet rows conflict with ids [1000017013..1000017015] / phones [251900017013..251900017015]. Nothing was inserted.",
  "path": "/api/v1/admin/bot-users",
  "data": null,
  "timestamp": "2026-09-24T00:00:00Z"
}
```

---

## Integration notes for the client

- **Atomicity** — the call is all-or-nothing. On any error response, assume zero
  rows were written; it is safe to retry with a different `startId`.
- **Choosing `startId`** — pick a range above existing ids. Existing bot seeds
  use `100001xxxx` blocks per room; keep the same convention (e.g. room 16 →
  `1000016xxx`) to avoid `409`s.
- **`startPhone` must stay in valid Ethiopian format** — the server stores
  `startPhone + i` verbatim; it does not reformat. Keep within `2519xxxxxxxx`.
- **`names` selection** — the pool is shuffled once per request and dealt in
  order, so every bot gets a distinct random name while `count <= pool size`
  (the built-in pool has 500+ entries). If you supply fewer than `count`
  names, the pool wraps around and names repeat.
- **Idempotency** — there is none by design: re-sending the same request returns
  `409` because the ids now exist. Treat `409` as "already created or range
  taken", not as a retryable error.
- **Sequence safety** — after a successful insert the server advances the
  `user_profile`/`wallet` id sequences, so normal user registration is
  unaffected.

### Fetch example

```ts
const res = await fetch("/api/v1/admin/bot-users", {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
    "X-Access-Token": token,
  },
  body: JSON.stringify({
    startId: 1000017013,
    startPhone: 251900017013,
    botRoomId: 16,
    agentId: 1,
    count: 20,
  }),
});

const body = await res.json();
if (!body.success) {
  // body.message describes the failure; body.errors has field-level detail on 400
  throw new Error(body.message);
}
console.log(`Created ${body.data.createdCount} bots, ids ${body.data.firstId}–${body.data.lastId}`);
```
