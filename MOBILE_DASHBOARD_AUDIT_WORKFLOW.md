# Mobile Dashboard Audit & Fix Workflow Plan

## Executive Summary

This document outlines a systematic workflow for auditing and fixing mobile adaptability issues on the SkillSoft dashboard page. The audit covers all dashboard widgets and their responsive behavior across mobile viewports.

---

## Phase 1: Visual Audit (Browser Automation)

### 1.1 Viewport Testing Matrix

| Viewport | Width | Device Representation |
|----------|-------|----------------------|
| Small Mobile | 320px | iPhone SE, older Android |
| Standard Mobile | 375px | iPhone 12/13/14 |
| Large Mobile | 414px | iPhone Plus/Max models |
| Tablet Portrait | 768px | iPad portrait |

### 1.2 Screenshot Capture Plan

**Tool:** Browser automation via `dev-browser` skill or Claude-in-Chrome MCP

**Capture Points:**
1. Full dashboard at 375px viewport (primary mobile)
2. Full dashboard at 320px viewport (edge case)
3. Individual widget screenshots for detailed analysis
4. Horizontal scroll detection (if page width > viewport width)

**Commands:**
```
1. Navigate to dashboard page
2. Resize viewport to 375px width
3. Take full-page screenshot
4. Check document.body.scrollWidth > window.innerWidth (overflow detection)
5. Repeat for 320px, 414px, 768px
```

### 1.3 Audit Checklist

For each component, verify:

- [ ] No horizontal overflow (scrollWidth <= clientWidth)
- [ ] Content fills available width appropriately
- [ ] Touch targets >= 44px (Apple HIG minimum)
- [ ] Text remains readable (minimum 14px for body text)
- [ ] Spacing is proportional (not cramped or excessive)
- [ ] Interactive elements are accessible

---

## Phase 2: Root Cause Analysis

### 2.1 Known Overflow Culprits

Based on code review, these patterns commonly cause mobile overflow:

| Pattern | Risk Level | Example |
|---------|------------|---------|
| Fixed `min-width` values | HIGH | `min-w-[280px]` |
| Hardcoded `width` values | HIGH | `w-[500px]` |
| Missing `overflow-hidden` | MEDIUM | Cards without clipping |
| Flex without `shrink-0` control | MEDIUM | Icons not shrinking |
| Large gaps without responsive variants | LOW | `gap-6` on mobile |

### 2.2 Component-by-Component Analysis

#### 2.2.1 DashboardContent (Main Container)
**File:** `app/(workspace)/dashboard/_components/dashboard-content.tsx`

**Current Implementation:**
```tsx
<div className="flex flex-1 flex-col gap-6 sm:gap-8 p-4 sm:p-6 lg:p-8 max-w-[1600px] mx-auto w-full">
```

**Issues to Check:**
- `max-w-[1600px]` is fine but ensure parent doesn't force overflow
- Header buttons: `flex-col sm:flex-row` pattern is correct
- Pending Assessments card grid: `md:grid-cols-2 lg:grid-cols-3`

**Fix Priority:** LOW (already has responsive patterns)

---

#### 2.2.2 CompactStatsRow / CompactStatsWidget
**File:** `src/components/dashboard/widgets/CompactStatsWidget.tsx`

**Current Implementation:**
```tsx
// Desktop: 4 in a row
<div className="hidden md:grid md:grid-cols-4 gap-3">

// Mobile: 2x2 grid
<div className="grid grid-cols-2 gap-2 md:hidden">
```

**Potential Issues:**
- `CompactStatCard`: Fixed height `h-[64px]` is fine
- `CompactStatCardMobile`: Fixed height `h-[72px]` is fine
- Content inside cards may overflow if text is too long

**Fixes Needed:**
1. Add `overflow-hidden` to card containers
2. Add `truncate` to text that may overflow
3. Ensure icon containers have `shrink-0`

**Fix Priority:** MEDIUM

---

#### 2.2.3 QuickActionsWidget
**File:** `src/components/dashboard/widgets/QuickActionsWidget.tsx`

**Current Implementation:**
```tsx
<div className="space-y-0.5">
  {visibleActions.map((action) => (
    <ActionRow key={action.href} {...action} />
  ))}
</div>
```

**ActionRow:**
```tsx
<div className="flex items-center gap-3 p-3 -mx-3 rounded-lg...">
  <div className="w-9 h-9 rounded-lg...shrink-0">  // Good: shrink-0
  <div className="flex-1 min-w-0">  // Good: min-w-0 prevents overflow
```

**Issues to Check:**
- `-mx-3` negative margin may cause alignment issues at edge cases
- `min-w-0` on content container is correct pattern

**Fix Priority:** LOW (well-implemented)

---

#### 2.2.4 RecentActivityWidget
**File:** `src/components/dashboard/widgets/RecentActivityWidget.tsx`

**Current Implementation:**
```tsx
<div className="flex items-start gap-3 py-2.5">
  <Avatar className="h-7 w-7">  // Fixed size, good
  <div className="flex-1 min-w-0">  // Good: min-w-0
```

**Issues to Check:**
- Activity text may wrap unexpectedly
- Badge group `flex items-center gap-2` may overflow

**Fixes Needed:**
1. Add `flex-wrap` to badge container for mobile
2. Add `truncate` to template name if too long

**Fix Priority:** MEDIUM

---

#### 2.2.5 TestTemplatesWidget
**File:** `src/components/dashboard/widgets/TestTemplatesWidget.tsx`

**Current Implementation:**
```tsx
<div className={cn(
  'grid gap-3',
  compact ? 'grid-cols-1' : 'grid-cols-1 sm:grid-cols-2 lg:grid-cols-3'
)}>
```

**TemplatePreviewCard:**
```tsx
<motion.div className="p-3 rounded-lg border...">
  <div className="flex items-start justify-between gap-2 mb-2">
    <div className="min-w-0 flex-1">
      <p className="text-sm font-medium truncate">  // Good: truncate
```

**Issues to Check:**
- Card content with icons and text may overflow
- Template info row `flex items-center gap-3` may need wrapping

**Fixes Needed:**
1. Add `overflow-hidden` to card container
2. Consider `flex-wrap` on info row for very small screens

**Fix Priority:** MEDIUM

---

#### 2.2.6 PsychometricHealthWidget
**File:** `src/components/dashboard/widgets/PsychometricHealthWidget.tsx`

**Current Implementation:**
```tsx
<div className="flex flex-col md:flex-row items-center gap-4">
  <div className="shrink-0">
    <CircularGauge value={data.healthScore} size={140}.../>  // Fixed 140px
  </div>
  <div className="flex-1 grid grid-cols-2 gap-2 w-full">
```

**Issues to Check:**
- Circular gauge at 140px may be too large for 320px viewport
- Score breakdown legend may overflow

**Fixes Needed:**
1. Make gauge size responsive: `size={isMobile ? 120 : 140}`
2. Add `flex-wrap` to legend container
3. Ensure breakdown bar percentages fit

**Fix Priority:** HIGH (140px gauge on 320px screen is 43% of width)

---

#### 2.2.7 DashboardGrid
**File:** `src/components/dashboard/layout/DashboardGrid.tsx`

**Current Implementation:**
```tsx
<div className={cn(
  'grid gap-4 sm:gap-6',
  'grid-cols-2',      // Mobile: 2 columns
  'md:grid-cols-8',   // Tablet: 8 columns
  'lg:grid-cols-12',  // Desktop: 12 columns
  className
)}>
```

**Issues to Check:**
- 2-column grid on mobile may cause issues if children have min-widths
- Gap of 4 (16px) on mobile takes 16px from available space

**Fix Priority:** LOW (structure is sound)

---

## Phase 3: Fix Implementation Strategy

### 3.1 Priority Order

1. **HIGH Priority** - Causes horizontal scroll
   - PsychometricHealthWidget gauge size
   - Any fixed min-width values on cards

2. **MEDIUM Priority** - Visual issues but no overflow
   - CompactStatsWidget text truncation
   - RecentActivityWidget badge wrapping
   - TestTemplatesWidget info row wrapping

3. **LOW Priority** - Minor refinements
   - Touch target sizing verification
   - Spacing fine-tuning

### 3.2 CSS/Tailwind Patterns to Apply

#### Pattern 1: Prevent Overflow
```tsx
// Before
<div className="p-3">

// After
<div className="p-3 overflow-hidden">
```

#### Pattern 2: Responsive Sizing
```tsx
// Before
<div className="w-[140px]">

// After
<div className="w-[120px] sm:w-[140px]">
```

#### Pattern 3: Text Truncation
```tsx
// Before
<span className="text-sm">{text}</span>

// After
<span className="text-sm truncate">{text}</span>
```

#### Pattern 4: Flexible Wrapping
```tsx
// Before
<div className="flex items-center gap-2">

// After
<div className="flex flex-wrap items-center gap-2">
```

#### Pattern 5: Min-Width Reset for Mobile
```tsx
// Before
<div className="min-w-[280px]">

// After
<div className="min-w-0 sm:min-w-[280px]">
```

#### Pattern 6: Container with Safe Overflow
```tsx
// For any container that might overflow
<div className="w-full max-w-full overflow-x-hidden">
```

### 3.3 Files to Modify (Ordered by Priority)

1. **PsychometricHealthWidget.tsx**
   - Make gauge size responsive
   - Add flex-wrap to legend
   - Ensure metric grid doesn't overflow

2. **CompactStatsWidget.tsx**
   - Add overflow-hidden to cards
   - Verify text truncation

3. **RecentActivityWidget.tsx**
   - Add flex-wrap to badge row
   - Verify text doesn't overflow

4. **TestTemplatesWidget.tsx**
   - Add overflow-hidden to card
   - Consider flex-wrap on info row

5. **dashboard-content.tsx**
   - Verify all grid children have min-w-0
   - Check pending sessions card grid

---

## Phase 4: Verification Checkpoints

### 4.1 Automated Tests (JavaScript Console)

Run in browser console at each breakpoint:

```javascript
// Check for horizontal overflow
function checkOverflow() {
  const hasOverflow = document.body.scrollWidth > window.innerWidth;
  console.log(`Viewport: ${window.innerWidth}px`);
  console.log(`Body scroll width: ${document.body.scrollWidth}px`);
  console.log(`Overflow: ${hasOverflow ? 'YES - PROBLEM!' : 'No'}`);

  // Find overflowing elements
  if (hasOverflow) {
    document.querySelectorAll('*').forEach(el => {
      if (el.scrollWidth > el.clientWidth) {
        console.log('Overflowing element:', el);
      }
    });
  }
  return hasOverflow;
}
checkOverflow();
```

### 4.2 Touch Target Verification

```javascript
// Check touch targets
function checkTouchTargets() {
  const buttons = document.querySelectorAll('button, a, [role="button"]');
  const smallTargets = [];

  buttons.forEach(btn => {
    const rect = btn.getBoundingClientRect();
    if (rect.width < 44 || rect.height < 44) {
      smallTargets.push({
        element: btn,
        width: rect.width,
        height: rect.height
      });
    }
  });

  console.log(`Found ${smallTargets.length} buttons smaller than 44x44`);
  smallTargets.forEach(t => console.log(t));
  return smallTargets;
}
checkTouchTargets();
```

### 4.3 Visual Verification Matrix

| Breakpoint | Overflow Check | Content Width | Touch Targets | Screenshot |
|------------|---------------|---------------|---------------|------------|
| 320px | [ ] Pass | [ ] Fills width | [ ] >= 44px | [ ] Captured |
| 375px | [ ] Pass | [ ] Fills width | [ ] >= 44px | [ ] Captured |
| 414px | [ ] Pass | [ ] Fills width | [ ] >= 44px | [ ] Captured |
| 768px | [ ] Pass | [ ] Fills width | [ ] >= 44px | [ ] Captured |

---

## Phase 5: Implementation Workflow

### Step 1: Create Feature Branch
```bash
git checkout -b fix/dashboard-mobile-responsiveness
```

### Step 2: Visual Audit (30 min)
1. Start dev server: `npm run dev`
2. Use browser DevTools to resize viewport
3. Take screenshots at each breakpoint
4. Run overflow detection script
5. Document all issues found

### Step 3: Implement Fixes (60-90 min)
1. Start with HIGH priority components
2. Make one change at a time
3. Test at 320px after each change
4. Commit after each component is fixed

### Step 4: Cross-Browser Testing (15 min)
1. Test in Chrome DevTools mobile emulation
2. Test in Firefox responsive design mode
3. If available, test on real mobile device

### Step 5: Final Verification (15 min)
1. Run all overflow checks
2. Run touch target checks
3. Visual scan at all breakpoints
4. Verify desktop layout unchanged

### Step 6: Create Pull Request
1. Commit all changes
2. Push to feature branch
3. Create PR with before/after screenshots

---

## Appendix A: Existing Mobile Utilities in globals.css

The project already has extensive mobile utilities that should be leveraged:

### Safe Area Insets
```css
.safe-area-inset-all
.pb-safe, .pt-safe, .px-safe
```

### Touch Targets
```css
.touch-target { min-height: 2.75rem; min-width: 2.75rem; }
.touch-target-min { min-height: 44px; min-width: 44px; }
```

### Mobile Container Prevention
```css
.no-mobile-overflow { overflow-x: hidden; max-width: 100vw; }
.mobile-container { max-width: 100vw; overflow-x: hidden; }
```

### Mobile Card Utilities
```css
.card-mobile-padding { padding: 1rem; }
.card-mobile-spacing > * + * { margin-top: 0.75rem; }
```

### Line Clamping
```css
.line-clamp-2, .line-clamp-3
```

---

## Appendix B: Component Dependency Map

```
DashboardPage
  |
  +-- DashboardContent
        |
        +-- CompactStatsRow
        |     +-- CompactStatCard (desktop)
        |     +-- CompactStatCardMobile (mobile)
        |
        +-- DashboardGrid
        |     +-- PsychometricHealthWidget
        |     +-- CompetencyByCategoryBarChart
        |     +-- Framework Progress Card
        |     +-- Standards Mapping Card
        |     +-- TestTemplatesWidget
        |
        +-- QuickActionsWidget
        +-- RecentActivityWidget
        +-- User Stats Card (admin only)
        +-- Assessment CTA Card
```

---

## Appendix C: Quick Fix Reference

### Add to Any Overflowing Card:
```tsx
className="overflow-hidden"
```

### Add to Text That May Be Too Long:
```tsx
className="truncate"
// or for multi-line:
className="line-clamp-2"
```

### Make Fixed Width Responsive:
```tsx
// Before: w-[140px]
// After: w-[120px] sm:w-[140px]
```

### Ensure Grid Children Don't Overflow:
```tsx
className="min-w-0"
```

### Force Wrapping on Badge/Tag Rows:
```tsx
className="flex flex-wrap gap-2"
```

---

**Document Version:** 1.0
**Created:** 2025-12-26
**Target Components:** Dashboard widgets
**Tech Stack:** Next.js 16, Tailwind CSS 4, shadcn/ui
