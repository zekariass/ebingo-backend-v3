# Accounting API — Client Integration Guide

Base URL: `/api/v1/accounting`

All endpoints are secured with the `X-Access-Token` header (when `endpoints.access-token` is configured on the server).

```
X-Access-Token: <token>
```

## Common Response Envelope

Every endpoint returns `ApiResponse<T>`:

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Daily accounting retrieved successfully",
  "error": null,
  "errors": null,
  "path": "/api/v1/accounting/daily/agent/1",
  "timestamp": "2026-09-23T20:00:00Z",
  "data": { }
}
```

Paginated endpoints wrap results in `PageResponse<T>`:

```json
{
  "content": [ ],
  "page": 0,
  "size": 10,
  "totalElements": 42
}
```

> **Pagination note:** these endpoints use **0-indexed** pages (`page=0` is the first page). This differs from the payment-orders endpoint which is 1-indexed.

---

# Daily Accounting — `/api/v1/accounting/daily`

Per-agent, per-day accounting records. One record per agent per `accountingDate`.

## DailyAccountingDto

| Field | Type | Description |
|---|---|---|
| `id` | number | Record ID |
| `accountingDate` | string `YYYY-MM-DD` | The day this record covers |
| `dailyDepositAmount` | number | Total deposits that day |
| `dailyWithdrawalAmount` | number | Total withdrawals that day |
| `dailyBetAmount` | number | Total bets placed that day |
| `dailyPrizeAmount` | number | Total prizes paid that day |
| `dailyCommissionAmount` | number | Commission earned that day |
| `dailyBotWinAmount` | number | Amount bots won that day |
| `dailyBotLossAmount` | number | Amount bots lost that day |
| `netIncome` | number | Net income for the day |
| `dailyPromotionalBonusAmount` | number | Promotional bonuses given |
| `dailyWelcomeBonusAmount` | number | Welcome bonuses given |
| `agentId` | number | Owning agent |
| `settledAt` | string datetime \| null | When the record was settled |
| `settledAmount` | number \| null | Amount paid at settlement |
| `createdAt` / `updatedAt` | string datetime | Audit timestamps |

## Endpoints

### Get record by ID

```
GET /api/v1/accounting/daily/{id}
```

Response: `ApiResponse<DailyAccountingDto>`

### Get today's records for all agents (admin)

```
GET /api/v1/accounting/daily?page=0&size=10
GET /api/v1/accounting/daily/today?page=0&size=10
```

Both paths return the same data — today's `DailyAccountingDto` for every agent.

Response: `ApiResponse<PageResponse<DailyAccountingDto>>`

### Get records for one agent (paginated history)

```
GET /api/v1/accounting/daily/agent/{agentId}?page=0&size=10
```

Response: `ApiResponse<PageResponse<DailyAccountingDto>>`

### Get today's record for one agent

```
GET /api/v1/accounting/daily/agent/{agentId}/today
```

Response: `ApiResponse<DailyAccountingDto>` — single record, not paginated.

### Get records for one agent in a date range

```
GET /api/v1/accounting/daily/agent/{agentId}/date-range?startDate=2026-09-01&endDate=2026-09-23&page=0&size=10
```

- `startDate`, `endDate` — required, format `YYYY-MM-DD`. Dates cannot be in the future.

Response: `ApiResponse<PageResponse<DailyAccountingDto>>`

### Update a record (admin)

```
PUT /api/v1/accounting/daily/{id}
Content-Type: application/json
```

Body (`DailyAccountingUpdateDto` — all fields optional, only provided fields are updated):

```json
{
  "accountingDate": "2026-09-23",
  "dailyDepositAmount": 1000.00,
  "dailyWithdrawalAmount": 500.00,
  "dailyBetAmount": 2000.00,
  "dailyPrizeAmount": 1500.00,
  "dailyCommissionAmount": 100.00,
  "dailyBotWinAmount": 0,
  "dailyBotLossAmount": 0,
  "dailyPromotionalBonusAmount": 0,
  "dailyWelcomeBonusAmount": 0,
  "dailyReferralBonusAmount": 0,
  "dailyDepositBonusAmount": 0,
  "settledAt": "2026-09-23T18:00:00"
}
```

Response: `ApiResponse<DailyAccountingDto>`

### Settle a record (admin)

```
PUT /api/v1/accounting/daily/{id}/settle
```

Marks the record as settled. Only records **not from today** and **not already settled** can be settled.

Response: `ApiResponse<DailyAccountingDto>`

---

# Total Accounting — `/api/v1/accounting/total`

Cumulative per-agent accounting. One record per agent, all-time totals.

## TotalAccountingDto

| Field | Type | Description |
|---|---|---|
| `id` | number | Record ID |
| `totalDepositAmount` | number | All-time deposits |
| `totalWithdrawalAmount` | number | All-time withdrawals |
| `totalBetAmount` | number | All-time bets |
| `totalPrizeAmount` | number | All-time prizes paid |
| `totalCommissionAmount` | number | All-time commission |
| `totalBotWinAmount` | number | All-time bot winnings |
| `totalBotLossAmount` | number | All-time bot losses |
| `totalPromotionalBonusAmount` | number | All-time promotional bonuses |
| `totalWelcomeBonusAmount` | number | All-time welcome bonuses |
| `netIncome` | number | All-time net income |
| `agentId` | number | Owning agent |
| `lastSettledAt` | string datetime \| null | Last settlement time |
| `lastSettledAmount` | number \| null | Last settlement amount |
| `totalSettledAmount` | number | Cumulative settled amount |
| `createdAt` / `updatedAt` | string datetime | Audit timestamps |

## Endpoints

### Get record by ID

```
GET /api/v1/accounting/total/{id}
```

Response: `ApiResponse<TotalAccountingDto>`

### Get all records (admin, paginated + sorted)

```
GET /api/v1/accounting/total?page=0&size=10&sortBy=netIncome
```

- `sortBy` — optional, one of: `id`, `netIncome`, `lastSettledAt`, `createdAt`, `updatedAt`. Default `id`.

Response: `ApiResponse<PageResponse<TotalAccountingDto>>`

### Get record for one agent

```
GET /api/v1/accounting/total/agent/{agentId}
```

Response: `ApiResponse<TotalAccountingDto>` — single record, not paginated.

### Update a record (admin)

```
PUT /api/v1/accounting/total/{id}
Content-Type: application/json
```

Body (`TotalAccountingUpdateDto` — all fields optional):

```json
{
  "totalDepositAmount": 50000.00,
  "totalWithdrawalAmount": 20000.00,
  "totalBetAmount": 100000.00,
  "totalPrizeAmount": 80000.00,
  "totalCommissionAmount": 5000.00,
  "totalBotWinAmount": 0,
  "totalBotLossAmount": 0,
  "totalPromotionalBonusAmount": 0,
  "totalWelcomeBonusAmount": 0,
  "totalReferralBonusAmount": 0,
  "totalDepositBonusAmount": 0,
  "lastSettledAt": "2026-09-23T18:00:00",
  "settledAmount": 1000.00
}
```

Response: `ApiResponse<TotalAccountingDto>`

---

# Quick Reference

| Method | Path | Scope | Returns |
|---|---|---|---|
| GET | `/accounting/daily/{id}` | admin | `DailyAccountingDto` |
| GET | `/accounting/daily` | admin | `PageResponse<DailyAccountingDto>` (today, all agents) |
| GET | `/accounting/daily/today` | admin | `PageResponse<DailyAccountingDto>` (today, all agents) |
| GET | `/accounting/daily/agent/{agentId}` | agent | `PageResponse<DailyAccountingDto>` |
| GET | `/accounting/daily/agent/{agentId}/today` | agent | `DailyAccountingDto` |
| GET | `/accounting/daily/agent/{agentId}/date-range` | agent | `PageResponse<DailyAccountingDto>` |
| PUT | `/accounting/daily/{id}` | admin | `DailyAccountingDto` |
| PUT | `/accounting/daily/{id}/settle` | admin | `DailyAccountingDto` |
| GET | `/accounting/total/{id}` | admin | `TotalAccountingDto` |
| GET | `/accounting/total` | admin | `PageResponse<TotalAccountingDto>` |
| GET | `/accounting/total/agent/{agentId}` | agent | `TotalAccountingDto` |
| PUT | `/accounting/total/{id}` | admin | `TotalAccountingDto` |
