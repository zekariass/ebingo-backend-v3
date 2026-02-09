# Golden Eggs Bonus System Implementation Guide

## ✅ Completed Components

### 1. Entities Created
- ✅ `GoldenEggsBonus` - Main bonus entity with all fields
- ✅ `GoldenEggsBonusTransaction` - Bonus transaction tracking

### 2. DTOs Created
- ✅ `BonusConfigs` - All game config classes (ChickenRoad, LuckyMines, ChickenRoadTwo, Plinko1000, ForestFortune)
- ✅ `BonusDTOs` - All request/response DTOs (Create, Fetch, Cancel, Webhooks)

### 3. Repositories Created
- ✅ `GoldenEggsBonusRepository` - Bonus CRUD operations
- ✅ `GoldenEggsBonusTransactionRepository` - Transaction tracking

### 4. Utilities Created
- ✅ `GoldenEggsBonusUtil` - SubOperatorId generation & Request signing

## 📋 Remaining Implementation Tasks

### Service Layer (GoldenEggsBonusService.java)

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class GoldenEggsBonusService {
    private final GoldenEggsBonusRepository bonusRepository;
    private final GoldenEggsBonusTransactionRepository transactionRepository;
    private final ExternalGameWalletService walletService;
    private final GoldenEggsConfig config;
    private final WebClient goldenEggsWebClient;
    private final ObjectMapper objectMapper;

    // Create bonus
    public Mono<CreateBonusResponse> createBonus(String subId, CreateBonusRequest request) {
        UUID subOperatorId = GoldenEggsBonusUtil.generateSubOperatorId(
            config.getAggregatorId(), subId);
        
        // Build bonus entity
        // Save to database
        // Call provider API with X-REQUEST-SIGN header
        // Return response
    }

    // Fetch bonuses with filters
    public Mono<FetchBonusesResponse> fetchBonuses(String subId, Map<String, String> filters) {
        // Generate subOperatorId
        // Call provider API with filters
        // Return paginated response
    }

    // View single bonus
    public Mono<BonusDTO> viewBonus(String subId, UUID bonusId) {
        // Generate subOperatorId
        // Call provider API
        // Return bonus details
    }

    // Cancel bonus
    public Mono<CancelBonusResponse> cancelBonus(String subId, UUID bonusId) {
        // Generate subOperatorId
        // Call provider DELETE API
        // Update local database
        // Return response
    }

    // Handle bonus-complete webhook
    public Mono<BonusWebhookResponse> handleBonusComplete(BonusWebhookRequest request) {
        // Check idempotency
        // Credit wallet with winSum
        // Update bonus status
        // Return balance
    }

    // Handle bonus-expired-when-active webhook
    public Mono<BonusWebhookResponse> handleBonusExpired(BonusWebhookRequest request) {
        // Check idempotency
        // Update bonus status
        // Return balance (no credit)
    }
}
```

### Controller Layer (GoldenEggsBonusController.java)

```java
@RestController
@RequestMapping("/external-games/golden-eggs/bonuses")
@RequiredArgsConstructor
@Slf4j
public class GoldenEggsBonusController {
    private final GoldenEggsBonusService bonusService;

    @PostMapping("/{subId}")
    public Mono<CreateBonusResponse> createBonus(
        @PathVariable String subId,
        @Valid @RequestBody CreateBonusRequest request) {
        return bonusService.createBonus(subId, request);
    }

    @GetMapping("/{subId}")
    public Mono<FetchBonusesResponse> fetchBonuses(
        @PathVariable String subId,
        @RequestParam Map<String, String> filters) {
        return bonusService.fetchBonuses(subId, filters);
    }

    @GetMapping("/{subId}/{bonusId}")
    public Mono<BonusDTO> viewBonus(
        @PathVariable String subId,
        @PathVariable UUID bonusId) {
        return bonusService.viewBonus(subId, bonusId);
    }

    @DeleteMapping("/{subId}/{bonusId}")
    public Mono<CancelBonusResponse> cancelBonus(
        @PathVariable String subId,
        @PathVariable UUID bonusId) {
        return bonusService.cancelBonus(subId, bonusId);
    }
}
```

### Webhook Handler (Update GoldenEggsWebhookController.java)

```java
// Add to existing webhook controller
public Mono<Object> handleWebhook(WebhookRequest request) {
    return switch (request.getAction()) {
        case "bonus-complete" -> bonusService.handleBonusComplete(request);
        case "bonus-expired-when-active" -> bonusService.handleBonusExpired(request);
        // ... existing cases
    };
}
```

### Database Migration (V4__golden_eggs_bonus.sql)

```sql
CREATE TABLE golden_eggs_bonus (
    bonus_id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL,
    sub_operator_id UUID NOT NULL,
    game_modes TEXT NOT NULL, -- JSON array
    currency VARCHAR(10) NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'FREEBET',
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    bonus_quantity INT NOT NULL,
    bonus_available INT NOT NULL,
    win_sum DECIMAL(20, 9) DEFAULT 0,
    freebet_config TEXT NOT NULL, -- JSON
    expires_at TIMESTAMP NOT NULL,
    expires_when_active_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bonus_user FOREIGN KEY (user_id) REFERENCES user_profile(id)
);

CREATE INDEX idx_bonus_sub_operator ON golden_eggs_bonus(sub_operator_id);
CREATE INDEX idx_bonus_user ON golden_eggs_bonus(user_id);
CREATE INDEX idx_bonus_status ON golden_eggs_bonus(status);

CREATE TABLE golden_eggs_bonus_transaction (
    id UUID PRIMARY KEY,
    bonus_id UUID NOT NULL,
    transaction_id UUID NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    win_sum DECIMAL(20, 9) NOT NULL,
    game_mode VARCHAR(50),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bonus_txn_bonus FOREIGN KEY (bonus_id) REFERENCES golden_eggs_bonus(bonus_id),
    CONSTRAINT fk_bonus_txn_user FOREIGN KEY (user_id) REFERENCES user_profile(id)
);

CREATE INDEX idx_bonus_txn_transaction ON golden_eggs_bonus_transaction(transaction_id);
CREATE INDEX idx_bonus_txn_bonus ON golden_eggs_bonus_transaction(bonus_id);
```

### Configuration Updates

```yaml
# application.yml
golden-eggs:
  operator-id: ${GOLDEN_EGGS_OPERATOR_ID:your-operator-id}
  aggregator-id: ${GOLDEN_EGGS_AGGREGATOR_ID:your-aggregator-id}
  api-base-url: ${GOLDEN_EGGS_API_BASE_URL:https://api.golden-eggs.games}
  signature-secret: ${GOLDEN_EGGS_SIGNATURE_SECRET:your-secret-key}
  bonus-signature-key: ${GOLDEN_EGGS_BONUS_SIGNATURE_KEY:your-bonus-signature-key}
```

## 🔑 Key Implementation Details

### 1. Request Signing

All bonus API calls MUST include `X-REQUEST-SIGN` header:

```java
String signature = GoldenEggsBonusUtil.generateRequestSignature(
    subOperatorId.toString(),
    config.getBonusSignatureKey()
);

webClient.post()
    .uri("/api/operator/v1/bonuses/" + subOperatorId)
    .header("X-REQUEST-SIGN", signature)
    .bodyValue(request)
    .retrieve()
    .bodyToMono(CreateBonusResponse.class);
```

### 2. SubOperatorId Generation

MUST be deterministic for same aggregatorId + subId:

```java
UUID subOperatorId = GoldenEggsBonusUtil.generateSubOperatorId(
    "ee2013ed-e1f0-4d6e-97d2-f36619e2eb88",  // aggregatorId
    "brand-123"                               // subId
);
// Result: d48ce2b5-2a56-4de7-952f-12254f0d2dd2
```

### 3. Webhook Idempotency

Check transaction_id before processing:

```java
return transactionRepository.findSuccessfulTransaction(request.getData().getTransactionId())
    .flatMap(existing -> Mono.just(cachedResponse))
    .switchIfEmpty(processNewTransaction(request));
```

### 4. WinSum Handling

Per spec: winSum INCLUDES bet amount

```java
// Example: 5 freebets @ 100 USD each
// Lost 4, won 1 with 150 USD total
// winSum = 150 USD (not 50 USD)
BigDecimal winSum = new BigDecimal(request.getData().getWinSum());
walletService.creditExternalGame(userId, winSum, currency, ...);
```

## 🧪 Testing Requirements

### Unit Tests

```java
@Test
void testSubOperatorIdGeneration() {
    UUID result = GoldenEggsBonusUtil.generateSubOperatorId(
        "ee2013ed-e1f0-4d6e-97d2-f36619e2eb88",
        "brand-123"
    );
    assertEquals("d48ce2b5-2a56-4de7-952f-12254f0d2dd2", result.toString());
}

@Test
void testRequestSignature() {
    String signature = GoldenEggsBonusUtil.generateRequestSignature(
        "6295c404-e633-48cc-ae14-8ca0880d55d4",
        "7A2DB2F4FE86998735835F538826B262E834662EA297AB8B4286DBFE315A2467521D791A94E3D12A942427A29F"
    );
    assertEquals("6548c3439d482c6b330d421aca1e9947bfd80da15286a6b8791ef39579d3fcae", signature);
}
```

### Integration Tests

- Create bonus flow
- Fetch with filters
- Cancel bonus
- Webhook handling with idempotency
- Signature validation

## 📚 API Examples

### Create Bonus

```bash
POST /external-games/golden-eggs/bonuses/brand-123
{
  "userId": "21704",
  "bonusId": "8eb034e0-ca4c-44e3-a9a4-692d71b261fe",
  "gameModes": ["lucky-mines"],
  "currency": "USD",
  "expiresAt": "2024-07-17T13:31:20.873Z",
  "type": "FREEBET",
  "freebetConfig": {
    "count": "10",
    "luckyMinesConfig": {
      "betAmount": "1000",
      "minesCount": "20"
    }
  }
}
```

### Fetch Bonuses

```bash
GET /external-games/golden-eggs/bonuses/brand-123?filter[userId]=123456&filter[status]=ACTIVE&pageable[page]=1&pageable[limit]=10
```

### Cancel Bonus

```bash
DELETE /external-games/golden-eggs/bonuses/brand-123/8eb034e0-ca4c-44e3-a9a4-692d71b261fe
```

## ✅ Implementation Checklist

- [x] Create entities (GoldenEggsBonus, GoldenEggsBonusTransaction)
- [x] Create DTOs (BonusConfigs, BonusDTOs)
- [x] Create repositories
- [x] Create utility (SubOperatorId, Signature)
- [ ] Create service layer
- [ ] Create controller
- [ ] Update webhook controller
- [ ] Create database migration
- [ ] Add configuration
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Create documentation

## 🚀 Next Steps

1. Create `GoldenEggsBonusService.java` with all methods
2. Create `GoldenEggsBonusController.java`
3. Update `GoldenEggsWebhookController.java` for bonus webhooks
4. Create migration `V4__golden_eggs_bonus.sql`
5. Add `bonusSignatureKey` to config
6. Write comprehensive tests
7. Test with provider sandbox

## 📝 Notes

- All bonus operations are fully reactive (Spring WebFlux)
- Request signing is MANDATORY for all API calls
- SubOperatorId generation MUST be deterministic
- Webhook idempotency is critical
- WinSum includes bet amount per spec
- hideFromStat is optional in webhook response
