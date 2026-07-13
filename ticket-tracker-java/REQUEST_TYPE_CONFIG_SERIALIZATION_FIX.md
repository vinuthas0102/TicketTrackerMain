# Request Type Field Display Fix - Config Serialization Issue

## Problem Summary

The request type dropdown was not displaying on the frontend because the `modules.config` field was being serialized as an escaped JSON string instead of a proper JSON object. This caused the frontend to be unable to access `config.requestTypes`.

### Root Cause

1. **Database Storage**: Supabase PostgreSQL stores `modules.config` correctly as JSONB
2. **DAO Layer**: `ModuleDAO` fetches config using `rs.getString("config")`, converting JSONB to String
3. **Model Layer**: `Module.java` had `@JsonDeserialize` for incoming requests but was **MISSING** `@JsonSerialize` for outgoing responses
4. **Serialization Issue**: Jackson serialized the String config as an escaped string: `"config": "{\"categories\":[...]}"`
5. **Frontend Impact**: Frontend expected: `"config": {"categories":[...],"requestTypes":[...]}`
6. **Result**: `selectedModule?.config?.requestTypes` was undefined, preventing dropdown rendering

## Solution Implemented

### Backend Changes (ticket-tracker-java)

#### 1. Created Custom JSON Serializer
**File**: `src/main/java/com/tickettracker/serializer/StringToJsonObjectSerializer.java`

```java
public class StringToJsonObjectSerializer extends JsonSerializer<String> {
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null || value.trim().isEmpty()) {
            gen.writeStartObject();
            gen.writeEndObject();
            return;
        }

        try {
            Object jsonObject = objectMapper.readValue(value, Object.class);
            gen.writeObject(jsonObject);
        } catch (Exception e) {
            logger.warn("Failed to parse config string as JSON, writing as empty object: {}", e.getMessage());
            gen.writeStartObject();
            gen.writeEndObject();
        }
    }
}
```

**Purpose**: Converts the String config field to a JSON object during serialization

#### 2. Updated Module Model
**File**: `src/main/java/com/tickettracker/model/Module.java`

**Added**:
```java
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tickettracker.serializer.StringToJsonObjectSerializer;

// On the config field:
@JsonDeserialize(using = JsonObjectToStringDeserializer.class)
@JsonSerialize(using = StringToJsonObjectSerializer.class)
private String config; // JSON string
```

**Impact**: Config is stored as String in Java but serialized as JSON object in API responses

### Frontend Changes (ticket-tracker-rest)

#### 3. Enhanced AuthService Parsing
**File**: `frontend/src/services/authService.ts`

**Added defensive parsing**:
```typescript
let parsedConfig = module.config;

if (typeof module.config === 'string') {
  try {
    parsedConfig = JSON.parse(module.config);
    console.warn('Config was received as string, parsed successfully for module:', module.name);
  } catch (e) {
    console.error('Failed to parse config string for module:', module.name, e);
    parsedConfig = {};
  }
}

return {
  // ...
  config: {
    categories: Array.isArray(parsedConfig?.categories)
      ? parsedConfig.categories
      : ['General'],
    requestTypes: Array.isArray(parsedConfig?.requestTypes)
      ? parsedConfig.requestTypes
      : [],
    ...parsedConfig
  },
  // ...
};
```

**Purpose**: Provides backward compatibility and handles edge cases

#### 4. Added TicketForm Debug Logging
**File**: `frontend/src/components/ticket/TicketForm.tsx`

**Added**:
```typescript
if (selectedModule && availableRequestTypes.length === 0) {
  console.warn('No request types configured for module:', selectedModule.name, 'Config:', selectedModule.config);
}
```

**Purpose**: Helps debug configuration issues

## Testing Instructions

### 1. Backend API Testing

Test the `/api/modules` endpoint:

```bash
curl -X GET http://localhost:8080/ticket-tracker-java/api/modules \
  -H "Authorization: Bearer <your-token>"
```

**Expected Response Structure**:
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440101",
    "name": "Maintenance Tracker",
    "schema_id": "maintenance",
    "config": {
      "categories": ["Electrical", "Plumbing", "HVAC"],
      "requestTypes": [
        {
          "label": "Pre-Occupation Maintenance",
          "value": "Pre-Occupation Maintenance",
          "requiresCEInspection": false
        },
        {
          "label": "Vacation Handover",
          "value": "Vacation Handover",
          "requiresCEInspection": true
        }
      ]
    },
    "active": true
  }
]
```

**Key Check**: `config` should be a JSON object, NOT a string

### 2. Frontend Testing

1. **Open Browser Developer Console**
2. **Navigate to Ticket Creation Page**
3. **Select a Module**
4. **Verify Request Type Dropdown Appears**
5. **Check Console Logs**:
   - Should NOT see: "Config was received as string"
   - Should NOT see: "No request types configured for module"
   - If you see these warnings, the backend fix may not be deployed

### 3. Full Integration Test

1. Login to the application
2. Navigate to ticket creation
3. Select "Maintenance Tracker" module
4. Verify the "Request Type" dropdown displays with options:
   - Pre-Occupation Maintenance
   - Vacation Handover
   - Annual Maintenance
   - Emergency Maintenance
5. Select "Vacation Handover"
6. Verify the CE Inspection notice appears below the dropdown
7. Create a ticket
8. Verify `request_type` is saved correctly in the database

### 4. Database Verification

```sql
-- Check if request type was saved
SELECT id, ticket_number, request_type FROM tickets WHERE request_type IS NOT NULL LIMIT 5;

-- Verify module config structure
SELECT name, schema_id, config FROM modules WHERE schema_id = 'maintenance';
```

## Files Modified

### Backend (ticket-tracker-java)
1. **NEW**: `src/main/java/com/tickettracker/serializer/StringToJsonObjectSerializer.java`
2. **MODIFIED**: `src/main/java/com/tickettracker/model/Module.java`

### Frontend (ticket-tracker-rest)
1. **MODIFIED**: `frontend/src/services/authService.ts`
2. **MODIFIED**: `frontend/src/components/ticket/TicketForm.tsx`

## Expected Behavior After Fix

1. ✅ API returns config as proper JSON object
2. ✅ Request type dropdown displays all configured types
3. ✅ Users can select request types when creating tickets
4. ✅ Selected request type saves to `tickets.request_type` column
5. ✅ CE Inspection notice displays for applicable request types
6. ✅ Frontend handles missing/empty requestTypes gracefully

## Rollback Instructions

If issues arise, revert these commits:

```bash
# Backend
cd ticket-tracker-java
git checkout HEAD~1 src/main/java/com/tickettracker/model/Module.java
rm src/main/java/com/tickettracker/serializer/StringToJsonObjectSerializer.java

# Frontend
cd ticket-tracker-rest/frontend
git checkout HEAD~1 src/services/authService.ts
git checkout HEAD~1 src/components/ticket/TicketForm.tsx
```

## Technical Details

### How the Fix Works

1. **Database → DAO**: PostgreSQL JSONB → JDBC getString() → Java String
2. **DAO → Service → Servlet**: String remains unchanged through layers
3. **Servlet → Response**: Jackson ObjectMapper serializes Module to JSON
4. **Serialization**: `@JsonSerialize(using = StringToJsonObjectSerializer.class)` triggers custom serializer
5. **Custom Serializer**: Parses String config → Writes as JSON object to response
6. **Response**: Client receives proper JSON object structure
7. **Frontend**: Accesses `config.requestTypes` successfully

### Why This Approach

- **Minimal Changes**: Only touches serialization layer
- **No Schema Changes**: Database and DAO remain unchanged
- **Type Safety**: Config remains String in Java (type-safe for JDBC)
- **API Contract**: Frontend gets expected JSON object structure
- **Error Handling**: Gracefully handles null/empty/malformed JSON
- **Backward Compatible**: Frontend includes defensive parsing as safety net

## Support

If you encounter issues:

1. Check browser console for error messages
2. Verify API response structure using browser DevTools Network tab
3. Check server logs for serialization warnings
4. Ensure both backend and frontend changes are deployed
5. Clear browser cache and reload

## Related Documentation

- Database Schema: `database/02-oracle-schema.sql`
- Migration Files: `supabase/migrations/20260303074252_add_request_type_step_type_and_ce_users.sql`
- Module Configuration Guide: See `modules.config` JSONB structure in database
