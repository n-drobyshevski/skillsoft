# Big Five Mobile Redesign Plan

## Executive Summary

This document consolidates recommendations from UI Design, UX Research, Workflow Architecture, and Next.js Development experts for redesigning the psychometrics/big-five page with a mobile-first approach.

**Core Insight:** Transform from "browse-first" to "alert-first" pattern - HR professionals need to quickly identify problematic traits, not browse all 5 equally.

---

## 1. Current Issues Identified

| Component | Issue | Impact |
|-----------|-------|--------|
| Trait Cards | 280px fixed width + horizontal scroll | Hidden content, awkward snap, no scroll indicators |
| Bar Chart | 280px height, 80-140px left margin | Labels truncated, only 140-200px for actual bars |
| Stats Grid | 4-column with 10px labels | Illegible on mobile, cramped spacing |
| Accordion | Dense content, nested grids | Small touch targets, overwhelming on expand |
| Workflow | No priority surfacing | User must scan all 5 cards to find problems |

---

## 2. Design Strategy: Alert-First Architecture

### Information Hierarchy (Mobile)

```
P0 (Always Visible): Alert count + problematic traits
P1 (One Tap): Specific trait details
P2 (Expandable): Full metrics + recommendations
```

### Entry Point Redesign

```
+-------------------------------------------+
| Big Five Reliability            [Audit]   |
+-------------------------------------------+
| [!] 2 TRAITS NEED ATTENTION               |
|                                           |
|   Neuroticism     alpha: 0.58   [Review]  |
|   Openness        alpha: 0.61   [Review]  |
|                                           |
|   3 traits performing well                |
+-------------------------------------------+
| All Traits Overview                       |
|   O     C     E     A     N               |
|  ███   ███   ███   ▓▓▓   ▓▓▓              |
|  .82   .85   .79   .61   .58              |
+-------------------------------------------+
```

---

## 3. Component Redesign Specifications

### A. Mobile Trait Card (Compact Row Pattern)

Replace horizontal scroll carousel with vertical stacked cards:

```tsx
// Structure per card
<div className="flex items-center gap-3 p-4 rounded-xl border min-h-[72px]">
  {/* Color indicator: 40x40px */}
  <div className="w-10 h-10 rounded-lg flex items-center justify-center bg-{trait}/10">
    <span className="text-lg font-bold text-{trait}">{abbreviation}</span>
  </div>

  {/* Content */}
  <div className="flex-1 min-w-0">
    <h3 className="font-medium text-sm truncate">{traitLabel}</h3>
    <p className="text-xs text-muted-foreground line-clamp-1">{description}</p>
  </div>

  {/* Value + Status + Chevron */}
  <div className="flex items-center gap-2 shrink-0">
    <span className="text-2xl font-bold tabular-nums text-{trait}">
      {alpha.toFixed(2)}
    </span>
    <ReliabilityStatusBadge status={status} size="sm" />
    <ChevronRight className="h-4 w-4" />
  </div>
</div>
```

**Tailwind Classes:**
```css
.trait-card-mobile {
  @apply flex items-center gap-3 p-4 rounded-xl border
         bg-card hover:bg-accent/50 transition-colors
         min-h-[72px] touch-action-manipulation;
}
```

### B. Chart Optimization

Transform horizontal bar chart to **vertical bars** for mobile:

```tsx
// Mobile: Vertical bars with values above
<div className="h-[200px] flex items-end justify-between gap-2 px-2">
  {/* Reference lines (horizontal) */}
  <div className="absolute inset-x-0 bottom-[60%] border-t border-dashed border-red-500/30" />
  <div className="absolute inset-x-0 bottom-[70%] border-t border-dashed border-amber-500/40" />
  <div className="absolute inset-x-0 bottom-[80%] border-t border-dashed border-emerald-500/40" />

  {chartData.map((trait) => (
    <div className="flex-1 flex flex-col items-center gap-1">
      <span className="text-xs font-bold tabular-nums" style={{ color: trait.color }}>
        {trait.alpha.toFixed(2)}
      </span>
      <div
        className="w-full max-w-[40px] rounded-t-md"
        style={{ height: `${trait.alpha * 100}%`, backgroundColor: trait.color }}
      />
      <span className="text-[11px] font-medium text-muted-foreground">
        {trait.abbreviation}
      </span>
    </div>
  ))}
</div>
```

### C. Typography & Spacing Standards

| Element | Current | Recommended |
|---------|---------|-------------|
| Micro labels | 10px | 11px minimum |
| Card values | 1.875rem | 1.5rem mobile |
| Touch targets | varied | 44px minimum |
| Card padding | 1rem | 1rem (keep) |
| Section gap | 1.5rem | 1.5rem mobile, 2rem desktop |

### D. Stats Grid Fix

```tsx
// Current (4 columns, cramped):
<div className="grid grid-cols-4 gap-2">

// Recommended (2x2 on mobile):
<div className="grid grid-cols-2 gap-3 sm:grid-cols-4 sm:gap-2">
```

---

## 4. Workflow State Machine

```
┌──────────────┐
│   LOADING    │
└──────┬───────┘
       │
       ▼
┌──────────────────────────────────────┐
│          OVERVIEW STATE              │
│  • Alert summary visible             │
│  • Mini chart visible                │
│  • Scroll: vertical only             │
└──────────┬───────────────────────────┘
           │
    ┌──────┴──────┐
    │             │
    ▼             ▼
┌─────────────┐  ┌─────────────────────┐
│ DETAIL      │  │ COMPARISON          │
│ Single trait│  │ Full chart view     │
│ Swipe nav   │  │ Tooltips active     │
└──────┬──────┘  └──────────┬──────────┘
       │                    │
       └──────┬─────────────┘
              ▼
       ┌──────────────┐
       │ AUDIT STATE  │
       │ Progress     │
       └──────────────┘
```

### Gesture Mapping

| Gesture | Context | Action |
|---------|---------|--------|
| Tap | Alert card trait | → Detail state |
| Tap | Back button | → Overview |
| Swipe Left | Detail state | → Next trait |
| Swipe Right | Detail state | → Prev trait |
| Swipe Down | Detail state | → Return to overview |
| Pull Down | Top of page | → Refresh |

---

## 5. Implementation Architecture

### Server vs Client Split

| Component | Type | Rationale |
|-----------|------|-----------|
| Page layout | Server | Zero JS, instant paint |
| Alert summary card | Server | Static, critical path |
| Trait card list | Server | SEO, initial render |
| Mini chart | Client | Needs responsiveness |
| Full comparison chart | Client | Interactive tooltips |
| Accordion | Client | Expand/collapse state |
| Audit trigger | Client | User interaction |

### Component Structure

```
frontend-app/app/(workspace)/psychometrics/big-five/
├── page.tsx                    # Server Component (main layout)
├── loading.tsx                 # Skeleton loader
├── _components/
│   ├── index.ts
│   ├── AlertSummaryCard.tsx    # Server - critical path content
│   ├── TraitCardList.tsx       # Server - stacked trait cards
│   ├── TraitCardMobile.tsx     # Client - expandable card
│   ├── MiniTraitChart.tsx      # Client - vertical bar chart
│   ├── ComparisonChartMobile.tsx # Client - full comparison
│   ├── TraitDetailSheet.tsx    # Client - bottom sheet pattern
│   └── QuickActionsBar.tsx     # Client - FAB pattern
```

---

## 6. Critical CSS Changes

### Add to globals.css

```css
/* Mobile-first trait card */
.trait-card-mobile {
  @apply flex items-center gap-3 p-4 rounded-xl border
         bg-card hover:bg-accent/50
         transition-colors duration-200
         min-h-[72px];
}

/* Touch-friendly accordion trigger */
.accordion-trigger-mobile {
  @apply w-full flex items-center gap-3 p-4
         min-h-[56px]
         hover:bg-accent/50 transition-colors;
}

/* Mobile chart container */
.chart-mobile {
  @apply relative h-[200px] flex items-end justify-between gap-2 px-2;
}

/* Minimum font size enforcement */
.text-mobile-safe {
  font-size: max(0.6875rem, 11px);
}
```

---

## 7. Accessibility Checklist

- [ ] All interactive elements: 44x44px minimum touch target
- [ ] No text smaller than 11px
- [ ] Color contrast WCAG AA (4.5:1)
- [ ] Focus indicators: 2px ring on focus-visible
- [ ] Screen reader: ARIA labels for status badges
- [ ] Reduced motion: respect `prefers-reduced-motion`
- [ ] Hidden data tables for chart accessibility

---

## 8. Implementation Priority

| Priority | Task | Effort | Impact |
|----------|------|--------|--------|
| P0 | Alert summary card (problem-first) | Medium | High |
| P0 | Replace horizontal scroll with stacked cards | Medium | High |
| P1 | Fix 10px labels to 11px | Low | Medium |
| P1 | Convert stats grid to 2x2 mobile | Low | Medium |
| P1 | Add 44px touch targets | Low | High |
| P2 | Implement vertical bar chart | Medium | Medium |
| P2 | Add bottom sheet for details | Medium | Medium |
| P3 | Swipe gestures between traits | Medium | Low |

---

## 9. Success Metrics

| Metric | Current | Target |
|--------|---------|--------|
| Time to identify problem trait | 15-20s | <5s |
| Taps to reach detail | 3-5 | 1-2 |
| Scroll to first action | 400px+ | 0 |
| Lighthouse Mobile Performance | TBD | >90 |
| Lighthouse Accessibility | TBD | 100 |

---

## 10. Files to Modify

### Primary Changes
1. `frontend-app/app/(workspace)/psychometrics/big-five/page.tsx` - Main layout restructure
2. `frontend-app/app/(workspace)/psychometrics/big-five/_components/BigFiveTraitCard.tsx` - Mobile compact version
3. `frontend-app/app/(workspace)/psychometrics/big-five/_components/BigFiveComparisonChart.tsx` - Vertical bars option
4. `frontend-app/app/(workspace)/psychometrics/big-five/_components/TraitDetailAccordion.tsx` - 2x2 grid fix

### New Components
5. `AlertSummaryCard.tsx` - Problem-first entry point
6. `TraitCardMobile.tsx` - Expandable mobile card
7. `MiniTraitChart.tsx` - Compact vertical bar visualization
8. `TraitDetailSheet.tsx` - Bottom sheet for details (optional)

### CSS
9. `frontend-app/app/globals.css` - Add mobile utility classes

---

## Decision Points for User

Before implementation, please confirm:

1. **Entry Point Strategy**: Alert-first vs. current browse-first?
2. **Chart Style**: Vertical bars vs. radial gauges for mobile?
3. **Detail Navigation**: Bottom sheet vs. inline expansion?
4. **Gesture Support**: Swipe navigation between traits (requires framer-motion)?
5. **Priority Scope**: P0 only vs. full implementation?

---

*Generated: 2025-12-28*
*Agents: ui-designer, ux-research-analyst, workflow-architect, nextjs-dev*
