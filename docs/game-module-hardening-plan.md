---
description: Revertible hardening plan for the game module (excludes WS auth and FakeWin bypass design)
---

# Game Module Hardening Plan

Scope: all findings from the module analysis **except** WebSocket authentication (deliberately open) and FakeWinService's bypass design (intentional admin feature).

Ordered safest-first. Every step is a separate commit so it can be reverted independently with `git revert`.

## Phase 0 — Safety Net

- Baseline branch `chore/game-module-hardening` off `main`. One commit per change.
- Smoke checklist before starting: join → select card → countdown → draw → mark → claim → payout. Re-run after each phase.
- Behavior-affecting changes get a `@Value`/`@ConditionalOnProperty` toggle defaulting to current behavior.

## Phase 1 — Zero-Risk Cleanups (no behavior change)

- **1.1** Delete dead files: `service/AutoPlayService.java` (fully commented), `autoplay/AutoPlayService_ORIG.java`, `BingoClaimController.java` (fully commented), `GlobalCardPool.java`, `GameMetrics.java`, `LoggingService.java` — after confirming zero references.
- **1.2** Remove dead methods: `CardPoolService.addSelectedCard`/`removeSelectedCard`/`getSelectedCards`, `RedisKeys.selectedCardsKey`, and `generateAndStoreCurrentPool`/`deleteCurrentCardPool`/`cardExistInRoom`/`getCurrentPool`/`getAllCardIds` if uncalled.
- **1.3** Strip commented-out code blocks in `BingoPatternVerifier`, `CardPoolService`, `PlayerStateService`, `GameStateSyncService`, `AutoPlayService_MULTI_AGENT`, `AutoPlayScheduleConfig`.
- **1.4** Replace `System.out.println` in `BingoCardGenerator` with `log.debug`.

## Phase 2 — Latent Bug Fixes (paths currently unused or broken)

- **2.1** `BingoClaimRepository`: `bingo_claim` → `bingo_claims`, `created_at` → `create_at` (match schema).
- **2.2** `RoomController.getRoomById`: `@RequestParam Long id` → `@PathVariable Long id` (verify no caller uses `?id=`).
- **2.3** `getMinPlayersToStart`: null-check `minPlayers`; stop wrapping `ResourceNotFoundException` in `RuntimeException`.

## Phase 3 — Cache & Multi-Tenancy (flag-guarded)

- **3.1** Per-agent cache key for `getAllRoomsWIthCardPool(agentId)` (e.g. `rooms:withCardPool:agent:{agentId}`); keep global key only for the unfiltered autoplay variant.
- **3.2** `updateRoomById`: also evict `getRoomWithCardPoolKey(id)` and `getRoomsWithCardPoolKey()` (+ new per-agent key) when `cardPoolJson` is regenerated.
- **3.3** Agent-ownership checks in `updateRoomById`/`deleteRoomById`/`getRoomById` behind `security.room-ownership-check` (default `false`); enable in test → monitor 403s → prod.

## Phase 4 — Resource Leak Fixes (additive)

- **4.1** `FakeWinService.refreshRoomsFromDb`: dispose `roomSubscription` before removing runtimes.
- **4.2** `AutoPlayService`: dispose `listenToChannel` subscription and remove runtime when a room leaves the active set.
- **4.3** TTL on `fakewin:{roomId}:drawnNumbers` (e.g. 2h, matching `triggeredKey`).
- **4.4** On `game.ended`/`bingoClaimed`: delete `playerMarkedNumbersKey(gameId, fakeUserId, fakeCardId)` and remove fake user from `gamePlayersKey`.

## Phase 5 — Concurrency Hardening (flag-guarded, one per commit)

- **5.1** Resolve duplicate AutoPlay beans: keep `AutoPlayService_MULTI_AGENT`, remove/condition `autoplay/AutoPlayService.java`; replace `flushDb()` with targeted `autoplay:*` key deletion. Highest risk — dedicated deploy window + rollback plan.
- **5.2** Ownership-checked lock release in `CardSelectionService` Lua scripts (token compare before `DEL`), behind `cardsel.owner-checked-locks`.
- **5.3** `GameState`: `drawnNumbers` → concurrent set or synchronized setter; `stopNumberDrawing`/`claimRequested` → `volatile`.
- **5.4** `markNumber`/`unmarkNumber`: verify cardId ∈ `playerCardsIdsKey` and number ∈ drawn set, flag-guarded.

## Phase 6 — Deferred / Needs Decision

- `PlayerStateService` TTL vs max game duration (extend or refresh on mark).
- `GameMapper`: `playersCount=1` when empty; bot cards inflating `entriesCount`/prize — needs accounting sign-off.
- `RedisKeys` `-`→`_` collision — requires versioned key-prefix migration.
- `InMemoryRoomRegistry` default — switch to `redis` when scaling to multi-instance.
- `AutoPlayAdminController` role check — when roles model is finalized.
- `BingoPatternVerifier` hardening — guard `numbers.add(2, 0)`, add duplicate/count checks.

## Revert Strategy

- Separate commits → `git revert <sha>` per change.
- Phases 3 & 5 behind config flags → rollback via `application.yml`.
- Phase 5.1 gets its own deploy window with rollback ready.
