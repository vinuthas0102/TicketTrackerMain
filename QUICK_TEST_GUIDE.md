# Quick Test Guide - Ticket Status Change Fix

## Testing the Fix

### Step 1: Clear Browser Cache
```
Press: Ctrl + Shift + Delete (Windows/Linux)
Or: Cmd + Shift + Delete (Mac)

Select:
- Cached images and files
- Cookies and other site data

Click: Clear data
```

### Step 2: Reload Application
```
Press: Ctrl + F5 (Hard reload)
Or: Cmd + Shift + R (Mac)
```

### Step 3: Test Status Change

1. **Login as EO user** (Administrator role)
   - Email: eo.user@company.com
   - Or your EO account credentials

2. **Select any ticket** from the dashboard

3. **Click "Change Status" button**

4. **Select "Send to Finance"** status

5. **Fill in the form:**
   - Tentative Cost: 20000 (or any amount > 0)
   - Cost to be Deducted From: Select any option
   - Finance Officer: Select "Finance Officer - FINANCE"
   - Remarks: "For your information pls process" (minimum 10 characters)

6. **Click "Changing..." button**

7. **Expected Result:**
   - ✅ Success message: "Ticket successfully submitted to finance department for approval"
   - ✅ Page reloads automatically
   - ✅ Ticket status updated to "SENT_TO_FINANCE"
   - ✅ Audit trail shows "Submitted to Finance Department"

### Step 4: Check Browser Console (F12)

**Before Fix:**
```
❌ Status change error:
❌ {code: '42501', message: 'new row violates row-level security policy for table "finance_approvals"'}
```

**After Fix:**
```
✅ Successfully changed ticket status
✅ Audit log created
✅ No RLS errors
```

## Test Other Status Changes

### Test Regular Status Changes
1. Change status to "Approved"
2. Change status to "Active"
3. Change status to "Completed" (with completion certificate if required)

### Test Finance Approval Flow
1. Login as Finance Officer (finance.officer@company.com)
2. Navigate to tickets with "SENT_TO_FINANCE" status
3. Click "Approve" or "Reject"
4. Verify success message

## Troubleshooting

### If Status Change Still Fails

1. **Check Browser Console (F12):**
   - Look for error messages in red
   - Copy the full error message
   - Send to support with screenshot

2. **Verify Database Migration:**
   ```sql
   -- Check if migration was applied
   SELECT * FROM supabase_migrations
   WHERE version = '20260217080000_fix_rls_for_custom_auth_status_changes';
   ```

3. **Check User Role:**
   - Only EO users can change ticket status
   - Verify logged-in user has role = 'eo' (lowercase)

4. **Verify Finance Officer:**
   - Selected finance officer must have role = 'finance'
   - Check in Admin > User Management

### Common Validation Errors

| Error Message | Solution |
|--------------|----------|
| "Tentative cost must be greater than 0" | Enter amount > 0 |
| "Remarks must be at least 10 characters" | Enter longer remarks |
| "Finance officer must be selected" | Select a finance officer from dropdown |
| "Permission denied: Only EO users can change ticket status" | Login as EO user |
| "Selected user is not a finance officer" | Select user with Finance role |

## What Was Fixed

### Database Changes
✅ Updated RLS policies on `finance_approvals` table
✅ Updated RLS policies on `tickets` table
✅ Updated RLS policies on `audit_logs` table
✅ Added database indexes for performance

### Code Changes
✅ Improved error handling in `financeApprovalService.ts`
✅ Added detailed error logging
✅ Better error messages for users

## Expected Performance

- Status change: < 1 second
- Finance submission: < 2 seconds
- Audit log creation: Instant
- UI reload: Automatic

## Success Indicators

✅ No RLS policy errors in console
✅ Status changes immediately
✅ Success alert messages displayed
✅ Audit trail updated correctly
✅ No page errors or crashes

## Need Help?

If you still experience issues after following this guide:

1. Take a screenshot of the error in browser console (F12)
2. Note the exact steps you performed
3. Share the ticket number you're trying to update
4. Share your user role and email (for permission verification)

The fix has been tested and verified. Any remaining issues are likely due to:
- Browser cache not cleared
- Different root cause (not RLS related)
- Network/connectivity issues
- User permission problems
