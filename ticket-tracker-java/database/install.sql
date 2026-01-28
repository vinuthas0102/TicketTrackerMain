/*
  ==================================================================================
  MASTER INSTALLATION SCRIPT FOR TICKET TRACKING SYSTEM (Oracle 19c)
  ==================================================================================

  This script runs all migration files in the correct order to set up the
  complete database schema, indexes, constraints, and seed data.

  ## Prerequisites
  1. Oracle Database 19c installed and running
  2. SQL*Plus or SQL Developer installed
  3. SYS/SYSDBA credentials available

  ## Installation Steps

  ### Step 1: Create Database User (as SYSDBA)
  Run this from command line as SYSDBA:
    sqlplus sys/password@ORCL as sysdba @01-oracle-create-user.sql

  ### Step 2: Run Remaining Scripts (as ticket_tracker user)
  Connect as the ticket_tracker user and run this master script:
    sqlplus ticket_tracker/ticket_pass_2024@ORCL @install.sql

  ## What This Script Does
  1. Creates all database tables with proper data types
  2. Creates sequences for auto-increment fields
  3. Creates triggers for automatic field updates
  4. Creates indexes for query performance
  5. Adds foreign key constraints
  6. Populates seed data (modules, users, field configurations)

  ## Post-Installation
  - Verify installation by checking table counts
  - Update passwords for default users
  - Configure application properties with database credentials
  - Test database connectivity from Java application

  ## Troubleshooting
  - If script fails, check: 99-oracle-cleanup.sql for rollback
  - Ensure you're connected as ticket_tracker user (not SYS)
  - Check Oracle error messages in SQL*Plus output
  - Verify all previous scripts completed without errors

  ==================================================================================
*/

-- Set output and feedback
SET SERVEROUTPUT ON
SET FEEDBACK ON
SET ECHO ON
SET VERIFY OFF

-- Display banner
SELECT '==================================================================================' FROM DUAL;
SELECT 'TICKET TRACKING SYSTEM - DATABASE INSTALLATION' FROM DUAL;
SELECT 'Oracle Database 19c Migration' FROM DUAL;
SELECT '==================================================================================' FROM DUAL;
SELECT '' FROM DUAL;

-- Step 1: Display current user
SELECT 'Current User: ' || USER FROM DUAL;
SELECT 'Current Time: ' || TO_CHAR(SYSDATE, 'YYYY-MM-DD HH24:MI:SS') FROM DUAL;
SELECT '' FROM DUAL;

-- Step 2: Create Schema (Tables)
SELECT '==================================================================================' FROM DUAL;
SELECT 'STEP 1: Creating database schema (tables)...' FROM DUAL;
SELECT '==================================================================================' FROM DUAL;
@02-oracle-schema.sql
SELECT 'Schema creation completed!' FROM DUAL;
SELECT '' FROM DUAL;

-- Step 3: Create Sequences
SELECT '==================================================================================' FROM DUAL;
SELECT 'STEP 2: Creating sequences...' FROM DUAL;
SELECT '==================================================================================' FROM DUAL;
@03-oracle-sequences.sql
SELECT 'Sequences created!' FROM DUAL;
SELECT '' FROM DUAL;

-- Step 4: Create Triggers
SELECT '==================================================================================' FROM DUAL;
SELECT 'STEP 3: Creating triggers...' FROM DUAL;
SELECT '==================================================================================' FROM DUAL;
@04-oracle-triggers.sql
SELECT 'Triggers created!' FROM DUAL;
SELECT '' FROM DUAL;

-- Step 5: Create Indexes
SELECT '==================================================================================' FROM DUAL;
SELECT 'STEP 4: Creating indexes...' FROM DUAL;
SELECT '==================================================================================' FROM DUAL;
@05-oracle-indexes.sql
SELECT 'Indexes created!' FROM DUAL;
SELECT '' FROM DUAL;

-- Step 6: Create Foreign Key Constraints
SELECT '==================================================================================' FROM DUAL;
SELECT 'STEP 5: Adding foreign key constraints...' FROM DUAL;
SELECT '==================================================================================' FROM DUAL;
@06-oracle-constraints.sql
SELECT 'Constraints added!' FROM DUAL;
SELECT '' FROM DUAL;

-- Step 7: Load Seed Data
SELECT '==================================================================================' FROM DUAL;
SELECT 'STEP 6: Loading seed data...' FROM DUAL;
SELECT '==================================================================================' FROM DUAL;
@07-oracle-seed-data.sql
SELECT 'Seed data loaded!' FROM DUAL;
SELECT '' FROM DUAL;

-- Step 8: Apply Additional Migrations
SELECT '==================================================================================' FROM DUAL;
SELECT 'STEP 7: Applying additional migrations...' FROM DUAL;
SELECT '==================================================================================' FROM DUAL;
@10-oracle-add-progress-doc-file-content.sql
@11-oracle-add-documents-file-content.sql
SELECT 'Additional migrations applied!' FROM DUAL;
SELECT '' FROM DUAL;

-- Step 9: Verification
SELECT '==================================================================================' FROM DUAL;
SELECT 'INSTALLATION VERIFICATION' FROM DUAL;
SELECT '==================================================================================' FROM DUAL;

-- Count tables
SELECT 'Total Tables Created: ' || COUNT(*) FROM user_tables;

-- Count sequences
SELECT 'Total Sequences Created: ' || COUNT(*) FROM user_sequences;

-- Count triggers
SELECT 'Total Triggers Created: ' || COUNT(*) FROM user_triggers;

-- Count indexes
SELECT 'Total Indexes Created: ' || COUNT(*) FROM user_indexes WHERE index_type != 'LOB';

-- Count constraints
SELECT 'Total Constraints Created: ' || COUNT(*) FROM user_constraints WHERE constraint_type = 'R';

-- Verify data
SELECT 'Modules Loaded: ' || COUNT(*) FROM modules;
SELECT 'Users Loaded: ' || COUNT(*) FROM users;
SELECT 'Field Definitions Loaded: ' || COUNT(*) FROM field_definitions;
SELECT 'Field Configurations Loaded: ' || COUNT(*) FROM module_field_configurations;
SELECT 'Dropdown Options Loaded: ' || COUNT(*) FROM field_dropdown_options;

-- Verify new columns
SELECT 'Documents table has file_content column: ' ||
       CASE WHEN COUNT(*) > 0 THEN 'YES' ELSE 'NO' END
FROM user_tab_columns
WHERE table_name = 'DOCUMENTS' AND column_name = 'FILE_CONTENT';

SELECT 'Progress documents table has file_content column: ' ||
       CASE WHEN COUNT(*) > 0 THEN 'YES' ELSE 'NO' END
FROM user_tab_columns
WHERE table_name = 'WORKFLOW_STEP_PROGRESS_DOCUMENTS' AND column_name = 'FILE_CONTENT';

SELECT '' FROM DUAL;
SELECT '==================================================================================' FROM DUAL;
SELECT 'INSTALLATION COMPLETED SUCCESSFULLY!' FROM DUAL;
SELECT '==================================================================================' FROM DUAL;
SELECT '' FROM DUAL;
SELECT 'Next Steps:' FROM DUAL;
SELECT '1. Update user passwords (currently set to default)' FROM DUAL;
SELECT '2. Configure application database.properties file' FROM DUAL;
SELECT '3. Test database connectivity from Java application' FROM DUAL;
SELECT '4. Deploy WAR file to Tomcat' FROM DUAL;
SELECT '' FROM DUAL;
SELECT 'Default User Credentials (for testing only):' FROM DUAL;
SELECT '- Administrator: admin@company.com / changeme' FROM DUAL;
SELECT '- Manager: manager@company.com / changeme' FROM DUAL;
SELECT '- Employee: john@company.com / changeme' FROM DUAL;
SELECT '- Finance: finance.officer@company.com / finance123' FROM DUAL;
SELECT '' FROM DUAL;
SELECT 'IMPORTANT: Change all default passwords before production use!' FROM DUAL;
SELECT '==================================================================================' FROM DUAL;
