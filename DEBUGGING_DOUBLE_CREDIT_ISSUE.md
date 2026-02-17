# Debugging Double Credit Issue - Golden Eggs Integration

## Problem
User account is being credited double the bet amount during integration testing.

## Investigation Steps

### 1. Check Transaction History
Run this query to see all transactions for the affected user:

```sql
SELECT 
    id,
    action,
    provider_transaction_id,
    debit_id,
    game_id,
    user_id,
    amount,
    result,
    coefficient,
    status,
    created_at,
    response_snapshot
FROM external_game_txns 
WHERE user_id = <USER_ID>
ORDER BY created_at DESC 
LIMIT 50;
```

**What to look for:**
- Multiple WITHDRAW transactions with same `provider_transaction_id` (idempotency issue)
- Multiple WITHDRAW transactions with different `provider_transaction_id` but same `debit_id` (provider sending duplicates)
- BET transaction status = 'SUCCESS' (verify bet was deducted)
- WITHDRAW `result` amount matches expected win

### 2. Check for Duplicate Transactions
```sql
SELECT 
    action,
    provider_transaction_id, 
    COUNT(*) as count,
    STRING_AGG(CAST(id AS TEXT), ', ') as transaction_ids
FROM external_game_txns 
WHERE action IN ('WITHDRAW', 'ROLLBACK')
GROUP BY action, provider_transaction_id 
HAVING COUNT(*) > 1;
```

**Expected:** No results (each transaction should be unique)
**If results found:** Idempotency is failing - database constraint not working

### 3. Verify Bet-Withdraw Pairing
```sql
SELECT 
    b.provider_transaction_id as bet_txn_id,
    b.amount as bet_amount,
    b.status as bet_status,
    b.created_at as bet_time,
    w.provider_transaction_id as withdraw_txn_id,
    w.debit_id,
    w.amount as withdraw_bet_amount,
    w.result as withdraw_result,
    w.status as withdraw_status,
    w.created_at as withdraw_time
FROM external_game_txns b
LEFT JOIN external_game_txns w ON b.provider_transaction_id = w.debit_id
WHERE b.action = 'BET' 
  AND b.user_id = <USER_ID>
  AND b.created_at > NOW() - INTERVAL '1 day'
ORDER BY b.created_at DESC;
```

**What to check:**
- Each BET should have corresponding WITHDRAW (if game finished)
- `withdraw_bet_amount` should equal `bet_amount`
- `withdraw_result` should be >= `bet_amount` (win) or 0 (loss)
- No WITHDRAW without matching BET

### 4. Check Wallet Balance Changes
```sql
SELECT 
    t.action,
    t.provider_transaction_id,
    t.amount,
    t.result,
    t.created_at,
    t.status
FROM external_game_txns t
WHERE t.user_id = <USER_ID>
  AND t.created_at > NOW() - INTERVAL '1 hour'
ORDER BY t.created_at ASC;
```

**Calculate expected balance:**
- Start balance: X
- After BET: X - bet_amount
- After WITHDRAW: (X - bet_amount) + result
- **If balance is higher:** Double credit occurred

## Common Causes & Solutions

### Cause 1: Provider Sending Duplicate Webhooks
**Symptom:** Multiple WITHDRAW with different `provider_transaction_id` but same `debit_id`

**Solution:** 
- Check if `debit_id` should be used for idempotency instead of `provider_transaction_id`
- Add additional check: `findByActionAndDebitId()`

### Cause 2: Race Condition
**Symptom:** Two WITHDRAW with same `provider_transaction_id` and both status='SUCCESS'

**Solution:** 
- Already implemented: DuplicateKeyException handling
- Verify database constraint exists: `CONSTRAINT uq_action_provider_txn UNIQUE (action, provider_transaction_id)`

### Cause 3: Bet Not Being Deducted
**Symptom:** BET transaction status='FAILED' but WITHDRAW still processed

**Solution:**
- Add validation: Check if BET exists and is successful before processing WITHDRAW
- Query: `SELECT * FROM external_game_txns WHERE provider_transaction_id = <debitId> AND action = 'BET' AND status = 'SUCCESS'`

### Cause 4: Incorrect Amount Calculation
**Symptom:** `result` amount is wrong

**Check:**
- Per spec: `result` already includes bet amount
- Example: Bet 100, Win 2x = result should be 200 (not 100)
- We credit `result` directly (correct)

## Recommended Fixes

### Fix 1: Add Bet Validation Before Withdraw
```java
// In handleWithdraw, before crediting wallet:
return txnRepository.findByActionAndProviderTransactionId("BET", request.getData().getDebitId())
    .switchIfEmpty(Mono.error(new RuntimeException("BET transaction not found")))
    .filter(betTxn -> "SUCCESS".equals(betTxn.getStatus()))
    .switchIfEmpty(Mono.error(new RuntimeException("BET transaction not successful")))
    .flatMap(betTxn -> {
        // Proceed with withdraw...
    });
```

### Fix 2: Add Debit ID Check for Idempotency
```java
// Check both provider_transaction_id AND debit_id
return txnRepository.findByActionAndProviderTransactionId("WITHDRAW", request.getData().getTransactionId())
    .switchIfEmpty(
        txnRepository.findByActionAndDebitId("WITHDRAW", request.getData().getDebitId())
    )
    .flatMap(existingTxn -> {
        // Return cached response
    });
```

### Fix 3: Add Detailed Logging
Add these logs to track the issue:

```java
log.info("WITHDRAW START: txnId={}, debitId={}, userId={}, betAmount={}, result={}",
    transactionId, debitId, userId, amount, result);

log.info("WITHDRAW CREDIT: txnId={}, userId={}, creditAmount={}, balanceBefore={}",
    transactionId, userId, result, balanceBefore);

log.info("WITHDRAW SUCCESS: txnId={}, userId={}, balanceAfter={}",
    transactionId, userId, balanceAfter);
```

## Testing Checklist

- [ ] Run SQL queries above to identify pattern
- [ ] Check application logs for duplicate webhook calls
- [ ] Verify database constraint exists
- [ ] Test with single game round
- [ ] Monitor wallet balance before/after each transaction
- [ ] Check Golden Eggs provider logs (if accessible)

## Current Implementation Status

✅ **Completed:**
- Currency validation (only ETB accepted)
- Idempotency check before processing
- DuplicateKeyException handling for WITHDRAW
- Database unique constraint on (action, provider_transaction_id)
- Response caching in database

⚠️ **Needs Completion:**
- DuplicateKeyException handling for ROLLBACK
- Enhanced logging for debugging
- Bet validation before withdraw
- Debit ID idempotency check

## Next Steps

1. Run the SQL queries above on your database
2. Share the results to identify the exact pattern
3. Check application logs for any errors or warnings
4. Implement recommended fixes based on findings
