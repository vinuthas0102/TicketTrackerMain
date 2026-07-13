# API Specification

Complete specification of REST API endpoints required for the Ticket Tracker backend.

## Base URL

All API endpoints are relative to: `{API_BASE_URL}/api`

Example: `https://your-domain.com/api`

## Authentication

All endpoints (except login) require JWT authentication.

### Headers

```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

## Response Format

### Success Response

```json
{
  "success": true,
  "data": {
    // Response data
  }
}
```

### Error Response

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error message",
    "details": {}
  }
}
```

## HTTP Status Codes

- `200` - Success
- `201` - Created
- `400` - Bad Request (validation error)
- `401` - Unauthorized (invalid/missing token)
- `403` - Forbidden (insufficient permissions)
- `404` - Not Found
- `500` - Internal Server Error

---

## Authentication Endpoints

### POST /auth/login

Authenticate user and return JWT token.

**Request:**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "dGhpc2lzYXJlZnJlc2h0b2tlbg==",
    "user": {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "email": "admin@company.com",
      "name": "System Administrator",
      "role": "eo",
      "department": "IT",
      "active": true,
      "last_login": "2024-01-15T10:30:00Z"
    }
  }
}
```

### POST /auth/logout

Logout current user (optional - can be client-side only).

**Request:** Empty body

**Response:**
```json
{
  "success": true,
  "data": {}
}
```

### POST /auth/refresh

Refresh JWT token using refresh token.

**Request:**
```json
{
  "refreshToken": "dGhpc2lzYXJlZnJlc2h0b2tlbg=="
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "bmV3cmVmcmVzaHRva2Vu"
  }
}
```

---

## User Management Endpoints

### GET /users

List all users.

**Query Parameters:** None

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "email": "admin@company.com",
      "name": "System Administrator",
      "role": "eo",
      "department": "IT",
      "active": true,
      "last_login": "2024-01-15T10:30:00Z",
      "created_at": "2024-01-01T00:00:00Z",
      "updated_at": "2024-01-15T10:30:00Z"
    }
  ]
}
```

### GET /users/{id}

Get user by ID.

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "email": "admin@company.com",
    "name": "System Administrator",
    "role": "eo",
    "department": "IT",
    "active": true,
    "last_login": "2024-01-15T10:30:00Z"
  }
}
```

### POST /users

Create new user.

**Request:**
```json
{
  "email": "newuser@company.com",
  "name": "New User",
  "role": "employee",
  "department": "HR",
  "active": true,
  "createdBy": "550e8400-e29b-41d4-a716-446655440001"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440002"
  }
}
```

### PUT /users/{id}

Update user.

**Request:**
```json
{
  "name": "Updated Name",
  "department": "IT",
  "updatedBy": "550e8400-e29b-41d4-a716-446655440001"
}
```

**Response:**
```json
{
  "success": true,
  "data": {}
}
```

### DELETE /users/{id}

Delete user.

**Query Parameters:**
- `deletedBy` (required): User ID performing deletion

**Response:**
```json
{
  "success": true,
  "data": {}
}
```

### PUT /users/{id}/enable

Enable user account.

**Request:**
```json
{
  "enabledBy": "550e8400-e29b-41d4-a716-446655440001"
}
```

### PUT /users/{id}/disable

Disable user account.

**Request:**
```json
{
  "disabledBy": "550e8400-e29b-41d4-a716-446655440001",
  "reason": "Security policy violation"
}
```

---

## Module Endpoints

### GET /modules

List all active modules.

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440101",
      "name": "Maintenance Tracker",
      "description": "Track maintenance requests",
      "icon": "Wrench",
      "color": "from-blue-500 to-indigo-500",
      "schema_id": "maintenance",
      "config": {
        "categories": ["Electrical", "Plumbing", "HVAC"]
      },
      "active": true,
      "created_at": "2024-01-01T00:00:00Z",
      "updated_at": "2024-01-01T00:00:00Z"
    }
  ]
}
```

---

## Ticket Endpoints

### GET /tickets

List tickets with filters.

**Query Parameters:**
- `moduleId` (required): Module ID
- `userId` (optional): User ID for access control
- `userRole` (optional): User role for filtering

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "ticket-uuid",
      "ticket_number": "TKT-1234567890",
      "module_id": "module-uuid",
      "title": "Fix broken light",
      "description": "Light in room 101 is not working",
      "status": "open",
      "priority": "high",
      "created_by": "user-uuid",
      "assigned_to": "user-uuid",
      "created_at": "2024-01-15T10:00:00Z",
      "updated_at": "2024-01-15T10:00:00Z",
      "due_date": "2024-01-20T00:00:00Z",
      "start_date": null,
      "property_id": "PROP001",
      "property_location": "Building A",
      "completion_documents_required": true,
      "requires_finance_approval": true,
      "finance_officer_id": null,
      "finance_submission_count": 0,
      "latest_finance_status": null,
      "data": {},
      "workflow": [],
      "attachments": [],
      "audit_trail": []
    }
  ]
}
```

### GET /tickets/{id}

Get ticket details.

**Response:** Same structure as list item above.

### POST /tickets

Create new ticket.

**Request:**
```json
{
  "moduleId": "module-uuid",
  "title": "Fix broken light",
  "description": "Light in room 101 is not working",
  "status": "open",
  "priority": "high",
  "createdBy": "user-uuid",
  "assignedTo": "user-uuid",
  "dueDate": "2024-01-20T00:00:00Z",
  "propertyId": "PROP001",
  "propertyLocation": "Building A",
  "data": {},
  "copiedFromTicketId": null
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "new-ticket-uuid"
  }
}
```

### PUT /tickets/{id}

Update ticket.

**Request:**
```json
{
  "title": "Updated title",
  "description": "Updated description",
  "priority": "medium",
  "userId": "user-uuid"
}
```

### PUT /tickets/{id}/status

Change ticket status.

**Request:**
```json
{
  "newStatus": "completed",
  "currentStatus": "wip",
  "remarks": "Work completed successfully",
  "userId": "user-uuid"
}
```

For status change to "completed" with file:
- Use multipart/form-data
- Include file with key "file"
- Include other fields as form data

### POST /tickets/bulk

Bulk create tickets.

**Request:**
```json
{
  "tickets": [
    {
      "title": "Ticket 1",
      "description": "Description 1",
      "status": "open",
      "priority": "medium",
      "propertyId": "PROP001",
      "propertyLocation": "Location 1",
      "department": "IT"
    }
  ],
  "moduleId": "module-uuid",
  "createdBy": "user-uuid"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "successCount": 1,
    "failedCount": 0,
    "totalCount": 1,
    "errors": [],
    "createdTicketIds": ["ticket-uuid-1"]
  }
}
```

### DELETE /tickets/{id}

Delete ticket.

---

## Workflow Step Endpoints

### GET /tickets/{ticketId}/steps

List all workflow steps for a ticket.

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "step-uuid",
      "ticket_id": "ticket-uuid",
      "step_number": "1.0.0",
      "title": "Step 1",
      "description": "Step description",
      "status": "not_started",
      "assigned_to": "user-uuid",
      "created_by": "user-uuid",
      "created_at": "2024-01-15T10:00:00Z",
      "completed_at": null,
      "due_date": null,
      "start_date": null,
      "level_1": 1,
      "level_2": 0,
      "level_3": 0,
      "parent_step_id": null,
      "is_parallel": true,
      "progress": 0,
      "dependencies": [],
      "dependency_mode": "all",
      "is_dependency_locked": false,
      "mandatory_documents": [],
      "optional_documents": []
    }
  ]
}
```

### POST /tickets/{ticketId}/steps

Create workflow step.

**Request:**
```json
{
  "title": "New Step",
  "description": "Step description",
  "status": "not_started",
  "assignedTo": "user-uuid",
  "level_1": 1,
  "level_2": 0,
  "level_3": 0,
  "parentStepId": null,
  "is_parallel": true,
  "progress": 0,
  "dependentOnStepIds": [],
  "userId": "user-uuid"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "new-step-uuid"
  }
}
```

### POST /tickets/{ticketId}/steps/bulk

Bulk create workflow steps.

**Request:**
```json
{
  "steps": [
    {
      "title": "Step 1",
      "description": "Description",
      "status": "not_started",
      "assignedTo": "user-uuid"
    }
  ],
  "userId": "user-uuid",
  "parentStepId": null
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "successCount": 1,
    "failedCount": 0,
    "totalCount": 1,
    "errors": [],
    "createdStepIds": ["step-uuid-1"]
  }
}
```

### PUT /steps/{id}

Update workflow step.

**Request:**
```json
{
  "title": "Updated title",
  "status": "wip",
  "progress": 50,
  "userId": "user-uuid",
  "remarks": "Progress update"
}
```

### PUT /steps/{id}/progress

Update step progress with files (multipart/form-data).

**Form Data:**
- `file`: File to upload
- `progress`: Progress percentage
- `comment`: Progress comment
- `userId`: User ID
- `ticketId`: Ticket ID

### DELETE /steps/{id}

Delete workflow step.

**Query Parameters:**
- `userId` (required): User ID performing deletion

---

## File Management Endpoints

### POST /files/upload

Upload file (multipart/form-data).

**Form Data:**
- `file`: File to upload
- `ticketId`: Ticket ID
- `userId`: User ID
- `stepId`: Step ID (optional)

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "file-uuid"
  }
}
```

### GET /files/{id}/download

Download file.

**Response:** Binary file data with appropriate headers.

### DELETE /files/{id}

Delete file.

**Query Parameters:**
- `userId` (required): User ID

### POST /files/progress-docs

Upload progress document (multipart/form-data).

**Form Data:**
- `file`: File to upload
- `stepId`: Step ID
- `ticketId`: Ticket ID
- `userId`: User ID
- `auditLogId`: Audit log ID

### POST /files/completion-cert

Upload completion certificate (multipart/form-data).

**Form Data:**
- `file`: File to upload
- `ticketId`: Ticket ID
- `userId`: User ID

---

## Finance Approval Endpoints

### POST /finance/submissions

Submit ticket for finance approval.

**Request (JSON or multipart if files included):**
```json
{
  "ticketId": "ticket-uuid",
  "requestedBy": "user-uuid",
  "estimatedCost": 50000,
  "remarks": "Cost estimate for materials"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "submission-uuid"
  }
}
```

### GET /finance/submissions

List finance submissions.

**Query Parameters:**
- `ticketId` (optional): Filter by ticket

### GET /finance/submissions/{id}

Get submission details.

### PUT /finance/submissions/{id}/approve

Approve finance submission.

**Request:**
```json
{
  "decidedBy": "user-uuid",
  "remarks": "Approved"
}
```

### PUT /finance/submissions/{id}/reject

Reject finance submission.

**Request:**
```json
{
  "decidedBy": "user-uuid",
  "remarks": "Budget exceeded"
}
```

---

## Dependency Endpoints

### POST /dependencies

Create dependencies.

**Request:**
```json
{
  "stepId": "step-uuid",
  "dependentOnStepIds": ["step-uuid-1", "step-uuid-2"],
  "createdBy": "user-uuid"
}
```

### GET /dependencies

List dependencies.

**Query Parameters:**
- `stepId` (required): Step ID

### DELETE /dependencies/{id}

Delete dependency.

**Query Parameters:**
- `deletedBy` (required): User ID

---

## Audit Log Endpoints

### POST /audit

Create audit log entry.

**Request:**
```json
{
  "ticketId": "ticket-uuid",
  "stepId": "step-uuid",
  "action": "WORKFLOW_UPDATED",
  "actionCategory": "workflow_action",
  "description": "Step status changed",
  "performedBy": "user-uuid",
  "oldData": "old_value",
  "newData": "new_value",
  "metadata": {}
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "audit-log-uuid"
  }
}
```

---

## CORS Configuration

Backend must enable CORS with these headers:

```
Access-Control-Allow-Origin: https://your-frontend-domain.com
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Authorization
Access-Control-Max-Age: 3600
```

## Data Types

### User Roles

- `eo` - Executive Officer
- `dept_officer` - Department Officer
- `employee` - Employee
- `vendor` - Vendor
- `finance` - Finance Officer

### Ticket/Step Status

- `open` - Open
- `wip` - Work In Progress
- `completed` - Completed
- `cancelled` - Cancelled
- `not_started` - Not Started

### Priority Levels

- `low`
- `medium`
- `high`
- `critical`

### Audit Action Categories

- `ticket_action` - Ticket-related actions
- `workflow_action` - Workflow-related actions
- `status_change` - Status changes
- `assignment_change` - Assignment changes
- `progress_update` - Progress updates
- `file_upload` - File uploads
- `comment` - Comments

---

## Testing Endpoints

Use tools like Postman or curl to test endpoints:

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'

# List tickets (with token)
curl -X GET "http://localhost:8080/api/tickets?moduleId=xxx" \
  -H "Authorization: Bearer {token}"
```

## Notes

1. All IDs are UUIDs
2. All dates are ISO 8601 format (UTC)
3. File uploads use multipart/form-data
4. All other requests use application/json
5. Implement proper error handling for all scenarios
6. Add request logging for debugging
7. Implement rate limiting to prevent abuse
