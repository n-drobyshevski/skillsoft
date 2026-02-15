# Phase 1: Goal-Aware Template Creation - Implementation Roadmap

## Executive Summary

This roadmap provides a detailed, step-by-step implementation plan for adapting the Test Building System frontend to support goal-aware template creation. Phase 1 is the highest priority and focuses on making the `NewTestForm` dynamically adapt its configuration panels based on the selected assessment goal (OVERVIEW, JOB_FIT, TEAM_FIT).

**Target Duration**: 5-7 days
**Priority**: HIGH
**Dependencies**: Backend O*NET and Teams endpoints (can be mocked initially)

---

## 1. Prerequisites and Existing Code Analysis

### 1.1 Current Implementation State

| File | Status | Key Observations |
|------|--------|------------------|
| `NewTestForm.tsx` (920 lines) | Functional | 4-step wizard, goal selection exists but form is NOT goal-aware |
| `BlueprintWorkspaceProvider.tsx` (360 lines) | Complete | Manages blueprint state with auto-save, needs goal context |
| `domain.ts` (804 lines) | Complete | Has `AssessmentGoal` enum, `AssessmentGoalInfo`, missing goal-specific DTOs |
| `api.ts` (1340 lines) | Complete | Missing O*NET search, Teams, and Passport API methods |
| `standards-search-combobox.tsx` (817 lines) | Reference | Pattern for O*NET search with worker-based fuzzy search |
| `ui-store.ts` (66 lines) | Reference | Zustand store pattern with selector hooks |

### 1.2 Current Form Schema (Lines 96-109 in NewTestForm.tsx)

```typescript
// CURRENT: Generic schema - same for ALL goals
const formSchema = z.object({
  name: z.string(),
  description: z.string().optional(),
  goal: z.nativeEnum(AssessmentGoal),      // Goal IS selected
  competencyIds: z.array(z.string()),       // Generic
  questionsPerIndicator: z.number(),
  timeLimitMinutes: z.number(),
  passingScore: z.number(),
  shuffleQuestions: z.boolean(),
  shuffleOptions: z.boolean(),
  allowSkip: z.boolean(),
  allowBackNavigation: z.boolean(),
  showResultsImmediately: z.boolean(),
  // MISSING: Goal-specific fields
});
```

### 1.3 Required Goal-Specific Fields

| Goal | Required Fields | Optional Fields |
|------|-----------------|-----------------|
| OVERVIEW | `competencyIds`, `questionsPerIndicator` | `includeBigFive`, `preferredDifficulty` |
| JOB_FIT | `onetSocCode`, `strictnessLevel` | `candidateClerkUserId` (delta testing), `competencyIds` (override) |
| TEAM_FIT | `teamId`, `saturationThreshold` | `competencyIds` (override) |

### 1.4 Existing UI Components Available

- `Slider` - Radix-based slider (for strictness, saturation)
- `Command` + `Popover` - Combobox pattern (for O*NET search)
- `RadioGroup` - Goal selection (already implemented)
- `Switch` - Toggle controls
- `Accordion` - Collapsible sections (Big Five grouping)
- `Badge` - Status indicators
- `useWorkerSearch` - Worker-based fuzzy search hook
- `useDebounce` - Debounce hook for search inputs

---

## 2. State Machine Design

### 2.1 Goal-Aware Configuration Flow

```
                     +-------------------+
                     |   STEP 1: BASIC   |
                     |   (Name + Goal)   |
                     +--------+----------+
                              |
              +---------------+---------------+
              |               |               |
              v               v               v
    +---------+-----+ +-------+-------+ +-----+---------+
    |   OVERVIEW    | |   JOB_FIT     | |   TEAM_FIT    |
    |   Config      | |   Config      | |   Config      |
    +-------+-------+ +-------+-------+ +-------+-------+
            |               |               |
            +---------------+---------------+
                            |
                            v
                    +-------+-------+
                    | STEP 2: GOAL- |
                    | SPECIFIC CONF |
                    +-------+-------+
                            |
                            v
                    +-------+-------+
                    |  STEP 3:      |
                    |  SETTINGS     |
                    +-------+-------+
                            |
                            v
                    +-------+-------+
                    |  STEP 4:      |
                    |  REVIEW       |
                    +---------------+
```

### 2.2 Form State Machine (XState-like)

```typescript
type FormState =
  | 'step1_basic'
  | 'step2_overview_config'
  | 'step2_jobfit_config'
  | 'step2_teamfit_config'
  | 'step3_settings'
  | 'step4_review';

type FormEvent =
  | { type: 'NEXT' }
  | { type: 'BACK' }
  | { type: 'SET_GOAL'; goal: AssessmentGoal }
  | { type: 'SUBMIT' };

// Transition logic
function getNextStep(currentStep: number, goal: AssessmentGoal): FormState {
  if (currentStep === 1) {
    switch (goal) {
      case 'OVERVIEW': return 'step2_overview_config';
      case 'JOB_FIT': return 'step2_jobfit_config';
      case 'TEAM_FIT': return 'step2_teamfit_config';
    }
  }
  // ... rest of transitions
}
```

### 2.3 Blueprint Context State

```typescript
interface GoalAwareBlueprintState {
  goal: AssessmentGoal;

  // Common fields
  name: string;
  description?: string;
  questionsPerIndicator: number;
  timeLimitMinutes: number;
  passingScore: number;

  // OVERVIEW-specific
  overview?: {
    competencyIds: string[];
    includeBigFive: boolean;
    preferredDifficulty: 'BASIC' | 'INTERMEDIATE' | 'ADVANCED';
  };

  // JOB_FIT-specific
  jobFit?: {
    onetSocCode: string;
    onetProfile?: ONetProfile;  // Cached from API
    strictnessLevel: number;    // 0-100
    candidateClerkUserId?: string;
    candidatePassport?: CompetencyPassport;
    enableDeltaTesting: boolean;
    competencyOverride?: string[];
  };

  // TEAM_FIT-specific
  teamFit?: {
    teamId: string;
    teamProfile?: TeamProfile;  // Cached from API
    saturationThreshold: number;  // 0.0-1.0
    competencyOverride?: string[];
  };
}
```

---

## 3. Detailed Task Breakdown

### Task 1: Create Type Definitions (0.5 day)

**File**: `D:\projects\diplom\skillsoft\frontend-app\src\types\domain.ts`

**Changes**:

```typescript
// Add after line 286 (after existing StandardCodesDto)

// ============================================
// O*NET TYPES (Goal-Aware Blueprint)
// ============================================

export interface ONetOccupation {
  socCode: string;       // e.g., "15-1252.00"
  title: string;         // e.g., "Software Developers"
  description: string;
}

export interface ONetBenchmark {
  competencyCode: string;
  competencyName: string;
  requiredLevel: number;  // 1-7 scale
  importance: number;     // 1-5 scale
}

export interface ONetProfile {
  socCode: string;
  occupationTitle: string;
  benchmarks: ONetBenchmark[];
  knowledgeAreas: string[];
  skills: string[];
}

// ============================================
// TEAM TYPES (Goal-Aware Blueprint)
// ============================================

export interface Team {
  id: string;
  name: string;
  memberCount: number;
  createdAt: string;
}

export interface TeamMemberSkill {
  memberId: string;
  memberName: string;
  competencyId: string;
  score: number;
}

export interface TeamProfile {
  teamId: string;
  teamName: string;
  saturation: Record<string, number>;  // competencyId -> 0.0-1.0
  undersaturatedCompetencies: string[];
  memberSkills: TeamMemberSkill[];
}

// ============================================
// PASSPORT TYPES (Delta Testing)
// ============================================

export interface CompetencyPassport {
  id: string;
  candidateId: string;
  clerkUserId: string;
  lastUpdated: string;
  scores: Record<string, number>;  // competencyId -> score
  bigFiveProfile?: BigFiveProfile;
  isValid: boolean;
  expiresAt?: string;
}

export interface BigFiveProfile {
  openness: number;
  conscientiousness: number;
  extraversion: number;
  agreeableness: number;
  emotionalStability: number;
}

// ============================================
// GOAL-SPECIFIC BLUEPRINT TYPES
// ============================================

export interface OverviewBlueprintConfig {
  competencyIds: string[];
  questionsPerIndicator: number;
  includeBigFive: boolean;
  preferredDifficulty?: 'BASIC' | 'INTERMEDIATE' | 'ADVANCED';
}

export interface JobFitBlueprintConfig {
  onetSocCode: string;
  strictnessLevel: number;  // 0-100
  candidateClerkUserId?: string;
  enableDeltaTesting?: boolean;
  competencyOverride?: string[];
}

export interface TeamFitBlueprintConfig {
  teamId: string;
  saturationThreshold: number;  // 0.0-1.0
  competencyOverride?: string[];
}

export type GoalSpecificConfig =
  | { goal: 'OVERVIEW'; config: OverviewBlueprintConfig }
  | { goal: 'JOB_FIT'; config: JobFitBlueprintConfig }
  | { goal: 'TEAM_FIT'; config: TeamFitBlueprintConfig };
```

**Validation Checkpoint**:
- [ ] TypeScript compiles without errors
- [ ] Types are exported correctly
- [ ] Run `npm run type-check`

---

### Task 2: Add API Methods (0.5 day)

**File**: `D:\projects\diplom\skillsoft\frontend-app\src\services\api.ts`

**Add after line 1146** (after psychometricsApi):

```typescript
// ============================================
// O*NET API (Goal-Aware Blueprint)
// ============================================

const ONET_BASE = '/onet';

export const onetApi = {
  /**
   * Search O*NET occupations by title or keyword
   */
  searchOccupations: async (query: string, limit = 10): Promise<ONetOccupation[]> => {
    const authHeaders = await getAuthHeaders();
    return fetchApi(`${ONET_BASE}/search?q=${encodeURIComponent(query)}&limit=${limit}`, {
      tags: ['onet-search'],
      revalidate: 300,
      authHeaders,
    });
  },

  /**
   * Get detailed O*NET profile for an occupation
   */
  getProfile: async (socCode: string): Promise<ONetProfile> => {
    const authHeaders = await getAuthHeaders();
    return fetchApi(`${ONET_BASE}/profiles/${encodeURIComponent(socCode)}`, {
      tags: [`onet-profile-${socCode}`],
      revalidate: 3600,  // Cache for 1 hour
      authHeaders,
    });
  },

  /**
   * Get popular/common occupations for quick selection
   */
  getPopularOccupations: async (): Promise<ONetOccupation[]> => {
    const authHeaders = await getAuthHeaders();
    return fetchApi(`${ONET_BASE}/popular`, {
      tags: ['onet-popular'],
      revalidate: 3600,
      authHeaders,
    });
  },
};

// ============================================
// TEAMS API (Goal-Aware Blueprint)
// ============================================

const TEAMS_BASE = '/teams';

export const teamsApi = {
  /**
   * Get all teams for the current organization
   */
  getAllTeams: async (): Promise<Team[]> => {
    const authHeaders = await getAuthHeaders();
    return fetchApi(TEAMS_BASE, {
      tags: ['teams'],
      revalidate: 60,
      authHeaders,
    });
  },

  /**
   * Get team profile with saturation data
   */
  getTeamProfile: async (teamId: string): Promise<TeamProfile> => {
    const authHeaders = await getAuthHeaders();
    return fetchApi(`${TEAMS_BASE}/${teamId}/profile`, {
      tags: [`team-profile-${teamId}`],
      revalidate: 60,
      authHeaders,
    });
  },
};

// ============================================
// PASSPORT API (Delta Testing)
// ============================================

const PASSPORT_BASE = '/passports';

export const passportApi = {
  /**
   * Get passport for a specific user
   */
  getPassport: async (clerkUserId: string): Promise<CompetencyPassport | null> => {
    const authHeaders = await getAuthHeaders();
    try {
      return await fetchApi(`${PASSPORT_BASE}/user/${clerkUserId}`, {
        tags: [`passport-${clerkUserId}`],
        cache: 'no-store',
        authHeaders,
        silentStatusCodes: [404],
      });
    } catch (error) {
      if (error instanceof Error && 'status' in error && (error as ApiError).status === 404) {
        return null;
      }
      throw error;
    }
  },

  /**
   * Check if user has a valid passport
   */
  hasValidPassport: async (clerkUserId: string): Promise<boolean> => {
    const authHeaders = await getAuthHeaders();
    try {
      const result = await fetchApi<{ valid: boolean }>(
        `${PASSPORT_BASE}/user/${clerkUserId}/valid`,
        {
          cache: 'no-store',
          authHeaders,
          silentStatusCodes: [404],
        }
      );
      return result?.valid ?? false;
    } catch {
      return false;
    }
  },
};
```

**Also update imports at top of file**:

```typescript
import {
  // ... existing imports
  ONetOccupation,
  ONetProfile,
  Team,
  TeamProfile,
  CompetencyPassport,
} from '@/types/domain';
```

**Validation Checkpoint**:
- [ ] Types import correctly
- [ ] No circular dependencies
- [ ] API methods follow existing patterns

---

### Task 3: Create GoalSelector Component (1 day)

**File**: `D:\projects\diplom\skillsoft\frontend-app\src\components\blueprint-config\GoalSelector.tsx`

**Create new file**:

```typescript
'use client';

import React from 'react';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import { AssessmentGoal, AssessmentGoalInfo } from '@/types/domain';
import {
  Crosshair,
  Briefcase,
  Users,
  CheckCircle2,
  Clock,
  Target,
  TrendingUp,
} from 'lucide-react';

// ============================================================================
// Types
// ============================================================================

interface GoalSelectorProps {
  value: AssessmentGoal;
  onChange: (goal: AssessmentGoal) => void;
  disabled?: boolean;
  className?: string;
}

interface GoalOption {
  value: AssessmentGoal;
  icon: React.ComponentType<{ className?: string }>;
  features: string[];
  estimatedTime: string;
  className: string;
  selectedClassName: string;
  iconColor: string;
}

// ============================================================================
// Configuration
// ============================================================================

const GOAL_OPTIONS: GoalOption[] = [
  {
    value: AssessmentGoal.OVERVIEW,
    icon: Crosshair,
    features: [
      'Competency Passport generation',
      'Big Five personality profile',
      'Full skill mapping',
    ],
    estimatedTime: '20-30 min',
    className: 'border-border/60 bg-card hover:border-primary/50 hover:bg-muted/30',
    selectedClassName: 'border-primary bg-primary/5 ring-1 ring-primary shadow-sm',
    iconColor: 'text-primary',
  },
  {
    value: AssessmentGoal.JOB_FIT,
    icon: Briefcase,
    features: [
      'O*NET benchmark comparison',
      'Gap analysis report',
      'Role-specific scoring',
    ],
    estimatedTime: '15-25 min',
    className: 'border-border/60 bg-card hover:border-blue-500/50 hover:bg-blue-500/5',
    selectedClassName: 'border-blue-500 bg-blue-500/10 ring-1 ring-blue-500 shadow-sm',
    iconColor: 'text-blue-500',
  },
  {
    value: AssessmentGoal.TEAM_FIT,
    icon: Users,
    features: [
      'Team saturation analysis',
      'Skill gap identification',
      'Compatibility scoring',
    ],
    estimatedTime: '15-20 min',
    className: 'border-border/60 bg-card hover:border-purple-500/50 hover:bg-purple-500/5',
    selectedClassName: 'border-purple-500 bg-purple-500/10 ring-1 ring-purple-500 shadow-sm',
    iconColor: 'text-purple-500',
  },
];

// ============================================================================
// Component
// ============================================================================

export function GoalSelector({
  value,
  onChange,
  disabled = false,
  className,
}: GoalSelectorProps) {
  return (
    <RadioGroup
      value={value}
      onValueChange={onChange}
      disabled={disabled}
      className={cn('grid gap-4', className)}
    >
      {/* Mobile: Stack vertically, Desktop: 3 columns */}
      <div className="grid gap-3 sm:grid-cols-3">
        {GOAL_OPTIONS.map((option) => {
          const Icon = option.icon;
          const info = AssessmentGoalInfo[option.value];
          const isSelected = value === option.value;

          return (
            <div key={option.value} className="relative group">
              <RadioGroupItem
                value={option.value}
                id={`goal-${option.value}`}
                className="sr-only"
              />
              <Label
                htmlFor={`goal-${option.value}`}
                className={cn(
                  // Base styles
                  'flex flex-col rounded-2xl border p-4 cursor-pointer transition-all duration-200',
                  'w-full h-full relative overflow-hidden',
                  // Touch feedback
                  'active:scale-[0.98]',
                  // State styles
                  isSelected ? option.selectedClassName : option.className,
                  // Disabled state
                  disabled && 'opacity-50 cursor-not-allowed'
                )}
              >
                {/* Selected indicator */}
                {isSelected && (
                  <div className="absolute top-3 right-3 animate-in zoom-in duration-200">
                    <CheckCircle2 className="h-5 w-5 text-current fill-background" />
                  </div>
                )}

                {/* Icon and Title */}
                <div className="flex items-center gap-3 mb-3">
                  <div
                    className={cn(
                      'p-2.5 rounded-xl transition-colors',
                      isSelected ? 'bg-background shadow-sm' : 'bg-muted group-hover:bg-background'
                    )}
                  >
                    <Icon className={cn('h-6 w-6', option.iconColor)} />
                  </div>
                  <div>
                    <p className={cn('font-bold text-sm', isSelected && option.iconColor)}>
                      {info.displayName}
                    </p>
                    <p className="text-xs text-muted-foreground mt-0.5">
                      {info.description}
                    </p>
                  </div>
                </div>

                {/* Features list - hidden on mobile, shown on tablet+ */}
                <div className="hidden sm:block space-y-1.5 mt-2 pt-3 border-t border-dashed">
                  {option.features.map((feature, idx) => (
                    <div key={idx} className="flex items-center gap-2 text-xs text-muted-foreground">
                      <Target className="h-3 w-3 shrink-0" />
                      <span>{feature}</span>
                    </div>
                  ))}
                </div>

                {/* Estimated time badge */}
                <div className="flex items-center gap-1.5 mt-3 pt-3 border-t">
                  <Clock className="h-3.5 w-3.5 text-muted-foreground" />
                  <span className="text-xs text-muted-foreground">
                    ~{option.estimatedTime}
                  </span>
                </div>
              </Label>
            </div>
          );
        })}
      </div>

      {/* Goal description footer for mobile */}
      <div className="sm:hidden p-3 rounded-xl bg-muted/50 border">
        <div className="flex items-start gap-2">
          <TrendingUp className="h-4 w-4 text-primary mt-0.5 shrink-0" />
          <div>
            <p className="text-sm font-medium">
              {AssessmentGoalInfo[value].displayName}
            </p>
            <p className="text-xs text-muted-foreground mt-1">
              {GOAL_OPTIONS.find(o => o.value === value)?.features.join(' | ')}
            </p>
          </div>
        </div>
      </div>
    </RadioGroup>
  );
}

export default GoalSelector;
```

**Mobile-First Design Notes**:
- Stack cards vertically on mobile (`grid gap-3 sm:grid-cols-3`)
- Hide feature list on mobile, show summary footer instead
- Touch targets are 44px+ (p-4 padding)
- Active state feedback with `active:scale-[0.98]`
- Reduced motion support via CSS animations

**Validation Checkpoint**:
- [ ] Component renders correctly on mobile (320px width)
- [ ] Component renders correctly on tablet (768px width)
- [ ] Component renders correctly on desktop (1024px+ width)
- [ ] Radio selection works with keyboard navigation
- [ ] Screen reader announces goal options correctly

---

### Task 4: Create ONetSearchCombobox Component (2 days)

**File**: `D:\projects\diplom\skillsoft\frontend-app\src\components\blueprint-config\ONetSearchCombobox.tsx`

**Create new file** (following pattern from `standards-search-combobox.tsx`):

```typescript
'use client';

import React, { useState, useCallback, useMemo, useEffect } from 'react';
import { Check, X, Loader2, Search, Briefcase, ChevronDown, Star, Clock, Sparkles } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from '@/components/ui/command';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { useDebounce } from '@/hooks/use-debounce';
import { onetApi } from '@/services/api';
import type { ONetOccupation, ONetProfile } from '@/types/domain';

// ============================================================================
// Types
// ============================================================================

interface ONetSearchComboboxProps {
  /** Currently selected O*NET SOC code */
  value?: string;
  /** Callback when selection changes */
  onChange: (socCode: string | undefined, profile?: ONetProfile) => void;
  /** Whether the field is disabled */
  disabled?: boolean;
  /** Placeholder text */
  placeholder?: string;
  /** CSS class name */
  className?: string;
}

// ============================================================================
// Popular Occupations (Fallback when API not available)
// ============================================================================

const POPULAR_OCCUPATIONS: ONetOccupation[] = [
  { socCode: '15-1252.00', title: 'Software Developers', description: 'Research, design, and develop computer software systems' },
  { socCode: '13-1111.00', title: 'Management Analysts', description: 'Conduct organizational studies and evaluations' },
  { socCode: '11-1021.00', title: 'General and Operations Managers', description: 'Plan, direct, or coordinate operations of organizations' },
  { socCode: '15-1211.00', title: 'Computer Systems Analysts', description: 'Analyze science, engineering, business problems' },
  { socCode: '13-2011.00', title: 'Accountants and Auditors', description: 'Examine, analyze, and interpret accounting records' },
  { socCode: '17-2199.00', title: 'Engineers, All Other', description: 'Design or develop engineering solutions' },
  { socCode: '11-3031.00', title: 'Financial Managers', description: 'Plan, direct, or coordinate financial activities' },
  { socCode: '13-1161.00', title: 'Market Research Analysts', description: 'Research market conditions to examine sales potential' },
];

// ============================================================================
// Component
// ============================================================================

export function ONetSearchCombobox({
  value,
  onChange,
  disabled = false,
  placeholder = 'Search job titles...',
  className,
}: ONetSearchComboboxProps) {
  const [open, setOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [isSearching, setIsSearching] = useState(false);
  const [searchResults, setSearchResults] = useState<ONetOccupation[]>([]);
  const [selectedOccupation, setSelectedOccupation] = useState<ONetOccupation | null>(null);
  const [isLoadingProfile, setIsLoadingProfile] = useState(false);
  const [recentSelections, setRecentSelections] = useState<ONetOccupation[]>([]);

  const debouncedQuery = useDebounce(searchQuery, 300);

  // Load popular occupations on mount
  const [popularOccupations, setPopularOccupations] = useState<ONetOccupation[]>(POPULAR_OCCUPATIONS);

  useEffect(() => {
    async function loadPopular() {
      try {
        const popular = await onetApi.getPopularOccupations();
        if (popular && popular.length > 0) {
          setPopularOccupations(popular);
        }
      } catch {
        // Use fallback
      }
    }
    loadPopular();
  }, []);

  // Search when query changes
  useEffect(() => {
    if (!debouncedQuery.trim()) {
      setSearchResults([]);
      return;
    }

    async function search() {
      setIsSearching(true);
      try {
        const results = await onetApi.searchOccupations(debouncedQuery);
        setSearchResults(results || []);
      } catch {
        setSearchResults([]);
      } finally {
        setIsSearching(false);
      }
    }

    search();
  }, [debouncedQuery]);

  // Resolve selected occupation from value
  useEffect(() => {
    if (!value) {
      setSelectedOccupation(null);
      return;
    }

    // Check if we already have this occupation
    const found = [...searchResults, ...popularOccupations, ...recentSelections].find(
      (o) => o.socCode === value
    );
    if (found) {
      setSelectedOccupation(found);
    }
  }, [value, searchResults, popularOccupations, recentSelections]);

  // Handle selection
  const handleSelect = useCallback(
    async (occupation: ONetOccupation) => {
      setSelectedOccupation(occupation);
      setOpen(false);
      setSearchQuery('');

      // Add to recent selections
      setRecentSelections((prev) => {
        const filtered = prev.filter((o) => o.socCode !== occupation.socCode);
        return [occupation, ...filtered].slice(0, 5);
      });

      // Load profile
      setIsLoadingProfile(true);
      try {
        const profile = await onetApi.getProfile(occupation.socCode);
        onChange(occupation.socCode, profile);
      } catch {
        onChange(occupation.socCode, undefined);
      } finally {
        setIsLoadingProfile(false);
      }
    },
    [onChange]
  );

  // Handle clear
  const handleClear = useCallback(() => {
    setSelectedOccupation(null);
    onChange(undefined, undefined);
  }, [onChange]);

  // Determine what to show
  const hasSearchQuery = searchQuery.trim().length > 0;
  const showResults = hasSearchQuery && searchResults.length > 0;
  const showPopular = !hasSearchQuery;

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="outline"
          role="combobox"
          aria-expanded={open}
          disabled={disabled}
          className={cn(
            'w-full justify-between h-12 font-normal text-left',
            !selectedOccupation && 'text-muted-foreground',
            selectedOccupation && 'border-blue-200 bg-blue-50/50 dark:border-blue-800 dark:bg-blue-950/30',
            className
          )}
        >
          <div className="flex items-center gap-3 min-w-0 flex-1">
            <div
              className={cn(
                'p-2 rounded-lg shrink-0',
                selectedOccupation
                  ? 'bg-blue-100 dark:bg-blue-900'
                  : 'bg-muted'
              )}
            >
              <Briefcase
                className={cn(
                  'h-4 w-4',
                  selectedOccupation ? 'text-blue-600 dark:text-blue-400' : 'text-muted-foreground'
                )}
              />
            </div>
            <div className="min-w-0 flex-1">
              {selectedOccupation ? (
                <>
                  <p className="text-sm font-medium truncate text-foreground">
                    {selectedOccupation.title}
                  </p>
                  <p className="text-xs text-muted-foreground font-mono">
                    {selectedOccupation.socCode}
                  </p>
                </>
              ) : (
                <span className="text-sm">{placeholder}</span>
              )}
            </div>
          </div>

          <div className="flex items-center gap-1 shrink-0">
            {isLoadingProfile && (
              <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
            )}
            {selectedOccupation && !isLoadingProfile && (
              <span
                role="button"
                tabIndex={0}
                onClick={(e) => {
                  e.stopPropagation();
                  e.preventDefault();
                  handleClear();
                }}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.stopPropagation();
                    e.preventDefault();
                    handleClear();
                  }
                }}
                className="rounded-full p-1 transition-colors hover:bg-muted cursor-pointer"
              >
                <X className="h-3.5 w-3.5" />
              </span>
            )}
            <ChevronDown className="h-4 w-4 text-muted-foreground" />
          </div>
        </Button>
      </PopoverTrigger>

      <PopoverContent
        className="w-[var(--radix-popover-trigger-width)] p-0"
        align="start"
        sideOffset={4}
      >
        <Command shouldFilter={false} className="rounded-lg">
          {/* Header */}
          <div className="flex items-center gap-2 border-b px-3 py-2.5 bg-blue-50/50 dark:bg-blue-950/30">
            <Briefcase className="h-4 w-4 text-blue-600 dark:text-blue-400" />
            <span className="text-sm font-semibold">O*NET Occupations</span>
          </div>

          {/* Search input */}
          <div className="flex items-center gap-2 border-b px-3 py-1">
            <Search className="h-4 w-4 text-muted-foreground shrink-0" />
            <CommandInput
              placeholder="Search by job title..."
              value={searchQuery}
              onValueChange={setSearchQuery}
              className="h-10 border-0 bg-transparent px-0 text-sm focus-visible:ring-0"
            />
            {isSearching && <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />}
          </div>

          {/* Results */}
          <CommandList className="max-h-[300px] overflow-y-auto">
            {isSearching ? (
              <div className="flex flex-col items-center justify-center py-8 text-muted-foreground">
                <Loader2 className="h-6 w-6 animate-spin mb-2 text-primary/60" />
                <span className="text-sm">Searching...</span>
              </div>
            ) : hasSearchQuery && searchResults.length === 0 ? (
              <CommandEmpty className="py-6 text-center">
                <Search className="h-6 w-6 mx-auto mb-2 text-muted-foreground/40" />
                <p className="text-sm text-muted-foreground">
                  No occupations found for &quot;{searchQuery}&quot;
                </p>
              </CommandEmpty>
            ) : showResults ? (
              <CommandGroup heading="Search Results" className="px-2 py-1">
                {searchResults.map((occupation) => (
                  <OccupationItem
                    key={occupation.socCode}
                    occupation={occupation}
                    isSelected={value === occupation.socCode}
                    onSelect={handleSelect}
                  />
                ))}
              </CommandGroup>
            ) : showPopular ? (
              <>
                {recentSelections.length > 0 && (
                  <CommandGroup
                    heading={
                      <span className="flex items-center gap-1.5">
                        <Clock className="h-3 w-3" />
                        Recent
                      </span>
                    }
                    className="px-2 py-1"
                  >
                    {recentSelections.map((occupation) => (
                      <OccupationItem
                        key={occupation.socCode}
                        occupation={occupation}
                        isSelected={value === occupation.socCode}
                        onSelect={handleSelect}
                      />
                    ))}
                  </CommandGroup>
                )}
                <CommandGroup
                  heading={
                    <span className="flex items-center gap-1.5">
                      <Star className="h-3 w-3" />
                      Popular Occupations
                    </span>
                  }
                  className="px-2 py-1"
                >
                  {popularOccupations.map((occupation) => (
                    <OccupationItem
                      key={occupation.socCode}
                      occupation={occupation}
                      isSelected={value === occupation.socCode}
                      onSelect={handleSelect}
                    />
                  ))}
                </CommandGroup>
              </>
            ) : null}
          </CommandList>
        </Command>
      </PopoverContent>
    </Popover>
  );
}

// ============================================================================
// Occupation Item Subcomponent
// ============================================================================

interface OccupationItemProps {
  occupation: ONetOccupation;
  isSelected: boolean;
  onSelect: (occupation: ONetOccupation) => void;
}

function OccupationItem({ occupation, isSelected, onSelect }: OccupationItemProps) {
  return (
    <CommandItem
      value={occupation.socCode}
      onSelect={() => onSelect(occupation)}
      className={cn(
        'flex items-start gap-3 py-2.5 px-2 rounded-md cursor-pointer mb-0.5 transition-all',
        isSelected
          ? 'bg-blue-100 dark:bg-blue-950/50 border border-blue-200 dark:border-blue-800'
          : 'hover:bg-muted/50 border border-transparent'
      )}
    >
      {/* Checkbox */}
      <div
        className={cn(
          'flex h-4 w-4 items-center justify-center rounded border shrink-0 mt-0.5 transition-colors',
          isSelected ? 'border-blue-500 bg-blue-500 text-white' : 'border-muted-foreground/30'
        )}
      >
        {isSelected && <Check className="h-3 w-3" />}
      </div>

      {/* Content */}
      <div className="flex flex-col min-w-0 flex-1">
        <span className="text-sm font-medium truncate">{occupation.title}</span>
        <div className="flex items-center gap-2 mt-0.5">
          <Badge variant="secondary" className="h-4 px-1.5 text-[9px] font-mono">
            {occupation.socCode}
          </Badge>
        </div>
        {occupation.description && (
          <p className="text-xs text-muted-foreground mt-1 line-clamp-2">
            {occupation.description}
          </p>
        )}
      </div>
    </CommandItem>
  );
}

export default ONetSearchCombobox;
```

**Mobile-First Design Notes**:
- Popover width matches trigger width (`w-[var(--radix-popover-trigger-width)]`)
- Touch-friendly item heights (py-2.5)
- Description truncated to 2 lines on mobile
- Clear button has 44px+ touch target

---

### Task 5: Create OverviewConfigPanel Component (1 day)

**File**: `D:\projects\diplom\skillsoft\frontend-app\src\components\blueprint-config\OverviewConfigPanel.tsx`

```typescript
'use client';

import React, { useMemo } from 'react';
import { useFormContext } from 'react-hook-form';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Switch } from '@/components/ui/switch';
import { Badge } from '@/components/ui/badge';
import { Slider } from '@/components/ui/slider';
import {
  FormField,
  FormItem,
  FormLabel,
  FormControl,
  FormDescription,
} from '@/components/ui/form';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { cn } from '@/lib/utils';
import {
  Brain,
  Sparkles,
  BarChart3,
  Clock,
  HelpCircle,
  Target,
  Lightbulb,
  ListChecks,
  Heart,
  Shield,
  Zap,
} from 'lucide-react';

// ============================================================================
// Types
// ============================================================================

interface OverviewConfigPanelProps {
  competencies: Array<{ id: string; name: string; category: string }>;
  selectedCompetencyIds: string[];
  onCompetencyChange: (ids: string[]) => void;
  className?: string;
}

// ============================================================================
// Big Five Info Cards
// ============================================================================

const BIG_FIVE_TRAITS = [
  { key: 'OPENNESS', name: 'Openness', icon: Lightbulb, color: 'purple' },
  { key: 'CONSCIENTIOUSNESS', name: 'Conscientiousness', icon: ListChecks, color: 'blue' },
  { key: 'EXTRAVERSION', name: 'Extraversion', icon: Sparkles, color: 'orange' },
  { key: 'AGREEABLENESS', name: 'Agreeableness', icon: Heart, color: 'pink' },
  { key: 'EMOTIONAL_STABILITY', name: 'Emotional Stability', icon: Shield, color: 'green' },
];

// ============================================================================
// Component
// ============================================================================

export function OverviewConfigPanel({
  competencies,
  selectedCompetencyIds,
  onCompetencyChange,
  className,
}: OverviewConfigPanelProps) {
  const form = useFormContext();

  // Calculate estimated questions and time
  const estimatedQuestions = useMemo(() => {
    const questionsPerIndicator = form.watch('questionsPerIndicator') || 2;
    // Assume ~3 indicators per competency on average
    const avgIndicators = 3;
    return selectedCompetencyIds.length * avgIndicators * questionsPerIndicator;
  }, [selectedCompetencyIds.length, form]);

  const estimatedTime = useMemo(() => {
    // ~30 seconds per question on average
    return Math.ceil(estimatedQuestions * 0.5);
  }, [estimatedQuestions]);

  const includeBigFive = form.watch('includeBigFive');

  return (
    <div className={cn('space-y-6', className)}>
      {/* Big Five Toggle Card */}
      <Card className="border-dashed">
        <CardHeader className="pb-3">
          <div className="flex items-start justify-between">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-lg bg-gradient-to-br from-purple-100 to-blue-100 dark:from-purple-900/50 dark:to-blue-900/50">
                <Brain className="h-5 w-5 text-purple-600 dark:text-purple-400" />
              </div>
              <div>
                <CardTitle className="text-base">Big Five Personality</CardTitle>
                <CardDescription className="text-xs mt-0.5">
                  Include OCEAN personality profiling in results
                </CardDescription>
              </div>
            </div>
            <FormField
              control={form.control}
              name="includeBigFive"
              render={({ field }) => (
                <FormControl>
                  <Switch
                    checked={field.value}
                    onCheckedChange={field.onChange}
                    className="data-[state=checked]:bg-purple-500"
                  />
                </FormControl>
              )}
            />
          </div>
        </CardHeader>

        {includeBigFive && (
          <CardContent className="pt-0">
            <div className="grid grid-cols-5 gap-2">
              {BIG_FIVE_TRAITS.map((trait) => {
                const Icon = trait.icon;
                return (
                  <div
                    key={trait.key}
                    className={cn(
                      'flex flex-col items-center p-2 rounded-lg text-center',
                      `bg-${trait.color}-50 dark:bg-${trait.color}-950/30`
                    )}
                  >
                    <Icon className={cn('h-4 w-4 mb-1', `text-${trait.color}-500`)} />
                    <span className="text-[10px] font-medium text-muted-foreground truncate w-full">
                      {trait.name.slice(0, 4)}
                    </span>
                  </div>
                );
              })}
            </div>
          </CardContent>
        )}
      </Card>

      {/* Difficulty Preference */}
      <FormField
        control={form.control}
        name="preferredDifficulty"
        render={({ field }) => (
          <FormItem>
            <FormLabel className="flex items-center gap-2">
              <BarChart3 className="h-4 w-4 text-muted-foreground" />
              Preferred Difficulty
            </FormLabel>
            <Select value={field.value} onValueChange={field.onChange}>
              <FormControl>
                <SelectTrigger className="h-11">
                  <SelectValue placeholder="Select difficulty level" />
                </SelectTrigger>
              </FormControl>
              <SelectContent>
                <SelectItem value="BASIC">
                  <div className="flex items-center gap-2">
                    <Badge variant="secondary" className="bg-green-100 text-green-700">Basic</Badge>
                    <span className="text-xs text-muted-foreground">Entry-level questions</span>
                  </div>
                </SelectItem>
                <SelectItem value="INTERMEDIATE">
                  <div className="flex items-center gap-2">
                    <Badge variant="secondary" className="bg-blue-100 text-blue-700">Intermediate</Badge>
                    <span className="text-xs text-muted-foreground">Standard complexity</span>
                  </div>
                </SelectItem>
                <SelectItem value="ADVANCED">
                  <div className="flex items-center gap-2">
                    <Badge variant="secondary" className="bg-orange-100 text-orange-700">Advanced</Badge>
                    <span className="text-xs text-muted-foreground">Senior-level scenarios</span>
                  </div>
                </SelectItem>
              </SelectContent>
            </Select>
            <FormDescription>
              Controls the complexity of situational judgment questions
            </FormDescription>
          </FormItem>
        )}
      />

      {/* Estimation Card */}
      <Card className="bg-muted/30">
        <CardContent className="pt-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-sm">
              <Target className="h-4 w-4 text-primary" />
              <span>{selectedCompetencyIds.length} competencies selected</span>
            </div>
            <Badge variant="outline" className="font-mono">
              ~{estimatedQuestions} questions
            </Badge>
          </div>
          <div className="flex items-center justify-between mt-2">
            <div className="flex items-center gap-2 text-sm">
              <Clock className="h-4 w-4 text-muted-foreground" />
              <span>Estimated duration</span>
            </div>
            <Badge variant="secondary">~{estimatedTime} min</Badge>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

export default OverviewConfigPanel;
```

---

### Task 6: Create JobFitConfigPanel Component (1.5 days)

**File**: `D:\projects\diplom\skillsoft\frontend-app\src\components\blueprint-config\JobFitConfigPanel.tsx`

```typescript
'use client';

import React, { useState, useCallback, useEffect } from 'react';
import { useFormContext } from 'react-hook-form';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Switch } from '@/components/ui/switch';
import { Badge } from '@/components/ui/badge';
import { Slider } from '@/components/ui/slider';
import { Skeleton } from '@/components/ui/skeleton';
import {
  FormField,
  FormItem,
  FormLabel,
  FormControl,
  FormDescription,
  FormMessage,
} from '@/components/ui/form';
import { cn } from '@/lib/utils';
import {
  Briefcase,
  Target,
  AlertTriangle,
  Check,
  TrendingUp,
  Zap,
  Shield,
  Clock,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';
import { ONetSearchCombobox } from './ONetSearchCombobox';
import { onetApi } from '@/services/api';
import type { ONetProfile, ONetBenchmark } from '@/types/domain';
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible';

// ============================================================================
// Types
// ============================================================================

interface JobFitConfigPanelProps {
  className?: string;
}

// ============================================================================
// Strictness Level Labels
// ============================================================================

function getStrictnessLabel(value: number): { label: string; description: string; color: string } {
  if (value <= 25) {
    return {
      label: 'Lenient',
      description: 'Accept candidates with potential, even if below benchmarks',
      color: 'text-green-600',
    };
  } else if (value <= 50) {
    return {
      label: 'Moderate',
      description: 'Balance between flexibility and benchmark adherence',
      color: 'text-blue-600',
    };
  } else if (value <= 75) {
    return {
      label: 'Strict',
      description: 'Require close match to O*NET benchmarks',
      color: 'text-orange-600',
    };
  } else {
    return {
      label: 'Very Strict',
      description: 'Require exact or above-benchmark performance',
      color: 'text-red-600',
    };
  }
}

// ============================================================================
// Component
// ============================================================================

export function JobFitConfigPanel({ className }: JobFitConfigPanelProps) {
  const form = useFormContext();

  const [onetProfile, setOnetProfile] = useState<ONetProfile | null>(null);
  const [isLoadingProfile, setIsLoadingProfile] = useState(false);
  const [showBenchmarks, setShowBenchmarks] = useState(false);

  const onetSocCode = form.watch('onetSocCode');
  const strictnessLevel = form.watch('strictnessLevel') ?? 50;
  const strictnessInfo = getStrictnessLabel(strictnessLevel);

  // Load profile when SOC code changes
  useEffect(() => {
    if (!onetSocCode) {
      setOnetProfile(null);
      return;
    }

    async function loadProfile() {
      setIsLoadingProfile(true);
      try {
        const profile = await onetApi.getProfile(onetSocCode);
        setOnetProfile(profile);
      } catch {
        setOnetProfile(null);
      } finally {
        setIsLoadingProfile(false);
      }
    }

    loadProfile();
  }, [onetSocCode]);

  // Handle O*NET selection
  const handleOnetChange = useCallback(
    (socCode: string | undefined, profile?: ONetProfile) => {
      form.setValue('onetSocCode', socCode || '');
      if (profile) {
        setOnetProfile(profile);
      }
    },
    [form]
  );

  return (
    <div className={cn('space-y-6', className)}>
      {/* O*NET Selection */}
      <FormField
        control={form.control}
        name="onetSocCode"
        render={({ field }) => (
          <FormItem>
            <FormLabel className="flex items-center gap-2 text-base font-medium">
              <Briefcase className="h-4 w-4 text-blue-600" />
              Target Occupation
              <Badge variant="destructive" className="text-[10px] px-1.5 h-4">
                Required
              </Badge>
            </FormLabel>
            <FormControl>
              <ONetSearchCombobox
                value={field.value}
                onChange={handleOnetChange}
                placeholder="Search for a job title..."
              />
            </FormControl>
            <FormDescription>
              Select the O*NET occupation to benchmark candidates against
            </FormDescription>
            <FormMessage />
          </FormItem>
        )}
      />

      {/* O*NET Profile Preview */}
      {onetSocCode && (
        <Card className={cn('transition-all', isLoadingProfile && 'animate-pulse')}>
          <CardHeader className="pb-2">
            <div className="flex items-start justify-between">
              <div>
                <CardTitle className="text-sm font-medium">
                  {isLoadingProfile ? (
                    <Skeleton className="h-4 w-48" />
                  ) : (
                    onetProfile?.occupationTitle || 'Loading...'
                  )}
                </CardTitle>
                <CardDescription className="text-xs font-mono">
                  {onetSocCode}
                </CardDescription>
              </div>
              <Badge variant="secondary" className="gap-1">
                <Target className="h-3 w-3" />
                {onetProfile?.benchmarks?.length || 0} benchmarks
              </Badge>
            </div>
          </CardHeader>

          {onetProfile && onetProfile.benchmarks.length > 0 && (
            <CardContent className="pt-0">
              <Collapsible open={showBenchmarks} onOpenChange={setShowBenchmarks}>
                <CollapsibleTrigger className="flex items-center gap-2 text-sm text-primary hover:underline w-full justify-between py-2">
                  <span>View competency benchmarks</span>
                  {showBenchmarks ? (
                    <ChevronUp className="h-4 w-4" />
                  ) : (
                    <ChevronDown className="h-4 w-4" />
                  )}
                </CollapsibleTrigger>
                <CollapsibleContent>
                  <div className="space-y-2 pt-2 border-t">
                    {onetProfile.benchmarks.slice(0, 5).map((benchmark) => (
                      <div
                        key={benchmark.competencyCode}
                        className="flex items-center justify-between text-sm"
                      >
                        <span className="text-muted-foreground truncate flex-1 mr-2">
                          {benchmark.competencyName}
                        </span>
                        <div className="flex items-center gap-2 shrink-0">
                          <div className="w-16 h-1.5 bg-muted rounded-full overflow-hidden">
                            <div
                              className="h-full bg-blue-500 rounded-full"
                              style={{ width: `${(benchmark.requiredLevel / 7) * 100}%` }}
                            />
                          </div>
                          <span className="text-xs font-mono w-6 text-right">
                            {benchmark.requiredLevel.toFixed(1)}
                          </span>
                        </div>
                      </div>
                    ))}
                    {onetProfile.benchmarks.length > 5 && (
                      <p className="text-xs text-muted-foreground pt-1">
                        +{onetProfile.benchmarks.length - 5} more competencies
                      </p>
                    )}
                  </div>
                </CollapsibleContent>
              </Collapsible>
            </CardContent>
          )}
        </Card>
      )}

      {/* Strictness Level */}
      <FormField
        control={form.control}
        name="strictnessLevel"
        render={({ field }) => (
          <FormItem>
            <div className="flex items-center justify-between">
              <FormLabel className="flex items-center gap-2">
                <Shield className="h-4 w-4 text-muted-foreground" />
                Strictness Level
              </FormLabel>
              <Badge variant="outline" className={cn('font-medium', strictnessInfo.color)}>
                {strictnessInfo.label}
              </Badge>
            </div>
            <FormControl>
              <div className="pt-2 pb-1">
                <Slider
                  value={[field.value ?? 50]}
                  onValueChange={([v]) => field.onChange(v)}
                  min={0}
                  max={100}
                  step={5}
                  className="w-full"
                />
                <div className="flex justify-between text-[10px] text-muted-foreground mt-1">
                  <span>Lenient</span>
                  <span>Moderate</span>
                  <span>Strict</span>
                </div>
              </div>
            </FormControl>
            <FormDescription className="text-xs">
              {strictnessInfo.description}
            </FormDescription>
          </FormItem>
        )}
      />

      {/* Delta Testing Toggle (Future - Passport Integration) */}
      <Card className="border-dashed border-muted-foreground/30 bg-muted/20">
        <CardHeader className="py-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="p-1.5 rounded bg-muted">
                <Zap className="h-4 w-4 text-muted-foreground" />
              </div>
              <div>
                <CardTitle className="text-sm font-medium text-muted-foreground">
                  Delta Testing
                </CardTitle>
                <CardDescription className="text-xs">
                  Skip competencies from existing Passport
                </CardDescription>
              </div>
            </div>
            <Switch disabled className="opacity-50" />
          </div>
        </CardHeader>
        <CardContent className="pt-0 pb-3">
          <p className="text-xs text-muted-foreground italic">
            Coming in Phase 3 - Passport Integration
          </p>
        </CardContent>
      </Card>
    </div>
  );
}

export default JobFitConfigPanel;
```

---

### Task 7: Create TeamFitConfigPanel Component (1 day)

**File**: `D:\projects\diplom\skillsoft\frontend-app\src\components\blueprint-config\TeamFitConfigPanel.tsx`

```typescript
'use client';

import React, { useState, useCallback, useEffect } from 'react';
import { useFormContext } from 'react-hook-form';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Slider } from '@/components/ui/slider';
import { Skeleton } from '@/components/ui/skeleton';
import {
  FormField,
  FormItem,
  FormLabel,
  FormControl,
  FormDescription,
  FormMessage,
} from '@/components/ui/form';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { cn } from '@/lib/utils';
import {
  Users,
  Target,
  AlertCircle,
  TrendingUp,
  BarChart3,
  Gauge,
  Check,
  AlertTriangle,
} from 'lucide-react';
import { teamsApi } from '@/services/api';
import type { Team, TeamProfile } from '@/types/domain';

// ============================================================================
// Types
// ============================================================================

interface TeamFitConfigPanelProps {
  className?: string;
}

// ============================================================================
// Saturation Threshold Labels
// ============================================================================

function getSaturationLabel(value: number): { label: string; description: string } {
  if (value <= 0.5) {
    return {
      label: 'Low Coverage',
      description: 'Focus on filling major skill gaps',
    };
  } else if (value <= 0.75) {
    return {
      label: 'Moderate Coverage',
      description: 'Balance between gaps and existing strengths',
    };
  } else {
    return {
      label: 'High Coverage',
      description: 'Prioritize diversity and redundancy',
    };
  }
}

// ============================================================================
// Component
// ============================================================================

export function TeamFitConfigPanel({ className }: TeamFitConfigPanelProps) {
  const form = useFormContext();

  const [teams, setTeams] = useState<Team[]>([]);
  const [isLoadingTeams, setIsLoadingTeams] = useState(true);
  const [teamProfile, setTeamProfile] = useState<TeamProfile | null>(null);
  const [isLoadingProfile, setIsLoadingProfile] = useState(false);

  const teamId = form.watch('teamId');
  const saturationThreshold = form.watch('saturationThreshold') ?? 0.75;
  const saturationInfo = getSaturationLabel(saturationThreshold);

  // Load teams on mount
  useEffect(() => {
    async function loadTeams() {
      setIsLoadingTeams(true);
      try {
        const result = await teamsApi.getAllTeams();
        setTeams(result || []);
      } catch {
        setTeams([]);
      } finally {
        setIsLoadingTeams(false);
      }
    }

    loadTeams();
  }, []);

  // Load team profile when teamId changes
  useEffect(() => {
    if (!teamId) {
      setTeamProfile(null);
      return;
    }

    async function loadProfile() {
      setIsLoadingProfile(true);
      try {
        const profile = await teamsApi.getTeamProfile(teamId);
        setTeamProfile(profile);
      } catch {
        setTeamProfile(null);
      } finally {
        setIsLoadingProfile(false);
      }
    }

    loadProfile();
  }, [teamId]);

  // Calculate saturation stats
  const saturationStats = React.useMemo(() => {
    if (!teamProfile) return null;

    const entries = Object.entries(teamProfile.saturation);
    const belowThreshold = entries.filter(([_, v]) => v < saturationThreshold);
    const avgSaturation = entries.reduce((sum, [_, v]) => sum + v, 0) / entries.length;

    return {
      total: entries.length,
      belowThreshold: belowThreshold.length,
      avgSaturation,
      undersaturated: teamProfile.undersaturatedCompetencies,
    };
  }, [teamProfile, saturationThreshold]);

  const selectedTeam = teams.find((t) => t.id === teamId);

  return (
    <div className={cn('space-y-6', className)}>
      {/* Team Selection */}
      <FormField
        control={form.control}
        name="teamId"
        render={({ field }) => (
          <FormItem>
            <FormLabel className="flex items-center gap-2 text-base font-medium">
              <Users className="h-4 w-4 text-purple-600" />
              Target Team
              <Badge variant="destructive" className="text-[10px] px-1.5 h-4">
                Required
              </Badge>
            </FormLabel>
            <Select
              value={field.value}
              onValueChange={field.onChange}
              disabled={isLoadingTeams}
            >
              <FormControl>
                <SelectTrigger className="h-12">
                  {isLoadingTeams ? (
                    <div className="flex items-center gap-2">
                      <Skeleton className="h-4 w-4 rounded-full" />
                      <Skeleton className="h-4 w-32" />
                    </div>
                  ) : (
                    <SelectValue placeholder="Select a team..." />
                  )}
                </SelectTrigger>
              </FormControl>
              <SelectContent>
                {teams.length === 0 ? (
                  <div className="py-4 text-center text-sm text-muted-foreground">
                    No teams available
                  </div>
                ) : (
                  teams.map((team) => (
                    <SelectItem key={team.id} value={team.id}>
                      <div className="flex items-center gap-3">
                        <div className="flex items-center justify-center w-8 h-8 rounded-full bg-purple-100 dark:bg-purple-900">
                          <Users className="h-4 w-4 text-purple-600 dark:text-purple-400" />
                        </div>
                        <div>
                          <p className="font-medium">{team.name}</p>
                          <p className="text-xs text-muted-foreground">
                            {team.memberCount} members
                          </p>
                        </div>
                      </div>
                    </SelectItem>
                  ))
                )}
              </SelectContent>
            </Select>
            <FormDescription>
              Select the team to analyze for skill gaps
            </FormDescription>
            <FormMessage />
          </FormItem>
        )}
      />

      {/* Team Profile Preview */}
      {teamId && (
        <Card className={cn('transition-all', isLoadingProfile && 'animate-pulse')}>
          <CardHeader className="pb-2">
            <div className="flex items-start justify-between">
              <div>
                <CardTitle className="text-sm font-medium">
                  {selectedTeam?.name || 'Team Profile'}
                </CardTitle>
                <CardDescription className="text-xs">
                  {selectedTeam?.memberCount || 0} team members
                </CardDescription>
              </div>
              {saturationStats && (
                <Badge
                  variant={saturationStats.belowThreshold > 0 ? 'destructive' : 'secondary'}
                  className="gap-1"
                >
                  {saturationStats.belowThreshold > 0 ? (
                    <>
                      <AlertTriangle className="h-3 w-3" />
                      {saturationStats.belowThreshold} gaps
                    </>
                  ) : (
                    <>
                      <Check className="h-3 w-3" />
                      Balanced
                    </>
                  )}
                </Badge>
              )}
            </div>
          </CardHeader>

          {isLoadingProfile ? (
            <CardContent className="space-y-2 pt-0">
              <Skeleton className="h-3 w-full" />
              <Skeleton className="h-3 w-3/4" />
              <Skeleton className="h-3 w-1/2" />
            </CardContent>
          ) : saturationStats ? (
            <CardContent className="pt-0">
              {/* Average saturation gauge */}
              <div className="flex items-center gap-3 py-2">
                <Gauge className="h-5 w-5 text-muted-foreground" />
                <div className="flex-1">
                  <div className="flex justify-between text-xs mb-1">
                    <span className="text-muted-foreground">Avg. Saturation</span>
                    <span className="font-mono font-medium">
                      {(saturationStats.avgSaturation * 100).toFixed(0)}%
                    </span>
                  </div>
                  <div className="h-2 bg-muted rounded-full overflow-hidden">
                    <div
                      className={cn(
                        'h-full rounded-full transition-all',
                        saturationStats.avgSaturation >= 0.75
                          ? 'bg-green-500'
                          : saturationStats.avgSaturation >= 0.5
                          ? 'bg-yellow-500'
                          : 'bg-red-500'
                      )}
                      style={{ width: `${saturationStats.avgSaturation * 100}%` }}
                    />
                  </div>
                </div>
              </div>

              {/* Undersaturated competencies */}
              {saturationStats.undersaturated.length > 0 && (
                <div className="pt-2 border-t mt-2">
                  <p className="text-xs font-medium text-muted-foreground mb-2">
                    Skill Gaps Detected:
                  </p>
                  <div className="flex flex-wrap gap-1">
                    {saturationStats.undersaturated.slice(0, 4).map((competencyName) => (
                      <Badge
                        key={competencyName}
                        variant="outline"
                        className="text-[10px] bg-red-50 text-red-700 border-red-200 dark:bg-red-950/50 dark:text-red-300 dark:border-red-800"
                      >
                        {competencyName}
                      </Badge>
                    ))}
                    {saturationStats.undersaturated.length > 4 && (
                      <Badge variant="outline" className="text-[10px]">
                        +{saturationStats.undersaturated.length - 4} more
                      </Badge>
                    )}
                  </div>
                </div>
              )}
            </CardContent>
          ) : null}
        </Card>
      )}

      {/* Saturation Threshold */}
      <FormField
        control={form.control}
        name="saturationThreshold"
        render={({ field }) => (
          <FormItem>
            <div className="flex items-center justify-between">
              <FormLabel className="flex items-center gap-2">
                <BarChart3 className="h-4 w-4 text-muted-foreground" />
                Saturation Threshold
              </FormLabel>
              <Badge variant="outline">
                {((field.value ?? 0.75) * 100).toFixed(0)}%
              </Badge>
            </div>
            <FormControl>
              <div className="pt-2 pb-1">
                <Slider
                  value={[field.value ?? 0.75]}
                  onValueChange={([v]) => field.onChange(v)}
                  min={0.3}
                  max={1.0}
                  step={0.05}
                  className="w-full"
                />
                <div className="flex justify-between text-[10px] text-muted-foreground mt-1">
                  <span>30%</span>
                  <span>75%</span>
                  <span>100%</span>
                </div>
              </div>
            </FormControl>
            <FormDescription className="text-xs">
              {saturationInfo.description}
            </FormDescription>
          </FormItem>
        )}
      />
    </div>
  );
}

export default TeamFitConfigPanel;
```

---

### Task 8: Update NewTestForm for Goal-Aware Configuration (1 day)

**File**: `D:\projects\diplom\skillsoft\frontend-app\app\(workspace)\test-templates\new\_components\NewTestForm.tsx`

**Key Changes**:

1. **Update form schema** (around line 96):

```typescript
const formSchema = z.object({
  // Basic info (Step 1)
  name: z.string().min(3, 'Minimum 3 characters').max(100, 'Maximum 100 characters'),
  description: z.string().max(500, 'Maximum 500 characters').optional(),
  goal: z.nativeEnum(AssessmentGoal),

  // OVERVIEW-specific
  competencyIds: z.array(z.string()).optional(),
  includeBigFive: z.boolean().optional(),
  preferredDifficulty: z.enum(['BASIC', 'INTERMEDIATE', 'ADVANCED']).optional(),

  // JOB_FIT-specific
  onetSocCode: z.string().optional(),
  strictnessLevel: z.number().min(0).max(100).optional(),

  // TEAM_FIT-specific
  teamId: z.string().optional(),
  saturationThreshold: z.number().min(0.3).max(1.0).optional(),

  // Common settings (Step 3)
  questionsPerIndicator: z.number().min(1).max(5),
  timeLimitMinutes: z.number().min(5).max(180),
  passingScore: z.number().min(10).max(100),
  shuffleQuestions: z.boolean(),
  shuffleOptions: z.boolean(),
  allowSkip: z.boolean(),
  allowBackNavigation: z.boolean(),
  showResultsImmediately: z.boolean(),
}).superRefine((data, ctx) => {
  // Goal-specific validation
  if (data.goal === AssessmentGoal.OVERVIEW) {
    if (!data.competencyIds || data.competencyIds.length === 0) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: 'Select at least one competency',
        path: ['competencyIds'],
      });
    }
  }

  if (data.goal === AssessmentGoal.JOB_FIT) {
    if (!data.onetSocCode) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: 'Select a target occupation',
        path: ['onetSocCode'],
      });
    }
  }

  if (data.goal === AssessmentGoal.TEAM_FIT) {
    if (!data.teamId) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: 'Select a target team',
        path: ['teamId'],
      });
    }
  }
});
```

2. **Replace BasicInfoStep goal selector** with `GoalSelector` component

3. **Add goal-aware Step 2**:

```typescript
function GoalConfigStep({ form, competencies, goal }: {
  form: any;
  competencies: CompetencyOption[];
  goal: AssessmentGoal;
}) {
  switch (goal) {
    case AssessmentGoal.OVERVIEW:
      return (
        <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
          <CompetenciesStep form={form} competencies={competencies} />
          <OverviewConfigPanel
            competencies={competencies}
            selectedCompetencyIds={form.watch('competencyIds') || []}
            onCompetencyChange={(ids) => form.setValue('competencyIds', ids)}
          />
        </div>
      );

    case AssessmentGoal.JOB_FIT:
      return (
        <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
          <JobFitConfigPanel />
        </div>
      );

    case AssessmentGoal.TEAM_FIT:
      return (
        <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
          <TeamFitConfigPanel />
        </div>
      );

    default:
      return null;
  }
}
```

4. **Update STEPS configuration**:

```typescript
const STEPS = [
  { id: 1, title: 'Basic Info', description: 'Name and goal', icon: FileText },
  { id: 2, title: 'Configuration', description: 'Goal-specific setup', icon: Target },
  { id: 3, title: 'Settings', description: 'Test parameters', icon: Settings },
  { id: 4, title: 'Review', description: 'Summary', icon: Eye },
] as const;
```

5. **Update step rendering**:

```typescript
// In main component render
{currentStep === 1 && <BasicInfoStep form={form} />}
{currentStep === 2 && (
  <GoalConfigStep
    form={form}
    competencies={competencies}
    goal={form.watch('goal')}
  />
)}
{currentStep === 3 && <ConfigurationStep form={form} />}
{currentStep === 4 && <ReviewStep form={form} competencies={competencies} />}
```

---

### Task 9: Create Component Index File (0.25 day)

**File**: `D:\projects\diplom\skillsoft\frontend-app\src\components\blueprint-config\index.ts`

```typescript
export { GoalSelector } from './GoalSelector';
export { ONetSearchCombobox } from './ONetSearchCombobox';
export { OverviewConfigPanel } from './OverviewConfigPanel';
export { JobFitConfigPanel } from './JobFitConfigPanel';
export { TeamFitConfigPanel } from './TeamFitConfigPanel';
```

---

## 4. Integration Testing Checkpoints

### Checkpoint 1: Type Definitions (After Task 1)

```bash
cd frontend-app
npm run type-check
```

Expected: No TypeScript errors related to new types.

### Checkpoint 2: API Methods (After Task 2)

Create a test page to verify API methods:

```typescript
// app/(workspace)/test-templates/new/api-test/page.tsx
// Temporary test page - remove after verification
```

Test cases:
- [ ] `onetApi.searchOccupations('software')` returns results
- [ ] `onetApi.getProfile('15-1252.00')` returns profile
- [ ] `teamsApi.getAllTeams()` returns teams (or empty array)
- [ ] `teamsApi.getTeamProfile(teamId)` returns profile

### Checkpoint 3: GoalSelector Component (After Task 3)

```bash
npm run dev
```

Navigate to `/test-templates/new` and verify:
- [ ] Goal cards render correctly on mobile (320px)
- [ ] Goal cards render correctly on desktop (1024px)
- [ ] Selection changes form value
- [ ] Keyboard navigation works (Tab, Enter, Space)
- [ ] Visual feedback on selection

### Checkpoint 4: ONetSearchCombobox (After Task 4)

Test in isolation:
- [ ] Popover opens/closes correctly
- [ ] Search debouncing works (300ms delay)
- [ ] Results display correctly
- [ ] Selection updates form
- [ ] Clear button works
- [ ] Loading states display
- [ ] Empty state displays

### Checkpoint 5: Config Panels (After Tasks 5-7)

For each panel:
- [ ] Renders without errors
- [ ] Form fields bind correctly
- [ ] Validation messages appear
- [ ] Mobile layout is usable
- [ ] API data loads and displays

### Checkpoint 6: Full Integration (After Task 8)

End-to-end test:
1. [ ] Navigate to `/test-templates/new`
2. [ ] Enter name, select OVERVIEW goal, proceed
3. [ ] Select competencies, configure Big Five
4. [ ] Configure settings
5. [ ] Review and create
6. [ ] Repeat for JOB_FIT goal
7. [ ] Repeat for TEAM_FIT goal
8. [ ] Verify created templates have correct blueprint data

---

## 5. Mobile-First Implementation Notes

### Touch Targets

All interactive elements must meet 44x44px minimum:
- Radio cards: `p-4` (16px padding on all sides)
- Buttons: `h-12` minimum height
- Slider thumbs: Already handled by Radix

### Safe Areas

Mobile footer uses `safe-area-bottom` for notch devices:
```css
.safe-area-bottom {
  padding-bottom: env(safe-area-inset-bottom, 16px);
}
```

### Viewport Considerations

- Popover content should not exceed viewport width
- Use `w-[var(--radix-popover-trigger-width)]` for popovers
- Limit dropdown heights to 300px with scroll

### Gesture Support

- Swipe navigation is already implemented in test player
- Form steps could add swipe support in future phases

### Reduced Motion

All animations respect `prefers-reduced-motion`:
```css
@media (prefers-reduced-motion: reduce) {
  .animate-in { animation: none; }
}
```

---

## 6. File Structure Summary

```
frontend-app/
├── src/
│   ├── types/
│   │   └── domain.ts                    # Task 1: Add types
│   ├── services/
│   │   └── api.ts                       # Task 2: Add API methods
│   └── components/
│       └── blueprint-config/
│           ├── index.ts                 # Task 9: Exports
│           ├── GoalSelector.tsx         # Task 3: Goal cards
│           ├── ONetSearchCombobox.tsx   # Task 4: O*NET search
│           ├── OverviewConfigPanel.tsx  # Task 5: Overview config
│           ├── JobFitConfigPanel.tsx    # Task 6: Job Fit config
│           └── TeamFitConfigPanel.tsx   # Task 7: Team Fit config
└── app/
    └── (workspace)/
        └── test-templates/
            └── new/
                └── _components/
                    └── NewTestForm.tsx  # Task 8: Integration
```

---

## 7. Dependencies and Execution Order

```
Task 1 (Types) ──┐
                 ├──> Task 2 (API) ──┐
                 │                   │
                 ├──> Task 3 (GoalSelector) ──────────┐
                 │                                    │
                 └──> Task 4 (ONetSearchCombobox) ────┼──> Task 6 (JobFitPanel)
                                                      │
                 Task 5 (OverviewPanel) ──────────────┤
                                                      │
                 Task 7 (TeamFitPanel) ───────────────┤
                                                      │
                                                      └──> Task 8 (Integration)
                                                                    │
                                                      Task 9 ───────┘
```

**Critical Path**: Tasks 1 -> 2 -> 4 -> 6 -> 8

**Parallel Tracks**:
- Track A: Tasks 3, 5 (OVERVIEW support)
- Track B: Tasks 4, 6 (JOB_FIT support)
- Track C: Task 7 (TEAM_FIT support)

---

## 8. Risk Mitigation

### Backend API Not Ready

If O*NET or Teams endpoints are not available:

1. Use mock data mode in API layer:

```typescript
const USE_MOCK_DATA = process.env.NEXT_PUBLIC_USE_MOCK_API === 'true';

export const onetApi = {
  searchOccupations: async (query: string) => {
    if (USE_MOCK_DATA) {
      return MOCK_OCCUPATIONS.filter(o =>
        o.title.toLowerCase().includes(query.toLowerCase())
      );
    }
    // ... real API call
  },
};
```

2. Set environment variable:
```env
NEXT_PUBLIC_USE_MOCK_API=true
```

### Performance Issues

- Use `useDebounce` for all search inputs (300ms)
- Limit search results to 15 items
- Use `React.memo` for list items
- Consider virtualization for large lists (Phase 5)

### Mobile Performance

- Lazy load config panels with `React.lazy`
- Use CSS containment for complex cards
- Avoid layout thrashing in scrollable areas

---

## 9. Success Criteria

Phase 1 is complete when:

1. **Functional**
   - [ ] All three assessment goals can be selected
   - [ ] Goal-specific configuration forms render correctly
   - [ ] O*NET search works with debouncing
   - [ ] Team selection works with saturation preview
   - [ ] Form validation enforces required fields per goal
   - [ ] Created templates have correct `blueprint` data

2. **Visual**
   - [ ] Goal selector cards match design spec
   - [ ] Configuration panels are visually distinct per goal
   - [ ] Mobile layout works on 320px screens
   - [ ] Dark mode is fully supported

3. **Performance**
   - [ ] Initial render < 500ms
   - [ ] Search results appear < 300ms after typing stops
   - [ ] No layout shift during loading

4. **Accessibility**
   - [ ] All interactive elements are keyboard navigable
   - [ ] Screen readers announce selections correctly
   - [ ] Color contrast meets WCAG AA (4.5:1)

---

*Document Version: 1.0*
*Created: 2025-12-31*
*Author: workflow-orchestrator agent*
*Based on SPEC.md version 2.0*
