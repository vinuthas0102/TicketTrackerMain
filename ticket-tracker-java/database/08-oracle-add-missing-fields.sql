/*
  ==================================================================================
  Oracle Database Migration: Add Missing Fields
  ==================================================================================

  ## Overview
  This migration script adds missing fields that were found during schema audit.
  These fields exist in the Supabase schema but were missing from the Oracle schema.

  ## Changes Made
  1. Add start_date column to workflow_steps table
  2. Add index for start_date for better query performance
  3. Add comment for documentation

  ## Run Order
  Run this after all other schema scripts have been executed.
  This is an idempotent script - it checks if columns exist before adding them.

  ## Important Notes
  - This migration is safe to run multiple times
  - No data will be lost
  - All new columns are nullable to allow existing data to remain valid
*/

-- Set session parameters
ALTER SESSION SET NLS_DATE_FORMAT = 'YYYY-MM-DD HH24:MI:SS';
ALTER SESSION SET NLS_TIMESTAMP_FORMAT = 'YYYY-MM-DD HH24:MI:SS.FF';

-- ==================================================================================
-- Add start_date column to workflow_steps table
-- ==================================================================================

DECLARE
  column_exists NUMBER := 0;
BEGIN
  -- Check if start_date column already exists
  SELECT COUNT(*) INTO column_exists
  FROM USER_TAB_COLUMNS
  WHERE TABLE_NAME = 'WORKFLOW_STEPS' AND COLUMN_NAME = 'START_DATE';

  -- Add column if it doesn't exist
  IF column_exists = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE workflow_steps ADD start_date TIMESTAMP';
    DBMS_OUTPUT.PUT_LINE('Added start_date column to workflow_steps table');
  ELSE
    DBMS_OUTPUT.PUT_LINE('start_date column already exists in workflow_steps table');
  END IF;
END;
/

-- ==================================================================================
-- Create index for start_date column
-- ==================================================================================

DECLARE
  index_exists NUMBER := 0;
BEGIN
  -- Check if index already exists
  SELECT COUNT(*) INTO index_exists
  FROM USER_INDEXES
  WHERE INDEX_NAME = 'IDX_WORKFLOW_STEPS_START_DATE';

  -- Create index if it doesn't exist
  IF index_exists = 0 THEN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_workflow_steps_start_date ON workflow_steps(start_date)';
    DBMS_OUTPUT.PUT_LINE('Created index idx_workflow_steps_start_date');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Index idx_workflow_steps_start_date already exists');
  END IF;
END;
/

-- ==================================================================================
-- Add comment for documentation
-- ==================================================================================

COMMENT ON COLUMN workflow_steps.start_date IS 'Timestamp when work begins on this workflow step';

-- ==================================================================================
-- Display completion message
-- ==================================================================================

SELECT 'Migration 08 completed successfully!' FROM DUAL;
SELECT 'Added start_date field to workflow_steps table' FROM DUAL;
