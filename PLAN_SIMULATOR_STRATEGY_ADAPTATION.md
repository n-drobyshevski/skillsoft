# Implementation Plan: SimulatorPanel Strategy Adaptation

## Overview

Adapt the test-templates/[id]/builder simulate tab (SimulatorPanel) to show scenario-specific content based on the test template's strategy type, with mobile-first responsive design.

## Strategies Supported

| Strategy | Purpose | Key Data |
|----------|---------|----------|
| `UNIVERSAL_BASELINE` | General competency assessment | Competencies, weights |
| `TARGETED_FIT` | Job-specific assessment | O*NET SOC code, job requirements |
| `DYNAMIC_GAP_ANALYSIS` | Team gap analysis | Team ID, team benchmark data |

---

## Phase 1: Core Infrastructure (Files: 4, Priority: HIGH)

### 1.1 Create Strategy Context & Theme System

**File:** `simulator/strategy-context.ts`

```typescript
// Strategy type definitions
export type Strategy = 'UNIVERSAL_BASELINE' | 'TARGETED_FIT' | 'DYNAMIC_GAP_ANALYSIS';

// Strategy configuration registry with:
// - label, description, icon
// - color classes (border, bg, iconBg, iconText, accentGradient)
// - available sections with priorities
// - validation requirements

export const STRATEGY_CONFIG: Record<Strategy, StrategyDisplayConfig>;
```

**Rationale:** Centralizes all strategy-specific configuration for consistent theming.

### 1.2 Create Simulator State Machine Hook

**File:** `simulator/use-simulator-state.ts`

```
State Machine:
IDLE → READY → SIMULATING → RESULTS
         ↓           ↓
       STALE ← ERROR
```

- Tracks: state, profile, strategyContext, result, error, blueprintHash, activeSection
- Detects blueprint changes to mark results as "stale"
- Syncs with BlueprintWorkspaceProvider

### 1.3 Create Strategy Header Component

**File:** `simulator/StrategyHeroBadge.tsx`

- Compact variant (desktop): Badge with icon + short label
- Expanded variant (mobile): Full card with icon, label, description
- Tooltip with strategy-specific help content
- Visual warning indicator when configuration is incomplete

### 1.4 Create Error Boundary

**File:** `simulator/SimulatorErrorBoundary.tsx`

- Catches rendering errors in simulator sections
- Shows friendly error message with retry button
- Prevents cascade failures

---

## Phase 2: Strategy-Specific Score Displays (Files: 2, Priority: HIGH)

### 2.1 Create Strategy Score Display

**File:** `simulator/StrategyScoreDisplay.tsx`

| Strategy | Display Type | Key Elements |
|----------|-------------|--------------|
| UNIVERSAL_BASELINE | Competency count | No pass/fail, "Discovery Mode" badge |
| TARGETED_FIT | Job Fit % | Progress bar + pass/fail threshold marker |
| DYNAMIC_GAP_ANALYSIS | Gap comparison | Individual vs team benchmark bars |

- Color-coded by persona (Perfect=emerald, Random=amber, Failing=red)
- Strategy-specific accent gradients
- Missing config warnings (no O*NET code, no team selected)

### 2.2 Create Empty/Loading States

**File:** `simulator/StrategyEmptyState.tsx`
**File:** `simulator/StrategyLoadingSkeleton.tsx`

Three empty state types:
- `no-competencies`: "Add competencies to simulate"
- `no-simulation`: "Click Run to simulate"
- `missing-config`: Strategy-specific configuration prompt

Loading skeletons adapt shape based on strategy.

---

## Phase 3: Strategy-Specific Insight Components (Files: 4, Priority: MEDIUM)

### 3.1 Job Fit Alignment Card (TARGETED_FIT)

**File:** `simulator/JobFitAlignmentCard.tsx`

```
+------------------------------------------+
| Job Fit Alignment                        |
+------------------------------------------+
| [Briefcase] O*NET: 15-1252.00           |
| Software Developer                       |
+------------------------------------------+
| Requirement Coverage: [=========] 72%    |
+------------------------------------------+
| Development Areas:                       |
| - Problem Solving (-18%)                 |
| Strengths: [Communication] [Leadership]  |
+------------------------------------------+
```

### 3.2 Team Comparison Card (DYNAMIC_GAP_ANALYSIS)

**File:** `simulator/TeamComparisonCard.tsx`

```
+------------------------------------------+
| Team Comparison        [+5% overall]     |
+------------------------------------------+
| [Users] Engineering Team                 |
+------------------------------------------+
| Competency | Individual | Team Avg | Gap |
| Leadership |    72%     |   68%    | +4% |
| Technical  |    65%     |   78%    | -13%|
+------------------------------------------+
| Legend: [Team Avg] [Individual]          |
+------------------------------------------+
```

### 3.3 Competency Balance Section (UNIVERSAL_BASELINE)

**File:** `simulator/sections/CompetencyBalanceSection.tsx`

- Coverage distribution across competencies
- Weight balance indicator
- "Competency Passport" preview concept

### 3.4 Strategy Insights Tab (Factory)

**File:** `simulator/tabs/StrategyInsightsTab.tsx`

Renders appropriate insight component based on strategy:
- UNIVERSAL_BASELINE → CompetencyBalanceSection
- TARGETED_FIT → JobFitAlignmentCard
- DYNAMIC_GAP_ANALYSIS → TeamComparisonCard

---

## Phase 4: Data Layer & API Integration (Files: 2, Priority: MEDIUM)

### 4.1 Strategy Data Hook

**File:** `simulator/hooks/use-strategy-data.ts`

```typescript
function useStrategyData(strategy, onetSocCode?, teamId?) {
  // TARGETED_FIT: Fetch O*NET job data (title, requirements)
  // DYNAMIC_GAP_ANALYSIS: Fetch team data (name, members, averageScores)
  // Returns: { data, isLoading, error }
}
```

### 4.2 Server Actions for Strategy Data

**File:** `builder/actions.ts` (extend existing)

```typescript
// New server actions:
export async function fetchOnetJobData(socCode: string);
export async function fetchTeamBenchmarkData(teamId: string);
```

---

## Phase 5: Updated SimulatorPanel Integration (Files: 1, Priority: HIGH)

### 5.1 Refactor SimulatorPanel

**File:** `simulator/SimulatorPanel.tsx`

Changes:
1. Add `StrategyHeroBadge` at top of header
2. Replace `ScoreDisplay` with `StrategyScoreDisplay`
3. Add new "Insights" tab/section between Timeline and Statistics
4. Update Help tab content to be strategy-specific
5. Mobile: Use strategy-specific `defaultOpen` for collapsibles
6. Desktop: Add 5th tab for "Insights"

**Layout Changes:**

Mobile (Collapsible Sections):
```
[Strategy Badge - expanded]
[Persona Selector + Run]
[Strategy Score Display]
[Stats Cards]
[Warnings]
▼ Timeline (default open)
▼ Strategy Insights (NEW - default open for TARGETED_FIT/DYNAMIC_GAP)
▼ Statistics
▼ Fine Tune
▼ Help
```

Desktop (Tabs):
```
[Strategy Badge - compact] [Persona] [Run]
[Strategy Score Display]
[Stats] [Warnings]
[Timeline | Insights | Stats | Tune | Help]
```

---

## Phase 6: Mobile-First Enhancements (Files: 2, Priority: MEDIUM)

### 6.1 Touch Target Improvements

Update across all simulator components:
- Slider thumbs: 24px → 32px
- Tab triggers: 52px → 56px minimum
- Ensure all interactive elements meet 44x44px minimum

### 6.2 Microinteractions

**File:** `simulator/hooks/use-simulation-feedback.ts`

- Haptic feedback on persona selection (navigator.vibrate)
- Score reveal animation (spring animation)
- Warning toast on issues detected
- Pull-to-refresh for re-simulation (optional)

---

## Phase 7: Accessibility Improvements (Priority: HIGH)

### 7.1 ARIA Enhancements

- PersonaSelector: Add `aria-pressed`, `aria-label`
- Charts: Add `role="img"` with descriptive `aria-label`
- Collapsibles: Announce expansion state
- Color-only warnings: Add icons + sr-only text

### 7.2 Focus Management

- Logical tab order verification
- Focus trapping in expanded sections
- Screen reader announcements for state changes

---

## File Structure (Final)

```
simulator/
├── SimulatorPanel.tsx              # Main (UPDATED)
├── strategy-context.ts             # NEW
├── use-simulator-state.ts          # NEW
├── StrategyHeroBadge.tsx           # NEW
├── StrategyScoreDisplay.tsx        # NEW
├── StrategyEmptyState.tsx          # NEW
├── StrategyLoadingSkeleton.tsx     # NEW
├── SimulatorErrorBoundary.tsx      # NEW
├── JobFitAlignmentCard.tsx         # NEW
├── TeamComparisonCard.tsx          # NEW
├── hooks/
│   ├── use-strategy-data.ts        # NEW
│   └── use-simulation-feedback.ts  # NEW (optional)
├── sections/
│   └── CompetencyBalanceSection.tsx # NEW
├── tabs/
│   ├── AnalyticsTab.tsx            # Existing
│   ├── TimelineTab.tsx             # UPDATED (strategy prop)
│   ├── FineTuneTab.tsx             # Existing
│   └── StrategyInsightsTab.tsx     # NEW
├── PersonaSelector.tsx             # UPDATED (accessibility)
├── ScoreDisplay.tsx                # Existing (can deprecate)
├── WarningsList.tsx                # Existing
└── types.ts                        # UPDATED
```

---

## Implementation Order

| Order | Phase | Effort | Dependencies |
|-------|-------|--------|--------------|
| 1 | Phase 1.1-1.4 (Infrastructure) | 2-3 days | None |
| 2 | Phase 2.1-2.2 (Score Displays) | 1-2 days | Phase 1 |
| 3 | Phase 5.1 (SimulatorPanel refactor) | 2 days | Phase 1, 2 |
| 4 | Phase 3.1-3.4 (Insight Components) | 2-3 days | Phase 1 |
| 5 | Phase 4.1-4.2 (Data Layer) | 1-2 days | Phase 3 |
| 6 | Phase 6.1-6.2 (Mobile Enhancements) | 1 day | Phase 5 |
| 7 | Phase 7.1-7.2 (Accessibility) | 1 day | All |

**Total Estimated Effort:** 10-14 days

---

## Success Metrics (A/B Testing)

| Metric | Definition | Target |
|--------|------------|--------|
| Simulation-to-Publish Rate | % users who simulate AND publish | +15% |
| Time to First Simulation | Time from builder open to first run | -20% |
| Mobile Completion Rate | % mobile users completing simulation | +25% |
| Tab/Section Engagement | Which sections are opened most | Track |

---

## Drift Alerts

1. **Duplicate SimulatorPanel files:** Consolidate `_components/SimulatorPanel.tsx` (older) with `_components/simulator/SimulatorPanel.tsx` (newer, modular)

2. **Strategy mapping:** Current `AssessmentGoal` enum (OVERVIEW, JOB_FIT, TEAM_FIT) maps to these strategies - ensure consistent naming

3. **Unused Blueprint fields:** `onetSocCode` and `teamId` exist in BlueprintState but not surfaced in simulator - this plan addresses that

4. **Big Five projection:** `useBigFiveProjection` hook exists in results but not simulator preview - consider for DYNAMIC_GAP_ANALYSIS
