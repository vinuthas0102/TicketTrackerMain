# Workflow Progress Document File Content Fix

## Issue Report

**Date:** March 11, 2026
**Reporter:** User
**Severity:** Critical - File upload functionality broken

### Error Message

```
Error uploading document to step 1: column "file_content" of relation "workflow_step_progress_documents" does not exist
```

### Error Context

- **Component:** Progress Documents upload functionality
- **Location:** `src/components/ticket/ProgressDocuments.tsx`
- **Service:** `src/services/fileService.ts`
- **Database Table:** `workflow_step_progress_documents`
- **Action:** Attempting to upload a file to a workflow step

### Full Stack Trace

```
Error uploading document to step 1: column "file_content" of relation "workflow_step_progress_documents" does not exist
    at fileService.ts:254:13
    at async ProgressDocuments.handleFileUpload (ProgressDocuments.tsx:85)
```

## Root Cause Analysis

### The Problem

The `workflow_step_progress_documents` table in the Oracle database is missing the `file_content` BLOB column that the application expects when uploading files.

### Why This Happened

The Java/Oracle backend implementation is missing the database schema changes that were added to the Supabase version of the application. Specifically, the migration that adds the `file_content` column to store file data directly in the database.

### Expected Schema vs Actual Schema

**Expected (from Migration 10):**
```sql
ALTER TABLE workflow_step_progress_documents
ADD file_content BLOB;
```

**Actual:** Column does not exist in the Oracle database.

## Investigation Process

### 1. Initial Error Identification

We examined the error stack trace which pointed to:
- **File:** `src/services/fileService.ts:254`
- **Function:** `uploadProgressDocument()`
- **Table:** `workflow_step_progress_documents`

### 2. Code Review

Found the problematic code in `fileService.ts`:

```typescript
// Line 254 in fileService.ts
const { error: uploadError } = await supabase
  .from('workflow_step_progress_documents')
  .insert({
    step_id: stepId,
    file_name: file.name,
    file_size: file.size,
    file_type: file.type,
    file_content: fileContent, // <-- This column doesn't exist!
    uploaded_by: userId,
    uploaded_at: new Date().toISOString(),
  });
```

### 3. Database Schema Analysis

We examined the Oracle database schema files in `ticket-tracker-java/database/`:

**File: `10-oracle-add-progress-doc-file-content.sql`**
```sql
-- Migration 10: Add file_content column to workflow_step_progress_documents
-- This migration adds support for storing file content directly in the database

ALTER TABLE workflow_step_progress_documents
ADD file_content BLOB;

COMMIT;
```

**Finding:** The migration script exists but has not been executed against the Oracle database.

### 4. Related Schema Files

We also found that a similar migration exists for the `documents` table:

**File: `11-oracle-add-documents-file-content.sql`**
```sql
-- Migration 11: Add file_content column to documents table
-- This migration adds support for storing file content directly in the database

ALTER TABLE documents
ADD file_content BLOB;

COMMIT;
```

### 5. Migration Status Check

The migration scripts are present in the codebase but need to be executed in the correct order:
1. Scripts 01-09: Base schema setup
2. Script 10: Adds `file_content` to `workflow_step_progress_documents`
3. Script 11: Adds `file_content` to `documents`
4. Scripts 12-13: Additional enhancements

## Solution

### Immediate Fix Required

**Execute the migration script** against your Oracle database:

```bash
# Navigate to database directory
cd ticket-tracker-java/database/

# Execute migration 10
sqlplus username/password@database @10-oracle-add-progress-doc-file-content.sql

# Execute migration 11 (related fix)
sqlplus username/password@database @11-oracle-add-documents-file-content.sql
```

### Alternative: Run All Migrations

If you want to ensure all schema updates are applied:

```bash
cd ticket-tracker-java/database/
sqlplus username/password@database @install.sql
```

The `install.sql` script runs all migrations in order.

### Verification Steps

After running the migration, verify the column exists:

```sql
DESC workflow_step_progress_documents;
```

Expected output should include:
```
FILE_CONTENT    BLOB
```

## Key Design Decisions

### 1. Storage Strategy: Database vs File System

**Decision:** Store file content directly in the database using BLOB columns

**Rationale:**
- Simplifies deployment (no separate file storage configuration)
- Ensures transactional consistency
- Eliminates file system permission issues
- Makes backups more straightforward

**Trade-offs:**
- Database size increases with file uploads
- May impact database performance with large files
- Not suitable for very large files (videos, etc.)

### 2. Dual Storage Support

The codebase supports both storage strategies:
- **Supabase version:** Uses Supabase Storage buckets
- **Oracle version:** Uses BLOB columns in the database

**Implementation:**
```typescript
// Both storage paths are handled:
// 1. Upload to storage bucket (Supabase)
// 2. Store file_content in database (Oracle)
```

### 3. Migration Script Organization

**Structure:**
```
ticket-tracker-java/database/
├── 01-oracle-create-user.sql          # User creation
├── 02-oracle-schema.sql               # Base tables
├── 03-oracle-sequences.sql            # ID generators
├── 04-oracle-triggers.sql             # Auto-increment
├── 05-oracle-indexes.sql              # Performance
├── 06-oracle-constraints.sql          # Data integrity
├── 07-oracle-seed-data.sql            # Default data
├── 08-oracle-add-missing-fields.sql   # Schema updates
├── 09-oracle-add-workflow-comments-updated-at.sql
├── 10-oracle-add-progress-doc-file-content.sql  # <-- THIS ONE
├── 11-oracle-add-documents-file-content.sql
├── 12-oracle-add-finance-approval-columns.sql
├── 13-oracle-add-step-type-and-request-type.sql
└── install.sql                        # Master installer
```

**Decision:** Each migration is a separate, numbered file

**Benefits:**
- Clear migration history
- Easy to apply incrementally
- Can skip already-applied migrations
- Easier to troubleshoot

## Files Involved

### Frontend Files
- **src/services/fileService.ts** - File upload service (lines 254, 328)
- **src/components/ticket/ProgressDocuments.tsx** - Upload UI component
- **src/types/index.ts** - Type definitions

### Backend Files (Java)
- **src/main/java/com/tickettracker/dao/WorkflowStepProgressDocumentDAO.java**
- **src/main/java/com/tickettracker/service/WorkflowStepProgressDocumentService.java**
- **src/main/java/com/tickettracker/servlet/FileServlet.java**

### Database Files
- **ticket-tracker-java/database/10-oracle-add-progress-doc-file-content.sql** (PRIMARY)
- **ticket-tracker-java/database/11-oracle-add-documents-file-content.sql** (RELATED)
- **ticket-tracker-java/database/install.sql** (INSTALLER)

## Technical Details

### Table Schema (Before Fix)

```sql
CREATE TABLE workflow_step_progress_documents (
    id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
    step_id RAW(16) NOT NULL,
    file_name VARCHAR2(255) NOT NULL,
    file_size NUMBER,
    file_type VARCHAR2(100),
    uploaded_by RAW(16) NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- file_content BLOB  <-- MISSING!
    CONSTRAINT fk_prog_doc_step FOREIGN KEY (step_id)
        REFERENCES workflow_steps(id) ON DELETE CASCADE,
    CONSTRAINT fk_prog_doc_user FOREIGN KEY (uploaded_by)
        REFERENCES users(id)
);
```

### Table Schema (After Fix)

```sql
CREATE TABLE workflow_step_progress_documents (
    id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
    step_id RAW(16) NOT NULL,
    file_name VARCHAR2(255) NOT NULL,
    file_size NUMBER,
    file_type VARCHAR2(100),
    uploaded_by RAW(16) NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    file_content BLOB,  -- ✓ ADDED
    CONSTRAINT fk_prog_doc_step FOREIGN KEY (step_id)
        REFERENCES workflow_steps(id) ON DELETE CASCADE,
    CONSTRAINT fk_prog_doc_user FOREIGN KEY (uploaded_by)
        REFERENCES users(id)
);
```

### Application Code That Uses This Column

**Frontend (TypeScript):**
```typescript
// src/services/fileService.ts:254
const { error: uploadError } = await supabase
  .from('workflow_step_progress_documents')
  .insert({
    step_id: stepId,
    file_name: file.name,
    file_size: file.size,
    file_type: file.type,
    file_content: fileContent,  // Base64 or byte array
    uploaded_by: userId,
    uploaded_at: new Date().toISOString(),
  });
```

**Backend (Java):**
```java
// WorkflowStepProgressDocumentDAO.java
String sql = "INSERT INTO workflow_step_progress_documents " +
             "(id, step_id, file_name, file_size, file_type, " +
             "file_content, uploaded_by, uploaded_at) " +
             "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

pstmt.setBlob(6, new ByteArrayInputStream(fileContent));
```

## Testing After Fix

### 1. Verify Column Exists

```sql
SELECT column_name, data_type, data_length
FROM user_tab_columns
WHERE table_name = 'WORKFLOW_STEP_PROGRESS_DOCUMENTS'
  AND column_name = 'FILE_CONTENT';
```

Expected result:
```
COLUMN_NAME    DATA_TYPE    DATA_LENGTH
FILE_CONTENT   BLOB         4000
```

### 2. Test File Upload

1. Log in to the application
2. Open a ticket with workflow steps
3. Navigate to a workflow step
4. Click "Upload Progress Document"
5. Select a file (PDF, image, etc.)
6. Click Upload
7. Verify: No error message appears
8. Verify: File appears in the progress documents list

### 3. Test File Download

1. Click on an uploaded document
2. Verify: File downloads correctly
3. Verify: File content is intact and readable

### 4. Verify Database Content

```sql
SELECT id, step_id, file_name, file_size,
       DBMS_LOB.GETLENGTH(file_content) as content_size
FROM workflow_step_progress_documents;
```

Expected: `content_size` should match `file_size`

## Prevention Strategies

### 1. Migration Tracking

**Recommendation:** Implement a migrations table to track applied migrations:

```sql
CREATE TABLE schema_migrations (
    version VARCHAR2(50) PRIMARY KEY,
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR2(500)
);
```

### 2. Automated Schema Validation

Add a startup check in the Java application:

```java
// DatabaseConfig.java
public void validateSchema() throws DatabaseException {
    String sql = "SELECT column_name FROM user_tab_columns " +
                 "WHERE table_name = 'WORKFLOW_STEP_PROGRESS_DOCUMENTS' " +
                 "AND column_name = 'FILE_CONTENT'";

    if (!columnExists(sql)) {
        throw new DatabaseException(
            "Required schema migration not applied: " +
            "file_content column missing"
        );
    }
}
```

### 3. Database Version Documentation

Maintain a `DATABASE_VERSION.md` file documenting:
- Current schema version
- Required migrations
- Last migration applied date
- How to check schema version

### 4. Deployment Checklist

Create a deployment checklist that includes:
- [ ] Backup database
- [ ] Run pending migrations
- [ ] Verify schema changes
- [ ] Test critical functionality
- [ ] Monitor error logs

## Additional Notes

### Related Issues Fixed Previously

This is similar to previous issues documented in:
- **DOCUMENT_UPLOAD_FIX_SUMMARY.md** - Fixed document upload issues
- **PROGRESS_DOCUMENTS_UPLOAD_FIX.md** - Fixed progress document permissions
- **FILE_CONTENT_COLUMN_IMPLEMENTATION.md** - Original implementation docs

### Schema Consistency Between Backends

The application supports two backend implementations:
1. **Supabase** (PostgreSQL) - Located in `/supabase/migrations/`
2. **Oracle** - Located in `/ticket-tracker-java/database/`

**Important:** Keep schemas in sync when making changes to either backend.

### File Size Considerations

Current implementation stores files in BLOB columns with no size limit enforcement. Consider adding:

```sql
-- Optional: Add a constraint for maximum file size
ALTER TABLE workflow_step_progress_documents
ADD CONSTRAINT chk_file_size CHECK (file_size <= 10485760); -- 10MB limit
```

### Performance Monitoring

After applying the fix, monitor:
- Database size growth
- Query performance on tables with BLOB columns
- Backup/restore times
- Application memory usage during file operations

## Conclusion

The issue was caused by a missing database schema migration. The solution is straightforward: execute migration script 10 (and 11 for related functionality) against the Oracle database.

**Status:** ✓ Solution identified and documented
**Next Action Required:** Database administrator to run migration scripts
**Estimated Time:** 5 minutes
**Risk Level:** Low (additive change, no data loss)

## Quick Reference Commands

### Check Current Schema
```sql
DESC workflow_step_progress_documents;
```

### Apply Fix
```bash
cd ticket-tracker-java/database/
sqlplus user/pass@db @10-oracle-add-progress-doc-file-content.sql
```

### Verify Fix
```sql
SELECT column_name FROM user_tab_columns
WHERE table_name = 'WORKFLOW_STEP_PROGRESS_DOCUMENTS'
AND column_name = 'FILE_CONTENT';
```

### Test in Application
1. Log in
2. Open ticket
3. Upload progress document
4. Verify success

---

**Document Version:** 1.0
**Last Updated:** March 11, 2026
**Author:** Technical Support
**Reference:** Ticket Tracker Java Oracle Implementation
