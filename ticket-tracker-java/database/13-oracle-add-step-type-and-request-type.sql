/*
  ==================================================================================
  Oracle Database Migration: Add step_type and request_type Columns
  ==================================================================================

  ## Overview
  This migration adds two columns that exist in the Java code and Supabase schema
  but were missing from the Oracle database schema. Without these columns, the
  application throws "Invalid column name" (java.sql.SQLException) whenever any
  ticket list is fetched, because WorkflowStepDAO.mapResultSetToWorkflowStep reads
  step_type from the result set and TicketDAO.mapResultSetToTicket reads request_type.

  ## Changes Made
  1. step_type (VARCHAR2(500)) added to workflow_steps table
     - Stores the type/category of a workflow step (e.g., 'approval', 'review', etc.)
     - Maps to the stepType field in WorkflowStep.java
     - Referenced in WorkflowStepDAO.java line 17 (INSERT) and line 435 (ResultSet mapping)

  2. request_type (VARCHAR2(500)) added to tickets table
     - Stores the request type/category for a ticket
     - Maps to the requestType field in Ticket.java
     - Referenced in TicketDAO.java INSERT, UPDATE, and ResultSet mapping

  ## Run Order
  Run after all base schema scripts (02 through 12).
  This script is idempotent - it checks whether each column exists before adding it.

  ## Important Notes
  - No data will be lost; all new columns are nullable
  - Safe to run multiple times on an already-migrated database
  - Corresponds to Supabase migration: 20260303074252_add_request_type_step_type_and_ce_users.sql
  ==================================================================================
*/

-- Set session parameters
ALTER SESSION SET NLS_DATE_FORMAT = 'YYYY-MM-DD HH24:MI:SS';
ALTER SESSION SET NLS_TIMESTAMP_FORMAT = 'YYYY-MM-DD HH24:MI:SS.FF';

-- ==================================================================================
-- Add step_type column to workflow_steps table
-- ==================================================================================

DECLARE
  column_exists NUMBER := 0;
BEGIN
  SELECT COUNT(*) INTO column_exists
  FROM USER_TAB_COLUMNS
  WHERE TABLE_NAME = 'WORKFLOW_STEPS' AND COLUMN_NAME = 'STEP_TYPE';

  IF column_exists = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE workflow_steps ADD step_type VARCHAR2(500)';
    DBMS_OUTPUT.PUT_LINE('Added step_type column to workflow_steps table');
  ELSE
    DBMS_OUTPUT.PUT_LINE('step_type column already exists in workflow_steps table');
  END IF;
END;
/

COMMENT ON COLUMN workflow_steps.step_type IS 'Type/category of the workflow step (e.g. approval, review, inspection)';

-- ==================================================================================
-- Add request_type column to tickets table
-- ==================================================================================

DECLARE
  column_exists NUMBER := 0;
BEGIN
  SELECT COUNT(*) INTO column_exists
  FROM USER_TAB_COLUMNS
  WHERE TABLE_NAME = 'TICKETS' AND COLUMN_NAME = 'REQUEST_TYPE';

  IF column_exists = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE tickets ADD request_type VARCHAR2(500)';
    DBMS_OUTPUT.PUT_LINE('Added request_type column to tickets table');
  ELSE
    DBMS_OUTPUT.PUT_LINE('request_type column already exists in tickets table');
  END IF;
END;
/

COMMENT ON COLUMN tickets.request_type IS 'Type of request for the ticket (e.g. corrective, preventive, inspection)';

-- ==================================================================================
-- Display completion message
-- ==================================================================================

SELECT 'Migration 13 completed successfully!' FROM DUAL;
SELECT 'Added step_type to workflow_steps and request_type to tickets' FROM DUAL;
