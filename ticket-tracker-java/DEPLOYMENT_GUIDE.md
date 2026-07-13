# Deployment Guide - Document Upload Fix

## Overview
This guide provides step-by-step instructions to deploy the document upload fix to both `ticket-tracker-java` and `ticket-tracker-rest` packages.

## Prerequisites

- Oracle Database access with DBA privileges
- Java Development Kit (JDK) 8 or higher
- Apache Maven 3.6+
- Application server (Tomcat 8+, WebLogic, etc.)
- Backup of current production environment

## Pre-Deployment Checklist

- [ ] Database backup completed
- [ ] Application server access verified
- [ ] Maven and JDK versions confirmed
- [ ] Review all code changes
- [ ] Staging environment available for testing
- [ ] Rollback plan prepared

## Step 1: Database Migration

### 1.1 Verify Current State

```sql
-- Connect to Oracle database
sqlplus username/password@database

-- Check if file_content column already exists
SELECT column_name, data_type, nullable
FROM user_tab_columns
WHERE table_name = 'DOCUMENTS'
AND column_name = 'FILE_CONTENT';

-- If no rows returned, the migration needs to be applied
```

### 1.2 Backup Documents Table

```sql
-- Create backup table
CREATE TABLE documents_backup_20260202 AS SELECT * FROM documents;

-- Verify backup
SELECT COUNT(*) FROM documents;
SELECT COUNT(*) FROM documents_backup_20260202;
-- Both counts should match
```

### 1.3 Apply Database Migration

```bash
# Navigate to database migration directory
cd /path/to/ticket-tracker-java/database

# Apply migration script
sqlplus username/password@database @11-oracle-add-documents-file-content.sql

# Verify migration was successful
sqlplus username/password@database << EOF
SELECT column_name, data_type
FROM user_tab_columns
WHERE table_name = 'DOCUMENTS'
AND column_name = 'FILE_CONTENT';
EXIT;
EOF
```

Expected output:
```
COLUMN_NAME  DATA_TYPE
FILE_CONTENT BLOB
```

### 1.4 Verify Index Creation

```sql
-- Check if index was created
SELECT index_name, table_name, column_name
FROM user_ind_columns
WHERE table_name = 'DOCUMENTS'
AND index_name = 'IDX_DOCUMENTS_HAS_CONTENT';

-- Should return one row confirming the index exists
```

## Step 2: Build Updated Application

### 2.1 For ticket-tracker-java Package

```bash
# Navigate to project directory
cd /path/to/ticket-tracker-java

# Ensure you have the latest code changes
# - DocumentDAO.java (updated CREATE method)
# - DocumentService.java (added file size validation)

# Build the project
./build.sh --skip-tests

# Verify WAR file was created
ls -lh target/ticket-tracker.war

# Expected output: WAR file with timestamp showing recent creation
```

### 2.2 For ticket-tracker-rest Package (if applicable)

Since `ticket-tracker-rest` uses the same backend (ticket-tracker-java), the same WAR file applies. However, if you have separate frontend builds:

```bash
# Navigate to frontend directory
cd /path/to/ticket-tracker-rest/frontend

# Install dependencies (if not already done)
npm install

# Build frontend
npm run build

# Verify build output
ls -lh dist/

# Expected output: Compiled frontend assets in dist/ directory
```

## Step 3: Deploy to Staging Environment

### 3.1 Stop Application Server

```bash
# For Tomcat
$TOMCAT_HOME/bin/shutdown.sh

# For WebLogic
$DOMAIN_HOME/bin/stopWebLogic.sh

# Wait for complete shutdown (check logs)
tail -f $SERVER_HOME/logs/catalina.out
```

### 3.2 Backup Current Deployment

```bash
# Create backup directory with timestamp
BACKUP_DIR=/path/to/backups/$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR

# For Tomcat
cp $TOMCAT_HOME/webapps/ticket-tracker.war $BACKUP_DIR/
cp -r $TOMCAT_HOME/webapps/ticket-tracker/ $BACKUP_DIR/

# Verify backup
ls -lh $BACKUP_DIR/
```

### 3.3 Deploy New WAR File

```bash
# Remove old deployment
rm -rf $TOMCAT_HOME/webapps/ticket-tracker.war
rm -rf $TOMCAT_HOME/webapps/ticket-tracker/

# Copy new WAR file
cp /path/to/ticket-tracker-java/target/ticket-tracker.war $TOMCAT_HOME/webapps/

# Set proper permissions
chmod 644 $TOMCAT_HOME/webapps/ticket-tracker.war
```

### 3.4 Start Application Server

```bash
# For Tomcat
$TOMCAT_HOME/bin/startup.sh

# Monitor startup logs
tail -f $TOMCAT_HOME/logs/catalina.out

# Look for these log entries:
# - "Server startup in [XXXX] ms"
# - "DocumentDAO" initialization messages
# - No ERROR or EXCEPTION entries
```

## Step 4: Verification Testing

### 4.1 Smoke Tests

```bash
# Test 1: Check application is accessible
curl -I http://localhost:8080/ticket-tracker-java/

# Expected: HTTP/1.1 200 OK or 302 Found (redirect to login)

# Test 2: Check API health endpoint (if available)
curl http://localhost:8080/ticket-tracker-java/api/modules

# Expected: JSON response with module list
```

### 4.2 Document Upload Test

**Manual Testing Steps:**

1. **Login to Application**
   - Navigate to: http://localhost:8080/ticket-tracker-java/
   - Login with test credentials
   - Select a module

2. **Create Ticket with Attachment**
   - Click "Create Ticket" button
   - Fill in required fields:
     - Title: "Test Document Upload Fix"
     - Description: "Testing file attachment functionality"
     - Priority: Medium
     - Category: General
   - Click "Choose File" and select a test PDF (< 5MB)
   - Click "Create" or "Submit"
   - Wait for success message

3. **Verify Document is Saved**
   - Open the newly created ticket
   - Check that attached document appears in the attachments list
   - Verify document name, size, and type are displayed correctly

4. **Test Document Download**
   - Click the download icon/link for the attached document
   - Verify file downloads successfully
   - Open the downloaded file
   - Confirm file is not corrupted and matches original

### 4.3 Database Verification

```sql
-- Connect to database
sqlplus username/password@database

-- Check recent uploads
SELECT id, name, file_size,
       CASE WHEN file_content IS NOT NULL THEN 'YES' ELSE 'NO' END as has_content,
       DBMS_LOB.GETLENGTH(file_content) as actual_content_size,
       uploaded_at
FROM documents
WHERE uploaded_at > SYSDATE - 1/24  -- Last hour
ORDER BY uploaded_at DESC;

-- Verify file content size matches
SELECT id, name, file_size,
       DBMS_LOB.GETLENGTH(file_content) as actual_size,
       CASE
         WHEN file_size = DBMS_LOB.GETLENGTH(file_content) THEN 'MATCH'
         WHEN file_content IS NULL THEN 'NO CONTENT'
         ELSE 'MISMATCH'
       END as size_check
FROM documents
WHERE uploaded_at > SYSDATE - 1/24
ORDER BY uploaded_at DESC;
```

Expected Results:
- `has_content` should be 'YES' for newly uploaded documents
- `actual_content_size` should match `file_size`
- `size_check` should show 'MATCH'

### 4.4 Application Logs Review

```bash
# Check for successful document creation
grep "Created document.*with file content" $TOMCAT_HOME/logs/catalina.out

# Expected output example:
# INFO  [DocumentDAO] Created document: test.pdf with file content: 245678 bytes (rows affected: 1)

# Check for any errors
grep -i "error\|exception" $TOMCAT_HOME/logs/catalina.out | tail -20

# No errors related to document upload should appear
```

## Step 5: Regression Testing

Test existing functionality to ensure no breaking changes:

- [ ] Login/Logout functionality
- [ ] Ticket creation without attachments
- [ ] Ticket editing
- [ ] Ticket search and filtering
- [ ] Workflow step management
- [ ] User management
- [ ] Audit trail viewing
- [ ] Existing tickets with no attachments still display correctly

## Step 6: Production Deployment

### 6.1 Production Deployment Schedule

**Recommended Schedule:**
- Deploy during maintenance window
- Notify users of planned downtime
- Have support team on standby
- Schedule rollback window if needed

### 6.2 Production Deployment Steps

```bash
# 1. Create production backup
PROD_BACKUP_DIR=/path/to/prod_backups/$(date +%Y%m%d_%H%M%S)
mkdir -p $PROD_BACKUP_DIR

# 2. Backup database (CRITICAL)
expdp username/password@prod_database \
  tables=DOCUMENTS,DOCUMENTS_BACKUP \
  directory=BACKUP_DIR \
  dumpfile=documents_backup_$(date +%Y%m%d_%H%M%S).dmp \
  logfile=documents_backup.log

# 3. Backup current WAR file
cp $PROD_TOMCAT_HOME/webapps/ticket-tracker.war $PROD_BACKUP_DIR/

# 4. Notify users (send notification)
echo "System maintenance in progress. Expected downtime: 15 minutes"

# 5. Stop production server
$PROD_TOMCAT_HOME/bin/shutdown.sh
sleep 30

# 6. Apply database migration (if not already applied)
sqlplus prod_user/prod_password@prod_database @11-oracle-add-documents-file-content.sql

# 7. Deploy new WAR file
rm -rf $PROD_TOMCAT_HOME/webapps/ticket-tracker.war
rm -rf $PROD_TOMCAT_HOME/webapps/ticket-tracker/
cp /path/to/ticket-tracker-java/target/ticket-tracker.war $PROD_TOMCAT_HOME/webapps/

# 8. Start production server
$PROD_TOMCAT_HOME/bin/startup.sh

# 9. Monitor startup
tail -f $PROD_TOMCAT_HOME/logs/catalina.out

# 10. Run smoke tests (wait 2-3 minutes for complete startup)
curl -I http://production-server:8080/ticket-tracker-java/
```

### 6.3 Post-Deployment Verification

```bash
# 1. Check application is running
curl http://production-server:8080/ticket-tracker-java/api/modules

# 2. Test file upload (use test account)
# Upload a small test file through UI

# 3. Verify in database
sqlplus prod_user/prod_password@prod_database << EOF
SELECT COUNT(*) as docs_with_content
FROM documents
WHERE file_content IS NOT NULL
AND uploaded_at > SYSDATE - 1/24;
EXIT;
EOF

# 4. Monitor logs for 15-30 minutes
tail -f $PROD_TOMCAT_HOME/logs/catalina.out | grep -i "document\|error\|exception"

# 5. Notify users deployment is complete
echo "System maintenance completed. Application is now available."
```

## Step 7: Monitoring Plan

### 7.1 Short-term Monitoring (First 24 Hours)

- [ ] Monitor application logs every 2 hours
- [ ] Check database tablespace usage
- [ ] Verify file uploads are working via user feedback
- [ ] Monitor application performance metrics
- [ ] Check error rates in logs

### 7.2 Long-term Monitoring (First Week)

- [ ] Daily log review for document-related errors
- [ ] Monitor database BLOB storage growth
- [ ] Track file upload success/failure rates
- [ ] Review user feedback and support tickets
- [ ] Verify backup procedures include new BLOB data

### 7.3 Monitoring Queries

```sql
-- Daily file upload statistics
SELECT TRUNC(uploaded_at) as upload_date,
       COUNT(*) as total_uploads,
       SUM(file_size) as total_size_bytes,
       ROUND(AVG(file_size)/1024/1024, 2) as avg_size_mb,
       MAX(file_size)/1024/1024 as max_size_mb
FROM documents
WHERE uploaded_at > SYSDATE - 7
GROUP BY TRUNC(uploaded_at)
ORDER BY upload_date DESC;

-- Check for failed uploads (no file_content)
SELECT COUNT(*) as no_content_count
FROM documents
WHERE uploaded_at > SYSDATE - 1
AND file_content IS NULL;

-- Database space usage for documents table
SELECT segment_name, segment_type,
       ROUND(bytes/1024/1024, 2) as size_mb
FROM user_segments
WHERE segment_name = 'DOCUMENTS';
```

## Step 8: Rollback Plan

If issues are detected after deployment:

### 8.1 Application Rollback

```bash
# 1. Stop application server
$TOMCAT_HOME/bin/shutdown.sh

# 2. Remove new deployment
rm -rf $TOMCAT_HOME/webapps/ticket-tracker.war
rm -rf $TOMCAT_HOME/webapps/ticket-tracker/

# 3. Restore previous version
cp $BACKUP_DIR/ticket-tracker.war $TOMCAT_HOME/webapps/

# 4. Start application server
$TOMCAT_HOME/bin/startup.sh

# 5. Monitor logs
tail -f $TOMCAT_HOME/logs/catalina.out
```

### 8.2 Database Rollback

```sql
-- WARNING: This will lose any documents uploaded after deployment
-- Only do this if absolutely necessary

-- Connect to database
sqlplus username/password@database

-- Drop the file_content column
ALTER TABLE documents DROP COLUMN file_content;

-- Verify column is dropped
SELECT column_name FROM user_tab_columns
WHERE table_name = 'DOCUMENTS'
AND column_name = 'FILE_CONTENT';
-- Should return no rows

-- Drop the index
DROP INDEX idx_documents_has_content;

COMMIT;
```

**Note**: Database rollback should be a last resort. If only application issues occur, rolling back just the application is preferable.

## Troubleshooting Common Issues

### Issue 1: "Column not found: file_content"

**Symptom**: Error in logs indicating file_content column doesn't exist

**Solution**:
```sql
-- Verify migration was applied
SELECT column_name FROM user_tab_columns
WHERE table_name = 'DOCUMENTS' AND column_name = 'FILE_CONTENT';

-- If no rows, apply migration:
sqlplus username/password@database @11-oracle-add-documents-file-content.sql
```

### Issue 2: "File too large" errors

**Symptom**: Users cannot upload files, receive "File size exceeds limit" error

**Solution**:
- Verify file is actually under 5MB
- Check FileServlet maxFileSize configuration
- Review DocumentService validation logic
- Adjust limits if business requirements changed

### Issue 3: Downloaded files are corrupted

**Symptom**: Files download but cannot be opened

**Solution**:
```java
// Verify Content-Type and headers in FileServlet.handleGetDocument()
// Should be:
response.setContentType("application/octet-stream");
response.setHeader("Content-Disposition", "attachment; filename=\"" + document.getName() + "\"");
```

### Issue 4: Slow application performance

**Symptom**: Application becomes slow after many file uploads

**Solution**:
- Monitor database tablespace
- Review query performance
- Consider archiving old documents
- Implement lazy loading for file content
- Add database partitioning for documents table

## Support and Contacts

**Technical Support**:
- Application Logs: `$TOMCAT_HOME/logs/catalina.out`
- Database Logs: Check Oracle alert log
- Issue Tracking: Log issues with deployment date/time

**Emergency Contacts**:
- DBA Team: For database issues
- Infrastructure Team: For server/network issues
- Development Team: For application code issues

## Sign-off

**Deployed By**: ___________________ Date: ___________

**Verified By**: ___________________ Date: ___________

**Approved By**: ___________________ Date: ___________

---

**Document Version**: 1.0
**Last Updated**: 2026-02-02
**Next Review Date**: 2026-03-02
