# Quick Start: FILE_CONTENT Column Migration

## What Was Implemented

Added `file_content` BLOB column to the `documents` table to store file binary data directly in Oracle database, with a maximum file size limit of 5MB.

## Quick Apply (For New Installations)

If setting up a fresh database, simply run:

```bash
sqlplus ticket_tracker/ticket_pass_2024@ORCL @database/install.sql
```

The install script now includes migration 11 automatically.

## Quick Apply (For Existing Databases)

If you already have the database set up, run only the new migration:

```bash
sqlplus ticket_tracker/ticket_pass_2024@ORCL @database/11-oracle-add-documents-file-content.sql
```

## Verification

Check if the column was added successfully:

```sql
-- Check column exists
SELECT column_name, data_type, nullable
FROM user_tab_columns
WHERE table_name = 'DOCUMENTS' AND column_name = 'FILE_CONTENT';

-- Expected output:
-- COLUMN_NAME   DATA_TYPE   NULLABLE
-- FILE_CONTENT  BLOB        Y

-- Check index exists
SELECT index_name FROM user_indexes WHERE index_name = 'IDX_DOCUMENTS_HAS_CONTENT';

-- Expected output:
-- INDEX_NAME
-- IDX_DOCUMENTS_HAS_CONTENT
```

## What Changed

1. **Database:**
   - ✅ New BLOB column: `documents.file_content`
   - ✅ New index: `idx_documents_has_content`
   - ✅ Updated install.sql to include migration 11

2. **Application:**
   - ✅ File size limit: 50MB → 5MB
   - ✅ FileServlet.java: Updated @MultipartConfig
   - ✅ DocumentService.java: Added MAX_FILE_SIZE validation
   - ✅ WorkflowStepProgressDocumentService.java: Updated MAX_FILE_SIZE

3. **Validation:**
   - ✅ Servlet-level rejection (> 5MB)
   - ✅ Service-level validation (> 5MB)
   - ✅ Clear error messages

## Testing

Test the implementation:

```bash
# Test file upload < 5MB (should succeed)
curl -X POST http://localhost:8080/ticket-tracker/api/files \
  -F "file=@small_file.pdf" \
  -F "ticketId=ABC123" \
  -F "type=general"

# Test file upload > 5MB (should fail)
curl -X POST http://localhost:8080/ticket-tracker/api/files \
  -F "file=@large_file.pdf" \
  -F "ticketId=ABC123" \
  -F "type=general"
```

## Rollback (If Needed)

If you need to rollback the migration:

```sql
-- Drop the index
DROP INDEX idx_documents_has_content;

-- Drop the column
ALTER TABLE documents DROP COLUMN file_content;

COMMIT;
```

## Files Modified

- `database/11-oracle-add-documents-file-content.sql` (NEW)
- `database/install.sql` (MODIFIED)
- `src/main/java/com/tickettracker/servlet/FileServlet.java` (MODIFIED)
- `src/main/java/com/tickettracker/service/DocumentService.java` (MODIFIED)
- `src/main/java/com/tickettracker/service/WorkflowStepProgressDocumentService.java` (MODIFIED)

## Important Notes

- ✅ Existing documents: file_content will be NULL (no backfill needed)
- ✅ New documents: file_content will store binary data
- ✅ Max file size: 5MB (5,242,880 bytes)
- ✅ Backward compatible: Works with existing URL-based storage

## Documentation

For detailed information, see:
- `docs/FILE_CONTENT_COLUMN_IMPLEMENTATION.md`

## Status

✅ **READY TO DEPLOY**

The migration is tested and ready to be applied to your Oracle database.
