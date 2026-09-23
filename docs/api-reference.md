# eBingo Backend — API Reference

Complete client integration reference for all modules.

## Authentication

All endpoints except `/webhooks/golden-eggs` require the header:

```
X-Access-Token: <server-configured token>
```

Validated against `endpoints.access-token` config. Missing/invalid → `401`:

```json
{ "statusCode": 401, "success": false, "message": "Invalid or missing access token" }
```

Some endpoints additionally verify Telegram WebApp `initData` (noted per endpoint, sent via `x-init-data` header or request body).

## Standard Envelope

Most endpoints return `ApiResponse<T>`:

```json
{
  "success": true, "statusCode": 200, "message": "…",
  "error": null, "errors": null, "path": "/…",
  "data": { }, "timestamp": "2026-09-21T20:00:00Z"
}
```

Paginated endpoints wrap `data` in `PageResponse<T>`:

```json
{ "content": [ ], "page": 0, "size": 10, "totalElements": 42 }
```

## Modules

- [1. Agents](#1-agents) — `/api/v1/agents`
- [2. Agent Games](#2-agent-games) — `/api/agent-games`
- [3. User Profiles](#3-user-profiles) — `/api/v1/public/user-profile`, `/api/v1/secured/user-profile`
- [4. Wallet](#4-wallet) — `/api/v1/secured/wallet`
- [5. Payments](#5-payments) — `/api/v1/secured/payments`, `/payment-orders/offline`, `/payment-methods`, `/transactions`, `/deposit/transfers`
- [6. Game Transactions](#6-game-transactions) — `/api/v1/secured/game/transaction`
- [7. Rooms](#7-rooms) — `/api/v1/public/rooms`, `/api/v1/secured/rooms`
- [8. Accounting](#8-accounting) — `/api/v1/accounting/daily`, `/api/v1/accounting/total`
- [9. Leaderboard](#9-leaderboard) — `/api/v1/leaderboard`
- [10. System Config](#10-system-config) — `/api/v1/system-configs`
- [11. Autoplay Admin](#11-autoplay-admin) — `/admin/autoplay`
- [12. Golden Eggs (External Games)](#12-golden-eggs-external-games) — `/external-games/**`, `/webhooks/golden-eggs`

## 1. Agents

Base path: `/api/v1/agents` — all require `X-Access-Token`.

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/agents` | List all agents |
| GET | `/api/v1/agents/{id}` | Get agent by ID |
| GET | `/api/v1/agents/{id}/bot-config` | Get agent bot config (brand name, admin IDs, support, bank details) — see [agent-config-api.md](agent-config-api.md) |
| GET | `/api/v1/agents/active` | List active agents |
| GET | `/api/v1/agents/search?name={name}` | Search agents by name |
| POST | `/api/v1/admin/agents` | Create agent (+ empty bot config, same transaction) |
| GET | `/api/v1/admin/agents/{id}/config` | Get agent bot config for editing |
| PUT | `/api/v1/admin/agents/{id}/config` | Upsert agent bot config |

**Response — `AgentDto`:**

```json
{
  "id": 1, "name": "Agent A", "code": "AGT-001",
  "phoneNumber": "+2519…", "email": "a@b.com", "contactName": "…",
  "isMaster": false, "isActive": true, "commissionRate": 10.0,
  "botToken": "…", "botUsername": "…", "contactAddress": "…",
  "createdAt": "…", "updatedAt": "…"
}
```

## 2. Agent Games

Base path: `/api/agent-games` — all require `X-Access-Token`.

| Method | Path | Description |
|---|---|---|
| GET | `/api/agent-games` | List all agent-game associations |
| GET | `/api/agent-games/{id}` | Get by ID |
| POST | `/api/agent-games` | Create association |
| PATCH | `/api/agent-games/{id}/status?enabled={bool}` | Enable/disable |
| DELETE | `/api/agent-games/{id}` | Delete |

**Request — `CreateAgentGameRequest`:**

```json
{ "agentId": 1, "gameCategory": "BINGO", "gameTypes": "…", "isEnabled": true }
```

`gameCategory`: `BINGO` | `EXTERNAL_GAMES`

**Response — `AgentGameResponse`:** `{ id, agentId, gameCategory, gameTypes, isEnabled, createdAt, updatedAt }`

## 3. User Profiles

### Public — `/api/v1/public/user-profile` (requires `X-Access-Token`)

| Method | Path | Description |
|---|---|---|
| POST | `/register` | Register a new user |
| POST | `/create-password` | Set initial password |
| POST | `/update-password` | Change password |

**`POST /register` — `UserProfileCreateDto`:**

```json
{ "telegramId": 123456, "referrerId": null, "firstName": "A", "lastName": "B", "phoneNumber": "+2519…", "agentId": 1 }
```

Required: `telegramId`, `firstName`, `phoneNumber`, `agentId`. Returns `UserProfileDto`.

**`POST /create-password` — `CreatePasswordRequestDto`:**

```json
{ "telegramId": 123456, "agentId": 1, "password": "1234" }
```

**`POST /update-password` — `UpdatePasswordRequestDto`:**

```json
{ "agentId": 1, "telegramId": 123456, "oldPassword": "1234", "newPassword": "5678" }
```

### Secured — `/api/v1/secured/user-profile` (requires `X-Access-Token`)

| Method | Path | Description |
|---|---|---|
| GET | `/{telegramId}?agentId={id}` | Get profile by Telegram ID |
| PUT | `/{telegramId}/nickname?agentId={id}&nickname={n}` | Update nickname |
| GET | `/ids?agentId={id}` | List all user Telegram IDs for agent |

**Response — `UserProfileDto`:**

```json
{
  "id": 1, "telegramId": 123456, "referrerId": null, "agentId": 1,
  "firstName": "A", "lastName": "B", "nickname": "…", "phoneNumber": "+2519…",
  "status": "ACTIVE", "role": "PLAYER", "isBot": false, "botRoomId": null,
  "hasPassword": true, "password": null, "createdAt": "…", "updatedAt": "…"
}
```

`status`: `ACTIVE` | `BANNED`. `role`: `PLAYER` | `MODERATOR` | `ADMIN` | `AGENT`.

**`/ids` response — `UserIdsResponseDto`:** `{ "agentId": 1, "ids": [123, 456] }`

## 4. Wallet

Base path: `/api/v1/secured/wallet` — requires `X-Access-Token`.

| Method | Path | Description |
|---|---|---|
| GET | `/telegram/{telegramId}?agentId={id}` | Wallet by Telegram ID |
| GET | `/details/{phoneNumber}?agentId={id}` | Wallet + profile by phone |

**Response — `WalletDto`:**

```json
{
  "id": 1, "userProfileId": 5, "agentId": 1,
  "welcomeBonus": 0, "availableWelcomeBonus": 0,
  "referralBonus": 0, "availableReferralBonus": 0,
  "totalPrizeAmount": 0, "pendingWithdrawal": 0,
  "totalAvailableBalance": 100.00, "availableToWithdraw": 80.00,
  "lockedAmount": 0, "depositBonus": 0, "promotionalBonus": 0,
  "lastPaymentFrom": "…"
}
```

`/details` returns `WalletWithUserProfileDto` (wallet + embedded user profile).

## 5. Payments

All require `X-Access-Token`.

### 5.1 Payment Orders — `/api/v1/secured/payments`

| Method | Path | Description |
|---|---|---|
| POST | `/orders` | Create payment order |
| POST | `/initiate` | Initiate online payment (e.g. AddisPay) |
| POST | `/withdraw` | Request withdrawal |
| GET | `/admin/withdrawals?agentId={id}&page=&size=` | Admin: list withdrawals (paginated) |
| GET | `/admin/deposits?agentId={id}&page=&size=` | Admin: list deposits (paginated) |
| GET | `/user/withdrawals?telegramId={id}&agentId={id}&page=&size=` | User withdrawals |
| GET | `/user/deposits?telegramId={id}&agentId={id}&page=&size=` | User deposits |

**`POST /orders` — `PaymentOrderRequestDto`:**

```json
{
  "userId": 5, "agentId": 1, "amount": 100.00, "currency": "ETB",
  "paymentMethodId": 2, "reason": "…", "txnType": "DEPOSIT",
  "phoneNumber": "+2519…", "metadata": { }
}
```

`txnType`: `DEPOSIT` | `WITHDRAWAL` | `WELCOME_BONUS` | `REFERRAL_BONUS` | `PROMOTIONAL_BONUS` | `REFUND`

**Response — `PaymentOrderResponseDto`:**

```json
{
  "orderId": 10, "agentId": 1, "txnRef": "…", "status": "PENDING",
  "amount": 100.00, "providerUuid": "…", "checkoutUrl": "…",
  "instructionsUrl": "…", "checkData": { }, "paymentMethodId": 2
}
```

`status` (`PaymentOrderStatus`): `PENDING` | `INITIATED` | `AWAITING_APPROVAL` | `COMPLETED` | `FAILED` | `CANCELLED` | `REJECTED`

**`POST /initiate` — `PaymentInitiateRequestDto`:**

```json
{
  "uuid": "…", "phoneNumber": "+2519…", "encryptedTotalAmount": "…",
  "merchantName": "…", "selectedService": "…", "selectedBank": "…", "agentId": 1
}
```

Returns `PaymentInitiateResponseDto` `{ message, details, statusCode, data, agentId }`.

**`POST /withdraw` — `WithdrawRequestDto`:**

```json
{
  "telegramId": 123456, "paymentMethodId": 2, "providerPaymentMethodName": "…",
  "phoneNumber": "+2519…", "amount": 50.00, "currency": "ETB",
  "txnType": "WITHDRAWAL", "bankName": "…", "accountName": "…",
  "accountNumber": "…", "withdrawalMode": "OFFLINE",
  "password": "1234", "agentId": 1
}
```

`withdrawalMode`: `ONLINE` | `OFFLINE`. `password` required when the payment method has `withdrawalRequirePassword`. Bank fields required for bank transfers.

**Response — `WithdrawalResponseDto`:** `{ agentId, data, status, detail, message, withdrawalMode }`

### 5.2 Admin Payment Orders — `/api/v1/secured/payment-orders/offline`

| Method | Path | Description |
|---|---|---|
| GET | `/?agentId={id}&status=&txnType=&page=&size=` | List payment orders (paginated) |
| GET | `/{orderId}?agentId={id}` | Order detail (incl. profile, method, wallet) |
| POST | `/approve-withdrawal` | Approve/reject a withdrawal |
| POST | `/promotional-deposit` | Credit promotional bonus |
| POST | `/offline-deposit` | Record offline deposit |

**`POST /approve-withdrawal` — `WithdrawalApprovalRequestDto`:**

```json
{ "orderId": 10, "agentId": 1, "approve": true, "reason": "…" }
```

**`POST /promotional-deposit` — `PromotionalDiscountDepositRequestDto`:**

```json
{ "userTelegramId": 123456, "agentId": 1, "amount": 25.00 }
```

**`POST /offline-deposit` — `OfflineDepositRequestDto`:**

```json
{
  "telegramId": 123456, "agentId": 1, "amount": 100.00,
  "paymentMethodCode": "…", "paymentMethodId": 2,
  "instructionsUrl": "…", "paymentProviderRef": "…", "metadata": { }
}
```

Required: `telegramId`, `amount`, `paymentProviderRef`.

**List response item — `PaymentOrderListDto`:** `{ id, userId, agentId, txnRef, providerOrderRef, amount, currency, status, reason, paymentMethodId, instructionsUrl, txnType, metaData, nonce, phoneNumber, approvedBy, createdAt, updatedAt }`

**Detail response — `PaymentOrderDetailDto`:** same fields plus `userProfile` (`UserProfileDto`), `paymentMethod` (`PaymentMethodMinimalDto`), `wallet` (`WalletDto`).

### 5.3 Payment Methods — `/api/v1/secured/payment-methods`

| Method | Path | Description |
|---|---|---|
| GET | `/?agentId={id}` | List payment methods |
| GET | `/{id}?agentId={id}` | Get by ID |

**Response — `PaymentMethodDto`:**

```json
{
  "id": 2, "code": "TELEBIRR", "name": "Telebirr", "description": "…",
  "isDefault": true, "isOnline": false, "isMobileMoney": true,
  "instructionUrl": "…", "withdrawalRequirePassword": false,
  "logoUrl": "…", "createdAt": "…", "updatedAt": "…"
}
```

### 5.4 Transactions — `/api/v1/secured/transactions`

| Method | Path | Description |
|---|---|---|
| GET | `/?agentId={id}&page=&size=` | Paginated transactions |

**Response item — `TransactionDto`:** `{ id, playerId, agentId, paymentMethodId, txnType, txnAmount, status, txnRef, description, metaData, createdAt }`

`status` (`TransactionStatus`): `PENDING` | `COMPLETED` | `FAILED` | `CANCELLED`

### 5.5 Deposit Transfers — `/api/v1/secured/deposit/transfers`

| Method | Path | Description |
|---|---|---|
| GET | `/{id}?agentId={id}` | Get transfer by ID |
| POST | `/` | Create transfer |
| DELETE | `/{id}?agentId={id}` | Delete transfer |

**`POST /` — `DepositTransferRequestDto`:**

```json
{ "phoneNumber": "+2519…", "amount": 50.00, "agentId": 1 }
```

**Response — `DepositTransferDto`:** `{ id, senderId, agentId, receiverId, amount, status, createdAt, updatedAt }`

`status` (`TransferStatus`): `PENDING` | `SUCCESS` | `FAIL` | `AWAITING_APPROVAL` | `CANCELLED`

## 6. Game Transactions

Base path: `/api/v1/secured/game/transaction` — requires `X-Access-Token` **and** `x-init-data` header (Telegram WebApp initData, HMAC-verified against the agent's bot token).

| Method | Path | Description |
|---|---|---|
| GET | `/?page=&size=` | Paginated game transactions for the authenticated user |
| GET | `/{id}` | Get game transaction by ID |

**Response — `GameTransactionDto`:**

```json
{
  "id": 1, "gameId": 10, "playerId": 5, "agentId": 1,
  "txnAmount": 20.00, "singleGameFee": 10.00, "commissionAmount": 2.00,
  "txnType": "GAME_FEE", "txnStatus": "SUCCESS",
  "createdAt": "…", "updatedAt": "…"
}
```

`txnType` (`GameTxnType`): `GAME_FEE` | `PRIZE_PAYOUT` | `REFUND` | `DISPUTE`
`txnStatus` (`GameTxnStatus`): `PENDING` | `SUCCESS` | `FAIL` | `AWAITING_APPROVAL` | `CANCELLED`

## 7. Rooms

### Public — `/api/v1/public/rooms` (requires `X-Access-Token`)

| Method | Path | Description |
|---|---|---|
| GET | `/{id}` | Get room by ID |
| GET | `/?agentId={id}` | List rooms for agent |

### Secured — `/api/v1/secured/rooms` (requires `X-Access-Token`)

| Method | Path | Description |
|---|---|---|
| POST | `/` | Create room |
| GET | `/{id}` | Get room by ID |
| GET | `/?agentId={id}` | List rooms |
| PUT | `/{id}` | Update room |
| DELETE | `/{id}` | Delete room |

**`POST /` — `RoomCreateDto`:**

```json
{
  "agentId": 1, "name": "Room A", "capacity": 100, "minPlayers": 2,
  "entryFee": 10.00, "pattern": "LINE", "commissionRate": 10.0,
  "botAllowed": true, "minBots": 0, "maxBots": 10, "maxCards": 2,
  "status": "OPEN", "minDraws": 1, "maxDraws": 75, "fakeWinEnabled": false
}
```

Required: `agentId`, `name`, `capacity`, `minPlayers`, `entryFee`, `pattern`.
`pattern` (`GamePattern`): `LINE` | `LINE_AND_CORNERS` | `CORNERS` | `FULL_HOUSE`
`status` (`RoomStatus`): `OPEN` | `CLOSED`. `maxBots` 0–100, `maxCards` 1–2.

**`PUT /{id}` — `RoomUpdateDto`:** same fields (all optional except `id`).

**Response — `RoomDto` / `RoomWithCardPoolDto`:** room fields plus `fakeWinnerId`, `fakeWinnerTelegramId`, `createdBy`, `createdAt`, `updatedAt`; `RoomWithCardPoolDto` adds `cardPool` (`CardInfo[]`) and `allCardIds` (`String[]`).

## 8. Accounting

All require `X-Access-Token`.

### 8.1 Daily — `/api/v1/accounting/daily`

| Method | Path | Description |
|---|---|---|
| GET | `/{id}` | Get record by ID |
| GET | `/` | List all |
| GET | `/agent/{agentId}` | Records for agent |
| GET | `/agent/{agentId}/today` | Today's record for agent |
| PUT | `/{id}` | Update record |
| GET | `/agent/{agentId}/range?startDate=&endDate=` | Date range (YYYY-MM-DD) |
| GET | `/today` | Today's records for all agents |
| PUT | `/{id}/settle` | Mark settled |
| PUT | `/{id}/unsettle` | Mark unsettled |

**`PUT /{id}` — `DailyAccountingUpdateDto`:** `{ accountingDate, dailyDepositAmount, dailyWithdrawalAmount, dailyBetAmount, dailyPrizeAmount, dailyCommissionAmount, dailyBotWinAmount, dailyBotLossAmount, dailyPromotionalBonusAmount, dailyWelcomeBonusAmount, dailyReferralBonusAmount, dailyDepositBonusAmount, settledAt }`

**Response — `DailyAccountingDto`:**

```json
{
  "id": 1, "accountingDate": "2026-09-21",
  "dailyDepositAmount": 0, "dailyWithdrawalAmount": 0,
  "dailyBetAmount": 0, "dailyPrizeAmount": 0, "dailyCommissionAmount": 0,
  "dailyBotWinAmount": 0, "dailyBotLossAmount": 0, "netIncome": 0,
  "dailyPromotionalBonusAmount": 0, "dailyWelcomeBonusAmount": 0,
  "agentId": 1, "settledAt": null, "settledAmount": null,
  "createdAt": "…", "updatedAt": "…"
}
```

### 8.2 Total — `/api/v1/accounting/total`

| Method | Path | Description |
|---|---|---|
| GET | `/{id}` | Get record by ID |
| GET | `/` | List all |
| GET | `/agent/{agentId}` | Record for agent |
| PUT | `/{id}` | Update record |

**`PUT /{id}` — `TotalAccountingUpdateDto`:** `{ totalDepositAmount, totalWithdrawalAmount, totalBetAmount, totalPrizeAmount, totalCommissionAmount, totalBotWinAmount, totalBotLossAmount, totalPromotionalBonusAmount, totalWelcomeBonusAmount, totalReferralBonusAmount, totalDepositBonusAmount, lastSettledAt, settledAmount, nextSettlementTime }`

**Response — `TotalAccountingDto`:** same totals plus `netIncome`, `agentId`, `lastSettledAt`, `lastSettledAmount`, `totalSettledAmount`, `nextSettlementTime`, `createdAt`, `updatedAt`.

## 9. Leaderboard

Base path: `/api/v1/leaderboard` — requires `X-Access-Token`.

| Method | Path | Description |
|---|---|---|
| GET | `/daily?agentId={id}` | Today's daily leaderboard |
| GET | `/daily/{date}?agentId={id}` | Daily leaderboard for date (YYYY-MM-DD) |
| GET | `/total?agentId={id}` | All-time leaderboard |
| GET | `/admin/daily?agentId={id}` | Admin daily leaderboard (incl. bots) |
| GET | `/admin/total?agentId={id}` | Admin total leaderboard (incl. bots) |

**Response — `DailyLeaderboardDto`:** `{ id, userId, userProfile, leaderboardDate, dailyGamesPlayed, dailyWins, dailyPrize, dailyBets, dailyDeposit, dailyWithdrawal, isBot, agentId, createdAt, updatedAt }`

**Response — `TotalLeaderboardDto`:** `{ id, userId, userProfile, totalGamesPlayed, totalWins, totalPrize, totalBets, totalDeposit, totalWithdrawal, isBot, agentId, createdAt, updatedAt }`

`userProfile` is `UserProfileMinimalDto`: `{ id, telegramId, agentId, firstName, lastName, nickname, phoneNumber, isBot }`.

## 10. System Config

Base path: `/api/v1/system-configs` — requires `X-Access-Token`.

| Method | Path | Description |
|---|---|---|
| GET | `/?agentId={id}` | List configs for agent |
| PUT | `/{id}` | Update config value |

**`PUT /{id}` — `SystemConfigUpdateDto`:** `{ "value": "…" }`

**Response — `SystemConfigDto`:** `{ id, agentId, name, value, possibleValues }` (`possibleValues` is comma-separated).

## 11. Autoplay Admin

Base path: `/admin/autoplay` — requires `X-Access-Token`.

| Method | Path | Description |
|---|---|---|
| POST | `/enable` | Enable autoplay |
| POST | `/disable` | Disable autoplay |
| GET | `/status` | Autoplay status |

## 12. Golden Eggs (External Games)

Full details in [`golden-eggs-api.md`](./golden-eggs-api.md). Summary:

### Client-facing — `/external-games/golden-eggs`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/game-modes` | `X-Access-Token` | List game modes (proxied from provider) |
| POST | `/launch` | `X-Access-Token` + `initData` in body | Generate game launch URL |

**`POST /launch` — `LaunchRequest`:**

```json
{ "agentId": 1, "gameMode": "…", "currency": "ETB", "initData": "<telegram-init-data>" }
```

`initData` is HMAC-SHA256 verified against the agent's bot token; `auth_date` freshness enforced (default max age 600s). Currency: **ETB only**.

**Response — `LaunchResponse`:** `{ "gameUrl": "https://…?token=…" }` — token TTL 20 min.

**Errors:** `404 AGENT_NOT_FOUND`, `401 INVALID_INIT_DATA`, `500 CONFIGURATION_ERROR`.

### Agent game settings — `/external-games/game-settings`

| Method | Path | Description |
|---|---|---|
| GET | `/{agentId}` | Get agent's external game settings |
| PUT | `/{agentId}/game-modes` | Update enabled game modes (`UpdateAgentGameModesRequest`) |

### Accounting — `/external-games/golden-eggs/accounting` (requires `agentId` query param)

| Method | Path | Description |
|---|---|---|
| GET | `/total?agentId=` | Total accounting summary |
| GET | `/daily?agentId=` | All daily records |
| GET | `/daily/{date}?agentId=` | Specific date |
| GET | `/daily/range?agentId=&start=&end=` | Date range |
| GET | `/daily/unsettled?agentId=` | Unsettled records |
| GET | `/daily/id/{id}?agentId=` | By ID |
| PUT | `/daily/{id}/settle?agentId=` | Mark settled |
| PUT | `/daily/{id}/unsettle?agentId=` | Mark unsettled |

### Webhook receiver — `/webhooks/golden-eggs` (no `X-Access-Token`; HMAC signature required)

`POST /webhooks/golden-eggs` — receives provider callbacks (`init`, `bet`, `withdraw`, `rollback`, `bonus-complete`, `bonus-expired-when-active`). Requests are verified via HMAC-SHA256 signature header using the configured signature secret; processed idempotently by provider transaction ID.

**Webhook error codes:** `NOT_FOUND`, `ACCOUNT_INVALID`, `INSUFFICIENT_FUNDS`, `CHECKS_FAIL`, `UNKNOWN_ERROR`.

## Multi-tenancy

Nearly every endpoint is scoped by `agentId` (query param or body field). Data is isolated per agent — always pass the correct `agentId`. Users, wallets, rooms, accounting, leaderboards, and payment records are all agent-scoped.

## Common Error Responses

| HTTP | When |
|---|---|
| 400 | Validation failure (`@Valid` body), bad params — `errors` map in envelope |
| 401 | Missing/invalid `X-Access-Token`, or invalid/expired `initData` |
| 404 | Resource not found (agent, user, room, order, etc.) |
| 409 | Conflicts (e.g. duplicate registration, optimistic-lock retry exhaustion) |
| 500 | Unhandled errors — `UNKNOWN_ERROR` / `CONFIGURATION_ERROR` |

