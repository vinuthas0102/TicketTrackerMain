# Oracle Database Migration Guide
## Ticket Tracker System - PostgreSQL to Oracle Conversion

---

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Data Type Mapping Reference](#data-type-mapping-reference)
4. [Step-by-Step Migration Instructions](#step-by-step-migration-instructions)
5. [PostgreSQL to Oracle Syntax Differences](#postgresql-to-oracle-syntax-differences)
6. [Verification Steps](#verification-steps)
7. [Performance Tuning](#performance-tuning)
8. [Troubleshooting](#troubleshooting)
9. [Rollback Procedures](#rollback-procedures)

---

## Overview

This guide provides complete instructions for migrating the Ticket Tracker database schema from PostgreSQL (Supabase) to Oracle Database.

### Migration Scope
- **20+ Tables**: All application tables with complete schema
- **100+ Indexes**: Performance-optimized indexes
- **40+ Triggers**: Auto-generation and audit triggers
- **3 Sequences**: For auto-incrementing values
- **1 Function**: UUID generation function

### Database Structure
```
ticket_tracker (Oracle Schema)
├── 20 Tables
├── 100+ Indexes
├── 40+ Triggers
├── 3 Sequences
└── 1 Function
```

---

## Prerequisites

### 1. Oracle Database Requirements
- **Oracle Database Version**: 11g Release 2 or higher (12c+ recommended for JSON support)
- **Minimum Disk Space**: 5 GB for database, 10 GB for file storage
- **Memory**: Minimum 2 GB RAM allocated to Oracle instance
- **Character Set**: AL32UTF8 (Unicode) recommended

### 2. Required Privileges
```sql
-- Connect as SYSTEM or DBA user
GRANT CONNECT TO ticket_tracker;
GRANT RESOURCE TO ticket_tracker;
GRANT UNLIMITED TABLESPACE TO ticket_tracker;
GRANT CREATE VIEW TO ticket_tracker;
GRANT CREATE SEQUENCE TO ticket_tracker;
GRANT CREATE TRIGGER TO ticket_tracker;
GRANT CREATE PROCEDURE TO ticket_tracker;
```

### 3. Tools Required
- SQL*Plus or SQL Developer
- Text editor for reviewing scripts
- Backup tools for rollback capability

### 4. Network Requirements
- Direct connection to Oracle Database
- Port 1521 (default) or custom Oracle listener port
- Firewall rules configured for database access

---

## Data Type Mapping Reference

### Complete PostgreSQL to Oracle Data Type Mapping

| PostgreSQL Type | Oracle Equivalent | Notes |
|----------------|------------------|-------|
| `UUID` | `VARCHAR2(36)` or `RAW(16)` | Using VARCHAR2(36) for readability |
| `text` | `VARCHAR2(4000)` or `CLOB` | Use CLOB for >4000 characters |
| `varchar(n)` | `VARCHAR2(n)` | Direct mapping |
| `integer` | `NUMBER(10)` | 32-bit integer |
| `bigint` | `NUMBER(19)` | 64-bit integer |
| `numeric(p,s)` | `NUMBER(p,s)` | Direct mapping |
| `boolean` | `NUMBER(1)` | 0=false, 1=true |
| `timestamptz` | `TIMESTAMP WITH TIME ZONE` | Direct mapping |
| `timestamp` | `TIMESTAMP` | Without timezone |
| `date` | `DATE` | Direct mapping |
| `jsonb` / `json` | `JSON` (12c+) or `CLOB` | Use CLOB for 11g |
| `array[]` | `VARCHAR2(4000)` or nested table | Comma-separated or VARRAY |
| `serial` | `NUMBER` + SEQUENCE + TRIGGER | Requires sequence/trigger |
| `bytea` | `BLOB` | Binary data |

### Special Cases

#### 1. UUID Handling
**PostgreSQL:**
```sql
id uuid PRIMARY KEY DEFAULT gen_random_uuid()
```

**Oracle:**
```sql
id VARCHAR2(36) PRIMARY KEY
-- Handled by trigger using generate_uuid() function
```

#### 2. Boolean Handling
**PostgreSQL:**
```sql
active boolean DEFAULT true
```

**Oracle:**
```sql
active NUMBER(1) DEFAULT 1
-- 0 = false, 1 = true
```

#### 3. Array Handling
**PostgreSQL:**
```sql
dependencies text[]
```

**Oracle Option 1 (Simple):**
```sql
dependencies VARCHAR2(4000)
-- Store as comma-separated values: 'id1,id2,id3'
```

**Oracle Option 2 (Complex):**
```sql
CREATE TYPE string_array AS VARRAY(100) OF VARCHAR2(100);
dependencies string_array
```

#### 4. JSON Handling
**PostgreSQL:**
```sql
config jsonb DEFAULT '{}'
```

**Oracle 12c+:**
```sql
config CLOB
CONSTRAINT chk_config CHECK (config IS JSON)
```

**Oracle 11g:**
```sql
config CLOB
-- Application validates JSON format
```

---

## Step-by-Step Migration Instructions

### Phase 1: Preparation

#### Step 1.1: Create Oracle User/Schema
```sql
-- Connect as SYSTEM or DBA
sqlplus system/password@database

-- Create user
CREATE USER ticket_tracker IDENTIFIED BY your_secure_password;

-- Grant privileges
GRANT CONNECT, RESOURCE TO ticket_tracker;
GRANT UNLIMITED TABLESPACE TO ticket_tracker;
GRANT CREATE VIEW TO ticket_tracker;
GRANT CREATE SEQUENCE TO ticket_tracker;
GRANT CREATE TRIGGER TO ticket_tracker;
GRANT CREATE PROCEDURE TO ticket_tracker;

-- Verify user creation
SELECT username, account_status, default_tablespace
FROM dba_users
WHERE username = 'TICKET_TRACKER';

-- Exit and reconnect as new user
DISCONNECT;
CONNECT ticket_tracker/your_secure_password@database
```

#### Step 1.2: Verify Oracle Environment
```sql
-- Check Oracle version
SELECT * FROM v$version;

-- Check character set
SELECT value FROM nls_database_parameters WHERE parameter = 'NLS_CHARACTERSET';

-- Check available tablespace
SELECT tablespace_name, bytes/1024/1024 AS mb_free
FROM dba_free_space
WHERE tablespace_name = (
    SELECT default_tablespace FROM dba_users WHERE username = 'TICKET_TRACKER'
);

-- Check JSON support (Oracle 12c+)
SELECT * FROM v$option WHERE parameter = 'JSON';
```

---

### Phase 2: Schema Creation

#### Step 2.1: Create Sequences and Functions
```bash
# Navigate to migration directory
cd database/01_schema

# Run sequences script
sqlplus ticket_tracker/password@database @03_create_sequences.sql
```

**Verification:**
```sql
SELECT sequence_name FROM user_sequences;
-- Should show:
-- SEQ_TICKET_NUMBER
-- SEQ_STEP_NUMBER
```

**Create UUID Function:**
```sql
-- This is included in 03_create_sequences.sql
-- Verify function creation:
SELECT object_name, object_type, status
FROM user_objects
WHERE object_type = 'FUNCTION';
```

#### Step 2.2: Create All Tables
```bash
sqlplus ticket_tracker/password@database @01_create_tables.sql
```

**Verification:**
```sql
-- Count tables created
SELECT COUNT(*) FROM user_tables;
-- Should return 20

-- List all tables
SELECT table_name FROM user_tables ORDER BY table_name;

-- Check specific table structure
DESC users;
DESC tickets;
DESC workflow_steps;
```

**Expected Tables:**
1. users
2. modules
3. tickets
4. workflow_steps
5. workflow_comments
6. documents
7. file_attachments
8. audit_logs
9. field_definitions
10. module_field_configurations
11. field_dropdown_options
12. ticket_field_values
13. workflow_step_field_values
14. workflow_step_dependencies
15. file_reference_templates
16. file_references
17. workflow_step_progress_documents
18. finance_approval_workflow
19. user_display_preferences
20. user_roles

#### Step 2.3: Create Indexes
```bash
sqlplus ticket_tracker/password@database @02_create_indexes.sql
```

**Verification:**
```sql
-- Count indexes created
SELECT COUNT(*) FROM user_indexes WHERE table_name NOT LIKE 'SYS%';

-- List indexes by table
SELECT table_name, index_name, uniqueness
FROM user_indexes
ORDER BY table_name, index_name;

-- Check index status
SELECT index_name, status FROM user_indexes WHERE status != 'VALID';
```

#### Step 2.4: Create Triggers
```bash
sqlplus ticket_tracker/password@database @04_create_triggers.sql
```

**Verification:**
```sql
-- Count triggers created
SELECT COUNT(*) FROM user_triggers;

-- List all triggers
SELECT trigger_name, table_name, triggering_event, status
FROM user_triggers
ORDER BY table_name, trigger_name;

-- Check trigger status
SELECT trigger_name, status FROM user_triggers WHERE status != 'ENABLED';
```

---

### Phase 3: Seed Initial Data

#### Step 3.1: Seed Users
```bash
sqlplus ticket_tracker/password@database @02_seed/01_seed_users.sql
```

**Verification:**
```sql
SELECT id, name, email, role, department FROM users;
```

#### Step 3.2: Seed Modules
```bash
sqlplus ticket_tracker/password@database @02_seed/02_seed_modules.sql
```

**Verification:**
```sql
SELECT id, name, schema_id, active FROM modules;
```

#### Step 3.3: Seed Field Definitions
```bash
sqlplus ticket_tracker/password@database @02_seed/03_seed_field_definitions.sql
```

**Verification:**
```sql
SELECT field_key, label, field_type FROM field_definitions;
```

---

### Phase 4: Verification and Testing

#### Step 4.1: Verify Schema Integrity
```sql
-- Check all foreign key constraints
SELECT constraint_name, table_name, constraint_type, status
FROM user_constraints
WHERE constraint_type = 'R'
ORDER BY table_name;

-- Check all check constraints
SELECT constraint_name, table_name, search_condition
FROM user_constraints
WHERE constraint_type = 'C'
AND constraint_name NOT LIKE 'SYS%';

-- Verify trigger functionality
INSERT INTO users (name, email, role, department)
VALUES ('Test User', 'test@example.com', 'employee', 'IT');
-- Check if ID was auto-generated
SELECT id, name FROM users WHERE email = 'test@example.com';
ROLLBACK;
```

#### Step 4.2: Test Data Insertion
```sql
-- Test ticket creation
INSERT INTO tickets (
    module_id, title, description, status,
    created_by, property_id, property_location
) VALUES (
    (SELECT id FROM modules WHERE schema_id = 'works' AND ROWNUM = 1),
    'Test Ticket',
    'Test Description',
    'open',
    (SELECT id FROM users WHERE role = 'employee' AND ROWNUM = 1),
    'TEST001',
    'Test Location'
);

-- Verify ticket was created with auto-generated ticket number
SELECT id, ticket_number, title FROM tickets WHERE title = 'Test Ticket';

ROLLBACK;
```

#### Step 4.3: Performance Test
```sql
-- Test query performance
SET TIMING ON;

SELECT t.*, u.name as created_by_name
FROM tickets t
JOIN users u ON t.created_by = u.id;

SELECT ws.*, t.ticket_number
FROM workflow_steps ws
JOIN tickets t ON ws.ticket_id = t.id;

SET TIMING OFF;
```

---

## PostgreSQL to Oracle Syntax Differences

### 1. Function Names
| PostgreSQL | Oracle |
|------------|--------|
| `NOW()` | `SYSTIMESTAMP` or `SYSDATE` |
| `CURRENT_DATE` | `TRUNC(SYSDATE)` |
| `gen_random_uuid()` | `generate_uuid()` (custom function) |
| `COALESCE()` | `NVL()` or `COALESCE()` |
| `string_agg()` | `LISTAGG()` |

### 2. String Functions
| PostgreSQL | Oracle |
|------------|--------|
| `||` (concatenation) | `||` (same) |
| `LOWER()` | `LOWER()` (same) |
| `UPPER()` | `UPPER()` (same) |
| `SUBSTRING()` | `SUBSTR()` |
| `LENGTH()` | `LENGTH()` (same) |
| `POSITION()` | `INSTR()` |

### 3. Date Functions
| PostgreSQL | Oracle |
|------------|--------|
| `AGE()` | Calculate difference manually |
| `DATE_TRUNC()` | `TRUNC()` |
| `EXTRACT()` | `EXTRACT()` (same) |
| `INTERVAL '1 day'` | `INTERVAL '1' DAY` |

### 4. Conditional Logic
**PostgreSQL:**
```sql
CASE WHEN status = 'open' THEN 1 ELSE 0 END
```

**Oracle (same):**
```sql
CASE WHEN status = 'open' THEN 1 ELSE 0 END
```

### 5. Limit/Offset
**PostgreSQL:**
```sql
SELECT * FROM tickets LIMIT 10 OFFSET 20;
```

**Oracle 12c+:**
```sql
SELECT * FROM tickets OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY;
```

**Oracle 11g:**
```sql
SELECT * FROM (
    SELECT t.*, ROWNUM rn FROM tickets t WHERE ROWNUM <= 30
) WHERE rn > 20;
```

### 6. Returning Clause
**PostgreSQL:**
```sql
INSERT INTO tickets (...) VALUES (...) RETURNING id;
```

**Oracle:**
```sql
INSERT INTO tickets (...) VALUES (...) RETURNING id INTO :id_var;
-- Or retrieve via trigger/sequence
```

---

## Performance Tuning

### 1. Gather Statistics
```sql
-- Gather statistics for all tables
BEGIN
    DBMS_STATS.GATHER_SCHEMA_STATS('TICKET_TRACKER');
END;
/

-- Gather statistics for specific table
BEGIN
    DBMS_STATS.GATHER_TABLE_STATS('TICKET_TRACKER', 'TICKETS');
END;
/
```

### 2. Analyze Query Plans
```sql
-- Enable execution plan display
SET AUTOTRACE ON EXPLAIN;

-- Run your query
SELECT * FROM tickets WHERE status = 'open';

-- View plan
SET AUTOTRACE OFF;
```

### 3. Create Additional Indexes
```sql
-- Add function-based indexes if needed
CREATE INDEX idx_tickets_upper_title ON tickets(UPPER(title));

-- Add composite indexes for common queries
CREATE INDEX idx_tickets_status_created ON tickets(status, created_at);
```

### 4. Configure Connection Pool
```properties
# In application.properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

---

## Troubleshooting

### Issue 1: ORA-00942: table or view does not exist
**Cause**: Table not created or incorrect schema
**Solution**:
```sql
-- Check if table exists
SELECT table_name FROM user_tables WHERE table_name = 'TICKETS';

-- If not found, run table creation script
@01_schema/01_create_tables.sql
```

### Issue 2: ORA-01400: cannot insert NULL into column
**Cause**: Trigger not firing or constraint violation
**Solution**:
```sql
-- Check trigger status
SELECT trigger_name, status FROM user_triggers WHERE table_name = 'TICKETS';

-- Enable trigger if disabled
ALTER TRIGGER trg_tickets_before_insert ENABLE;
```

### Issue 3: ORA-02291: integrity constraint violated - parent key not found
**Cause**: Foreign key reference to non-existent record
**Solution**:
```sql
-- Check parent table has data
SELECT COUNT(*) FROM modules;
SELECT COUNT(*) FROM users;

-- Insert required parent data first
```

### Issue 4: JSON validation errors (Oracle 12c+)
**Cause**: Invalid JSON in CLOB column
**Solution**:
```sql
-- Test JSON validity
SELECT id FROM modules WHERE config IS NOT JSON;

-- Fix invalid JSON
UPDATE modules SET config = '{}' WHERE config IS NULL OR config IS NOT JSON;
COMMIT;
```

### Issue 5: Performance issues
**Cause**: Missing statistics or indexes
**Solution**:
```sql
-- Gather statistics
EXEC DBMS_STATS.GATHER_SCHEMA_STATS('TICKET_TRACKER');

-- Check for missing indexes
SELECT * FROM user_indexes WHERE table_name = 'TICKETS';
```

---

## Rollback Procedures

### Option 1: Drop Everything
```sql
-- Drop all tables (CASCADE CONSTRAINTS drops foreign keys)
BEGIN
    FOR t IN (SELECT table_name FROM user_tables) LOOP
        EXECUTE IMMEDIATE 'DROP TABLE ' || t.table_name || ' CASCADE CONSTRAINTS';
    END LOOP;
END;
/

-- Drop sequences
DROP SEQUENCE seq_ticket_number;
DROP SEQUENCE seq_step_number;

-- Drop functions
DROP FUNCTION generate_uuid;

-- Verify cleanup
SELECT COUNT(*) FROM user_tables;
SELECT COUNT(*) FROM user_sequences;
```

### Option 2: Keep Schema, Delete Data
```sql
-- Disable constraints
BEGIN
    FOR c IN (SELECT constraint_name, table_name FROM user_constraints WHERE constraint_type = 'R') LOOP
        EXECUTE IMMEDIATE 'ALTER TABLE ' || c.table_name || ' DISABLE CONSTRAINT ' || c.constraint_name;
    END LOOP;
END;
/

-- Truncate all tables
BEGIN
    FOR t IN (SELECT table_name FROM user_tables) LOOP
        EXECUTE IMMEDIATE 'TRUNCATE TABLE ' || t.table_name;
    END LOOP;
END;
/

-- Re-enable constraints
BEGIN
    FOR c IN (SELECT constraint_name, table_name FROM user_constraints WHERE constraint_type = 'R') LOOP
        EXECUTE IMMEDIATE 'ALTER TABLE ' || c.table_name || ' ENABLE CONSTRAINT ' || c.constraint_name;
    END LOOP;
END;
/
```

### Option 3: Export Before Migration (Backup)
```bash
# Export schema
exp ticket_tracker/password@database file=backup_before_migration.dmp

# Import if needed
imp ticket_tracker/password@database file=backup_before_migration.dmp
```

---

## Quick Reference Commands

### Connect to Database
```bash
sqlplus ticket_tracker/password@database
```

### Run Script File
```sql
@path/to/script.sql
```

### Check Object Counts
```sql
SELECT 'Tables' as object_type, COUNT(*) as count FROM user_tables
UNION ALL
SELECT 'Indexes', COUNT(*) FROM user_indexes
UNION ALL
SELECT 'Triggers', COUNT(*) FROM user_triggers
UNION ALL
SELECT 'Sequences', COUNT(*) FROM user_sequences;
```

### View Errors
```sql
-- View compilation errors
SELECT * FROM user_errors;

-- View invalid objects
SELECT object_name, object_type, status
FROM user_objects
WHERE status = 'INVALID';
```

---

## Support and Additional Resources

### Oracle Documentation
- Oracle Database SQL Language Reference
- Oracle Database Administrator's Guide
- Oracle Database PL/SQL Language Reference

### Useful Queries
```sql
-- Find table size
SELECT segment_name, bytes/1024/1024 AS size_mb
FROM user_segments
WHERE segment_type = 'TABLE'
ORDER BY bytes DESC;

-- Find index size
SELECT segment_name, bytes/1024/1024 AS size_mb
FROM user_segments
WHERE segment_type = 'INDEX'
ORDER BY bytes DESC;

-- Check tablespace usage
SELECT tablespace_name,
       SUM(bytes)/1024/1024 AS total_mb,
       SUM(CASE WHEN autoextensible = 'YES' THEN maxbytes ELSE bytes END)/1024/1024 AS max_mb
FROM dba_data_files
GROUP BY tablespace_name;
```

---

## Migration Checklist

- [ ] Oracle Database 11g+ installed and running
- [ ] Schema/user created with proper privileges
- [ ] Sequences and functions created successfully
- [ ] All 20 tables created without errors
- [ ] All 100+ indexes created successfully
- [ ] All 40+ triggers created and enabled
- [ ] Initial data seeded (users, modules, field definitions)
- [ ] Foreign key constraints verified
- [ ] Check constraints verified
- [ ] Sample data inserted successfully
- [ ] Query performance tested
- [ ] Statistics gathered
- [ ] Backup created before going live
- [ ] Application configuration updated with Oracle connection
- [ ] Connection pool configured
- [ ] Application tested end-to-end

---

**Document Version**: 1.0
**Last Updated**: 2025-11-22
**Author**: Ticket Tracker Migration Team
