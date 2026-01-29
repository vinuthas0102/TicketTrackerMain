/*
  ==================================================================================
  Oracle Database Migration: Add updated_at to workflow_comments
  ==================================================================================

  ## Overview
  This migration script adds the updated_at column to the workflow_comments table.
  This field is required for tracking when comments are edited by users.

  ## Changes Made
  1. Add updated_at column to workflow_comments table
  2. Set default value to created_at for existing records
  3. Add index for updated_at for better query performance
  4. Add comment for documentation

  ## Run Order
  Run this after 08-oracle-add-missing-fields.sql has been executed.
  This is an idempotent script - it checks if columns exist before adding them.

  ## Important Notes
  - This migration is safe to run multiple times
  - No data will be lost
  - Existing records will have updated_at set to created_at
  - New records will have updated_at set via trigger
*/

-- Set session parameters
ALTER SESSION SET NLS_DATE_FORMAT = 'YYYY-MM-DD HH24:MI:SS';
ALTER SESSION SET NLS_TIMESTAMP_FORMAT = 'YYYY-MM-DD HH24:MI:SS.FF';

-- ==================================================================================
-- Add updated_at column to workflow_comments table
-- ==================================================================================

DECLARE
  column_exists NUMBER := 0;
BEGIN
  -- Check if updated_at column already exists
  SELECT COUNT(*) INTO column_exists
  FROM USER_TAB_COLUMNS
  WHERE TABLE_NAME = 'WORKFLOW_COMMENTS' AND COLUMN_NAME = 'UPDATED_AT';

  -- Add column if it doesn't exist
  IF column_exists = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE workflow_comments ADD updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL';
    DBMS_OUTPUT.PUT_LINE('Added updated_at column to workflow_comments table');

    -- Set updated_at to created_at for existing records
    EXECUTE IMMEDIATE 'UPDATE workflow_comments SET updated_at = created_at WHERE updated_at IS NULL';
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Updated existing records with created_at value');
  ELSE
    DBMS_OUTPUT.PUT_LINE('updated_at column already exists in workflow_comments table');
  END IF;
END;
/

-- ==================================================================================
-- Create index for updated_at column
-- ==================================================================================

DECLARE
  index_exists NUMBER := 0;
BEGIN
  -- Check if index already exists
  SELECT COUNT(*) INTO index_exists
  FROM USER_INDEXES
  WHERE INDEX_NAME = 'IDX_WORKFLOW_COMMENTS_UPDATED_AT';

  -- Create index if it doesn't exist
  IF index_exists = 0 THEN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_workflow_comments_updated_at ON workflow_comments(updated_at)';
    DBMS_OUTPUT.PUT_LINE('Created index idx_workflow_comments_updated_at');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Index idx_workflow_comments_updated_at already exists');
  END IF;
END;
/

-- ==================================================================================
-- Add comment for documentation
-- ==================================================================================

COMMENT ON COLUMN workflow_comments.updated_at IS 'Timestamp when the comment was last updated';

-- ==================================================================================
-- Display completion message
-- ==================================================================================

SELECT 'Migration 09 completed successfully!' FROM DUAL;
SELECT 'Added updated_at field to workflow_comments table' FROM DUAL;
