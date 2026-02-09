# Game Aggregator Launch URL Update

## Overview

Updated the Golden Eggs launch endpoint to generate URLs in the game aggregator format with all required parameters.

## Changes Made

### 1. LaunchRequest DTO Updated

**File**: `src/main/java/com/ebingo/backend/externalgame/dto/LaunchRequest.java`

**Added Fields**:
- `subId` - Sub-identifier for the game session
- `lobbyUrl` - URL to return to lobby
- `brandName` - Brand name for the game
- `lang` - Language preference (optional, defaults to "en")
- `adaptive` - Adaptive mode flag (optional, defaults to true)
- `isDemoPlay` - Demo play mode flag (optional, defaults to false)

**Removed Fields**:
- `returnUrl` - Replaced by `lobbyUrl`

### 2. GoldenEggsConfig Updated

**File**: `src/main/java/com/ebingo/backend/externalgame/config/GoldenEggsConfig.java`

**Added Field**:
- `aggregatorId` - Aggregator identifier for the game provider

### 3. URL Generation Updated

**File**: `src/main/java/com/ebingo/backend/externalgame/service/GoldenEggsIntegrationService.java`

**Method**: `generateAndSaveAuthToken()`

**New URL Format**:
```
https://api.golden-eggs.games/api/aggregator/launch?
aggregatorId=${aggregatorId}&
subId=${subId}&
gameMode=${gameMode}&
currency=${currency}&
authToken=${authToken}&
lang=${lang}&
adaptive=${adaptive}&
isDemoPlay=${isDemoPlay}&
token=${token}&
lobbyUrl=${lobbyUrl}&
brandName=${brandName}
```

**Parameters**:
- `aggregatorId` - From config (required)
- `subId` - From request (optional)
- `gameMode` - From request (required)
- `currency` - From request (required)
- `authToken` - Generated secure token (required)
- `lang` - From request, defaults to "en"
- `adaptive` - From request, defaults to "true"
- `isDemoPlay` - From request, defaults to "false"
- `token` - Same as authToken (required)
- `lobbyUrl` - From request (optional)
- `brandName` - From request (optional)

### 4. Configuration Updated

**File**: `src/main/resources/application.yml`

**Added**:
```yaml
golden-eggs:
  aggregator-id: ${GOLDEN_EGGS_AGGREGATOR_ID:your-aggregator-id}
```

**Environment Variable**:
- `GOLDEN_EGGS_AGGREGATOR_ID` - Must be set in production

## API Request Example

### POST `/external-games/golden-eggs/launch`

**Request Body**:
```json
{
  "agentId": 1,
  "gameMode": "crash",
  "currency": "USD",
  "initData": "query_id=...&user=...&auth_date=...&hash=...",
  "subId": "user123",
  "lobbyUrl": "https://yourgame.com/lobby",
  "brandName": "YourBrand",
  "lang": "en",
  "adaptive": true,
  "isDemoPlay": false
}
```

**Response**:
```json
{
  "url": "https://api.golden-eggs.games/api/aggregator/launch?aggregatorId=agg123&subId=user123&gameMode=crash&currency=USD&authToken=AbCdEf123...&lang=en&adaptive=true&isDemoPlay=false&token=AbCdEf123...&lobbyUrl=https://yourgame.com/lobby&brandName=YourBrand"
}
```

## Configuration Required

### Environment Variables

Set the following environment variable:

```bash
export GOLDEN_EGGS_AGGREGATOR_ID="your-aggregator-id-from-provider"
```

### Optional Request Parameters

All optional parameters have sensible defaults:
- `lang` → defaults to "en"
- `adaptive` → defaults to true
- `isDemoPlay` → defaults to false
- `subId` → defaults to empty string
- `lobbyUrl` → defaults to empty string
- `brandName` → defaults to empty string

## Backward Compatibility

⚠️ **Breaking Change**: The `returnUrl` field has been removed from `LaunchRequest` and replaced with `lobbyUrl`.

If you have existing clients using `returnUrl`, update them to use `lobbyUrl` instead.

## Testing

The endpoint maintains all existing authentication and security features:
- ✅ Telegram initData verification
- ✅ Agent-based bot token validation
- ✅ User creation/resolution
- ✅ Secure token generation
- ✅ Token TTL management

**Build Status**: ✅ Compilation successful

## URL Structure

The generated URL follows the game aggregator specification:

```
Base URL: https://api.golden-eggs.games/api/aggregator/launch
Query Parameters:
  - aggregatorId: Identifies the aggregator
  - subId: Optional sub-identifier
  - gameMode: Game type (e.g., "crash")
  - currency: Currency code (e.g., "USD")
  - authToken: Secure authentication token
  - lang: Language code
  - adaptive: Adaptive display mode
  - isDemoPlay: Demo mode flag
  - token: Duplicate of authToken (provider requirement)
  - lobbyUrl: Return URL for lobby
  - brandName: Brand identifier
```

## Next Steps

1. Set `GOLDEN_EGGS_AGGREGATOR_ID` environment variable
2. Update frontend clients to use new `lobbyUrl` field instead of `returnUrl`
3. Test the launch flow with the new URL format
4. Verify game loads correctly with all parameters

## Summary

✅ **Completed**:
- LaunchRequest DTO updated with new fields
- GoldenEggsConfig updated with aggregatorId
- URL generation updated to match aggregator format
- Configuration file updated
- All code compiles successfully

The launch endpoint now generates URLs in the exact format required by the game aggregator API.
