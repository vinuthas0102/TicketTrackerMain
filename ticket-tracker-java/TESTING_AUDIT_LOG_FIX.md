# Testing Audit Log Creation Fix

## Quick Test Guide

### Prerequisites
1. Build the updated WAR file
2. Deploy to Tomcat
3. Ensure database is accessible
4. Have a valid test ticket

### Test 1: Create Audit Log via API

**Endpoint:** `POST /api/audit-logs`

**Request:**
```json
{
  "ticketId": "existing-ticket-uuid",
  "stepId": "existing-step-uuid",
  "action": "PROGRESS_DOCUMENTS_UPLOADED",
  "actionCategory": "document_action",
  "description": "Test audit log entry",
  "performedBy": "test-user-uuid",
  "metadata": {
    "progress": 50,
    "comment": "Test comment",
    "fileCount": 2
  }
}
```

**Expected Response:**
- Status: `201 Created`
- Body:
```json
{
  "id": "generated-uuid-string"
}
```

### Test 2: Verify Audit Log in Database

```sql
SELECT
  id, ticket_id, step_id, action,
  action_category, description, metadata,
  performed_by, performed_at
FROM audit_logs
WHERE action = 'PROGRESS_DOCUMENTS_UPLOADED'
ORDER BY performed_at DESC
LIMIT 1;
```

**Expected:**
- New row with correct data
- metadata column contains JSON string
- performed_at timestamp is set

### Test 3: Progress Document Upload Flow

**Steps:**
1. Open a ticket in the UI
2. Navigate to workflow step
3. Update progress (e.g., 50%)
4. Add a comment (optional)
5. Upload progress documents
6. Click Save/Update

**Expected:**
- No JavaScript console errors
- Audit log is created
- Progress documents are uploaded
- Audit log ID links documents
- Documents appear in progress history

### Test 4: Validation Tests

**Test Missing Required Fields:**

```bash
# Missing ticketId
curl -X POST http://localhost:8080/ticket-tracker-java/api/audit-logs \
  -H "Content-Type: application/json" \
  -d '{
    "action": "TEST",
    "actionCategory": "test_action",
    "description": "Test",
    "performedBy": "uuid"
  }'
```

**Expected:**
- Status: `400 Bad Request`
- Body: `{"status": 400, "message": "ticketId is required"}`

**Test Unauthenticated Request:**

```bash
# Without session cookie
curl -X POST http://localhost:8080/ticket-tracker-java/api/audit-logs \
  -H "Content-Type: application/json" \
  -d '{
    "ticketId": "uuid",
    "action": "TEST",
    "actionCategory": "test_action",
    "description": "Test",
    "performedBy": "uuid"
  }'
```

**Expected:**
- Status: `401 Unauthorized`
- Body: `{"status": 401, "message": "Not authenticated"}`

### Test 5: Metadata Conversion

**Request with Complex Metadata:**

```json
{
  "ticketId": "ticket-uuid",
  "stepId": "step-uuid",
  "action": "TEST_ACTION",
  "actionCategory": "test_action",
  "description": "Test with metadata",
  "performedBy": "user-uuid",
  "metadata": {
    "string": "value",
    "number": 123,
    "boolean": true,
    "array": [1, 2, 3],
    "nested": {
      "key": "value"
    }
  }
}
```

**Expected:**
- Status: `201 Created`
- Metadata stored as JSON string in database
- All data types preserved

### Test 6: Retrieve Created Audit Log

**Endpoint:** `GET /api/audit-logs?ticketId={ticketId}`

**Expected:**
- Status: `200 OK`
- Response includes newly created audit log
- All fields match creation request
- metadata is properly formatted

### Test 7: End-to-End Progress History

1. Create ticket
2. Add workflow step
3. Update step progress with documents
4. View audit trail
5. Verify progress documents appear with correct audit log entry

**Expected:**
- Audit trail shows progress update
- Documents are linked to audit log
- Comment is visible
- Timestamp is correct
- User name is displayed

## Common Issues

### Issue: 401 Unauthorized
**Cause:** Session not established or expired
**Fix:** Login first, ensure cookies are sent with request

### Issue: 400 Bad Request (missing field)
**Cause:** Required field not provided
**Fix:** Verify all required fields are present

### Issue: 500 Internal Server Error
**Cause:** Database connection issue or DAO error
**Check:**
- Database is running
- Connection pool is configured
- Check Tomcat logs for SQLException

### Issue: Audit log created but no ID returned
**Cause:** DAO.create() not returning created object
**Fix:** Verify AuditLogDAO.create() returns the created audit log

## Browser Console Tests

Open browser console (F12) and run:

```javascript
// Test audit log creation
fetch('/api/audit-logs', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    ticketId: 'existing-ticket-uuid',
    stepId: 'existing-step-uuid',
    action: 'TEST_ACTION',
    actionCategory: 'test_action',
    description: 'Browser test',
    performedBy: 'current-user-uuid',
    metadata: {
      test: true,
      timestamp: new Date().toISOString()
    }
  })
})
.then(res => res.json())
.then(data => console.log('Success:', data))
.catch(error => console.error('Error:', error));
```

**Expected Console Output:**
```
Success: {id: "generated-uuid"}
```

## Verification Checklist

- [ ] POST /api/audit-logs returns 201 with ID
- [ ] Audit log saved to database
- [ ] Metadata converted to JSON string
- [ ] Required field validation works
- [ ] Authentication check works
- [ ] Progress document upload completes
- [ ] Documents linked to audit log
- [ ] Audit trail displays correctly
- [ ] No JavaScript console errors
- [ ] No Java exceptions in Tomcat logs

## Success Criteria

✅ All validation tests pass
✅ Audit logs created successfully
✅ Progress documents upload and link correctly
✅ Audit trail displays complete information
✅ No errors in browser console
✅ No errors in Tomcat logs

---

**Fix Status:** Ready for Testing
**Build Required:** Yes - rebuild and redeploy WAR file
**Database Changes:** No schema changes required
