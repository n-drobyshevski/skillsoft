# PPR via Cache Components Migration Plan

**Project:** SkillSoft Frontend (Next.js 16.0.7)
**Branch:** `feat/ppr-cache-components`
**Author:** Principal Architect
**Date:** 2026-02-10
**Status:** COMPLETE (All 7 phases implemented)

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Current State Analysis](#2-current-state-analysis)
3. [Target Architecture](#3-target-architecture)
4. [Phase 0: Prerequisites and Verification](#4-phase-0-prerequisites-and-verification)
5. [Phase 1: Clerk Provider Restructuring](#5-phase-1-clerk-provider-restructuring)
6. [Phase 2: Workspace Layout Server Migration](#6-phase-2-workspace-layout-server-migration)
7. [Phase 3: Cache Directive Migration (unstable_cache to use cache)](#7-phase-3-cache-directive-migration)
8. [Phase 4: Route Segment Config Migration](#8-phase-4-route-segment-config-migration)
9. [Phase 5: Enable cacheComponents and PPR](#9-phase-5-enable-cachecomponents-and-ppr)
10. [Phase 6: Invalidation Modernization (updateTag)](#10-phase-6-invalidation-modernization)
11. [Risk Register](#11-risk-register)
12. [Rollback Playbook](#12-rollback-playbook)
13. [Testing Strategy](#13-testing-strategy)
14. [Appendix: File Change Inventory](#14-appendix-file-change-inventory)

---

## 1. Executive Summary

This plan migrates SkillSoft's Next.js 16 frontend from `cacheComponents: false` to `cacheComponents: true`, enabling Partial Prerendering (PPR) as the default rendering strategy. PPR allows the static shell of every page to be served instantly from CDN while dynamic content streams in via Suspense boundaries.

**Expected outcomes:**

- Time to First Byte (TTFB) reduced by 40-60% on workspace pages (static shell served from edge)
- Largest Contentful Paint (LCP) improved for dashboard, entity lists, and docs pages
- Elimination of the deprecated `unstable_cache()` API in favor of the stable `'use cache'` directive
- Removal of all route segment config overrides (`dynamic`, `revalidate`, `fetchCache`)

**Estimated effort:** 5-7 working days across 7 phases, each independently deployable.

**Critical dependency:** Clerk `@clerk/nextjs` v6.35.1+ (already satisfied -- current version is `^6.35.1`).

---

## 2. Current State Analysis

### 2.1 Configuration

| Setting | Current Value | Target Value |
|---------|---------------|--------------|
| `cacheComponents` | `false` | `true` |
| `experimental.ppr` | commented out | REMOVED (PPR is default with cacheComponents) |
| `cacheLife` profiles | 4 defined (realtime, entityData, userData, referenceData) | Keep + add `staticContent` |
| `experimental.staleTimes` | dynamic: 30, static: 180 | Keep as-is |

### 2.2 Auth Architecture

| Component | Pattern | PPR Impact |
|-----------|---------|------------|
| Root layout (`app/layout.tsx`) | `<ClerkProvider dynamic>` wraps entire app | Must restructure -- wrapping everything forces dynamic |
| Workspace layout (`app/(workspace)/layout.tsx`) | `"use client"` + `useAuth()` | Must convert to Server Component + server-side auth |
| Auth layout (`app/(auth)/layout.tsx`) | Server Component + `connection()` in Suspense | Already PPR-ready |
| `proxy.ts` | `clerkMiddleware()` with route matchers | Already PPR-compatible (does not force dynamic rendering) |
| `LensInitializer.tsx` | Client Component using `useUser()` | Keep as Client Component inside Suspense |

### 2.3 Caching Architecture

| File | Pattern | Migration Target |
|------|---------|------------------|
| `src/services/api.cache.ts` | `unstable_cache()` with tags | `'use cache'` + `cacheLife()` + `cacheTag()` |
| `src/services/api.cache.dashboard.ts` | `unstable_cache()` with tags | `'use cache'` + `cacheLife()` + `cacheTag()` |
| `src/services/api.cache.my-tests.ts` | `unstable_cache()` with auth headers | `'use cache'` with auth param forwarding |
| `src/services/api.cache.psychometrics.ts` | React `cache()` for dedup | Keep `cache()` (request-scoped dedup is still valid) |
| `src/lib/cached-data.ts` | React `cache()` for dedup | Keep `cache()` (request-scoped dedup) |
| `src/app/actions.ts` | `revalidateTag()` + `revalidatePath()` | Add `updateTag()` for immediate invalidation where needed |

### 2.4 Route Segment Configs

| File | Config | Migration |
|------|--------|-----------|
| `app/(workspace)/test-templates/[id]/builder/layout.tsx` | `dynamic = 'force-dynamic'`, `fetchCache = 'default-cache'` | Remove all three exports |
| `app/(workspace)/my-tests/page.tsx` | `revalidate = 60` | `'use cache'` + `cacheLife({ stale: 30, revalidate: 60, expire: 120 })` |
| `app/(workspace)/profile/page.tsx` | `revalidate = 60` | `'use cache'` + `cacheLife('userData')` |
| `app/(workspace)/profile/edit/page.tsx` | `revalidate = 60` | `'use cache'` + `cacheLife('userData')` |
| `app/(public)/docs/layout.tsx` | `revalidate = 3600` | `'use cache'` + `cacheLife('referenceData')` |
| `app/(public)/docs/page.tsx` | `dynamic = 'force-static'`, `revalidate = 3600` | `'use cache'` + `cacheLife('max')` |
| `app/(public)/docs/getting-started/page.tsx` | `dynamic = 'force-static'`, `revalidate = 3600` | `'use cache'` + `cacheLife('max')` |
| `app/(public)/docs/how-it-works/page.tsx` | same | same |
| `app/(public)/docs/authoring/page.tsx` | same | same |
| `app/(public)/docs/authoring/questions/page.tsx` | same | same |
| `app/(public)/docs/authoring/indicators/page.tsx` | same | same |
| `app/(public)/docs/authoring/competencies/page.tsx` | same | same |
| `app/(public)/docs/best-practices/page.tsx` | same | same |
| `app/(public)/docs/glossary/page.tsx` | same | same |
| `app/(public)/docs/test-building/page.tsx` | same | same |
| `app/(public)/docs/scoring/page.tsx` | same | same |
| `app/(public)/docs/psychometrics/page.tsx` | same | same |

### 2.5 Suspense Readiness

- **35 `loading.tsx` files** across the app (auto-Suspense boundaries)
- Root layout: `<Suspense fallback={<AuthLoadingFallback />}>`
- Workspace layout: `<Suspense fallback={<ContentSkeleton />}>` around children
- Auth layout: `<Suspense fallback={<AuthSkeleton />}>`
- Public layout: `<Suspense fallback={<ContentSkeleton />}>`

**Assessment:** Suspense coverage is excellent. The existing `loading.tsx` files will serve as PPR dynamic boundaries automatically.

---

## 3. Target Architecture

### 3.1 Rendering Model

```
Request --> Edge (static shell from build) --> Stream dynamic holes via Suspense
             |                                      |
             v                                      v
    Sidebar skeleton, header,               Auth-dependent content,
    navigation chrome, page                 user-specific data,
    structure (INSTANT)                     real-time stats (STREAMED)
```

### 3.2 Component Boundary Model

```
Root Layout (Server Component, cached shell)
  |
  +-- <html>, <body>, metadata, fonts (STATIC - prerendered)
  |
  +-- <ClerkProvider dynamic> (Client boundary - does NOT force dynamic on shell)
  |     |
  |     +-- <NextIntlClientProvider> (Client - locale from server)
  |           |
  |           +-- <Suspense fallback={AuthLoadingFallback}>
  |                 |
  |                 +-- <LayoutContent> (DYNAMIC hole - auth-dependent)
  |
  +-- (workspace) Layout (Server Component)
  |     |
  |     +-- Sidebar chrome (STATIC shell)
  |     +-- <Suspense> around auth-checking component (DYNAMIC hole)
  |     +-- <Suspense> around page children (DYNAMIC holes via loading.tsx)
  |
  +-- (public) Layout (Server Component - fully STATIC with 'use cache')
  +-- (auth) Layout (Server Component - DYNAMIC via connection())
```

### 3.3 Cache Directive Model

```
'use cache' (file-level)              -> Entire module is cached
  cacheLife('entityData')             -> 5 min revalidation, 10 min expiry
  cacheTag('competencies')            -> Tag for targeted invalidation

'use cache' (function-level)          -> Individual function cached
  cacheLife('realtime')               -> 30s revalidation
  cacheTag('dashboard-stats')         -> Tag for invalidation

Server Action (mutation)
  updateTag('competencies')           -> Immediate invalidation
  revalidateTag('dashboard')          -> Eventual consistency (background)
  revalidatePath('/competencies')     -> Path-based invalidation (keep)
```

---

## 4. Phase 0: Prerequisites and Verification

**Goal:** Verify all external dependencies support PPR before any code changes.

**Duration:** 0.5 days

### 4.0.1 Clerk Version Verification

```bash
cd D:\projects\diplom\skillsoft\frontend-app
npm ls @clerk/nextjs
```

**Required:** `@clerk/nextjs >= 6.35.0` (currently `^6.35.1` -- PASS).

If the resolved version is older than 6.35.0:
```bash
npm install @clerk/nextjs@latest
```

### 4.0.2 Next.js Version Verification

**Required:** `next >= 16.0.0` (currently `16.0.7` -- PASS).

### 4.0.3 Create Feature Branch

```bash
git checkout dev
git pull origin dev
git checkout -b feat/ppr-cache-components
```

### 4.0.4 Baseline Performance Snapshot

Before any changes, capture baseline metrics:

```bash
cd D:\projects\diplom\skillsoft\frontend-app
npm run build 2>&1 | tee build-baseline.log
```

Record from build output:
- Total build time
- Route list with rendering mode (Static vs Dynamic)
- Bundle sizes

Also capture Lighthouse scores for:
- `/dashboard` (authenticated, dynamic)
- `/docs` (public, static)
- `/hr/competencies` (entity list)

### 4.0.5 Verify Existing Tests Pass

```bash
npm run type-check
npm run test
```

**Gate:** All tests must pass before proceeding.

### Rollback: N/A (no code changes)

---

## 5. Phase 1: Clerk Provider Restructuring

**Goal:** Move `<ClerkProvider dynamic>` out of the root layout so the HTML shell can be statically prerendered.

**Duration:** 0.5 days

**Dependencies:** Phase 0 complete.

### 5.1 Problem Statement

Currently, `<ClerkProvider dynamic>` wraps the entire `<html>` element in `app/layout.tsx`. When `cacheComponents: true` is enabled, this forces the entire page into a single dynamic boundary because ClerkProvider is a Client Component that accesses runtime data.

The fix is to keep `<ClerkProvider dynamic>` in the root layout but ensure it is **inside** the `<html>/<body>` tags, not wrapping them. The Clerk docs for Next.js 16 confirm that `<ClerkProvider dynamic>` can coexist with PPR as long as it does not prevent the HTML shell from being prerendered.

### 5.2 File Changes

#### `app/layout.tsx` (MODIFY)

**Before (lines 111-127):**
```tsx
<ClerkProvider appearance={clerkAppearance} dynamic>
  <html lang={locale} suppressHydrationWarning className="mobile-container">
    <body className="mobile-container" suppressHydrationWarning>
      <NextIntlClientProvider messages={messages} locale={locale}>
        <Suspense fallback={<AuthLoadingFallback />}>
          <LayoutContent>{children}</LayoutContent>
        </Suspense>
      </NextIntlClientProvider>
      <Analytics />
    </body>
  </html>
</ClerkProvider>
```

**After:**
```tsx
<html lang={locale} suppressHydrationWarning className="mobile-container">
  <body className="mobile-container" suppressHydrationWarning>
    <ClerkProvider appearance={clerkAppearance} dynamic>
      <NextIntlClientProvider messages={messages} locale={locale}>
        <Suspense fallback={<AuthLoadingFallback />}>
          <LayoutContent>{children}</LayoutContent>
        </Suspense>
      </NextIntlClientProvider>
    </ClerkProvider>
    <Analytics />
  </body>
</html>
```

**Rationale:** The `<html>` and `<body>` tags and their attributes (`lang`, `className`) become part of the static shell. `<ClerkProvider>` is still high enough in the tree to provide auth context to all routes, but it no longer prevents the HTML shell from being prerendered.

**Also update the no-Clerk fallback branch (lines 131-149) for consistency:**
The fallback branch already has `<html>/<body>` outside providers, so it is already correct.

### 5.3 Impact Assessment

| Concern | Assessment |
|---------|------------|
| Auth context availability | `<ClerkProvider>` is still above all routes -- no change in behavior |
| `useAuth()`, `useUser()` hooks | Still work because ClerkProvider is an ancestor |
| `auth()` server function | Works from proxy.ts and server components -- unaffected |
| SSR hydration | `<html>` attributes are now deterministic (locale, className) -- safer hydration |
| `suppressHydrationWarning` | Still present on both `<html>` and `<body>` |

### 5.4 Testing Checkpoint

```bash
npm run dev
# Manual verification:
# 1. Visit /dashboard -- auth works, sidebar renders, user menu shows
# 2. Visit /sign-in -- Clerk UI renders correctly
# 3. Visit /docs -- public page loads without auth
# 4. Sign out and back in -- redirect flow works
# 5. Check browser console for hydration warnings

npm run test
npm run type-check
```

### 5.5 Rollback

Revert the single file change:
```bash
git checkout -- app/layout.tsx
```

---

## 6. Phase 2: Workspace Layout Server Migration

**Goal:** Convert the workspace layout from a Client Component (`"use client"` + `useAuth()`) to a Server Component with server-side auth.

**Duration:** 1.5 days (highest risk phase)

**Dependencies:** Phase 1 complete.

### 6.1 Problem Statement

`app/(workspace)/layout.tsx` is currently a Client Component that:
1. Calls `useAuth()` to check `isSignedIn` and `isLoaded`
2. Shows skeleton while auth loads
3. Redirects to `/sign-in` if not authenticated
4. Renders `<ViewModeProvider>`, `<LensInitializer>`, `<HeaderProvider>`, `<BreadcrumbProvider>`, sidebar, header

This entire layout is forced into a dynamic Client Component boundary, preventing PPR from prerendering the sidebar/header shell.

### 6.2 Architecture Decision

**Option A (CHOSEN): Server Component with `auth()` + Suspense**

The workspace layout becomes a Server Component. Authentication is checked server-side using `auth()` from `@clerk/nextjs/server`. The client-only parts (`LensInitializer`, `ViewModeProvider`, etc.) are wrapped in a Client Component boundary.

**Why:** This allows the static layout chrome (sidebar skeleton, header structure) to be prerendered, while auth-dependent content streams in.

**Option B (REJECTED): Keep Client Component, use `<Suspense>` islands**

Would still prevent the layout shell from being static. Minimal PPR benefit.

### 6.3 File Changes

#### `app/(workspace)/layout.tsx` (REWRITE)

```tsx
import React, { Suspense } from "react";
import { auth } from "@clerk/nextjs/server";
import { redirect } from "next/navigation";
import { SidebarInset, SidebarProvider } from "@/components/ui/sidebar";
import { AppSidebar } from "@/components/layout/app-sidebar";
import { SiteHeader } from "@/components/layout/site-header";
import { MobileBottomNav } from "@/components/layout/mobile-bottom-nav";
import { HeaderProvider } from "@/src/context/HeaderContext";
import { BreadcrumbProvider } from "@/src/context/BreadcrumbContext";
import { LensInitializer } from "@/components/providers/LensInitializer";
import { Skeleton } from "@/components/ui/skeleton";
import { WorkspaceShell } from "./_components/WorkspaceShell";

// Skeleton components (moved from current file, kept as-is)
function SidebarSkeleton() { /* ... same as current ... */ }
function HeaderSkeleton() { /* ... same as current ... */ }
function ContentSkeleton() { /* ... same as current ... */ }

/**
 * Server-side auth check component.
 * Must be inside <Suspense> because auth() is a dynamic API.
 */
async function AuthGate({ children }: { children: React.ReactNode }) {
  const { userId } = await auth();

  if (!userId) {
    redirect("/sign-in");
  }

  return <>{children}</>;
}

/**
 * Workspace Layout (Server Component)
 *
 * Static shell: sidebar structure, header bar, main content area.
 * Dynamic holes: auth check, user-specific sidebar content, lens state.
 *
 * The sidebar and header chrome can be prerendered as static HTML.
 * Auth-dependent content streams in via Suspense boundaries.
 */
export default function WorkspaceLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <Suspense fallback={
      <div className="flex min-h-screen">
        <SidebarSkeleton />
        <div className="flex flex-1 flex-col">
          <HeaderSkeleton />
          <main className="flex flex-1 flex-col">
            <ContentSkeleton />
          </main>
        </div>
      </div>
    }>
      <AuthGate>
        <WorkspaceShell>
          {children}
        </WorkspaceShell>
      </AuthGate>
    </Suspense>
  );
}
```

#### NEW: `app/(workspace)/_components/WorkspaceShell.tsx` (CREATE)

This Client Component handles view mode, lens initialization, and the interactive sidebar/header.

```tsx
"use client";

import React, { Suspense } from "react";
import { usePathname } from "next/navigation";
import { SidebarInset, SidebarProvider } from "@/components/ui/sidebar";
import { AppSidebar } from "@/components/layout/app-sidebar";
import { SiteHeader } from "@/components/layout/site-header";
import { MobileBottomNav } from "@/components/layout/mobile-bottom-nav";
import { HeaderProvider } from "@/src/context/HeaderContext";
import { BreadcrumbProvider } from "@/src/context/BreadcrumbContext";
import { LensInitializer } from "@/components/providers/LensInitializer";
import { Skeleton } from "@/components/ui/skeleton";
import { ViewModeProvider, useViewMode, shouldBeFocused } from "@/src/context/ViewModeContext";
import { cn } from "@/lib/utils";

function ContentSkeleton() {
  return (
    <div className="flex-1 p-6 space-y-6">
      <Skeleton className="h-8 w-64" />
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {Array.from({ length: 6 }).map((_, i) => (
          <Skeleton key={i} className="h-32 w-full rounded-lg" />
        ))}
      </div>
    </div>
  );
}

function WorkspaceLayoutContent({ children }: { children: React.ReactNode }) {
  // ... exact same implementation as current WorkspaceLayoutContent ...
  // (usePathname, useViewMode, shouldBeFocused, SidebarProvider, etc.)
}

export function WorkspaceShell({ children }: { children: React.ReactNode }) {
  return (
    <ViewModeProvider>
      <LensInitializer />
      <HeaderProvider>
        <BreadcrumbProvider>
          <WorkspaceLayoutContent>
            {children}
          </WorkspaceLayoutContent>
        </BreadcrumbProvider>
      </HeaderProvider>
    </ViewModeProvider>
  );
}
```

### 6.4 Critical Concern: `redirect()` Inside Suspense

In Next.js 16, calling `redirect()` inside an async Server Component that is wrapped in `<Suspense>` works correctly. The redirect throws a special error that Next.js catches at the framework level, above the Suspense boundary. This is documented behavior.

However, there is a subtle difference: the fallback will flash briefly before the redirect completes. Since the proxy (`proxy.ts`) already redirects unauthenticated users for protected routes, the `AuthGate` redirect is a defense-in-depth measure. The proxy handles the fast path; `AuthGate` handles edge cases where the session expires mid-navigation.

### 6.5 Impact on LensInitializer

`LensInitializer` remains a Client Component. It currently uses `useUser()` from Clerk, which works inside `<ClerkProvider dynamic>`. No changes needed to this component.

The key difference: Previously, `LensInitializer` rendered before the sidebar because the entire layout was a Client Component. Now, it still renders before the sidebar because `WorkspaceShell` renders `<LensInitializer />` before `<WorkspaceLayoutContent>` in the JSX tree.

### 6.6 Testing Checkpoint

This is the highest-risk phase. Extensive testing required:

```bash
npm run type-check
npm run test
npm run dev
```

**Manual verification checklist:**

- [ ] Visit `/dashboard` signed in -- page loads with correct sidebar, header, user info
- [ ] Visit `/dashboard` signed out -- redirects to `/sign-in`
- [ ] Refresh `/dashboard` while signed in -- no flash of wrong content
- [ ] Navigate between workspace pages -- sidebar state preserved
- [ ] Check lens system -- correct role lens selected after login
- [ ] Test immersive mode (builder page) -- sidebar hides correctly
- [ ] Test mobile view -- bottom nav renders, sidebar collapses
- [ ] Check breadcrumbs -- update correctly on navigation
- [ ] Sign out from workspace page -- redirects to sign-in
- [ ] Open browser DevTools Network tab -- verify no extra auth API calls

**Build verification:**
```bash
npm run build
# Check that (workspace) routes show as "Dynamic" in build output
# (They should remain dynamic because of auth() usage)
```

### 6.7 Rollback

```bash
git checkout -- "app/(workspace)/layout.tsx"
rm -rf "app/(workspace)/_components/WorkspaceShell.tsx"
```

---

## 7. Phase 3: Cache Directive Migration

**Goal:** Replace all `unstable_cache()` usage with the `'use cache'` directive and `cacheLife()`/`cacheTag()`.

**Duration:** 1.5 days

**Dependencies:** Phase 2 complete (but can technically be done in parallel since these are separate files).

### 7.1 Migration Pattern

**Before (unstable_cache):**
```ts
import { unstable_cache } from 'next/cache';

async function fetchCompetencies() { /* ... */ }

export const getCompetenciesCached = unstable_cache(
  fetchCompetencies,
  ['competencies'],
  { revalidate: 300, tags: ['competencies'] }
);
```

**After ('use cache'):**
```ts
'use cache';

import { cacheLife, cacheTag } from 'next/cache';

export async function getCompetenciesCached(): Promise<Competency[] | null> {
  cacheLife('entityData');
  cacheTag('competencies');

  try {
    const response = await fetch(`${getApiBaseUrl()}/competencies`, {
      headers: { 'Content-Type': 'application/json' },
    });
    if (!response.ok) return null;
    return (await response.json()) as Competency[];
  } catch {
    return null;
  }
}
```

**Key differences:**
- `'use cache'` at the top of the file (file-level) or before individual functions
- `cacheLife()` replaces the `revalidate` option (uses the config profiles from `next.config.ts`)
- `cacheTag()` replaces the `tags` option
- No key array needed -- Next.js automatically derives the cache key from the function name + arguments
- `fetch()` options `next: { revalidate }` are no longer needed inside `'use cache'` functions

### 7.2 File Changes

#### `src/services/api.cache.ts` (REWRITE)

```ts
'use cache';
/**
 * Server-side Cached API Functions
 *
 * Uses Next.js 16 'use cache' directive for data fetching caching.
 * Cache profiles are defined in next.config.ts cacheLife.
 *
 * Profiles used:
 * - realtime: 30s stale, 30s revalidate, 60s expire
 * - entityData: 60s stale, 300s revalidate, 600s expire
 * - userData: 300s stale, 900s revalidate, 1800s expire
 */

import { cacheLife, cacheTag } from 'next/cache';
import { Competency, BehavioralIndicator, AssessmentQuestion } from '@/types/domain';
import { User } from '@/types/user';

const getApiBaseUrl = () => {
    const apiUrl = process.env.NEXT_PUBLIC_API_URL;
    return apiUrl ? `https://${apiUrl}/api` : "http://localhost:8080/api";
};

// ============================================================================
// Competencies
// ============================================================================

export async function getCompetenciesCached(): Promise<Competency[] | null> {
    cacheLife('entityData');
    cacheTag('competencies');

    try {
        const response = await fetch(`${getApiBaseUrl()}/competencies`, {
            headers: { 'Content-Type': 'application/json' },
        });
        if (!response.ok) return null;
        return (await response.json()) as Competency[];
    } catch {
        return null;
    }
}

export async function getCompetencyCached(id: string): Promise<Competency | null> {
    cacheLife('entityData');
    cacheTag('competencies', `competency-${id}`);

    try {
        const response = await fetch(`${getApiBaseUrl()}/competencies/${id}`, {
            headers: { 'Content-Type': 'application/json' },
        });
        if (!response.ok) return null;
        return (await response.json()) as Competency;
    } catch {
        return null;
    }
}

// ... (same pattern for indicators, questions, users, dashboard stats)
// Each function gets its own cacheLife() and cacheTag() calls
```

**Important:** The `'use cache'` directive at the file top applies to ALL exported async functions in the file. Each function can override with its own `cacheLife()` call.

#### `src/services/api.cache.dashboard.ts` (REWRITE)

This file is more complex because `getDashboardDataCached` takes `clerkUserId` and `userRole` as parameters. With `'use cache'`, function arguments become part of the cache key automatically.

```ts
'use cache';

import { cacheLife, cacheTag } from 'next/cache';
// ... imports ...

export async function getDashboardDataCached(
  clerkUserId?: string,
  userRole?: 'ADMIN' | 'EDITOR' | 'USER'
): Promise<DashboardSummary> {
    cacheLife('realtime');
    cacheTag('dashboard', 'competencies', 'templates', 'questions');

    // ... same fetchDashboardData logic, inlined ...
}
```

**Note:** The `clerkUserId` and `userRole` parameters automatically become part of the cache key. Different users/roles will get separate cache entries. This is an improvement over `unstable_cache` where keys had to be manually specified.

#### `src/services/api.cache.my-tests.ts` (REWRITE)

**Critical concern:** This file currently calls `getAuthHeaders()` outside the `unstable_cache` scope because `headers()` is a dynamic API. With `'use cache'`, we must pass auth headers as parameters (they become part of the cache key).

```ts
'use cache';

import { cacheLife, cacheTag } from 'next/cache';
// ... imports ...

export async function getUserSessionsCached(
  userId: string,
  authHeaders: Record<string, string>,
  page: number = 0,
  size: number = 100
): Promise<PaginatedResponse<TestSessionSummary> | null> {
    cacheLife('realtime');
    cacheTag('user-sessions', `user-sessions-${userId}`);

    // ... fetch with authHeaders ...
}
```

**IMPORTANT:** Callers of `getUserSessionsCached` must now pass `authHeaders` explicitly. The call site (in the my-tests page Server Component) becomes:

```ts
import { getAuthHeaders } from '@/services/roleApi';

// In the Server Component (outside 'use cache'):
const authHeaders = await getAuthHeaders();
const sessions = await getUserSessionsCached(userId, authHeaders);
```

This is functionally identical to the current pattern but cleaner -- the dynamic data (`headers()`) is resolved in the Server Component, and the pure data-fetching function is cached.

#### `src/services/api.cache.psychometrics.ts` (NO CHANGES)

This file uses React's `cache()` for request-scoped deduplication, not `unstable_cache`. The `cache()` function is orthogonal to `'use cache'` -- it deduplicates within a single request, while `'use cache'` persists across requests.

**Decision:** Keep React `cache()` as-is. It provides complementary request deduplication that works alongside the `'use cache'` directive used elsewhere.

#### `src/lib/cached-data.ts` (NO CHANGES)

Same reasoning as psychometrics cache. React `cache()` for request deduplication remains valid.

### 7.3 Auth Headers in Cached Functions

**Rule:** Dynamic APIs (`cookies()`, `headers()`, `searchParams`) CANNOT be called inside `'use cache'` functions. They must be resolved in the calling Server Component and passed as parameters.

Files affected:
- `src/services/api.cache.my-tests.ts` -- `getAuthHeaders()` call must be moved to caller
- `src/services/api.cache.dashboard.ts` -- already receives userId/role as params (no headers call inside)

Files NOT affected (no auth headers inside cache):
- `src/services/api.cache.ts` -- public endpoints, no auth headers
- `src/services/api.cache.psychometrics.ts` -- uses API service which handles auth internally

### 7.4 Testing Checkpoint

```bash
npm run type-check  # Verify no import errors from removed unstable_cache
npm run test        # All cache-related tests
npm run dev
```

**Manual verification:**
- [ ] Dashboard loads with correct stats
- [ ] Competency list loads (cached)
- [ ] Create a competency -- verify cache invalidation works (list updates)
- [ ] My tests page loads with correct sessions
- [ ] Psychometrics dashboard loads

**Cache behavior verification (dev tools):**
```bash
# In dev mode, check server logs for cache HIT/MISS indicators
# Next.js 16 logs cache behavior when devIndicators is enabled
```

### 7.5 Rollback

```bash
git checkout -- src/services/api.cache.ts
git checkout -- src/services/api.cache.dashboard.ts
git checkout -- src/services/api.cache.my-tests.ts
```

---

## 8. Phase 4: Route Segment Config Migration

**Goal:** Replace all `export const dynamic`, `export const revalidate`, and `export const fetchCache` with `'use cache'` + `cacheLife()`.

**Duration:** 1 day

**Dependencies:** Phase 3 complete.

### 8.1 Migration Rules

| Old Pattern | New Pattern |
|-------------|-------------|
| `export const dynamic = 'force-dynamic'` | Remove entirely (dynamic is default) |
| `export const dynamic = 'force-static'` | Add `'use cache'` + `cacheLife('max')` to the default export |
| `export const revalidate = N` | Add `'use cache'` + `cacheLife({ stale: N/2, revalidate: N, expire: N*2 })` or use a named profile |
| `export const fetchCache = 'default-cache'` | Remove entirely (fetch caching is handled by `'use cache'`) |
| `export const runtime = 'nodejs'` | Keep (not related to caching) |

### 8.2 File Changes

#### Builder Layout: `app/(workspace)/test-templates/[id]/builder/layout.tsx`

**Remove:**
```ts
export const dynamic = 'force-dynamic';
export const fetchCache = 'default-cache';
// Keep: export const runtime = 'nodejs';
```

**Rationale:** The builder layout uses `getBuilderDataCached()` which calls React `cache()` functions. The layout itself calls `await params` and fetches data, making it inherently dynamic. No route segment config needed.

#### My Tests Page: `app/(workspace)/my-tests/page.tsx`

**Remove:**
```ts
export const revalidate = 60;
```

**The page component itself does not need `'use cache'`** because it calls `getUserSessionsCached()` which is already a `'use cache'` function. The caching happens at the data layer, not the page layer.

#### Profile Pages

**`app/(workspace)/profile/page.tsx`** -- Remove `export const revalidate = 60`
**`app/(workspace)/profile/edit/page.tsx`** -- Remove `export const revalidate = 60`

Same reasoning: caching should be at the data fetching layer.

#### Docs Pages (11 files)

All docs pages follow the same pattern. For each:

**Remove:**
```ts
export const dynamic = "force-static";
export const revalidate = 3600;
```

**Add `'use cache'` to the page component or file level:**

For pages that are purely static content (no data fetching), add file-level directive:

```ts
'use cache';

import { cacheLife } from 'next/cache';

// At the top of the default export function:
export default function DocsPage() {
    cacheLife('max');
    // ... existing JSX ...
}
```

For the docs layout:

**`app/(public)/docs/layout.tsx`** -- Remove `export const revalidate = 3600`, add `'use cache'` if the layout itself should be cached (yes -- it wraps static content).

**Full list of docs files to modify:**
- `app/(public)/docs/layout.tsx`
- `app/(public)/docs/page.tsx`
- `app/(public)/docs/getting-started/page.tsx`
- `app/(public)/docs/how-it-works/page.tsx`
- `app/(public)/docs/authoring/page.tsx`
- `app/(public)/docs/authoring/questions/page.tsx`
- `app/(public)/docs/authoring/indicators/page.tsx`
- `app/(public)/docs/authoring/competencies/page.tsx`
- `app/(public)/docs/best-practices/page.tsx`
- `app/(public)/docs/glossary/page.tsx`
- `app/(public)/docs/test-building/page.tsx`
- `app/(public)/docs/scoring/page.tsx`
- `app/(public)/docs/psychometrics/page.tsx`

### 8.3 Testing Checkpoint

```bash
npm run type-check
npm run build  # CRITICAL: Check build output for route rendering modes
npm run test
```

**Build output verification:**
- Docs pages should show as "Static" or "PPR" in the build output
- Workspace pages should show as "Dynamic" or "PPR"
- No pages should show warnings about conflicting configs

### 8.4 Rollback

```bash
git checkout -- "app/(workspace)/test-templates/[id]/builder/layout.tsx"
git checkout -- "app/(workspace)/my-tests/page.tsx"
git checkout -- "app/(workspace)/profile/page.tsx"
git checkout -- "app/(workspace)/profile/edit/page.tsx"
git checkout -- "app/(public)/docs/"  # Revert all docs files
```

---

## 9. Phase 5: Enable cacheComponents and PPR

**Goal:** Flip the switch. Enable `cacheComponents: true` and verify the entire application works.

**Duration:** 1 day

**Dependencies:** Phases 1-4 complete.

### 9.1 File Changes

#### `next.config.ts` (MODIFY)

```ts
const nextConfig: NextConfig = {
    reactCompiler: true,
    // PPR enabled via Cache Components (Next.js 16)
    // Clerk @clerk/nextjs v6.35+ supports this configuration.
    cacheComponents: true,
    turbopack: {
        root: __dirname,
    },
    cacheLife: {
        // ... existing profiles, unchanged ...
        // NEW: static content profile for docs
        staticContent: {
            stale: 3600,      // Allow stale for 1 hour
            revalidate: 3600, // Revalidate every hour
            expire: 86400,    // Expire after 24 hours
        },
    },
    experimental: {
        // REMOVED: ppr: 'incremental' -- PPR is default when cacheComponents is true
        turbopackFileSystemCacheForDev: true,
        staleTimes: {
            dynamic: 30,
            static: 180,
        },
        // ... rest unchanged ...
    },
    // ... rest unchanged ...
};
```

**Changes summary:**
1. `cacheComponents: false` --> `cacheComponents: true`
2. Remove TODO comments about Clerk compatibility
3. Remove commented-out `ppr: 'incremental'` and its TODO comments
4. Add `staticContent` cache profile for docs pages
5. Update code comments to reflect the enabled state

### 9.2 Verify connection() in Auth Routes

The auth layout and sign-in/sign-up pages already use `connection()` inside `<Suspense>`:

- `app/(auth)/layout.tsx` -- `await connection()` inside `AuthContent` wrapped in `<Suspense>` (CORRECT)
- `app/(auth)/sign-in/[[...rest]]/page.tsx` -- `await connection()` inside Suspense (CORRECT)
- `app/(auth)/sign-up/[[...rest]]/page.tsx` -- `await connection()` inside Suspense (CORRECT)

These are already PPR-compatible. No changes needed.

### 9.3 Expected Build Behavior

With `cacheComponents: true`, the build output will show:

```
Route (app)                              Size  Rendering
-------                                  ----  ---------
/                                        ...   PPR
/(auth)/sign-in/[[...rest]]              ...   Dynamic
/(auth)/sign-up/[[...rest]]              ...   Dynamic
/(public)/docs                           ...   Static (via 'use cache')
/(public)/docs/getting-started           ...   Static (via 'use cache')
/(workspace)/dashboard                   ...   PPR
/(workspace)/hr/competencies             ...   PPR
/(workspace)/test-templates              ...   PPR
/(workspace)/test-templates/[id]/builder ...   Dynamic
/(workspace)/my-tests                    ...   PPR
/(workspace)/profile                     ...   PPR
```

**PPR routes** have a static shell prerendered at build time and dynamic holes filled at request time.

### 9.4 Testing Checkpoint -- COMPREHENSIVE

This is the make-or-break phase. Full regression testing required.

```bash
npm run type-check
npm run build 2>&1 | tee build-ppr.log
npm run test
```

**Compare build output with baseline:**
- Verify no routes unexpectedly became Static or Dynamic
- Compare bundle sizes (should be similar or smaller)
- Check for any new build warnings or errors

**Dev mode testing:**
```bash
npm run dev
```

**Full manual test matrix:**

| Route | Test | Expected |
|-------|------|----------|
| `/` | Load landing page | Static shell instant, auth state streams |
| `/sign-in` | Load sign-in | Clerk UI renders after Suspense |
| `/sign-up` | Load sign-up | Same as sign-in |
| `/dashboard` | Load as admin | Static shell (sidebar, header) instant, stats stream |
| `/dashboard` | Load as user | Same but fewer stats |
| `/hr/competencies` | Load list | Static table shell, data streams |
| `/hr/competencies/[id]` | Load detail | Static layout, data streams |
| `/hr/competencies` | Create new | Form works, cache invalidates, list updates |
| `/hr/behavioral-indicators` | Load list | Same pattern |
| `/hr/assessment-questions` | Load list | Same pattern |
| `/test-templates` | Load list | Same pattern |
| `/test-templates/[id]/builder` | Load builder | Full dynamic (force-dynamic removed, but auth makes it dynamic) |
| `/my-tests` | Load as user | Sessions stream into static shell |
| `/profile` | Load profile | User data streams |
| `/profile/edit` | Edit profile | Form works, saves, cache invalidates |
| `/psychometrics` | Load dashboard | Stats stream |
| `/admin/users` | Load as admin | User list streams |
| `/docs` | Load docs index | Fully static, instant |
| `/docs/getting-started` | Load docs page | Fully static, instant |

**Performance verification:**
- Compare Lighthouse scores with Phase 0 baseline
- TTFB should be lower for PPR routes
- LCP should be lower or equal for all routes

### 9.5 Rollback

```bash
# Revert just the config change
git checkout -- next.config.ts
```

This single revert disables PPR entirely and returns to the pre-migration state. Phases 1-4 changes are backward-compatible and can remain even with `cacheComponents: false`.

---

## 10. Phase 6: Invalidation Modernization

**Goal:** Introduce `updateTag()` for immediate invalidation in mutation-heavy flows, keeping `revalidateTag()` for background eventual consistency.

**Duration:** 0.5 days

**Dependencies:** Phase 5 complete and stable.

### 10.1 Invalidation Model

| Function | Behavior | Use Case |
|----------|----------|----------|
| `revalidateTag(tag)` | Marks tag as stale; next request triggers background revalidation; current request may still serve stale | Dashboard stats, list views, background refreshes |
| `updateTag(tag)` | Immediately purges the cache entry; next request MUST fetch fresh data | After CRUD mutations where the user expects to see the change immediately |
| `revalidatePath(path)` | Marks all cache entries on a path as stale | Broad invalidation after complex mutations |

### 10.2 File Changes

#### `src/app/actions.ts` (MODIFY)

Add `updateTag` import and use it for immediate invalidation after mutations:

```ts
'use server';

import { revalidatePath, revalidateTag } from 'next/cache';
import { updateTag } from 'next/cache';  // NEW: immediate invalidation

// In mutation actions, replace revalidateTag with updateTag:
export async function revalidateCompetencyTags(competencyId?: string) {
  try {
    revalidatePath('/competencies');
    revalidatePath('/hr/competencies');
    revalidatePath('/');
    revalidatePath('/dashboard');
    if (competencyId) {
      revalidatePath(`/competencies/${competencyId}`);
      revalidatePath(`/hr/competencies/${competencyId}`);
    }

    // Immediate invalidation for mutation responses
    updateTag('competencies');
    if (competencyId) {
      updateTag(`competency-${competencyId}`);
    }
    // Dashboard can use eventual consistency (background revalidation)
    revalidateTag('dashboard');
  } catch {
    // Silently fail in production
  }
}
```

**Pattern:** Use `updateTag()` for the primary entity that was mutated. Use `revalidateTag()` for secondary/aggregate data (dashboards, stats) where eventual consistency is acceptable.

Apply the same pattern to:
- `revalidateTestTemplateTags()` -- `updateTag('test-templates')`, `revalidateTag('dashboard')`
- `revalidateUserTags()` -- `updateTag('users')`, `revalidateTag('users-stats')`
- `revalidatePsychometricsTags()` -- `updateTag('psychometrics-items')`, `revalidateTag('psychometrics-dashboard')`

**Note:** The current `revalidateTag('competencies', 'max')` syntax (with 'max' as second argument) is not standard. In the `'use cache'` model, `revalidateTag()` takes only the tag name. The `'max'` parameter should be removed during this phase.

#### `src/app/actions/user-actions.ts` (MODIFY)

Same pattern: replace `revalidateTag(tag, 'max')` with `updateTag(tag)` for immediate and `revalidateTag(tag)` for eventual.

#### `src/app/actions/psychometrics.ts` (NO CHANGES)

This file uses only `revalidatePath()`, which remains valid.

### 10.3 Testing Checkpoint

```bash
npm run type-check
npm run test
```

**Manual verification:**
- [ ] Create a competency -- list updates IMMEDIATELY (not after refresh)
- [ ] Delete a competency -- list updates IMMEDIATELY
- [ ] Update a user role -- change reflected IMMEDIATELY
- [ ] Dashboard stats eventually update (within 30s) after competency changes

### 10.4 Rollback

```bash
git checkout -- src/app/actions.ts
git checkout -- src/app/actions/user-actions.ts
```

---

## 11. Risk Register

| ID | Risk | Severity | Likelihood | Phase | Mitigation |
|----|------|----------|------------|-------|------------|
| R1 | Clerk `<ClerkProvider dynamic>` inside `<body>` breaks auth state | HIGH | LOW | 1 | Test auth flows immediately; `dynamic` prop is designed for this pattern |
| R2 | `auth()` in Server Component workspace layout causes different behavior than `useAuth()` | HIGH | MEDIUM | 2 | `auth()` returns the same userId; proxy.ts handles redirects; `AuthGate` is defense-in-depth |
| R3 | ViewMode/Lens state lost during layout migration | MEDIUM | MEDIUM | 2 | `WorkspaceShell` preserves exact same component tree for client state |
| R4 | `'use cache'` function arguments make cache keys too specific (cache miss rate too high) | MEDIUM | LOW | 3 | Monitor cache hit rates in production; adjust argument structure if needed |
| R5 | Auth headers as cache key parameters create per-user cache entries (memory) | MEDIUM | MEDIUM | 3 | For `api.cache.my-tests.ts`, this is intentional (user-specific data). Monitor memory usage. |
| R6 | `updateTag()` not available in current Next.js version | LOW | LOW | 6 | Check `next@16.0.7` changelog; fall back to `revalidateTag()` if unavailable |
| R7 | Docs pages with `'use cache'` + `cacheLife('max')` serve stale content after updates | LOW | LOW | 4 | `revalidateDocsTags()` action already exists for manual invalidation |
| R8 | Third-party components (Radix, shadcn) incompatible with PPR | LOW | LOW | 5 | These are Client Components inside Suspense boundaries -- unaffected |
| R9 | `next-intl` `getLocale()`/`getMessages()` in root layout are dynamic APIs | MEDIUM | MEDIUM | 5 | Already inside the Server Component; the Suspense boundary in the layout handles this |
| R10 | Build time significantly increases with PPR | LOW | MEDIUM | 5 | PPR adds prerendering work; monitor build times; Turbopack cache mitigates |

---

## 12. Rollback Playbook

### Full Rollback (revert everything)

```bash
cd D:\projects\diplom\skillsoft\frontend-app
git checkout dev -- next.config.ts
git checkout dev -- app/layout.tsx
git checkout dev -- "app/(workspace)/layout.tsx"
git checkout dev -- src/services/api.cache.ts
git checkout dev -- src/services/api.cache.dashboard.ts
git checkout dev -- src/services/api.cache.my-tests.ts
git checkout dev -- src/app/actions.ts
git checkout dev -- src/app/actions/user-actions.ts
# Remove new files
rm -rf "app/(workspace)/_components/WorkspaceShell.tsx"
# Revert docs files
git checkout dev -- "app/(public)/docs/"
git checkout dev -- "app/(workspace)/test-templates/[id]/builder/layout.tsx"
git checkout dev -- "app/(workspace)/my-tests/page.tsx"
git checkout dev -- "app/(workspace)/profile/page.tsx"
git checkout dev -- "app/(workspace)/profile/edit/page.tsx"

npm run build
npm run test
```

### Partial Rollback (disable PPR but keep refactoring)

If PPR causes issues but the code refactoring (Phases 1-4) is stable:

```bash
# Just disable cacheComponents
# In next.config.ts, set cacheComponents: false
```

The `'use cache'` directives will be ignored when `cacheComponents: false`. The functions will behave as regular async functions. The `cacheLife()` and `cacheTag()` calls become no-ops. This is a documented degradation path.

### Emergency Rollback (production)

If deployed to production and issues arise:

1. Revert the `next.config.ts` change (`cacheComponents: false`)
2. Deploy immediately
3. The `'use cache'` directives are harmlessly ignored

This is the safest aspect of the migration: the kill switch is a single boolean.

---

## 13. Testing Strategy

### 13.1 Unit Tests

No new unit tests required for the migration itself. Existing tests should continue to pass because:
- `'use cache'` is a server-side concern; React Testing Library tests render client components
- Cache functions are called in Server Components, not directly in tests
- Mock data in tests is unaffected by caching

### 13.2 Integration Tests

If there are any integration tests that import from `api.cache.ts`:
- Update imports (function signatures may change due to parameter additions in `api.cache.my-tests.ts`)
- The `unstable_cache` mock may need to be replaced with a `'use cache'` test helper

### 13.3 Build Verification Tests

Create a CI step that verifies build output:

```bash
# In CI pipeline after build:
npm run build 2>&1 | grep -E "/(workspace|public|auth)" > build-routes.txt
# Verify expected rendering modes
```

### 13.4 Performance Tests

After Phase 5, compare against Phase 0 baseline:

| Metric | Baseline | Target | Measurement |
|--------|----------|--------|-------------|
| TTFB (dashboard) | TBD | -40% | Lighthouse |
| LCP (dashboard) | TBD | -20% | Lighthouse |
| TTFB (docs) | TBD | -60% | Lighthouse (static) |
| Build time | TBD | +10% max | CI timing |
| Bundle size | TBD | No increase | Build output |

### 13.5 Regression Checklist

Run before merging each phase:

- [ ] `npm run type-check` passes
- [ ] `npm run test` passes
- [ ] `npm run build` succeeds
- [ ] Dev server starts without errors
- [ ] Auth flow: sign-in, sign-out, redirect
- [ ] Lens system: role detection, lens switching
- [ ] CRUD operations: create, update, delete with cache invalidation
- [ ] Navigation: sidebar links, breadcrumbs, back button
- [ ] Mobile: responsive layout, bottom nav
- [ ] i18n: language switching (if applicable)

---

## 14. Appendix: File Change Inventory

### New Files

| File | Phase | Purpose |
|------|-------|---------|
| `app/(workspace)/_components/WorkspaceShell.tsx` | 2 | Client Component shell extracted from workspace layout |

### Modified Files

| File | Phase | Change Type |
|------|-------|-------------|
| `app/layout.tsx` | 1 | Move ClerkProvider inside html/body |
| `app/(workspace)/layout.tsx` | 2 | Rewrite from Client to Server Component |
| `src/services/api.cache.ts` | 3 | Replace unstable_cache with 'use cache' |
| `src/services/api.cache.dashboard.ts` | 3 | Replace unstable_cache with 'use cache' |
| `src/services/api.cache.my-tests.ts` | 3 | Replace unstable_cache with 'use cache' + signature change |
| `app/(workspace)/test-templates/[id]/builder/layout.tsx` | 4 | Remove route segment configs |
| `app/(workspace)/my-tests/page.tsx` | 4 | Remove revalidate export |
| `app/(workspace)/profile/page.tsx` | 4 | Remove revalidate export |
| `app/(workspace)/profile/edit/page.tsx` | 4 | Remove revalidate export |
| `app/(public)/docs/layout.tsx` | 4 | Remove revalidate, add 'use cache' |
| `app/(public)/docs/page.tsx` | 4 | Remove dynamic+revalidate, add 'use cache' |
| `app/(public)/docs/getting-started/page.tsx` | 4 | Same |
| `app/(public)/docs/how-it-works/page.tsx` | 4 | Same |
| `app/(public)/docs/authoring/page.tsx` | 4 | Same |
| `app/(public)/docs/authoring/questions/page.tsx` | 4 | Same |
| `app/(public)/docs/authoring/indicators/page.tsx` | 4 | Same |
| `app/(public)/docs/authoring/competencies/page.tsx` | 4 | Same |
| `app/(public)/docs/best-practices/page.tsx` | 4 | Same |
| `app/(public)/docs/glossary/page.tsx` | 4 | Same |
| `app/(public)/docs/test-building/page.tsx` | 4 | Same |
| `app/(public)/docs/scoring/page.tsx` | 4 | Same |
| `app/(public)/docs/psychometrics/page.tsx` | 4 | Same |
| `next.config.ts` | 5 | Enable cacheComponents, add staticContent profile |
| `src/app/actions.ts` | 6 | Add updateTag(), remove 'max' parameter |
| `src/app/actions/user-actions.ts` | 6 | Add updateTag(), remove 'max' parameter |

### Unchanged Files (verified compatible)

| File | Reason |
|------|--------|
| `proxy.ts` | clerkMiddleware() is PPR-compatible |
| `app/(auth)/layout.tsx` | Already uses connection() in Suspense |
| `app/(auth)/sign-in/[[...rest]]/page.tsx` | Already uses connection() in Suspense |
| `app/(auth)/sign-up/[[...rest]]/page.tsx` | Already uses connection() in Suspense |
| `app/(public)/layout.tsx` | Already a Server Component with Suspense |
| `src/services/api.cache.psychometrics.ts` | Uses React cache() (not unstable_cache) |
| `src/lib/cached-data.ts` | Uses React cache() (not unstable_cache) |
| `src/components/providers/LensInitializer.tsx` | Client Component, works inside Suspense |
| `src/app/actions/psychometrics.ts` | Uses revalidatePath() only (still valid) |
| All 35 `loading.tsx` files | Already serve as PPR dynamic boundaries |

---

## Phase Dependency Graph

```
Phase 0 (Prerequisites)
    |
    v
Phase 1 (ClerkProvider restructure) ----+
    |                                    |
    v                                    |  (can run in parallel)
Phase 2 (Workspace layout migration) ---+
    |                                    |
    v                                    v
Phase 3 (Cache directive migration) ----+
    |
    v
Phase 4 (Route segment config migration)
    |
    v
Phase 5 (Enable cacheComponents) -- THE SWITCH
    |
    v
Phase 6 (updateTag modernization) -- OPTIONAL, can be deferred
```

**Critical path:** 0 --> 1 --> 2 --> 5
**Parallel path:** 3 and 4 can be done alongside or after Phase 2, but must complete before Phase 5.
**Deferrable:** Phase 6 improves UX but is not required for PPR to function.

---

*End of migration plan.*
