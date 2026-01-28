# Workflow Comments Quick Fix

## Immediate Action Required

Users cannot see workflow comments because the Oracle database is missing the `updated_at` column in the `workflow_comments` table.

## Fix in 3 Steps

### Step 1: Connect to Oracle Database

```bash
sqlplus your_username/your_password@your_database
```

### Step 2: Run Migration Script

**Option A - Run single migration:**
```bash
cd ticket-tracker-java/database
@09-oracle-add-workflow-comments-updated-at.sql
```

**Option B - Run complete installation (if database is new):**
```bash
cd ticket-tracker-java/database
@install.sql
```

### Step 3: Verify Fix

```sql
-- Check column exists
DESC workflow_comments;

-- Should show:
-- Name           Null?    Type
-- -------------- -------- -------------
-- ID             NOT NULL RAW(16)
-- STEP_ID        NOT NULL RAW(16)
-- CONTENT        NOT NULL CLOB
-- CREATED_BY     NOT NULL RAW(16)
-- CREATED_AT     NOT NULL TIMESTAMP
-- UPDATED_AT     NOT NULL TIMESTAMP    <-- This should now exist
```

### Step 4: Test

1. Restart your Java application server (if needed)
2. Login to the application
3. Open any workflow step
4. Add a comment
5. Verify the comment appears immediately
6. Refresh the page
7. Verify the comment persists

## What This Fixes

- ✅ Comments can now be saved to database
- ✅ Users can see their own comments immediately
- ✅ Comments persist after page refresh
- ✅ EO users can see all comments
- ✅ No more silent failures

## If You Still Have Issues

Check the application logs for SQL errors:
```bash
tail -f logs/ticket-tracker.log
```

Look for these log messages:
- **Success:** `Created workflow comment for step (rows affected: 1)`
- **Success:** `Retrieved N comments for step`
- **Error:** `SQL Error creating workflow comment. Error code: 904`

If you see error code 904, the migration was not applied correctly. Verify you're connected to the correct database.

## Migration Script Details

**File:** `database/09-oracle-add-workflow-comments-updated-at.sql`

**What it does:**
- Adds `updated_at TIMESTAMP` column to `workflow_comments` table
- Sets default value to `CURRENT_TIMESTAMP`
- Updates existing records with `updated_at = created_at`
- Creates performance index on `updated_at`
- Safe to run multiple times (idempotent)

## Support

For detailed information, see: `WORKFLOW_COMMENTS_FIX_GUIDE.md`
