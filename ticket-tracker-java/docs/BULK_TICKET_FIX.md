# Bulk Ticket Creation Fix

## Issue
The bulk ticket creation endpoint was failing with a JSON deserialization error when the frontend sent requests to create multiple tickets at once.

## Root Cause
The frontend was sending a request body with this structure:
```json
{
  "tickets": [...],
  "moduleId": "...",
  "createdBy": "..."
}
```

However, the Java backend was trying to deserialize the body directly as a `List<Ticket>`, which caused Jackson to fail with the error:
```
Cannot deserialize value of type `java.util.ArrayList<com.tickettracker.model.Ticket>` from Object value
```

## Solution
Created a new request wrapper model to match the frontend's request structure.

### Changes Made

1. **Created `BulkTicketCreateRequest.java`**
   - Location: `/src/main/java/com/tickettracker/model/BulkTicketCreateRequest.java`
   - Purpose: Wrapper class to properly deserialize the bulk ticket creation request
   - Fields:
     - `List<Ticket> tickets` - The list of tickets to create
     - `String moduleId` - The module ID (as hex string)
     - `String createdBy` - The user ID who is creating the tickets (for reference)

2. **Updated `TicketServlet.java`**
   - Added import for `BulkTicketCreateRequest`
   - Modified `handleBulkCreate()` method to:
     - Deserialize the request body as `BulkTicketCreateRequest` instead of `List<Ticket>`
     - Extract the `moduleId` from the request
     - Convert the `moduleId` from hex string to byte array
     - Apply the `moduleId` to each ticket if not already set
     - Pass the tickets list to the service layer

### Code Changes

**TicketServlet.handleBulkCreate()** (lines 93-113):
```java
private void handleBulkCreate(HttpServletRequest request, HttpServletResponse response, User currentUser)
        throws IOException, TicketTrackerException {
    String body = getRequestBody(request);
    BulkTicketCreateRequest bulkRequest = objectMapper.readValue(body, BulkTicketCreateRequest.class);

    byte[] moduleIdBytes = null;
    if (bulkRequest.getModuleId() != null && !bulkRequest.getModuleId().isEmpty()) {
        moduleIdBytes = hexToBytes(bulkRequest.getModuleId());
    }

    for (Ticket ticket : bulkRequest.getTickets()) {
        if (ticket.getModuleId() == null && moduleIdBytes != null) {
            ticket.setModuleId(moduleIdBytes);
        }
    }

    List<Ticket> createdTickets = ticketService.createTicketsBulk(bulkRequest.getTickets(), currentUser.getId());

    response.setStatus(HttpServletResponse.SC_CREATED);
    sendJsonResponse(response, createdTickets);
}
```

## Testing
After deploying these changes:
1. The bulk ticket creation endpoint should properly deserialize the request
2. All tickets will be created with the correct module ID
3. The endpoint will return the created tickets with HTTP 201 status

## API Contract
**Endpoint**: `POST /api/tickets/bulk`

**Request Body**:
```json
{
  "tickets": [
    {
      "title": "Ticket 1",
      "description": "Description 1",
      "priority": "high",
      "assignedTo": "user-id-hex",
      "dueDate": "2024-12-31T23:59:59.000Z",
      ...
    },
    ...
  ],
  "moduleId": "module-id-as-hex-string",
  "createdBy": "user-id-hex"
}
```

**Response**: Array of created tickets with HTTP 201 status

## Files Modified
1. `/src/main/java/com/tickettracker/model/BulkTicketCreateRequest.java` (new file)
2. `/src/main/java/com/tickettracker/servlet/TicketServlet.java` (modified)

## Deployment
1. Rebuild the project: `./build.sh --skip-tests`
2. Deploy the WAR file to Tomcat
3. Restart the application server
4. Test the bulk ticket creation feature
