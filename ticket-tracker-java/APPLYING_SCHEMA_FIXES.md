# Quick Guide: Applying Schema Fixes to Your Oracle Database

## Prerequisites
- Oracle 19c database with ticket_tracker user
- SQL*Plus or SQL Developer access
- Database credentials

## Step-by-Step Instructions

### 1. Verify Current State
First, check if the fixes have already been applied:

```sql
-- Connect to database
sqlplus ticket_tracker/your_password@your_database

-- Check if start_date column exists
DESC workflow_steps;

-- If start_date is listed, the migration has been applied
-- If not listed, proceed with step 2
```

### 2. Apply Database Migration

```sql
-- Run the migration script
@database/08-oracle-add-missing-fields.sql

-- You should see:
-- "Added start_date column to workflow_steps table"
-- "Created index idx_workflow_steps_start_date"
-- "Migration 08 completed successfully!"
```

### 3. Verify Migration Success

```sql
-- Verify column was added
DESC workflow_steps;

-- Verify index was created
SELECT index_name FROM user_indexes WHERE table_name = 'WORKFLOW_STEPS';

-- Should include: IDX_WORKFLOW_STEPS_START_DATE
```

### 4. Check created_by Column
The created_by column should already exist. Verify it:

```sql
-- Check if created_by exists
SELECT column_name, data_type, nullable
FROM user_tab_columns
WHERE table_name = 'WORKFLOW_STEPS'
AND column_name = 'CREATED_BY';

-- If missing, add it manually:
ALTER TABLE workflow_steps ADD created_by RAW(16);
CREATE INDEX idx_workflow_steps_created_by ON workflow_steps(created_by);
ALTER TABLE workflow_steps ADD CONSTRAINT fk_workflow_steps_created_by
  FOREIGN KEY (created_by) REFERENCES users(id);
```

### 5. Compile and Deploy Java Code

```bash
# Navigate to project directory
cd ticket-tracker-java

# Build the project with Maven
mvn clean package

# Or with provided build script
./build.sh
```

### 6. Deploy to Application Server

```bash
# Copy WAR file to Tomcat webapps
cp target/ticket-tracker.war $CATALINA_HOME/webapps/

# Restart Tomcat
$CATALINA_HOME/bin/shutdown.sh
$CATALINA_HOME/bin/startup.sh
```

### 7. Test the Fixes

#### Test 1: Workflow Step Creation
```bash
# Via API or UI, create a new workflow step
# Verify:
# - created_by field is populated
# - start_date can be set
# - No SQL errors in logs
```

#### Test 2: Progress Updates
```bash
# Update progress on a workflow step
# Verify:
# - Progress value maintains decimal precision (e.g., 47.5% not 47%)
# - No rounding errors
```

#### Test 3: File Reference Templates
```bash
# Via API, create a file reference template
# Verify:
# - Template is created successfully
# - Can be retrieved by ID
# - Can be assigned to workflow steps
```

## Troubleshooting

### Issue: "Table or view does not exist"
**Solution:** Make sure you're connected as the ticket_tracker user, not SYSDBA.

### Issue: "Column already exists"
**Solution:** The migration is idempotent. This message means the fix was already applied. Verify with DESC command.

### Issue: "ORA-00001: unique constraint violated"
**Solution:** The created_by column might already exist. Check the table definition.

### Issue: Java compilation errors
**Solution:**
1. Verify all dependencies are in pom.xml
2. Run `mvn clean` before `mvn compile`
3. Check Java version (requires Java 11+)

### Issue: "Cannot find symbol: class BigDecimal"
**Solution:** BigDecimal is in java.math package. Check imports at top of WorkflowStepDAO.java.

## Rollback Instructions

If you need to rollback the changes:

```sql
-- Remove start_date column
ALTER TABLE workflow_steps DROP COLUMN start_date;

-- Remove index
DROP INDEX idx_workflow_steps_start_date;

-- Note: Only rollback if absolutely necessary
-- This will lose any start_date data that was entered
```

## Validation Queries

Run these queries to validate the fixes:

```sql
-- 1. Count workflow steps with created_by
SELECT COUNT(*) as steps_with_creator
FROM workflow_steps
WHERE created_by IS NOT NULL;

-- 2. Count workflow steps with start_date
SELECT COUNT(*) as steps_with_start_date
FROM workflow_steps
WHERE start_date IS NOT NULL;

-- 3. Check progress precision
SELECT step_number, progress
FROM workflow_steps
WHERE progress > 0
AND ROWNUM <= 10;

-- 4. Verify file reference templates
SELECT COUNT(*) as template_count
FROM file_reference_templates;

-- 5. Verify file references
SELECT COUNT(*) as reference_count
FROM workflow_step_file_references;
```

## Post-Deployment Checks

After deploying to production:

1. **Monitor Logs** - Check application logs for any SQL errors
2. **Test UI** - Verify created_by displays in workflow step details
3. **Test Workflow** - Create a new ticket with workflow steps
4. **Test Progress** - Update progress and verify precision
5. **Test File References** - Upload file reference templates

## Getting Help

If you encounter issues:
1. Check application logs in `logs/ticket-tracker.log`
2. Check Tomcat logs in `$CATALINA_HOME/logs/catalina.out`
3. Verify database connectivity
4. Review SCHEMA_FIX_SUMMARY.md for detailed changes

## Success Criteria

The fixes are successfully applied when:
- ✅ DESC workflow_steps shows start_date column
- ✅ DESC workflow_steps shows created_by column
- ✅ Java application compiles without errors
- ✅ Workflow steps can be created with created_by populated
- ✅ Progress updates maintain decimal precision
- ✅ File reference templates can be created and managed
- ✅ No SQL errors in application logs

## Maintenance

After applying these fixes:
- Run the validation queries weekly for the first month
- Monitor progress values to ensure precision is maintained
- Keep the migration script (08-oracle-add-missing-fields.sql) for future database refreshes
- Document any additional schema changes in new migration scripts
