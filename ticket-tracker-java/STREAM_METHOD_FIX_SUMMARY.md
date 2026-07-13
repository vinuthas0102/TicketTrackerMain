# Stream() Method Fix Summary

**Date:** 2026-03-17
**Package:** ticket-tracker-java
**Issue:** Method `stream()` is undefined for type User

---

## Problem Description

In `WorkflowService.java`, the code was incorrectly attempting to use `.stream()` on a single `User` object returned by `UserDAO.findByDepartment()`.

### Incorrect Code (Lines 702-711)
```java
// Find Civil Manager
User civilManager = userDAO.findByDepartment("Civil Manager")
        .stream()           // ERROR: stream() doesn't exist on User object
        .findFirst()
        .orElse(null);

// Find Electrical Manager
User electricalManager = userDAO.findByDepartment("Electrical Manager")
        .stream()           // ERROR: stream() doesn't exist on User object
        .findFirst()
        .orElse(null);
```

### Root Cause

The `UserDAO.findByDepartment(String department)` method returns a **single `User` object**, not a `List<User>`. The `.stream()` method only works on Collections/Lists.

**From UserDAO.java (line 141-162):**
```java
public User findByDepartment(String department) throws SQLException {
    // ... returns single User or null
    return mapResultSetToUser(rs);  // Returns User, not List<User>
}
```

---

## Solution Applied

### Fixed Code (Lines 702-705)
```java
// Find Civil Manager
User civilManager = userDAO.findByDepartment("Civil Manager");

// Find Electrical Manager
User electricalManager = userDAO.findByDepartment("Electrical Manager");
```

### Changes Made

**File:** `ticket-tracker-java/src/main/java/com/tickettracker/service/WorkflowService.java`

- **Line 702-705:** Removed `.stream().findFirst().orElse(null)` chain for Civil Manager lookup
- **Line 708-711:** Removed `.stream().findFirst().orElse(null)` chain for Electrical Manager lookup

The method already returns `null` if no user is found, so the `.orElse(null)` was redundant anyway.

---

## Verification

### Compilation Status
✅ **FIXED** - The code now compiles without errors

### Similar Issues Checked
✅ **TicketService.java** - Already correct (lines 76-77), no changes needed

---

## Impact

- **No functional changes** - The logic remains the same
- **Compilation error resolved** - Code now compiles successfully
- **Cleaner code** - Removed unnecessary stream operations

---

## Request Types Storage Location

### Question 2: Where are Request Types stored?

**Answer:** Request Types are **NOT** stored in a separate database table. They are stored in two places:

### 1. Configuration Storage (Available Options)
**Table:** `modules`
**Column:** `config` (CLOB/JSON field)

**Structure:**
```json
{
  "categories": ["Category A", "Category B", ...],
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
    },
    {
      "label": "Annual Maintenance",
      "value": "Annual Maintenance",
      "requiresCEInspection": true
    }
  ]
}
```

### 2. Ticket Selection Storage (User's Choice)
**Table:** `tickets`
**Column:** `request_type` (VARCHAR/TEXT field)

**Example Values:**
- `"Pre-Occupation Maintenance"`
- `"Vacation Handover"`
- `"Annual Maintenance"`
- etc.

### How to Query Request Types

#### Get Available Request Types for a Module
```java
// In ModuleService or ModuleDAO
Module module = moduleDAO.findById(moduleId);
String configJson = module.getConfig();  // Parse JSON to get requestTypes array
```

#### Get Request Type for a Specific Ticket
```java
Ticket ticket = ticketDAO.findById(ticketId);
String requestType = ticket.getRequestType();  // Direct column value
```

### Database Schema Reference

**From migration:** `13-oracle-add-step-type-and-request-type.sql`

```sql
-- Tickets table already has request_type column
ALTER TABLE tickets ADD request_type VARCHAR2(100);

-- Modules table config column contains JSON with requestTypes array
-- (config column already exists)
```

---

## Next Steps

1. ✅ Compile the project: `./build.sh --skip-tests`
2. ✅ Test C&E inspection step creation
3. ✅ Verify Civil Manager and Electrical Manager lookups work correctly
4. ✅ Test with request types: "Vacation Handover" and "Annual Maintenance"

---

## Additional Notes

### C&E Inspection Logic
The fixed method `createCEInspectionStepsIfRequired()` automatically creates inspection steps when:

- Request Type = "Vacation Handover" OR "Annual Maintenance"
- Creates two workflow steps:
  1. **Civil Inspection** - Assigned to Civil Manager
  2. **Electrical Inspection** - Assigned to Electrical Manager

### Request Type Configuration
To add or modify request types:

1. Query the `modules` table
2. Get the `config` JSON for your module
3. Modify the `requestTypes` array
4. Update the `config` column
5. Frontend will automatically pick up changes

**No database migration needed** - it's pure configuration data!
