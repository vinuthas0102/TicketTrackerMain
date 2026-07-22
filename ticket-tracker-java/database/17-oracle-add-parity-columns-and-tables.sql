/*
  ==================================================================================
  Migration: Add Parity Columns and Tables (Bolt feature parity)
  ==================================================================================

  ## Overview
  Adds columns and tables that exist in the Bolt (Supabase) schema but are
  missing from the Oracle schema, to achieve full feature parity.

  ## Changes Made
  1. Add sap_id column to users table
  2. Add remarks column to workflow_steps table
  3. Add actual_completed_at column to workflow_steps table
  4. Create user_management_audit table
  5. Create user_activity_logs table
  6. Add triggers and indexes for new tables

  ## Run Order
  Run after all base schema scripts (02 through 16).
  This script is idempotent - it checks existence before adding.

  ## Notes
  - No data will be lost; all new columns are nullable
  - Safe to run multiple times
  ==================================================================================
*/

ALTER SESSION SET NLS_DATE_FORMAT = 'YYYY-MM-DD HH24:MI:SS';
ALTER SESSION SET NLS_TIMESTAMP_FORMAT = 'YYYY-MM-DD HH24:MI:SS.FF';
SET SERVEROUTPUT ON;

-- ==================================================================================
-- 1. Add sap_id column to users table
-- ==================================================================================
DECLARE
  col_exists NUMBER := 0;
BEGIN
  SELECT COUNT(*) INTO col_exists
  FROM USER_TAB_COLUMNS
  WHERE TABLE_NAME = 'USERS' AND COLUMN_NAME = 'SAP_ID';

  IF col_exists = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE users ADD sap_id VARCHAR2(50)';
    DBMS_OUTPUT.PUT_LINE('Added sap_id column to users table');
  ELSE
    DBMS_OUTPUT.PUT_LINE('sap_id column already exists in users table');
  END IF;
END;
/
COMMENT ON COLUMN users.sap_id IS 'SAP system ID for the user (optional)';

-- Index on sap_id for lookups
DECLARE
  idx_exists NUMBER := 0;
BEGIN
  SELECT COUNT(*) INTO idx_exists
  FROM USER_INDEXES
  WHERE INDEX_NAME = 'IDX_USERS_SAP_ID';

  IF idx_exists = 0 THEN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_users_sap_id ON users(sap_id)';
    DBMS_OUTPUT.PUT_LINE('Created index idx_users_sap_id');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Index idx_users_sap_id already exists');
  END IF;
END;
/

-- ==================================================================================
-- 2. Add remarks column to workflow_steps table
-- ==================================================================================
DECLARE
  col_exists NUMBER := 0;
BEGIN
  SELECT COUNT(*) INTO col_exists
  FROM USER_TAB_COLUMNS
  WHERE TABLE_NAME = 'WORKFLOW_STEPS' AND COLUMN_NAME = 'REMARKS';

  IF col_exists = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE workflow_steps ADD remarks CLOB';
    DBMS_OUTPUT.PUT_LINE('Added remarks column to workflow_steps table');
  ELSE
    DBMS_OUTPUT.PUT_LINE('remarks column already exists in workflow_steps table');
  END IF;
END;
/
COMMENT ON COLUMN workflow_steps.remarks IS 'Additional remarks for the workflow step';

-- ==================================================================================
-- 3. Add actual_completed_at column to workflow_steps table
-- ==================================================================================
DECLARE
  col_exists NUMBER := 0;
BEGIN
  SELECT COUNT(*) INTO col_exists
  FROM USER_TAB_COLUMNS
  WHERE TABLE_NAME = 'WORKFLOW_STEPS' AND COLUMN_NAME = 'ACTUAL_COMPLETED_AT';

  IF col_exists = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE workflow_steps ADD actual_completed_at TIMESTAMP';
    DBMS_OUTPUT.PUT_LINE('Added actual_completed_at column to workflow_steps table');
  ELSE
    DBMS_OUTPUT.PUT_LINE('actual_completed_at column already exists in workflow_steps table');
  END IF;
END;
/
COMMENT ON COLUMN workflow_steps.actual_completed_at IS 'Actual timestamp when the step was completed (may differ from completed_at)';

-- Index on actual_completed_at
DECLARE
  idx_exists NUMBER := 0;
BEGIN
  SELECT COUNT(*) INTO idx_exists
  FROM USER_INDEXES
  WHERE INDEX_NAME = 'IDX_WORKFLOW_STEPS_ACTUAL_COMPLETED';

  IF idx_exists = 0 THEN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_workflow_steps_actual_completed ON workflow_steps(actual_completed_at)';
    DBMS_OUTPUT.PUT_LINE('Created index idx_workflow_steps_actual_completed');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Index idx_workflow_steps_actual_completed already exists');
  END IF;
END;
/

-- ==================================================================================
-- 4. Create user_management_audit table
-- ==================================================================================
DECLARE
  tbl_exists NUMBER := 0;
BEGIN
  SELECT COUNT(*) INTO tbl_exists
  FROM USER_TABLES
  WHERE TABLE_NAME = 'USER_MANAGEMENT_AUDIT';

  IF tbl_exists = 0 THEN
    EXECUTE IMMEDIATE 'CREATE TABLE user_management_audit (
      id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
      action VARCHAR2(100) NOT NULL,
      target_user_id RAW(16),
      performed_by RAW(16) NOT NULL,
      old_data CLOB,
      new_data CLOB,
      description VARCHAR2(2000),
      performed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
    )';
    DBMS_OUTPUT.PUT_LINE('Created user_management_audit table');
  ELSE
    DBMS_OUTPUT.PUT_LINE('user_management_audit table already exists');
  END IF;
END;
/
COMMENT ON TABLE user_management_audit IS 'Audit trail for user management actions (create, update, delete, lock, unlock, password reset)';

-- ==================================================================================
-- 5. Create user_activity_logs table
-- ==================================================================================
DECLARE
  tbl_exists NUMBER := 0;
BEGIN
  SELECT COUNT(*) INTO tbl_exists
  FROM USER_TABLES
  WHERE TABLE_NAME = 'USER_ACTIVITY_LOGS';

  IF tbl_exists = 0 THEN
    EXECUTE IMMEDIATE 'CREATE TABLE user_activity_logs (
      id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
      user_id RAW(16) NOT NULL,
      action VARCHAR2(100) NOT NULL,
      ip_address VARCHAR2(100),
      user_agent VARCHAR2(1000),
      details CLOB,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
    )';
    DBMS_OUTPUT.PUT_LINE('Created user_activity_logs table');
  ELSE
    DBMS_OUTPUT.PUT_LINE('user_activity_logs table already exists');
  END IF;
END;
/
COMMENT ON TABLE user_activity_logs IS 'User activity tracking for login, logout, and session events';

-- ==================================================================================
-- 6. Add foreign keys for new tables
-- ==================================================================================
DECLARE
  cons_exists NUMBER := 0;
BEGIN
  SELECT COUNT(*) INTO cons_exists
  FROM USER_CONSTRAINTS
  WHERE CONSTRAINT_NAME = 'FK_USER_MGMT_AUDIT_TARGET';

  IF cons_exists = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE user_management_audit ADD CONSTRAINT fk_user_mgmt_audit_target
      FOREIGN KEY (target_user_id) REFERENCES users(id) ON DELETE SET NULL';
    DBMS_OUTPUT.PUT_LINE('Added FK fk_user_mgmt_audit_target');
  ELSE
    DBMS_OUTPUT.PUT_LINE('FK fk_user_mgmt_audit_target already exists');
  END IF;
END;
/

DECLARE
  cons_exists NUMBER := 0;
BEGIN
  SELECT COUNT(*) INTO cons_exists
  FROM USER_CONSTRAINTS
  WHERE CONSTRAINT_NAME = 'FK_USER_MGMT_AUDIT_PERFORMER';

  IF cons_exists = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE user_management_audit ADD CONSTRAINT fk_user_mgmt_audit_performer
      FOREIGN KEY (performed_by) REFERENCES users(id) ON DELETE SET NULL';
    DBMS_OUTPUT.PUT_LINE('Added FK fk_user_mgmt_audit_performer');
  ELSE
    DBMS_OUTPUT.PUT_LINE('FK fk_user_mgmt_audit_performer already exists');
  END IF;
END;
/

DECLARE
  cons_exists NUMBER := 0;
BEGIN
  SELECT COUNT(*) INTO cons_exists
  FROM USER_CONSTRAINTS
  WHERE CONSTRAINT_NAME = 'FK_USER_ACTIVITY_LOGS_USER';

  IF cons_exists = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE user_activity_logs ADD CONSTRAINT fk_user_activity_logs_user
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE';
    DBMS_OUTPUT.PUT_LINE('Added FK fk_user_activity_logs_user');
  ELSE
    DBMS_OUTPUT.PUT_LINE('FK fk_user_activity_logs_user already exists');
  END IF;
END;
/

-- ==================================================================================
-- 7. Add indexes for new tables
-- ==================================================================================
DECLARE
  idx_exists NUMBER := 0;
BEGIN
  SELECT COUNT(*) INTO idx_exists FROM USER_INDEXES WHERE INDEX_NAME = 'IDX_USER_MGMT_AUDIT_TARGET';
  IF idx_exists = 0 THEN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_user_mgmt_audit_target ON user_management_audit(target_user_id)';
    DBMS_OUTPUT.PUT_LINE('Created index idx_user_mgmt_audit_target');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Index idx_user_mgmt_audit_target already exists');
  END IF;
END;
/

DECLARE
  idx_exists NUMBER := 0;
BEGIN
  SELECT COUNT(*) INTO idx_exists FROM USER_INDEXES WHERE INDEX_NAME = 'IDX_USER_MGMT_AUDIT_PERFORMER';
  IF idx_exists = 0 THEN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_user_mgmt_audit_performer ON user_management_audit(performed_by)';
    DBMS_OUTPUT.PUT_LINE('Created index idx_user_mgmt_audit_performer');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Index idx_user_mgmt_audit_performer already exists');
  END IF;
END;
/

DECLARE
  idx_exists NUMBER := 0;
BEGIN
  SELECT COUNT(*) INTO idx_exists FROM USER_INDEXES WHERE INDEX_NAME = 'IDX_USER_MGMT_AUDIT_ACTION';
  IF idx_exists = 0 THEN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_user_mgmt_audit_action ON user_management_audit(action)';
    DBMS_OUTPUT.PUT_LINE('Created index idx_user_mgmt_audit_action');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Index idx_user_mgmt_audit_action already exists');
  END IF;
END;
/

DECLARE
  idx_exists NUMBER := 0;
BEGIN
  SELECT COUNT(*) INTO idx_exists FROM USER_INDEXES WHERE INDEX_NAME = 'IDX_USER_MGMT_AUDIT_PERFORMED_AT';
  IF idx_exists = 0 THEN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_user_mgmt_audit_performed_at ON user_management_audit(performed_at DESC)';
    DBMS_OUTPUT.PUT_LINE('Created index idx_user_mgmt_audit_performed_at');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Index idx_user_mgmt_audit_performed_at already exists');
  END IF;
END;
/

DECLARE
  idx_exists NUMBER := 0;
BEGIN
  SELECT COUNT(*) INTO idx_exists FROM USER_INDEXES WHERE INDEX_NAME = 'IDX_USER_ACTIVITY_LOGS_USER';
  IF idx_exists = 0 THEN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_user_activity_logs_user ON user_activity_logs(user_id)';
    DBMS_OUTPUT.PUT_LINE('Created index idx_user_activity_logs_user');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Index idx_user_activity_logs_user already exists');
  END IF;
END;
/

DECLARE
  idx_exists NUMBER := 0;
BEGIN
  SELECT COUNT(*) INTO idx_exists FROM USER_INDEXES WHERE INDEX_NAME = 'IDX_USER_ACTIVITY_LOGS_ACTION';
  IF idx_exists = 0 THEN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_user_activity_logs_action ON user_activity_logs(action)';
    DBMS_OUTPUT.PUT_LINE('Created index idx_user_activity_logs_action');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Index idx_user_activity_logs_action already exists');
  END IF;
END;
/

DECLARE
  idx_exists NUMBER := 0;
BEGIN
  SELECT COUNT(*) INTO idx_exists FROM USER_INDEXES WHERE INDEX_NAME = 'IDX_USER_ACTIVITY_LOGS_CREATED_AT';
  IF idx_exists = 0 THEN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_user_activity_logs_created_at ON user_activity_logs(created_at DESC)';
    DBMS_OUTPUT.PUT_LINE('Created index idx_user_activity_logs_created_at');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Index idx_user_activity_logs_created_at already exists');
  END IF;
END;
/

-- ==================================================================================
-- 8. Add updated_at trigger for workflow_steps remarks/actual_completed_at
--    (already covered by existing trg_workflow_steps_updated_at trigger)
-- ==================================================================================

-- ==================================================================================
-- Display completion message
-- ==================================================================================
SELECT 'Migration 17 completed successfully!' FROM DUAL;
SELECT 'Added: sap_id to users, remarks + actual_completed_at to workflow_steps' FROM DUAL;
SELECT 'Created: user_management_audit, user_activity_logs tables' FROM DUAL;
