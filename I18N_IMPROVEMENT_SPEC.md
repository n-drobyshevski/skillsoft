# SkillSoft i18n Improvement Specification

## Document Version
- **Version:** 1.0
- **Date:** 2026-01-01
- **Status:** Ready for Implementation

---

## Executive Summary

This specification outlines the remaining work needed to complete the internationalization (i18n) implementation for SkillSoft's frontend application. The current implementation covers ~70% of UI strings. This document details the gaps, prioritized phases, and technical approaches for achieving 100% coverage.

---

## Current State Assessment

### Infrastructure (Complete)
| Component | Status | Location |
|-----------|--------|----------|
| next-intl configuration | ✅ Done | `next.config.ts` |
| NextIntlClientProvider | ✅ Done | `app/layout.tsx` |
| Server-side locale detection | ✅ Done | `src/i18n/request.ts` |
| Language store (Zustand) | ✅ Done | `src/stores/language-store.ts` |
| Language switcher UI | ✅ Done | `src/components/language-switcher.tsx` |
| Translation files | ✅ Done | `messages/en.json`, `messages/ru.json` |

### Migrated Components (~68 files)
- [x] Global layout (sidebar, header, mobile nav)
- [x] Dashboard widgets
- [x] Test player components
- [x] Error handling components
- [x] HR list pages (competencies, indicators, questions)
- [x] my-tests pages
- [x] test-results pages
- [x] Partial form migrations (toast messages)

### Translation Coverage
- **English:** ~1,100 keys
- **Russian:** ~1,100 keys
- **Namespaces:** common, navigation, dashboard, competency, indicator, question, template, assessment, likert, frequency, errors, forms, myTests, results, table, status

---

## Gap Analysis

### Gap 1: Zod Schema Validation Messages (Critical)

**Problem:** Validation schemas use hardcoded English error messages.

**Current Code Example:**
```typescript
// competencies/validation.ts
name: z.string().min(2, 'Name must be at least 2 characters')
description: z.string().min(50, 'Description must be at least 50 characters')

// indicators/validation.ts
title: z.string().min(5, 'Title must be at least 5 characters')
weight: z.number().min(0.01, 'Weight must be at least 0.01')
```

**Impact:** Form validation errors display in English regardless of user's locale setting.

**Affected Files:**
- `app/(workspace)/hr/competencies/validation.ts`
- `app/(workspace)/hr/behavioral-indicators/validation.ts`
- `app/(workspace)/hr/assessment-questions/validation.ts`
- `app/(workspace)/test-templates/[id]/settings/validation.ts`

---

### Gap 2: Enum Display Values (Critical)

**Problem:** Enum values rendered as raw strings or simple replace transforms.

**Current Code Example:**
```typescript
// CompetencyForm.tsx
{Object.values(CompetencyCategory).map((category) => (
  <SelectItem key={category} value={category}>
    {category}  // Displays "COGNITIVE", "INTERPERSONAL"
  </SelectItem>
))}

// IndicatorForm.tsx
{Object.values(ObservabilityLevel).map((level) => (
  <SelectItem key={level} value={level}>
    {level.replace(/_/g, ' ')}  // "DIRECTLY_OBSERVABLE" -> "DIRECTLY OBSERVABLE"
  </SelectItem>
))}
```

**Affected Enums:**
| Enum | Values Count | Used In |
|------|-------------|---------|
| `CompetencyCategory` | 9 | CompetencyForm |
| `ObservabilityLevel` | 5 | IndicatorForm |
| `ApprovalStatus` | 6 | Multiple forms |
| `IndicatorMeasurementType` | 5 | IndicatorForm |
| `ContextScope` | 4 | IndicatorForm |
| `QuestionType` | 11 | QuestionForm |
| `DifficultyLevel` | 5 | QuestionForm |
| `SessionStatus` | 5 | TestCard, status badges |
| `AssessmentGoal` | 3 | TemplateForm |

---

### Gap 3: Form Field Labels (High Priority)

**Problem:** Form labels, placeholders, and section headers are hardcoded.

**IndicatorForm.tsx Hardcoded Strings:**
- Section headers: "Core Information", "Classification & Metrics", "Contextual Examples", "Status & Approval"
- Field labels: "Title", "Description", "Observability Level", "Measurement Type", "Weight", "Order Index", "Context Scope", "Positive Examples", "Counter Examples", "Approval Status", "Visibility"
- Placeholders: "e.g., Proactive Communication", "Detailed description of what this indicator measures..."
- Button text: "Cancel", "Update Preview", "Saving...", "Creating...", "Save Changes", "Create Indicator"

**QuestionForm.tsx Hardcoded Strings:**
- Similar pattern with question-specific fields

**CompetencyForm.tsx:**
- Partially migrated but needs completion

---

### Gap 4: Help Tooltips (Medium Priority)

**Problem:** `formHelp` object in `help-tooltip.tsx` contains hardcoded English help text.

**Location:** `src/components/ui/help-tooltip.tsx`

---

### Gap 5: Page Metadata (Low Priority)

**Problem:** Next.js `metadata` exports use hardcoded strings.

**Example:**
```typescript
export const metadata: Metadata = {
  title: "Behavioral Indicators - SkillSoft",  // Hardcoded
  description: "Define and manage measurable behavioral indicators..."
};
```

---

## Implementation Phases

### Phase 1: Enum Translations
**Priority:** 🔴 Critical
**Estimated Time:** 2-3 hours
**Dependencies:** None

#### Step 1.1: Add Enum Namespace to Translation Files

**File:** `messages/en.json`
```json
{
  "enums": {
    "competencyCategory": {
      "COGNITIVE": "Cognitive",
      "INTERPERSONAL": "Interpersonal",
      "LEADERSHIP": "Leadership",
      "ADAPTABILITY": "Adaptability",
      "EMOTIONAL_INTELLIGENCE": "Emotional Intelligence",
      "COMMUNICATION": "Communication",
      "COLLABORATION": "Collaboration",
      "CRITICAL_THINKING": "Critical Thinking",
      "TIME_MANAGEMENT": "Time Management"
    },
    "observabilityLevel": {
      "DIRECTLY_OBSERVABLE": "Directly Observable",
      "PARTIALLY_OBSERVABLE": "Partially Observable",
      "INFERRED": "Inferred",
      "SELF_REPORTED": "Self-Reported",
      "REQUIRES_DOCUMENTATION": "Requires Documentation"
    },
    "approvalStatus": {
      "DRAFT": "Draft",
      "PENDING_REVIEW": "Pending Review",
      "APPROVED": "Approved",
      "REJECTED": "Rejected",
      "ARCHIVED": "Archived",
      "UNDER_REVISION": "Under Revision"
    },
    "contextScope": {
      "UNIVERSAL": "Universal",
      "UNIVERSAL_DESC": "Context-neutral (Active Listening, Emotional Regulation)",
      "PROFESSIONAL": "Professional",
      "PROFESSIONAL_DESC": "White-collar environments (Email Etiquette, Meeting Facilitation)",
      "TECHNICAL": "Technical",
      "TECHNICAL_DESC": "IT, Engineering, Data (Code Review, Technical Documentation)",
      "MANAGERIAL": "Managerial",
      "MANAGERIAL_DESC": "People management (Delegation, Performance Feedback)"
    },
    "measurementType": {
      "FREQUENCY": "Frequency",
      "QUALITY": "Quality",
      "IMPACT": "Impact",
      "CONSISTENCY": "Consistency",
      "IMPROVEMENT": "Improvement"
    },
    "difficultyLevel": {
      "FOUNDATIONAL": "Foundational",
      "INTERMEDIATE": "Intermediate",
      "ADVANCED": "Advanced",
      "EXPERT": "Expert",
      "SPECIALIZED": "Specialized"
    },
    "questionType": {
      "LIKERT": "Likert Scale",
      "LIKERT_AGREEMENT": "Likert (Agreement)",
      "LIKERT_FREQUENCY": "Likert (Frequency)",
      "SJT": "Situational Judgment",
      "SJT_RANKING": "SJT (Ranking)",
      "SJT_RATING": "SJT (Rating)",
      "SJT_BEST_WORST": "SJT (Best/Worst)",
      "MULTIPLE_CHOICE": "Multiple Choice",
      "TRUE_FALSE": "True/False",
      "OPEN_ENDED": "Open Ended",
      "FORCED_CHOICE": "Forced Choice"
    },
    "sessionStatus": {
      "NOT_STARTED": "Not Started",
      "IN_PROGRESS": "In Progress",
      "COMPLETED": "Completed",
      "ABANDONED": "Abandoned",
      "TIMED_OUT": "Timed Out"
    },
    "assessmentGoal": {
      "OVERVIEW": "Universal Baseline",
      "OVERVIEW_DESC": "Generates Competency Passport with Big Five personality profile",
      "JOB_FIT": "Job Fit Assessment",
      "JOB_FIT_DESC": "Compares candidate against O*NET occupation benchmarks",
      "TEAM_FIT": "Team Fit Analysis",
      "TEAM_FIT_DESC": "Analyzes skill gaps and personality fit within a team"
    }
  }
}
```

**File:** `messages/ru.json`
```json
{
  "enums": {
    "competencyCategory": {
      "COGNITIVE": "Когнитивные",
      "INTERPERSONAL": "Межличностные",
      "LEADERSHIP": "Лидерство",
      "ADAPTABILITY": "Адаптивность",
      "EMOTIONAL_INTELLIGENCE": "Эмоциональный интеллект",
      "COMMUNICATION": "Коммуникация",
      "COLLABORATION": "Сотрудничество",
      "CRITICAL_THINKING": "Критическое мышление",
      "TIME_MANAGEMENT": "Управление временем"
    },
    "observabilityLevel": {
      "DIRECTLY_OBSERVABLE": "Напрямую наблюдаемый",
      "PARTIALLY_OBSERVABLE": "Частично наблюдаемый",
      "INFERRED": "Выводимый",
      "SELF_REPORTED": "Самооценка",
      "REQUIRES_DOCUMENTATION": "Требует документации"
    },
    "approvalStatus": {
      "DRAFT": "Черновик",
      "PENDING_REVIEW": "На рассмотрении",
      "APPROVED": "Утверждён",
      "REJECTED": "Отклонён",
      "ARCHIVED": "В архиве",
      "UNDER_REVISION": "На доработке"
    },
    "contextScope": {
      "UNIVERSAL": "Универсальный",
      "UNIVERSAL_DESC": "Контекстно-нейтральный (Активное слушание, Эмоциональная регуляция)",
      "PROFESSIONAL": "Профессиональный",
      "PROFESSIONAL_DESC": "Офисная среда (Деловой этикет, Проведение совещаний)",
      "TECHNICAL": "Технический",
      "TECHNICAL_DESC": "IT, Инженерия, Данные (Код-ревью, Техническая документация)",
      "MANAGERIAL": "Управленческий",
      "MANAGERIAL_DESC": "Управление людьми (Делегирование, Обратная связь)"
    },
    "measurementType": {
      "FREQUENCY": "Частота",
      "QUALITY": "Качество",
      "IMPACT": "Влияние",
      "CONSISTENCY": "Постоянство",
      "IMPROVEMENT": "Улучшение"
    },
    "difficultyLevel": {
      "FOUNDATIONAL": "Базовый",
      "INTERMEDIATE": "Средний",
      "ADVANCED": "Продвинутый",
      "EXPERT": "Экспертный",
      "SPECIALIZED": "Специализированный"
    },
    "questionType": {
      "LIKERT": "Шкала Лайкерта",
      "LIKERT_AGREEMENT": "Лайкерт (согласие)",
      "LIKERT_FREQUENCY": "Лайкерт (частота)",
      "SJT": "Ситуационный тест",
      "SJT_RANKING": "СТ (ранжирование)",
      "SJT_RATING": "СТ (оценка)",
      "SJT_BEST_WORST": "СТ (лучший/худший)",
      "MULTIPLE_CHOICE": "Множественный выбор",
      "TRUE_FALSE": "Верно/Неверно",
      "OPEN_ENDED": "Открытый ответ",
      "FORCED_CHOICE": "Принудительный выбор"
    },
    "sessionStatus": {
      "NOT_STARTED": "Не начат",
      "IN_PROGRESS": "В процессе",
      "COMPLETED": "Завершён",
      "ABANDONED": "Прерван",
      "TIMED_OUT": "Время истекло"
    },
    "assessmentGoal": {
      "OVERVIEW": "Универсальная оценка",
      "OVERVIEW_DESC": "Создаёт Паспорт компетенций с профилем Big Five",
      "JOB_FIT": "Оценка соответствия должности",
      "JOB_FIT_DESC": "Сравнивает кандидата с бенчмарками O*NET",
      "TEAM_FIT": "Анализ командной совместимости",
      "TEAM_FIT_DESC": "Анализирует пробелы в навыках и личностную совместимость"
    }
  }
}
```

#### Step 1.2: Create Enum Translation Hook

**File:** `src/hooks/useEnumTranslation.ts`
```typescript
'use client';

import { useTranslations } from 'next-intl';
import { useMemo } from 'react';

type EnumNamespace =
  | 'competencyCategory'
  | 'observabilityLevel'
  | 'approvalStatus'
  | 'contextScope'
  | 'measurementType'
  | 'difficultyLevel'
  | 'questionType'
  | 'sessionStatus'
  | 'assessmentGoal';

interface EnumOption<T extends string = string> {
  value: T;
  label: string;
  description?: string;
}

export function useEnumTranslation<T extends string>(namespace: EnumNamespace) {
  const t = useTranslations(`enums.${namespace}`);

  return useMemo(() => ({
    /**
     * Translate a single enum value
     */
    translate: (value: T): string => {
      try {
        return t(value);
      } catch {
        return value.replace(/_/g, ' ');
      }
    },

    /**
     * Translate enum value with optional description
     */
    translateWithDescription: (value: T): { label: string; description?: string } => {
      try {
        const descKey = `${value}_DESC`;
        return {
          label: t(value),
          description: t.has(descKey) ? t(descKey) : undefined,
        };
      } catch {
        return { label: value.replace(/_/g, ' ') };
      }
    },

    /**
     * Get translated options array for Select components
     */
    getOptions: (enumValues: readonly T[]): EnumOption<T>[] =>
      enumValues.map((value) => {
        try {
          const descKey = `${value}_DESC`;
          return {
            value,
            label: t(value),
            description: t.has(descKey) ? t(descKey) : undefined,
          };
        } catch {
          return {
            value,
            label: value.replace(/_/g, ' '),
          };
        }
      }),
  }), [t]);
}
```

#### Step 1.3: Update Form Select Components

**Example Migration (IndicatorForm.tsx):**
```typescript
// Before
{Object.values(ObservabilityLevel).map((level) => (
  <SelectItem key={level} value={level}>
    {level.replace(/_/g, ' ')}
  </SelectItem>
))}

// After
const { getOptions } = useEnumTranslation<ObservabilityLevel>('observabilityLevel');
const observabilityOptions = getOptions(Object.values(ObservabilityLevel));

{observabilityOptions.map((option) => (
  <SelectItem key={option.value} value={option.value}>
    {option.label}
  </SelectItem>
))}
```

---

### Phase 2: Zod Schema i18n
**Priority:** 🔴 Critical
**Estimated Time:** 3-4 hours
**Dependencies:** None

#### Step 2.1: Add Validation Messages to Translation Files

**File:** `messages/en.json` (add to existing)
```json
{
  "validation": {
    "required": "This field is required",
    "minLength": "Must be at least {min} characters",
    "maxLength": "Must not exceed {max} characters",
    "minValue": "Must be at least {min}",
    "maxValue": "Must not exceed {max}",
    "minItems": "Must have at least {min} items",
    "maxItems": "Must not exceed {max} items",
    "invalidEmail": "Please enter a valid email address",
    "invalidUrl": "Please enter a valid URL",
    "invalidFormat": "Invalid format",
    "selectOption": "Please select an option",
    "invalidNumber": "Please enter a valid number",
    "positiveNumber": "Must be a positive number",
    "integerRequired": "Must be a whole number",
    "weightRange": "Weight must be between {min} and {max}",
    "weightSum": "Total weight must equal 1.0",
    "uniqueTitle": "This title already exists"
  }
}
```

**File:** `messages/ru.json` (add to existing)
```json
{
  "validation": {
    "required": "Это поле обязательно",
    "minLength": "Минимум {min} символов",
    "maxLength": "Максимум {max} символов",
    "minValue": "Минимальное значение: {min}",
    "maxValue": "Максимальное значение: {max}",
    "minItems": "Минимум {min} элементов",
    "maxItems": "Максимум {max} элементов",
    "invalidEmail": "Введите корректный email",
    "invalidUrl": "Введите корректный URL",
    "invalidFormat": "Неверный формат",
    "selectOption": "Выберите вариант",
    "invalidNumber": "Введите корректное число",
    "positiveNumber": "Должно быть положительным числом",
    "integerRequired": "Должно быть целым числом",
    "weightRange": "Вес должен быть от {min} до {max}",
    "weightSum": "Сумма весов должна равняться 1.0",
    "uniqueTitle": "Такое название уже существует"
  }
}
```

#### Step 2.2: Create Schema Factory Utility

**File:** `src/lib/schema-factory.ts`
```typescript
import { z } from 'zod';

type TranslationFunction = (key: string, params?: Record<string, unknown>) => string;

/**
 * Creates an i18n-aware Zod error map
 */
export function createI18nErrorMap(t: TranslationFunction): z.ZodErrorMap {
  return (issue, ctx) => {
    let message: string;

    switch (issue.code) {
      case z.ZodIssueCode.too_small:
        if (issue.type === 'string') {
          message = t('validation.minLength', { min: issue.minimum });
        } else if (issue.type === 'number') {
          message = t('validation.minValue', { min: issue.minimum });
        } else if (issue.type === 'array') {
          message = t('validation.minItems', { min: issue.minimum });
        } else {
          message = t('validation.required');
        }
        break;

      case z.ZodIssueCode.too_big:
        if (issue.type === 'string') {
          message = t('validation.maxLength', { max: issue.maximum });
        } else if (issue.type === 'number') {
          message = t('validation.maxValue', { max: issue.maximum });
        } else if (issue.type === 'array') {
          message = t('validation.maxItems', { max: issue.maximum });
        } else {
          message = ctx.defaultError;
        }
        break;

      case z.ZodIssueCode.invalid_string:
        if (issue.validation === 'email') {
          message = t('validation.invalidEmail');
        } else if (issue.validation === 'url') {
          message = t('validation.invalidUrl');
        } else {
          message = t('validation.invalidFormat');
        }
        break;

      case z.ZodIssueCode.invalid_enum_value:
        message = t('validation.selectOption');
        break;

      case z.ZodIssueCode.invalid_type:
        if (issue.expected === 'number') {
          message = t('validation.invalidNumber');
        } else {
          message = t('validation.required');
        }
        break;

      default:
        message = ctx.defaultError;
    }

    return { message };
  };
}

/**
 * Hook to get i18n-aware Zod resolver
 */
export function useI18nZodResolver<T extends z.ZodSchema>(
  schema: T,
  t: TranslationFunction
) {
  const errorMap = createI18nErrorMap(t);
  return zodResolver(schema, { errorMap });
}
```

#### Step 2.3: Create Schema Factories

**File:** `src/lib/schemas/indicator-schema.ts`
```typescript
import { z } from 'zod';
import { ObservabilityLevel, ApprovalStatus, IndicatorMeasurementType, ContextScope } from '@/types/domain';

type T = (key: string, params?: Record<string, unknown>) => string;

export function createIndicatorSchema(t: T) {
  return z.object({
    title: z.string()
      .min(5, t('validation.minLength', { min: 5 }))
      .max(200, t('validation.maxLength', { max: 200 })),
    description: z.string()
      .min(20, t('validation.minLength', { min: 20 }))
      .max(2000, t('validation.maxLength', { max: 2000 })),
    observabilityLevel: z.nativeEnum(ObservabilityLevel),
    measurementType: z.nativeEnum(IndicatorMeasurementType),
    weight: z.number()
      .min(0.01, t('validation.weightRange', { min: 0.01, max: 1.0 }))
      .max(1.0, t('validation.weightRange', { min: 0.01, max: 1.0 })),
    examples: z.string().optional(),
    counterExamples: z.string().optional(),
    isActive: z.boolean(),
    approvalStatus: z.nativeEnum(ApprovalStatus),
    orderIndex: z.number().int().min(1).optional(),
    contextScope: z.nativeEnum(ContextScope),
  });
}

export type IndicatorFormValues = z.infer<ReturnType<typeof createIndicatorSchema>>;
```

#### Step 2.4: Update Form Components

**Example (IndicatorForm.tsx):**
```typescript
import { useTranslations } from 'next-intl';
import { createIndicatorSchema } from '@/lib/schemas/indicator-schema';

export function IndicatorForm({ ... }) {
  const t = useTranslations();

  // Create schema with translations
  const schema = useMemo(() => createIndicatorSchema(t), [t]);

  const form = useForm<IndicatorFormValues>({
    resolver: zodResolver(schema),
    // ...
  });
}
```

---

### Phase 3: Form Labels and Sections
**Priority:** 🟡 High
**Estimated Time:** 4-5 hours
**Dependencies:** Phase 1, Phase 2

#### Step 3.1: Expand Forms Namespace

**File:** `messages/en.json` (expand existing `forms` namespace)
```json
{
  "forms": {
    "indicator": {
      "sections": {
        "coreInformation": "Core Information",
        "coreInformationDesc": "Title and description for this behavioral indicator",
        "classification": "Classification & Metrics",
        "classificationDesc": "Observability level, measurement type, and weight configuration",
        "contextualExamples": "Contextual Examples",
        "contextualExamplesDesc": "Provide examples to clarify expected and unexpected behaviors",
        "statusApproval": "Status & Approval",
        "statusApprovalDesc": "Manage visibility and approval workflow"
      },
      "fields": {
        "title": "Title",
        "titlePlaceholder": "e.g., Proactive Communication",
        "titleHelp": "A clear, specific name for this behavioral indicator",
        "description": "Description",
        "descriptionPlaceholder": "Detailed description of what this indicator measures and how it manifests...",
        "descriptionHelp": "Explain what this behavior looks like in practice",
        "observabilityLevel": "Observability Level",
        "observabilityLevelHelp": "How easily can this behavior be observed or measured?",
        "measurementType": "Measurement Type",
        "measurementTypeHelp": "What aspect of the behavior are we evaluating?",
        "weight": "Weight",
        "weightHelp": "Relative importance within this competency (all weights must sum to 1.0)",
        "orderIndex": "Display Order",
        "orderIndexHelp": "Position in the indicator list",
        "contextScope": "Context Scope",
        "contextScopeHelp": "In what professional context is this indicator most relevant?",
        "examples": "Positive Examples",
        "examplesPlaceholder": "List behaviors that demonstrate this indicator well...",
        "examplesHelp": "Concrete examples of behaviors that show this indicator",
        "counterExamples": "Counter Examples",
        "counterExamplesPlaceholder": "List behaviors that contradict this indicator...",
        "counterExamplesHelp": "Examples of behaviors that indicate the absence of this indicator",
        "approvalStatus": "Approval Status",
        "approvalStatusHelp": "Current workflow status",
        "isActive": "Active",
        "isActiveHelp": "Whether this indicator is available for use"
      },
      "buttons": {
        "adjustWeights": "Adjust Existing Weights",
        "updatePreview": "Update Preview"
      },
      "alerts": {
        "weightExceeded": "Weight validation failed",
        "weightRemaining": "Only {remaining} weight remaining for this competency.",
        "totalWeight": "Total weight: {total}"
      }
    },
    "question": {
      "sections": {
        "questionContent": "Question Content",
        "questionContentDesc": "The question text and type configuration",
        "answerOptions": "Answer Options",
        "answerOptionsDesc": "Configure the available responses",
        "metadata": "Metadata & Tags",
        "metadataDesc": "Difficulty, tags, and classification"
      },
      "fields": {
        "questionText": "Question Text",
        "questionTextPlaceholder": "Enter the question that will be shown to candidates...",
        "questionType": "Question Type",
        "difficultyLevel": "Difficulty Level",
        "scoringRubric": "Scoring Rubric",
        "scoringRubricPlaceholder": "Describe how responses should be evaluated...",
        "tags": "Tags",
        "tagsHelp": "Add tags to categorize this question"
      }
    },
    "competency": {
      "sections": {
        "basicInformation": "Basic Information",
        "basicInformationDesc": "Name and description of the competency",
        "classification": "Classification",
        "classificationDesc": "Category and standard mapping",
        "statusSettings": "Status & Settings",
        "statusSettingsDesc": "Approval workflow and visibility"
      },
      "fields": {
        "name": "Competency Name",
        "namePlaceholder": "e.g., Strategic Thinking",
        "description": "Description",
        "descriptionPlaceholder": "Comprehensive description of this competency...",
        "category": "Category",
        "categoryHelp": "Group similar competencies together",
        "standardCodes": "Standard Mapping",
        "standardCodesHelp": "Map to external frameworks (O*NET, ESCO)"
      }
    }
  }
}
```

#### Step 3.2: Migration Checklist

**IndicatorForm.tsx:**
- [ ] Section headers (4 sections)
- [ ] Field labels (12 fields)
- [ ] Placeholders (6 fields)
- [ ] Help text (where applicable)
- [ ] Button labels (5 buttons)
- [ ] Alert messages (3 alerts)
- [ ] Context scope descriptions (4 items)

**QuestionForm.tsx:**
- [ ] Section headers
- [ ] Field labels
- [ ] Placeholders
- [ ] Tag metadata descriptions
- [ ] Button labels
- [ ] Alert messages

**CompetencyForm.tsx:**
- [ ] Verify all strings migrated
- [ ] Add any missing translations

---

### Phase 4: Help Tooltips
**Priority:** 🟡 Medium
**Estimated Time:** 2 hours
**Dependencies:** Phase 1

#### Step 4.1: Add Help Namespace

**File:** `messages/en.json`
```json
{
  "help": {
    "competency": {
      "name": "A clear, concise name that identifies this competency. Use active language and avoid jargon.",
      "description": "Detailed explanation of what this competency entails, its importance, and how it manifests in workplace behavior. Aim for 100-500 characters.",
      "category": "Group related competencies together for easier navigation, filtering, and reporting.",
      "approvalStatus": "Track the review workflow. Only approved competencies can be used in test templates.",
      "isActive": "Control whether this competency is available for selection when creating assessments."
    },
    "indicator": {
      "title": "A specific, measurable behavior that demonstrates this competency. Be concrete and observable.",
      "description": "Describe what this behavior looks like in practice, why it matters, and how it relates to the parent competency.",
      "observabilityLevel": "Consider how easily this behavior can be detected: directly observed, inferred from outcomes, or self-reported.",
      "measurementType": "Choose what aspect to measure: frequency (how often), quality (how well), impact (what results), consistency, or improvement over time.",
      "weight": "Set relative importance within this competency. All indicator weights must sum to exactly 1.0.",
      "examples": "Provide 2-3 concrete examples of behaviors that clearly demonstrate this indicator.",
      "counterExamples": "Describe behaviors that would indicate the absence or opposite of this indicator."
    },
    "question": {
      "questionText": "Write clear, unambiguous questions. Avoid double negatives and leading language.",
      "questionType": "Choose the format: Likert scales for attitudes, SJT for behavioral judgment, MCQ for knowledge.",
      "difficultyLevel": "Match to your target audience: Foundational for entry-level, Expert for senior positions.",
      "scoringRubric": "Define how each response option maps to competency demonstration levels."
    }
  }
}
```

#### Step 4.2: Update HelpTooltip Component

**File:** `src/components/ui/help-tooltip.tsx`
```typescript
import { useTranslations } from 'next-intl';

interface HelpTooltipProps {
  helpKey: string;  // e.g., "competency.name" or "indicator.weight"
  children: React.ReactNode;
}

export function HelpTooltip({ helpKey, children }: HelpTooltipProps) {
  const t = useTranslations('help');

  return (
    <TooltipProvider>
      <Tooltip>
        <TooltipTrigger asChild>{children}</TooltipTrigger>
        <TooltipContent>
          <p className="max-w-xs">{t(helpKey)}</p>
        </TooltipContent>
      </Tooltip>
    </TooltipProvider>
  );
}
```

---

### Phase 5: Page Metadata
**Priority:** 🟢 Low
**Estimated Time:** 1-2 hours
**Dependencies:** None

#### Step 5.1: Convert to generateMetadata

**Example (competencies/page.tsx):**
```typescript
import { getTranslations } from 'next-intl/server';
import type { Metadata } from 'next';

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations('competency');

  return {
    title: `${t('title')} - SkillSoft`,
    description: t('pageDescription'),
    openGraph: {
      title: `${t('title')} - SkillSoft`,
      description: t('pageDescription'),
    },
  };
}
```

#### Step 5.2: Pages to Update
- [ ] `hr/competencies/page.tsx`
- [ ] `hr/behavioral-indicators/page.tsx`
- [ ] `hr/assessment-questions/page.tsx`
- [ ] `test-templates/page.tsx`
- [ ] `my-tests/page.tsx`
- [ ] `test-results/[resultId]/page.tsx`

---

### Phase 6: Testing & Validation
**Priority:** 🟡 High
**Estimated Time:** 2-3 hours
**Dependencies:** All phases

#### Step 6.1: Manual Testing Checklist

```markdown
## Language Switching
- [ ] Header language toggle works
- [ ] Page content updates without refresh
- [ ] Cookie persists across sessions
- [ ] Server components render correct locale

## Navigation
- [ ] Sidebar items translate
- [ ] Breadcrumbs translate
- [ ] Mobile bottom nav translates

## Forms
- [ ] CompetencyForm labels translate
- [ ] IndicatorForm labels translate
- [ ] QuestionForm labels translate
- [ ] Validation errors translate
- [ ] Toast messages translate

## Enum Dropdowns
- [ ] CompetencyCategory dropdown
- [ ] ObservabilityLevel dropdown
- [ ] ApprovalStatus dropdown
- [ ] ContextScope dropdown (with descriptions)
- [ ] MeasurementType dropdown
- [ ] DifficultyLevel dropdown
- [ ] QuestionType dropdown

## Data Display
- [ ] Table headers translate
- [ ] Status badges translate
- [ ] Empty states translate

## Error Handling
- [ ] Error pages translate
- [ ] Network error messages translate
- [ ] Validation error messages translate
```

#### Step 6.2: Automated Tests

**File:** `src/__tests__/i18n/translation-coverage.test.ts`
```typescript
import enMessages from '@/messages/en.json';
import ruMessages from '@/messages/ru.json';

describe('Translation Coverage', () => {
  function getAllKeys(obj: object, prefix = ''): string[] {
    return Object.entries(obj).flatMap(([key, value]) => {
      const path = prefix ? `${prefix}.${key}` : key;
      if (typeof value === 'object' && value !== null) {
        return getAllKeys(value, path);
      }
      return [path];
    });
  }

  const enKeys = getAllKeys(enMessages);
  const ruKeys = getAllKeys(ruMessages);

  it('Russian has all English keys', () => {
    const missingInRu = enKeys.filter(key => !ruKeys.includes(key));
    expect(missingInRu).toEqual([]);
  });

  it('English has all Russian keys', () => {
    const missingInEn = ruKeys.filter(key => !enKeys.includes(key));
    expect(missingInEn).toEqual([]);
  });

  it('No empty translation values in English', () => {
    const emptyKeys = enKeys.filter(key => {
      const value = key.split('.').reduce((obj, k) => obj?.[k], enMessages as any);
      return value === '' || value === null;
    });
    expect(emptyKeys).toEqual([]);
  });

  it('No empty translation values in Russian', () => {
    const emptyKeys = ruKeys.filter(key => {
      const value = key.split('.').reduce((obj, k) => obj?.[k], ruMessages as any);
      return value === '' || value === null;
    });
    expect(emptyKeys).toEqual([]);
  });
});
```

**File:** `src/__tests__/i18n/enum-translations.test.ts`
```typescript
import enMessages from '@/messages/en.json';
import ruMessages from '@/messages/ru.json';
import {
  CompetencyCategory,
  ObservabilityLevel,
  ApprovalStatus,
  ContextScope,
  IndicatorMeasurementType,
  DifficultyLevel,
  QuestionType,
  SessionStatus
} from '@/types/domain';

describe('Enum Translations', () => {
  const enumsToTest = [
    { name: 'competencyCategory', values: Object.values(CompetencyCategory) },
    { name: 'observabilityLevel', values: Object.values(ObservabilityLevel) },
    { name: 'approvalStatus', values: Object.values(ApprovalStatus) },
    { name: 'contextScope', values: Object.values(ContextScope) },
    { name: 'measurementType', values: Object.values(IndicatorMeasurementType) },
    { name: 'difficultyLevel', values: Object.values(DifficultyLevel) },
    { name: 'questionType', values: Object.values(QuestionType) },
    { name: 'sessionStatus', values: Object.values(SessionStatus) },
  ];

  enumsToTest.forEach(({ name, values }) => {
    describe(name, () => {
      it(`has all ${values.length} values translated in English`, () => {
        const translations = (enMessages as any).enums?.[name] || {};
        values.forEach(value => {
          expect(translations[value]).toBeDefined();
          expect(translations[value]).not.toBe('');
        });
      });

      it(`has all ${values.length} values translated in Russian`, () => {
        const translations = (ruMessages as any).enums?.[name] || {};
        values.forEach(value => {
          expect(translations[value]).toBeDefined();
          expect(translations[value]).not.toBe('');
        });
      });
    });
  });
});
```

---

## Implementation Timeline

| Phase | Priority | Duration | Dependencies | Deliverables |
|-------|----------|----------|--------------|--------------|
| **Phase 1** | Critical | 2-3h | None | Enum translations, useEnumTranslation hook |
| **Phase 2** | Critical | 3-4h | None | Schema factories, i18n Zod utility |
| **Phase 3** | High | 4-5h | P1, P2 | All form labels migrated |
| **Phase 4** | Medium | 2h | P1 | Help tooltips migrated |
| **Phase 5** | Low | 1-2h | None | Dynamic page metadata |
| **Phase 6** | High | 2-3h | All | Test coverage, validation |

**Total Estimated Time:** 14-19 hours

---

## Best Practices

### 1. Translation Key Naming
```
namespace.section.key
namespace.entity.field

Examples:
- forms.indicator.fields.title
- enums.approvalStatus.DRAFT
- validation.minLength
- help.competency.description
```

### 2. Interpolation
```typescript
// Use ICU message format for parameters
t('validation.minLength', { min: 5 })

// Russian pluralization
"{count, plural, =0 {нет элементов} one {# элемент} few {# элемента} many {# элементов} other {# элементов}}"
```

### 3. Fallback Strategy
- Primary: User's selected locale (cookie)
- Fallback: Russian (ru)
- Last resort: Translation key displayed

### 4. Component Pattern
```typescript
// Prefer namespace-specific translations
const t = useTranslations('forms.indicator');
t('fields.title')  // Clean, typed access

// Avoid generic translations in components
const t = useTranslations();
t('forms.indicator.fields.title')  // Verbose, error-prone
```

---

## Success Criteria

| Metric | Target |
|--------|--------|
| Translation coverage | 100% UI strings |
| Enum translation coverage | 100% (all 9 enums) |
| Form validation i18n | 100% of schemas |
| Bundle size increase | < 20KB |
| Language switch latency | < 200ms |
| Test coverage | > 80% translation keys tested |

---

## Appendix: File Inventory

### Files to Create
- `src/hooks/useEnumTranslation.ts`
- `src/lib/schema-factory.ts`
- `src/lib/schemas/indicator-schema.ts`
- `src/lib/schemas/question-schema.ts`
- `src/lib/schemas/competency-schema.ts`
- `src/__tests__/i18n/translation-coverage.test.ts`
- `src/__tests__/i18n/enum-translations.test.ts`

### Files to Modify
- `messages/en.json` (add ~200 keys)
- `messages/ru.json` (add ~200 keys)
- `app/(workspace)/hr/competencies/_components/CompetencyForm.tsx`
- `app/(workspace)/hr/behavioral-indicators/_components/IndicatorForm.tsx`
- `app/(workspace)/hr/assessment-questions/_components/QuestionForm.tsx`
- `app/(workspace)/test-templates/[id]/settings/_components/SettingsForm.tsx`
- `src/components/ui/help-tooltip.tsx`
- Multiple page.tsx files (for generateMetadata)

---

**Document Prepared By:** Claude Code
**Last Updated:** 2026-01-01
