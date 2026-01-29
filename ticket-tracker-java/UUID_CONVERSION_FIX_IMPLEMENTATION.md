# UUID Conversion Fix Implementation

## Problem Summary

The `getTicketsByModule` method was not returning data from the database even though tickets existed. This was caused by incorrect UUID-to-bytes conversion in the servlet layer.

### Root Cause

The local `hexToBytes()` method in `TicketServlet.java` did NOT remove hyphens from UUID strings before conversion, while:
- Frontend sends UUIDs WITH hyphens (e.g., `550e8400-e29b-41d4-a716-446655440101`)
- Database stores UUIDs as RAW(16) byte arrays
- The utility class `ByteArrayUtil.hexToBytes()` correctly removes hyphens

This mismatch caused incorrect byte arrays that didn't match database records.

## Implementation Completed

### ✅ Fixed: TicketServlet.java

**Changes Made:**

1. **Added proper imports:**
   ```java
   import com.tickettracker.util.ByteArrayUtil;
   import com.tickettracker.util.UuidUtil;
   ```

2. **Replaced all `hexToBytes()` calls with `ByteArrayUtil.hexToBytes()`:**
   - Line 111: `handleBulkCreate()` method - moduleId conversion
   - Line 195: `doDelete()` method - ticketId conversion
   - Line 231: `handleGetAllTickets()` method - moduleId conversion
   - Line 255: `handleGetTicket()` method - ticketId conversion

3. **Added UUID validation in `handleGetAllTickets()`:**
   ```java
   if (!UuidUtil.isValidUuid(moduleId)) {
       logger.error("Invalid UUID format for moduleId: {}", moduleId);
       sendError(response, 400, "Invalid module ID format. Expected UUID format.");
       return;
   }
   ```

4. **Added comprehensive debug logging:**
   - Logs incoming moduleId parameter
   - Logs converted byte array in hex format
   - Logs number of tickets found
   - Logs all major operations (search, status filter, delete, bulk create)

5. **Removed duplicate `hexToBytes()` method:**
   - Eliminated the local implementation at lines 300-310
   - Now uses centralized `ByteArrayUtil` for consistent behavior

### ✅ Enhanced: TicketDAO.java

**Changes Made:**

1. **Added debug logging in `findByModuleId()`:**
   ```java
   logger.debug("Executing findByModuleId query with moduleId bytes: {}", bytesToHex(moduleId));
   // ... query execution ...
   logger.debug("Found {} tickets for moduleId: {}", tickets.size(), bytesToHex(moduleId));
   ```

This helps trace the exact byte values being used in database queries.

## Testing Recommendations

### 1. Verify Module ID Format
Check what format your frontend is sending:
```javascript
// In browser console or API logs
console.log("Module ID being sent:", moduleId);
```

### 2. Verify Database Data
Check if tickets exist with the expected module_id:
```sql
-- Query to see all module IDs in hex format
SELECT
    RAWTOHEX(id) as ticket_id,
    RAWTOHEX(module_id) as module_id_hex,
    title
FROM tickets;

-- Query to test specific module ID (without hyphens)
SELECT * FROM tickets
WHERE module_id = HEXTORAW('550E8400E29B41D4A716446655440101');
```

### 3. Test API Endpoints

**Test with valid module ID:**
```bash
curl -X GET "http://localhost:8080/ticket-tracker/api/tickets?moduleId=550e8400-e29b-41d4-a716-446655440101"
```

**Test with invalid module ID format:**
```bash
curl -X GET "http://localhost:8080/ticket-tracker/api/tickets?moduleId=invalid-uuid"
# Should return 400 Bad Request
```

### 4. Check Application Logs
Enable DEBUG logging for detailed traces:
```xml
<!-- In log4j2.xml -->
<Logger name="com.tickettracker.servlet" level="debug"/>
<Logger name="com.tickettracker.dao" level="debug"/>
```

Expected log output:
```
DEBUG [TicketServlet] Fetching tickets by moduleId: 550e8400-e29b-41d4-a716-446655440101
DEBUG [TicketServlet] Converted moduleId to bytes: 550E8400E29B41D4A716446655440101
DEBUG [TicketDAO] Executing findByModuleId query with moduleId bytes: 550E8400E29B41D4A716446655440101
DEBUG [TicketDAO] Found 5 tickets for moduleId: 550E8400E29B41D4A716446655440101
DEBUG [TicketServlet] Found 5 tickets for moduleId: 550e8400-e29b-41d4-a716-446655440101
```

## ⚠️ Remaining Issues - Other Servlets

The same UUID conversion issue exists in **9 other servlets**. They all have duplicate `hexToBytes()` methods without hyphen removal:

### Affected Servlets:
1. `DependencyServlet.java`
2. `FieldValueServlet.java`
3. `FileReferenceServlet.java`
4. `FinanceApprovalServlet.java`
5. `UserManagementServlet.java`
6. `AuditServlet.java`
7. `FileServlet.java`
8. `ModuleServlet.java`
9. `WorkflowStepServlet.java`

### Required Fix for Each Servlet:

**Step 1: Add imports**
```java
import com.tickettracker.util.ByteArrayUtil;
import com.tickettracker.util.UuidUtil;
```

**Step 2: Replace all `hexToBytes()` calls**
```java
// OLD (INCORRECT):
byte[] id = hexToBytes(someUuidString);

// NEW (CORRECT):
byte[] id = ByteArrayUtil.hexToBytes(someUuidString);
```

**Step 3: Add UUID validation where appropriate**
```java
if (uuidParam != null && !UuidUtil.isValidUuid(uuidParam)) {
    sendError(response, 400, "Invalid UUID format");
    return;
}
```

**Step 4: Remove duplicate `hexToBytes()` method**
```java
// DELETE this entire method from each servlet
private byte[] hexToBytes(String hex) {
    if (hex == null || hex.isEmpty()) {
        return null;
    }
    int len = hex.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
        data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                + Character.digit(hex.charAt(i + 1), 16));
    }
    return data;
}
```

**Step 5: Add debug logging**
```java
logger.debug("Processing request with UUID: {}", uuidParam);
byte[] bytes = ByteArrayUtil.hexToBytes(uuidParam);
logger.debug("Converted UUID to bytes: {}", ByteArrayUtil.bytesToHex(bytes));
```

## Common Patterns to Search and Replace

### Pattern 1: Path Parameter Conversion
```java
// OLD:
String[] pathParts = pathInfo.split("/");
byte[] id = hexToBytes(pathParts[1]);

// NEW:
String[] pathParts = pathInfo.split("/");
logger.debug("Processing ID from path: {}", pathParts[1]);
byte[] id = ByteArrayUtil.hexToBytes(pathParts[1]);
```

### Pattern 2: Query Parameter Conversion
```java
// OLD:
String moduleId = request.getParameter("moduleId");
if (moduleId != null) {
    byte[] moduleIdBytes = hexToBytes(moduleId);
    // ... use moduleIdBytes
}

// NEW:
String moduleId = request.getParameter("moduleId");
if (moduleId != null) {
    if (!UuidUtil.isValidUuid(moduleId)) {
        sendError(response, 400, "Invalid module ID format");
        return;
    }
    logger.debug("Processing moduleId: {}", moduleId);
    byte[] moduleIdBytes = ByteArrayUtil.hexToBytes(moduleId);
    // ... use moduleIdBytes
}
```

### Pattern 3: Request Body UUID Conversion
```java
// OLD:
SomeRequest request = objectMapper.readValue(body, SomeRequest.class);
byte[] relatedId = hexToBytes(request.getRelatedId());

// NEW:
SomeRequest request = objectMapper.readValue(body, SomeRequest.class);
logger.debug("Processing relatedId: {}", request.getRelatedId());
byte[] relatedId = ByteArrayUtil.hexToBytes(request.getRelatedId());
```

## Benefits of This Fix

1. **Consistent UUID Handling:** All UUID conversions now use the same utility method
2. **Hyphen Support:** UUIDs with or without hyphens are correctly processed
3. **Better Error Handling:** Invalid UUID formats are caught early with clear error messages
4. **Improved Debugging:** Comprehensive logging helps trace data flow
5. **Maintainability:** Single source of truth for UUID conversion logic
6. **Reduced Duplication:** Eliminates duplicate code across servlets

## Next Steps

1. **Apply the same fix to all 9 remaining servlets** using the patterns above
2. **Test each endpoint** that accepts UUID parameters
3. **Review logs** to ensure conversions are working correctly
4. **Update API documentation** to specify UUID format requirements (with or without hyphens)
5. **Consider adding integration tests** for UUID conversion scenarios

## Build and Deploy

After making changes:

```bash
# Build the project
cd ticket-tracker-java
./build.sh --skip-tests

# Deploy to Tomcat
cp target/ticket-tracker.war $TOMCAT_HOME/webapps/

# Restart Tomcat
$TOMCAT_HOME/bin/shutdown.sh
$TOMCAT_HOME/bin/startup.sh

# Check logs
tail -f $TOMCAT_HOME/logs/catalina.out
```

## Summary

The `getTicketsByModule` issue has been **completely fixed** in `TicketServlet.java`. The fix ensures:
- UUIDs with hyphens are properly converted to bytes
- Invalid UUID formats are rejected with clear error messages
- Detailed logging helps diagnose any future issues
- All UUID conversions use the centralized utility class

However, **9 other servlets need the same fix** to prevent similar issues in other parts of the application. Follow the patterns documented above to apply the fix consistently across all servlets.
