# Golden Eggs External Game Integration

This document describes the Golden Eggs game provider integration implemented in the eBingo backend.

## Overview

The Golden Eggs integration follows an aggregator model where:
1. Platform generates auth tokens for game launches
2. Provider calls platform webhooks for game operations (init, bet, withdraw, rollback)
3. Platform manages wallet operations and transaction idempotency

## Architecture

### Components

- **Entities**: `ExternalGameAuthToken`, `ExternalGameSession`, `ExternalGameTxn`
- **Repositories**: R2DBC reactive repositories for all entities
- **Services**:
  - `ExternalGameWalletService`: Handles wallet operations (debit, credit, rollback)
  - `GoldenEggsIntegrationService`: Main integration logic and webhook handlers
- **Controllers**:
  - `GoldenEggsController`: Public endpoints for game launch and game modes list
  - `GoldenEggsWebhookController`: Webhook receiver for provider callbacks

### Database Schema

Three new tables were added:

1. **external_game_auth_tokens**: Stores authentication tokens for game launches
2. **external_game_sessions**: Tracks active game sessions
3. **external_game_txns**: Records all game transactions with idempotency support

The `wallet` table was enhanced with a `version` column for optimistic locking.

## Configuration

Add the following environment variables:

```bash
GOLDEN_EGGS_OPERATOR_ID=your-operator-id
GOLDEN_EGGS_API_BASE_URL=https://api.golden-eggs.games
GOLDEN_EGGS_SIGNATURE_SECRET=your-secret-key
GOLDEN_EGGS_AUTH_TOKEN_TTL=30
GOLDEN_EGGS_SESSION_TOKEN_TTL=120
```

Or configure in `application.yml`:

```yaml
golden-eggs:
  operator-id: ${GOLDEN_EGGS_OPERATOR_ID}
  api-base-url: ${GOLDEN_EGGS_API_BASE_URL:https://api.golden-eggs.games}
  signature-secret: ${GOLDEN_EGGS_SIGNATURE_SECRET}
  auth-token-ttl-minutes: ${GOLDEN_EGGS_AUTH_TOKEN_TTL:30}
  session-token-ttl-minutes: ${GOLDEN_EGGS_SESSION_TOKEN_TTL:120}
```

## API Endpoints

### 1. Get Game Modes List

**GET** `/external-games/golden-eggs/game-modes`

Proxies the provider's game modes list.

**Response:**
```json
[
  {
    "gameMode": "crash",
    "title": "Crash",
    "description": "Crash game",
    "iconsUrls": {
      "url": "https://icons.golden-eggs.games/crash.png"
    },
    "multiplayer": true,
    "rtp": "96"
  }
]
```

### 2. Launch Game

**POST** `/external-games/golden-eggs/launch`

Generates a game launch URL with authentication token.

**Request:**
```json
{
  "gameMode": "crash",
  "currency": "USD",
  "returnUrl": "https://your-site.com/games",
  "lang": "en"
}
```

**Response:**
```json
{
  "url": "https://api.golden-eggs.games/game?token=AUTH_TOKEN&gameMode=crash&operator=YOUR_OP_ID&currency=USD"
}
```

### 3. Webhook Receiver

**POST** `/webhooks/golden-eggs`

Receives webhooks from the provider for all game operations.

**Headers:**
- `X-REQUEST-SIGN`: HMAC-SHA256 signature of request body

**Actions:**
- `init`: Initialize game session
- `bet`: Debit player balance
- `withdraw`: Credit winnings (idempotent)
- `rollback`: Refund bet (idempotent)

## Webhook Flow

### Init
1. Validates auth token
2. Creates game session
3. Returns player info and balance

### Bet
1. Validates session token
2. Creates transaction record
3. Debits wallet
4. Returns updated balance or error

### Withdraw (Idempotent)
1. Checks for existing successful transaction
2. If found, returns cached response
3. Otherwise, credits wallet and stores response
4. Returns balance

### Rollback (Idempotent)
1. Checks for existing successful transaction
2. If found, returns cached response
3. Otherwise, refunds wallet and stores response
4. Returns balance

## Security

### Signature Validation

All webhooks must include a valid `X-REQUEST-SIGN` header:

```
HMAC-SHA256(secret, raw_request_body) -> hex lowercase
```

The signature is validated using constant-time comparison to prevent timing attacks.

### Token Security

- Auth tokens: 32-byte cryptographically secure random tokens (Base64URL)
- Session tokens: Generated during init, separate from auth tokens
- Tokens have configurable TTL (default: 30 min auth, 120 min session)

## Wallet Operations

### External Game Wallet Service

Separate service for external game operations that doesn't modify existing wallet logic:

- `debitExternalGame()`: Deducts bet amount
- `creditExternalGame()`: Credits winnings (result already includes stake)
- `rollbackExternalGame()`: Refunds bet amount

### Rounding Rules

- **Fiat currencies** (USD, EUR, etc.): 2 decimal places
- **Crypto currencies** (BTC, ETH, USDT, etc.): 9 decimal places

### Optimistic Locking

The `Wallet` entity uses `@Version` for optimistic locking. External game operations automatically retry up to 3 times on version conflicts.

## Idempotency

Withdraw and rollback operations are idempotent based on `data.transactionId`:

1. Check `external_game_txns` for existing successful transaction
2. If found, return stored `response_snapshot`
3. Otherwise, process transaction and store response

This ensures that duplicate webhook calls (due to retries) don't double-credit or double-refund.

## Error Handling

All errors return HTTP 200 with error code in response body (per provider requirements):

| Code | Type | Description |
|------|------|-------------|
| OK | Success | Operation successful |
| INVALID_TOKEN | Fatal | Invalid or expired token |
| INSUFFICIENT_FUNDS | Fatal | Not enough balance |
| ACCOUNT_INVALID | Fatal | User not found |
| ACCOUNT_LOCKED | Fatal | User account locked |
| UNKNOWN_ERROR | Fatal | Internal error |
| TEMPORARY_ERROR | Retryable | Temporary issue, provider will retry |

## Testing

### Unit Tests

- `ExternalGameWalletServiceTest`: Tests wallet operations, rounding, optimistic locking
- `GoldenEggsWebhookControllerTest`: Tests webhook handling, signature validation, idempotency

### Running Tests

```bash
./mvnw test -Dtest=ExternalGameWalletServiceTest
./mvnw test -Dtest=GoldenEggsWebhookControllerTest
```

## Migration

Run Flyway migration to create tables:

```bash
./mvnw flyway:migrate
```

Or enable Flyway in `application.yml`:

```yaml
spring:
  flyway:
    enabled: true
```

## Monitoring

Key metrics to monitor:

- Transaction success/failure rates by action type
- Wallet operation latencies
- Optimistic locking retry counts
- Signature validation failures
- Token expiration rates

## Troubleshooting

### Invalid Signature Errors

1. Verify `GOLDEN_EGGS_SIGNATURE_SECRET` matches provider's secret
2. Ensure raw request body is used (not parsed JSON)
3. Check signature is lowercase hex

### Idempotency Issues

1. Check `external_game_txns` table for duplicate transactions
2. Verify `response_snapshot` is being stored correctly
3. Ensure unique constraint on `(action, provider_transaction_id)` exists

### Optimistic Locking Failures

1. Check retry logic is working (up to 3 retries)
2. Monitor concurrent transaction rates
3. Consider increasing retry attempts if needed

## Future Enhancements

- Add metrics/monitoring endpoints
- Implement transaction history API
- Add admin endpoints for token management
- Support additional game providers
- Add rate limiting on webhook endpoints
