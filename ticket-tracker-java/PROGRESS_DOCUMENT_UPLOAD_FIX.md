# Progress Document Upload and Display Fix

## Issue Summary

Documents uploaded during progress updates by assigned users were not being displayed in the application. This was because the backend was saving them to the wrong database table.

## Root Cause

When an assigned user uploaded a progress document via the "Update Workflow" modal:

1. The frontend called `POST /files/progress-docs` with parameters including `stepId`, `ticketId`, and `auditLogId`
2. The `FileServlet.java` treated all POST requests the same way, regardless of the path
3. Documents were saved to the `documents` table instead of the `workflow_step_progress_documents` table
4. When retrieving progress documents, the system queried `workflow_step_progress_documents` which was empty
5. Result: No documents appeared even though they were uploaded

## Changes Made

### 1. Enhanced ProgressDocument Model (WorkflowStepProgressDocumentDAO.java)

**File:** `ticket-tracker-java/src/main/java/com/tickettracker/dao/WorkflowStepProgressDocumentDAO.java`

- Added `fileContent` field to store binary file data in the database
- Updated `create()` method to insert `file_content` BLOB column
- Updated `mapResultSetToProgressDocument()` to retrieve `file_content` from the database

### 2. Updated Progress Document Service (WorkflowStepProgressDocumentService.java)

**File:** `ticket-tracker-java/src/main/java/com/tickettracker/service/WorkflowStepProgressDocumentService.java`

- Relaxed `filePath` validation to auto-generate a path if not provided
- Maintained all other validations (stepId, ticketId, fileName, fileSize, uploadedBy)

### 3. Enhanced FileServlet with Progress Document Upload Support (FileServlet.java)

**File:** `ticket-tracker-java/src/main/java/com/tickettracker/servlet/FileServlet.java`

#### Changes:

1. **Added Imports:**
   - `WorkflowStepProgressDocumentService`
   - `ProgressDocument` from `WorkflowStepProgressDocumentDAO`

2. **Initialized Progress Document Service:**
   - Added `progressDocumentService` field
   - Initialized in `init()` method

3. **Enhanced doPost() Method with Path Routing:**
   - Routes `/progress-docs` requests to `handleProgressDocumentUpload()`
   - Routes `/upload` or null/empty path to existing `handleFileUpload()`
   - Validates that progress document uploads use multipart form data

4. **Added handleProgressDocumentUpload() Method:**
   - Extracts file, stepId, ticketId, and auditLogId from multipart request
   - Validates required parameters (stepId and ticketId are mandatory)
   - Reads file content into byte array
   - Creates `ProgressDocument` object with all required fields
   - Calls `progressDocumentService.uploadProgressDocument()` to save to database
   - Returns created progress document ID to frontend
   - Strips file content from response to reduce payload size

5. **Enhanced handleGetDocument() Method:**
   - First attempts to retrieve document from regular `documents` table
   - If not found, falls back to checking `workflow_step_progress_documents` table
   - Supports download for both document types
   - Maintains backward compatibility with existing document downloads

## Database Schema

The `workflow_step_progress_documents` table already has the required `file_content` BLOB column from migration `10-oracle-add-progress-doc-file-content.sql`.

## API Endpoints

### Upload Progress Document
- **Endpoint:** `POST /files/progress-docs`
- **Content-Type:** `multipart/form-data`
- **Parameters:**
  - `file`: The file to upload (required)
  - `stepId`: Workflow step ID (required)
  - `ticketId`: Ticket ID (required)
  - `auditLogId`: Audit log ID (optional)
  - `userId`: User ID (from session)
- **Response:** Created progress document metadata (without file content)

### Download Progress Document
- **Endpoint:** `GET /files/{id}?download=true`
- **Description:** Downloads either a regular document or progress document based on ID
- **Response:** Binary file content with appropriate headers

### List Progress Documents
- **Endpoint:** `GET /workflow-steps/{stepId}/progress-documents`
- **Description:** Lists all non-deleted progress documents for a workflow step
- **Response:** Array of progress document metadata

## Testing Checklist

1. **Upload Progress Document:**
   - ✓ Assigned user can upload progress document during workflow step update
   - ✓ Document appears immediately in "Progress Documents" section
   - ✓ Document is linked to the correct audit log entry

2. **Display Progress Documents:**
   - ✓ Documents show correct filename, size, and upload date
   - ✓ Uploader name is displayed correctly
   - ✓ Documents appear in chronological order

3. **Download Progress Documents:**
   - ✓ Users can download progress documents
   - ✓ Downloaded file has correct filename and content
   - ✓ Download works for both regular documents and progress documents

4. **Delete Progress Documents:**
   - ✓ EO and DO can delete any progress document
   - ✓ Assigned user can delete their own progress documents
   - ✓ Deleted documents are soft-deleted (is_deleted=1)
   - ✓ Deleted documents do not appear in the list

5. **Progress History View:**
   - ✓ Progress documents appear grouped with their audit log entry
   - ✓ Multiple documents can be uploaded for a single update
   - ✓ Documents from different users are all visible

## Benefits

1. **Correct Data Storage:** Progress documents are now stored in the correct table
2. **Proper Association:** Documents are linked to audit log entries for traceability
3. **Immediate Visibility:** Users see their uploaded documents immediately after saving
4. **Backward Compatibility:** Regular document upload and download still works
5. **Unified Download:** Single endpoint handles both document types transparently

## Frontend Compatibility

The frontend code remains unchanged because:
- Upload endpoint `/files/progress-docs` is now properly handled
- Download endpoint `/files/{id}?download=true` works for both document types
- List endpoint returns the expected progress document structure

## Future Enhancements

1. Consider adding file size validation on the frontend before upload
2. Add progress indicators for large file uploads
3. Implement file preview for common document types
4. Add batch upload support for multiple documents
