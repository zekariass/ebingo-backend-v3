# Golden Eggs External Game — Client Integration API

Base path: `/external-games/golden-eggs` (game launch) and `/external-games/game-settings` (agent settings).

All endpoints are reactive JSON APIs. Unless noted otherwise, every response uses the standard `ApiResponse` envelope.

## Authentication

Endpoints marked **Auth: Access Token** require the header:

```
X-Access-Token: <server-configured token>
```

The token is validated against the `endpoints.access-token` server configuration. Missing or invalid tokens return:

```json
{ "statusCode": 401, "success": false, "message": "Invalid or missing access token" }
```

The `/launch` endpoint additionally performs **Telegram WebApp initData verification** (HMAC-SHA256 against the agent's bot token) — see below.

## Standard Response Envelope

```json
{
  "success": true,
  "statusCode": 200,
  "message": "…",
  "error": null,
  "errors": null,
  "path": "/external-games/golden-eggs/launch",
  "data": { },
  "timestamp": "2026-09-21T20:00:00Z"
}
```

| Field | Type | Description |
|---|---|---|
| `success` | boolean | Request succeeded |
| `statusCode` | int | HTTP status code |
| `message` | string | Human-readable result |
| `error` | string \| null | Machine-readable error code (on failure) |
| `errors` | object \| null | Field-level validation errors |
| `path` | string | Request path |
| `data` | T \| null | Payload |
| `timestamp` | ISO-8601 | Server time |

---

## 1. Game Modes

**Auth: Access Token**

```
GET /external-games/golden-eggs/game-modes
```

Returns the list of game modes available from the Golden Eggs provider.

### Response `200` — `data`: array of

| Field | Type | Description |
|---|---|---|
| `gameMode` | string | Mode identifier (pass this to `/launch`) |
| `title` | string | Display title |
| `description` | string | Description |
| `category` | string | Game category |
| `iconsUrls` | object | Map of icon keys → URLs |
| `multiplayer` | boolean | Multiplayer support |
| `rtp` | string | Return-to-player value |
| `bonusTypes` | string[] | Supported bonus types |

```json
{
  "success": true,
  "statusCode": 200,
  "data": [
    {
      "gameMode": "CLASSIC",
      "title": "Golden Eggs Classic",
      "description": "...",
      "category": "crash",
      "iconsUrls": { "default": "https://…/icon.png" },
      "multiplayer": true,
      "rtp": "97.0",
      "bonusTypes": ["FREEBET"]
    }
  ]
}
```

---

## 2. Launch Game

**Auth: Access Token + Telegram initData**

```
POST /external-games/golden-eggs/launch
Content-Type: application/json
```

Verifies the Telegram WebApp `initData` against the agent's bot token, finds or creates the player, and returns a signed game URL containing a single-use launch token (TTL ~20 minutes).

### Request body

| Field | Type | Required | Description |
|---|---|---|---|
| `agentId` | long | yes | Agent (tenant) ID |
| `gameMode` | string | yes | Game mode from `/game-modes` |
| `currency` | string | yes | Currency code — `ETB` supported |
| `initData` | string | yes | Raw `Telegram.WebApp.initData` string |
| `subId` | string | no | Sub-player / affiliate ID |
| `lobbyUrl` | string | no | URL the game returns to on exit |
| `brandName` | string | no | Brand name shown in-game |
| `lang` | string | no | UI language code |
| `adaptive` | boolean | no | Adaptive layout flag |
| `isDemoPlay` | boolean | no | Launch in demo mode |
| `userCountryCode` | string | no | ISO 3166-1 alpha-2 (e.g. `ET`, `TR`) |

```json
{
  "agentId": 12,
  "gameMode": "CLASSIC",
  "currency": "ETB",
  "initData": "query_id=…&user=…&auth_date=…&hash=…",
  "lang": "en",
  "userCountryCode": "ET"
}
```

### Response `200`

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Launch URL generated successfully",
  "data": { "url": "https://provider.example/game?token=…" }
}
```

Redirect / open `data.url` in the WebApp to start the game.

### Errors

| HTTP | `error` | Cause |
|---|---|---|
| 400 | — | Validation failure (`errors` map has field details) |
| 401 | `INVALID_INIT_DATA` | Bad initData signature or expired `auth_date` |
| 404 | `AGENT_NOT_FOUND` | Unknown `agentId` |
| 500 | `CONFIGURATION_ERROR` | Agent missing bot token / provider config |
| 500 | `UNKNOWN_ERROR` | Unexpected failure |

---

## 3. Agent Game Settings

Base path: `/external-games/game-settings` — **Auth: Access Token**

### 3.1 Get enabled game modes

```
GET /external-games/game-settings?agentId={id}
```

Response `200` — `data`:

```json
{
  "id": 3,
  "agentId": 12,
  "gameModes": ["CLASSIC", "TURBO"],
  "createdAt": "2026-09-01T10:00:00",
  "updatedAt": "2026-09-10T12:00:00"
}
```

### 3.2 Update enabled game modes

```
PUT /external-games/game-settings
Content-Type: application/json
```

```json
{ "agentId": 12, "gameModes": ["CLASSIC", "TURBO", "MEGA"] }
```

Creates the settings record if none exists. Response `200` returns the updated `AgentGameSettingDto`. `400` on invalid input.

### 3.3 Check a game mode

```
GET /external-games/game-settings/check?agentId={id}&gameMode={mode}
```

Response `200` — `data`: `true` / `false`.

---

## 4. Accounting

Base path: `/external-games/golden-eggs/accounting` — **Auth: Access Token**

All endpoints are scoped by `agentId` (required query param). Amounts are decimal (up to 9 places). `netProfit = bets − wins` (positive = platform profit).

### Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/total?agentId=` | Cumulative totals for the agent |
| GET | `/daily?agentId=` | All daily records |
| GET | `/daily/{date}?agentId=` | Single day (`YYYY-MM-DD`) |
| GET | `/daily/range?agentId=&startDate=&endDate=` | Date range (`YYYY-MM-DD`) |
| GET | `/daily/unsettled?agentId=` | Records not yet settled |
| GET | `/daily/id/{id}?agentId=` | Record by ID |
| PUT | `/daily/{id}/settle?agentId=` | Mark record settled |
| PUT | `/daily/{id}/unsettle?agentId=` | Mark record unsettled |

### `data` shapes

**Total accounting**

```json
{
  "id": 1,
  "agentId": 12,
  "totalBetsCount": 1520,
  "totalBetsAmount": "30500.00",
  "totalWinsAmount": "28100.00",
  "totalLossAmount": "28100.00",
  "totalNetProfitAmount": "2400.00",
  "totalRollbackCount": 4,
  "totalRollbackAmount": "120.00",
  "createdAt": "…", "updatedAt": "…", "version": 37
}
```

**Daily accounting** — same fields with `daily*` prefix, plus:

```json
{ "accountingDate": "2026-09-21", "isSettled": false }
```

`404` with `error: "NOT_FOUND"` when a record doesn't exist for the given agent/id/date.

---

## 5. Provider Webhook (informational)

```
POST /webhooks/golden-eggs
```

This endpoint is called **by the Golden Eggs provider**, not by the client. It is authenticated via the `X-REQUEST-SIGN` HMAC-SHA256 header (no access token) and handles `init`, `bet`, `withdraw`, `rollback`, `bonus-complete`, and `bonus-expired-when-active` actions. Always returns HTTP 200 with a provider-format body; errors use codes such as `INVALID_TOKEN`, `CHECKS_FAIL`, `ACCOUNT_INVALID`, `INSUFFICIENT_FUNDS`, `UNKNOWN_ERROR`.

## Integration Flow

1. `GET /game-modes` → pick a mode (optionally filter by `GET /game-settings/check`).
2. Collect `Telegram.WebApp.initData` in the WebApp.
3. `POST /launch` with `agentId`, `gameMode`, `currency`, `initData`.
4. Open `data.url` — the provider calls the webhook for session init and all wallet operations.
5. Use accounting endpoints for reconciliation/reporting.
