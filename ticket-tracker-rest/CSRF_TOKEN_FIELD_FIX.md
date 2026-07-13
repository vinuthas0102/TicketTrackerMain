# CSRF Token Field Name Mismatch Fix

## Issue
The CSRF token was returning `null` in all POST/PUT/DELETE requests, causing 403 Forbidden errors. The root cause was a field name mismatch between frontend and backend.

## Root Cause
- **Backend** (`CSRFTokenFilter.java`): Returns CSRF token with field name `csrfToken`
- **Frontend** (`authService.ts` & `apiClient.ts`): Was looking for field name `token`

This mismatch meant the token was never extracted and stored, so `getCsrfToken()` always returned `null`.

## Changes Made

### 1. Fixed AuthService CSRF Token Fetch
**File**: `frontend/src/services/authService.ts`

```typescript
// Before:
const response = await apiClient.get<{ token: string }>(API_ENDPOINTS.AUTH.CSRF_TOKEN);
if (response && response.token) {
  setCsrfToken(response.token);
}

// After:
const response = await apiClient.get<{ csrfToken: string }>(API_ENDPOINTS.AUTH.CSRF_TOKEN);
console.log('[AuthService] CSRF token response:', response);
if (response && response.csrfToken) {
  setCsrfToken(response.csrfToken);
} else {
  console.warn('[AuthService] CSRF token not found in response:', response);
}
```

### 2. Fixed API Client CSRF Token Refresh
**File**: `frontend/src/lib/apiClient.ts`

```typescript
// Before:
if (data && data.token) {
  setCsrfToken(data.token);
  console.log('[ApiClient] CSRF token refreshed successfully');
  return true;
}

// After:
console.log('[ApiClient] CSRF token refresh response:', data);
if (data && data.csrfToken) {
  setCsrfToken(data.csrfToken);
  console.log('[ApiClient] CSRF token refreshed successfully');
  return true;
} else {
  console.warn('[ApiClient] CSRF token not found in refresh response:', data);
}
```

### 3. Added Defensive Logging
Added console logging to show:
- The actual response structure received from backend
- When token is successfully extracted and stored
- When token extraction fails with the actual response structure

## Testing

### Verify the Fix:
1. **Login**: After login, check browser console for `[AuthService] CSRF token fetched and stored successfully`
2. **Check Storage**: Run `localStorage.getItem('csrf_token')` in browser console - should return a token value
3. **Create Ticket**: Try creating a ticket - should work without 403 errors
4. **Check Headers**: Open Network tab, create a ticket, and verify the request has `X-CSRF-Token` header

### Expected Console Output:
```
[AuthService] CSRF token response: { csrfToken: "abc123..." }
[AuthService] CSRF token fetched and stored successfully
```

## Impact
This fix resolves all 403 Forbidden errors when performing POST/PUT/DELETE operations after login. The CSRF token is now properly fetched, stored, and included in protected requests.

## Build Status
✅ Frontend build successful with no errors
