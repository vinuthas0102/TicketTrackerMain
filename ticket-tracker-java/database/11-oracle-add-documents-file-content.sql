-- ==================================================================================
-- Migration: Add file_content BLOB column to documents table
-- Description: Adds BLOB column to store file binary content inline in the database
-- Author: System
-- Date: 2025-01-23
-- ==================================================================================

-- Add file_content column to store binary file data
ALTER TABLE documents ADD (
  file_content BLOB
);

COMMENT ON COLUMN documents.file_content IS 'Binary file content stored as BLOB (max 5MB)';

-- Create index for better query performance when checking for file content existence
CREATE INDEX idx_documents_has_content ON documents(
  CASE WHEN file_content IS NOT NULL THEN 1 ELSE 0 END
);

COMMIT;

-- Display completion message
SELECT 'Migration 11 completed: file_content column added to documents table' FROM DUAL;
