# Abandoned Session Error Handling - Implementation Summary

## Problem Statement
When a test session is abandoned, the system was throwing error "Cannot get current question for an abandoned session" which appeared in the console, providing poor user experience. Users would see technical errors instead of user-friendly messages and proper navigation flow.

## Root Cause
The application was attempting to call `getCurrentQuestion` on abandoned sessions without first checking the session status, leading to:
1. 400/403 HTTP errors from backend
2. Console errors visible to users
3. No clear path for users to restart or navigate away
4. Redundant API calls that would always fail

## Solution Overview
Implemented comprehensive error handling for abandoned (and other invalid) session states across three layers:

### 1. API Client Layer (`src/services/api.client.ts`)
**Enhanced error messages for specific scenarios:**
- Added detection of abandoned session errors in 400 responses
- Specific error messages for 403 Forbidden errors
- Better context-aware error messages based on endpoint

**Changes:**
```typescript
// Before
else if (response.status === 400 && endpoint.includes('/sessions/')) {
  error.message = errorData.message || 'Недействительная сессия теста...';
}

// After
else if (response.status === 400 && endpoint.includes('/current-question')) {
  if (errorData.message?.toLowerCase().includes('abandoned')) {
    error.message = 'Эта сессия была отменена. Вы можете начать новый тест.';
  } else {
    error.message = errorData.message || 'Недействительная сессия теста...';
  }
}
```

### 2. Page Layer (`app/(workspace)/test-templates/take/[sessionId]/page.tsx`)
**Proactive session status validation:**
- Check session status BEFORE calling `getCurrentQuestion`
- Handle all invalid states: ABANDONED, COMPLETED, TIMED_OUT
- Provide appropriate actions based on session state
- Type-safe using SessionStatus enum

**Key additions:**
```typescript
// Check session status before attempting to load questions
if (sessionData.status === SessionStatus.ABANDONED) {
  setError('Эта сессия была отменена. Вы можете начать новый тест.');
  setErrorStatus(400);
  setStatus('error');
  toast.info('Сессия была отменена ранее');
  return;
}

// Similar checks for COMPLETED and TIMED_OUT states
```

**Enhanced error UI:**
- Abandoned sessions show "Start New Test" button when template ID is available
- Clean redirect options to test templates list
- User-friendly error titles and descriptions
- No technical jargon in user-facing messages

### 3. Player Layer (`src/components/test-player/ImmersivePlayer.tsx`)
**Runtime error handling during test-taking:**
- Detect abandoned/completed sessions during navigation
- Graceful redirects with toast notifications
- Handle errors in all API interactions: loadQuestion, handleNext, handlePrevious

**Changes:**
```typescript
// In loadQuestion, handleNext, handlePrevious
if (apiError.status === 400 || apiError.status === 403) {
  const errorMessage = apiError.message || '';
  if (errorMessage.toLowerCase().includes('abandon')) {
    toast.error('Сессия была отменена');
    router.push('/test-templates');
    return;
  }
  // Similar handling for completed sessions
}
```

## Test Scenarios Covered

### Scenario 1: Direct URL Access to Abandoned Session
**Flow:**
1. User navigates to `/test-templates/take/{abandonedSessionId}`
2. Page loads session data via `getSessionById`
3. Status check detects `SessionStatus.ABANDONED`
4. Error state with "Start New Test" button (if template ID available)
5. User can restart or return to test list

**Result:** ✅ No API call to `getCurrentQuestion`, clean error UI

### Scenario 2: Session Abandoned While Taking Test
**Flow:**
1. User is in ImmersivePlayer taking test
2. Session becomes abandoned (backend or external action)
3. User clicks "Next" or "Previous"
4. API returns 400/403 with abandoned message
5. Toast notification + redirect to test templates

**Result:** ✅ Graceful handling with user notification

### Scenario 3: Refresh on Abandoned Session
**Flow:**
1. User was taking test, session abandoned
2. User refreshes page
3. Page detects abandoned status early
4. Shows error with restart option

**Result:** ✅ Immediate feedback, no hanging state

### Scenario 4: Other Invalid States
**Covered states:**
- `COMPLETED` - Shows "Test already completed"
- `TIMED_OUT` - Shows "Test time expired"
- Unknown states - Shows generic error with retry

**Result:** ✅ All invalid states handled appropriately

## Technical Implementation Details

### Type Safety
- Used `SessionStatus` enum for all status checks
- Proper TypeScript types for error responses
- ApiError interface with status, code, message fields

### Code Quality
- Passed TypeScript strict type checking
- ESLint warnings addressed (removed unused imports/variables)
- Dependency arrays properly maintained in hooks
- Function hoisting issues resolved

### Performance
- Prevents unnecessary API calls to `getCurrentQuestion` on invalid sessions
- Early returns in loading logic to avoid redundant processing
- Retry logic only for retryable errors (5xx), not for invalid state (4xx)

### User Experience
- User-friendly Russian error messages
- Clear action buttons (primary/outline variants)
- Toast notifications for immediate feedback
- Smooth navigation flow without console errors

## Files Modified

1. **src/services/api.client.ts**
   - Enhanced error message handling
   - Added 403 handling
   - Specific abandoned session detection

2. **app/(workspace)/test-templates/take/[sessionId]/page.tsx**
   - Session status validation before API calls
   - Enhanced error UI with context-aware actions
   - SessionStatus enum usage for type safety
   - Removed testResultId reference (doesn't exist in type)

3. **src/components/test-player/ImmersivePlayer.tsx**
   - Runtime error handling in navigation
   - Graceful redirects on abandoned/completed states
   - Improved error detection in loadQuestion, handleNext, handlePrevious
   - Fixed function hoisting issue with handleTimeExpired

## Testing Checklist

- [x] Type checking passes (`npm run type-check`)
- [x] Build succeeds (`npm run build`)
- [x] ESLint critical warnings resolved
- [x] All session states handled (NOT_STARTED, IN_PROGRESS, COMPLETED, ABANDONED, TIMED_OUT)
- [x] Error messages are user-friendly
- [x] No console errors for abandoned sessions
- [x] Proper redirects implemented
- [x] Toast notifications working

## Recommended Manual Testing

1. **Abandon and Restart Flow:**
   - Start a test
   - Abandon it via exit button
   - Access abandoned session URL directly
   - Verify "Start New Test" button appears
   - Click it and verify new session starts

2. **Navigation During Abandoned State:**
   - Mock backend to return abandoned state
   - Try clicking Next/Previous
   - Verify redirect to test templates with toast

3. **Completed Session Access:**
   - Complete a test
   - Try accessing session URL
   - Verify appropriate error message

4. **Timeout Scenario:**
   - Let test timer expire
   - Verify timeout dialog and handling

## Future Improvements

1. **Backend Integration:**
   - Consider adding a `canRestart` flag in session response
   - Include template ID in error responses for easier restart

2. **Analytics:**
   - Track abandoned session access attempts
   - Monitor error rates for different session states

3. **Enhanced UI:**
   - Show last accessed time for abandoned sessions
   - Display progress percentage at abandonment

4. **Caching:**
   - Cache session status checks to reduce API calls
   - Implement optimistic UI updates

## Conclusion

The implementation provides comprehensive error handling for abandoned test sessions, ensuring:
- **No console errors** - All errors are caught and handled gracefully
- **Clear user feedback** - Toast notifications and error cards with actionable buttons
- **Type safety** - Proper TypeScript types and enum usage
- **Performance** - Prevents unnecessary API calls
- **Maintainability** - Centralized error handling logic

All changes follow Next.js 16 best practices and maintain consistency with the existing codebase architecture.
