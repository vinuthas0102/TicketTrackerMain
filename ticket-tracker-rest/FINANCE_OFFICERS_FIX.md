# Finance Officers Loading Fix

## Issue
When trying to use the "Send to Finance" option in the Change Ticket Status screen, finance officers were not loading, causing the error:
```
Error: Error loading finance officers: TypeError: FinanceApprovalService.getFinanceOfficers is not a function
```

## Root Cause
The REST version's `FinanceApprovalService` class was missing the `getFinanceOfficers()` method that the `StatusTransitionModal` component was trying to call.

## Solution Implemented
Added the `getFinanceOfficers()` method to the `FinanceApprovalService` class that:
1. Fetches all users from the backend via the existing `/api/users` endpoint
2. Filters users by role === 'FINANCE'
3. Maps them to the required format (id, name, email, department)
4. Sorts them alphabetically by name
5. Returns the filtered list to the component

## Files Modified
- **ticket-tracker-rest/frontend/src/services/financeApprovalService.ts**
  - Added `User` type import
  - Added `getFinanceOfficers()` static method

## Implementation Details

### Method Signature
```typescript
static async getFinanceOfficers(): Promise<Array<{
  id: string;
  name: string;
  email: string;
  department: string
}>>
```

### Key Features
- Uses existing users API endpoint (no backend changes required)
- Client-side filtering for FINANCE role users
- Alphabetically sorted by name
- Handles errors gracefully with try-catch
- Returns empty array handled by throwing error for proper UI feedback

## Testing Steps
1. Open a ticket with status "ACTIVE"
2. Click "Change Status" button
3. Select "Send to Finance" from the dropdown
4. Verify:
   - Finance officers dropdown populates with names
   - No console errors appear
   - All finance role users are listed alphabetically

## Notes
- This implementation uses the existing users endpoint with client-side filtering
- No backend changes were required
- If performance becomes an issue with many users, a dedicated backend endpoint can be added later
- The method matches the signature expected by `StatusTransitionModal.tsx:52`
