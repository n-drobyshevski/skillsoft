# Overview Tab UI Redesign Plan

## Executive Summary

Redesign the Test Template Overview tab to be **functional** (not just read-only), **mobile-first**, and integrate **sharing/visibility** features prominently. Uses shadcn/ui components throughout for a clean, modern design.

---

## Current State Analysis

### Existing Components (Overview Tab)
| Component | Purpose | Issues |
|-----------|---------|--------|
| `BlueprintSpecSheet` | Shows config + competencies | Read-only, not mobile-optimized |
| `PerformanceCard` | Shows pass rate, duration, completion | Empty state dominant, low value |
| `RecentActivityCard` | Table of last 5 sessions | Table is mobile-unfriendly |

### Existing Sharing Infrastructure (Access Tab)
- **VisibilitySelector** - Toggle PRIVATE/PUBLIC/LINK
- **UserShareList** - Manage user/team shares
- **ShareLinkManager** - Create/manage share links
- **ResponsiveShareModal** - Dialog (desktop) / Drawer (mobile)
- **PermissionSelect** - VIEW/EDIT/MANAGE dropdown
- Full React Query hooks: `useTemplateVisibility`, `useTemplateShares`, `useActiveShareLinks`

### Key Problems
1. **Not Functional** - Everything is read-only
2. **No Sharing Visibility** - Must navigate to Access tab
3. **Poor Mobile UX** - Grid and table don't collapse well
4. **Weak Information Hierarchy** - Important actions not prominent

---

## Proposed Architecture

### Mobile-First Layout (< 768px)
```
┌─────────────────────────────────────┐
│ HERO SECTION                        │
│ Name, Description, Status, Actions  │
├─────────────────────────────────────┤
│ SHARING SUMMARY CARD                │
│ Visibility, Share count, Quick link │
├─────────────────────────────────────┤
│ CONFIGURATION CARD                  │
│ Editable settings, Toggles          │
├─────────────────────────────────────┤
│ QUICK STATS GRID (2x2)              │
│ Sessions, Pass rate, Duration, etc  │
├─────────────────────────────────────┤
│ RECENT ACTIVITY LIST                │
│ Card-based session list             │
└─────────────────────────────────────┘
```

### Desktop Layout (>= 1024px)
```
┌───────────────────────────────────────────────────────┐
│ HERO SECTION (Full Width)                             │
│ Name, Description, Status Badge, Primary Actions      │
├─────────────────────────────────┬─────────────────────┤
│ CONFIGURATION CARD              │ SHARING SUMMARY     │
│ (2 columns)                     │ (1 column)          │
│                                 │                     │
│ - Time Limit [Edit]             │ 🔒 Private          │
│ - Passing Score [Edit]          │                     │
│ - Questions/Indicator [Edit]    │ 0 shared • 0 links  │
│                                 │                     │
│ Toggles:                        │ [Share Template]    │
│ ☑ Shuffle Questions             │                     │
│ ☑ Shuffle Options               ├─────────────────────┤
│ ☑ Allow Skip                    │ QUICK STATS         │
│ ☑ Allow Back Navigation         │ (1 column)          │
│ ☑ Show Results Immediately      │                     │
│                                 │ Sessions: 0         │
│ Competencies: 5 selected        │ Pass Rate: --       │
│ [View in Builder →]             │ Completion: --      │
│                                 │ Avg Duration: --    │
├─────────────────────────────────┴─────────────────────┤
│ RECENT ACTIVITY (Full Width)                          │
│ Avatar | Name | Status | Score | Duration | Time      │
└───────────────────────────────────────────────────────┘
```

---

## Component Specifications

### 1. OverviewHero Component (NEW)

**Purpose:** Template identity, status, and primary actions

**Props:**
```typescript
interface OverviewHeroProps {
  template: TestTemplate;
  isDraft: boolean;
  canEdit: boolean;
}
```

**Features:**
- Template name (editable inline if draft + canEdit)
- Description (collapsible on mobile, full on desktop)
- Status badge: DRAFT (amber), PUBLISHED (green), ARCHIVED (gray)
- Assessment goal badge (OVERVIEW, JOB_FIT, TEAM_FIT)
- Created/Updated timestamps (relative time)
- Primary action button:
  - Draft: "Go to Builder" (primary)
  - Published: "New Version" (outline)

**Mobile Behavior:**
- Name truncates with ellipsis
- Description hidden by default, "Show more" toggle
- Timestamps below name

---

### 2. SharingSummaryCard Component (NEW)

**Purpose:** Quick sharing access without navigating to Access tab

**Props:**
```typescript
interface SharingSummaryCardProps {
  templateId: string;
  templateName: string;
  isOwner: boolean;
  canManage: boolean;
}
```

**Features:**
- Visibility badge with icon:
  - PRIVATE: 🔒 Lock, slate-100/700
  - PUBLIC: 🌐 Globe, green-100/700
  - LINK: 🔗 Link, blue-100/700
- Description text for current visibility
- Badges showing: "X people shared" and "X active links"
- "Share" button (opens ResponsiveShareModal)
- "Copy Link" button (only when visibility=LINK and links exist)
- "Manage access settings →" link to Access tab

**Data Fetching:**
- Uses `useTemplateVisibility(templateId)` hook
- Uses `useActiveShareLinks(templateId)` for link count

**Mobile Behavior:**
- Stacked layout
- Full-width buttons
- 44px min touch targets

---

### 3. ConfigurationCard Component (REFACTOR)

**Purpose:** Editable test settings with inline controls

**Props:**
```typescript
interface ConfigurationCardProps {
  template: TestTemplate;
  isDraft: boolean;
  canEdit: boolean;
  competencyCount: number;
  templateId: string;
}
```

**Editable Fields (only when isDraft && canEdit):**
| Field | Input Type | Validation |
|-------|------------|------------|
| Time Limit | Number input | 1-180 minutes |
| Passing Score | Number input | 0-100% |
| Questions per Indicator | Number input | 1-10 |

**Toggle Switches:**
| Setting | Default |
|---------|---------|
| Shuffle Questions | true |
| Shuffle Options | true |
| Allow Skip | true |
| Allow Back Navigation | true |
| Show Results Immediately | true |

**Competency Summary:**
- Count badge: "5 competencies selected"
- Link: "View in Builder →" (navigates to builder tab)

**Edit Mode:**
- Click pencil icon to enter edit mode
- Inline number inputs appear
- Save/Cancel buttons
- Optimistic updates with rollback on error

**Mobile Behavior:**
- Full-width inputs
- Switches stack vertically with labels
- Larger touch targets

---

### 4. QuickStatsGrid Component (NEW)

**Purpose:** Compact performance metrics

**Props:**
```typescript
interface QuickStatsGridProps {
  stats: {
    totalSessions: number;
    completedSessions: number;
    passRate: number;
    avgDuration: number;
    completionRate: number;
  } | null;
}
```

**Metrics Displayed:**
| Metric | Icon | Format |
|--------|------|--------|
| Total Sessions | Users | "12 sessions" |
| Pass Rate | TrendingUp | "85%" with color |
| Completion Rate | CheckCircle | "92%" with color |
| Avg Duration | Timer | "25 min" |

**Layout:**
- Mobile: 2x2 grid of compact cards
- Desktop: 1-column vertical stack

**Empty State:**
- Show "--" for all values
- Muted text: "No sessions yet"

---

### 5. RecentActivityList Component (NEW)

**Purpose:** Mobile-friendly session list (replaces table)

**Props:**
```typescript
interface RecentActivityListProps {
  sessions: TestSession[];
  templateId: string;
  passingScore: number;
}
```

**Item Layout (Mobile):**
```
┌─────────────────────────────────────┐
│ ┌────┐  John Doe                    │
│ │ JD │  Completed • 85%             │
│ └────┘  2 hours ago                 │
└─────────────────────────────────────┘
```

**Item Layout (Desktop):**
```
┌────┐  John Doe      Completed  ✓ 85%   25 min   2h ago
│ JD │  john@email
└────┘
```

**Features:**
- Avatar with initials fallback
- Candidate name (email as fallback)
- Status badge (color-coded)
- Score (color-coded: green >= passing, red < passing)
- Relative timestamp
- "View All" link to results tab

**Empty State:**
- Illustration/icon
- "No test sessions yet"
- "Sessions will appear here once candidates start the test"

---

## File Structure

```
frontend-app/app/(workspace)/test-templates/[id]/(overview)/
├── page.tsx                    # Server component (data fetching)
├── loading.tsx                 # Updated skeleton
└── _components/
    ├── OverviewContent.tsx     # Client wrapper (coordinates all sections)
    ├── OverviewHero.tsx        # Hero section
    ├── SharingSummaryCard.tsx  # Sharing quick access
    ├── ConfigurationCard.tsx   # Editable settings
    ├── QuickStatsGrid.tsx      # Compact metrics
    └── RecentActivityList.tsx  # Mobile-friendly activity
```

---

## Implementation Phases

### Phase 1: Core Structure (Foundation)
- [ ] Create `OverviewContent.tsx` client wrapper
- [ ] Update `page.tsx` to fetch visibility data
- [ ] Create responsive grid layout shell
- [ ] Update `loading.tsx` skeleton to match new layout

### Phase 2: Hero & Sharing Sections
- [ ] Create `OverviewHero.tsx` component
- [ ] Create `SharingSummaryCard.tsx` component
- [ ] Integrate `ResponsiveShareModal` trigger
- [ ] Add copy link functionality with toast

### Phase 3: Configuration Section
- [ ] Create `ConfigurationCard.tsx` with inline editing
- [ ] Add `useUpdateTemplateSettings` mutation hook
- [ ] Implement toggle switches for settings
- [ ] Add validation and error handling

### Phase 4: Stats & Activity Sections
- [ ] Create `QuickStatsGrid.tsx` component
- [ ] Create `RecentActivityList.tsx` component
- [ ] Add proper empty states for both

### Phase 5: Polish & Testing
- [ ] Add loading skeletons for each section
- [ ] Add error boundaries
- [ ] Mobile testing across breakpoints
- [ ] Accessibility audit (keyboard nav, screen readers)
- [ ] RTL support verification

---

## Technical Notes

### Data Dependencies
```typescript
// page.tsx - Server Component
async function getOverviewData(id: string) {
  const [template, competencies, visibility] = await Promise.all([
    testTemplatesApi.getTemplateById(id),
    competenciesApi.getAllCompetencies(),
    templateSharingApi.getVisibility(id),  // NEW
  ]);
  // ... rest of logic
}
```

### Client State
- Inline edit mode: `useState<boolean>(false)`
- Pending values: `useState<Partial<TestTemplate>>({})`
- Share modal open: `useState<boolean>(false)`

### React Query Hooks Used
| Hook | Purpose |
|------|---------|
| `useTemplateVisibility` | Get visibility + counts |
| `useActiveShareLinks` | Get active links for copy |
| `useChangeVisibility` | Mutation (if needed in modal) |
| `useUpdateTemplate` | Mutation for inline editing (NEW) |

### Responsive Breakpoints
| Breakpoint | Width | Layout |
|------------|-------|--------|
| `xs` | < 640px | Single column, stacked |
| `sm` | 640-768px | Single column, more padding |
| `md` | 768-1024px | 2-column for some sections |
| `lg` | >= 1024px | Full 3-column grid |

---

## Visual Design Tokens

### Status Colors
```css
/* Draft */
--status-draft-bg: theme(colors.amber.100);
--status-draft-text: theme(colors.amber.700);

/* Published */
--status-published-bg: theme(colors.green.100);
--status-published-text: theme(colors.green.700);

/* Archived */
--status-archived-bg: theme(colors.gray.100);
--status-archived-text: theme(colors.gray.500);
```

### Visibility Colors
```css
/* Private */
--visibility-private-bg: theme(colors.slate.100);
--visibility-private-text: theme(colors.slate.700);

/* Public */
--visibility-public-bg: theme(colors.green.100);
--visibility-public-text: theme(colors.green.700);

/* Link */
--visibility-link-bg: theme(colors.blue.100);
--visibility-link-text: theme(colors.blue.700);
```

### Score Colors
```css
/* Passing (>= threshold) */
--score-pass: theme(colors.green.600);

/* Failing (< threshold) */
--score-fail: theme(colors.red.600);
```

---

## Shadcn/UI Components Used

| Component | Usage |
|-----------|-------|
| Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter | All sections |
| Badge | Status, visibility, stats |
| Button | Actions, links |
| Input | Inline editing |
| Switch | Toggle settings |
| Avatar, AvatarFallback | Activity list items |
| Separator | Visual dividers |
| Skeleton | Loading states |
| Tooltip | Info icons |
| Dialog/Drawer | Share modal (via ResponsiveShareModal) |

---

## Accessibility Checklist

- [ ] All interactive elements have visible focus states
- [ ] Minimum 44x44px touch targets on mobile
- [ ] Color is not the only indicator (icons accompany colors)
- [ ] Proper heading hierarchy (h2 for sections)
- [ ] ARIA labels on icon-only buttons
- [ ] Keyboard navigation for inline editing
- [ ] Screen reader announcements for state changes
- [ ] Reduced motion support for animations

---

## Migration Notes

### Breaking Changes: None
The new design is additive. Existing routes remain unchanged.

### Data Model: No Changes
Uses existing `TestTemplate`, `VisibilityInfo`, `ShareLink` types.

### API: No New Endpoints Needed
- Uses existing `testTemplatesApi.updateTemplate` for settings
- Uses existing `templateSharingApi.getVisibility` for sharing data

### Backwards Compatibility
- Falls back gracefully if visibility API fails
- Inline editing disabled for non-owners/non-editors
- Works without sharing data (shows "Private" default)

---

**Plan Created:** 2026-01-09
**Estimated Complexity:** Medium-High
**Dependencies:** Existing sharing infrastructure, shadcn/ui components
