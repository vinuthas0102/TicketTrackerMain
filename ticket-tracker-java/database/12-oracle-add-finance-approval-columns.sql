/*
  ==================================================================================
  Oracle Database Migration: Add Missing Finance Approval Columns
  ==================================================================================

  ## Overview
  This migration adds columns to the finance_approvals table that exist in the
  Supabase schema but were missing from the Oracle schema. Without these columns
  the FinanceApprovalDAO.mapResultSetToFinanceApproval throws "Invalid column name"
  whenever finance approvals are fetched, preventing finance officers from approving
  or reviewing any ticket.

  ## Changes Made
  1. approval_remarks       - Finance officer remarks when approving a request
  2. approval_document_file_name   - Name of the uploaded approval document
  3. approval_document_file_path   - Storage path of the approval document
  4. approval_document_file_size   - Size in bytes of the approval document
  5. approval_document_file_type   - MIME type of the approval document
  6. approval_document_uploaded_at - Timestamp when the approval document was uploaded

  ## Run Order
  Run after all base schema scripts (02-oracle-schema.sql through 11-*).
  This script is idempotent - it checks whether each column exists before adding it.

  ## Important Notes
  - No data will be lost; all new columns are nullable
  - Safe to run multiple times on an already-migrated database
  - Applies to both fresh installs (via install.sql) and existing deployments
==================================================================================
*/

ALTER SESSION SET NLS_DATE_FORMAT = 'YYYY-MM-DD HH24:MI:SS';
ALTER SESSION SET NLS_TIMESTAMP_FORMAT = 'YYYY-MM-DD HH24:MI:SS.FF';

-- ==================================================================================
-- 1. approval_remarks
-- ==================================================================================
DECLARE
  col_exists NUMBER := 0;
BEGIN
  SELECT COUNT(*) INTO col_exists
  FROM USER_TAB_COLUMNS
  WHERE TABLE_NAME = 'FINANCE_APPROVALS' AND COLUMN_NAME = 'APPROVAL_REMARKS';

  IF col_exists = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE finance_approvals ADD approval_remarks VARCHAR2(2000)';
    DBMS_OUTPUT.PUT_LINE('Added approval_remarks column to finance_approvals');
  ELSE
    DBMS_OUTPUT.PUT_LINE('approval_remarks already exists in finance_approvals');
  END IF;
END;
/

-- ==================================================================================
-- 2. approval_document_file_name
-- ==================================================================================
DECLARE
  col_exists NUMBER := 0;
BEGIN
  SELECT COUNT(*) INTO col_exists
  FROM USER_TAB_COLUMNS
  WHERE TABLE_NAME = 'FINANCE_APPROVALS' AND COLUMN_NAME = 'APPROVAL_DOCUMENT_FILE_NAME';

  IF col_exists = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE finance_approvals ADD approval_document_file_name VARCHAR2(500)';
    DBMS_OUTPUT.PUT_LINE('Added approval_document_file_name column to finance_approvals');
  ELSE
    DBMS_OUTPUT.PUT_LINE('approval_document_file_name already exists in finance_approvals');
  END IF;
END;
/

-- ==================================================================================
-- 3. approval_document_file_path
-- ==================================================================================
DECLARE
  col_exists NUMBER := 0;
BEGIN
  SELECT COUNT(*) INTO col_exists
  FROM USER_TAB_COLUMNS
  WHERE TABLE_NAME = 'FINANCE_APPROVALS' AND COLUMN_NAME = 'APPROVAL_DOCUMENT_FILE_PATH';

  IF col_exists = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE finance_approvals ADD approval_document_file_path VARCHAR2(1000)';
    DBMS_OUTPUT.PUT_LINE('Added approval_document_file_path column to finance_approvals');
  ELSE
    DBMS_OUTPUT.PUT_LINE('approval_document_file_path already exists in finance_approvals');
  END IF;
END;
/

-- ==================================================================================
-- 4. approval_document_file_size
-- ==================================================================================
DECLARE
  col_exists NUMBER := 0;
BEGIN
  SELECT COUNT(*) INTO col_exists
  FROM USER_TAB_COLUMNS
  WHERE TABLE_NAME = 'FINANCE_APPROVALS' AND COLUMN_NAME = 'APPROVAL_DOCUMENT_FILE_SIZE';

  IF col_exists = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE finance_approvals ADD approval_document_file_size NUMBER(10)';
    DBMS_OUTPUT.PUT_LINE('Added approval_document_file_size column to finance_approvals');
  ELSE
    DBMS_OUTPUT.PUT_LINE('approval_document_file_size already exists in finance_approvals');
  END IF;
END;
/

-- ==================================================================================
-- 5. approval_document_file_type
-- ==================================================================================
DECLARE
  col_exists NUMBER := 0;
BEGIN
  SELECT COUNT(*) INTO col_exists
  FROM USER_TAB_COLUMNS
  WHERE TABLE_NAME = 'FINANCE_APPROVALS' AND COLUMN_NAME = 'APPROVAL_DOCUMENT_FILE_TYPE';

  IF col_exists = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE finance_approvals ADD approval_document_file_type VARCHAR2(100)';
    DBMS_OUTPUT.PUT_LINE('Added approval_document_file_type column to finance_approvals');
  ELSE
    DBMS_OUTPUT.PUT_LINE('approval_document_file_type already exists in finance_approvals');
  END IF;
END;
/

-- ==================================================================================
-- 6. approval_document_uploaded_at
-- ==================================================================================
DECLARE
  col_exists NUMBER := 0;
BEGIN
  SELECT COUNT(*) INTO col_exists
  FROM USER_TAB_COLUMNS
  WHERE TABLE_NAME = 'FINANCE_APPROVALS' AND COLUMN_NAME = 'APPROVAL_DOCUMENT_UPLOADED_AT';

  IF col_exists = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE finance_approvals ADD approval_document_uploaded_at TIMESTAMP';
    DBMS_OUTPUT.PUT_LINE('Added approval_document_uploaded_at column to finance_approvals');
  ELSE
    DBMS_OUTPUT.PUT_LINE('approval_document_uploaded_at already exists in finance_approvals');
  END IF;
END;
/

-- ==================================================================================
-- Column comments for documentation
-- ==================================================================================
COMMENT ON COLUMN finance_approvals.approval_remarks IS 'Remarks provided by the finance officer when approving the request';
COMMENT ON COLUMN finance_approvals.approval_document_file_name IS 'Original file name of the uploaded approval document';
COMMENT ON COLUMN finance_approvals.approval_document_file_path IS 'Storage path where the approval document is stored';
COMMENT ON COLUMN finance_approvals.approval_document_file_size IS 'Size of the approval document in bytes';
COMMENT ON COLUMN finance_approvals.approval_document_file_type IS 'MIME type of the approval document';
COMMENT ON COLUMN finance_approvals.approval_document_uploaded_at IS 'Timestamp when the approval document was uploaded';

SELECT 'Migration 12 completed successfully!' FROM DUAL;
SELECT 'Added 6 missing columns to finance_approvals table' FROM DUAL;
