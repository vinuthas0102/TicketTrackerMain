# Testing Guide: Workflow Steps Endpoints

## Quick Verification Steps

### 1. Start the Java Backend
```bash
cd ticket-tracker-java
./build.sh  # or build.bat on Windows
# Deploy the WAR to Tomcat and start it
```

### 2. Start the React Frontend
```bash
cd ticket-tracker-rest/frontend
npm install  # if not already done
npm run dev
```

### 3. Test Using Browser Console

Once logged in to the application, open the browser console (F12) and run:

#### Test 1: Fetch all steps for a ticket
```javascript
// Replace with an actual ticket ID from your system
const ticketId = 'your-ticket-id-here';

TicketService.getWorkflowSteps(ticketId)
  .then(steps => {
    console.log('✅ Successfully fetched steps:', steps);
    console.log('Number of steps:', steps.length);
  })
  .catch(error => {
    console.error('❌ Error:', error);
  });
```

#### Test 2: Fetch a single step
```javascript
// Replace with an actual step ID from your system
const stepId = 'your-step-id-here';

TicketService.getWorkflowStep(stepId)
  .then(step => {
    console.log('✅ Successfully fetched step:', step);
    console.log('Step title:', step.title);
    console.log('Step status:', step.status);
  })
  .catch(error => {
    console.error('❌ Error:', error);
  });
```

### 4. Test Using Network Tab

1. Open Developer Tools (F12) → Network Tab
2. Filter by "XHR" or "Fetch"
3. Perform an action that triggers workflow step loading
4. Look for requests to:
   - `GET /api/workflow-steps?ticketId={id}` - Should return 200 OK with array of steps
   - `GET /api/workflow-steps/{id}` - Should return 200 OK with single step object

### 5. Expected Responses

#### GET /api/workflow-steps?ticketId={id}
**Success (200):**
```json
[
  {
    "id": "...",
    "ticket_id": "...",
    "title": "Step Title",
    "description": "Step Description",
    "status": "not_started",
    "assigned_to": "...",
    "progress": 0,
    "level_1": 1,
    "level_2": 0,
    "level_3": 0,
    "created_at": "2024-01-01T00:00:00Z",
    ...
  }
]
```

**Error (400):**
```json
{
  "status": 400,
  "message": "ticketId parameter required"
}
```

#### GET /api/workflow-steps/{id}
**Success (200):**
```json
{
  "id": "...",
  "ticket_id": "...",
  "title": "Step Title",
  "status": "not_started",
  ...
}
```

**Error (404):**
```json
{
  "status": 404,
  "message": "Workflow step not found"
}
```

## Testing Checklist

- [ ] Backend servlet responds to `/api/workflow-steps?ticketId={id}`
- [ ] Backend servlet responds to `/api/workflow-steps/{id}`
- [ ] Frontend service method `getWorkflowSteps()` returns array
- [ ] Frontend service method `getWorkflowStep()` returns single object
- [ ] Data transformation works correctly (dates, status, etc.)
- [ ] Error handling works for invalid IDs
- [ ] Error handling works for missing parameters
- [ ] Authentication is required for protected endpoints

## Common Issues and Solutions

### Issue: 404 Not Found
**Cause:** Servlet not deployed or URL mapping incorrect
**Solution:** Verify `@WebServlet("/api/workflow-steps/*")` annotation and redeploy

### Issue: 400 Bad Request - "ticketId parameter required"
**Cause:** Missing ticketId query parameter
**Solution:** Ensure the endpoint is called with `?ticketId={id}`

### Issue: 401 Unauthorized
**Cause:** Session expired or not authenticated
**Solution:** Re-login to the application

### Issue: CORS Error
**Cause:** CORS filter not configured
**Solution:** Uncomment CORS filter in web.xml or verify CorsFilter configuration

## Integration Points

These methods can be integrated into:

1. **TicketView Component** - Refresh workflow steps without reloading entire ticket
2. **StepManagement Component** - Load latest step data
3. **Dashboard** - Display step statistics
4. **Real-time Updates** - Poll for step changes
5. **Modals/Dialogs** - Load detailed step information

## Example Integration

### In a React Component:
```typescript
import { TicketService } from '../services/ticketService';
import { useEffect, useState } from 'react';

function WorkflowStepsPanel({ ticketId }) {
  const [steps, setSteps] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    async function loadSteps() {
      try {
        setLoading(true);
        const data = await TicketService.getWorkflowSteps(ticketId);
        setSteps(data);
        setError(null);
      } catch (err) {
        setError(err.message);
        console.error('Failed to load steps:', err);
      } finally {
        setLoading(false);
      }
    }

    if (ticketId) {
      loadSteps();
    }
  }, [ticketId]);

  if (loading) return <div>Loading steps...</div>;
  if (error) return <div>Error: {error}</div>;

  return (
    <div>
      <h3>Workflow Steps ({steps.length})</h3>
      {steps.map(step => (
        <div key={step.id}>
          {step.title} - {step.status}
        </div>
      ))}
    </div>
  );
}
```

## Performance Considerations

- **Caching:** Consider caching step data to reduce API calls
- **Polling:** If using polling, set appropriate intervals (30-60 seconds)
- **Batch Loading:** Use the embedded steps in ticket response when loading initial ticket data
- **Independent Updates:** Use these methods when you need to refresh steps without reloading the entire ticket
