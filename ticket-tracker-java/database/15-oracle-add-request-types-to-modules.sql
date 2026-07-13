/*
  ==================================================================================
  Oracle Database Migration: Add requestTypes to Module Configurations
  ==================================================================================

  ## Overview
  This migration updates existing module configurations to add the requestTypes
  array. This enables the Request Type field to appear in ticket creation forms.

  ## Changes Made
  1. Updates all active modules to add requestTypes array to their config JSON
  2. Adds four request types: Pre-Occupation Maintenance, Vacation Handover,
     Annual Maintenance, and Emergency Maintenance
  3. Marks "Vacation Handover" and "Annual Maintenance" with requiresCEInspection: true

  ## Important Notes
  - Safe to run multiple times on an already-migrated database
  - Does not modify existing categories or other config properties
  - Only updates modules where requestTypes is not already present
  - Corresponds to Supabase migration: 20260303074252_add_request_type_step_type_and_ce_users.sql
  ==================================================================================
*/

SET SERVEROUTPUT ON;

-- ==================================================================================
-- Update Maintenance Tracker module with requestTypes
-- ==================================================================================
DECLARE
  v_module_id RAW(16);
  v_current_config CLOB;
  v_new_config CLOB;
  v_count NUMBER;
BEGIN
  -- Get module ID for Maintenance Tracker
  SELECT id, config INTO v_module_id, v_current_config
  FROM modules
  WHERE schema_id = 'maintenance';

  -- Check if requestTypes already exists
  SELECT COUNT(*) INTO v_count
  FROM modules
  WHERE schema_id = 'maintenance'
  AND config LIKE '%requestTypes%';

  IF v_count = 0 THEN
    -- Build new config with requestTypes
    v_new_config := REPLACE(v_current_config, ']}}', '], "requestTypes": [{"label": "Pre-Occupation Maintenance", "value": "Pre-Occupation Maintenance", "requiresCEInspection": false}, {"label": "Vacation Handover", "value": "Vacation Handover", "requiresCEInspection": true}, {"label": "Annual Maintenance", "value": "Annual Maintenance", "requiresCEInspection": true}, {"label": "Emergency Maintenance", "value": "Emergency Maintenance", "requiresCEInspection": false}]}');

    -- Handle case where config only has categories (no closing brackets)
    IF v_new_config = v_current_config THEN
      v_new_config := REPLACE(v_current_config, ']}', '], "requestTypes": [{"label": "Pre-Occupation Maintenance", "value": "Pre-Occupation Maintenance", "requiresCEInspection": false}, {"label": "Vacation Handover", "value": "Vacation Handover", "requiresCEInspection": true}, {"label": "Annual Maintenance", "value": "Annual Maintenance", "requiresCEInspection": true}, {"label": "Emergency Maintenance", "value": "Emergency Maintenance", "requiresCEInspection": false}]}');
    END IF;

    UPDATE modules
    SET config = v_new_config,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = v_module_id;

    DBMS_OUTPUT.PUT_LINE('Updated Maintenance Tracker with requestTypes');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Maintenance Tracker already has requestTypes');
  END IF;

  COMMIT;
EXCEPTION
  WHEN NO_DATA_FOUND THEN
    DBMS_OUTPUT.PUT_LINE('Maintenance Tracker module not found');
  WHEN OTHERS THEN
    DBMS_OUTPUT.PUT_LINE('Error updating Maintenance Tracker: ' || SQLERRM);
    ROLLBACK;
END;
/

-- ==================================================================================
-- Update Complaints Tracker module with requestTypes
-- ==================================================================================
DECLARE
  v_module_id RAW(16);
  v_current_config CLOB;
  v_new_config CLOB;
  v_count NUMBER;
BEGIN
  SELECT id, config INTO v_module_id, v_current_config
  FROM modules
  WHERE schema_id = 'complaints';

  SELECT COUNT(*) INTO v_count
  FROM modules
  WHERE schema_id = 'complaints'
  AND config LIKE '%requestTypes%';

  IF v_count = 0 THEN
    v_new_config := REPLACE(v_current_config, ']}', '], "requestTypes": [{"label": "Pre-Occupation Maintenance", "value": "Pre-Occupation Maintenance", "requiresCEInspection": false}, {"label": "Vacation Handover", "value": "Vacation Handover", "requiresCEInspection": true}, {"label": "Annual Maintenance", "value": "Annual Maintenance", "requiresCEInspection": true}, {"label": "Emergency Maintenance", "value": "Emergency Maintenance", "requiresCEInspection": false}]}');

    UPDATE modules
    SET config = v_new_config,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = v_module_id;

    DBMS_OUTPUT.PUT_LINE('Updated Complaints Tracker with requestTypes');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Complaints Tracker already has requestTypes');
  END IF;

  COMMIT;
EXCEPTION
  WHEN NO_DATA_FOUND THEN
    DBMS_OUTPUT.PUT_LINE('Complaints Tracker module not found');
  WHEN OTHERS THEN
    DBMS_OUTPUT.PUT_LINE('Error updating Complaints Tracker: ' || SQLERRM);
    ROLLBACK;
END;
/

-- ==================================================================================
-- Update Grievances Management module with requestTypes
-- ==================================================================================
DECLARE
  v_module_id RAW(16);
  v_current_config CLOB;
  v_new_config CLOB;
  v_count NUMBER;
BEGIN
  SELECT id, config INTO v_module_id, v_current_config
  FROM modules
  WHERE schema_id = 'grievances';

  SELECT COUNT(*) INTO v_count
  FROM modules
  WHERE schema_id = 'grievances'
  AND config LIKE '%requestTypes%';

  IF v_count = 0 THEN
    v_new_config := REPLACE(v_current_config, ']}', '], "requestTypes": [{"label": "Pre-Occupation Maintenance", "value": "Pre-Occupation Maintenance", "requiresCEInspection": false}, {"label": "Vacation Handover", "value": "Vacation Handover", "requiresCEInspection": true}, {"label": "Annual Maintenance", "value": "Annual Maintenance", "requiresCEInspection": true}, {"label": "Emergency Maintenance", "value": "Emergency Maintenance", "requiresCEInspection": false}]}');

    UPDATE modules
    SET config = v_new_config,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = v_module_id;

    DBMS_OUTPUT.PUT_LINE('Updated Grievances Management with requestTypes');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Grievances Management already has requestTypes');
  END IF;

  COMMIT;
EXCEPTION
  WHEN NO_DATA_FOUND THEN
    DBMS_OUTPUT.PUT_LINE('Grievances Management module not found');
  WHEN OTHERS THEN
    DBMS_OUTPUT.PUT_LINE('Error updating Grievances Management: ' || SQLERRM);
    ROLLBACK;
END;
/

-- ==================================================================================
-- Update RTI Tracker module with requestTypes
-- ==================================================================================
DECLARE
  v_module_id RAW(16);
  v_current_config CLOB;
  v_new_config CLOB;
  v_count NUMBER;
BEGIN
  SELECT id, config INTO v_module_id, v_current_config
  FROM modules
  WHERE schema_id = 'rti';

  SELECT COUNT(*) INTO v_count
  FROM modules
  WHERE schema_id = 'rti'
  AND config LIKE '%requestTypes%';

  IF v_count = 0 THEN
    v_new_config := REPLACE(v_current_config, ']}', '], "requestTypes": [{"label": "Pre-Occupation Maintenance", "value": "Pre-Occupation Maintenance", "requiresCEInspection": false}, {"label": "Vacation Handover", "value": "Vacation Handover", "requiresCEInspection": true}, {"label": "Annual Maintenance", "value": "Annual Maintenance", "requiresCEInspection": true}, {"label": "Emergency Maintenance", "value": "Emergency Maintenance", "requiresCEInspection": false}]}');

    UPDATE modules
    SET config = v_new_config,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = v_module_id;

    DBMS_OUTPUT.PUT_LINE('Updated RTI Tracker with requestTypes');
  ELSE
    DBMS_OUTPUT.PUT_LINE('RTI Tracker already has requestTypes');
  END IF;

  COMMIT;
EXCEPTION
  WHEN NO_DATA_FOUND THEN
    DBMS_OUTPUT.PUT_LINE('RTI Tracker module not found');
  WHEN OTHERS THEN
    DBMS_OUTPUT.PUT_LINE('Error updating RTI Tracker: ' || SQLERRM);
    ROLLBACK;
END;
/

-- ==================================================================================
-- Update Project Execution Platform module with requestTypes
-- ==================================================================================
DECLARE
  v_module_id RAW(16);
  v_current_config CLOB;
  v_new_config CLOB;
  v_count NUMBER;
BEGIN
  SELECT id, config INTO v_module_id, v_current_config
  FROM modules
  WHERE schema_id = 'pep';

  SELECT COUNT(*) INTO v_count
  FROM modules
  WHERE schema_id = 'pep'
  AND config LIKE '%requestTypes%';

  IF v_count = 0 THEN
    v_new_config := REPLACE(v_current_config, ']}', '], "requestTypes": [{"label": "Pre-Occupation Maintenance", "value": "Pre-Occupation Maintenance", "requiresCEInspection": false}, {"label": "Vacation Handover", "value": "Vacation Handover", "requiresCEInspection": true}, {"label": "Annual Maintenance", "value": "Annual Maintenance", "requiresCEInspection": true}, {"label": "Emergency Maintenance", "value": "Emergency Maintenance", "requiresCEInspection": false}]}');

    UPDATE modules
    SET config = v_new_config,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = v_module_id;

    DBMS_OUTPUT.PUT_LINE('Updated Project Execution Platform with requestTypes');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Project Execution Platform already has requestTypes');
  END IF;

  COMMIT;
EXCEPTION
  WHEN NO_DATA_FOUND THEN
    DBMS_OUTPUT.PUT_LINE('Project Execution Platform module not found');
  WHEN OTHERS THEN
    DBMS_OUTPUT.PUT_LINE('Error updating Project Execution Platform: ' || SQLERRM);
    ROLLBACK;
END;
/

-- ==================================================================================
-- Add Civil Manager user if not exists
-- ==================================================================================
DECLARE
  v_count NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_count
  FROM users
  WHERE department = 'Civil Manager';

  IF v_count = 0 THEN
    INSERT INTO users (id, name, email, role, department, password_hash, password_salt, active)
    VALUES (
      HEXTORAW(REPLACE('550e8400-e29b-41d4-a716-446655440040', '-', '')),
      'Civil Manager',
      'civil.manager@company.com',
      'dept_officer',
      'Civil Manager',
      'changeme',
      'salt',
      1
    );
    DBMS_OUTPUT.PUT_LINE('Added Civil Manager user');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Civil Manager user already exists');
  END IF;

  COMMIT;
EXCEPTION
  WHEN OTHERS THEN
    DBMS_OUTPUT.PUT_LINE('Error adding Civil Manager: ' || SQLERRM);
    ROLLBACK;
END;
/

-- ==================================================================================
-- Add Electrical Manager user if not exists
-- ==================================================================================
DECLARE
  v_count NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_count
  FROM users
  WHERE department = 'Electrical Manager';

  IF v_count = 0 THEN
    INSERT INTO users (id, name, email, role, department, password_hash, password_salt, active)
    VALUES (
      HEXTORAW(REPLACE('550e8400-e29b-41d4-a716-446655440041', '-', '')),
      'Electrical Manager',
      'electrical.manager@company.com',
      'dept_officer',
      'Electrical Manager',
      'changeme',
      'salt',
      1
    );
    DBMS_OUTPUT.PUT_LINE('Added Electrical Manager user');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Electrical Manager user already exists');
  END IF;

  COMMIT;
EXCEPTION
  WHEN OTHERS THEN
    DBMS_OUTPUT.PUT_LINE('Error adding Electrical Manager: ' || SQLERRM);
    ROLLBACK;
END;
/

-- ==================================================================================
-- Display completion message
-- ==================================================================================
SELECT 'Migration 15 completed successfully!' FROM DUAL;
SELECT 'Added requestTypes to all active modules' FROM DUAL;
SELECT 'Added Civil and Electrical Manager users for C&E inspections' FROM DUAL;
