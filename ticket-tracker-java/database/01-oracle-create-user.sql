/*
  Oracle Database User and Schema Setup

  This script creates the Oracle user/schema for the ticket tracking system.
  Run this script as SYSDBA before running other migration scripts.

  Usage:
    sqlplus sys/password@ORCL as sysdba @01-oracle-create-user.sql

  NOTE: Change the password 'ticket_pass_2024' to a secure password in production!
*/

-- Drop user if exists (caution: removes all objects)
-- Uncomment the following lines if you need to recreate the user
-- DROP USER ticket_tracker CASCADE;

-- Create user with necessary privileges
CREATE USER ticket_tracker
IDENTIFIED BY ticket_pass_2024
DEFAULT TABLESPACE USERS
TEMPORARY TABLESPACE TEMP
QUOTA UNLIMITED ON USERS;

-- Grant necessary system privileges
GRANT CREATE SESSION TO ticket_tracker;
GRANT CREATE TABLE TO ticket_tracker;
GRANT CREATE SEQUENCE TO ticket_tracker;
GRANT CREATE TRIGGER TO ticket_tracker;
GRANT CREATE PROCEDURE TO ticket_tracker;
GRANT CREATE VIEW TO ticket_tracker;
GRANT CREATE SYNONYM TO ticket_tracker;

-- Grant privileges to create and manage types
GRANT CREATE TYPE TO ticket_tracker;

-- Grant resource role (includes CREATE TABLE, CREATE SEQUENCE, etc.)
GRANT RESOURCE TO ticket_tracker;

-- Grant connect role
GRANT CONNECT TO ticket_tracker;

-- Grant privileges to execute DBMS packages
GRANT EXECUTE ON DBMS_CRYPTO TO ticket_tracker;
GRANT EXECUTE ON DBMS_RANDOM TO ticket_tracker;

-- Display user creation status
SELECT 'User ticket_tracker created successfully!' FROM DUAL;

-- Instructions for next steps
SELECT '========================================' AS "INFO" FROM DUAL;
SELECT 'Next Steps:' AS "INFO" FROM DUAL;
SELECT '1. Connect as ticket_tracker user' AS "INFO" FROM DUAL;
SELECT '2. Run 02-oracle-schema.sql' AS "INFO" FROM DUAL;
SELECT '3. Run 03-oracle-sequences.sql' AS "INFO" FROM DUAL;
SELECT '4. Run 04-oracle-triggers.sql' AS "INFO" FROM DUAL;
SELECT '5. Run 05-oracle-indexes.sql' AS "INFO" FROM DUAL;
SELECT '6. Run 06-oracle-constraints.sql' AS "INFO" FROM DUAL;
SELECT '7. Run 07-oracle-seed-data.sql' AS "INFO" FROM DUAL;
SELECT '========================================' AS "INFO" FROM DUAL;
