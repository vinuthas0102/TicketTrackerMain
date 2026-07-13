# Quick Testing Guide: Workflow Comments & Attachments

## Prerequisites
- Backend server running on port 8080
- Frontend running (or built and deployed)
- At least one ticket with workflow steps created
- User logged in

## Test Scenario 1: View Workflow Comments

### Steps:
1. Navigate to ticket details page
2. Locate a workflow step in the workflow list
3. Click on the workflow step to expand details
4. Click the "Show comments" button/icon

### Expected Result:
- Comments section should appear
- If comments exist, they should display with:
  - User avatar (initials)
  - User name
  - User role badge (color-coded)
  - Comment content
  - Timestamp (e.g., "2 hours ago")
- If no comments, should show "No comments yet" message

## Test Scenario 2: Create a Comment

### Steps:
1. In an open workflow step, find the comment section
2. Type text in the "Add a comment..." textarea
3. Click "Post Comment" button

### Expected Result:
- "Posting..." loading state appears
- Comment is added to the list immediately after posting
- Comment shows your name, role, and "Just now" timestamp
- Comment textarea clears after successful post
- Edit and delete icons appear next to your comment

### Common Issues:
- If "Authentication required" error: Check that `/api/workflow-comments` is in PUBLIC_PATHS
- If comment doesn't appear: Check browser console for API errors
- If 401 error: Verify user session is valid

## Test Scenario 3: Edit Your Comment

### Steps:
1. Find a comment you created
2. Click the edit icon (pencil)
3. Modify the text in the textarea
4. Click "Save"

### Expected Result:
- Textarea appears with current comment text
- "Save" and "Cancel" buttons visible
- After saving, updated text displays immediately
- Edit mode closes automatically
- Updated timestamp may show

### Verification:
- Refresh the page and verify edit persisted
- Check that only you can edit your comments (no edit icon on others' comments)

## Test Scenario 4: Delete a Comment

### Steps:
1. Find a comment you created
2. Click the delete icon (trash)
3. Confirm deletion in the popup

### Expected Result:
- Confirmation dialog appears
- After confirming, comment disappears immediately
- Comment count updates
- No errors in console

### Verification:
- Refresh page to verify deletion persisted
- Verify other users' comments cannot be deleted

## Test Scenario 5: Upload Files During Workflow Update

### Steps:
1. Click on a workflow step
2. Click "Update progress" or similar action
3. In the update form, add progress comment
4. Attach files using the file upload button
5. Submit the update

### Expected Result:
- Files upload successfully
- Progress update completes
- New audit log entry created
- Files appear in audit trail with download links

### Verification:
1. Open ticket's audit trail
2. Find the progress update entry
3. Verify attached files are listed
4. Click file name to view/download
5. Verify file type icon and size display correctly

## Test Scenario 6: File Reference Template Uploads

### Steps:
1. Create/open workflow step with file reference template
2. Click "Show documents" on the workflow step
3. Find "File References (Template-Based)" section
4. Click upload button for a required file
5. Select and upload file

### Expected Result:
- Upload progress indicator appears
- File status changes from "Required" to "Uploaded"
- Check mark appears next to uploaded file
- Progress counter updates (e.g., "2/5 uploaded")
- Mandatory file count updates separately

### Verification:
- Refresh page to verify upload persisted
- Verify download link works
- Check file appears in appropriate audit log entry

## Test Scenario 7: Multi-User Comment Interaction

### Setup:
- Use two different user accounts (e.g., EO and DO roles)

### Steps:
1. User A: Add a comment to workflow step
2. User B: Log in and view same workflow step
3. User B: Verify they can see User A's comment
4. User B: Verify they CANNOT edit/delete User A's comment
5. User B: Add their own comment
6. User A: Refresh and verify they see both comments

### Expected Result:
- Comments from both users visible
- Each user can only edit/delete their own comments
- Role badges show correctly for each user
- Timestamps are accurate

## API Endpoint Testing (Optional - For Developers)

### Using curl or Postman:

**1. Get Comments:**
```bash
curl -X GET "http://localhost:8080/ticket-tracker-java/api/workflow-comments?stepId={STEP_UUID}" \
  -H "Cookie: JSESSIONID={your-session-id}"
```

**2. Create Comment:**
```bash
curl -X POST "http://localhost:8080/ticket-tracker-java/api/workflow-comments" \
  -H "Content-Type: application/json" \
  -H "Cookie: JSESSIONID={your-session-id}" \
  -d '{
    "stepId": "{STEP_UUID}",
    "content": "Test comment",
    "userId": "{USER_UUID}"
  }'
```

**3. Update Comment:**
```bash
curl -X PUT "http://localhost:8080/ticket-tracker-java/api/workflow-comments/{COMMENT_UUID}" \
  -H "Content-Type: application/json" \
  -H "Cookie: JSESSIONID={your-session-id}" \
  -d '{
    "content": "Updated comment text",
    "userId": "{USER_UUID}"
  }'
```

**4. Delete Comment:**
```bash
curl -X DELETE "http://localhost:8080/ticket-tracker-java/api/workflow-comments/{COMMENT_UUID}" \
  -H "Cookie: JSESSIONID={your-session-id}"
```

## Troubleshooting

### Comments Not Loading
**Check:**
1. Browser console for errors
2. Network tab - verify API call to `/api/workflow-comments?stepId=...`
3. Response status (should be 200)
4. Response body contains array of comments

**Common Fixes:**
- Clear browser cache
- Verify backend server is running
- Check database has workflow_comments table
- Verify stepId is valid UUID

### Cannot Create Comments
**Check:**
1. User is logged in (session exists)
2. Network request shows 401 or 403 error
3. `/api/workflow-comments` in PUBLIC_PATHS
4. Request payload includes stepId, content, userId

**Common Fixes:**
- Log out and log back in
- Verify AuthenticationFilter configuration
- Check WorkflowCommentServlet authentication logic

### Files Not Displaying
**Check:**
1. Files uploaded successfully (check response)
2. Audit trail loaded completely
3. File paths in database are correct
4. Storage bucket accessible

**Common Fixes:**
- Verify file upload completed
- Check audit_logs and progress_documents tables
- Verify file storage configuration
- Check RLS policies on storage bucket

### Edit/Delete Not Working
**Check:**
1. You own the comment (createdBy matches your userId)
2. Network request succeeds (200/204 status)
3. Comment ID is valid

**Common Fixes:**
- Verify userId matches comment creator
- Check permissions in servlet
- Verify comment still exists in database

## Success Criteria

✅ Comments load and display with user information
✅ New comments can be created and appear immediately
✅ Own comments can be edited and deleted
✅ Other users' comments cannot be edited/deleted
✅ Progress documents appear in audit trail
✅ File reference uploads show completion status
✅ All operations handle errors gracefully
✅ Loading states display during operations

## Performance Notes

- Comments are fetched separately per workflow step (not pre-loaded with ticket)
- File uploads are processed individually (not batched)
- Audit trail loads all entries at once (may be slow for tickets with many entries)

## Next Steps After Successful Testing

1. Deploy to production environment
2. Monitor error logs for any issues
3. Collect user feedback on comment functionality
4. Consider adding features like:
   - Comment reactions/likes
   - @mentions
   - Comment notifications
   - Comment attachments
   - Rich text formatting
