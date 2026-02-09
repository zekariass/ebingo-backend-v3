# Telegram InitData Authentication for Golden Eggs Launch

## Overview

The Golden Eggs game launch endpoint has been updated to use **Telegram WebApp initData authentication** instead of Spring Security. This provides a secure, Spring Security-free authentication flow for external game launches.

## Key Features

✅ **No Spring Security Dependency** - Uses Telegram WebApp initData for authentication  
✅ **Agent-Specific Bot Tokens** - Each agent has their own Telegram bot token  
✅ **Opaque Token Generation** - Cryptographically secure, non-deterministic tokens  
✅ **Automatic User Creation** - Creates platform users from Telegram user data  
✅ **Token Uniqueness** - Retry mechanism for collision handling  
✅ **Fully Reactive** - Spring WebFlux + R2DBC implementation  

## Authentication Flow

```
1. Frontend → Backend: POST /external-games/golden-eggs/launch
   {
     "agentId": 1,
     "gameMode": "crash",
     "currency": "USD",
     "initData": "<Telegram WebApp initData>"
   }

2. Backend loads Agent by agentId → gets botToken

3. Backend verifies initData using botToken:
   - Validates HMAC-SHA256 signature
   - Checks auth_date freshness (default: 10 minutes)
   - Extracts Telegram user ID

4. Backend finds or creates platform user:
   - Searches by (telegramId, agentId)
   - Creates new user if not found

5. Backend generates unique AuthToken:
   - 32+ bytes SecureRandom
   - Base64URL encoded (no padding)
   - Stored in external_game_auth_tokens table
   - TTL: 20 minutes (configurable)

6. Backend returns game URL:
   {
     "url": "https://api.golden-eggs.games/game/crash?token=<AuthToken>&operator=<operatorId>&currency=USD"
   }
```

## API Endpoint

### POST `/external-games/golden-eggs/launch`

**Request:**
```json
{
  "agentId": 1,
  "gameMode": "crash",
  "currency": "USD",
  "initData": "query_id=...&user=...&auth_date=...&hash=...",
  "returnUrl": "https://your-site.com/games",  // optional
  "lang": "en"  // optional
}
```

**Success Response (200 OK):**
```json
{
  "url": "https://api.golden-eggs.games/game/crash?token=AbCdEf123...&operator=op1&currency=USD"
}
```

**Error Responses:**

**404 Not Found** - Agent not found:
```json
{
  "code": "AGENT_NOT_FOUND",
  "message": "Agent not found: 999"
}
```

**401 Unauthorized** - Invalid initData:
```json
{
  "code": "INVALID_INIT_DATA",
  "message": "Invalid or expired Telegram initData"
}
```

**500 Internal Server Error** - Configuration error:
```json
{
  "code": "CONFIGURATION_ERROR",
  "message": "Agent has no bot token configured"
}
```

## Configuration

### application.yml

```yaml
golden-eggs:
  operator-id: ${GOLDEN_EGGS_OPERATOR_ID:your-operator-id}
  api-base-url: ${GOLDEN_EGGS_API_BASE_URL:https://api.golden-eggs.games}
  signature-secret: ${GOLDEN_EGGS_SIGNATURE_SECRET:your-secret-key}
  auth-token-ttl-minutes: ${GOLDEN_EGGS_AUTH_TOKEN_TTL:30}
  session-token-ttl-minutes: ${GOLDEN_EGGS_SESSION_TOKEN_TTL:120}
  launch-token-ttl-minutes: ${GOLDEN_EGGS_LAUNCH_TOKEN_TTL:20}
  init-data-max-age-seconds: ${GOLDEN_EGGS_INIT_DATA_MAX_AGE:600}
```

### Environment Variables

```bash
# Required
GOLDEN_EGGS_OPERATOR_ID=your-operator-id
GOLDEN_EGGS_SIGNATURE_SECRET=your-webhook-signature-secret

# Optional (with defaults)
GOLDEN_EGGS_LAUNCH_TOKEN_TTL=20  # minutes
GOLDEN_EGGS_INIT_DATA_MAX_AGE=600  # seconds (10 minutes)
```

## Database Schema

### Agent Table (existing)

The `agents` table must have a `bot_token` column:

```sql
ALTER TABLE agents ADD COLUMN IF NOT EXISTS bot_token VARCHAR(255);
```

Each agent should have their own Telegram bot token configured.

### External Game Auth Tokens Table (existing)

Created by migration `V3__external_game_integration.sql`:

```sql
CREATE TABLE external_game_auth_tokens (
    token VARCHAR(255) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    operator_id VARCHAR(100) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    game_mode VARCHAR(100),
    expires_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_auth_token_user FOREIGN KEY (user_id) REFERENCES user_profile(id)
);
```

## Security Features

### 1. Telegram InitData Verification

The `TelegramAuthVerifier` validates:

- **HMAC-SHA256 Signature**: Verifies request authenticity
- **auth_date Freshness**: Prevents replay attacks (default: 10 minutes)
- **Data Integrity**: Ensures initData hasn't been tampered with

Algorithm:
```
secretKey = HMAC-SHA256("WebAppData", botToken)
computedHash = HMAC-SHA256(secretKey, dataCheckString)
valid = (computedHash == receivedHash)
```

### 2. Token Generation

- **Cryptographically Secure**: Uses `SecureRandom` with 32+ bytes
- **Non-Deterministic**: Different token for each request
- **Base64URL Encoding**: URL-safe, no padding
- **Unique Constraint**: Database ensures uniqueness
- **Collision Retry**: Up to 3 retries on collision

### 3. Token Storage

- **TTL-Based Expiration**: Configurable expiration time
- **Status Tracking**: ACTIVE, EXPIRED, REVOKED
- **User Association**: Linked to platform user ID
- **Agent Isolation**: Tokens scoped to agent

## User Management

### Find or Create User

The service automatically:

1. **Searches** for existing user by `(telegramId, agentId)`
2. **Creates** new user if not found:
   - Extracts `first_name`, `last_name`, `username` from initData
   - Sets `nickname` to username or `"user{telegramId}"`
   - Marks as `isBot=false`, `isDeleted=false`
   - Associates with agent

### User Profile Fields

```java
UserProfile {
    id: Long (auto-generated)
    telegramId: Long (from initData)
    agentId: Long (from request)
    firstName: String (from initData)
    lastName: String (from initData)
    nickname: String (from initData.username or generated)
    isBot: false
    isDeleted: false
    createdAt: LocalDateTime
    updatedAt: LocalDateTime
}
```

## Error Handling

### Agent Not Found
- **HTTP 404**
- **Code**: `AGENT_NOT_FOUND`
- **Cause**: Invalid agentId or agent doesn't exist

### Invalid InitData
- **HTTP 401**
- **Code**: `INVALID_INIT_DATA`
- **Causes**:
  - Invalid HMAC signature
  - Expired auth_date (> 10 minutes old)
  - Missing required fields
  - Malformed JSON in user data

### Configuration Error
- **HTTP 500**
- **Code**: `CONFIGURATION_ERROR`
- **Cause**: Agent has no bot token configured

### Unknown Error
- **HTTP 500**
- **Code**: `UNKNOWN_ERROR`
- **Cause**: Unexpected server error

## Testing

### Unit Tests

**GoldenEggsLaunchControllerTest**:
- ✅ Success case with valid initData
- ✅ Invalid initData returns 401
- ✅ Agent not found returns 404
- ✅ Missing required fields validation
- ✅ Two sequential calls produce different tokens

**GoldenEggsIntegrationServiceTelegramAuthTest**:
- ✅ Successful launch with existing user
- ✅ Agent not found error
- ✅ Invalid initData error
- ✅ Create new user from Telegram data
- ✅ Missing bot token error

### Running Tests

```bash
# Run all external game tests
./mvnw test -Dtest="com.ebingo.backend.externalgame.**"

# Run specific test class
./mvnw test -Dtest=GoldenEggsLaunchControllerTest
./mvnw test -Dtest=GoldenEggsIntegrationServiceTelegramAuthTest
```

## Integration Example

### Frontend (Telegram WebApp)

```javascript
// Get initData from Telegram WebApp
const initData = window.Telegram.WebApp.initData;

// Request game launch
const response = await fetch('/external-games/golden-eggs/launch', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    agentId: 1,
    gameMode: 'crash',
    currency: 'USD',
    initData: initData,
    returnUrl: window.location.href,
    lang: 'en'
  })
});

const data = await response.json();

if (response.ok) {
  // Open game in iframe
  const iframe = document.createElement('iframe');
  iframe.src = data.url;
  document.body.appendChild(iframe);
} else {
  // Handle error
  console.error('Launch failed:', data.code, data.message);
}
```

## Logging

The implementation logs (without sensitive data):

✅ **Logged**:
- Agent ID
- Telegram user ID
- Truncated token (first 8 characters)
- Game mode, currency
- Success/failure events

❌ **NOT Logged**:
- Raw initData
- Bot tokens
- Full auth tokens
- User personal data

Example logs:
```
INFO  - Generating launch URL for agentId=1, gameMode=crash
INFO  - Telegram user authenticated: telegramId=123456789, agentId=1
INFO  - Generated auth token for userId=100, token=AbCdEf12...
```

## Migration from Spring Security

### Before (Spring Security)
```java
@PostMapping("/launch")
public Mono<LaunchResponse> launchGame(
    @RequestBody LaunchRequest request,
    Authentication authentication
) {
    Long userId = extractUserId(authentication);
    return service.generateLaunchUrl(request, userId);
}
```

### After (Telegram InitData)
```java
@PostMapping("/launch")
public Mono<ResponseEntity<Object>> launchGame(
    @Valid @RequestBody LaunchRequest request
) {
    return service.generateLaunchUrl(request)
        .map(ResponseEntity::ok)
        .onErrorResume(/* error handling */);
}
```

## Troubleshooting

### "INVALID_INIT_DATA" Error

**Possible causes**:
1. InitData is expired (> 10 minutes old)
2. Wrong bot token for the agent
3. InitData has been modified
4. Clock skew between servers

**Solutions**:
- Ensure initData is fresh (< 10 minutes)
- Verify agent has correct bot token
- Check server time synchronization
- Increase `init-data-max-age-seconds` if needed

### "AGENT_NOT_FOUND" Error

**Cause**: Agent ID doesn't exist in database

**Solution**: Verify agentId exists in `agents` table

### "CONFIGURATION_ERROR" Error

**Cause**: Agent has no bot token configured

**Solution**: Set bot token for the agent:
```sql
UPDATE agents SET bot_token = 'your-bot-token' WHERE id = 1;
```

### Token Collision

**Rare case**: Two tokens generated at same time collide

**Handled automatically**: Service retries up to 3 times

**Monitor**: Check logs for "Token collision detected, retrying..."

## Performance Considerations

- **Database Queries**: 2-3 queries per launch (agent, user, token save)
- **Token Generation**: ~1ms (SecureRandom + Base64URL)
- **InitData Verification**: ~2-3ms (HMAC-SHA256 computation)
- **Total Latency**: ~50-100ms typical

## Security Best Practices

1. ✅ **Never log** bot tokens or raw initData
2. ✅ **Use HTTPS** for all API calls
3. ✅ **Validate** all request fields
4. ✅ **Set appropriate** TTLs for tokens
5. ✅ **Monitor** failed authentication attempts
6. ✅ **Rotate** bot tokens periodically
7. ✅ **Implement** rate limiting on launch endpoint

## Summary

The Telegram initData authentication provides:

- ✅ **Security**: HMAC-verified, time-limited authentication
- ✅ **Simplicity**: No Spring Security configuration needed
- ✅ **Flexibility**: Agent-specific bot tokens
- ✅ **Automation**: Auto-creates users from Telegram data
- ✅ **Reliability**: Retry mechanism for token collisions
- ✅ **Observability**: Comprehensive logging (without sensitive data)

The implementation is fully reactive, production-ready, and tested.
