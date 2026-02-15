# Implementation Plan: /profile/edit Page for Employees

## Executive Summary

This plan synthesizes recommendations from 4 specialized agents (workflow-architect, ui-designer, ux-research-analyst, nextjs-dev) to create a comprehensive `/profile/edit` page for employee users in the SkillSoft HR assessment platform.

**Key Findings:**
- Existing profile infrastructure at `app/(workspace)/profile/` provides excellent foundation
- Sidebar already includes "Мой профиль" link for user lens at `/profile`
- Profile API, store, and types are already implemented
- Design should follow existing patterns from `ProfileHeroCard`, `QuickStatsGrid`, etc.

---

## Phase 1: Route and Sidebar Configuration

### 1.1 Update Lens Configuration

**File:** `frontend-app/src/config/lens-configs.ts`

Add `/profile/edit` to user lens visible routes:

```typescript
// Line ~94-99: Add to user lens visibleRoutes
visibleRoutes: [
  ROUTE_DASHBOARD,
  ROUTE_MY_TESTS,
  ROUTE_TESTS,
  ROUTE_PROFILE,
  '/profile/edit',  // NEW: Add edit route
],
```

### 1.2 Update Sidebar Navigation

**File:** `frontend-app/src/components/layout/app-sidebar.tsx`

The sidebar already has a "Мой профиль" link at line ~444-467. The edit button will be accessible from the profile page itself via `ProfileHeroCard`.

---

## Phase 2: Page Structure (Next.js 16 Optimized)

### 2.1 File Structure

```
frontend-app/app/(workspace)/profile/edit/
├── page.tsx                        # Server Component - auth, data fetch
├── loading.tsx                     # Loading skeleton
├── error.tsx                       # Error boundary
├── _components/
│   ├── ProfileEditContent.tsx      # Client - main tabs container
│   ├── AccountInfoSection.tsx      # Client - personal info form
│   ├── PreferencesSection.tsx      # Client - notification settings
│   ├── TestResultsSummarySection.tsx  # Server - read-only results
│   ├── CompetencyPassportSection.tsx  # Server - read-only passport
│   └── ProfileEditSkeleton.tsx     # Loading state
└── _actions/
    └── profile-actions.ts          # Server actions for form
```

### 2.2 Server vs Client Component Strategy

| Component | Type | Reason |
|-----------|------|--------|
| `page.tsx` | Server | Auth check, initial data fetch |
| `ProfileEditContent.tsx` | Client | Tab navigation, form state |
| `AccountInfoSection.tsx` | Client | Form interactions |
| `PreferencesSection.tsx` | Client | Toggle switches |
| `TestResultsSummarySection.tsx` | Server | Read-only, can stream |
| `CompetencyPassportSection.tsx` | Server | Read-only, can stream |

### 2.3 Data Fetching Strategy

```typescript
// page.tsx - Server Component
export const revalidate = 60; // ISR: 60 seconds

export default async function ProfileEditPage() {
  const user = await currentUser();
  if (!user) redirect('/sign-in');

  // Preload data for Suspense boundaries
  preloadProfileData(user.id);

  // Extract user info for form (from Clerk)
  const userInfo: ProfileUserInfo = {
    clerkId: user.id,
    email: user.emailAddresses[0]?.emailAddress || '',
    firstName: user.firstName || '',
    lastName: user.lastName || '',
    avatarUrl: user.imageUrl,
    organizationName: (user.publicMetadata?.organization as string) || null,
    createdAt: new Date(user.createdAt),
  };

  return (
    <div className="min-h-screen bg-background">
      <div className="container max-w-5xl mx-auto px-4 py-6 sm:py-8">
        <Suspense fallback={<ProfileEditSkeleton />}>
          <ProfileEditContent userInfo={userInfo} />
        </Suspense>
      </div>
    </div>
  );
}
```

---

## Phase 3: UI Design Specification

### 3.1 Page Layout

**Desktop (lg: 1024px+):**
```
+----------------------------------------------------------+
|  [<] Редактировать профиль                               |
|      Обновите информацию о себе и настройки              |
+----------------------------------------------------------+
|                                                          |
|  [ Аккаунт ][ Результаты ][ Паспорт ]                   |
|  --------------------------------------------------------|
|                                                          |
|  +------------------------+  +------------------------+  |
|  |  Personal Info Form    |  |  Preferences Card      |  |
|  |  - Avatar (view only)  |  |  - Language            |  |
|  |  - First name          |  |  - Notifications       |  |
|  |  - Last name           |  |                        |  |
|  |  - Email (read-only)   |  +------------------------+  |
|  +------------------------+                              |
|                                                          |
|                        [ Отмена ]  [ Сохранить ]         |
+----------------------------------------------------------+
```

**Mobile (< 768px):**
```
+---------------------------+
| [<] Редактировать профиль |
+---------------------------+
|                           |
| [Аккаунт][Рез-ты][Пасп]  |  <- Scrollable tabs
| ----------------------------|
|                           |
| +------------------------+|
| | Avatar + Name fields   ||
| +------------------------+|
|                           |
| +------------------------+|
| | Preferences (Accordion)||
| +------------------------+|
|                           |
+---------------------------+
| [Отмена]    [Сохранить]  |  <- Sticky footer
+---------------------------+
```

### 3.2 Tab Structure

```typescript
<Tabs defaultValue="account" className="space-y-6">
  <TabsList className="grid w-full grid-cols-3">
    <TabsTrigger value="account">
      <User className="h-4 w-4 mr-2" />
      <span className="hidden sm:inline">Аккаунт</span>
    </TabsTrigger>
    <TabsTrigger value="results">
      <ClipboardCheck className="h-4 w-4 mr-2" />
      <span className="hidden sm:inline">Результаты</span>
    </TabsTrigger>
    <TabsTrigger value="passport">
      <Brain className="h-4 w-4 mr-2" />
      <span className="hidden sm:inline">Паспорт</span>
    </TabsTrigger>
  </TabsList>

  <TabsContent value="account">
    <AccountInfoSection userInfo={userInfo} />
  </TabsContent>

  <TabsContent value="results">
    {/* Reuse QuickStatsGrid + RecentResultsSection */}
  </TabsContent>

  <TabsContent value="passport">
    {/* Reuse PersonalityPassportCard + TopCompetenciesCard */}
  </TabsContent>
</Tabs>
```

### 3.3 Component Reuse Strategy

| Section | Existing Component | Modification |
|---------|-------------------|--------------|
| Test Results | `QuickStatsGrid` | None - import as-is |
| Test Results | `RecentResultsSection` | None - import as-is |
| Competency Passport | `PersonalityPassportCard` | None - import as-is |
| Competency Passport | `TopCompetenciesCard` | None - import as-is |

---

## Phase 4: Form Implementation

### 4.1 Validation Schema (Zod)

```typescript
const profileEditSchema = z.object({
  firstName: z
    .string()
    .min(1, 'Имя обязательно')
    .max(50, 'Имя не должно превышать 50 символов'),
  lastName: z
    .string()
    .min(1, 'Фамилия обязательна')
    .max(50, 'Фамилия не должна превышать 50 символов'),
  organization: z
    .string()
    .max(100, 'Название организации не должно превышать 100 символов')
    .optional(),
  language: z.enum(['ru', 'en']).default('ru'),
  emailNotifications: z.boolean().default(true),
});

type ProfileEditFormData = z.infer<typeof profileEditSchema>;
```

### 4.2 Form Fields

| Field | Type | Editable | Source |
|-------|------|----------|--------|
| Avatar | Image | No (managed by Clerk) | Clerk |
| First Name | Text | Yes | Clerk |
| Last Name | Text | Yes | Clerk |
| Email | Text | No | Clerk |
| Organization | Text | Yes | Clerk publicMetadata |
| Language | Select | Yes | User preferences |
| Email Notifications | Switch | Yes | User preferences |

### 4.3 Server Action

```typescript
// _actions/profile-actions.ts
'use server';

import { revalidatePath, revalidateTag } from 'next/cache';
import { clerkClient } from '@clerk/nextjs/server';

export async function updateProfileAction(
  clerkId: string,
  data: ProfileEditFormData
): Promise<{ success: boolean; message: string }> {
  try {
    const client = await clerkClient();

    // Update Clerk user
    await client.users.updateUser(clerkId, {
      firstName: data.firstName,
      lastName: data.lastName,
      publicMetadata: {
        organization: data.organization,
        preferences: {
          language: data.language,
          emailNotifications: data.emailNotifications,
        },
      },
    });

    // Invalidate caches
    revalidatePath('/profile');
    revalidatePath('/profile/edit');
    revalidatePath('/dashboard');
    revalidateTag('users');
    revalidateTag(`user-clerk-${clerkId}`);

    return { success: true, message: 'Профиль успешно обновлен' };
  } catch (error) {
    return {
      success: false,
      message: error instanceof Error ? error.message : 'Ошибка при обновлении профиля'
    };
  }
}
```

---

## Phase 5: Mobile Responsiveness

### 5.1 Breakpoint Strategy

| Breakpoint | Width | Layout Changes |
|------------|-------|----------------|
| Default | < 640px | Single column, stacked layout, sticky footer |
| sm | >= 640px | 2-column form grids |
| md | >= 768px | Larger touch targets, side-by-side cards |
| lg | >= 1024px | Full 2-column layout with sidebar |

### 5.2 Mobile Optimizations

```typescript
// Sticky action footer on mobile
<div className="fixed bottom-0 left-0 right-0 p-4 bg-background border-t
                md:relative md:border-0 md:p-0 md:mt-6
                safe-area-bottom">
  <div className="flex gap-3 justify-end">
    <Button variant="outline">Отмена</Button>
    <Button>Сохранить</Button>
  </div>
</div>

// Accordion for preferences on mobile
<div className="block md:hidden">
  <Accordion type="single" collapsible>
    <AccordionItem value="preferences">
      <AccordionTrigger>Настройки</AccordionTrigger>
      <AccordionContent>{/* Preferences fields */}</AccordionContent>
    </AccordionItem>
  </Accordion>
</div>

// Cards visible on desktop
<div className="hidden md:block">
  <Card>{/* Preferences fields */}</Card>
</div>
```

### 5.3 Touch Targets

```css
/* Minimum touch target size */
.touch-target {
  min-height: 44px;
  min-width: 44px;
}

/* Mobile inputs prevent iOS zoom */
input, select, textarea {
  font-size: 16px;
}
```

---

## Phase 6: UX Considerations

### 6.1 Information Architecture

```
/profile/edit (edit mode)
  |
  +-- EditProfileHeader
  |     +-- Back to profile (ArrowLeft)
  |     +-- Save/Cancel actions
  |     +-- Unsaved changes indicator
  |
  +-- Tab: Аккаунт
  |     +-- Avatar (view only, link to Clerk)
  |     +-- Display name fields
  |     +-- Email (read-only)
  |     +-- Organization (optional)
  |
  +-- Tab: Результаты (read-only)
  |     +-- QuickStatsGrid
  |     +-- RecentResultsSection
  |
  +-- Tab: Паспорт (read-only)
        +-- PersonalityPassportCard
        +-- TopCompetenciesCard
```

### 6.2 User Journey

1. **Entry**: User clicks "Редактировать" on ProfileHeroCard
2. **Orientation**: Page loads with Account tab active
3. **Edit**: User modifies name/organization fields
4. **Review**: Optional check of Results/Passport tabs
5. **Save**: Click "Сохранить", see success message
6. **Return**: Automatic redirect to /profile after save

### 6.3 Error Handling

- Inline field validation with immediate feedback
- Form-level error alert for server errors
- Network error detection with retry option
- Auth error redirect to sign-in

### 6.4 Accessibility

- All form fields have visible labels
- Error messages announced to screen readers
- Focus management on tab switch
- Color contrast meets WCAG AA (4.5:1)
- Keyboard navigation works throughout

---

## Phase 7: Implementation Tasks

### Task 1: Create Page Structure
- [ ] Create `app/(workspace)/profile/edit/page.tsx`
- [ ] Create `loading.tsx` with skeleton
- [ ] Create `error.tsx` with retry logic

### Task 2: Implement Components
- [ ] Create `_components/ProfileEditContent.tsx` (Client)
- [ ] Create `_components/AccountInfoSection.tsx` (Client)
- [ ] Create `_components/PreferencesSection.tsx` (Client)
- [ ] Create `_components/ProfileEditSkeleton.tsx`

### Task 3: Create Server Actions
- [ ] Create `_actions/profile-actions.ts`
- [ ] Implement `updateProfileAction`
- [ ] Add cache invalidation logic

### Task 4: Update Existing Files
- [ ] Update `ProfileHeroCard.tsx` edit button link to `/profile/edit`
- [ ] Update `lens-configs.ts` if needed

### Task 5: Testing
- [ ] Test form submission
- [ ] Test mobile responsiveness
- [ ] Test tab navigation
- [ ] Test error states
- [ ] Test auth redirect

---

## Phase 8: Files to Create

### 8.1 `page.tsx` (Server Component)

Key features:
- Auth check with `currentUser()`
- Data preloading with `preloadProfileData()`
- Suspense boundaries for streaming
- ISR with 60-second revalidation

### 8.2 `loading.tsx`

Key features:
- Skeleton matching final layout
- Maintains CLS < 0.1
- Matches page header style

### 8.3 `error.tsx`

Key features:
- Network error detection
- Auth error handling
- Retry mechanism
- Return to profile link

### 8.4 `ProfileEditContent.tsx` (Client Component)

Key features:
- Tabs component with 3 tabs
- Form state management with react-hook-form
- Zod validation
- useTransition for optimistic updates
- Success/error message display

### 8.5 `AccountInfoSection.tsx` (Client Component)

Key features:
- Avatar display (not editable)
- Name fields with validation
- Email (read-only)
- Organization field

### 8.6 `profile-actions.ts` (Server Action)

Key features:
- Clerk user update
- Cache invalidation
- Error handling

---

## Summary

This implementation follows existing SkillSoft patterns while adding:

1. **Tab-based navigation** for Account/Results/Passport sections
2. **Server Components** for data fetching + **Client Components** for forms
3. **Mobile-first responsive design** with sticky footer and accordion
4. **Clerk SDK integration** for profile updates
5. **Component reuse** from existing profile page (QuickStatsGrid, PersonalityPassportCard, etc.)
6. **Proper cache invalidation** after updates
7. **Accessibility compliance** (WCAG 2.1 AA)
8. **Bilingual support** (Russian primary, English labels)

**Estimated Complexity:** Medium
**Reuse Factor:** High (leverages existing profile infrastructure)
**Mobile Adaptation:** Complete (breakpoint strategy defined)
