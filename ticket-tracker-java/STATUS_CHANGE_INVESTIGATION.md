# Java Package - Status Change Investigation

## Overview

This document addresses potential status change issues in the Java-based ticket tracker implementation that uses Oracle Database 19c.

## Architecture Differences

### Boltt Package (Fixed)
- **Database**: Supabase PostgreSQL
- **Security**: Row-Level Security (RLS) policies
- **Authentication**: Custom user management with anon role
- **Issue**: RLS policy violations preventing status changes
- **Fix**: Updated RLS policies to support custom auth

### Java Package (This Package)
- **Database**: Oracle Database 19c
- **Security**: Application-layer authentication
- **Authentication**: Session-based servlet authentication
- **Issue**: TBD - Needs investigation if problems exist
- **Fix**: Different approach required

## Potential Issues to Investigate

### 1. Database Constraint Violations

**Check Oracle Constraints:**
```sql
-- Check status column constraints
SELECT constraint_name, constraint_type, search_condition
FROM user_constraints
WHERE table_name = 'TICKETS'
AND constraint_type = 'C'
AND constraint_name LIKE '%STATUS%';

-- Check foreign key constraints
SELECT constraint_name, constraint_type, r_constraint_name
FROM user_constraints
WHERE table_name = 'FINANCE_APPROVALS'
AND constraint_type = 'R';
```

**Common Constraint Issues:**
- CHECK constraint on status column may reject new values
- Foreign key constraint on FINANCE_OFFICER_ID may reject invalid UUIDs
- NOT NULL constraints on required fields

### 2. Transaction Management

**Check for Transaction Issues:**

The Java servlets should use proper transaction management:

```java
Connection conn = null;
try {
    conn = dataSource.getConnection();
    conn.setAutoCommit(false);

    // Update ticket status
    updateTicketStatus(conn, ticketId, newStatus);

    // Insert audit log
    insertAuditLog(conn, ticketId, userId, action);

    // Insert finance approval if needed
    if (sentToFinance) {
        insertFinanceApproval(conn, approvalData);
    }

    conn.commit();
} catch (SQLException e) {
    if (conn != null) {
        try {
            conn.rollback();
        } catch (SQLException ex) {
            logger.error("Rollback failed", ex);
        }
    }
    throw new DatabaseException("Failed to change status", e);
} finally {
    if (conn != null) {
        try {
            conn.close();
        } catch (SQLException e) {
            logger.error("Failed to close connection", e);
        }
    }
}
```

### 3. Servlet Error Handling

**Check TicketServlet.java:**

Currently, the servlet may not have a dedicated status change endpoint. Status changes might be handled through the generic PUT endpoint:

```java
@Override
protected void doPut(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
    // Check if this includes status change logic
}
```

**Recommended Enhancement:**

Add a dedicated status change endpoint:

```java
// Add to TicketServlet.java
private void handleStatusChange(HttpServletRequest request, HttpServletResponse response)
        throws IOException, TicketTrackerException {

    String body = getRequestBody(request);
    StatusChangeRequest statusChange = objectMapper.readValue(body, StatusChangeRequest.class);

    User currentUser = getCurrentUser(request);
    if (currentUser == null) {
        sendError(response, 401, "Authentication required");
        return;
    }

    // Validate user has permission to change status
    if (!currentUser.getRole().equalsIgnoreCase("EO")) {
        sendError(response, 403, "Only EO users can change ticket status");
        return;
    }

    try {
        ticketService.changeTicketStatus(
            statusChange.getTicketId(),
            statusChange.getNewStatus(),
            statusChange.getRemarks(),
            currentUser.getId()
        );

        response.setStatus(HttpServletResponse.SC_OK);
        sendJsonResponse(response, new SuccessResponse("Status changed successfully"));

    } catch (SQLException e) {
        logger.error("Database error changing status", e);

        // Parse Oracle error code
        String errorMessage = parseOracleError(e);
        sendError(response, 500, errorMessage);

    } catch (ValidationException e) {
        logger.warn("Validation error changing status", e);
        sendError(response, 400, e.getMessage());
    }
}

private String parseOracleError(SQLException e) {
    int errorCode = e.getErrorCode();

    switch (errorCode) {
        case 2290: // ORA-02290: check constraint violated
            return "Invalid status value. Please select a valid status.";
        case 2291: // ORA-02291: integrity constraint violated - parent key not found
            return "Invalid reference. Selected user or data does not exist.";
        case 1: // ORA-00001: unique constraint violated
            return "Duplicate entry. This operation has already been performed.";
        default:
            return "Database error: " + e.getMessage();
    }
}
```

### 4. Oracle Database Setup Verification

**Check Database Schema:**

```bash
# Navigate to database directory
cd ticket-tracker-java/database

# Review schema files
cat 02-oracle-schema.sql | grep -i status
cat 06-oracle-constraints.sql | grep -i tickets
```

**Verify Tables Exist:**

```sql
-- Check if tables exist
SELECT table_name FROM user_tables
WHERE table_name IN ('TICKETS', 'FINANCE_APPROVALS', 'AUDIT_LOGS');

-- Check if sequences exist
SELECT sequence_name FROM user_sequences
WHERE sequence_name LIKE '%TICKET%' OR sequence_name LIKE '%FINANCE%';

-- Check if triggers are enabled
SELECT trigger_name, status FROM user_triggers
WHERE table_name IN ('TICKETS', 'FINANCE_APPROVALS');
```

### 5. Frontend REST API Integration

**Check API Client:**

The REST frontend (`ticket-tracker-rest/frontend`) should properly handle status change API calls:

```typescript
// In ticket-tracker-rest/frontend/src/lib/apiClient.ts
async function changeTicketStatus(ticketId: string, statusData: any) {
    try {
        const response = await fetch(`${API_BASE_URL}/api/tickets/${ticketId}/status`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${getAuthToken()}`
            },
            body: JSON.stringify(statusData)
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to change status');
        }

        return await response.json();
    } catch (error) {
        console.error('Status change error:', error);
        throw error;
    }
}
```

## Testing Steps for Java Package

### 1. Check If Issue Exists

1. Build and deploy the Java application:
   ```bash
   cd ticket-tracker-java
   mvn clean package
   # Deploy ticket-tracker.war to Tomcat
   ```

2. Access the application and attempt a status change

3. If it fails, check:
   - Tomcat logs: `$TOMCAT_HOME/logs/catalina.out`
   - Application logs: Look for Log4j2 output
   - Browser console (F12)

### 2. Check Oracle Database Logs

```sql
-- Check for recent errors
SELECT * FROM v$diag_alert_ext
WHERE originating_timestamp > SYSDATE - 1/24
ORDER BY originating_timestamp DESC;

-- Check for constraint violations
SELECT constraint_name, table_name
FROM user_constraints
WHERE status = 'DISABLED';
```

### 3. Enable Detailed Logging

Update `src/main/resources/log4j2.xml`:

```xml
<Logger name="com.tickettracker.service.TicketService" level="DEBUG" additivity="false">
    <AppenderRef ref="Console"/>
    <AppenderRef ref="File"/>
</Logger>

<Logger name="com.tickettracker.dao" level="DEBUG" additivity="false">
    <AppenderRef ref="Console"/>
    <AppenderRef ref="File"/>
</Logger>
```

### 4. Test Specific Scenarios

- Change status to each valid value: DRAFT, CREATED, APPROVED, ACTIVE, COMPLETED, etc.
- Submit to finance with all required fields
- Test with different user roles
- Test with missing required fields (should fail gracefully)

## Recommended Fixes for Java Package

### Fix 1: Add Dedicated Status Change Endpoint

Create `StatusChangeRequest.java`:

```java
package com.tickettracker.model;

public class StatusChangeRequest {
    private String ticketId;
    private String newStatus;
    private String remarks;
    private FinanceApprovalData financeData;

    // Getters and setters
}
```

Update TicketServlet to handle `/api/tickets/{id}/status` endpoint.

### Fix 2: Improve Exception Handling

Wrap all database operations in try-catch blocks and provide meaningful error messages to the frontend.

### Fix 3: Add Transaction Support

Use proper transaction management with commit/rollback for all multi-step operations.

### Fix 4: Validate Data Before Database Operations

Add validation layer in service classes before attempting database updates.

## Comparison with Boltt Package Fix

| Aspect | Boltt Package | Java Package |
|--------|---------------|--------------|
| **Root Cause** | RLS policy violations | TBD - Likely constraint violations |
| **Fix Approach** | Update RLS policies | Update servlet logic + error handling |
| **Database Changes** | Migration script | May need constraint updates |
| **Code Changes** | Service layer error handling | Servlet + DAO + Service layers |
| **Testing** | Browser console + Supabase logs | Tomcat logs + Oracle logs |

## Summary

The Java package requires separate investigation because:

1. **Different database** (Oracle vs PostgreSQL)
2. **Different security model** (Application vs RLS)
3. **Different architecture** (Servlets vs Supabase API)

If you're experiencing status change issues in the Java package, please:

1. Check Tomcat logs for servlet errors
2. Check Oracle database for constraint violations
3. Enable DEBUG logging for detailed trace
4. Share error logs for specific diagnosis

The Boltt package fix (RLS policy updates) does NOT apply to the Java package.
