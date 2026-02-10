# Fix for R2DBC Duplicate Key Error in WalletServiceImpl

## Problem
`duplicate key value violates unique constraint "wallet_pkey"` error occurs when debiting/crediting wallets.

## Root Cause
R2DBC treats detached entities (created via `WalletMapper.toEntity()`) as new entities and attempts INSERT instead of UPDATE.

## Solution
Fetch the wallet fresh from the database at the start of both `debit()` and `credit()` methods.

## Changes Required in WalletServiceImpl.java

### 1. Update the `debit()` method (line 266)

**BEFORE:**
```java
public Mono<WalletDto> debit(Wallet wallet, BigDecimal amount, GameTxnType gameTxnType, Long gameId) {
    log.info("Debiting wallet with id: {} for amount: {}", wallet.getId(), amount);

    String walletCacheKey = CacheKeyUtil.getWalletByUserProfileIdAndAgentIdKey(wallet.getUserProfileId(), wallet.getAgentId());
    Mono<Boolean> evictByUserId = cacheService.evict(walletCacheKey);

    Mono<UserProfileDto> userProfileMono = userProfileService.getUserProfileById(wallet.getUserProfileId());

    return evictByUserId
            .then(userProfileMono)
            .flatMap(userProfile -> {
                String walletCacheKeyByTelegram = CacheKeyUtil.getWalletByTelegramIdKey(userProfile.getTelegramId(), wallet.getAgentId());
```

**AFTER:**
```java
public Mono<WalletDto> debit(Wallet wallet, BigDecimal amount, GameTxnType gameTxnType, Long gameId) {
    log.info("Debiting wallet with id: {} for amount: {}", wallet.getId(), amount);

    // Fetch wallet fresh from DB to ensure R2DBC tracks it as existing entity
    return walletRepository.findById(wallet.getId())
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Wallet not found with id: " + wallet.getId())))
            .flatMap(dbWallet -> {
                String walletCacheKey = CacheKeyUtil.getWalletByUserProfileIdAndAgentIdKey(dbWallet.getUserProfileId(), dbWallet.getAgentId());
                Mono<Boolean> evictByUserId = cacheService.evict(walletCacheKey);

                Mono<UserProfileDto> userProfileMono = userProfileService.getUserProfileById(dbWallet.getUserProfileId());

                return evictByUserId
                        .then(userProfileMono)
                        .flatMap(userProfile -> {
                            String walletCacheKeyByTelegram = CacheKeyUtil.getWalletByTelegramIdKey(userProfile.getTelegramId(), dbWallet.getAgentId());
```

**Then replace ALL occurrences of `wallet.` with `dbWallet.` within this method (lines 266-409)**

**Add closing brace before the final closing brace of the method:**
```java
                        .onErrorMap(e -> {
                            log.error("Error debiting wallet", e);
                            return new RuntimeException("Failed to debit wallet", e);
                        });
                }); // <- ADD THIS CLOSING BRACE
    }
```

### 2. Update the `credit()` method (line 412)

Apply the same pattern:
- Add `return walletRepository.findById(wallet.getId()).switchIfEmpty(...).flatMap(dbWallet -> {` at the beginning
- Replace all `wallet.` with `dbWallet.` throughout the method
- Add closing brace before the final `}`

## Manual Steps

1. Open `WalletServiceImpl.java`
2. Find the `debit()` method starting at line 266
3. Add the wallet fetch at the beginning
4. Use Find & Replace (Ctrl+H) with "Match Case" and "Match Whole Word":
   - Find: `wallet\.`
   - Replace: `dbWallet.`
   - **IMPORTANT**: Only replace within the `debit()` method body (lines 266-409)
5. Add the extra closing brace
6. Repeat for `credit()` method (lines 412-575)
7. Compile and test

## Verification
After changes, compile with:
```bash
./mvnw.cmd clean compile -DskipTests
```

Should compile successfully without duplicate key errors.
