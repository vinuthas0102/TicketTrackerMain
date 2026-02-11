# Implementation Next Steps Guide

## Overview
This document provides a roadmap for completing the remaining security and business logic features identified in the comprehensive implementation plan.

---

## Phase 2: Business Logic & Validation (Priority: HIGH)

### 2.1 Workflow State Machine Implementation

**Goal:** Implement a finite state machine to validate ticket status transitions.

**Files to Create:**
- `src/main/java/com/tickettracker/service/TicketStateMachine.java`

**Implementation Steps:**
1. Define valid ticket statuses as enum:
   - DRAFT, CREATED, APPROVED, ACTIVE, COMPLETED, CLOSED, CANCELLED
   - SENT_TO_FINANCE, APPROVED_BY_FINANCE (finance-specific)

2. Create transition matrix:
   ```java
   Map<String, Map<String, Set<String>>> transitions;
   // Key: from status -> Map of role -> Set of allowed to statuses
   ```

3. Implement validation method:
   ```java
   public boolean canTransition(String fromStatus, String toStatus, String userRole)
   ```

4. Define business rules:
   - COMPLETED requires all workflow steps completed
   - SENT_TO_FINANCE requires finance_approval flag set
   - CLOSED requires COMPLETED status first
   - EO can transition to CANCELLED from any state

5. Update `TicketService.updateStatus()` to use state machine

**Test Cases:**
- Valid transitions succeed
- Invalid transitions fail with descriptive error
- Role-specific transitions work correctly
- Edge cases (null statuses, unknown statuses)

---

### 2.2 Auto-Field Creation Service

**Goal:** Automatically create field values when tickets or workflow steps are created.

**Files to Create:**
- `src/main/java/com/tickettracker/service/FieldValueAutoPopulationService.java`

**Implementation Steps:**
1. Create service with methods:
   ```java
   public void populateTicketFields(byte[] ticketId, byte[] moduleId)
   public void populateWorkflowStepFields(byte[] stepId, byte[] workflowTemplateId)
   ```

2. Query field configurations:
   - For tickets: Query `field_configs` table by module_id
   - For steps: Query workflow template field definitions

3. Create field_values entries:
   - Set default values based on field type
   - Handle text, number, date, select, multiselect types
   - Link to ticket_id or workflow_step_id

4. Integrate with creation methods:
   - Call after `TicketService.createTicket()`
   - Call after `WorkflowService.createWorkflowStep()`
   - Wrap in transaction boundary

5. Add audit logging for field creation

**Test Cases:**
- Fields created for all configured field types
- Default values set correctly
- Transaction rolls back on failure
- Fields not duplicated on retry

---

### 2.3 Soft Delete Implementation

**Goal:** Replace hard deletes with soft deletes to preserve data integrity.

**Database Changes Required:**
Add columns to tables via Oracle migration scripts:
```sql
ALTER TABLE tickets ADD (
    deleted_at TIMESTAMP,
    deleted_by RAW(16),
    delete_reason VARCHAR2(500)
);

ALTER TABLE workflow_steps ADD (
    deleted_at TIMESTAMP,
    deleted_by RAW(16),
    delete_reason VARCHAR2(500)
);

-- Repeat for documents, users, etc.
```

**DAO Changes Required:**
Update all DAO delete methods:
```java
public boolean softDelete(byte[] id, byte[] deletedBy, String reason) {
    String sql = "UPDATE tickets SET deleted_at = ?, deleted_by = ?, " +
                 "delete_reason = ? WHERE id = ?";
    // Execute update
}
```

Update all DAO query methods:
```java
// Add WHERE deleted_at IS NULL to all SELECT queries
String sql = "SELECT * FROM tickets WHERE deleted_at IS NULL";
```

**Service Changes Required:**
- Add `deleteReason` parameter to all delete methods
- Validate reason is provided and not empty
- Log deletion to audit trail
- Create restore methods for EO users

**Test Cases:**
- Soft deleted records not returned in queries
- Deleted records preserve all data
- Deletion reason required
- Restore functionality works
- Foreign keys remain intact

---

### 2.4 Dependency Validation Enhancement

**Goal:** Implement cycle detection and dependency locking.

**Files to Modify:**
- `src/main/java/com/tickettracker/service/DependencyService.java`

**Implementation Steps:**

**Cycle Detection:**
1. Create graph representation:
   ```java
   private Map<byte[], List<byte[]>> buildDependencyGraph()
   ```

2. Implement DFS-based cycle detection:
   ```java
   public boolean hasCycle(byte[] stepId, Map<byte[], List<byte[]>> graph)
   ```

3. Call before creating/updating dependencies:
   ```java
   if (hasCycle(newDependency)) {
       throw new ValidationException("Circular dependency detected");
   }
   ```

**Dependency Locking:**
1. Add columns to workflow_step_dependencies table:
   ```sql
   ALTER TABLE workflow_step_dependencies ADD (
       locked_at TIMESTAMP,
       locked_by RAW(16)
   );
   ```

2. Implement locking methods:
   ```java
   public void lockDependencies(byte[] stepId, byte[] userId)
   public void unlockDependencies(byte[] stepId, byte[] userId)
   ```

3. Prevent modification of locked dependencies:
   ```java
   if (dependency.isLocked()) {
       throw new ForbiddenException("Cannot modify locked dependency");
   }
   ```

**Completion Checking:**
```java
public boolean areDependenciesMet(byte[] stepId) {
    List<Dependency> deps = getDependencies(stepId);
    for (Dependency dep : deps) {
        if (dep.getMode().equals("ALL") && !dep.isComplete()) {
            return false;
        }
    }
    return true;
}
```

**Test Cases:**
- Simple cycles detected (A→B→A)
- Complex cycles detected (A→B→C→A)
- Valid graphs pass validation
- Locked dependencies cannot be modified
- EO can override locks
- Dependency completion checked correctly

---

### 2.5 Comprehensive Audit Logging

**Goal:** Create a service to log all security and business events.

**Files to Create:**
- `src/main/java/com/tickettracker/service/AuditLogService.java`

**Implementation Steps:**
1. Create service with logging methods:
   ```java
   public void logUserManagementAction(String action, byte[] targetUserId,
                                      byte[] performedBy, Map<String, String> changes)
   public void logTicketStatusChange(byte[] ticketId, String oldStatus,
                                     String newStatus, byte[] performedBy)
   public void logFinanceDecision(byte[] approvalId, String decision,
                                  String remarks, byte[] performedBy)
   public void logFileOperation(String operation, byte[] fileId,
                               byte[] performedBy, String details)
   public void logPermissionViolation(String resource, String action,
                                     byte[] attemptedBy, String reason)
   ```

2. Enhance audit log entries with:
   - IP address of requester
   - User agent string
   - Before/after values for updates
   - Reason/justification for actions

3. Integrate throughout services:
   - UserService: Creation, role changes, disable/enable
   - TicketService: All status transitions
   - FinanceApprovalService: All approval decisions
   - FileService: Uploads, downloads, deletions
   - All servlets: Permission violations

4. Add structured logging:
   ```java
   AuditLog log = new AuditLog();
   log.setEntityType("ticket");
   log.setEntityId(ticketId);
   log.setAction("status_change");
   log.setOldValue(oldStatus);
   log.setNewValue(newStatus);
   log.setPerformedBy(userId);
   log.setIpAddress(request.getRemoteAddr());
   log.setTimestamp(new Timestamp(System.currentTimeMillis()));
   ```

**Test Cases:**
- All actions logged correctly
- Structured data parseable
- Old/new values captured
- IP addresses recorded
- User context preserved

---

## Phase 3: Missing API Endpoints (Priority: MEDIUM)

### 3.1 Progress History Endpoints

**Servlet to Create:**
- `ProgressHistoryServlet.java` mapped to `/api/workflow-steps/{stepId}/progress-history`

**Endpoints:**
- `POST /api/workflow-steps/{stepId}/progress-history`
  - Accept multipart file upload
  - Store file and description
  - Link to workflow step
  - Return created progress entry

**Implementation:**
```java
@WebServlet("/api/workflow-steps/*/progress-history")
public class ProgressHistoryServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        // Parse multipart form data
        // Validate user has access to step
        // Store file to database or file system
        // Create progress_document record
        // Return created document with metadata
    }
}
```

---

### 3.2 Dependency Management Endpoints

**Add to DependencyServlet:**

**Lock Dependencies:**
```java
PUT /api/steps/{stepId}/lock-dependencies
- Requires EO role or assigned user
- Sets locked_at timestamp
- Sets locked_by to current user
- Logs lock action
```

**Unlock Dependencies:**
```java
PUT /api/steps/{stepId}/unlock-dependencies
- Requires EO role or assigned user
- Clears lock
- Logs unlock action with reason
```

**Get Dependency Status:**
```java
GET /api/steps/{stepId}/dependencies/status
- Returns detailed status of each dependency
- Shows completion percentage
- Indicates blocking vs non-blocking
- Returns whether step can start
```

---

### 3.3 Ticket Operations Endpoints

**Add to TicketServlet:**

**Copy Attachments:**
```java
POST /api/tickets/{ticketId}/copy-attachments
Body: { "targetTicketId": "uuid" }
- Validate user has access to both tickets
- Copy all documents and file references
- Preserve metadata
- Update audit trail
```

**Check Completion Certificate:**
```java
GET /api/tickets/{ticketId}/completion-certificate/exists
Response: {
    "exists": true,
    "fileId": "uuid",
    "uploadedAt": "2026-02-11T10:30:00Z",
    "uploadedBy": "user@example.com"
}
```

**Check Access:**
```java
GET /api/tickets/{ticketId}/access
Response: {
    "hasAccess": true,
    "reason": "assigned_to_workflow_step",
    "permissionLevel": "write"
}
```

---

### 3.4 User Management Endpoints

**Servlet to Create:**
- `UserManagementServlet.java` mapped to `/api/admin/users/*`

**Endpoints:**

```java
PUT /api/admin/users/{userId}/enable
PUT /api/admin/users/{userId}/disable
PUT /api/admin/users/{userId}/lock
PUT /api/admin/users/{userId}/unlock
```

**Requirements:**
- All endpoints require EO role
- Require reason parameter for disable/lock
- Log all actions to audit trail
- Return updated user object

---

## Phase 4: Response Standardization (Priority: MEDIUM)

### 4.1 Response Wrapper Utility

**File to Create:**
- `src/main/java/com/tickettracker/util/ResponseWrapper.java`

**Implementation:**
```java
public class ResponseWrapper<T> {
    private boolean success;
    private T data;
    private String message;
    private ErrorDetails error;

    public static <T> ResponseWrapper<T> success(T data) {
        ResponseWrapper<T> wrapper = new ResponseWrapper<>();
        wrapper.setSuccess(true);
        wrapper.setData(data);
        return wrapper;
    }

    public static <T> ResponseWrapper<T> error(String code, String message) {
        ResponseWrapper<T> wrapper = new ResponseWrapper<>();
        wrapper.setSuccess(false);
        wrapper.setError(new ErrorDetails(code, message));
        return wrapper;
    }
}

public class ErrorDetails {
    private String code;
    private String message;
    private Map<String, String> validationErrors;
}
```

**Update All Servlets:**
```java
// Success
sendJsonResponse(response, ResponseWrapper.success(data));

// Error
sendJsonResponse(response, ResponseWrapper.error("VALIDATION_FAILED", "Invalid input"));
```

---

## Phase 5: Performance Optimizations (Priority: LOW)

### 5.1 Connection Pooling

**Add Dependency to pom.xml:**
```xml
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>5.0.1</version>
</dependency>
```

**Create Configuration:**
```java
public class ConnectionPoolConfig {
    private static HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.getProperty("db.url"));
        config.setUsername(properties.getProperty("db.username"));
        config.setPassword(properties.getProperty("db.password"));
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
```

### 5.2 Database Indexes

**Create Migration Script:**
```sql
-- Tickets table indexes
CREATE INDEX idx_tickets_created_by ON tickets(created_by);
CREATE INDEX idx_tickets_status ON tickets(status);
CREATE INDEX idx_tickets_department ON tickets(department);
CREATE INDEX idx_tickets_module_id ON tickets(module_id);

-- Workflow steps table indexes
CREATE INDEX idx_workflow_steps_ticket_id ON workflow_steps(ticket_id);
CREATE INDEX idx_workflow_steps_status ON workflow_steps(status);
CREATE INDEX idx_workflow_steps_assigned_to_user ON workflow_steps(assigned_to_user);
CREATE INDEX idx_workflow_steps_assigned_to_group ON workflow_steps(assigned_to_group);

-- Documents table indexes
CREATE INDEX idx_documents_ticket_id ON documents(ticket_id);
CREATE INDEX idx_documents_step_id ON documents(workflow_step_id);

-- Audit logs table indexes
CREATE INDEX idx_audit_logs_entity_type_id ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_performed_by ON audit_logs(performed_by);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
```

### 5.3 Caching Layer

**Option 1: Caffeine (In-Memory)**
```java
LoadingCache<String, User> userCache = Caffeine.newBuilder()
    .maximumSize(1000)
    .expireAfterWrite(5, TimeUnit.MINUTES)
    .build(key -> userDAO.findById(key));
```

**Option 2: Redis (Distributed)**
```java
Jedis jedis = new Jedis("localhost", 6379);
String cachedUser = jedis.get("user:" + userId);
if (cachedUser == null) {
    User user = userDAO.findById(userId);
    jedis.setex("user:" + userId, 300, serialize(user));
}
```

**Cache Invalidation Strategy:**
- Invalidate on update
- TTL-based expiration
- Cache warming on startup

---

## Testing Strategy

### Unit Testing Framework
Use JUnit 5 and Mockito:
```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.9.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>5.3.1</version>
    <scope>test</scope>
</dependency>
```

### Test Categories

**1. Unit Tests**
- Service logic tests
- State machine validation tests
- Cycle detection algorithm tests
- Permission checking methods tests

**2. Integration Tests**
- Complete ticket workflow tests
- File upload and retrieval tests
- CSRF protection end-to-end tests
- Rate limiting under concurrent load

**3. Security Tests**
- RBAC enforcement tests for each role
- CSRF attack simulation tests
- Session fixation attack tests
- Rate limiting bypass attempts

**4. Performance Tests**
- Load test critical endpoints
- Measure query performance
- Test connection pool under load
- Validate cache effectiveness

---

## Deployment Checklist

- [ ] All unit tests passing
- [ ] Integration tests passing
- [ ] Security tests passing
- [ ] Performance benchmarks met
- [ ] Database migrations tested
- [ ] Configuration documented
- [ ] Monitoring configured
- [ ] Logging configured
- [ ] Security headers validated
- [ ] CSRF protection tested
- [ ] Rate limiting tested
- [ ] Session management tested
- [ ] RBAC enforcement verified
- [ ] Audit logging working
- [ ] Error handling comprehensive
- [ ] Documentation complete

---

## Priority Order

**Phase 1 (Complete):**
✅ RBAC enforcement in TicketServlet
✅ RBAC enforcement in WorkflowStepServlet
✅ CSRF protection
✅ Rate limiting
✅ Session security
✅ Security headers

**Phase 2 (Next - Business Logic):**
1. Soft delete implementation (highest impact on data safety)
2. Workflow state machine (prevents invalid state transitions)
3. Audit logging service (comprehensive security logging)
4. Dependency validation with cycle detection (data integrity)
5. Auto-field creation service (feature completeness)

**Phase 3 (API Completeness):**
1. User management endpoints (admin functionality)
2. Progress history endpoints (feature parity)
3. Dependency management endpoints (workflow features)
4. Ticket operations endpoints (convenience features)

**Phase 4 (Polish):**
1. Response standardization (API consistency)
2. Performance optimizations (scalability)
3. Comprehensive testing (quality assurance)

---

## Estimated Effort

- **Phase 2 (Business Logic)**: 3-4 weeks
- **Phase 3 (API Endpoints)**: 2 weeks
- **Phase 4 (Polish & Testing)**: 2-3 weeks

**Total remaining: 7-9 weeks** for complete implementation

---

## Getting Started

To continue implementation:

1. Review the SECURITY_IMPLEMENTATION_SUMMARY.md for what's been completed
2. Choose a task from Phase 2 (highest priority)
3. Create the necessary files as outlined above
4. Implement the functionality with proper error handling
5. Add comprehensive logging
6. Write unit tests
7. Test the feature manually
8. Update the implementation summary
9. Move to the next task

**Good luck with the remaining implementation!**
