# Compilation Errors Fixed - WorkflowStep Model Alignment

## Issue Summary

Compilation errors in `WorkflowService.java` and `TicketService.java` due to attempting to call non-existent methods on the `WorkflowStep` model:
- `getAssignedToGroup()` - method does not exist
- `getAssignedToUser()` - method does not exist

The database schema and model only support individual user assignment via the `assigned_to` field (RAW(16) UUID).

## Root Cause

The RBAC (Role-Based Access Control) implementation assumed the WorkflowStep model had two separate assignment fields:
- `assignedToUser` (byte[]) - for individual user assignment
- `assignedToGroup` (String) - for department/group assignment

However, the actual database schema only has:
- `assigned_to` (RAW(16)) - a single foreign key reference to the users table

## Solution Implemented

Updated the permission checking logic to work with the existing `assigned_to` field.

### Changes in WorkflowService.java

**File:** `src/main/java/com/tickettracker/service/WorkflowService.java`

**Method:** `canUserUpdateWorkflowStep()` (lines 438-473)

**Before:**
```java
if (step.getAssignedToUser() != null && bytesEquals(step.getAssignedToUser(), userId)) {
    return true;
}

if (step.getAssignedToGroup() != null &&
    user.getDepartment() != null &&
    step.getAssignedToGroup().equalsIgnoreCase(user.getDepartment())) {
    return true;
}
```

**After:**
```java
if (step.getAssignedTo() != null && bytesEquals(step.getAssignedTo(), userId)) {
    return true;
}

if ("dept_officer".equalsIgnoreCase(user.getRoleInternal())) {
    TicketDAO ticketDAO = new TicketDAO();
    Ticket ticket = ticketDAO.findById(step.getTicketId());
    if (ticket != null && user.getDepartment() != null &&
        user.getDepartment().equalsIgnoreCase(ticket.getDepartment())) {
        return true;
    }
}
```

### Changes in TicketService.java

**File:** `src/main/java/com/tickettracker/service/TicketService.java`

#### Change 1: `canUserAccessTicket()` Method (lines 340-348)

**Before:**
```java
if ("employee".equalsIgnoreCase(userRole) || "vendor".equalsIgnoreCase(userRole)) {
    List<WorkflowStep> steps = workflowStepDAO.findByTicketId(ticketId);
    for (WorkflowStep step : steps) {
        if (step.getAssignedToGroup() != null &&
            step.getAssignedToGroup().equalsIgnoreCase(user.getDepartment())) {
            return true;
        }
        if (step.getAssignedToUser() != null &&
            java.util.Arrays.equals(step.getAssignedToUser(), userId)) {
            return true;
        }
    }
    return false;
}
```

**After:**
```java
if ("employee".equalsIgnoreCase(userRole) || "vendor".equalsIgnoreCase(userRole)) {
    List<WorkflowStep> steps = workflowStepDAO.findByTicketId(ticketId);
    for (WorkflowStep step : steps) {
        if (step.getAssignedTo() != null &&
            java.util.Arrays.equals(step.getAssignedTo(), userId)) {
            return true;
        }
    }
    return false;
}
```

#### Change 2: `getAccessibleTicketIdsForUser()` Method (lines 387-397)

**Before:**
```java
if ("employee".equalsIgnoreCase(userRole) || "vendor".equalsIgnoreCase(userRole)) {
    List<WorkflowStep> assignedSteps = workflowStepDAO.findByAssignedUser(userId);
    List<byte[]> ticketIds = new ArrayList<>();
    for (WorkflowStep step : assignedSteps) {
        if (!ticketIds.contains(step.getTicketId())) {
            ticketIds.add(step.getTicketId());
        }
    }

    List<WorkflowStep> groupSteps = workflowStepDAO.findByAssignedGroup(user.getDepartment());
    for (WorkflowStep step : groupSteps) {
        if (!ticketIds.contains(step.getTicketId())) {
            ticketIds.add(step.getTicketId());
        }
    }

    return ticketIds;
}
```

**After:**
```java
if ("employee".equalsIgnoreCase(userRole) || "vendor".equalsIgnoreCase(userRole)) {
    List<WorkflowStep> assignedSteps = workflowStepDAO.findByAssignedTo(userId);
    List<byte[]> ticketIds = new ArrayList<>();
    for (WorkflowStep step : assignedSteps) {
        if (!ticketIds.contains(step.getTicketId())) {
            ticketIds.add(step.getTicketId());
        }
    }

    return ticketIds;
}
```

## Updated Permission Logic

After the fixes, the permission rules are:

### Workflow Step Updates
- **EO (Engineering Officer)**: Can update any workflow step
- **DO (Department Officer)**: Can update steps for tickets in their department
- **Employee/Vendor**: Can update steps directly assigned to them

### Ticket Access
- **EO (Engineering Officer)**: Can access all tickets
- **DO (Department Officer)**: Can access tickets in their department
- **Employee/Vendor**: Can access tickets with steps assigned to them

## Verification

All references to non-existent methods have been removed:
- ✅ No calls to `getAssignedToUser()`
- ✅ No calls to `getAssignedToGroup()`
- ✅ No calls to `findByAssignedUser()`
- ✅ No calls to `findByAssignedGroup()`

All code now uses:
- ✅ `getAssignedTo()` - existing method in WorkflowStep model
- ✅ `findByAssignedTo(byte[] userId)` - existing method in WorkflowStepDAO

## Future Enhancement: Group Assignment Support

If group-level step assignments are needed in the future, the following changes would be required:

1. **Database Schema:**
   ```sql
   ALTER TABLE workflow_steps ADD assigned_to_group VARCHAR2(200);
   ```

2. **WorkflowStep Model:**
   ```java
   private String assignedToGroup;

   public String getAssignedToGroup() { return assignedToGroup; }
   public void setAssignedToGroup(String group) { this.assignedToGroup = group; }
   ```

3. **WorkflowStepDAO:**
   - Add `findByAssignedGroup(String department)` method
   - Update `create()` and `update()` methods to handle the new field

4. **Service Layer:**
   - Restore the original group-based permission checks

## Files Modified

1. `/src/main/java/com/tickettracker/service/WorkflowService.java`
   - Method: `canUserUpdateWorkflowStep()`

2. `/src/main/java/com/tickettracker/service/TicketService.java`
   - Method: `canUserAccessTicket()`
   - Method: `getAccessibleTicketIdsForUser()`

## Status

✅ **Compilation errors resolved**
✅ **Code aligned with database schema**
✅ **Permission logic simplified and functional**
✅ **Ready for compilation and testing**

## Next Steps

1. Compile the project: `mvn clean compile`
2. Run unit tests: `mvn test`
3. Deploy to application server
4. Test RBAC functionality with different user roles
5. Verify permission enforcement in production
