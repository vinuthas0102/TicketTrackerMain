# Workflow Comments Implementation Summary

## Overview
This document describes the complete implementation of the workflow comments endpoint for the Ticket Tracker Java backend. This implementation addresses the missing endpoint that was causing workflow updates to fail.

## Requirements
1. Comments should be editable only by the user who created them
2. Comments should be visible to:
   - The user who created them
   - Users with the EO (Executive Officer) role

## Implementation Details

### 1. Model Layer
**File**: `src/main/java/com/tickettracker/model/WorkflowComment.java`

Created a new model class representing workflow comments with the following fields:
- `id` (byte[]): Unique identifier (RAW(16) in Oracle)
- `stepId` (byte[]): Reference to the workflow step
- `content` (String): The comment text
- `createdBy` (byte[]): Reference to the user who created the comment
- `createdAt` (Timestamp): When the comment was created
- `updatedAt` (Timestamp): When the comment was last updated
- `createdByName` (String): Transient field for display purposes
- `createdByRole` (String): Transient field for permission checks

The model includes proper JSON serialization with UUID conversion using `@JsonProperty` and `@JsonIgnore` annotations.

### 2. Data Access Layer
**File**: `src/main/java/com/tickettracker/dao/WorkflowCommentDAO.java`

Implemented DAO operations:
- `create(WorkflowComment)`: Insert new comment with auto-generated UUID
- `update(WorkflowComment)`: Update existing comment content and updated_at timestamp
- `findById(byte[])`: Get specific comment with creator information
- `findByStepId(byte[])`: Get all comments for a workflow step
- `findByStepIdAndUser(byte[], byte[])`: Get comments for a step filtered by user

All queries join with the users table to include creator name and role for display and permission checks.

### 3. Service Layer
**File**: `src/main/java/com/tickettracker/service/WorkflowCommentService.java`

Implemented business logic with security checks:

#### createComment(stepId, content, userId)
- Validates that content is not empty
- Validates that the workflow step exists
- Creates a new comment associated with the step
- Returns the created comment with creator information

#### updateComment(commentId, content, userId)
- Validates that content is not empty
- Validates that the comment exists
- **Security Check**: Verifies that the user is the comment creator
- Returns 403 Forbidden if user is not the creator
- Updates the comment and sets updated_at to current timestamp

#### getStepComments(stepId, userId)
- Validates that the workflow step exists
- **Visibility Logic**:
  - If user role is "eo": Returns ALL comments for the step
  - If user role is not "eo": Returns ONLY comments created by that user
- Returns list of comments with creator information

#### validatePermissions(commentId, userId)
- Helper method to validate edit permissions
- Throws ForbiddenException if user is not the comment creator

### 4. API Endpoint
**File**: `src/main/java/com/tickettracker/servlet/WorkflowCommentServlet.java`

Implemented servlet with URL mapping: `/api/workflow-comments/*`

#### GET /api/workflow-comments?stepId={id}
- Requires authentication
- Fetches comments for a workflow step
- Filters results based on user role (EO sees all, others see only their own)
- Returns: `200 OK` with list of comments

#### POST /api/workflow-comments
- Requires authentication
- Creates a new comment
- Request body: `{ "stepId": "uuid", "content": "comment text" }`
- Returns: `201 Created` with the created comment
- Errors: `400 Bad Request` for validation errors, `401 Unauthorized` if not authenticated

#### PUT /api/workflow-comments/{commentId}
- Requires authentication
- Updates an existing comment
- Request body: `{ "content": "updated text" }`
- **Security**: Only the comment creator can edit
- Returns: `200 OK` with updated comment
- Errors: `403 Forbidden` if user is not the creator, `404 Not Found` if comment doesn't exist

### 5. Database Migration
**File**: `database/09-oracle-add-workflow-comments-updated-at.sql`

Added migration script to update the database schema:
- Adds `updated_at` column to workflow_comments table
- Sets default value to CURRENT_TIMESTAMP
- Backfills existing records with created_at value
- Creates index on updated_at for query performance
- Idempotent script that can be run multiple times safely

**To apply the migration:**
```sql
sqlplus ticket_tracker/password@database
@09-oracle-add-workflow-comments-updated-at.sql
```

## Security Features

### Edit Permissions
- Users can ONLY edit their own comments
- Attempts to edit others' comments result in 403 Forbidden
- Even EO users cannot edit comments created by others
- Permission check happens at service layer before any database update

### Visibility Permissions
- EO users can see ALL comments on any workflow step
- Regular users can ONLY see comments they created
- Filtering happens at database query level for efficiency
- User role is retrieved from database to prevent client-side manipulation

## Testing Checklist

### Create Comment
- [ ] POST to `/api/workflow-comments` with valid data creates comment
- [ ] Response includes comment ID, content, creator name, timestamps
- [ ] Returns 400 if content is empty
- [ ] Returns 400 if stepId is missing or invalid
- [ ] Returns 401 if user is not authenticated

### Update Comment
- [ ] PUT to `/api/workflow-comments/{id}` with valid data updates comment
- [ ] updated_at timestamp is updated correctly
- [ ] User can update their own comment
- [ ] User CANNOT update another user's comment (403 Forbidden)
- [ ] Returns 404 if comment ID doesn't exist

### View Comments
- [ ] GET with valid stepId returns comments
- [ ] EO user sees ALL comments for the step
- [ ] Regular user sees ONLY their own comments for the step
- [ ] Comments include creator name and role
- [ ] Comments are ordered by created_at ascending

### Edge Cases
- [ ] Creating comment on non-existent step returns 404
- [ ] Empty or whitespace-only content is rejected
- [ ] Very long content is properly stored (CLOB field)
- [ ] UUID format validation works correctly
- [ ] Multiple comments by same user on same step all appear

## API Examples

### Create Comment
```bash
POST /api/workflow-comments
Content-Type: application/json

{
  "stepId": "123e4567-e89b-12d3-a456-426614174000",
  "content": "Progress update: 50% complete"
}

Response: 201 Created
{
  "id": "987fcdeb-51a2-43d8-b234-567890abcdef",
  "stepId": "123e4567-e89b-12d3-a456-426614174000",
  "content": "Progress update: 50% complete",
  "createdBy": "user-id",
  "createdByName": "John Doe",
  "createdByRole": "employee",
  "createdAt": "2026-01-16T10:30:00.000Z",
  "updatedAt": "2026-01-16T10:30:00.000Z"
}
```

### Update Comment
```bash
PUT /api/workflow-comments/987fcdeb-51a2-43d8-b234-567890abcdef
Content-Type: application/json

{
  "content": "Progress update: 75% complete (updated)"
}

Response: 200 OK
{
  "id": "987fcdeb-51a2-43d8-b234-567890abcdef",
  "stepId": "123e4567-e89b-12d3-a456-426614174000",
  "content": "Progress update: 75% complete (updated)",
  "createdBy": "user-id",
  "createdByName": "John Doe",
  "createdByRole": "employee",
  "createdAt": "2026-01-16T10:30:00.000Z",
  "updatedAt": "2026-01-16T11:15:00.000Z"
}
```

### Get Comments (EO User)
```bash
GET /api/workflow-comments?stepId=123e4567-e89b-12d3-a456-426614174000

Response: 200 OK
[
  {
    "id": "comment-1",
    "content": "Started work on this task",
    "createdByName": "Alice Smith",
    "createdByRole": "employee",
    "createdAt": "2026-01-16T09:00:00.000Z",
    "updatedAt": "2026-01-16T09:00:00.000Z"
  },
  {
    "id": "comment-2",
    "content": "75% complete",
    "createdByName": "Bob Johnson",
    "createdByRole": "employee",
    "createdAt": "2026-01-16T10:30:00.000Z",
    "updatedAt": "2026-01-16T10:30:00.000Z"
  }
]
```

### Get Comments (Regular User)
```bash
GET /api/workflow-comments?stepId=123e4567-e89b-12d3-a456-426614174000

Response: 200 OK (only user's own comments)
[
  {
    "id": "comment-2",
    "content": "75% complete",
    "createdByName": "Bob Johnson",
    "createdByRole": "employee",
    "createdAt": "2026-01-16T10:30:00.000Z",
    "updatedAt": "2026-01-16T10:30:00.000Z"
  }
]
```

## Integration with Workflow Step Updates

The workflow comments endpoint is now available for use when updating workflow step progress. The frontend can:

1. Display existing comments when viewing a workflow step
2. Allow users to add new comments when updating progress
3. Allow users to edit their own comments
4. Show all comments to EO users, but only own comments to regular users

## Files Created/Modified

### New Files
1. `src/main/java/com/tickettracker/model/WorkflowComment.java`
2. `src/main/java/com/tickettracker/dao/WorkflowCommentDAO.java`
3. `src/main/java/com/tickettracker/service/WorkflowCommentService.java`
4. `src/main/java/com/tickettracker/servlet/WorkflowCommentServlet.java`
5. `database/09-oracle-add-workflow-comments-updated-at.sql`

### Database Changes
- Added `updated_at` column to `workflow_comments` table
- Added index on `updated_at` for performance
- Backfilled existing records with appropriate values

## Next Steps

1. **Apply Database Migration**: Run the migration script on the Oracle database
2. **Build and Deploy**: Compile and deploy the updated WAR file to Tomcat
3. **Test Endpoints**: Use the testing checklist above to verify all functionality
4. **Frontend Integration**: Update the frontend to use the new endpoints
5. **Monitor Logs**: Watch application logs for any errors or issues

## Troubleshooting

### Common Issues

**403 Forbidden when updating comment:**
- Verify the user is the comment creator
- Check that the session is valid and user ID matches

**Empty comment list for non-EO user:**
- This is expected if the user hasn't created any comments on that step
- Verify user role is correctly set in the database

**400 Bad Request with UUID error:**
- Ensure UUID format is correct (hyphenated, lowercase)
- Verify stepId or commentId is a valid UUID string

**Database migration fails:**
- Check if column already exists
- Verify database user has ALTER TABLE privileges
- Review Oracle error logs for specific issues

## Conclusion

This implementation provides a complete, secure workflow comments system that:
- Allows users to track progress with comments
- Enforces edit permissions (only creator can edit)
- Implements role-based visibility (EO sees all, others see only their own)
- Follows existing code patterns and conventions
- Includes proper error handling and logging
- Is production-ready and thoroughly documented
