# Test Results Page Redesign - Workflow Plan

## Executive Summary

This document outlines the comprehensive workflow for redesigning the test results page (`test-templates/results/[resultId]/page.tsx`) to support three distinct assessment scenarios with appropriate UX patterns and data visualizations.

**Current Issue:** All scenarios use the same score-focused layout with pass/fail badges, which is inappropriate for Scenario A (OVERVIEW/Competency Passport) that should focus on profile building rather than scoring.

**Goal:** Implement a Strategy pattern-based component architecture that delivers scenario-appropriate visualizations and messaging.

---

## 1. Current State Analysis

### 1.1 Three Assessment Scenarios

Based on `AssessmentGoal` enum and backend scoring strategies:

#### Scenario A: OVERVIEW (Universal Baseline - Triple-Standard Aggregation)
- **Purpose:** Generate "Competency Passport" with Big Five personality profile
- **Scoring:** OverviewScoringStrategy - aggregates O*NET, ESCO, Big Five mappings
- **Key Output:** Psychological profile, not competitive ranking
- **Blueprint:** OverviewBlueprint (no specific requirements, uses UNIVERSAL context scope)
- **Visualization Needs:**
  - Big Five radar chart (OCEAN model pentagon)
  - Competency profile WITHOUT pass/fail judgment
  - Personality trait descriptions
  - NO score percentage in hero section
  - NO pass/fail badges
  - Focus on "This is who you are" messaging

#### Scenario B: JOB_FIT (O*NET Benchmark Injection)
- **Purpose:** Compare candidate against occupation benchmarks
- **Scoring:** JobFitScoringStrategy - compares to O*NET SOC code requirements
- **Key Output:** Gap analysis with benchmark comparison
- **Blueprint:** JobFitBlueprint with `onet_soc_code`
- **Visualization Needs:**
  - Gap analysis bar chart (actual vs. target)
  - Pass/fail based on meeting benchmarks
  - Score percentage meaningful (% of benchmark met)
  - Competency radar with target overlay
  - Recommendations for improvement areas

#### Scenario C: TEAM_FIT (ESCO Gap Analysis)
- **Purpose:** Analyze skill gaps and personality fit within a team
- **Scoring:** TeamFitScoringStrategy - normalizes via ESCO URIs for team comparison
- **Blueprint:** TeamFitBlueprint with `team_id`
- **Visualization Needs:**
  - Team skill gap visualization
  - Personality fit with team dynamics
  - Pass/fail based on team compatibility
  - Contextual team information
  - Complementary skills identification

### 1.2 Current Implementation Problems

**Location:** `D:\projects\diplom\skillsoft\frontend-app\app\(workspace)\test-templates\results\[resultId]\page.tsx`

**Issues:**
1. **Unified Layout:** Lines 108-335 show single hero section for all scenarios
2. **Inappropriate Scoring Display:** Lines 188-235 always show score percentage circle
3. **Pass/Fail Always Shown:** Lines 235-247 show pass/fail badge even for Scenario A
4. **No Big Five Visualization:** Missing for Scenario A despite backend support
5. **Conditional Gap Analysis:** Lines 97, 355-362 show gap chart only for B/C, but Scenario A still has score-focused messaging
6. **Hard-coded Messages:** Lines 146-147, 406-437 use pass/fail language inappropriate for profiles

**Current Data Flow:**
```typescript
TestResult {
  templateId: string
  templateName: string
  passed: boolean          // ❌ Should NOT be shown for OVERVIEW
  overallPercentage: number // ❌ Should NOT be shown for OVERVIEW
  competencyScores: CompetencyScore[] {
    onetCode?: string      // ✅ Used for Big Five projection
    percentage: number
    ...
  }
}
```

### 1.3 Available Components and Hooks

**Big Five Support (Already Implemented):**
- `D:\projects\diplom\skillsoft\frontend-app\src\components\charts\BigFiveRadar.tsx`
  - BigFiveRadar component (with card wrapper)
  - BigFiveRadarSimple (embeddable version)
  - TRAIT_DESCRIPTIONS for tooltips

- `D:\projects\diplom\skillsoft\frontend-app\src\hooks\useBigFiveProjection.ts`
  - `useBigFiveProjection(competencyScores)` hook
  - Projects competency scores → Big Five profile via O*NET mapping
  - Returns BigFiveProfile with 0-100 scores for OCEAN traits

**Existing Visualization Components:**
- `CompetencyRadarChart` - Current competency visualization
- `GapAnalysisBarChart` - Bar chart for gap analysis (B/C scenarios)
- Compact stat cards and competency accordions

---

## 2. Component Architecture Design

### 2.1 Strategy Pattern Implementation

**File Structure:**
```
app/(workspace)/test-templates/results/[resultId]/
├── page.tsx                              # Router - delegates to correct view
├── _components/
│   ├── ResultViewFactory.tsx             # Factory for scenario selection
│   ├── overview/
│   │   ├── OverviewResultView.tsx        # Scenario A main component
│   │   ├── CompetencyPassportHero.tsx    # Hero without scores/badges
│   │   ├── BigFivePersonalityCard.tsx    # Big Five radar + descriptions
│   │   └── ProfileCompetenciesCard.tsx   # Competencies as profile traits
│   ├── job-fit/
│   │   ├── JobFitResultView.tsx          # Scenario B main component
│   │   ├── JobFitHero.tsx                # Hero with score + pass/fail
│   │   ├── GapAnalysisCard.tsx           # Gap visualization
│   │   └── BenchmarkComparisonCard.tsx   # O*NET benchmark context
│   ├── team-fit/
│   │   ├── TeamFitResultView.tsx         # Scenario C main component
│   │   ├── TeamFitHero.tsx               # Hero with team context
│   │   ├── TeamGapCard.tsx               # Team skill gaps
│   │   └── TeamDynamicsCard.tsx          # Personality fit with team
│   └── shared/
│       ├── CompetencyDetailAccordion.tsx # Reusable competency details
│       ├── ActionButtonsBar.tsx          # Reusable action buttons
│       └── types.ts                      # Shared interfaces
```

### 2.2 Component Interfaces

```typescript
// _components/shared/types.ts

import { TestResult, TestTemplate, AssessmentGoal } from '@/types/domain';

/**
 * Base props for all result view components
 */
export interface BaseResultViewProps {
  result: TestResult;
  template: TestTemplate;
}

/**
 * Extended props for specific scenarios
 */
export interface OverviewResultViewProps extends BaseResultViewProps {
  // No additional props needed
}

export interface JobFitResultViewProps extends BaseResultViewProps {
  onetSocCode: string; // From template.blueprint.onet_soc_code
  benchmarkData?: OnetBenchmark; // Future: fetch from external service
}

export interface TeamFitResultViewProps extends BaseResultViewProps {
  teamId: string; // From template.blueprint.team_id
  teamData?: TeamContext; // Future: fetch from team service
}

/**
 * Factory function return type
 */
export type ResultViewComponent = React.ComponentType<BaseResultViewProps>;
```

### 2.3 Factory Pattern Implementation

```typescript
// _components/ResultViewFactory.tsx

import { AssessmentGoal } from '@/types/domain';
import { BaseResultViewProps } from './shared/types';
import { OverviewResultView } from './overview/OverviewResultView';
import { JobFitResultView } from './job-fit/JobFitResultView';
import { TeamFitResultView } from './team-fit/TeamFitResultView';

/**
 * Factory function to select the appropriate result view based on assessment goal
 */
export function getResultViewComponent(goal: AssessmentGoal) {
  const viewMap = {
    [AssessmentGoal.OVERVIEW]: OverviewResultView,
    [AssessmentGoal.JOB_FIT]: JobFitResultView,
    [AssessmentGoal.TEAM_FIT]: TeamFitResultView,
  };

  return viewMap[goal] ?? OverviewResultView; // Default to OVERVIEW
}

/**
 * HOC wrapper for consistent error boundaries and suspense
 */
export function ResultViewWrapper({ result, template }: BaseResultViewProps) {
  const ViewComponent = getResultViewComponent(template.goal);

  return (
    <ErrorBoundary fallback={<ResultErrorFallback />}>
      <Suspense fallback={<ResultsSkeleton />}>
        <ViewComponent result={result} template={template} />
      </Suspense>
    </ErrorBoundary>
  );
}
```

---

## 3. Data Flow Design

### 3.1 Page Router (Entry Point)

**File:** `app/(workspace)/test-templates/results/[resultId]/page.tsx`

```typescript
// Simplified page.tsx with factory delegation

export default async function TestResultsPage({ params }: PageProps) {
  const { resultId } = await params;

  return (
    <Suspense fallback={<ResultsSkeleton />}>
      <ResultsContent resultId={resultId} />
    </Suspense>
  );
}

async function ResultsContent({ resultId }: { resultId: string }) {
  // Fetch result (try by ID, fallback to session ID)
  const result = await fetchTestResult(resultId);
  if (!result) notFound();

  // Fetch template to determine visualization strategy
  const template = await testTemplatesApi.getTemplateById(result.templateId);

  // Delegate to factory
  return <ResultViewWrapper result={result} template={template} />;
}
```

### 3.2 Scenario A: Overview Result View

**Component:** `_components/overview/OverviewResultView.tsx`

**Data Requirements:**
```typescript
{
  result: {
    competencyScores: CompetencyScore[] // WITH onetCode for Big Five projection
    totalTimeSeconds: number
    questionsAnswered: number
    completedAt: string
    // ❌ DO NOT USE: passed, overallPercentage
  },
  template: {
    name: string
    goal: AssessmentGoal.OVERVIEW
    // No blueprint requirements
  }
}
```

**Visual Layout:**
```
┌─────────────────────────────────────────────────────────┐
│  COMPETENCY PASSPORT HERO                                │
│  ┌─────┐  Competency Profile                            │
│  │ 📋 │  [Template Name]                                │
│  └─────┘  Completed: [Date]                             │
│           Questions answered: [N] • Time: [MM:SS]       │
│  ─────────────────────────────────────────────────────  │
│  📊 View Full Report  •  🔄 Take Again  •  💾 Save      │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  BIG FIVE PERSONALITY PROFILE                           │
│  ┌───────────────────────────────────────────────────┐  │
│  │         [Pentagon Radar Chart]                    │  │
│  │   O - Openness: 72                                │  │
│  │   C - Conscientiousness: 85                       │  │
│  │   E - Extraversion: 58                            │  │
│  │   A - Agreeableness: 78                           │  │
│  │   ES - Emotional Stability: 65                    │  │
│  └───────────────────────────────────────────────────┘  │
│                                                          │
│  Your personality profile shows...                      │
│  - High Conscientiousness: Organized, dependable        │
│  - Moderate Openness: Balanced creativity               │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  COMPETENCY PROFILE                                      │
│  Your strengths across [N] competencies:                │
│                                                          │
│  Communication         ████████████░░░ 85               │
│  Leadership            ██████████░░░░░ 72               │
│  Critical Thinking     ███████░░░░░░░░ 58               │
│  [Expand for details ▼]                                 │
└─────────────────────────────────────────────────────────┘
```

**Key Messaging Differences:**
- ❌ "Congratulations! You passed" → ✅ "Your Competency Profile"
- ❌ "85% score" → ✅ "Profile across 5 competencies"
- ❌ Pass/fail badge → ✅ "Profile" or "Passport" icon
- ❌ Gap analysis → ✅ Big Five personality dimensions

### 3.3 Scenario B: Job Fit Result View

**Component:** `_components/job-fit/JobFitResultView.tsx`

**Data Requirements:**
```typescript
{
  result: {
    passed: boolean           // ✅ Show pass/fail
    overallPercentage: number // ✅ Show score percentage
    competencyScores: CompetencyScore[]
    // ... all fields
  },
  template: {
    blueprint: {
      onet_soc_code: string // e.g., "15-1252.00" (Software Developer)
    },
    passingScore: number // Benchmark threshold
  }
}
```

**Visual Layout:**
```
┌─────────────────────────────────────────────────────────┐
│  JOB FIT ASSESSMENT                                      │
│  ┌─────┐  85% Match                      ✅ QUALIFIED   │
│  │ 🎯 │  [Template Name]                                │
│  └─────┘  vs. [O*NET Job Title]                        │
│           Score: 85/100 • 92nd percentile               │
│  ─────────────────────────────────────────────────────  │
│  📄 Download Report  •  🔄 Retake  •  📧 Share         │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  GAP ANALYSIS                                           │
│  [Bar Chart: Your Score vs. Benchmark]                 │
│                                                          │
│  Communication:     ████████████░░░ 85 | Target: 70 ✅  │
│  Leadership:        ██████████░░░░░ 72 | Target: 70 ✅  │
│  Critical Thinking: ███████░░░░░░░░ 58 | Target: 70 ⚠️  │
│                                                          │
│  Strengths: 2 competencies exceed requirements          │
│  Growth Areas: Focus on Critical Thinking               │
└─────────────────────────────────────────────────────────┘
```

### 3.4 Scenario C: Team Fit Result View

**Component:** `_components/team-fit/TeamFitResultView.tsx`

**Data Requirements:**
```typescript
{
  result: {
    passed: boolean           // ✅ Show team compatibility
    overallPercentage: number // ✅ Show fit percentage
    competencyScores: CompetencyScore[]
    // ... all fields
  },
  template: {
    blueprint: {
      team_id: string // Team identifier
    }
  },
  teamData?: {
    teamName: string
    averageSkills: Record<string, number>
    personalityProfile: BigFiveProfile
    // Future: from TeamService
  }
}
```

**Visual Layout:**
```
┌─────────────────────────────────────────────────────────┐
│  TEAM FIT ANALYSIS                                       │
│  ┌─────┐  78% Team Fit              ✅ GOOD FIT         │
│  │ 👥 │  [Template Name]                                │
│  └─────┘  Team: [Team Name]                            │
│           Complements 4 members • Adds value in 2 areas │
│  ─────────────────────────────────────────────────────  │
│  📊 Team Dashboard  •  🔄 Retake  •  📩 Share          │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  SKILL GAPS & CONTRIBUTIONS                             │
│  [Stacked Bar: Your Skills vs. Team Average]            │
│                                                          │
│  Your Strengths (above team average):                   │
│  • Communication (+15)                                  │
│  • Leadership (+8)                                      │
│                                                          │
│  Team Coverage Gaps (below average):                    │
│  • Critical Thinking (-12)                              │
│  • Technical Skills (-5)                                │
└─────────────────────────────────────────────────────────┘
```

---

## 4. Implementation Order

### Phase 1: Foundation Setup (Day 1)
**Tasks:**
1. Create folder structure (`_components/overview/`, `job-fit/`, `team-fit/`, `shared/`)
2. Implement `shared/types.ts` with interfaces
3. Create `ResultViewFactory.tsx` with factory function
4. Update `page.tsx` to use factory delegation
5. Create skeleton components for each scenario (placeholders)

**Deliverables:**
- Basic routing works
- Correct component loads per scenario
- No visual changes yet (use placeholders)

### Phase 2: Scenario A - Overview/Competency Passport (Day 2-3)
**Tasks:**
1. Implement `CompetencyPassportHero.tsx`
   - Remove score circle
   - Remove pass/fail badge
   - Add "Competency Profile" messaging
   - Show completion stats (time, questions)

2. Implement `BigFivePersonalityCard.tsx`
   - Integrate `useBigFiveProjection` hook
   - Render `BigFiveRadar` component
   - Add trait descriptions
   - Handle edge case: no O*NET mappings

3. Implement `ProfileCompetenciesCard.tsx`
   - Display competencies as profile traits (not scores)
   - Use neutral language ("strength", "developing area")
   - NO pass/fail color coding

4. Integrate into `OverviewResultView.tsx`
   - Assemble hero + Big Five + competencies
   - Add action buttons (Download Profile, Retake)

**Deliverables:**
- Scenario A shows profile view (no scores/badges)
- Big Five radar visualized
- Tests pass for OVERVIEW goal

### Phase 3: Scenario B - Job Fit (Day 4)
**Tasks:**
1. Implement `JobFitHero.tsx`
   - Keep score circle and pass/fail badge
   - Add O*NET job title from blueprint
   - Show benchmark comparison

2. Implement `GapAnalysisCard.tsx`
   - Reuse existing `GapAnalysisBarChart`
   - Add target lines for benchmarks
   - Highlight gaps vs. strengths

3. Implement `BenchmarkComparisonCard.tsx`
   - Show O*NET SOC code context
   - Display job requirements
   - Recommendations for gaps

4. Integrate into `JobFitResultView.tsx`

**Deliverables:**
- Scenario B shows gap analysis
- O*NET context displayed
- Tests pass for JOB_FIT goal

### Phase 4: Scenario C - Team Fit (Day 5)
**Tasks:**
1. Implement `TeamFitHero.tsx`
   - Team context in hero
   - Team compatibility badge
   - Contribution highlights

2. Implement `TeamGapCard.tsx`
   - Comparison with team average
   - Stacked bar chart (optional)
   - Skill complement analysis

3. Implement `TeamDynamicsCard.tsx`
   - Personality fit visualization
   - Team Big Five overlay (if available)

4. Integrate into `TeamFitResultView.tsx`

**Deliverables:**
- Scenario C shows team context
- Team comparisons visualized
- Tests pass for TEAM_FIT goal

### Phase 5: Shared Components & Polish (Day 6)
**Tasks:**
1. Extract `CompetencyDetailAccordion.tsx` (reusable across scenarios)
2. Extract `ActionButtonsBar.tsx` (download, retake, share)
3. Mobile responsiveness testing
4. Accessibility audit (ARIA labels, keyboard navigation)
5. Error state handling
6. Loading skeleton improvements

**Deliverables:**
- All scenarios responsive
- Accessibility compliant
- Shared components extracted

### Phase 6: Testing & Documentation (Day 7)
**Tasks:**
1. Write component tests for each scenario
2. Write integration tests (routing to correct view)
3. Visual regression tests
4. Update CLAUDE.md with new architecture
5. Create migration guide for future changes

**Deliverables:**
- Test coverage > 70%
- Documentation complete
- Ready for deployment

---

## 5. Risks and Mitigation

### 5.1 Data Availability Risks

**Risk:** Big Five projection requires O*NET codes in `CompetencyScore.onetCode`
- **Likelihood:** Medium
- **Impact:** High (Scenario A broken without Big Five)
- **Mitigation:**
  - Verify backend includes `onetCode` in `CompetencyScoreDto`
  - Add fallback: if no O*NET codes, show message "Personality profile unavailable"
  - Show competency profile only (without Big Five radar)

**Risk:** Team context data not available from backend
- **Likelihood:** High (team service may not be fully implemented)
- **Impact:** Medium (Scenario C reduced functionality)
- **Mitigation:**
  - Phase 1: Show individual results only (no team comparison)
  - Phase 2: Add team service integration when available
  - Use mock data for development/testing

### 5.2 UX Consistency Risks

**Risk:** Three different layouts may confuse users
- **Likelihood:** Medium
- **Impact:** Medium (user confusion, support tickets)
- **Mitigation:**
  - Add clear header: "Assessment Type: [Competency Passport | Job Fit | Team Fit]"
  - Consistent action button placement
  - Similar visual hierarchy across scenarios
  - User education (tooltips, help text)

**Risk:** Users expect scores even for Scenario A
- **Likelihood:** High (common expectation from assessments)
- **Impact:** Medium (user dissatisfaction)
- **Mitigation:**
  - Clear messaging: "This is a profile, not a test"
  - Provide downloadable report with more details
  - User feedback collection to validate approach

### 5.3 Technical Risks

**Risk:** Factory pattern adds complexity
- **Likelihood:** Low
- **Impact:** Low (maintainability concern)
- **Mitigation:**
  - Clear documentation
  - TypeScript strict mode for type safety
  - Component tests for each scenario
  - Code review for factory logic

**Risk:** Big Five projection algorithm inaccurate
- **Likelihood:** Medium (depends on O*NET mapping quality)
- **Impact:** High (incorrect personality profiles)
- **Mitigation:**
  - Validate mapping data (`onet_to_bigfive_map.json`)
  - Add disclaimer: "Personality profile is indicative, not diagnostic"
  - Allow users to hide/collapse Big Five if inaccurate
  - Collect feedback for mapping improvements

**Risk:** Mobile layout breaks with three different views
- **Likelihood:** Medium
- **Impact:** High (mobile users frustrated)
- **Mitigation:**
  - Mobile-first development
  - Test on real devices (iOS, Android)
  - Responsive design system (consistent breakpoints)
  - Progressive disclosure (collapse sections on mobile)

---

## 6. Component Breakdown Detail

### 6.1 Scenario A: OverviewResultView

**File:** `_components/overview/OverviewResultView.tsx`

```typescript
'use client';

import { BigFiveRadar } from '@/components/charts/BigFiveRadar';
import { useBigFiveProjection } from '@/hooks/useBigFiveProjection';
import { CompetencyPassportHero } from './CompetencyPassportHero';
import { ProfileCompetenciesCard } from './ProfileCompetenciesCard';
import { ActionButtonsBar } from '../shared/ActionButtonsBar';
import { BaseResultViewProps } from '../shared/types';

export function OverviewResultView({ result, template }: BaseResultViewProps) {
  // Project competencies → Big Five personality profile
  const bigFiveProfile = useBigFiveProjection(result.competencyScores);

  return (
    <div className="min-h-screen bg-muted/30 py-4 md:py-8">
      <div className="container max-w-7xl mx-auto px-4 space-y-4">
        {/* Hero without scores/badges */}
        <CompetencyPassportHero
          templateName={result.templateName}
          completedAt={result.completedAt}
          questionsAnswered={result.questionsAnswered}
          totalQuestions={result.totalQuestions}
          timeSpent={result.totalTimeSeconds}
        />

        {/* Big Five Personality Profile */}
        <BigFiveRadar
          profile={bigFiveProfile}
          title="Personality Profile (Big Five OCEAN Model)"
          description="Your personality dimensions derived from competency assessment"
        />

        {/* Competency Profile (not scored) */}
        <ProfileCompetenciesCard
          competencies={result.competencyScores}
          showAsProfile={true} // ❌ No pass/fail colors
        />

        {/* Action buttons */}
        <ActionButtonsBar
          templateId={result.templateId}
          resultId={result.id}
          actions={['download_profile', 'retake', 'save_to_profile']}
        />
      </div>
    </div>
  );
}
```

**CompetencyPassportHero Component:**

```typescript
interface CompetencyPassportHeroProps {
  templateName: string;
  completedAt: string;
  questionsAnswered: number;
  totalQuestions: number;
  timeSpent: number;
}

export function CompetencyPassportHero(props: CompetencyPassportHeroProps) {
  const { templateName, completedAt, questionsAnswered, totalQuestions, timeSpent } = props;

  return (
    <Card className="border-2 border-primary/20 bg-gradient-to-br from-primary/5 to-muted">
      <CardContent className="p-6">
        <div className="flex items-center gap-4">
          {/* Icon: Clipboard/Passport instead of Trophy */}
          <div className="rounded-full p-4 border-2 border-primary/30 bg-primary/10">
            <FileText className="h-10 w-10 text-primary" />
          </div>

          <div className="flex-1">
            <h1 className="text-2xl font-bold text-primary">
              Your Competency Profile
            </h1>
            <p className="text-lg text-muted-foreground">{templateName}</p>
            <div className="flex gap-4 mt-2 text-sm text-muted-foreground">
              <span>Completed: {new Date(completedAt).toLocaleString()}</span>
              <span>Questions: {questionsAnswered}/{totalQuestions}</span>
              <span>Time: {formatDuration(timeSpent)}</span>
            </div>
          </div>
        </div>

        {/* ❌ NO SCORE CIRCLE */}
        {/* ❌ NO PASS/FAIL BADGE */}
      </CardContent>
    </Card>
  );
}
```

**ProfileCompetenciesCard Component:**

```typescript
interface ProfileCompetenciesCardProps {
  competencies: CompetencyScore[];
  showAsProfile: boolean; // true = profile view, false = score view
}

export function ProfileCompetenciesCard({ competencies, showAsProfile }: ProfileCompetenciesCardProps) {
  // Sort by percentage descending
  const sorted = [...competencies].sort((a, b) => b.percentage - a.percentage);

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <BarChart3 className="h-5 w-5" />
          Competency Profile
        </CardTitle>
        <CardDescription>
          Your strengths across {competencies.length} competencies
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        {sorted.map(comp => (
          <div key={comp.competencyId} className="flex items-center gap-3 py-2">
            {/* Competency name */}
            <span className="flex-1 text-sm font-medium">{comp.competencyName}</span>

            {/* Progress bar (neutral colors, not pass/fail) */}
            <div className="w-32 h-2 bg-muted/30 rounded-full overflow-hidden">
              <div
                className="h-full bg-gradient-to-r from-primary/60 to-primary rounded-full"
                style={{ width: `${comp.percentage}%` }}
              />
            </div>

            {/* Percentage (neutral presentation) */}
            <span className="text-sm font-semibold text-muted-foreground w-12 text-right">
              {Math.round(comp.percentage)}%
            </span>

            {/* ❌ NO color coding for pass/fail */}
            {/* ❌ NO badges like "Excellent" / "Needs Improvement" */}
          </div>
        ))}
      </CardContent>
    </Card>
  );
}
```

### 6.2 Scenario B: JobFitResultView

**File:** `_components/job-fit/JobFitResultView.tsx`

```typescript
export function JobFitResultView({ result, template }: BaseResultViewProps) {
  const onetSocCode = template.blueprint?.onet_soc_code;
  const isPassed = result.passed;

  return (
    <div className="min-h-screen bg-muted/30 py-4 md:py-8">
      <div className="container max-w-7xl mx-auto px-4 space-y-4">
        {/* Hero WITH scores and pass/fail */}
        <JobFitHero
          result={result}
          templateName={result.templateName}
          onetSocCode={onetSocCode}
          isPassed={isPassed}
        />

        {/* Gap Analysis Chart */}
        <GapAnalysisCard
          competencies={result.competencyScores}
          passingScore={template.passingScore}
        />

        {/* Benchmark Context */}
        {onetSocCode && (
          <BenchmarkComparisonCard
            onetSocCode={onetSocCode}
            competencies={result.competencyScores}
          />
        )}

        {/* Detailed competency breakdown */}
        <CompetencyDetailAccordion competencies={result.competencyScores} />

        <ActionButtonsBar
          templateId={result.templateId}
          resultId={result.id}
          actions={['download_report', 'retake', 'share']}
        />
      </div>
    </div>
  );
}
```

### 6.3 Scenario C: TeamFitResultView

**File:** `_components/team-fit/TeamFitResultView.tsx`

```typescript
export function TeamFitResultView({ result, template }: BaseResultViewProps) {
  const teamId = template.blueprint?.team_id;
  // TODO: Fetch team data from TeamService when available

  return (
    <div className="min-h-screen bg-muted/30 py-4 md:py-8">
      <div className="container max-w-7xl mx-auto px-4 space-y-4">
        {/* Hero with team context */}
        <TeamFitHero
          result={result}
          templateName={result.templateName}
          teamId={teamId}
          isPassed={result.passed}
        />

        {/* Team skill gap visualization */}
        <TeamGapCard
          competencies={result.competencyScores}
          teamAverages={null} // TODO: from TeamService
        />

        {/* Personality fit with team */}
        <TeamDynamicsCard
          individualProfile={useBigFiveProjection(result.competencyScores)}
          teamProfile={null} // TODO: from TeamService
        />

        <CompetencyDetailAccordion competencies={result.competencyScores} />

        <ActionButtonsBar
          templateId={result.templateId}
          resultId={result.id}
          actions={['team_dashboard', 'retake', 'share_with_team']}
        />
      </div>
    </div>
  );
}
```

---

## 7. Testing Strategy

### 7.1 Unit Tests

**Test Files:**
- `_components/overview/OverviewResultView.test.tsx`
- `_components/job-fit/JobFitResultView.test.tsx`
- `_components/team-fit/TeamFitResultView.test.tsx`
- `_components/ResultViewFactory.test.tsx`

**Test Cases:**

```typescript
// OverviewResultView.test.tsx
describe('OverviewResultView', () => {
  it('should NOT show score percentage', () => {
    render(<OverviewResultView result={mockOverviewResult} template={mockTemplate} />);
    expect(screen.queryByText(/85%/)).not.toBeInTheDocument();
  });

  it('should NOT show pass/fail badge', () => {
    render(<OverviewResultView result={mockOverviewResult} template={mockTemplate} />);
    expect(screen.queryByText(/passed/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/failed/i)).not.toBeInTheDocument();
  });

  it('should render Big Five radar chart', () => {
    render(<OverviewResultView result={mockOverviewResult} template={mockTemplate} />);
    expect(screen.getByText(/personality profile/i)).toBeInTheDocument();
  });

  it('should show profile messaging', () => {
    render(<OverviewResultView result={mockOverviewResult} template={mockTemplate} />);
    expect(screen.getByText(/competency profile/i)).toBeInTheDocument();
  });
});

// JobFitResultView.test.tsx
describe('JobFitResultView', () => {
  it('should show score percentage', () => {
    render(<JobFitResultView result={mockJobFitResult} template={mockTemplate} />);
    expect(screen.getByText(/85%/)).toBeInTheDocument();
  });

  it('should show pass/fail badge', () => {
    render(<JobFitResultView result={mockJobFitResult} template={mockTemplate} />);
    expect(screen.getByText(/passed|qualified/i)).toBeInTheDocument();
  });

  it('should render gap analysis chart', () => {
    render(<JobFitResultView result={mockJobFitResult} template={mockTemplate} />);
    expect(screen.getByText(/gap analysis/i)).toBeInTheDocument();
  });
});

// ResultViewFactory.test.tsx
describe('ResultViewFactory', () => {
  it('should return OverviewResultView for OVERVIEW goal', () => {
    const component = getResultViewComponent(AssessmentGoal.OVERVIEW);
    expect(component).toBe(OverviewResultView);
  });

  it('should return JobFitResultView for JOB_FIT goal', () => {
    const component = getResultViewComponent(AssessmentGoal.JOB_FIT);
    expect(component).toBe(JobFitResultView);
  });

  it('should default to OverviewResultView for unknown goal', () => {
    const component = getResultViewComponent('UNKNOWN' as any);
    expect(component).toBe(OverviewResultView);
  });
});
```

### 7.2 Integration Tests

**Test routing from page.tsx to correct view:**

```typescript
describe('Test Results Page Routing', () => {
  it('should render OverviewResultView for OVERVIEW template', async () => {
    mockTemplateApi({ goal: AssessmentGoal.OVERVIEW });

    const { container } = render(await TestResultsPage({ params: { resultId: '123' }}));

    // Should NOT have score circle
    expect(container.querySelector('[data-testid="score-circle"]')).toBeNull();

    // Should have Big Five radar
    expect(screen.getByText(/personality profile/i)).toBeInTheDocument();
  });

  it('should render JobFitResultView for JOB_FIT template', async () => {
    mockTemplateApi({ goal: AssessmentGoal.JOB_FIT });

    const { container } = render(await TestResultsPage({ params: { resultId: '123' }}));

    // Should have score circle
    expect(container.querySelector('[data-testid="score-circle"]')).toBeInTheDocument();

    // Should have gap analysis
    expect(screen.getByText(/gap analysis/i)).toBeInTheDocument();
  });
});
```

### 7.3 Visual Regression Tests

Use Playwright or Chromatic for visual testing:

```typescript
test.describe('Test Results Visual Regression', () => {
  test('Scenario A - Overview matches snapshot', async ({ page }) => {
    await page.goto('/test-templates/results/overview-result-123');
    await expect(page).toHaveScreenshot('overview-result.png');
  });

  test('Scenario B - Job Fit matches snapshot', async ({ page }) => {
    await page.goto('/test-templates/results/jobfit-result-456');
    await expect(page).toHaveScreenshot('jobfit-result.png');
  });
});
```

---

## 8. Accessibility Checklist

### 8.1 Keyboard Navigation
- [ ] Tab order logical (hero → charts → competencies → actions)
- [ ] Focus indicators visible on all interactive elements
- [ ] Escape key closes modals/dialogs
- [ ] Arrow keys navigate accordion items

### 8.2 Screen Reader Support
- [ ] Hero section has proper heading hierarchy (h1 → h2 → h3)
- [ ] Charts have `aria-label` describing content
- [ ] Progress bars have `role="progressbar"` and `aria-valuenow`
- [ ] Badges have `aria-label` for context (e.g., "Passed badge")

### 8.3 Color Contrast
- [ ] All text meets WCAG AA standard (4.5:1 ratio)
- [ ] Pass/fail indicators don't rely solely on color
- [ ] Charts use patterns in addition to colors
- [ ] Dark mode tested for contrast

### 8.4 Responsive Design
- [ ] Touch targets minimum 44x44px on mobile
- [ ] Text scales with user font size preferences
- [ ] No horizontal scrolling on small screens
- [ ] Charts remain readable on mobile

---

## 9. Performance Considerations

### 9.1 Code Splitting
```typescript
// Lazy load scenario-specific components
const OverviewResultView = lazy(() => import('./overview/OverviewResultView'));
const JobFitResultView = lazy(() => import('./job-fit/JobFitResultView'));
const TeamFitResultView = lazy(() => import('./team-fit/TeamFitResultView'));
```

### 9.2 Data Fetching
- Fetch template in parallel with result (Promise.all)
- Cache template data (SWR or React Query)
- Prefetch Big Five mapping data on hover

### 9.3 Chart Optimization
- Memoize `useBigFiveProjection` (already implemented)
- Lazy render charts (only when visible)
- Use `React.memo` for expensive chart components

---

## 10. Migration Guide

### 10.1 Rollout Strategy

**Option A: Instant Switch (Recommended)**
- Deploy new factory-based system
- All users immediately see scenario-appropriate views
- Monitor for errors/complaints
- Rollback plan: revert to old page.tsx

**Option B: Gradual Rollout**
- Use feature flag `NEXT_PUBLIC_SCENARIO_VIEWS=true`
- A/B test with 10% → 50% → 100% rollout
- Collect user feedback
- Full rollout after validation

**Option C: User Preference**
- Add toggle in user settings
- "Classic View" vs. "Enhanced View"
- Deprecate classic view after 3 months

### 10.2 Backward Compatibility

Ensure old URLs still work:
```typescript
// page.tsx
async function ResultsContent({ resultId }: { resultId: string }) {
  const result = await fetchTestResult(resultId);
  const template = await testTemplatesApi.getTemplateById(result.templateId);

  // Use search params for override (for testing/debugging)
  const searchParams = useSearchParams();
  const forceView = searchParams.get('view'); // ?view=overview|jobfit|teamfit

  if (forceView) {
    // Override factory logic for testing
    return <ForceViewComponent view={forceView} result={result} template={template} />;
  }

  // Normal factory delegation
  return <ResultViewWrapper result={result} template={template} />;
}
```

---

## 11. Success Metrics

### 11.1 Technical Metrics
- [ ] Page load time < 1.5s (LCP)
- [ ] Component render time < 150ms
- [ ] Zero console errors on all scenarios
- [ ] Test coverage > 70%
- [ ] Lighthouse score > 90

### 11.2 UX Metrics
- [ ] Users understand Scenario A is a profile (survey)
- [ ] Reduced support tickets about "why no score?"
- [ ] Increased engagement with Big Five visualization
- [ ] Higher satisfaction scores (NPS)
- [ ] Lower bounce rate on results page

### 11.3 Business Metrics
- [ ] Increased test completions (profile focus less intimidating)
- [ ] More profile downloads (Scenario A)
- [ ] Higher retake rate (users want to improve profile)
- [ ] Better employer adoption (clearer candidate insights)

---

## 12. Future Enhancements

### 12.1 Scenario A Enhancements
- [ ] Downloadable PDF Competency Passport
- [ ] Share profile to LinkedIn
- [ ] Historical profile comparison (track growth over time)
- [ ] Personalized development recommendations
- [ ] Integration with external personality assessments (e.g., Myers-Briggs)

### 12.2 Scenario B Enhancements
- [ ] Live O*NET data integration (fetch job requirements)
- [ ] Multiple job comparison (candidate fits for 3 roles)
- [ ] Skill gap remediation plan (courses, resources)
- [ ] Employer-specific benchmarks (not just O*NET)

### 12.3 Scenario C Enhancements
- [ ] Real-time team dashboard
- [ ] Team diversity analysis (skills + personality)
- [ ] Role recommendation within team
- [ ] Collaboration insights (who works well together)
- [ ] Team Big Five profile aggregation

---

## Appendix A: File Change Summary

**New Files:**
```
app/(workspace)/test-templates/results/[resultId]/
├── _components/
│   ├── ResultViewFactory.tsx                 [NEW]
│   ├── shared/
│   │   ├── types.ts                          [NEW]
│   │   ├── CompetencyDetailAccordion.tsx     [NEW]
│   │   └── ActionButtonsBar.tsx              [NEW]
│   ├── overview/
│   │   ├── OverviewResultView.tsx            [NEW]
│   │   ├── CompetencyPassportHero.tsx        [NEW]
│   │   ├── BigFivePersonalityCard.tsx        [NEW]
│   │   └── ProfileCompetenciesCard.tsx       [NEW]
│   ├── job-fit/
│   │   ├── JobFitResultView.tsx              [NEW]
│   │   ├── JobFitHero.tsx                    [NEW]
│   │   ├── GapAnalysisCard.tsx               [NEW]
│   │   └── BenchmarkComparisonCard.tsx       [NEW]
│   └── team-fit/
│       ├── TeamFitResultView.tsx             [NEW]
│       ├── TeamFitHero.tsx                   [NEW]
│       ├── TeamGapCard.tsx                   [NEW]
│       └── TeamDynamicsCard.tsx              [NEW]
```

**Modified Files:**
```
app/(workspace)/test-templates/results/[resultId]/
├── page.tsx                                  [MODIFIED - use factory]
```

**Test Files:**
```
src/__tests__/components/test-results/
├── OverviewResultView.test.tsx               [NEW]
├── JobFitResultView.test.tsx                 [NEW]
├── TeamFitResultView.test.tsx                [NEW]
└── ResultViewFactory.test.tsx                [NEW]
```

---

## Appendix B: Key Questions for Stakeholders

Before implementation, confirm:

1. **Big Five Data Availability:** Does backend include `onetCode` in `CompetencyScoreDto`? ✅ Verify
2. **Scenario A Messaging:** Approve language "Competency Profile" vs. "Assessment Results"? ✅ Get sign-off
3. **Team Data Integration:** Timeline for TeamService implementation? ⏳ Clarify scope
4. **Download/Export Features:** Priority for PDF generation? 🔧 Define MVP
5. **User Education:** Need tooltips/help text for new views? 📚 Content requirements

---

## Conclusion

This workflow provides a comprehensive, phased approach to redesigning the test results page with scenario-appropriate visualizations. The Strategy pattern ensures maintainability, and the phased rollout minimizes risk while delivering immediate value for Scenario A users seeking competency profiles rather than competitive scores.

**Next Steps:**
1. Stakeholder review of this document
2. Backend verification (O*NET codes in results)
3. Begin Phase 1 implementation (foundation setup)
4. Iterate based on user feedback

**Estimated Timeline:** 7 days (with 1 developer)
**Risk Level:** Medium (data dependencies, UX validation)
**Impact:** High (improved user experience, clearer assessment differentiation)
