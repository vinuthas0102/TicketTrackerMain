# Oracle Database Setup Guide

Complete guide for setting up Oracle Database 19c for the Ticket Tracker application.

## Quick Start

```bash
# 1. Connect as SYSDBA
sqlplus sys/password@ORCL as sysdba

# 2. Create user
@database/01-oracle-create-user.sql

# 3. Connect as ticket_tracker user
sqlplus ticket_tracker/ticket_pass_2024@ORCL

# 4. Run all migrations
@database/install.sql
```

## Detailed Setup Instructions

### Prerequisites

- Oracle Database 19c installed and running
- SQL*Plus or SQL Developer installed
- SYSDBA credentials
- Network connectivity to Oracle database

### Step-by-Step Installation

#### 1. Create Database User and Schema

The first script creates the application user with necessary privileges.

**File**: `database/01-oracle-create-user.sql`

**What it does**:
- Creates user `ticket_tracker` with password `ticket_pass_2024`
- Grants CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, etc.
- Sets up quotas on USERS tablespace

**Run as SYSDBA**:
```sql
sqlplus sys/your_password@ORCL as sysdba
@database/01-oracle-create-user.sql
```

**Security Note**: Change the default password immediately after installation!

#### 2. Verify User Creation

```sql
SELECT username, default_tablespace, account_status
FROM dba_users
WHERE username = 'TICKET_TRACKER';
```

Expected output:
```
USERNAME       DEFAULT_TABLESPACE  ACCOUNT_STATUS
-------------- ------------------- --------------
TICKET_TRACKER USERS               OPEN
```

#### 3. Run Master Installation Script

The master script executes all migration files in the correct order.

**Connect as ticket_tracker user**:
```sql
sqlplus ticket_tracker/ticket_pass_2024@ORCL
```

**Run installation**:
```sql
@database/install.sql
```

**What it does**:
1. Creates all 20 tables with proper data types
2. Creates sequences for auto-increment fields
3. Creates 18 triggers for automatic updates
4. Creates 70+ indexes for performance
5. Adds 37 foreign key constraints
6. Loads seed data (5 modules, 9+ users, configurations)

**Estimated time**: 2-3 minutes

#### 4. Verify Installation

**Check table count**:
```sql
SELECT COUNT(*) as table_count FROM user_tables;
```
Expected: 20 tables

**Check specific tables**:
```sql
SELECT table_name FROM user_tables ORDER BY table_name;
```

Expected tables:
- AUDIT_LOGS
- DOCUMENTS
- FIELD_DEFINITIONS
- FIELD_DROPDOWN_OPTIONS
- FILE_ATTACHMENTS
- FILE_REFERENCE_TEMPLATES
- FILE_UPLOAD_CONFIG
- FINANCE_APPROVALS
- MODULE_FIELD_CONFIGURATIONS
- MODULES
- TICKET_FIELD_VALUES
- TICKETS
- USER_DISPLAY_PREFERENCES
- USERS
- WORKFLOW_COMMENTS
- WORKFLOW_STEP_DEPENDENCIES
- WORKFLOW_STEP_FIELD_VALUES
- WORKFLOW_STEP_FILE_REFERENCES
- WORKFLOW_STEP_PROGRESS_DOCUMENTS
- WORKFLOW_STEPS

**Check seed data**:
```sql
-- Modules (should be 5)
SELECT id, name, schema_id, active FROM modules;

-- Users (should be 9+)
SELECT name, email, role, department FROM users;

-- Field definitions
SELECT COUNT(*) FROM field_definitions;

-- Module configurations
SELECT COUNT(*) FROM module_field_configurations;
```

## Database Schema Overview

### Core Tables

**USERS**
- Stores user accounts with role-based access control
- Roles: employee, eo, dept_officer, vendor, finance
- Passwords hashed with SHA-256 + salt

**MODULES**
- Workflow modules (Maintenance, Complaints, RTI, etc.)
- Each module has its own configuration and field definitions

**TICKETS**
- Main workflow instances
- Links to modules, users, and contains dynamic data

**WORKFLOW_STEPS**
- Hierarchical steps within tickets
- Supports dependencies and parallel execution
- Progress tracking with percentage completion

**DOCUMENTS**
- File attachments for tickets and steps
- Supports mandatory documents and completion certificates

**AUDIT_LOGS**
- Complete audit trail of all system actions
- Categorized by action type
- Includes old/new data for changes

### Dynamic Field System

**FIELD_DEFINITIONS**
- Global field type definitions
- Supports text, number, date, dropdown, file upload, etc.

**MODULE_FIELD_CONFIGURATIONS**
- Module-specific field configurations
- Role visibility and validation rules
- Applies to both tickets and workflow steps

**FIELD_DROPDOWN_OPTIONS**
- Options for dropdown and multi-select fields

**TICKET_FIELD_VALUES** / **WORKFLOW_STEP_FIELD_VALUES**
- Stores dynamic field values as key-value pairs

### Finance Approval System

**FINANCE_APPROVALS**
- Finance approval requests for tickets
- Tracks tentative costs and approval decisions
- Links to finance officers

**TICKETS** (finance columns)
- finance_officer_id
- finance_submission_count
- latest_finance_status
- requires_finance_approval

### File Management

**FILE_UPLOAD_CONFIG**
- Database-driven file upload configuration
- Per-field rules for size, extensions, storage paths

**FILE_REFERENCE_TEMPLATES**
- JSON-based templates for required documents
- Used by workflow steps

**WORKFLOW_STEP_FILE_REFERENCES**
- Tracks upload status of required documents

**WORKFLOW_STEP_PROGRESS_DOCUMENTS**
- Progress tracking documents with soft delete support

## Data Type Conversions

PostgreSQL → Oracle mappings used:

| PostgreSQL | Oracle |
|-----------|--------|
| UUID | RAW(16) with SYS_GUID() |
| TEXT | VARCHAR2(4000) or CLOB |
| BOOLEAN | NUMBER(1) with CHECK (0,1) |
| TIMESTAMPTZ | TIMESTAMP |
| JSONB | CLOB with IS JSON CHECK |
| SERIAL | NUMBER + SEQUENCE |
| TEXT[] | CLOB (comma-separated) |

## Database Maintenance

### Backup and Recovery

**Export schema**:
```bash
exp ticket_tracker/password file=ticket_tracker_backup.dmp owner=ticket_tracker
```

**Import schema**:
```bash
imp ticket_tracker/password file=ticket_tracker_backup.dmp full=y
```

**Using Data Pump (preferred)**:
```bash
# Export
expdp ticket_tracker/password directory=DATA_PUMP_DIR dumpfile=ticket_tracker.dmp schemas=ticket_tracker

# Import
impdp ticket_tracker/password directory=DATA_PUMP_DIR dumpfile=ticket_tracker.dmp schemas=ticket_tracker
```

### Reset Database

To completely remove and reinstall:

```sql
-- Connect as ticket_tracker
sqlplus ticket_tracker/password@ORCL

-- Run cleanup
@database/99-oracle-cleanup.sql

-- Run installation again
@database/install.sql
```

**WARNING**: This deletes ALL data!

### Performance Tuning

**Analyze tables**:
```sql
BEGIN
  DBMS_STATS.GATHER_SCHEMA_STATS('TICKET_TRACKER');
END;
/
```

**Check index usage**:
```sql
SELECT index_name, table_name, uniqueness
FROM user_indexes
ORDER BY table_name;
```

**Monitor table sizes**:
```sql
SELECT segment_name, bytes/1024/1024 as size_mb
FROM user_segments
WHERE segment_type = 'TABLE'
ORDER BY bytes DESC;
```

## Troubleshooting

### Cannot Connect

**Check Oracle listener**:
```bash
lsnrctl status
```

**Check database status**:
```bash
sqlplus / as sysdba
SELECT status FROM v$instance;
```

### User Creation Fails

**Drop and recreate**:
```sql
-- As SYSDBA
DROP USER ticket_tracker CASCADE;
@database/01-oracle-create-user.sql
```

### Installation Errors

**Check logs during installation**:
- SQL*Plus shows all errors and warnings
- Look for "ORA-" error codes

**Common issues**:
1. Insufficient privileges → Rerun 01-oracle-create-user.sql
2. Duplicate objects → Run 99-oracle-cleanup.sql first
3. Invalid JSON → Check CLOB data in seed scripts

### Connection Pool Issues

**Check active connections**:
```sql
SELECT username, program, machine, COUNT(*)
FROM v$session
WHERE username = 'TICKET_TRACKER'
GROUP BY username, program, machine;
```

**Kill hung sessions**:
```sql
-- Find SID and SERIAL#
SELECT sid, serial#, status FROM v$session WHERE username = 'TICKET_TRACKER';

-- Kill session
ALTER SYSTEM KILL SESSION 'sid,serial#';
```

## Security Best Practices

### Change Default Passwords

```sql
-- Change database user password
ALTER USER ticket_tracker IDENTIFIED BY new_secure_password;
```

Update `database.properties`:
```properties
db.password=new_secure_password
```

### Restrict Network Access

Configure `sqlnet.ora`:
```
TCP.VALIDNODE_CHECKING = YES
TCP.INVITED_NODES = (192.168.1.100)
```

### Enable Auditing

```sql
AUDIT ALL ON ticket_tracker.users;
AUDIT ALL ON ticket_tracker.finance_approvals;
```

### Regular Password Rotation

```sql
-- Set password expiry (90 days)
ALTER PROFILE DEFAULT LIMIT PASSWORD_LIFE_TIME 90;
```

## Advanced Configuration

### Connection Pooling

Edit `database.properties`:
```properties
db.pool.initial=10
db.pool.max=50
db.pool.idle=20
db.pool.maxWaitMillis=5000
```

### Query Optimization

Create additional indexes for common queries:
```sql
CREATE INDEX idx_tickets_created_status
ON tickets(created_at DESC, status);

CREATE INDEX idx_audit_logs_ticket_performed
ON audit_logs(ticket_id, performed_at DESC);
```

### Partitioning Large Tables

For large deployments, consider partitioning:
```sql
-- Example: Partition audit_logs by month
ALTER TABLE audit_logs
MODIFY PARTITION BY RANGE (performed_at)
INTERVAL (NUMTOYMINTERVAL(1, 'MONTH'))
(PARTITION p_initial VALUES LESS THAN (TIMESTAMP '2024-01-01 00:00:00'));
```

## Next Steps

- [Configure application](INSTALLATION.md#application-configuration)
- [Deploy to Tomcat](INSTALLATION.md#deployment)
- [Set up monitoring](MONITORING.md)
- [Review API endpoints](API_DOCUMENTATION.md)
