/*
  ==================================================================================
  DATABASE CLEANUP / ROLLBACK SCRIPT
  ==================================================================================

  WARNING: This script will DROP ALL TABLES, SEQUENCES, and DATA!
  Use this script ONLY for:
  - Development environment cleanup
  - Reinstalling the database from scratch
  - Rolling back a failed installation

  DO NOT RUN IN PRODUCTION!

  Usage:
    sqlplus ticket_tracker/ticket_pass_2024@ORCL @99-oracle-cleanup.sql

  ==================================================================================
*/

SET SERVEROUTPUT ON
SET FEEDBACK ON
SET ECHO ON

-- Display warning
SELECT '==================================================================================' FROM DUAL;
SELECT 'WARNING: THIS WILL DELETE ALL DATA AND DROP ALL TABLES!' FROM DUAL;
SELECT '==================================================================================' FROM DUAL;
SELECT 'Current User: ' || USER FROM DUAL;
SELECT 'Current Time: ' || TO_CHAR(SYSDATE, 'YYYY-MM-DD HH24:MI:SS') FROM DUAL;
SELECT '' FROM DUAL;
SELECT 'Waiting 5 seconds... Press Ctrl+C to cancel!' FROM DUAL;
SELECT '' FROM DUAL;

-- Give user time to cancel
EXEC DBMS_LOCK.SLEEP(5);

SELECT 'Starting cleanup...' FROM DUAL;

-- ==================================================================================
-- Drop All Triggers
-- ==================================================================================
BEGIN
  FOR trig IN (SELECT trigger_name FROM user_triggers) LOOP
    BEGIN
      EXECUTE IMMEDIATE 'DROP TRIGGER ' || trig.trigger_name;
      DBMS_OUTPUT.PUT_LINE('Dropped trigger: ' || trig.trigger_name);
    EXCEPTION
      WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Error dropping trigger ' || trig.trigger_name || ': ' || SQLERRM);
    END;
  END LOOP;
END;
/

-- ==================================================================================
-- Drop All Procedures
-- ==================================================================================
BEGIN
  FOR proc IN (SELECT object_name FROM user_procedures WHERE object_type = 'PROCEDURE') LOOP
    BEGIN
      EXECUTE IMMEDIATE 'DROP PROCEDURE ' || proc.object_name;
      DBMS_OUTPUT.PUT_LINE('Dropped procedure: ' || proc.object_name);
    EXCEPTION
      WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Error dropping procedure ' || proc.object_name || ': ' || SQLERRM);
    END;
  END LOOP;
END;
/

-- ==================================================================================
-- Drop All Functions
-- ==================================================================================
BEGIN
  FOR func IN (SELECT object_name FROM user_objects WHERE object_type = 'FUNCTION') LOOP
    BEGIN
      EXECUTE IMMEDIATE 'DROP FUNCTION ' || func.object_name;
      DBMS_OUTPUT.PUT_LINE('Dropped function: ' || func.object_name);
    EXCEPTION
      WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Error dropping function ' || func.object_name || ': ' || SQLERRM);
    END;
  END LOOP;
END;
/

-- ==================================================================================
-- Drop All Tables (with CASCADE CONSTRAINTS)
-- ==================================================================================
BEGIN
  FOR tab IN (SELECT table_name FROM user_tables) LOOP
    BEGIN
      EXECUTE IMMEDIATE 'DROP TABLE ' || tab.table_name || ' CASCADE CONSTRAINTS PURGE';
      DBMS_OUTPUT.PUT_LINE('Dropped table: ' || tab.table_name);
    EXCEPTION
      WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Error dropping table ' || tab.table_name || ': ' || SQLERRM);
    END;
  END LOOP;
END;
/

-- ==================================================================================
-- Drop All Sequences
-- ==================================================================================
BEGIN
  FOR seq IN (SELECT sequence_name FROM user_sequences) LOOP
    BEGIN
      EXECUTE IMMEDIATE 'DROP SEQUENCE ' || seq.sequence_name;
      DBMS_OUTPUT.PUT_LINE('Dropped sequence: ' || seq.sequence_name);
    EXCEPTION
      WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Error dropping sequence ' || seq.sequence_name || ': ' || SQLERRM);
    END;
  END LOOP;
END;
/

-- ==================================================================================
-- Purge Recyclebin
-- ==================================================================================
BEGIN
  EXECUTE IMMEDIATE 'PURGE RECYCLEBIN';
  DBMS_OUTPUT.PUT_LINE('Recyclebin purged');
EXCEPTION
  WHEN OTHERS THEN
    DBMS_OUTPUT.PUT_LINE('Error purging recyclebin: ' || SQLERRM);
END;
/

-- ==================================================================================
-- Verification
-- ==================================================================================
SELECT '==================================================================================' FROM DUAL;
SELECT 'CLEANUP COMPLETED' FROM DUAL;
SELECT '==================================================================================' FROM DUAL;

SELECT 'Remaining Tables: ' || COUNT(*) FROM user_tables;
SELECT 'Remaining Sequences: ' || COUNT(*) FROM user_sequences;
SELECT 'Remaining Triggers: ' || COUNT(*) FROM user_triggers;
SELECT 'Remaining Procedures: ' || COUNT(*) FROM user_procedures;

SELECT '' FROM DUAL;
SELECT 'Database schema has been completely removed.' FROM DUAL;
SELECT 'You can now run install.sql to reinstall the database.' FROM DUAL;
SELECT '==================================================================================' FROM DUAL;
