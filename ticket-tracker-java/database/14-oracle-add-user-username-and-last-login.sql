/*
  ==================================================================================
  Migration: Add username and last_login columns to users table
  ==================================================================================

  ## Overview
  Adds two missing columns to the users table that are referenced in the application
  code but were not present in the initial schema creation.

  ## Changes Made
  1. Add username column - unique identifier for user login
  2. Add last_login column - tracks last successful login timestamp
  3. Add indexes for performance optimization
  4. Populate username with email for existing users

  ## Column Specifications
  - username: VARCHAR2(100) UNIQUE - User's login name (defaults to email prefix)
  - last_login: TIMESTAMP - Last successful login time (NULL if never logged in)

  ## Migration Safety
  - Uses ALTER TABLE ADD if not exists pattern
  - Populates existing users with safe defaults
  - Non-breaking change for existing applications

  ## Run After
  - 02-oracle-schema.sql (base schema)
  - 07-oracle-seed-data.sql (seed data)
*/

-- Add username column if it doesn't exist
DECLARE
  column_exists NUMBER;
BEGIN
  SELECT COUNT(*) INTO column_exists
  FROM user_tab_columns
  WHERE table_name = 'USERS' AND column_name = 'USERNAME';

  IF column_exists = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE users ADD (username VARCHAR2(100))';
    DBMS_OUTPUT.PUT_LINE('Column username added successfully');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Column username already exists');
  END IF;
END;
/

-- Add last_login column if it doesn't exist
DECLARE
  column_exists NUMBER;
BEGIN
  SELECT COUNT(*) INTO column_exists
  FROM user_tab_columns
  WHERE table_name = 'USERS' AND column_name = 'LAST_LOGIN';

  IF column_exists = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE users ADD (last_login TIMESTAMP)';
    DBMS_OUTPUT.PUT_LINE('Column last_login added successfully');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Column last_login already exists');
  END IF;
END;
/

-- Populate username with email prefix for existing users who don't have a username
BEGIN
  UPDATE users
  SET username = SUBSTR(email, 1, INSTR(email, '@') - 1)
  WHERE username IS NULL
    AND email IS NOT NULL
    AND INSTR(email, '@') > 0;

  COMMIT;
  DBMS_OUTPUT.PUT_LINE('Updated ' || SQL%ROWCOUNT || ' users with default username');
END;
/

-- Add unique constraint on username if it doesn't exist
DECLARE
  constraint_exists NUMBER;
BEGIN
  SELECT COUNT(*) INTO constraint_exists
  FROM user_constraints
  WHERE table_name = 'USERS' AND constraint_name = 'UQ_USERS_USERNAME';

  IF constraint_exists = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE users ADD CONSTRAINT uq_users_username UNIQUE (username)';
    DBMS_OUTPUT.PUT_LINE('Unique constraint on username added successfully');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Unique constraint on username already exists');
  END IF;
END;
/

-- Add index on username for fast lookups
DECLARE
  index_exists NUMBER;
BEGIN
  SELECT COUNT(*) INTO index_exists
  FROM user_indexes
  WHERE table_name = 'USERS' AND index_name = 'IDX_USERS_USERNAME';

  IF index_exists = 0 THEN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_users_username ON users(username)';
    DBMS_OUTPUT.PUT_LINE('Index on username created successfully');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Index on username already exists');
  END IF;
END;
/

-- Add index on last_login for activity tracking queries
DECLARE
  index_exists NUMBER;
BEGIN
  SELECT COUNT(*) INTO index_exists
  FROM user_indexes
  WHERE table_name = 'USERS' AND index_name = 'IDX_USERS_LAST_LOGIN';

  IF index_exists = 0 THEN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_users_last_login ON users(last_login)';
    DBMS_OUTPUT.PUT_LINE('Index on last_login created successfully');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Index on last_login already exists');
  END IF;
END;
/

-- Add comments to document the new columns
COMMENT ON COLUMN users.username IS 'Unique username for login (defaults to email prefix)';
COMMENT ON COLUMN users.last_login IS 'Timestamp of last successful login (NULL if never logged in)';

-- Display summary
SELECT 'Migration 14 completed successfully' AS status FROM dual;

-- Verify the changes
SELECT column_name, data_type, data_length, nullable
FROM user_tab_columns
WHERE table_name = 'USERS'
  AND column_name IN ('USERNAME', 'LAST_LOGIN')
ORDER BY column_name;
