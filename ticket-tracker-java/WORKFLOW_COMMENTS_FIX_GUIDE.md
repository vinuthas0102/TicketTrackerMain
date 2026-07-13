# Workflow Comments Fix Guide

## Problem Summary

**Issue:** Users cannot see their workflow step comments, including their own comments immediately after adding them.

**Root Cause:** The Oracle database table `workflow_comments` is missing the `updated_at` column, but the Java application code expects this column to exist for both INSERT and SELECT operations.

## Symptoms

- User adds a comment to a workflow step
- Frontend reports success (201 Created)
- Comment does not appear in the UI for any user
- No error messages visible to the user
- Database INSERT operation fails silently

## Technical Details

### Database Schema Issue

**Current Schema** (02-oracle-schema.sql):
```sql
CREATE TABLE workflow_comments (
  id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
  step_id RAW(16) NOT NULL,
  content CLOB NOT NULL,
  created_by RAW(16) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);
```

**Expected Schema** (after migration):
```sql
CREATE TABLE workflow_comments (
  id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
  step_id RAW(16) NOT NULL,
  content CLOB NOT NULL,
  created_by RAW(16) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL  -- MISSING COLUMN
);
```

### Application Code Dependency

The Java DAO attempts to INSERT into the `updated_at` column:

**File:** `WorkflowCommentDAO.java` (line 12-13)
```java
String sql = "INSERT INTO workflow_comments (id, step_id, content, created_by, created_at, updated_at) " +
        "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
```

When this column doesn't exist in the database:
1. SQLException is thrown: "ORA-00904: invalid identifier"
2. The error is caught and logged but doesn't prevent HTTP 201 response
3. No comment data is actually saved to the database
4. User sees "success" but no comment appears

## Solution

### Step 1: Run Database Migration (CRITICAL)

Execute the migration script that adds the missing `updated_at` column:

```bash
cd ticket-tracker-java/database
sqlplus username/password@database @09-oracle-add-workflow-comments-updated-at.sql
```

**Or run via install.sql:**
```bash
sqlplus username/password@database @install.sql
```

The migration script will:
- Add `updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL` column
- Set `updated_at = created_at` for existing records
- Create an index on `updated_at` for performance
- Handle idempotency (safe to run multiple times)

### Step 2: Verify Database Changes

After running the migration, verify the column was added:

```sql
-- Check column exists
SELECT column_name, data_type, nullable, data_default
FROM USER_TAB_COLUMNS
WHERE table_name = 'WORKFLOW_COMMENTS'
ORDER BY column_id;

-- Expected output should include:
-- UPDATED_AT | TIMESTAMP | N | CURRENT_TIMESTAMP
```

### Step 3: Verify Existing Data

Check if any comments exist and have proper timestamps:

```sql
-- Check existing comments
SELECT id, step_id, created_at, updated_at
FROM workflow_comments
ORDER BY created_at DESC;

-- All records should have both created_at and updated_at populated
```

### Step 4: Test Comment Operations

After applying the migration, test the following:

#### 4.1 Create New Comment
1. Login as a Dept Manager or assigned user
2. Open a workflow step
3. Add a comment: "Test comment after fix"
4. Verify the comment appears immediately in the UI
5. Check server logs for "Created workflow comment for step" message

#### 4.2 View Own Comments
1. Refresh the page
2. Verify your comment persists and is visible
3. Check the timestamp displays correctly

#### 4.3 View Comments as EO
1. Login as EO user
2. Open the same workflow step
3. Verify you can see all comments from all users

#### 4.4 Update Comment
1. Edit your own comment
2. Verify the updated content appears
3. Verify updated_at timestamp changes

#### 4.5 Delete Comment
1. Delete your own comment
2. Verify it disappears from the UI
3. Verify it's removed from the database

### Step 5: Monitor Application Logs

The improved error handling will now log detailed SQL errors:

```log
ERROR - SQL Error creating workflow comment. Error code: 904, SQL state: 42000, Message: ORA-00904: "UPDATED_AT": invalid identifier
ERROR - SQL statement: INSERT INTO workflow_comments (id, step_id, content, created_by, created_at, updated_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ERROR - Full exception: [stack trace]
```

If you see these errors after the migration, the migration was not applied correctly.

## Implementation Changes

### Enhanced Error Logging

The following files were updated to provide better error diagnostics:

**File:** `src/main/java/com/tickettracker/dao/WorkflowCommentDAO.java`

- Added detailed SQL exception logging in `create()` method
- Added detailed SQL exception logging in `update()` method
- Added detailed SQL exception logging in `findByStepId()` method
- Logs SQL error code, SQL state, error message, and full SQL statement

These improvements help identify database schema issues quickly during development and troubleshooting.

## Role-Based Visibility Logic

After fixing the database issue, the comment visibility works as follows:

**For EO Users:**
- Can see ALL comments on any workflow step
- Method: `commentDAO.findByStepId(stepId)`

**For Non-EO Users (Dept Manager, Employee, Vendor, Finance):**
- Can only see their OWN comments
- Method: `commentDAO.findByStepIdAndUser(stepId, userId)`

**Business Requirement Note:** If collaboration requires all users to see all comments, modify line 103-106 in `WorkflowCommentService.java`:

```java
// Current implementation (filtered)
if ("eo".equalsIgnoreCase(user.getRole())) {
    return commentDAO.findByStepId(stepId);
} else {
    return commentDAO.findByStepIdAndUser(stepId, userId);
}

// Alternative implementation (all users see all comments)
return commentDAO.findByStepId(stepId);
```

## Verification Checklist

- [ ] Migration script executed successfully
- [ ] `updated_at` column exists in `workflow_comments` table
- [ ] Existing comments have `updated_at` values populated
- [ ] Index `idx_workflow_comments_updated_at` created
- [ ] New comments can be created without SQL errors
- [ ] Comments are visible to the user who created them
- [ ] Comments persist after page refresh
- [ ] EO users can see all comments
- [ ] Non-EO users see appropriate comments (own or all, depending on business logic)
- [ ] Comment editing works correctly
- [ ] Comment deletion works correctly
- [ ] Application logs show "Created workflow comment" messages
- [ ] No SQL errors in application logs

## Troubleshooting

### Issue: Migration script fails with "column already exists"

**Solution:** This is expected if the migration was run before. The script is idempotent and will skip adding the column if it already exists.

### Issue: Comments still not appearing after migration

**Diagnostic Steps:**
1. Check database column exists:
   ```sql
   SELECT column_name FROM USER_TAB_COLUMNS WHERE table_name = 'WORKFLOW_COMMENTS';
   ```

2. Check application logs for SQL errors

3. Test direct database insert:
   ```sql
   INSERT INTO workflow_comments (id, step_id, content, created_by, created_at, updated_at)
   VALUES (SYS_GUID(), (SELECT id FROM workflow_steps WHERE rownum = 1), 'Test',
           (SELECT id FROM users WHERE rownum = 1), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
   ```

4. Verify application is connected to the correct database

### Issue: "ORA-00904: invalid identifier" in logs

**Cause:** The migration was not applied to the database the application is connected to.

**Solution:**
1. Verify database connection in `database.properties`
2. Run migration on the correct database
3. Restart application server

### Issue: Users still can't see other users' comments

**Cause:** This is by design for non-EO users. Only EO users see all comments.

**Solution:** If business requirements change, modify `WorkflowCommentService.java` line 103-106 to return all comments for all users.

## Files Modified

### Database Schema
- `database/09-oracle-add-workflow-comments-updated-at.sql` - Migration script (MUST BE RUN)

### Java Application
- `src/main/java/com/tickettracker/dao/WorkflowCommentDAO.java` - Enhanced error logging
- `src/main/java/com/tickettracker/service/WorkflowCommentService.java` - Role-based filtering
- `src/main/java/com/tickettracker/servlet/WorkflowCommentServlet.java` - HTTP endpoint handling

### No Frontend Changes Required
The frontend code is correct and requires no modifications.

## Prevention

To prevent similar issues in the future:

1. **Always run all migration scripts** in sequence when setting up a new database
2. **Check migration status** before deploying application updates
3. **Review application logs** for SQL errors during development
4. **Test database schema changes** in development environment first
5. **Document schema dependencies** in DAO classes

## Additional Resources

- Oracle SQL*Plus documentation for running migration scripts
- Migration script location: `ticket-tracker-java/database/09-oracle-add-workflow-comments-updated-at.sql`
- Complete installation guide: `ticket-tracker-java/database/install.sql`
- Database setup documentation: `ticket-tracker-java/docs/DATABASE_SETUP.md`

## Support

If issues persist after following this guide:
1. Check application logs: `logs/ticket-tracker.log`
2. Check database logs: Oracle alert log
3. Verify database connectivity
4. Confirm all migration scripts were executed in order
5. Review servlet logs for HTTP error responses

---

**Last Updated:** 2025-01-22
**Status:** Ready for Implementation
**Priority:** CRITICAL - Blocks all comment functionality
