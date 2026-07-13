# Workflow Step Sub-Resource Endpoints Fix

## Problem Summary

The Java backend was returning HTTP 400 errors when the frontend tried to access workflow step sub-resources:

1. `/api/workflow-steps/{stepId}/files` - For fetching step documents
2. `/api/workflow-steps/{stepId}/progress-documents` - For fetching progress documents
3. `/api/workflow-steps/{stepId}/progress-history` - For fetching progress history

The `WorkflowStepServlet` was only configured to handle 2-part URL paths (e.g., `/api/workflow-steps/{id}`), causing it to return "Invalid request path" errors for 3-part paths.

## Root Cause

In `WorkflowStepServlet.doGet()`, the path parsing logic rejected any path with more than 2 parts:

```java
// OLD CODE
if (pathParts.length == 2) {
    handleGetStep(pathParts[1], response);
} else {
    sendError(response, 400, "Invalid request path");  // <-- This was the problem
}
```

## Solution Implemented

### 1. Updated doGet() Method

Modified the `doGet()` method in `WorkflowStepServlet` to handle 3-part paths:

```java
if (pathParts.length == 2) {
    handleGetStep(pathParts[1], response);
} else if (pathParts.length == 3) {
    String stepId = pathParts[1];
    String subResource = pathParts[2];

    switch (subResource) {
        case "files":
            handleGetStepFiles(stepId, response);
            break;
        case "progress-documents":
            handleGetProgressDocuments(stepId, request, response);
            break;
        case "progress-history":
            handleGetProgressHistory(stepId, response);
            break;
        default:
            sendError(response, 400, "Invalid sub-resource: " + subResource);
    }
}
```

### 2. Added Three Handler Methods

#### handleGetStepFiles()

Fetches all documents associated with a workflow step:

```java
private void handleGetStepFiles(String stepId, HttpServletResponse response)
        throws TicketTrackerException, IOException {
    logger.debug("Fetching files for step ID: {}", stepId);
    byte[] id = hexToBytes(stepId);
    List<Document> documents = documentService.getDocumentsByStepId(id);
    sendJsonResponse(response, documents);
}
```

#### handleGetProgressDocuments()

Fetches all progress documents for a workflow step:

```java
private void handleGetProgressDocuments(String stepId, HttpServletRequest request,
        HttpServletResponse response) throws TicketTrackerException, IOException {
    logger.debug("Fetching progress documents for step ID: {}", stepId);
    byte[] id = hexToBytes(stepId);

    List<ProgressDocument> documents = progressDocumentService.getProgressDocumentsByStepId(id);
    sendJsonResponse(response, documents);
}
```

#### handleGetProgressHistory()

Aggregates data from multiple sources to build a complete progress history:

```java
private void handleGetProgressHistory(String stepId, HttpServletResponse response)
        throws TicketTrackerException, IOException {
    // 1. Fetch audit logs for the step
    List<AuditLog> auditLogs = auditLogDAO.findByStepId(id);

    // 2. Fetch all progress documents
    List<ProgressDocument> progressDocuments = new WorkflowStepProgressDocumentDAO().findByStepId(id);

    // 3. Fetch all documents (including completion certificates)
    List<Document> allDocuments = documentService.getDocumentsByStepId(id);

    // 4. Fetch all users for name/role lookup
    List<User> allUsers = userDAO.findAll();

    // 5. Build history entries by correlating data
    // - Link progress documents to audit logs via audit_log_id
    // - Filter completion certificates from regular documents
    // - Include user information in each entry

    // 6. Return as JSON
    sendJsonResponse(response, historyEntries);
}
```

### 3. Added Required Dependencies

Added service and DAO instances to the servlet:

```java
private WorkflowService workflowService;
private DocumentService documentService;
private WorkflowStepProgressDocumentService progressDocumentService;
private AuditLogDAO auditLogDAO;
private UserDAO userDAO;
```

Initialized them in the `init()` method:

```java
@Override
public void init() throws ServletException {
    super.init();
    this.workflowService = new WorkflowService();
    this.documentService = new DocumentService();
    this.progressDocumentService = new WorkflowStepProgressDocumentService();
    this.auditLogDAO = new AuditLogDAO();
    this.userDAO = new UserDAO();
    this.objectMapper = JsonUtil.getObjectMapper();
}
```

### 4. Added Required Imports

```java
import com.tickettracker.dao.AuditLogDAO;
import com.tickettracker.dao.UserDAO;
import com.tickettracker.dao.WorkflowStepProgressDocumentDAO;
import com.tickettracker.dao.WorkflowStepProgressDocumentDAO.ProgressDocument;
import com.tickettracker.model.AuditLog;
import com.tickettracker.model.Document;
import com.tickettracker.service.DocumentService;
import com.tickettracker.service.WorkflowStepProgressDocumentService;
import java.sql.SQLException;
import java.util.ArrayList;
```

## What Was NOT Changed

- No database schema changes required
- No changes to existing Service or DAO classes
- No changes to frontend code (it was already calling these endpoints correctly)
- All existing functionality remains unchanged

## Implementation Pattern

The implementation follows the exact same pattern used in `TicketServlet` for the `/tickets/{id}/files` endpoint, ensuring consistency across the codebase.

## Testing

After rebuilding and redeploying the WAR file, test the following:

1. **Step Documents**: Click "Show Documents" button in a workflow step
   - Should display all documents for that step
   - No HTTP 400 errors

2. **Progress Documents**: View progress documents section
   - Should display all progress documents
   - No HTTP 400 errors

3. **Progress History**: Click "Show Progress history" button
   - Should display complete audit trail with documents
   - Should include user names and roles
   - Should link progress documents to their audit log entries
   - No HTTP 400 errors

## Files Modified

- `/ticket-tracker-java/src/main/java/com/tickettracker/servlet/WorkflowStepServlet.java`

## Build Instructions

To rebuild the project:

```bash
cd ticket-tracker-java
./build.sh --skip-tests

# Or with Maven directly:
mvn clean package -DskipTests
```

Deploy the generated WAR file:
```bash
cp target/ticket-tracker.war $TOMCAT_HOME/webapps/
```

## Response Format

All endpoints return data in the standard format:

```json
{
  "success": true,
  "data": [ /* array of objects */ ]
}
```

Error responses:

```json
{
  "status": 400,
  "message": "Error message here"
}
```

## Additional Notes

- The `includeDeleted` query parameter for progress documents is currently ignored because the DAO method `findByStepId()` hardcodes `is_deleted = 0` filter. The default behavior (excluding deleted documents) matches the frontend's default parameter value.

- Progress history entries include all audit logs for the step, with nested arrays for related progress documents and completion certificates.

- The implementation uses Maps to avoid N+1 query problems when looking up user information.
