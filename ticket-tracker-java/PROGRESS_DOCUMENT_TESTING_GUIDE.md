# Progress Document Upload Testing Guide

## Prerequisites

- Backend server running (ticket-tracker-java)
- Frontend application accessible
- Test user with assigned workflow steps
- Oracle database with updated schema (migration 10-oracle-add-progress-doc-file-content.sql applied)

## Test Scenarios

### Scenario 1: Upload Progress Document

**Steps:**
1. Log in as an assigned user
2. Navigate to a ticket with workflow steps
3. Click on a workflow step that is assigned to you
4. Click "Update Workflow" button
5. In the modal, update the progress percentage (e.g., 50%)
6. Add a comment (e.g., "Work in progress")
7. Click "Attach Document" or "Upload Progress Document"
8. Select a file from your computer (PDF, image, or any document)
9. Click "Save" or "Submit"

**Expected Results:**
- Upload should succeed without errors
- Modal should close
- Progress Documents section should show the uploaded file
- File should display:
  - Correct filename
  - File size
  - Upload timestamp
  - Uploader name (your name)

### Scenario 2: View Progress Documents

**Steps:**
1. Navigate to a ticket with uploaded progress documents
2. Scroll to the "Progress Documents" section
3. Verify all uploaded documents are visible

**Expected Results:**
- All non-deleted progress documents are listed
- Each document shows:
  - File icon appropriate for file type
  - Filename
  - File size (formatted: KB, MB)
  - Upload date and time
  - Uploader name
- Documents are sorted by upload date (newest first)

### Scenario 3: Download Progress Document

**Steps:**
1. In the Progress Documents section, locate a document
2. Click the download icon (download button)
3. Wait for download to complete

**Expected Results:**
- File downloads successfully
- Downloaded file has the correct filename
- File opens correctly in appropriate application
- File content matches the original uploaded file

### Scenario 4: Delete Progress Document

**Steps:**
1. As the uploader, EO, or DO, locate a progress document
2. Click the delete icon (trash button)
3. Enter a reason for deletion (minimum 5 characters)
4. Click "Confirm Delete"

**Expected Results:**
- Deletion succeeds
- Document is removed from the list
- Audit log entry is created for the deletion
- Document record is soft-deleted (is_deleted=1) in database

### Scenario 5: Progress History View

**Steps:**
1. Navigate to a workflow step details page
2. Scroll to "Progress History & Updates" section
3. Locate an update entry with uploaded documents
4. Verify documents appear under the update

**Expected Results:**
- Progress documents appear grouped with their audit log entry
- Multiple documents for the same update are all visible
- Each document can be downloaded independently
- Upload timestamp matches the update timestamp

### Scenario 6: Multiple Users Upload to Same Step

**Steps:**
1. User A uploads a progress document to a workflow step
2. User B (EO or DO) uploads another progress document to the same step
3. View the Progress Documents section

**Expected Results:**
- Both documents are visible
- Each document shows the correct uploader name
- Documents are sorted by upload time
- Each user can download both documents

### Scenario 7: Upload Large File

**Steps:**
1. Attempt to upload a file larger than 5 MB
2. Submit the update

**Expected Results:**
- Upload should fail with validation error
- Error message: "File size exceeds maximum allowed size of 5 MB"
- No partial data is saved to the database

### Scenario 8: Upload Without Required Parameters

**Test 8a: Missing stepId**
- Attempt to upload without stepId parameter
- Expected: Error "stepId is required for progress document upload"

**Test 8b: Missing ticketId**
- Attempt to upload without ticketId parameter
- Expected: Error "ticketId is required for progress document upload"

**Test 8c: Missing file**
- Attempt to upload without selecting a file
- Expected: Error "No file provided"

## Database Verification

After uploading a progress document, verify the database:

```sql
-- Check progress document was saved
SELECT
    RAWTOHEX(id) as id,
    RAWTOHEX(step_id) as step_id,
    RAWTOHEX(ticket_id) as ticket_id,
    RAWTOHEX(audit_log_id) as audit_log_id,
    file_name,
    file_size,
    file_type,
    uploaded_at,
    is_deleted
FROM workflow_step_progress_documents
ORDER BY uploaded_at DESC;

-- Check file content exists
SELECT
    RAWTOHEX(id) as id,
    file_name,
    DBMS_LOB.GETLENGTH(file_content) as content_size
FROM workflow_step_progress_documents
WHERE file_content IS NOT NULL
ORDER BY uploaded_at DESC;
```

**Expected:**
- Record exists in `workflow_step_progress_documents` table
- `file_content` BLOB is not null
- `content_size` matches `file_size`
- `audit_log_id` links to a valid audit log entry

## API Endpoint Testing

### Upload Endpoint Test

```bash
# Test progress document upload
curl -X POST http://localhost:8080/ticket-tracker-java/api/files/progress-docs \
  -H "Cookie: JSESSIONID=<session-id>" \
  -F "file=@test-document.pdf" \
  -F "stepId=<step-id-hex>" \
  -F "ticketId=<ticket-id-hex>" \
  -F "auditLogId=<audit-log-id-hex>"
```

**Expected Response:**
```json
{
  "id": "<document-id-hex>",
  "stepId": "<step-id-hex>",
  "ticketId": "<ticket-id-hex>",
  "auditLogId": "<audit-log-id-hex>",
  "fileName": "test-document.pdf",
  "fileSize": 12345,
  "fileType": "application/pdf",
  "uploadedBy": "<user-id-hex>",
  "uploadedAt": "2025-01-23T10:30:00Z",
  "isDeleted": false
}
```

### Download Endpoint Test

```bash
# Test progress document download
curl -X GET "http://localhost:8080/ticket-tracker-java/api/files/<document-id>?download=true" \
  -H "Cookie: JSESSIONID=<session-id>" \
  -o downloaded-file.pdf
```

**Expected:**
- File downloads successfully
- Content-Type header matches original file type
- Content-Disposition header includes original filename
- File content is identical to uploaded file

### List Endpoint Test

```bash
# Test list progress documents for a step
curl -X GET "http://localhost:8080/ticket-tracker-java/api/workflow-steps/<step-id>/progress-documents" \
  -H "Cookie: JSESSIONID=<session-id>"
```

**Expected Response:**
```json
[
  {
    "id": "<document-id-hex>",
    "stepId": "<step-id-hex>",
    "ticketId": "<ticket-id-hex>",
    "auditLogId": "<audit-log-id-hex>",
    "fileName": "test-document.pdf",
    "fileSize": 12345,
    "fileType": "application/pdf",
    "uploadedBy": "<user-id-hex>",
    "uploadedAt": "2025-01-23T10:30:00Z",
    "isDeleted": false
  }
]
```

## Troubleshooting

### Issue: Upload fails with "Authentication required"
- **Cause:** User session expired or not authenticated
- **Solution:** Log in again and retry

### Issue: Upload fails with "File size exceeds maximum allowed size"
- **Cause:** File is larger than 5 MB
- **Solution:** Reduce file size or split into multiple files

### Issue: Documents don't appear after upload
- **Cause:** Wrong table being queried or RLS policy blocking access
- **Solution:** Check database logs, verify `workflow_step_progress_documents` table has records

### Issue: Download returns 404
- **Cause:** Document ID not found in either table
- **Solution:** Verify document ID is correct and exists in database

### Issue: File content is corrupted on download
- **Cause:** BLOB data not read correctly or encoding issue
- **Solution:** Verify `file_content` column in database is not null and has correct size

## Success Criteria

All tests pass with:
- ✓ Upload succeeds and document appears immediately
- ✓ Download produces correct file with original content
- ✓ Multiple documents can be uploaded for same step
- ✓ Documents appear in progress history grouped by update
- ✓ Deletion works and removes document from list
- ✓ Database records are correct with file content stored
- ✓ API endpoints return expected responses
- ✓ Error handling works for invalid inputs
