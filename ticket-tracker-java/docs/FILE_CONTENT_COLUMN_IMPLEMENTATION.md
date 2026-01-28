# FILE_CONTENT Column Implementation Summary

## Overview
This document describes the implementation of the `file_content` BLOB column in the `documents` table to store file binary content directly in the Oracle database.

## Changes Made

### 1. Database Migration

**File:** `database/11-oracle-add-documents-file-content.sql`

- Added `file_content BLOB` column to the `documents` table
- Added column comment explaining the purpose and 5MB limit
- Created performance index `idx_documents_has_content` for checking content existence
- Migration includes verification output

**Key Features:**
- Column is nullable (NULL for existing records, no backfill required)
- BLOB type can store binary file content up to 5MB
- Index optimizes queries checking for file content presence

### 2. Installation Script Update

**File:** `database/install.sql`

Added new migration step (Step 8) that executes:
- Migration 10: Add file_content to workflow_step_progress_documents
- Migration 11: Add file_content to documents table

Added verification queries to confirm:
- Documents table has file_content column
- Progress documents table has file_content column

### 3. File Size Limit Update

Changed maximum file size from 50MB to 5MB across all components:

**Files Updated:**

1. **FileServlet.java** (line 31-33)
   - `maxFileSize = 5242880` (5 MB)
   - `maxRequestSize = 10485760` (10 MB)

2. **DocumentService.java** (line 17, 171-173)
   - Added `MAX_FILE_SIZE = 5242880L` constant
   - Added validation: "Document size exceeds maximum allowed size of 5 MB"

3. **WorkflowStepProgressDocumentService.java** (line 16, 44)
   - Updated `MAX_FILE_SIZE = 5242880L` (was 52428800L)
   - Updated error message: "File size exceeds maximum allowed size of 5 MB"

## File Size Validation Layers

The implementation uses multiple validation layers for defense in depth:

1. **Servlet Level** (@MultipartConfig)
   - Rejects uploads > 5MB before processing
   - Returns HTTP 413 (Request Entity Too Large)

2. **Service Level** (DocumentService & WorkflowStepProgressDocumentService)
   - Validates file size in business logic
   - Provides clear error messages
   - Ensures consistency across all upload paths

## Database Schema

### Documents Table (Updated)

```sql
CREATE TABLE documents (
  id RAW(16) PRIMARY KEY,
  ticket_id RAW(16),
  step_id RAW(16),
  name VARCHAR2(1000) NOT NULL,
  type VARCHAR2(200) NOT NULL,
  size NUMBER(20) NOT NULL,
  url VARCHAR2(2000),
  storage_path VARCHAR2(2000) NOT NULL,
  uploaded_by RAW(16) NOT NULL,
  uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  is_mandatory NUMBER(1) DEFAULT 0,
  is_completion_certificate NUMBER(1) DEFAULT 0,
  file_content BLOB  -- NEW COLUMN
);
```

### Index Created

```sql
CREATE INDEX idx_documents_has_content ON documents(
  CASE WHEN file_content IS NOT NULL THEN 1 ELSE 0 END
);
```

This index improves performance for queries that filter documents by whether they have content stored.

## Usage Examples

### Storing File Content

```java
Document document = new Document();
document.setName("example.pdf");
document.setType("application/pdf");
document.setSize(fileData.length);
document.setStoragePath("blob_" + System.currentTimeMillis());
document.setFileContent(fileData);  // Store binary content
document.setUploadedBy(userId);

documentService.createDocument(document, userId);
```

### Retrieving File Content

```java
// Get document with content
Document doc = documentService.getDocumentById(documentId);
byte[] fileData = doc.getFileContent();

// Download via servlet
GET /api/files/{id}?download=true
```

## Migration Steps

To apply this migration to an existing database:

1. **Connect to Oracle as ticket_tracker user:**
   ```bash
   sqlplus ticket_tracker/ticket_pass_2024@ORCL
   ```

2. **Run the migration:**
   ```sql
   @11-oracle-add-documents-file-content.sql
   ```

3. **Verify the column was created:**
   ```sql
   SELECT column_name, data_type, nullable
   FROM user_tab_columns
   WHERE table_name = 'DOCUMENTS' AND column_name = 'FILE_CONTENT';
   ```

4. **Verify the index was created:**
   ```sql
   SELECT index_name, table_name, uniqueness
   FROM user_indexes
   WHERE index_name = 'IDX_DOCUMENTS_HAS_CONTENT';
   ```

## Backward Compatibility

- Existing records will have NULL for file_content
- No data migration/backfill required
- Application handles NULL gracefully
- Legacy URL-based file storage still supported

## Testing Checklist

After applying this migration:

- [ ] Upload a document < 5MB (should succeed)
- [ ] Upload a document > 5MB (should fail with clear error)
- [ ] Upload a document exactly 5MB (should succeed)
- [ ] Retrieve document with content
- [ ] Retrieve document without content (NULL)
- [ ] Download document via API
- [ ] Complete workflow step with document upload
- [ ] Verify document appears in audit trail
- [ ] Check database storage size
- [ ] Verify index is being used in query plans

## Performance Considerations

1. **Storage:**
   - 5MB max per file
   - BLOB storage inline with row
   - Consider storage growth over time

2. **Query Performance:**
   - Index optimizes content existence checks
   - Large BLOBs can slow down full table scans
   - Consider separate content retrieval queries

3. **Memory:**
   - 5MB limit prevents excessive memory usage
   - Files loaded into memory during upload/download
   - Connection pool may need adjustment

## Future Enhancements

1. Consider adding file compression
2. Implement chunked upload for larger files
3. Add file type validation
4. Implement virus scanning integration
5. Consider external storage for files > 5MB

## Error Resolution

### ORA-00904: FILE_CONTENT: invalid identifier

This error occurs if the migration hasn't been applied. Solution:
1. Run migration 11: `@11-oracle-add-documents-file-content.sql`
2. Verify column exists: `DESC documents`

### ORA-01400: cannot insert NULL into FILE_CONTENT

This should not occur as file_content is nullable. If it does:
1. Check for table-level constraints
2. Verify migration was applied correctly

### File size validation failing

If uploads are rejected incorrectly:
1. Check FileServlet.java @MultipartConfig
2. Verify MAX_FILE_SIZE constants
3. Check for conversion errors (bytes vs MB)

## Implementation Date

- **Migration Created:** 2025-01-23
- **File Size Updated:** 2025-01-23
- **Status:** Implemented and Ready for Testing

## Related Files

- `database/11-oracle-add-documents-file-content.sql`
- `database/install.sql`
- `src/main/java/com/tickettracker/servlet/FileServlet.java`
- `src/main/java/com/tickettracker/service/DocumentService.java`
- `src/main/java/com/tickettracker/service/WorkflowStepProgressDocumentService.java`
- `src/main/java/com/tickettracker/dao/DocumentDAO.java`
- `src/main/java/com/tickettracker/model/Document.java`
