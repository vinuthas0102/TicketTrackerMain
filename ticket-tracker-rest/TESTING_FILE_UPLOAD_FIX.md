# Testing Guide: File Upload Null StepId Fix

## Quick Test Steps

### Test 1: Ticket Creation with File Upload (PREVIOUSLY FAILING)
**Scenario:** Upload file when creating a new ticket (no workflow steps involved)

1. Log in to the application
2. Navigate to Maintenance Tracker module
3. Click "New Ticket" button
4. Fill in ticket details:
   - Title: `TestFileUpload/[Date]/001`
   - Description: Any text
   - Category: General
   - Priority: Medium
5. **Attach a file** using the file upload section
6. Click "Create Ticket"

**Expected Result:**
- ✅ Ticket created successfully
- ✅ File uploaded successfully
- ✅ No error messages in console
- ✅ No "EFEF" error in backend logs

**Previous Error:**
```
404 Not Found
Workflow Step with ID EFEF not found
```

### Test 2: Workflow Step Document Upload
**Scenario:** Upload file to a specific workflow step

1. Open an existing ticket with workflow steps
2. Navigate to a workflow step
3. Click to upload a progress document
4. Select a file and upload
5. Verify upload completes

**Expected Result:**
- ✅ File uploaded to step successfully
- ✅ File appears in step's document list
- ✅ No errors in console or logs

### Test 3: Multiple File Upload
**Scenario:** Upload multiple files to a ticket

1. Create a new ticket with 2-3 files attached
2. Verify all files upload successfully
3. Check ticket view shows all uploaded files

**Expected Result:**
- ✅ All files uploaded successfully
- ✅ All files visible in ticket view
- ✅ No partial failures

## What to Check in Logs

### Frontend Console Logs (Browser DevTools)
Look for these new log messages:

```
[ApiClient] Skipping null/undefined field: stepId = null
```

This confirms the frontend is filtering out null values.

### Backend Logs (Tomcat/Server Logs)
Look for these new log messages:

```
File upload parameters - ticketId: [UUID], stepId: null, fileName: [filename]
```

**What NOT to see:**
- ❌ `Rejecting invalid parameter value: null`
- ❌ `Workflow Step with ID EFEF not found`
- ❌ Any error related to stepId validation

## Before and After Comparison

### BEFORE (Broken)
```
Frontend sends: stepId = null (JavaScript)
↓
FormData converts: stepId = "null" (string)
↓
Backend receives: "null" as parameter
↓
ByteArrayUtil parses: "null" → [0xEF, 0xEF]
↓
Validation fails: "Workflow Step with ID EFEF not found"
```

### AFTER (Fixed)
```
Frontend sends: (stepId not included in FormData)
↓
Backend receives: null/empty parameter
↓
Validation skips: isValidParameter() returns false
↓
Document created: with stepId = null
↓
Success: File uploaded to ticket
```

## Troubleshooting

### If Test 1 Still Fails

1. **Check Frontend Build:**
   ```bash
   cd ticket-tracker-rest/frontend
   npm run build
   ```

2. **Check Browser Console:**
   - Look for the new skip log: `[ApiClient] Skipping null/undefined field: stepId`
   - If not present, frontend changes may not be deployed

3. **Check Backend Logs:**
   - Look for: `File upload parameters - ticketId: ..., stepId: null`
   - If you see: `Rejecting invalid parameter value: null`, backend is working but still receiving the parameter
   - If you see: `EFEF not found`, backend changes may not be deployed

### If Test 2 Fails

1. **Verify stepId is being sent:**
   - Check browser console for FormData fields
   - Should see: `[ApiClient] FormData field: stepId = [valid-uuid]`

2. **Check Backend:**
   - Verify valid UUID is received
   - Verify workflow step exists in database

## Performance Check

The fix adds minimal overhead:
- Frontend: One conditional check per FormData field
- Backend: One string validation per UUID parameter
- Expected impact: < 1ms per request

## Deployment Checklist

- [ ] Frontend build successful
- [ ] Backend compilation successful
- [ ] Test 1 passes (ticket with file upload)
- [ ] Test 2 passes (workflow step document)
- [ ] Test 3 passes (multiple files)
- [ ] No "EFEF" errors in logs
- [ ] Frontend console shows skip logs for null fields
- [ ] Backend logs show correct parameter values

## Success Criteria

✅ All three tests pass without errors
✅ No "EFEF" errors in backend logs
✅ Files upload successfully in all scenarios
✅ User experience is smooth and error-free
