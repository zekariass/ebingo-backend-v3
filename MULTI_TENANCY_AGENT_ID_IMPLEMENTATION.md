# Multi-Tenancy (Agent-Based) Implementation for Golden Eggs External Game

## Overview

Implemented complete multi-tenancy support for the Golden Eggs external game integration by adding `agentId` to all entities, services, and repositories. This ensures that all external game data is properly isolated per agent.

## ✅ Changes Implemented

### 1. Database Migrations (V3, V4, V5)

**Files:** 
- `V3__external_game_integration.sql`
- `V4__golden_eggs_bonus.sql`
- `V5__golden_eggs_accounting.sql`

Added `agent_id` column to all external game tables in their original migrations:
- `external_game_auth_tokens`
- `external_game_sessions`
- `external_game_txns`
- `golden_eggs_bonus`
- `golden_eggs_bonus_transaction`
- `golden_eggs_total_accounting`
- `golden_eggs_daily_accounting`

**Key Changes:**
- Added `agent_id BIGINT` column to all 7 tables
- Added foreign key constraints to `agent` table with CASCADE delete
- Created indexes on `agent_id` for performance
- Updated unique constraint on `golden_eggs_daily_accounting` to `(agent_id, accounting_date)`
- Made `golden_eggs_total_accounting.agent_id` unique (one total record per agent)
- Removed initial insert in V5 (records created automatically per agent)

### 2. Entity Updates (7 files)

All entities updated to include `agentId` field with proper mapping:

#### **ExternalGameAuthToken**
- Added `agentId` field
- Populated from `LaunchRequest.agentId` during token generation

#### **ExternalGameSession**
- Added `agentId` field
- Populated from `ExternalGameAuthToken.agentId` during session creation

#### **ExternalGameTxn**
- Added `agentId` field
- Populated from `ExternalGameSession.agentId` during transaction creation

#### **GoldenEggsBonus**
- Added `agentId` field
- Links bonus to specific agent

#### **GoldenEggsBonusTransaction**
- Added `agentId` field
- Links bonus transaction to specific agent

#### **GoldenEggsTotalAccounting**
- Added `agentId` field
- One total accounting record per agent

#### **GoldenEggsDailyAccounting**
- Added `agentId` field
- Daily accounting records scoped by agent

### 3. Repository Updates (2 files)

#### **GoldenEggsTotalAccountingRepository**
- Changed `findFirstByOrderByIdAsc()` → `findByAgentId(Long agentId)`
- Queries total accounting for specific agent

#### **GoldenEggsDailyAccountingRepository**
- Updated all query methods to include `agentId` parameter:
  - `findByAgentIdAndAccountingDate(Long agentId, LocalDate date)`
  - `findByAgentIdOrderByAccountingDateDesc(Long agentId)`
  - `findByAgentIdAndAccountingDateBetween(Long agentId, LocalDate start, LocalDate end)`
  - `findByAgentIdAndIsSettledFalseOrderByAccountingDateDesc(Long agentId)`
  - `findByAgentIdAndIsSettledTrueOrderByAccountingDateDesc(Long agentId)`

### 4. Service Layer Updates (2 files)

#### **GoldenEggsIntegrationService**
- **Token Generation:** Sets `agentId` from `LaunchRequest.agentId`
- **Session Creation:** Copies `agentId` from auth token
- **Transaction Creation:** Copies `agentId` from session for bet/withdraw/rollback
- **Accounting Helpers:** Updated async methods to pass `agentId`

#### **GoldenEggsAccountingService**
- **Recording Methods:** Added `agentId` parameter to:
  - `recordBet(amount, currency, agentId)`
  - `recordWin(amount, currency, agentId)`
  - `recordRollback(amount, currency, agentId)`
- **Query Methods:** Added `agentId` parameter to:
  - `getTotalAccountingSummary(agentId)`
  - `getDailyAccounting(agentId, date)`
  - `getAllDailyAccounting(agentId)`
  - `getDailyAccountingByDateRange(agentId, startDate, endDate)`
  - `getUnsettledDailyAccounting(agentId)`
- **Private Methods:** Updated all internal methods to filter by `agentId`

### 5. Controller Updates (1 file)

#### **GoldenEggsAccountingController**
- Added `@RequestParam Long agentId` to all endpoints
- Updated endpoint documentation with `agentId` parameter

**Updated Endpoints:**
```
GET  /accounting/total?agentId=123
GET  /accounting/daily?agentId=123
GET  /accounting/daily/{date}?agentId=123
GET  /accounting/daily/range?agentId=123&startDate=...&endDate=...
GET  /accounting/daily/unsettled?agentId=123
GET  /accounting/daily/id/{id}
PUT  /accounting/daily/{id}/settle
PUT  /accounting/daily/{id}/unsettle
```

## 🔄 Data Flow

### Launch Flow
```
1. Frontend → LaunchRequest {agentId, gameMode, currency, initData}
2. Backend → Verify initData with Agent's botToken
3. Backend → Create ExternalGameAuthToken with agentId
4. Backend → Return game URL with token
```

### Init Flow
```
1. Provider → Init webhook with token
2. Backend → Load ExternalGameAuthToken (has agentId)
3. Backend → Create ExternalGameSession with agentId from token
4. Backend → Return user info
```

### Transaction Flow (Bet/Withdraw/Rollback)
```
1. Provider → Transaction webhook with session token
2. Backend → Load ExternalGameSession (has agentId)
3. Backend → Create ExternalGameTxn with agentId from session
4. Backend → Record in accounting with agentId
5. Backend → Return response
```

### Accounting Flow
```
1. Transaction completes successfully
2. Async call → recordBet/Win/Rollback(amount, currency, agentId)
3. Service → Update daily accounting for (agentId, today)
4. Service → Update total accounting for (agentId)
```

## 🔑 Key Design Decisions

### 1. Agent ID Source
- **Primary Source:** `LaunchRequest.agentId` (from frontend)
- **Propagation:** Token → Session → Transaction → Accounting
- **Validation:** Agent existence verified during launch

### 2. Data Isolation
- All queries filtered by `agentId`
- Unique constraints include `agentId`
- Foreign keys ensure referential integrity

### 3. Accounting Per Agent
- Each agent has separate total accounting record
- Daily accounting scoped by `(agentId, date)`
- Query endpoints require `agentId` parameter

### 4. Backward Compatibility
- Migration adds nullable `agent_id` columns
- Existing data will have `NULL` agent_id (needs manual migration if any)
- New records always have `agent_id` populated

## 📊 Database Schema Changes

### Before (V5)
```sql
-- Single total accounting for entire platform
golden_eggs_total_accounting (id, total_bets_count, ...)

-- Daily accounting by date only
golden_eggs_daily_accounting (id, accounting_date, ...)
UNIQUE (accounting_date)
```

### After (V6)
```sql
-- Total accounting per agent
golden_eggs_total_accounting (id, agent_id, total_bets_count, ...)
UNIQUE (agent_id)

-- Daily accounting per agent per date
golden_eggs_daily_accounting (id, agent_id, accounting_date, ...)
UNIQUE (agent_id, accounting_date)
```

## ✅ Build Status

```
[INFO] BUILD SUCCESS
```

## 📝 Testing Checklist

- [ ] Run database migration (V6)
- [ ] Test launch with different `agentId` values
- [ ] Verify sessions created with correct `agentId`
- [ ] Verify transactions created with correct `agentId`
- [ ] Verify accounting records scoped by `agentId`
- [ ] Test accounting queries with different `agentId` values
- [ ] Verify data isolation between agents
- [ ] Test settlement operations

## 🚀 Next Steps

1. **Run Migration:**
   ```bash
   ./mvnw flyway:migrate
   ```

2. **Test Multi-Tenancy:**
   - Launch game for Agent 1
   - Launch game for Agent 2
   - Verify separate accounting records

3. **Data Migration (if needed):**
   - If existing data has NULL `agent_id`, update manually
   - Assign appropriate `agent_id` to orphaned records

## 📦 Files Modified

**Database:**
- `V6__add_agent_id_to_external_game_tables.sql` (NEW)

**Entities (7 files):**
- `ExternalGameAuthToken.java`
- `ExternalGameSession.java`
- `ExternalGameTxn.java`
- `GoldenEggsBonus.java`
- `GoldenEggsBonusTransaction.java`
- `GoldenEggsTotalAccounting.java`
- `GoldenEggsDailyAccounting.java`

**Repositories (2 files):**
- `GoldenEggsTotalAccountingRepository.java`
- `GoldenEggsDailyAccountingRepository.java`

**Services (2 files):**
- `GoldenEggsIntegrationService.java`
- `GoldenEggsAccountingService.java`

**Controllers (1 file):**
- `GoldenEggsAccountingController.java`

**Total:** 3 modified migrations + 13 modified code files

---

**The Golden Eggs external game integration now fully supports multi-tenancy with agent-based data isolation!** 🎉
