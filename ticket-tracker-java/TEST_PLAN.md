# Test Plan - Document Upload Fix

## Test Environment Setup

### Prerequisites
- [ ] Database migration 11 applied successfully
- [ ] Application deployed with updated code
- [ ] Test user accounts available
- [ ] Test files prepared (various sizes and types)
- [ ] Database access for verification queries

### Test Data Preparation

Create test files in a test directory:

```bash
# Create test directory
mkdir -p /tmp/test-files

# Create small text file (< 1KB)
echo "This is a test document" > /tmp/test-files/small-text.txt

# Create medium PDF (~500KB - you need to provide your own)
# cp your-test-500kb.pdf /tmp/test-files/medium-doc.pdf

# Create large PDF (~4.5MB - you need to provide your own)
# cp your-test-4.5mb.pdf /tmp/test-files/large-doc.pdf

# Create oversized file (> 5MB - you need to provide your own)
# cp your-test-6mb.pdf /tmp/test-files/oversized-doc.pdf
```

### Test Users

| Username | Role | Department | Purpose |
|----------|------|------------|---------|
| admin | Admin | IT | Full access testing |
| do_manager | DO Manager | DO | Role-based testing |
| vendor_user | Vendor | External | Limited access testing |

## Test Cases

### TC-001: Ticket Creation Without Attachments (Baseline)

**Objective**: Verify existing functionality still works

**Prerequisites**:
- Logged in as any user
- Module selected

**Test Steps**:
1. Click "Create Ticket" button
2. Fill in required fields:
   - Title: "Test Ticket Without Attachments"
   - Description: "Baseline test"
   - Priority: Medium
   - Category: General
3. Do NOT attach any files
4. Click "Create" or "Submit"

**Expected Results**:
- ✅ Ticket created successfully
- ✅ Success message displayed
- ✅ Ticket appears in ticket list
- ✅ No errors in logs

**SQL Verification**:
```sql
SELECT id, ticket_number, title, status
FROM tickets
WHERE title = 'Test Ticket Without Attachments'
ORDER BY created_at DESC;
```

**Status**: [ ] PASS [ ] FAIL

**Notes**: _________________________________

---

### TC-002: Ticket Creation With Single Small PDF Attachment

**Objective**: Verify basic file upload functionality

**Prerequisites**:
- Logged in as any user
- Small PDF file ready (< 1MB)

**Test Steps**:
1. Click "Create Ticket" button
2. Fill in required fields:
   - Title: "Test Ticket With Small PDF"
   - Description: "Testing small file upload"
   - Priority: Medium
   - Category: General
3. Click "Choose File" or "Upload" button
4. Select small PDF file (< 1MB)
5. Verify file name appears in UI
6. Click "Create" or "Submit"
7. Wait for upload completion

**Expected Results**:
- ✅ Ticket created successfully
- ✅ Upload progress shown (if implemented)
- ✅ Success message: "Successfully uploaded 1 file"
- ✅ Ticket view shows attached document
- ✅ Document metadata displayed (name, size, type)
- ✅ No errors in logs

**SQL Verification**:
```sql
-- Get the ticket ID first
SELECT id, ticket_number FROM tickets
WHERE title = 'Test Ticket With Small PDF'
ORDER BY created_at DESC;

-- Then check documents (replace TICKET_ID with actual value)
SELECT id, name, type, file_size,
       CASE WHEN file_content IS NOT NULL THEN 'YES' ELSE 'NO' END as has_content,
       DBMS_LOB.GETLENGTH(file_content) as actual_content_size,
       uploaded_at
FROM documents
WHERE ticket_id = HEXTORAW('TICKET_ID_IN_HEX')
ORDER BY uploaded_at DESC;
```

**Expected SQL Results**:
- has_content = 'YES'
- actual_content_size matches file_size
- name matches uploaded file name

**Status**: [ ] PASS [ ] FAIL

**Notes**: _________________________________

---

### TC-003: Download and Verify Attached Document

**Objective**: Verify file download works correctly

**Prerequisites**:
- TC-002 passed
- Ticket with attached PDF available

**Test Steps**:
1. Open ticket created in TC-002
2. Locate attached document in attachments section
3. Click download icon/link
4. Wait for file download
5. Open downloaded file in PDF viewer

**Expected Results**:
- ✅ File downloads successfully
- ✅ Downloaded filename matches original
- ✅ File opens without errors
- ✅ File content matches original (visually verify)
- ✅ File size matches original

**Verification**:
```bash
# Compare checksums (if you have original file)
md5sum /tmp/test-files/original.pdf
md5sum ~/Downloads/downloaded.pdf
# Checksums should match
```

**Status**: [ ] PASS [ ] FAIL

**Notes**: _________________________________

---

### TC-004: Ticket Creation With Multiple Attachments

**Objective**: Verify multiple file upload functionality

**Prerequisites**:
- Logged in as any user
- Multiple test files ready (PDF, JPG, DOCX)

**Test Steps**:
1. Click "Create Ticket" button
2. Fill in required fields:
   - Title: "Test Ticket With Multiple Attachments"
   - Description: "Testing multiple file uploads"
   - Priority: High
   - Category: General
3. Attach 3 files:
   - File 1: PDF document
   - File 2: JPG image
   - File 3: DOCX document
4. Verify all file names appear in UI
5. Click "Create" or "Submit"
6. Wait for all uploads to complete

**Expected Results**:
- ✅ Ticket created successfully
- ✅ Success message: "Successfully uploaded 3 files"
- ✅ All 3 documents appear in ticket view
- ✅ Each document shows correct metadata
- ✅ No errors in logs

**SQL Verification**:
```sql
-- Count documents for the ticket
SELECT COUNT(*) as doc_count,
       SUM(CASE WHEN file_content IS NOT NULL THEN 1 ELSE 0 END) as with_content,
       SUM(file_size) as total_size_bytes
FROM documents
WHERE ticket_id = HEXTORAW('TICKET_ID_IN_HEX');

-- Should return: doc_count=3, with_content=3
```

**Status**: [ ] PASS [ ] FAIL

**Notes**: _________________________________

---

### TC-005: Download Each File Type

**Objective**: Verify different file types download correctly

**Prerequisites**:
- TC-004 passed
- Ticket with multiple file types attached

**Test Steps**:
1. Open ticket from TC-004
2. Download PDF file and verify it opens
3. Download JPG image and verify it displays
4. Download DOCX file and verify it opens

**Expected Results**:
- ✅ PDF opens in PDF viewer correctly
- ✅ JPG displays in image viewer correctly
- ✅ DOCX opens in Word/compatible editor correctly
- ✅ All files match originals (visually verify)
- ✅ No corruption or errors

**Status**: [ ] PASS [ ] FAIL

**Notes**: _________________________________

---

### TC-006: Upload Large File (Near 5MB Limit)

**Objective**: Verify files near size limit work

**Prerequisites**:
- Large PDF file ready (~4.5MB)

**Test Steps**:
1. Click "Create Ticket" button
2. Fill in required fields:
   - Title: "Test Ticket With Large File"
   - Description: "Testing 4.5MB file upload"
   - Priority: Medium
   - Category: General
3. Attach 4.5MB PDF file
4. Click "Create" or "Submit"
5. Wait for upload (may take longer)

**Expected Results**:
- ✅ Ticket created successfully
- ✅ File uploaded successfully
- ✅ Upload may show progress indicator
- ✅ File appears in ticket view
- ✅ File can be downloaded
- ✅ No errors in logs

**SQL Verification**:
```sql
SELECT name, file_size, file_size/1024/1024 as size_mb,
       DBMS_LOB.GETLENGTH(file_content) as actual_size
FROM documents
WHERE ticket_id = HEXTORAW('TICKET_ID_IN_HEX')
ORDER BY uploaded_at DESC;

-- Should show file_size around 4.5MB
```

**Status**: [ ] PASS [ ] FAIL

**Notes**: _________________________________

---

### TC-007: Reject Oversized File (> 5MB)

**Objective**: Verify file size validation works

**Prerequisites**:
- Oversized PDF file ready (> 5MB, e.g., 6MB)

**Test Steps**:
1. Click "Create Ticket" button
2. Fill in required fields:
   - Title: "Test Oversized File Upload"
   - Description: "Testing file size validation"
   - Priority: Low
   - Category: General
3. Attempt to attach 6MB PDF file
4. Click "Create" or "Submit"

**Expected Results**:
- ✅ Error message displayed
- ✅ Message indicates: "Document size must not exceed 5MB"
- ✅ Upload prevented
- ✅ Ticket creation may succeed but without attachment
- ✅ Clear error message to user

**SQL Verification**:
```sql
-- Verify no documents > 5MB were created
SELECT COUNT(*) as oversized_count
FROM documents
WHERE file_size > 5242880;

-- Should return: 0
```

**Status**: [ ] PASS [ ] FAIL

**Notes**: _________________________________

---

### TC-008: Copy Ticket With Attachments

**Objective**: Verify ticket copy functionality includes attachments

**Prerequisites**:
- Ticket with attachments exists (from TC-002 or TC-004)

**Test Steps**:
1. Open ticket with attachments
2. Click "Copy Ticket" button
3. Verify copy form shows original ticket data
4. Check if attachments are indicated for copying
5. Click "Create" to create copied ticket
6. Wait for copy operation to complete

**Expected Results**:
- ✅ Copied ticket created successfully
- ✅ Attachments are copied to new ticket (if feature implemented)
- ✅ Original attachments remain unchanged
- ✅ Copied ticket shows same attachments
- ✅ No errors in logs

**Note**: Attachment copy behavior depends on implementation

**Status**: [ ] PASS [ ] FAIL [ ] N/A

**Notes**: _________________________________

---

### TC-009: Workflow Step Document Upload

**Objective**: Verify document upload to workflow steps

**Prerequisites**:
- Ticket with workflow steps exists

**Test Steps**:
1. Open ticket with workflow steps
2. Navigate to a workflow step
3. Click "Upload Document" for that step
4. Select test PDF file
5. Set document as mandatory/optional as needed
6. Click "Upload" or "Submit"

**Expected Results**:
- ✅ Document uploaded successfully
- ✅ Document appears in step's document list
- ✅ Document can be downloaded
- ✅ Mandatory flag is set correctly
- ✅ No errors in logs

**SQL Verification**:
```sql
SELECT id, name, type, file_size,
       is_mandatory,
       CASE WHEN file_content IS NOT NULL THEN 'YES' ELSE 'NO' END as has_content
FROM documents
WHERE step_id = HEXTORAW('STEP_ID_IN_HEX')
ORDER BY uploaded_at DESC;
```

**Status**: [ ] PASS [ ] FAIL

**Notes**: _________________________________

---

### TC-010: Backward Compatibility - View Old Tickets

**Objective**: Verify old tickets without file_content still work

**Prerequisites**:
- Tickets created before the fix exist in database

**Test Steps**:
1. Query for old tickets in database:
   ```sql
   SELECT id, ticket_number, title, created_at
   FROM tickets
   WHERE created_at < TO_DATE('2026-02-02', 'YYYY-MM-DD')
   ORDER BY created_at DESC
   FETCH FIRST 5 ROWS ONLY;
   ```
2. Open one of these old tickets in UI
3. Check ticket details display correctly
4. Check if any old documents (with NULL file_content) are shown

**Expected Results**:
- ✅ Old tickets display without errors
- ✅ Ticket details are correct
- ✅ No JavaScript errors in browser console
- ✅ Old documents (if any) display metadata
- ✅ Download may not work for old documents (expected)
- ✅ No application errors in logs

**SQL Verification**:
```sql
-- Check documents with NULL file_content
SELECT COUNT(*) as null_content_count
FROM documents
WHERE file_content IS NULL;

-- Some count is expected (old documents)
```

**Status**: [ ] PASS [ ] FAIL

**Notes**: _________________________________

---

### TC-011: Concurrent File Uploads

**Objective**: Verify system handles multiple simultaneous uploads

**Prerequisites**:
- Multiple users or browser tabs
- Test files ready

**Test Steps**:
1. Open application in 2-3 browser tabs
2. Login as different users in each tab
3. Simultaneously create tickets with attachments in all tabs
4. Click "Create" in all tabs within a few seconds
5. Wait for all uploads to complete

**Expected Results**:
- ✅ All tickets created successfully
- ✅ All files uploaded successfully
- ✅ No file corruption
- ✅ No database deadlocks
- ✅ All documents can be downloaded correctly

**SQL Verification**:
```sql
-- Check recent uploads
SELECT COUNT(*) as recent_uploads
FROM documents
WHERE uploaded_at > SYSDATE - 5/1440; -- Last 5 minutes

-- Should match number of files uploaded
```

**Status**: [ ] PASS [ ] FAIL

**Notes**: _________________________________

---

### TC-012: Application Performance - Multiple Large Files

**Objective**: Verify performance with multiple large uploads

**Prerequisites**:
- Multiple large files ready (each ~4MB)

**Test Steps**:
1. Create ticket
2. Attach 5 files, each ~4MB
3. Click "Create" or "Submit"
4. Monitor upload time and application responsiveness

**Expected Results**:
- ✅ All files uploaded successfully (may take time)
- ✅ Application remains responsive
- ✅ Progress indicator shows upload status (if implemented)
- ✅ No timeouts or errors
- ✅ Total upload time reasonable (< 5 minutes on good connection)

**Performance Metrics**:
- Upload time: _________ seconds
- Application CPU usage: _________ %
- Memory usage: _________ MB

**Status**: [ ] PASS [ ] FAIL

**Notes**: _________________________________

---

### TC-013: Database Storage Verification

**Objective**: Verify BLOB data is stored correctly in database

**Prerequisites**:
- Several test cases with file uploads completed

**Test Steps**:
1. Run database verification queries
2. Check storage utilization
3. Verify data integrity

**SQL Verification**:
```sql
-- 1. Check documents with file content
SELECT COUNT(*) as total_docs,
       COUNT(file_content) as docs_with_content,
       SUM(file_size) as total_size_bytes,
       ROUND(SUM(file_size)/1024/1024, 2) as total_size_mb
FROM documents;

-- 2. Verify file size matches content size
SELECT COUNT(*) as mismatch_count
FROM documents
WHERE file_content IS NOT NULL
AND file_size != DBMS_LOB.GETLENGTH(file_content);
-- Should return: 0 (no mismatches)

-- 3. Check database segment size
SELECT segment_name, segment_type,
       ROUND(bytes/1024/1024, 2) as size_mb
FROM user_segments
WHERE segment_name = 'DOCUMENTS';

-- 4. Check tablespace usage
SELECT tablespace_name,
       ROUND(SUM(bytes)/1024/1024, 2) as used_mb
FROM user_segments
WHERE segment_name IN ('DOCUMENTS', 'SYS_LOB0000*')
GROUP BY tablespace_name;
```

**Expected Results**:
- ✅ All uploaded documents have file_content
- ✅ No size mismatches
- ✅ Reasonable database growth
- ✅ Tablespace has sufficient free space

**Status**: [ ] PASS [ ] FAIL

**Notes**: _________________________________

---

### TC-014: Application Logs Verification

**Objective**: Verify logging is working correctly

**Prerequisites**:
- Several test cases completed

**Test Steps**:
1. Check application logs for document-related entries
2. Verify log messages are clear and helpful

**Log Verification**:
```bash
# Check for successful uploads
grep "Created document.*with file content" $TOMCAT_HOME/logs/catalina.out | tail -10

# Check for any errors
grep -i "error.*document" $TOMCAT_HOME/logs/catalina.out | tail -10

# Check for validation errors
grep "Document size must not exceed" $TOMCAT_HOME/logs/catalina.out
```

**Expected Results**:
- ✅ Log entries show successful document creation
- ✅ File content size is logged
- ✅ No unexpected errors
- ✅ Validation errors are logged when appropriate
- ✅ Log messages are clear and actionable

**Status**: [ ] PASS [ ] FAIL

**Notes**: _________________________________

---

## Test Summary

| Test Case | Status | Priority | Notes |
|-----------|--------|----------|-------|
| TC-001: No attachments | [ ] | High | Baseline |
| TC-002: Single PDF | [ ] | Critical | Core functionality |
| TC-003: Download PDF | [ ] | Critical | Core functionality |
| TC-004: Multiple files | [ ] | High | Common use case |
| TC-005: Different types | [ ] | High | File type support |
| TC-006: Large file | [ ] | Medium | Size limits |
| TC-007: Oversized file | [ ] | High | Validation |
| TC-008: Copy ticket | [ ] | Low | Advanced feature |
| TC-009: Workflow docs | [ ] | High | Step documents |
| TC-010: Backward compat | [ ] | Critical | Existing data |
| TC-011: Concurrent uploads | [ ] | Medium | Load testing |
| TC-012: Performance | [ ] | Medium | Performance |
| TC-013: Database verify | [ ] | High | Data integrity |
| TC-014: Logs verify | [ ] | Medium | Monitoring |

## Overall Assessment

**Total Test Cases**: 14
**Passed**: _____
**Failed**: _____
**Blocked**: _____
**Not Applicable**: _____

**Critical Issues Found**: _____________________________

**Recommendation**: [ ] APPROVE FOR PRODUCTION [ ] REJECT [ ] RETEST REQUIRED

## Sign-off

**Tested By**: ___________________ Date: ___________

**Reviewed By**: ___________________ Date: ___________

**Approved By**: ___________________ Date: ___________

---

**Test Environment**: ___________________________
**Database Version**: ___________________________
**Application Version**: ___________________________
**Test Duration**: ___________________________
