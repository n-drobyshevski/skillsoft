# Workflow: i18n Improvement & Help Tooltips for Settings Page

## Overview

This document outlines the implementation workflow for adding internationalized help tooltips to the test-templates settings page scenario configuration panels.

**Target Components:**
- `OverviewConfigPanel.tsx` - OVERVIEW goal (Competency Passport)
- `JobFitConfigPanel.tsx` - JOB_FIT goal (O*NET Benchmarking)
- `TeamFitConfigPanel.tsx` - TEAM_FIT goal (Team Skill Analysis)

**Existing Infrastructure:**
- `HelpTooltip` component: `src/components/ui/help-tooltip.tsx`
- Translation hook: `useHelpTranslation()` in `src/hooks/useHelpTranslation.ts`
- Translation files: `messages/en.json`, `messages/ru.json`

---

## Design Decisions

### 1. Translation Key Organization

**Decision:** Extend the root-level `help` namespace with `scenario` sub-object.

**Rationale:**
- `useHelpTranslation` hook already uses `useTranslations('help')`
- Centralizes all contextual help content
- Avoids deep nesting in `template.settings.*`
- Clean separation of concerns

**Structure:**
```
help.scenario.{panel}.{element}.{detail}
```

### 2. Implementation Order

**Order:** Translations FIRST, then components.

1. `messages/en.json` - Add `help.scenario.*` keys
2. `messages/ru.json` - Mirror structure with Russian translations
3. `src/hooks/useHelpTranslation.ts` - Extend types (optional but recommended)
4. `OverviewConfigPanel.tsx` - Integrate tooltips
5. `JobFitConfigPanel.tsx` - Integrate tooltips
6. `TeamFitConfigPanel.tsx` - Integrate tooltips

### 3. Big Five Traits Pattern

**Decision:** Individual keys with consistent naming convention.

**Rationale:**
- Each trait requires distinct explanation
- Russian translations have different sentence structures
- Individual keys allow targeted updates
- Maps cleanly to component data

### 4. Namespace Decision

**Decision:** Use `help.scenario.*` (extend existing `help` namespace).

**Alternative Rejected:** `template.settings.help.*` - would create deep nesting and break `useHelpTranslation` hook pattern.

---

## Phase 1: Translation Keys (Estimated: 30 min)

### 1.1 English Translations (`messages/en.json`)

Add to existing `"help"` object (approximately line 3695):

```json
"scenario": {
  "overview": {
    "bigFive": {
      "title": "Big Five Personality Model",
      "description": "The OCEAN model assesses five core personality dimensions that predict workplace behavior and team dynamics.",
      "enable": "Include personality profiling in assessment results to provide richer candidate insights.",
      "traits": {
        "openness": "Openness to Experience: Creativity, curiosity, intellectual interests, and willingness to embrace new ideas and unconventional approaches.",
        "conscientiousness": "Conscientiousness: Self-discipline, organization, goal-directed behavior, and reliability in following through on commitments.",
        "extraversion": "Extraversion: Sociability, assertiveness, positive emotions, and tendency to seek stimulation in the company of others.",
        "agreeableness": "Agreeableness: Cooperation, empathy, trust, and orientation toward helping others and maintaining harmony.",
        "emotionalStability": "Emotional Stability: Calmness under pressure, resilience to stress, and consistent emotional responses."
      }
    },
    "difficulty": {
      "title": "Question Difficulty Level",
      "description": "Controls the complexity of situational judgment scenarios presented to candidates.",
      "basic": "Entry-level questions suitable for early-career candidates or foundational skill assessment.",
      "intermediate": "Standard complexity appropriate for most professional roles and mid-career candidates.",
      "advanced": "Senior-level scenarios requiring strategic thinking, complex problem-solving, and leadership judgment."
    },
    "estimation": {
      "questions": "Question count is estimated based on: selected competencies x average indicators per competency x questions per indicator.",
      "duration": "Duration is calculated at approximately 30 seconds per question, including reading and response time."
    }
  },
  "jobFit": {
    "onetRole": "O*NET (Occupational Information Network) occupation codes define standardized job competency requirements based on extensive labor market research.",
    "benchmark": "The benchmark shows competencies and proficiency levels required for the selected job role, as defined by O*NET occupational data.",
    "strictness": {
      "title": "Benchmark Matching Strictness",
      "description": "Controls how closely a candidate must match the job benchmark to be considered a good fit.",
      "lenient": "Broader candidate pool: Accepts partial competency matches, suitable for entry-level hiring or talent development programs.",
      "moderate": "Balanced approach: Reasonable match requirements suitable for most standard hiring needs.",
      "standard": "Industry-typical requirements: Matches common hiring standards for professional roles.",
      "strict": "High bar: Only strong matches qualify, suitable for specialized or senior positions.",
      "exact": "Precise alignment: Requires near-perfect benchmark match, for critical roles with no room for skill gaps."
    },
    "deltaTesting": "Delta testing identifies and focuses only on the gaps between a candidate's existing competency passport and job requirements, reducing assessment length."
  },
  "teamFit": {
    "team": "Select the team to analyze for current skill coverage and identify competency gaps that new candidates should fill.",
    "saturation": {
      "title": "Skill Saturation Threshold",
      "description": "The minimum team coverage level for a skill before it is flagged as a gap.",
      "threshold": "Skills with team coverage below this percentage are considered undersaturated and will be prioritized in candidate assessment."
    },
    "gapStatus": {
      "full": "Skill fully covered: 90%+ team saturation indicates strong existing capability.",
      "adequate": "Skill adequately covered: Above threshold, no immediate hiring need for this competency.",
      "gap": "Skill gap detected: Below threshold, candidates with this skill would strengthen the team.",
      "critical": "Critical gap: Significantly below threshold, high priority for new hire skill requirements."
    },
    "assessment": "Team Fit assessment focuses on undersaturated competencies, evaluating candidates on their ability to fill specific team skill gaps."
  }
}
```

### 1.2 Russian Translations (`messages/ru.json`)

Add to existing `"help"` object (approximately line 3695):

```json
"scenario": {
  "overview": {
    "bigFive": {
      "title": "Модель личности Big Five",
      "description": "Модель OCEAN оценивает пять ключевых измерений личности, которые предсказывают рабочее поведение и командную динамику.",
      "enable": "Включите профилирование личности в результаты оценки для более полного понимания кандидатов.",
      "traits": {
        "openness": "Открытость опыту: Креативность, любознательность, интеллектуальные интересы и готовность принимать новые идеи и нестандартные подходы.",
        "conscientiousness": "Добросовестность: Самодисциплина, организованность, целеустремленность и надежность в выполнении обязательств.",
        "extraversion": "Экстраверсия: Общительность, уверенность, позитивные эмоции и склонность искать стимуляцию в компании других.",
        "agreeableness": "Доброжелательность: Сотрудничество, эмпатия, доверие и ориентация на помощь другим и поддержание гармонии.",
        "emotionalStability": "Эмоциональная стабильность: Спокойствие под давлением, устойчивость к стрессу и последовательные эмоциональные реакции."
      }
    },
    "difficulty": {
      "title": "Уровень сложности вопросов",
      "description": "Определяет сложность ситуационных сценариев, представляемых кандидатам.",
      "basic": "Базовые вопросы для начинающих специалистов или оценки фундаментальных навыков.",
      "intermediate": "Стандартная сложность для большинства профессиональных ролей и специалистов среднего уровня.",
      "advanced": "Продвинутые сценарии, требующие стратегического мышления, решения сложных проблем и лидерских решений."
    },
    "estimation": {
      "questions": "Количество вопросов рассчитывается: выбранные компетенции x среднее число индикаторов x вопросов на индикатор.",
      "duration": "Длительность рассчитывается примерно 30 секунд на вопрос, включая чтение и ответ."
    }
  },
  "jobFit": {
    "onetRole": "Коды профессий O*NET определяют стандартизированные требования к компетенциям на основе обширных исследований рынка труда.",
    "benchmark": "Эталон показывает компетенции и требуемые уровни владения для выбранной должности согласно данным O*NET.",
    "strictness": {
      "title": "Строгость соответствия эталону",
      "description": "Определяет, насколько точно кандидат должен соответствовать эталону должности.",
      "lenient": "Широкий отбор: Принимает частичное соответствие, подходит для начальных позиций или программ развития талантов.",
      "moderate": "Сбалансированный подход: Разумные требования для большинства стандартных вакансий.",
      "standard": "Типичные требования отрасли: Соответствует общим стандартам найма для профессиональных ролей.",
      "strict": "Высокая планка: Только сильные кандидаты, подходит для специализированных или старших позиций.",
      "exact": "Точное соответствие: Требуется практически идеальное совпадение с эталоном для критически важных ролей."
    },
    "deltaTesting": "Дельта-тестирование выявляет только пробелы между паспортом компетенций кандидата и требованиями должности, сокращая время оценки."
  },
  "teamFit": {
    "team": "Выберите команду для анализа текущего покрытия навыков и выявления пробелов в компетенциях.",
    "saturation": {
      "title": "Порог насыщенности навыков",
      "description": "Минимальный уровень покрытия навыка в команде, ниже которого он считается пробелом.",
      "threshold": "Навыки с покрытием ниже этого процента считаются недостаточно насыщенными и будут приоритетными при оценке кандидатов."
    },
    "gapStatus": {
      "full": "Навык полностью покрыт: 90%+ насыщенность команды указывает на сильную существующую способность.",
      "adequate": "Навык достаточно покрыт: Выше порога, нет срочной потребности в найме по этой компетенции.",
      "gap": "Обнаружен пробел: Ниже порога, кандидаты с этим навыком усилят команду.",
      "critical": "Критический пробел: Значительно ниже порога, высокий приоритет для требований к новому сотруднику."
    },
    "assessment": "Оценка Team Fit фокусируется на недостаточно насыщенных компетенциях, оценивая способность кандидатов заполнить конкретные пробелы в навыках команды."
  }
}
```

---

## Phase 2: Hook Enhancement (Estimated: 15 min)

### 2.1 Extend Type Definitions (`src/hooks/useHelpTranslation.ts`)

```typescript
// Add 'scenario' to existing categories
type HelpCategory = 'competency' | 'indicator' | 'question' | 'scenario';

// Add scenario-specific key types
type ScenarioHelpKey =
  | 'overview.bigFive.title'
  | 'overview.bigFive.description'
  | 'overview.bigFive.enable'
  | `overview.bigFive.traits.${'openness' | 'conscientiousness' | 'extraversion' | 'agreeableness' | 'emotionalStability'}`
  | 'overview.difficulty.title'
  | 'overview.difficulty.description'
  | `overview.difficulty.${'basic' | 'intermediate' | 'advanced'}`
  | 'overview.estimation.questions'
  | 'overview.estimation.duration'
  | 'jobFit.onetRole'
  | 'jobFit.benchmark'
  | 'jobFit.deltaTesting'
  | 'jobFit.strictness.title'
  | 'jobFit.strictness.description'
  | `jobFit.strictness.${'lenient' | 'moderate' | 'standard' | 'strict' | 'exact'}`
  | 'teamFit.team'
  | 'teamFit.saturation.title'
  | 'teamFit.saturation.description'
  | 'teamFit.saturation.threshold'
  | `teamFit.gapStatus.${'full' | 'adequate' | 'gap' | 'critical'}`
  | 'teamFit.assessment';

// Update HelpKeyMap
type HelpKeyMap = {
  competency: CompetencyHelpKey;
  indicator: IndicatorHelpKey;
  question: QuestionHelpKey;
  scenario: ScenarioHelpKey;
};
```

**Alternative (Simpler):** Skip type extension and use `useTranslations('help')` directly in components with template literal keys.

---

## Phase 3: Component Integration (Estimated: 45 min per panel)

### 3.1 OverviewConfigPanel.tsx

**File:** `src/components/blueprint-config/OverviewConfigPanel.tsx`

**Changes Required:**

1. **Add Imports:**
```typescript
import { useTranslations } from 'next-intl';
import { HelpTooltip } from '@/components/ui/help-tooltip';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';
```

2. **Add Translation Hook:**
```typescript
const t = useTranslations('help.scenario.overview');
```

3. **Big Five Card Header - Add Tooltip:**
```tsx
<CardTitle className="text-base">
  Big Five Personality
  <HelpTooltip
    content={t('bigFive.description')}
    variant="info"
    size="sm"
  />
</CardTitle>
```

4. **Big Five Trait Cards - Replace title with Tooltip:**
```tsx
{BIG_FIVE_TRAITS.map((trait) => {
  const Icon = trait.icon;
  const traitKey = trait.key.toLowerCase() as
    'openness' | 'conscientiousness' | 'extraversion' | 'agreeableness' | 'emotionalstability';

  return (
    <TooltipProvider key={trait.key}>
      <Tooltip>
        <TooltipTrigger asChild>
          <div className={cn('flex flex-col items-center p-2 rounded-lg text-center cursor-help', trait.bg)}>
            <Icon className={cn('h-4 w-4 mb-1', trait.color)} />
            <span className="text-[10px] font-medium text-muted-foreground">
              {trait.name}
            </span>
          </div>
        </TooltipTrigger>
        <TooltipContent side="bottom" className="max-w-xs text-xs">
          {t(`bigFive.traits.${traitKey === 'emotionalstability' ? 'emotionalStability' : traitKey}`)}
        </TooltipContent>
      </Tooltip>
    </TooltipProvider>
  );
})}
```

5. **Difficulty Label - Add Tooltip:**
```tsx
<FormLabel className="flex items-center gap-2">
  <BarChart3 className="h-4 w-4 text-muted-foreground" />
  Preferred Difficulty
  <HelpTooltip content={t('difficulty.description')} variant="help" />
</FormLabel>
```

6. **Estimation Card - Add Tooltip:**
```tsx
<div className="flex items-center gap-2 text-sm">
  <Target className="h-4 w-4 text-primary" />
  <span>{selectedCompetencyCount} competencies selected</span>
  <HelpTooltip content={t('estimation.questions')} variant="info" size="sm" />
</div>
```

---

### 3.2 JobFitConfigPanel.tsx

**File:** `src/components/blueprint-config/JobFitConfigPanel.tsx`

**Changes Required:**

1. **Add Imports:**
```typescript
import { useTranslations } from 'next-intl';
import { HelpTooltip } from '@/components/ui/help-tooltip';
```

2. **Add Translation Hook:**
```typescript
const t = useTranslations('help.scenario.jobFit');
const tLabels = useTranslations('template.settings'); // For UI labels
```

3. **Refactor STRICTNESS_LABELS to Use Translations:**

Move from static constant to useMemo within component:
```typescript
const STRICTNESS_LABELS = useMemo(() => ({
  20: {
    label: tLabels('strictness.lenient', { defaultValue: 'Lenient' }),
    description: t('strictness.lenient'),
    color: 'text-green-600'
  },
  40: {
    label: tLabels('strictness.moderate', { defaultValue: 'Moderate' }),
    description: t('strictness.moderate'),
    color: 'text-teal-600'
  },
  60: {
    label: tLabels('strictness.standard', { defaultValue: 'Standard' }),
    description: t('strictness.standard'),
    color: 'text-blue-600'
  },
  80: {
    label: tLabels('strictness.strict', { defaultValue: 'Strict' }),
    description: t('strictness.strict'),
    color: 'text-orange-600'
  },
  100: {
    label: tLabels('strictness.exact', { defaultValue: 'Exact' }),
    description: t('strictness.exact'),
    color: 'text-red-600'
  },
}), [t, tLabels]);
```

4. **Target Job Role Label - Add Tooltip:**
```tsx
<FormLabel className="flex items-center gap-2">
  <Briefcase className="h-4 w-4 text-blue-500" />
  Target Job Role
  <HelpTooltip content={t('onetRole')} variant="info" />
</FormLabel>
```

5. **Strictness Level Label - Add Tooltip:**
```tsx
<FormLabel className="flex items-center gap-2">
  <SlidersHorizontal className="h-4 w-4 text-muted-foreground" />
  Strictness Level
  <HelpTooltip content={t('strictness.description')} variant="help" />
</FormLabel>
```

6. **Benchmark Preview Header - Add Tooltip:**
```tsx
<CardTitle className="text-sm">
  Benchmark Preview
  <HelpTooltip content={t('benchmark')} variant="info" size="sm" />
</CardTitle>
```

---

### 3.3 TeamFitConfigPanel.tsx

**File:** `src/components/blueprint-config/TeamFitConfigPanel.tsx`

**Changes Required:**

1. **Add Imports:**
```typescript
import { useTranslations } from 'next-intl';
import { HelpTooltip } from '@/components/ui/help-tooltip';
```

2. **Add Translation Hook:**
```typescript
const t = useTranslations('help.scenario.teamFit');
```

3. **Target Team Label - Add Tooltip:**
```tsx
<FormLabel className="flex items-center gap-2">
  <Users className="h-4 w-4 text-purple-500" />
  Target Team
  <HelpTooltip content={t('team')} variant="help" />
</FormLabel>
```

4. **Gap Threshold Label - Add Tooltip:**
```tsx
<FormLabel className="flex items-center gap-2">
  <BarChart3 className="h-4 w-4 text-muted-foreground" />
  Gap Threshold
  <HelpTooltip content={t('saturation.description')} variant="help" />
</FormLabel>
```

5. **Saturation Status Badges - Add Tooltips:**
Replace static badge with tooltip-wrapped version:
```tsx
<TooltipProvider>
  <Tooltip>
    <TooltipTrigger asChild>
      <Badge
        variant={getSaturationBadgeVariant(status)}
        className="h-4 text-[9px] px-1.5 cursor-help"
      >
        {status === 'critical' ? 'Critical' : 'Gap'}
      </Badge>
    </TooltipTrigger>
    <TooltipContent className="max-w-xs text-xs">
      {t(`gapStatus.${status}`)}
    </TooltipContent>
  </Tooltip>
</TooltipProvider>
```

6. **Team Fit Info Card - Add Help Icon:**
```tsx
<p className="text-sm font-medium">
  Team Fit Assessment
  <HelpTooltip content={t('assessment')} variant="tip" />
</p>
```

---

## Phase 4: Testing & Verification (Estimated: 30 min)

### 4.1 Static Analysis Checklist
- [ ] TypeScript compilation passes (`npm run type-check`)
- [ ] ESLint passes (`npm run lint`)
- [ ] No missing translation key warnings in console

### 4.2 Visual Testing Checklist
- [ ] **EN Locale:** All tooltips display English text correctly
- [ ] **RU Locale:** All tooltips display Russian text correctly
- [ ] **Dark Mode:** Tooltips readable with proper contrast
- [ ] **Mobile (375px):** Touch-friendly tooltip triggers, no overflow

### 4.3 Accessibility Testing Checklist
- [ ] **Keyboard Navigation:** Tab through all tooltip triggers
- [ ] **Focus Indicators:** Visible focus ring on tooltip triggers
- [ ] **Screen Reader:** sr-only "Help" label is announced
- [ ] **Touch Devices:** Long-press activates tooltips on mobile

### 4.4 Integration Testing Checklist
- [ ] **Locale Switch:** Tooltips update when changing language
- [ ] **Form Validation:** Adding tooltips doesn't break form behavior
- [ ] **Console Errors:** No React warnings or translation errors
- [ ] **Hydration:** No mismatch between server and client render

---

## Error Handling Considerations

### 1. Missing Translation Keys
The `useHelpTranslation` hook includes try/catch with fallback to the key path:
```typescript
try {
  return t(`${category}.${key}`);
} catch {
  return `${category}.${key}`;
}
```

### 2. Component Loading
Translations are loaded synchronously with the page via `next-intl`'s server component support. No loading states needed.

### 3. Hydration Safety
Avoid conditional translations based on client-only state (e.g., `window.innerWidth`). All translation keys should be deterministic at SSR time.

### 4. Type Safety
If extending `useHelpTranslation` types, ensure all new keys are included in the union type to prevent typos.

---

## Summary

| Phase | Duration | Files Modified |
|-------|----------|----------------|
| 1. Translations | 30 min | `en.json`, `ru.json` |
| 2. Hook Extension | 15 min | `useHelpTranslation.ts` (optional) |
| 3a. Overview Panel | 45 min | `OverviewConfigPanel.tsx` |
| 3b. JobFit Panel | 45 min | `JobFitConfigPanel.tsx` |
| 3c. TeamFit Panel | 45 min | `TeamFitConfigPanel.tsx` |
| 4. Testing | 30 min | N/A |
| **Total** | **~3.5 hours** | **5-6 files** |

---

## Appendix: Component Pattern Reference

### Pattern A: Inline HelpTooltip (Most Common)
```tsx
<FormLabel>
  Field Name
  <HelpTooltip content={t('path.to.help')} variant="help" />
</FormLabel>
```

### Pattern B: Wrapped Element with Tooltip
```tsx
<TooltipProvider>
  <Tooltip>
    <TooltipTrigger asChild>
      <div className="cursor-help">Content</div>
    </TooltipTrigger>
    <TooltipContent>{t('path.to.help')}</TooltipContent>
  </Tooltip>
</TooltipProvider>
```

### Pattern C: FieldLabel Compound Component
```tsx
import { FieldLabel } from '@/components/ui/help-tooltip';

<FieldLabel
  label="Field Name"
  help={t('path.to.help')}
  required={true}
/>
```

---

**Last Updated:** 2025-01-12
**Author:** Principal Architect
**Status:** Ready for Implementation
