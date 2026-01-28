-- ==================================================================================
-- Migration: Add file_content BLOB column to workflow_step_progress_documents
-- Description: Adds BLOB column to store file binary content inline in the database
-- Author: System
-- Date: 2025-01-23
-- ==================================================================================

-- Add file_content column to store binary file data
ALTER TABLE workflow_step_progress_documents ADD (
  file_content BLOB
);

COMMENT ON COLUMN workflow_step_progress_documents.file_content IS 'Binary file content stored as BLOB';

-- Create index for better query performance when checking for file content existence
CREATE INDEX idx_progress_docs_has_content ON workflow_step_progress_documents(
  CASE WHEN file_content IS NOT NULL THEN 1 ELSE 0 END
);

COMMIT;
