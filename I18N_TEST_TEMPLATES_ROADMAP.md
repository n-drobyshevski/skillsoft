# I18N Implementation Roadmap: test-templates/ Pages

## Executive Summary

This document outlines a comprehensive implementation plan for extending internationalization (i18n) to the `test-templates/` pages in the SkillSoft frontend application. The project uses Next.js 16 with App Router and `next-intl` library, supporting English and Russian locales.

**Current State**: The application already has a mature i18n setup with `next-intl`, translation files (`messages/en.json`, `messages/ru.json`), and established patterns. However, 19 files in `test-templates/` still contain hardcoded Russian strings that need to be migrated to the translation system.

**Target**: Full bilingual support (English/Russian) for all 87 test-template components.

---

## Table of Contents

1. [Current I18N Architecture](#1-current-i18n-architecture)
2. [Files Requiring I18N Migration](#2-files-requiring-i18n-migration)
3. [Translation Key Naming Conventions](#3-translation-key-naming-conventions)
4. [Implementation Workflow State Machine](#4-implementation-workflow-state-machine)
5. [Phase-by-Phase Implementation Plan](#5-phase-by-phase-implementation-plan)
6. [Code Patterns and Examples](#6-code-patterns-and-examples)
7. [Dynamic Content Handling](#7-dynamic-content-handling)
8. [Edge Cases and Special Considerations](#8-edge-cases-and-special-considerations)
9. [Testing Strategy](#9-testing-strategy)
10. [Rollback and Compensation Strategies](#10-rollback-and-compensation-strategies)
11. [Success Metrics](#11-success-metrics)

---

## 1. Current I18N Architecture

### Technology Stack
- **Library**: `next-intl` (integrated with Next.js 16 App Router)
- **Locales**: `en` (English), `ru` (Russian - default)
- **Config Location**: `D:\projects\diplom\skillsoft\frontend-app\src\i18n\`

### File Structure
```
frontend-app/
  messages/
    en.json          # English translations (~35K tokens)
    ru.json          # Russian translations (~50K tokens)
  src/
    i18n/
      config.ts      # Locale definitions and helpers
      request.ts     # Server-side locale detection (cookie > Accept-Language > default)
    __tests__/
      i18n/
        translation-coverage.test.ts  # Key sync validation
        enum-translations.test.ts     # Enum translation tests
        validation.test.ts            # Format validation
```

### Existing Patterns

**Server Components** (RSC):
```typescript
import { getTranslations } from 'next-intl/server';

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("metadata.testTemplates");
  return { title: t("title") };
}
```

**Client Components**:
```typescript
'use client';
import { useTranslations } from 'next-intl';

export function MyComponent() {
  const t = useTranslations('template');
  const tCommon = useTranslations('common');
  return <h1>{t('title')}</h1>;
}
```

### Current Namespace Structure
The existing translation files use these top-level namespaces:
- `common` - Shared UI strings (save, cancel, loading, etc.)
- `navigation` - Menu and navigation items
- `assessment` - Test-taking related strings
- `template` - Test template management strings
- `competency`, `indicator`, `question` - Domain entities
- `forms` - Form labels and validation
- `errors` - Error messages
- `metadata` - Page metadata for SEO
- `enums` - Enum display values
- `help` - Help tooltips and descriptions

---

## 2. Files Requiring I18N Migration

### Priority 1: Critical User-Facing Pages (19 files with hardcoded Cyrillic)

| File | Type | Hardcoded Strings | Priority |
|------|------|-------------------|----------|
| `test-templates/page.tsx` | Server | Error messages | High |
| `test-templates/history/page.tsx` | Server | Full page content | **Critical** |
| `test-templates/take/[sessionId]/page.tsx` | Client | Error/status messages | **Critical** |
| `test-templates/take/[sessionId]/error.tsx` | Client | Error messages | High |
| `test-templates/error.tsx` | Client | Error messages | High |
| `test-templates/new/_components/NewTestForm.tsx` | Client | Form labels | High |
| `test-templates/_components/ErrorDisplay.tsx` | Client | Error display | High |
| `test-templates/_components/StartTestSessionButton.tsx` | Client | Button text | Medium |
| `test-templates/_components/StartTestDriveButton.tsx` | Client | Button text | Medium |
| `test-templates/_components/TemplateFilters.tsx` | Client | Filter labels | Medium |
| `test-templates/_components/TestTemplatesGrid.tsx` | Client | UI text | Medium |
| `test-templates/results/[resultId]/_components/shared/CompetencyProfile.tsx` | Client | Labels | Medium |
| `test-templates/results/[resultId]/_components/shared/HeroCard.tsx` | Client | Labels | Medium |
| `test-templates/results/[resultId]/page_new.tsx` | Server | Page content | Medium |
| `test-templates/[id]/_components/TemplateHeader.tsx` | Client | Headers | Low |
| `test-templates/[id]/_components/TemplateHubHeader.tsx` | Client | Headers | Low |
| `test-templates/take/[sessionId]/_components/MultipleChoiceQuestion.tsx` | Client | Instructions | Medium |
| `test-templates/take/[sessionId]/_components/OpenTextQuestion.tsx` | Client | Placeholders | Medium |
| `test-templates/loading.tsx` | Server | Loading text | Low |

### Priority 2: Components Already Using i18n (partial migration)

These components use `useTranslations` but may have some remaining hardcoded strings:
- `TemplatesPageHeader.tsx` - Uses `template` namespace
- `TemplatesPageContent.tsx` - Uses `template` namespace
- `TestTemplateCard.tsx` - Uses `template` and `common` namespaces
- `DeleteTestTemplateButton.tsx` - Uses `template` namespace
- `MobileTemplateActions.tsx` - Uses `template` namespace
- `SettingsForm.tsx` - Uses `template` and `common` namespaces
- `LikertScaleQuestion.tsx` - Uses `assessment` namespace

### Priority 3: Components Without User-Facing Text

These components primarily use props/data from API and don't need direct i18n:
- Builder components (Canvas, WeightedCanvas, etc.)
- Skeleton/Loading components
- Layout components

---

## 3. Translation Key Naming Conventions

### Namespace Organization

```
template.                           # Root namespace for test-templates
  title                            # Page title
  selectTemplate                   # Subtitle
  create                           # "Create Template"

  history.                         # History page section
    title                          # "My Results"
    description                    # "History of completed tests..."
    tabs.
      results                      # "Results"
      sessions                     # "In Progress"
    stats.
      testsTaken                   # "Tests Taken"
      passed                       # "Passed"
      averageScore                 # "Average Score"
      totalTime                    # "Total Time"
    noResults.
      title                        # "No results"
      description                  # "You haven't completed..."
    noSessions.
      title                        # "No active sessions"
      description                  # "You don't have..."

  take.                            # Test-taking section
    errors.
      notFound                     # "Test not found"
      notFoundDescription          # "Session not found..."
      invalidSession               # "Invalid session"
      serverError                  # "Server error"
      abandoned                    # "This session was abandoned"
      completed                    # "This test was already completed"
      timedOut                     # "Time expired"
    dialogs.
      existingSession.
        title                      # "Continue previous session?"
        description                # "You have an unfinished test..."
        continue                   # "Continue"
        startNew                   # "Start new"
    loading.
      retrying                     # "Retrying {attempt} of {max}..."

  start.                           # Start assessment page
    checking                       # "Checking Readiness..."
    notAvailable                   # "Assessment Not Available"
    begin                          # "Begin Assessment"
    progressSaved                  # "Your progress will be saved..."
    labels.
      time                         # "Time"
      questions                    # "Questions"
      competencies                 # "Competencies"

  results.                         # Results section
    title                          # "Results"
    labels.
      score                        # "Score"
      passed                       # "Passed"
      failed                       # "Failed"
      questionsAnswered            # "Questions answered"
      timeSpent                    # "Time spent"
```

### Key Naming Rules

1. **Use camelCase** for all keys: `noTemplatesAvailable`, not `no_templates_available`
2. **Group by feature/page**: `template.history.stats.passed`
3. **Use descriptive names**: `confirmDeleteDescription`, not `desc1`
4. **Suffix patterns**:
   - `*Title` - Headings and titles
   - `*Description` - Longer explanatory text
   - `*Label` - Form labels and short identifiers
   - `*Placeholder` - Input placeholders
   - `*Error` - Error messages
   - `*Success` - Success messages
   - `*Confirm` - Confirmation dialogs

---

## 4. Implementation Workflow State Machine

```
                                    +------------------+
                                    |    START         |
                                    +--------+---------+
                                             |
                                             v
                          +------------------+------------------+
                          |   PHASE 1: PREPARATION             |
                          |   - Create translation keys        |
                          |   - Update en.json & ru.json       |
                          |   - Run translation coverage tests |
                          +------------------+-----------------+
                                             |
                           pass tests?       |
                              +----NO--------+--------YES------+
                              |                                |
                              v                                v
                    +---------+----------+        +------------+-----------+
                    | FIX KEY MISMATCH   |        |  PHASE 2: MIGRATION    |
                    | - Sync keys        |        |  - Update components   |
                    | - Fix empty values |        |  - Replace strings     |
                    +--------------------+        |  - Add imports         |
                              |                   +------------+-----------+
                              |                                |
                              +--------------------------------+
                                             |
                                             v
                          +------------------+------------------+
                          |   PHASE 3: TESTING                 |
                          |   - Manual UI testing both locales |
                          |   - Run unit tests                 |
                          |   - Check console for missing keys |
                          +------------------+-----------------+
                                             |
                           all pass?         |
                              +----NO--------+--------YES------+
                              |                                |
                              v                                v
                    +---------+----------+        +------------+-----------+
                    | COMPENSATION       |        |  PHASE 4: VALIDATION   |
                    | - Rollback changes |        |  - Full regression     |
                    | - Fix issues       |        |  - Performance check   |
                    | - Retry migration  |        |  - Accessibility audit |
                    +--------------------+        +------------+-----------+
                                                               |
                                                               v
                                                  +------------+-----------+
                                                  |   COMPLETE             |
                                                  |   - Document changes   |
                                                  |   - Update changelog   |
                                                  +------------------------+
```

### State Descriptions

| State | Entry Criteria | Exit Criteria | Rollback Trigger |
|-------|---------------|---------------|------------------|
| PREPARATION | Task assigned | Translation files updated, tests pass | Key sync failure |
| MIGRATION | Keys ready | All files updated | Build failure |
| TESTING | Migration complete | All tests pass | Test failure |
| VALIDATION | Tests pass | Full QA approval | Regression found |
| COMPLETE | Validation pass | PR merged | - |

---

## 5. Phase-by-Phase Implementation Plan

### Phase 1: Translation File Preparation (Day 1)

**Objective**: Add all required translation keys to `en.json` and `ru.json`

#### Step 1.1: Audit Existing Translations
```bash
# Run translation coverage test
cd frontend-app
npm run test -- src/__tests__/i18n/translation-coverage.test.ts
```

#### Step 1.2: Add History Page Keys

**en.json additions:**
```json
{
  "template": {
    "history": {
      "title": "My Results",
      "description": "History of completed tests and current sessions",
      "allTemplates": "All templates",
      "tabs": {
        "results": "Results",
        "sessions": "In Progress"
      },
      "stats": {
        "testsTaken": "Tests Taken",
        "passed": "Passed",
        "averageScore": "Average Score",
        "totalTime": "Total Time"
      },
      "noResults": {
        "title": "No results",
        "description": "You haven't completed any tests yet.",
        "action": "Take your first test"
      },
      "noSessions": {
        "title": "No active sessions",
        "description": "You don't have any unfinished tests.",
        "action": "Start a new test"
      },
      "resultCard": {
        "passed": "Passed",
        "failed": "Failed",
        "questions": "questions",
        "details": "Details",
        "inProgress": "In Progress",
        "started": "Started",
        "continue": "Continue",
        "of": "of"
      },
      "completionHistory": "Completion History"
    }
  }
}
```

**ru.json additions:**
```json
{
  "template": {
    "history": {
      "title": "Мои результаты",
      "description": "История пройденных тестов и текущие сессии",
      "allTemplates": "Все шаблоны",
      "tabs": {
        "results": "Результаты",
        "sessions": "В процессе"
      },
      "stats": {
        "testsTaken": "Тестов пройдено",
        "passed": "Успешно",
        "averageScore": "Средний балл",
        "totalTime": "Общее время"
      },
      "noResults": {
        "title": "Нет результатов",
        "description": "Вы ещё не завершили ни одного теста.",
        "action": "Пройти первый тест"
      },
      "noSessions": {
        "title": "Нет активных сессий",
        "description": "У вас нет незавершённых тестов.",
        "action": "Начать новый тест"
      },
      "resultCard": {
        "passed": "Пройден",
        "failed": "Не пройден",
        "questions": "вопросов",
        "details": "Подробнее",
        "inProgress": "В процессе",
        "started": "Начат",
        "continue": "Продолжить",
        "of": "из"
      },
      "completionHistory": "История прохождений"
    }
  }
}
```

#### Step 1.3: Add Test-Taking Error Keys

**en.json additions:**
```json
{
  "template": {
    "take": {
      "errors": {
        "notFound": "Test not found",
        "notFoundDescription": "The test session was not found. It may have been deleted or completed.",
        "invalidSession": "Invalid session",
        "invalidSessionDescription": "This test session has already been completed or cancelled.",
        "serverError": "Server error",
        "serverErrorDescription": "Failed to load the test due to a server error. Please try again in a few seconds.",
        "abandoned": "Test was cancelled",
        "abandonedDescription": "This session was cancelled. You can start a new test.",
        "completed": "Test already completed",
        "completedDescription": "This test has already been finished.",
        "timedOut": "Time expired",
        "timedOutDescription": "The allocated time for the test has ended.",
        "templateNotSpecified": "Test template not specified",
        "sessionNotFound": "Session not found",
        "userNotAuthenticated": "You must sign in to take the test",
        "userDataError": "Failed to get user data",
        "invalidStatus": "Invalid session status"
      },
      "dialogs": {
        "existingSession": {
          "title": "Continue previous session?",
          "description": "You have an unfinished test. You can continue it or start over.",
          "continue": "Continue",
          "startNew": "Start over",
          "loading": "Loading..."
        }
      },
      "actions": {
        "toTestList": "Back to tests",
        "tryAgain": "Try again",
        "startNewTest": "Start new test"
      },
      "loading": {
        "retrying": "Retrying {attempt} of {max}..."
      }
    }
  }
}
```

#### Step 1.4: Add Start Page Keys

**en.json additions:**
```json
{
  "template": {
    "start": {
      "title": "Start Assessment",
      "description": "Begin your competency assessment.",
      "checking": "Checking Readiness...",
      "notAvailable": "Assessment Not Available",
      "begin": "Begin Assessment",
      "progressSaved": "Your progress will be saved automatically. You can pause and resume at any time.",
      "failedToStart": "Failed to Start Assessment",
      "labels": {
        "time": "Time",
        "questions": "Questions",
        "competencies": "Competencies",
        "min": "min"
      }
    }
  }
}
```

#### Step 1.5: Run Coverage Tests
```bash
npm run test -- src/__tests__/i18n/translation-coverage.test.ts
```

**Expected Output:**
- "Russian has all English keys" - PASS
- "English has all Russian keys" - PASS
- "Both files have the same number of keys" - PASS


### Phase 2: Component Migration (Days 2-4)

#### Step 2.1: History Page Migration

**File**: `app/(workspace)/test-templates/history/page.tsx`

```typescript
// Before
<PageHeader
  title="Мои результаты"
  description="История пройденных тестов и текущие сессии"
>

// After
import { getTranslations } from 'next-intl/server';

export default async function TestHistoryPage() {
  const t = await getTranslations('template.history');

  return (
    <PageHeader
      title={t('title')}
      description={t('description')}
    >
```

**Full migration checklist for history/page.tsx:**
- [ ] Import `getTranslations` from `next-intl/server`
- [ ] Add `const t = await getTranslations('template.history');`
- [ ] Replace "Мои результаты" with `t('title')`
- [ ] Replace "История пройденных тестов..." with `t('description')`
- [ ] Replace "Все шаблоны" with `t('allTemplates')`
- [ ] Replace "Результаты" with `t('tabs.results')`
- [ ] Replace "В процессе" with `t('tabs.sessions')`
- [ ] Replace stat labels with `t('stats.*')`
- [ ] Replace empty state messages with `t('noResults.*')` / `t('noSessions.*')`
- [ ] Replace result card labels
- [ ] Update `formatDate` to use locale-aware formatting

#### Step 2.2: Test Take Page Migration

**File**: `app/(workspace)/test-templates/take/[sessionId]/page.tsx`

This is a client component, so use `useTranslations`:

```typescript
'use client';
import { useTranslations } from 'next-intl';

export default function TestTakePage() {
  const t = useTranslations('template.take');
  const tErrors = useTranslations('errors');

  // Replace hardcoded error messages
  setError(t('errors.templateNotSpecified'));
  setError(t('errors.sessionNotFound'));
  setError(t('errors.abandoned'));
  // etc.
}
```

**Migration checklist:**
- [ ] Import `useTranslations` from `next-intl`
- [ ] Add translation hooks at component top
- [ ] Replace all error message strings
- [ ] Replace dialog content
- [ ] Replace loading/retry messages
- [ ] Replace action button labels

#### Step 2.3: Error Display Components

**Files to migrate:**
- `error.tsx`
- `take/[sessionId]/error.tsx`
- `_components/ErrorDisplay.tsx`

Pattern:
```typescript
'use client';
import { useTranslations } from 'next-intl';

export default function Error({ error, reset }: { error: Error; reset: () => void }) {
  const t = useTranslations('errors');

  return (
    <div>
      <h2>{t('somethingWentWrong')}</h2>
      <p>{t('errorLoadingPage')}</p>
      <button onClick={reset}>{t('tryAgain')}</button>
    </div>
  );
}
```

#### Step 2.4: Start Assessment Components

**Files to migrate:**
- `[id]/start/page.tsx` - Server component (metadata)
- `[id]/start/StartAssessmentClient.tsx` - Client component

```typescript
// StartAssessmentClient.tsx
'use client';
import { useTranslations } from 'next-intl';

export function StartAssessmentClient({ ... }) {
  const t = useTranslations('template.start');

  return (
    <Button>
      {isCheckingReadiness ? (
        <>
          <Loader2 className="animate-spin" />
          {t('checking')}
        </>
      ) : !isReady ? (
        t('notAvailable')
      ) : (
        <>
          {t('begin')}
          <ArrowRight />
        </>
      )}
    </Button>
  );
}
```

#### Step 2.5: Results Components

**Files to migrate:**
- `results/[resultId]/_components/shared/CompetencyProfile.tsx`
- `results/[resultId]/_components/shared/HeroCard.tsx`
- `results/[resultId]/_components/overview/OverviewResultView.tsx`

```typescript
'use client';
import { useTranslations } from 'next-intl';

export function CompetencyProfile({ result }) {
  const t = useTranslations('template.results');

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t('competencyProfile')}</CardTitle>
      </CardHeader>
    </Card>
  );
}
```

### Phase 3: Testing (Day 5)

#### Step 3.1: Automated Tests
```bash
# Run all i18n tests
npm run test -- src/__tests__/i18n/

# Run component tests
npm run test -- src/__tests__/components/test-templates/
```

#### Step 3.2: Manual Testing Checklist

**English Locale Testing:**
1. [ ] Visit `/test-templates` - verify page title and empty state
2. [ ] Visit `/test-templates/history` - verify all labels
3. [ ] Start a test - verify start page labels
4. [ ] Take a test - verify question UI and navigation
5. [ ] Complete a test - verify results display
6. [ ] Check error states - force errors and verify messages

**Russian Locale Testing:**
Repeat all above steps with Russian locale (set cookie or browser language)

#### Step 3.3: Console Verification
Open browser console and check for missing translation warnings:
```
[intl] Missing message: "template.history.unknown" in locale "en"
```

### Phase 4: Validation (Day 6)

#### Step 4.1: Full Regression Test
- [ ] Test all test-template flows end-to-end
- [ ] Verify no layout breaking from longer text
- [ ] Check RTL-readiness (no hardcoded directional styles)
- [ ] Verify date/number formatting

#### Step 4.2: Performance Check
- [ ] Bundle size comparison before/after
- [ ] No additional HTTP requests for translations
- [ ] SSR rendering time unchanged

#### Step 4.3: Accessibility Audit
- [ ] Screen reader testing
- [ ] Language attribute properly set
- [ ] All interactive elements labeled

---

## 6. Code Patterns and Examples

### Server Component Pattern

```typescript
// app/(workspace)/test-templates/page.tsx
import { getTranslations } from 'next-intl/server';
import type { Metadata } from 'next';

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations('metadata.testTemplates');
  return {
    title: t('title'),
    description: t('description'),
  };
}

export default async function TestsPage() {
  const t = await getTranslations('template');
  const tErrors = await getTranslations('errors');

  return (
    <div>
      <h1>{t('title')}</h1>
    </div>
  );
}
```

### Client Component Pattern

```typescript
// app/(workspace)/test-templates/_components/TemplateCard.tsx
'use client';

import { useTranslations } from 'next-intl';

interface Props {
  template: TestTemplate;
}

export function TemplateCard({ template }: Props) {
  const t = useTranslations('template');
  const tCommon = useTranslations('common');

  return (
    <Card>
      <CardHeader>
        <h3>{template.name}</h3>
        <Badge>{t('recommended')}</Badge>
      </CardHeader>
      <CardFooter>
        <Button>{tCommon('edit')}</Button>
      </CardFooter>
    </Card>
  );
}
```

### Interpolation Pattern

```typescript
// Translation file
{
  "questionsAnswered": "{answered} of {total} questions answered",
  "retrying": "Retrying {attempt} of {max}..."
}

// Component
const t = useTranslations('template');
<p>{t('questionsAnswered', { answered: 5, total: 20 })}</p>
<p>{t('retrying', { attempt: 2, max: 3 })}</p>
```

### Pluralization Pattern

```typescript
// Translation file (ICU format)
{
  "questionsCount": "{count, plural, one {# question} other {# questions}}"
}

// Russian
{
  "questionsCount": "{count, plural, one {# вопрос} few {# вопроса} many {# вопросов} other {# вопросов}}"
}

// Component
<span>{t('questionsCount', { count: questionCount })}</span>
```

### Date Formatting Pattern

```typescript
import { useFormatter } from 'next-intl';

function DateDisplay({ date }: { date: Date }) {
  const format = useFormatter();

  return (
    <time dateTime={date.toISOString()}>
      {format.dateTime(date, {
        day: 'numeric',
        month: 'short',
        year: 'numeric'
      })}
    </time>
  );
}
```

### Rich Text Pattern

```typescript
// Translation file
{
  "welcomeMessage": "Welcome to <bold>SkillSoft</bold>! Start your <link>first assessment</link>."
}

// Component
<p>
  {t.rich('welcomeMessage', {
    bold: (chunks) => <strong>{chunks}</strong>,
    link: (chunks) => <Link href="/test-templates">{chunks}</Link>
  })}
</p>
```

---

## 7. Dynamic Content Handling

### API Data (Do NOT translate)

Content coming from the backend API should NOT be translated on the frontend:
- Competency names and descriptions
- Test template names and descriptions
- Question text and answer options
- User-generated content

The backend handles bilingual content storage in JSONB fields.

### Enum Display Values

Use the existing `enums` namespace:

```typescript
// en.json
{
  "enums": {
    "assessmentGoal": {
      "JOB_FIT": "Job Fit Assessment",
      "TEAM_FIT": "Team Fit Assessment",
      "OVERVIEW": "General Overview"
    }
  }
}

// Component
const t = useTranslations('enums');
<Badge>{t(`assessmentGoal.${template.goal}`)}</Badge>
```

### Status Values

```typescript
// en.json
{
  "status": {
    "completed": "Completed",
    "inProgress": "In Progress",
    "notStarted": "Not Started",
    "abandoned": "Abandoned",
    "timedOut": "Timed Out"
  }
}

// Component
const t = useTranslations('status');
<Badge>{t(session.status.toLowerCase())}</Badge>
```

---

## 8. Edge Cases and Special Considerations

### 8.1 RTL Readiness

Although current locales are LTR, prepare for future RTL support:

```css
/* Use logical properties */
.card {
  margin-inline-start: 1rem;  /* Not margin-left */
  padding-inline-end: 1rem;   /* Not padding-right */
}
```

### 8.2 Text Expansion

Russian text is typically 20-30% longer than English. Test UI with Russian to ensure:
- Buttons don't overflow
- Cards maintain proportions
- Tables handle longer headers

### 8.3 Number Formatting

Use `Intl.NumberFormat` or `useFormatter`:

```typescript
const format = useFormatter();

// Percentages
format.number(0.75, { style: 'percent' }); // "75%" or "75 %"

// Durations
format.relativeTime(-3, 'day'); // "3 days ago" or "3 дня назад"
```

### 8.4 Timezone Handling

The i18n config sets default timezone to 'Europe/Moscow'. Ensure date displays are consistent:

```typescript
// src/i18n/request.ts
return {
  locale,
  messages,
  timeZone: 'Europe/Moscow',
  now: new Date(),
};
```

### 8.5 Error Boundary Fallbacks

Ensure error boundaries have translated messages:

```typescript
'use client';

export default function Error({ error, reset }) {
  const t = useTranslations('errors');

  return (
    <div>
      <h2>{t('somethingWentWrong')}</h2>
      <Button onClick={reset}>{t('tryAgain')}</Button>
    </div>
  );
}
```

### 8.6 Loading States

Use translated loading messages:

```typescript
// loading.tsx
export default function Loading() {
  // Note: loading.tsx is a Server Component by default
  // Cannot use hooks, use static text or pass from parent
  return <Skeleton aria-label="Loading content..." />;
}
```

For client-side loading with translations:
```typescript
const t = useTranslations('common');
<Spinner label={t('loading')} />
```

---

## 9. Testing Strategy

### 9.1 Unit Tests

**Translation Key Existence:**
```typescript
// src/__tests__/i18n/test-templates-keys.test.ts
import { describe, it, expect } from 'vitest';
import en from '../../../messages/en.json';
import ru from '../../../messages/ru.json';

describe('Test Templates Translation Keys', () => {
  const requiredKeys = [
    'template.history.title',
    'template.history.tabs.results',
    'template.take.errors.notFound',
    // ... all required keys
  ];

  requiredKeys.forEach(key => {
    it(`has key "${key}" in English`, () => {
      expect(getNestedValue(en, key)).toBeDefined();
    });

    it(`has key "${key}" in Russian`, () => {
      expect(getNestedValue(ru, key)).toBeDefined();
    });
  });
});
```

### 9.2 Component Tests

```typescript
// src/__tests__/components/test-templates/HistoryPage.test.tsx
import { render, screen } from '@testing-library/react';
import { NextIntlClientProvider } from 'next-intl';
import en from '../../../../messages/en.json';

function renderWithIntl(component: React.ReactNode, locale = 'en') {
  const messages = locale === 'en' ? en : ru;
  return render(
    <NextIntlClientProvider locale={locale} messages={messages}>
      {component}
    </NextIntlClientProvider>
  );
}

describe('History Page', () => {
  it('displays translated title in English', () => {
    renderWithIntl(<HistoryPageClient />, 'en');
    expect(screen.getByText('My Results')).toBeInTheDocument();
  });

  it('displays translated title in Russian', () => {
    renderWithIntl(<HistoryPageClient />, 'ru');
    expect(screen.getByText('Мои результаты')).toBeInTheDocument();
  });
});
```

### 9.3 Integration Tests

```typescript
// cypress/e2e/i18n/test-templates.cy.ts
describe('Test Templates i18n', () => {
  it('switches language via cookie', () => {
    cy.setCookie('NEXT_LOCALE', 'en');
    cy.visit('/test-templates/history');
    cy.contains('My Results').should('exist');

    cy.setCookie('NEXT_LOCALE', 'ru');
    cy.reload();
    cy.contains('Мои результаты').should('exist');
  });
});
```

### 9.4 Visual Regression Tests

Use tools like Chromatic or Percy to catch layout issues from text expansion:

```typescript
// storybook/test-templates.stories.tsx
export const HistoryPageEnglish = {
  parameters: { locale: 'en' },
  render: () => <HistoryPage />,
};

export const HistoryPageRussian = {
  parameters: { locale: 'ru' },
  render: () => <HistoryPage />,
};
```

---

## 10. Rollback and Compensation Strategies

### 10.1 Rollback Triggers

Initiate rollback if:
1. Translation coverage tests fail
2. Build fails after migration
3. Critical UI regression detected
4. Performance degradation > 5%

### 10.2 Rollback Procedure

```bash
# 1. Revert component changes
git checkout HEAD~1 -- app/(workspace)/test-templates/

# 2. Revert translation file changes
git checkout HEAD~1 -- messages/en.json messages/ru.json

# 3. Verify build
npm run build

# 4. Run tests
npm run test
```

### 10.3 Compensation Patterns

**Missing Translation Fallback:**
```typescript
// next-intl automatically falls back to key if translation missing
// Additionally, you can use onError handler in NextIntlClientProvider
<NextIntlClientProvider
  onError={(error) => {
    console.error('Translation error:', error);
    // Report to monitoring
  }}
>
```

**Partial Migration:**
If some components fail, isolate them:
```typescript
// Wrap problematic components
function SafeTranslation({ children, fallback }) {
  try {
    return children;
  } catch {
    return fallback;
  }
}
```

### 10.4 Feature Flags

Consider using feature flags for gradual rollout:

```typescript
// lib/feature-flags.ts
export const USE_I18N_TEST_TEMPLATES = process.env.NEXT_PUBLIC_I18N_TEST_TEMPLATES === 'true';

// Component
const t = USE_I18N_TEST_TEMPLATES
  ? useTranslations('template')
  : { title: 'Hardcoded Title' }; // Fallback
```

---

## 11. Success Metrics

### 11.1 Quantitative Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Translation coverage | 100% | `npm run test -- translation-coverage` |
| Missing key errors | 0 | Browser console monitoring |
| Build time increase | < 5% | CI pipeline timing |
| Bundle size increase | < 2% | `npm run analyze` |
| Test pass rate | 100% | CI test results |

### 11.2 Qualitative Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| UI consistency | No layout breaks | Manual QA both locales |
| Text truncation | None | Visual inspection |
| Date/number formats | Locale-appropriate | Manual verification |
| Error messages | Helpful and translated | User feedback |

### 11.3 Definition of Done

A file is considered fully migrated when:
1. No hardcoded user-facing strings remain
2. All translations exist in both `en.json` and `ru.json`
3. Unit tests pass
4. Manual testing in both locales complete
5. Code review approved
6. No console warnings for missing translations

---

## Appendix A: File-by-File Change Summary

| File | Changes Required |
|------|-----------------|
| `history/page.tsx` | Add `getTranslations`, replace ~40 strings |
| `take/[sessionId]/page.tsx` | Add `useTranslations`, replace ~25 strings |
| `take/[sessionId]/error.tsx` | Add `useTranslations`, replace ~5 strings |
| `error.tsx` | Add `useTranslations`, replace ~5 strings |
| `_components/ErrorDisplay.tsx` | Add `useTranslations`, replace ~10 strings |
| `_components/StartTestSessionButton.tsx` | Add `useTranslations`, replace ~3 strings |
| `_components/StartTestDriveButton.tsx` | Add `useTranslations`, replace ~3 strings |
| `_components/TemplateFilters.tsx` | Add `useTranslations`, replace ~5 strings |
| `_components/TestTemplatesGrid.tsx` | Add `useTranslations`, replace ~3 strings |
| `results/[resultId]/_components/shared/*.tsx` | Add translations, ~15 strings total |
| `[id]/start/StartAssessmentClient.tsx` | Add `useTranslations`, replace ~10 strings |
| `loading.tsx` | Add aria-label translation |
| `page.tsx` | Add error message translations |

**Total estimated changes**: ~150 string replacements across 19 files

---

## Appendix B: Translation File Diff Estimate

**en.json additions**: ~200 new keys
**ru.json additions**: ~200 new keys

Estimated file size increase: ~15KB per file

---

## Appendix C: Timeline

| Phase | Duration | Dependencies |
|-------|----------|--------------|
| Phase 1: Preparation | 1 day | None |
| Phase 2: Migration | 3 days | Phase 1 complete |
| Phase 3: Testing | 1 day | Phase 2 complete |
| Phase 4: Validation | 1 day | Phase 3 complete |
| **Total** | **6 days** | - |

---

## Document Metadata

- **Created**: 2026-01-02
- **Author**: Claude (Principal Architect)
- **Version**: 1.0
- **Status**: Ready for Implementation
- **Review Required**: Yes

---

## References

- [next-intl Documentation](https://next-intl.dev/docs)
- [Next.js 16 Internationalization Guide](https://nextjs.org/docs/app/guides/internationalization)
- [ICU Message Format](https://unicode-org.github.io/icu/userguide/format_parse/messages/)
- Project CLAUDE.md for coding standards
