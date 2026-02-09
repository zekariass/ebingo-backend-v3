# Golden Eggs Bonus Creation Flow

## Overview

This document describes the complete bonus creation flow, including local state management and provider integration.

## Bonus Creation Flow

### Step 1: Platform Creates Bonus Request

The platform (this backend) creates a bonus request with `freebetConfig` containing game-specific configurations:

```json
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

### Step 2: Save Locally with PENDING Status

Before calling the provider, the bonus is saved locally with **PENDING** status:

```java
GoldenEggsBonus bonus = GoldenEggsBonus.builder()
    .bonusId(request.getBonusId())
    .userId(Long.parseLong(request.getUserId()))
    .subOperatorId(subOperatorId)
    .gameModes(serializeGameModes(request.getGameModes()))
    .currency(request.getCurrency())
    .type(request.getType())
    .status("PENDING") // Initial status
    .bonusQuantity(Integer.parseInt(request.getFreebetConfig().getCount()))
    .bonusAvailable(Integer.parseInt(request.getFreebetConfig().getCount()))
    .winSum(BigDecimal.ZERO)
    .freebetConfig(serializeFreebetConfig(request.getFreebetConfig()))
    .expiresAt(request.getExpiresAt())
    .createdAt(Instant.now())
    .updatedAt(Instant.now())
    .build();

bonusRepository.save(bonus);
```

**Why PENDING?**
- Indicates the bonus creation is in progress
- Allows tracking of failed creation attempts
- Provides audit trail of all bonus creation requests

### Step 3: Generate SubOperatorId

The `subOperatorId` is deterministically generated from `aggregatorId` and `subId`:

```java
UUID subOperatorId = GoldenEggsBonusUtil.generateSubOperatorId(
    config.getAggregatorId(),  // e.g., "ee2013ed-e1f0-4d6e-97d2-f36619e2eb88"
    subId                       // e.g., "brand-123"
);
// Result: d48ce2b5-2a56-4de7-952f-12254f0d2dd2
```

**Algorithm:**
1. Concatenate: `aggregatorId:subId`
2. Generate MD5 hash
3. Convert to UUID v4 format

### Step 4: Generate Request Signature

All bonus API calls require `X-REQUEST-SIGN` header:

```java
String signature = GoldenEggsBonusUtil.generateRequestSignature(
    subOperatorId.toString(),
    config.getSignatureSecret()
);
// HMAC-SHA256(operatorId, signatureKey) in hex lowercase
```

### Step 5: Call Provider API

Send POST request to provider:

```http
POST https://api.golden-eggs.games/api/operator/v1/bonuses/{subOperatorId}
X-REQUEST-SIGN: 6548c3439d482c6b330d421aca1e9947bfd80da15286a6b8791ef39579d3fcae
Content-Type: application/json

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

### Step 6: Update Local Status Based on Response

**Success Response:**
```json
{
  "status": true
}
```

→ Update local bonus status to **CREATED**

**Failure Response:**
```json
{
  "status": false
}
```

→ Update local bonus status to **FAILED**

**Error (Network/Timeout):**

→ Update local bonus status to **FAILED**

### Implementation Code

```java
public Mono<CreateBonusResponse> createBonus(String subId, CreateBonusRequest request) {
    UUID subOperatorId = GoldenEggsBonusUtil.generateSubOperatorId(
        config.getAggregatorId(), subId);
    String signature = GoldenEggsBonusUtil.generateRequestSignature(
        subOperatorId.toString(), config.getSignatureSecret());

    // Step 1 & 2: Save locally with PENDING status
    GoldenEggsBonus bonus = buildBonus(request, subOperatorId, "PENDING");
    
    return bonusRepository.save(bonus)
        .flatMap(savedBonus -> {
            // Step 3 & 4 & 5: Call provider API
            return goldenEggsWebClient.post()
                .uri("/api/operator/v1/bonuses/" + subOperatorId)
                .header("X-REQUEST-SIGN", signature)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(CreateBonusResponse.class)
                .flatMap(providerResponse -> {
                    // Step 6: Update status based on response
                    if (providerResponse.getStatus() != null && providerResponse.getStatus()) {
                        savedBonus.setStatus("CREATED");
                    } else {
                        savedBonus.setStatus("FAILED");
                    }
                    savedBonus.setUpdatedAt(Instant.now());
                    
                    return bonusRepository.save(savedBonus)
                        .thenReturn(providerResponse);
                })
                .onErrorResume(error -> {
                    // Provider API call failed
                    savedBonus.setStatus("FAILED");
                    savedBonus.setUpdatedAt(Instant.now());
                    
                    return bonusRepository.save(savedBonus)
                        .thenReturn(CreateBonusResponse.builder().status(false).build());
                });
        });
}
```

## Bonus Status Lifecycle

```
PENDING → CREATED → ACTIVE → PENDING_COMPLETE → COMPLETED
                                                ↓
                                            EXPIRED
                                                ↓
                                        EXPIRED_WHEN_ACTIVE

PENDING → FAILED (if creation fails)

CREATED/ACTIVE → CANCELLED (manual cancellation)
```

### Status Descriptions

| Status | Description |
|--------|-------------|
| **PENDING** | Bonus creation request sent to provider, awaiting response |
| **CREATED** | Bonus successfully created at provider, ready to be activated |
| **ACTIVE** | Bonus is active and usable by the user |
| **PENDING_COMPLETE** | Bonus is awaiting completion (all freebets used) |
| **COMPLETED** | Bonus completed successfully, winnings credited |
| **EXPIRED** | Bonus expired before being used |
| **EXPIRED_WHEN_ACTIVE** | Bonus expired after being activated (time limit exceeded) |
| **CANCELLED** | Bonus manually cancelled by operator |
| **FAILED** | Bonus creation failed at provider |

## FreebetConfig Management

The platform (this backend) creates and manages the `freebetConfig` for each bonus. This config specifies:

1. **Total count** of freebets
2. **Game-specific configuration** (bet amount, difficulty, risk, etc.)

### Supported Game Configurations

#### 1. Lucky Mines
```json
{
  "count": "10",
  "luckyMinesConfig": {
    "betAmount": "1000",
    "minesCount": "20"
  }
}
```

#### 2. Chicken Road
```json
{
  "count": "5",
  "chickenRoadConfig": {
    "betAmount": "500",
    "difficulty": "MEDIUM"
  }
}
```

#### 3. Chicken Road Two
```json
{
  "count": "8",
  "chickenRoadTwoConfig": {
    "betAmount": "300",
    "difficulty": "HARD"
  }
}
```

#### 4. Plinko 1000
```json
{
  "count": "12",
  "plinko1000Config": {
    "betAmount": "200",
    "risk": "HIGH"
  }
}
```

#### 5. Forest Fortune
```json
{
  "count": "15",
  "forestFortuneConfig": {
    "betAmount": "100",
    "risk": "LOW"
  }
}
```

### Multiple Game Modes

A single bonus can support multiple game modes:

```json
{
  "userId": "21704",
  "bonusId": "8eb034e0-ca4c-44e3-a9a4-692d71b261fe",
  "gameModes": ["lucky-mines", "chicken-road", "plinko"],
  "currency": "USD",
  "expiresAt": "2024-07-17T13:31:20.873Z",
  "type": "FREEBET",
  "freebetConfig": {
    "count": "20",
    "luckyMinesConfig": {
      "betAmount": "1000",
      "minesCount": "20"
    },
    "chickenRoadConfig": {
      "betAmount": "500",
      "difficulty": "EASY"
    },
    "plinko1000Config": {
      "betAmount": "200",
      "risk": "MEDIUM"
    }
  }
}
```

## Error Handling

### Local Save Failure
```java
.onErrorResume(e -> {
    log.error("Error saving bonus locally", e);
    return Mono.just(CreateBonusResponse.builder().status(false).build());
});
```

### Provider API Failure
```java
.onErrorResume(error -> {
    log.error("Failed to create bonus at provider: bonusId={}", request.getBonusId(), error);
    savedBonus.setStatus("FAILED");
    savedBonus.setUpdatedAt(Instant.now());
    
    return bonusRepository.save(savedBonus)
        .thenReturn(CreateBonusResponse.builder().status(false).build());
});
```

## Monitoring & Logging

### Key Log Points

1. **Bonus Creation Start**
   ```
   Creating bonus: bonusId={}, userId={}, subOperatorId={}
   ```

2. **Local Save Success**
   ```
   Bonus saved locally with PENDING status: bonusId={}
   ```

3. **Provider Success**
   ```
   Bonus created successfully at provider: bonusId={}
   ```

4. **Provider Failure**
   ```
   Bonus creation failed at provider: bonusId={}
   ```

5. **Status Update**
   ```
   Bonus status updated: bonusId={}, status={}
   ```

## Database Schema

```sql
CREATE TABLE golden_eggs_bonus (
    bonus_id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL,
    sub_operator_id UUID NOT NULL,
    game_modes TEXT NOT NULL,           -- JSON array
    currency VARCHAR(10) NOT NULL,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,        -- PENDING, CREATED, ACTIVE, etc.
    bonus_quantity INT NOT NULL,
    bonus_available INT NOT NULL,
    win_sum DECIMAL(20, 9) DEFAULT 0,
    freebet_config TEXT NOT NULL,       -- JSON object
    expires_at TIMESTAMP NOT NULL,
    expires_when_active_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

## Summary

✅ **Platform manages freebetConfig** - All game configurations created locally  
✅ **PENDING status first** - Tracks creation attempts  
✅ **Provider creates bonus** - POST request with signature  
✅ **Status updated** - CREATED (success) or FAILED (error)  
✅ **Full audit trail** - All states tracked in database  
✅ **Error resilient** - Handles all failure scenarios  

The bonus creation flow ensures:
- Complete control over bonus configurations
- Proper state tracking at all stages
- Resilient error handling
- Full audit trail for compliance
- Seamless integration with provider
