# Servlet UUID Conversion Fix - Quick Checklist

Use this checklist to fix each of the 9 remaining servlets with UUID conversion issues.

## Servlets to Fix

- [ ] DependencyServlet.java
- [ ] FieldValueServlet.java
- [ ] FileReferenceServlet.java
- [ ] FinanceApprovalServlet.java
- [ ] UserManagementServlet.java
- [ ] AuditServlet.java
- [ ] FileServlet.java
- [ ] ModuleServlet.java
- [ ] WorkflowStepServlet.java

## Fix Steps for Each Servlet

### ☑️ Step 1: Add Imports
Add these two imports at the top of the file:
```java
import com.tickettracker.util.ByteArrayUtil;
import com.tickettracker.util.UuidUtil;
```

### ☑️ Step 2: Find All `hexToBytes()` Calls
Search the file for `hexToBytes(` and note each occurrence.

### ☑️ Step 3: Replace Each Call
For each `hexToBytes()` call:

**Before:**
```java
byte[] id = hexToBytes(someParam);
```

**After:**
```java
byte[] id = ByteArrayUtil.hexToBytes(someParam);
```

### ☑️ Step 4: Add Validation (Optional but Recommended)
For critical UUID parameters, add validation:
```java
if (paramValue != null && !UuidUtil.isValidUuid(paramValue)) {
    sendError(response, 400, "Invalid UUID format");
    return;
}
```

### ☑️ Step 5: Add Debug Logging (Optional but Recommended)
```java
logger.debug("Processing UUID parameter: {}", paramValue);
byte[] bytes = ByteArrayUtil.hexToBytes(paramValue);
logger.debug("Converted to bytes: {}", ByteArrayUtil.bytesToHex(bytes));
```

### ☑️ Step 6: Remove Duplicate Method
Find and delete the entire `hexToBytes()` method from the servlet:
```java
private byte[] hexToBytes(String hex) {
    // ... DELETE THIS ENTIRE METHOD ...
}
```

### ☑️ Step 7: Verify
- Code compiles without errors
- All hexToBytes references now use ByteArrayUtil
- No duplicate hexToBytes method remains

## Common Patterns

### Pattern A: Path Parameter
```java
// FIND:
String[] pathParts = pathInfo.split("/");
byte[] id = hexToBytes(pathParts[1]);

// REPLACE WITH:
String[] pathParts = pathInfo.split("/");
logger.debug("Processing ID from path: {}", pathParts[1]);
byte[] id = ByteArrayUtil.hexToBytes(pathParts[1]);
```

### Pattern B: Query Parameter
```java
// FIND:
String paramId = request.getParameter("someId");
byte[] idBytes = hexToBytes(paramId);

// REPLACE WITH:
String paramId = request.getParameter("someId");
if (paramId != null && !UuidUtil.isValidUuid(paramId)) {
    sendError(response, 400, "Invalid UUID format");
    return;
}
logger.debug("Processing parameter: {}", paramId);
byte[] idBytes = ByteArrayUtil.hexToBytes(paramId);
```

### Pattern C: Request Body Field
```java
// FIND:
RequestObject obj = objectMapper.readValue(body, RequestObject.class);
byte[] id = hexToBytes(obj.getSomeId());

// REPLACE WITH:
RequestObject obj = objectMapper.readValue(body, RequestObject.class);
logger.debug("Processing ID from request: {}", obj.getSomeId());
byte[] id = ByteArrayUtil.hexToBytes(obj.getSomeId());
```

## Time Estimate

- Each servlet: ~10-15 minutes
- Total for all 9 servlets: ~2 hours

## Testing After Each Fix

1. Compile the project
2. Test the affected endpoints
3. Check logs for proper UUID conversion
4. Verify database operations work correctly

## Command to Find hexToBytes Usage

```bash
# Search for hexToBytes usage in a specific servlet
grep -n "hexToBytes" /path/to/SomeServlet.java

# Count occurrences
grep -c "hexToBytes" /path/to/SomeServlet.java
```

## Example: Complete Fix for One Method

**Before:**
```java
@Override
protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
    String pathInfo = request.getPathInfo();

    try {
        if (pathInfo == null || pathInfo.equals("/")) {
            handleGetAll(request, response);
        } else {
            String[] pathParts = pathInfo.split("/");
            if (pathParts.length == 2) {
                byte[] id = hexToBytes(pathParts[1]);
                handleGetById(id, response);
            }
        }
    } catch (Exception e) {
        logger.error("Error in doGet", e);
        sendError(response, 500, "Internal server error");
    }
}
```

**After:**
```java
@Override
protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
    String pathInfo = request.getPathInfo();

    try {
        if (pathInfo == null || pathInfo.equals("/")) {
            handleGetAll(request, response);
        } else {
            String[] pathParts = pathInfo.split("/");
            if (pathParts.length == 2) {
                logger.debug("Processing ID from path: {}", pathParts[1]);

                if (!UuidUtil.isValidUuid(pathParts[1])) {
                    sendError(response, 400, "Invalid UUID format");
                    return;
                }

                byte[] id = ByteArrayUtil.hexToBytes(pathParts[1]);
                logger.debug("Converted to bytes: {}", ByteArrayUtil.bytesToHex(id));
                handleGetById(id, response);
            }
        }
    } catch (Exception e) {
        logger.error("Error in doGet", e);
        sendError(response, 500, "Internal server error");
    }
}
```

## Automated Search and Replace (Be Careful!)

If you want to do bulk replacement, you can use sed, but review changes carefully:

```bash
# Backup first!
cp SomeServlet.java SomeServlet.java.bak

# Replace hexToBytes calls (simple cases only)
sed -i 's/hexToBytes(/ByteArrayUtil.hexToBytes(/g' SomeServlet.java

# Note: This won't add imports or remove the duplicate method
# You still need to do those manually
```

## Quality Checklist

After fixing all servlets, verify:

- [ ] All servlets compile successfully
- [ ] No servlet has a local `hexToBytes()` method
- [ ] All UUID conversions use `ByteArrayUtil.hexToBytes()`
- [ ] Debug logging is present for tracing
- [ ] UUID validation is added where critical
- [ ] All endpoints tested with valid UUIDs
- [ ] All endpoints tested with invalid UUIDs (should return 400)
- [ ] Application logs show proper UUID conversion

## Get Help

If you encounter issues:
1. Check `UUID_CONVERSION_FIX_IMPLEMENTATION.md` for detailed explanations
2. Compare with the fixed `TicketServlet.java` as a reference
3. Review the logs for specific error messages
4. Verify UUID format being sent from frontend matches expectations
