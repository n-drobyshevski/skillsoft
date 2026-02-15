# Psychometrics Module i18n Implementation Specification

## Document Version
- **Version:** 1.0
- **Date:** 2026-01-01
- **Status:** Ready for Implementation
- **Related:** I18N_IMPROVEMENT_SPEC.md

---

## Executive Summary

This specification extends the SkillSoft internationalization system to the psychometrics module. The module currently has **~95 translation keys defined in messages files but NONE are used** - all UI text is hardcoded in components (mixed Russian/English).

**Key Metrics:**
| Metric | Value |
|--------|-------|
| Components to migrate | 68 |
| New translation keys | ~503 |
| Existing unused keys | 95 |
| Estimated effort | ~34 hours (4.5 dev days) |

---

## 1. State Machine Diagram

```
                                +------------------+
                                |      IDLE        |
                                +--------+---------+
                                         |
                                         | trigger: start_migration
                                         v
                                +------------------+
                                |    ANALYZE       |
                                +--------+---------+
                                         |
                                         | complete: inventory_ready
                                         v
                                +------------------+
                                |  EXTRACT_KEYS    |
                                +--------+---------+
                                         |
                                         | complete: keys_extracted
                                         v
                                +------------------+
                                |  SYNC_MESSAGES   |
                                +--------+---------+
                                         |
                                         | complete: messages_synced
                                         v
                        +----------------+----------------+
                        |                                 |
                        v                                 v
                +---------------+                 +---------------+
                | MIGRATE_PHASE |                 | MIGRATE_PHASE |
                |      1        |       ...       |      N        |
                +-------+-------+                 +-------+-------+
                        |                                 |
                        | phase_complete                  | phase_complete
                        v                                 v
                +---------------+                 +---------------+
                |   VALIDATE    |                 |   VALIDATE    |
                +-------+-------+                 +-------+-------+
                        |                                 |
                        | pass/fail                       | pass/fail
                        v                                 v
                +---------------+                 +---------------+
                |    TEST       |                 |    TEST       |
                +-------+-------+                 +-------+-------+
                        |                                 |
                        +----------------+----------------+
                                         |
                                         | all_phases_complete
                                         v
                                +------------------+
                                |    COMPLETE      |
                                +------------------+

Recovery Transitions:
- Any state -> ROLLBACK: on critical_error
- ROLLBACK -> previous_checkpoint: on rollback_complete
- VALIDATE (fail) -> MIGRATE_PHASE (same): on fix_required
```

---

## 2. Current State Analysis

### Module Structure

```
frontend-app/app/(workspace)/psychometrics/
├── page.tsx                              # Main dashboard
├── _components/
│   ├── DashboardHero.tsx                 # Health score (mixed EN/RU)
│   ├── PsychometricStatsCards.tsx        # Item stats (RU)
│   ├── TriggerAuditButton.tsx            # Audit dialog (EN)
│   ├── QuickFilterPills.tsx              # Filters (RU)
│   ├── PsychometricHelpTooltip.tsx       # Help content (RU, ~750 lines)
│   ├── ActionableInsightCard.tsx         # AI insights (EN)
│   ├── MetricDistributionChart.tsx       # Charts
│   ├── ItemQualityScatter.tsx            # Scatter plot
│   ├── ReliabilityGaugesGrid.tsx         # Gauges
│   └── MobileAnalyticsAccordion.tsx      # Mobile layout
├── big-five/
│   ├── page.tsx                          # Big Five reliability
│   └── _components/
│       ├── BigFiveTraitCard.tsx          # Trait cards (RU)
│       ├── TraitDetailAccordion.tsx      # Trait analysis (RU)
│       ├── TraitDetailDrawer.tsx         # Mobile drawer
│       ├── BigFiveComparisonChart.tsx    # Comparison chart
│       ├── MobileTraitCarouselEnhanced.tsx
│       └── MobileTraitCardSimple.tsx
├── competencies/
│   ├── page.tsx                          # Competency reliability
│   ├── [competencyId]/page.tsx           # Competency detail
│   └── _components/
│       ├── CompetenciesTableClient.tsx   # Table
│       ├── CompetencyReliabilityCard.tsx # Card display
│       ├── AlphaInterpretationScale.tsx  # Alpha scale
│       ├── HeroAlphaGauge.tsx            # Hero gauge
│       └── CompactReliabilityList.tsx    # Compact list
├── items/
│   ├── page.tsx                          # Items table
│   ├── [questionId]/page.tsx             # Item detail
│   └── _components/
│       ├── ItemsTableClient.tsx          # Table
│       ├── ItemDetailClient.tsx          # Detail view
│       ├── IssuesBanner.tsx              # Issue warnings (RU)
│       ├── ThresholdsAccordion.tsx       # Thresholds (RU)
│       ├── QuestionSection.tsx           # Question display
│       ├── LinearMetricGauge.tsx         # Gauges
│       └── ResponsiveGauges.tsx          # Responsive gauges
└── flagged/
    ├── page.tsx                          # Flagged items
    ├── [itemId]/page.tsx                 # Flagged item detail
    └── _components/
        ├── FlaggedItemsClient.tsx        # Main client (mixed)
        ├── FlaggedItemsTable.tsx         # Table
        ├── MobileFlaggedItemLayout.tsx   # Mobile layout
        ├── StickyActionBar.tsx           # Actions
        └── MobileSeverityBanner.tsx      # Severity
```

### Psychometrics-Specific Enums

| Enum | Values | Current State |
|------|--------|---------------|
| `ItemValidityStatus` | ACTIVE, PROBATION, FLAGGED_FOR_REVIEW, RETIRED | Display objects in types/psychometrics.ts |
| `DifficultyFlag` | NONE, TOO_HARD, TOO_EASY | Hardcoded labels |
| `DiscriminationFlag` | NONE, WARNING, CRITICAL, NEGATIVE | Hardcoded labels |
| `ReliabilityStatus` | RELIABLE, ACCEPTABLE, UNRELIABLE, INSUFFICIENT_DATA | Hardcoded labels |
| `BigFiveTrait` | OPENNESS, CONSCIENTIOUSNESS, EXTRAVERSION, AGREEABLENESS, EMOTIONAL_STABILITY | BigFiveTraitDisplay object (RU) |

### Text Categories Requiring i18n

1. **Page Titles & Descriptions** - Headers, subheaders, page descriptions
2. **Navigation Labels** - Sidebar items, tabs, breadcrumbs
3. **Dashboard Metrics** - Stat labels, tooltips, gauge labels
4. **Enum Display Values** - Status badges, filter pills, dropdown options
5. **Help/Educational Content** - Tooltips (~750 lines in PsychometricHelpTooltip)
6. **Audit Dialog** - Confirmation dialog content
7. **Warning Messages** - Issue banners, severity indicators
8. **Big Five Trait Content** - Trait names, descriptions, recommendations
9. **Mobile-Specific UI** - Mobile layouts with different text

---

## 3. Translation Key Structure

### Namespace Organization

```json
{
  "psychometrics": {
    "nav": {
      "items": "Items",
      "competencies": "Competencies",
      "flagged": "Flagged",
      "bigFive": "Big Five"
    },
    "hero": {
      "health": "Health",
      "psychometricHealth": "Psychometric Health",
      "lastAudit": "Last Audit",
      "totalItems": "Total Items",
      "activeRate": "Active Rate",
      "issues": "Issues",
      "vsLast7Days": "vs last 7 days",
      "scoreBreakdown": "Score Breakdown:",
      "activeItems": "Active Items ({percent}%)",
      "reliableCompetencies": "Reliable Competencies ({percent}%)",
      "nonFlaggedItems": "Non-Flagged Items ({percent}%)"
    },
    "stats": {
      "itemStatus": "Item Status",
      "competencyReliability": "Competency Reliability",
      "bigFiveReliability": "Big Five Reliability",
      "avgCompetencyAlpha": "Average Competency Alpha",
      "avgDiscrimination": "Average Discrimination Index",
      "avgBigFiveAlpha": "Average Big Five Alpha",
      "active": "Active",
      "probation": "Probation",
      "flagged": "Flagged",
      "retired": "Retired",
      "reliable": "Reliable",
      "acceptable": "Acceptable",
      "unreliable": "Unreliable",
      "insufficientData": "Insufficient Data"
    },
    "filters": {
      "all": "All",
      "byStatus": "By Status",
      "byReliability": "By Reliability",
      "searchPlaceholder": "Search items..."
    },
    "items": {
      "title": "Assessment Items",
      "description": "Psychometric statistics for questions: difficulty and discrimination indices",
      "difficultyIndex": "Difficulty Index (p)",
      "discriminationIndex": "Discrimination Index (rpb)",
      "optimalRange": "Optimal range: {min} - {max}",
      "goodValue": "Good value: >= {threshold}",
      "excellentValue": "Excellent value: >= {threshold}",
      "hierarchy": "Hierarchy:"
    },
    "competencies": {
      "title": "Competency Reliability",
      "description": "Cronbach's Alpha and reliability statistics for competencies",
      "alphaCoefficient": "Cronbach's Alpha",
      "itemCount": "Item Count",
      "alphaThreshold": "Threshold: {value}"
    },
    "bigFive": {
      "title": "Big Five Reliability",
      "description": "Cronbach's Alpha reliability analysis for personality trait measurements",
      "traits": {
        "openness": {
          "label": "Openness to Experience",
          "description": "Openness reflects a person's curiosity, creativity, and willingness to explore new ideas"
        },
        "conscientiousness": {
          "label": "Conscientiousness",
          "description": "Conscientiousness reflects organization, dependability, and goal-directed behavior"
        },
        "extraversion": {
          "label": "Extraversion",
          "description": "Extraversion reflects sociability, assertiveness, and positive emotionality"
        },
        "agreeableness": {
          "label": "Agreeableness",
          "description": "Agreeableness reflects cooperation, trust, and prosocial orientation"
        },
        "emotionalStability": {
          "label": "Emotional Stability",
          "description": "Emotional stability reflects calmness, resilience, and emotional regulation"
        }
      },
      "recommendations": {
        "excellent": "Excellent Reliability",
        "recommended": "Recommended Improvement",
        "attention": "Requires Attention",
        "insufficient": "Insufficient Data",
        "closeToThreshold": "Close to Threshold"
      }
    },
    "flagged": {
      "title": "Flagged Items",
      "description": "Assessment items requiring attention due to low psychometric indicators",
      "severity": {
        "negative": "Negative",
        "critical": "Critical",
        "warnings": "Warnings",
        "total": "Total"
      },
      "issues": {
        "negativeDiscrimination": "Negative discrimination index - item works in reverse direction",
        "criticalDiscrimination": "Critically low discrimination index",
        "weakDiscrimination": "Weak discrimination index",
        "tooHard": "Question is too difficult",
        "tooEasy": "Question is too easy"
      }
    },
    "audit": {
      "title": "Run Psychometric Audit?",
      "description": "The audit will recalculate all psychometric metrics:",
      "items": {
        "difficulty": "Difficulty and discrimination indices for all questions",
        "alpha": "Cronbach's Alpha coefficients for competencies",
        "bigFive": "Big Five trait scale reliability",
        "status": "Automatic item status updates"
      },
      "warning": "This may take several minutes depending on data volume.",
      "cancel": "Cancel",
      "runAudit": "Run Audit",
      "processing": "Processing..."
    },
    "tooltips": {
      "difficultyIndex": {
        "title": "Difficulty Index (p)",
        "description": "Proportion of correct responses. Range: 0-1. Optimal: 0.3-0.7."
      },
      "discriminationIndex": {
        "title": "Discrimination Index (rpb)",
        "description": "Point-biserial correlation between item and total score. Good: >= 0.25, Excellent: >= 0.35."
      }
    },
    "insights": {
      "noIssues": "All items are performing well",
      "flaggedCount": "{count} items require attention",
      "topIssue": "Most common issue: {issue}"
    },
    "empty": {
      "noItems": "No items found",
      "noFlagged": "No flagged items",
      "noData": "No data available",
      "tryFilters": "Try changing filters or run an audit"
    },
    "thresholds": {
      "allOk": "All OK",
      "oneWarning": "1 warning",
      "multipleWarnings": "{count} warnings"
    }
  },
  "enums": {
    "itemValidityStatus": {
      "ACTIVE": "Active",
      "ACTIVE_DESC": "Items with good psychometric properties",
      "PROBATION": "Probation",
      "PROBATION_DESC": "New items collecting data",
      "FLAGGED_FOR_REVIEW": "Flagged for Review",
      "FLAGGED_FOR_REVIEW_DESC": "Items requiring attention",
      "RETIRED": "Retired",
      "RETIRED_DESC": "Items removed from active use"
    },
    "discriminationFlag": {
      "NONE": "None",
      "WARNING": "Warning",
      "CRITICAL": "Critical",
      "NEGATIVE": "Negative"
    },
    "difficultyFlag": {
      "NONE": "None",
      "TOO_HARD": "Too Hard",
      "TOO_EASY": "Too Easy"
    },
    "reliabilityStatus": {
      "RELIABLE": "Reliable",
      "RELIABLE_DESC": "Alpha >= 0.7",
      "ACCEPTABLE": "Acceptable",
      "ACCEPTABLE_DESC": "Alpha 0.6 - 0.7",
      "UNRELIABLE": "Unreliable",
      "UNRELIABLE_DESC": "Alpha < 0.6",
      "INSUFFICIENT_DATA": "Insufficient Data",
      "INSUFFICIENT_DATA_DESC": "Requires more answers"
    },
    "bigFiveTrait": {
      "OPENNESS": "Openness",
      "CONSCIENTIOUSNESS": "Conscientiousness",
      "EXTRAVERSION": "Extraversion",
      "AGREEABLENESS": "Agreeableness",
      "EMOTIONAL_STABILITY": "Emotional Stability"
    }
  }
}
```

### Russian Translations (Partial Example)

```json
{
  "psychometrics": {
    "hero": {
      "health": "Здоровье",
      "psychometricHealth": "Здоровье психометрики",
      "lastAudit": "Последний аудит",
      "totalItems": "Всего элементов",
      "activeRate": "Активных",
      "issues": "Проблемы",
      "vsLast7Days": "за 7 дней"
    },
    "stats": {
      "itemStatus": "Статус элементов оценки",
      "competencyReliability": "Надежность компетенций",
      "bigFiveReliability": "Надежность Big Five",
      "active": "Активные",
      "probation": "Пробационные",
      "flagged": "На проверке",
      "retired": "Отключены",
      "reliable": "Надежные",
      "acceptable": "Приемлемые",
      "unreliable": "Ненадежные",
      "insufficientData": "Недостаточно данных"
    },
    "bigFive": {
      "traits": {
        "openness": {
          "label": "Открытость опыту"
        },
        "conscientiousness": {
          "label": "Добросовестность"
        },
        "extraversion": {
          "label": "Экстраверсия"
        },
        "agreeableness": {
          "label": "Доброжелательность"
        },
        "emotionalStability": {
          "label": "Эмоциональная стабильность"
        }
      }
    }
  },
  "enums": {
    "itemValidityStatus": {
      "ACTIVE": "Активный",
      "ACTIVE_DESC": "Элементы с хорошими психометрическими свойствами",
      "PROBATION": "Пробационный",
      "PROBATION_DESC": "Новые элементы, собирающие данные",
      "FLAGGED_FOR_REVIEW": "На проверке",
      "FLAGGED_FOR_REVIEW_DESC": "Элементы, требующие внимания",
      "RETIRED": "Отключён",
      "RETIRED_DESC": "Элементы, удалённые из активного использования"
    },
    "reliabilityStatus": {
      "RELIABLE": "Надежный",
      "RELIABLE_DESC": "Alpha >= 0.7",
      "ACCEPTABLE": "Приемлемый",
      "ACCEPTABLE_DESC": "Alpha 0.6 - 0.7",
      "UNRELIABLE": "Ненадежный",
      "UNRELIABLE_DESC": "Alpha < 0.6",
      "INSUFFICIENT_DATA": "Недостаточно данных",
      "INSUFFICIENT_DATA_DESC": "Требуется больше ответов"
    }
  }
}
```

---

## 4. Implementation Phases

### Phase 0: Analysis & Inventory (2 hours)

**Objective**: Catalog all hardcoded text in psychometrics components.

**Tasks**:
1. Scan all `.tsx` files in `/app/(workspace)/psychometrics/`
2. Identify hardcoded strings (Russian, English, mixed)
3. Categorize by text type
4. Generate inventory for tracking
5. Audit existing 95 keys for reusability

**Outputs**:
- Inventory JSON with text locations and types
- Priority ranking based on user visibility
- List of reusable vs obsolete existing keys

**Success Criteria**:
- [ ] All 68 components scanned
- [ ] Text categorized by type
- [ ] Priority assigned

---

### Phase 1: Key Extraction & Message Sync (4 hours)

**Objective**: Define translation keys and update message files.

**Tasks**:
1. Add ~503 new keys to `messages/en.json`
2. Add ~503 new keys to `messages/ru.json`
3. Add 5 psychometrics enum namespaces
4. Update `useEnumTranslation` hook with new namespaces

**Files to Modify**:
- `messages/en.json`
- `messages/ru.json`
- `src/hooks/useEnumTranslation.ts`

**Success Criteria**:
- [ ] All keys defined following naming convention
- [ ] en.json and ru.json in sync (same key structure)
- [ ] No duplicate keys
- [ ] Enum namespaces added

---

### Phase 2: Core Infrastructure Components (3 hours)

**Objective**: Migrate shared/reusable psychometrics components.

**Components (in order)**:
1. `PsychometricHelpTooltip.tsx` - Move `psychometricHelp` object (~750 lines RU JSX)
2. `ValidityStatusBadge.tsx` - Use enum translations
3. `ReliabilityStatusBadge.tsx` - Use enum translations
4. `QuickFilterPills.tsx` - Filter labels
5. `StatPill.tsx` - Stat labels
6. `MetricBadge.tsx` - Metric display

**Migration Pattern**:
```tsx
// BEFORE
<span>Active</span>

// AFTER
import { useTranslations } from 'next-intl';
const t = useTranslations('psychometrics.stats');
<span>{t('active')}</span>
```

**For Enums**:
```tsx
// BEFORE
const label = status === 'ACTIVE' ? 'Активные' : 'Пробационные';

// AFTER
import { useEnumTranslation } from '@/hooks/useEnumTranslation';
const { translate } = useEnumTranslation('itemValidityStatus');
const label = translate(status);
```

**Success Criteria**:
- [ ] All badge components use translations
- [ ] No hardcoded enum display values
- [ ] Fallback behavior works (missing key returns formatted enum)

---

### Phase 3: Dashboard Components (4 hours)

**Objective**: Migrate main dashboard page components.

**Components**:
1. `DashboardHero.tsx` - Health score, stats, legend
2. `PsychometricStatsCards.tsx` - All stat cards
3. `TriggerAuditButton.tsx` - Dialog content
4. `QuickNavCard.tsx` - Navigation cards

**Special Considerations**:
- `DashboardHero.tsx` has mixed EN/RU text - needs careful extraction
- `PsychometricStatsCards.tsx` has separate mobile/desktop layouts
- `TriggerAuditButton.tsx` dialog has list items needing proper i18n

**Success Criteria**:
- [ ] Dashboard loads in both EN/RU
- [ ] Mobile and desktop layouts show correct translations
- [ ] Audit dialog fully translated

---

### Phase 4: Items Module (4 hours)

**Objective**: Migrate items table and item detail pages.

**Components**:
1. `ItemsTableClient.tsx` - Table headers, actions
2. `ItemDetailClient.tsx` - Detail view
3. `IssuesBanner.tsx` - Warning messages (RU hardcoded)
4. `ThresholdsAccordion.tsx` - Threshold explanations (RU educational content)
5. `LinearMetricGauge.tsx` - Gauge labels
6. `QuestionSection.tsx` - Question display

**Success Criteria**:
- [ ] Items table fully translated
- [ ] Item detail page fully translated
- [ ] Issue warnings localized

---

### Phase 5: Competencies Module (3 hours)

**Objective**: Migrate competency reliability components.

**Components**:
1. `CompetenciesTableClient.tsx` - Table
2. `CompetencyReliabilityCard.tsx` - Card display
3. `AlphaInterpretationScale.tsx` - Scale labels
4. `HeroAlphaGauge.tsx` - Gauge display
5. `CompactReliabilityList.tsx` - Compact list

**Success Criteria**:
- [ ] Competency tables translated
- [ ] Alpha interpretation fully localized

---

### Phase 6: Big Five Module (4 hours)

**Objective**: Migrate Big Five personality trait components.

**Critical Changes**:
- `BigFiveTraitDisplay` type in `types/psychometrics.ts` has hardcoded RU labels
- `getTraitInterpretation()` function has extensive RU text
- Recommendation generation is all RU

**Components**:
1. Update `types/psychometrics.ts` - Make locale-aware
2. `BigFiveTraitCard.tsx` - Trait cards
3. `TraitDetailAccordion.tsx` - Detailed analysis + recommendations
4. `TraitDetailDrawer.tsx` - Drawer content
5. `BigFiveComparisonChart.tsx` - Chart labels
6. `MobileTraitCarousel.tsx` - Mobile carousel

**Success Criteria**:
- [ ] All Big Five traits show correct locale labels
- [ ] Trait interpretations fully localized
- [ ] Recommendations translated

---

### Phase 7: Flagged Items Module (3 hours)

**Objective**: Migrate flagged items review workflow.

**Components**:
1. `FlaggedItemsClient.tsx` - Main client (EN suggestions)
2. `FlaggedItemDetailClient.tsx` - Detail view
3. `StickyActionBar.tsx` - Action buttons
4. `MobileSeverityBanner.tsx` - Severity indicators

**Success Criteria**:
- [ ] Flagged items workflow fully localized
- [ ] Batch actions localized
- [ ] Suggestion system localized

---

### Phase 8: Insights & Charts (3 hours)

**Objective**: Migrate insight generation and chart components.

**Critical**: `generateInsights()` function creates EN text dynamically

**Components**:
1. `ActionableInsightCard.tsx` - Dynamic insights
2. `SuggestedActionsCard.tsx` - Suggestions
3. `ItemQualityScatter.tsx` - Chart labels
4. `MetricDistributionChart.tsx` - Distribution chart
5. `ReliabilityGauge.tsx` - Gauge labels

**Success Criteria**:
- [ ] Dynamic insight generation uses translations
- [ ] Chart labels localized
- [ ] Empty states localized

---

### Phase 9: Testing & QA (4 hours)

**Objective**: Validate complete migration.

**Unit Tests**:
- [ ] Translation hook integration tests
- [ ] Enum translation tests for all 5 psychometrics enums
- [ ] Missing key fallback tests
- [ ] Interpolation tests for dynamic content

**Integration Tests**:
- [ ] Dashboard renders in EN locale
- [ ] Dashboard renders in RU locale
- [ ] Language switching preserves page state
- [ ] Filter pills update with locale
- [ ] Charts update with locale

**Manual Testing Scenarios**:
1. Load dashboard in EN, verify all text
2. Switch to RU, verify all text updates
3. Navigate to items table, verify headers/filters
4. Open item detail, verify all content
5. View Big Five page, verify trait names/descriptions
6. Review flagged items, verify suggestions
7. Trigger audit, verify dialog content
8. Test mobile layouts in both locales

**Success Criteria**:
- [ ] All automated tests pass
- [ ] Manual testing complete in both locales
- [ ] No visual regressions

---

## 5. Component Migration Checklist

### Core Components
- [ ] PsychometricHelpTooltip.tsx
- [ ] ValidityStatusBadge.tsx
- [ ] ReliabilityStatusBadge.tsx
- [ ] MetricCell.tsx
- [ ] QuickFilterPills.tsx
- [ ] StatPill.tsx
- [ ] MetricBadge.tsx

### Dashboard
- [ ] DashboardHero.tsx
- [ ] PsychometricStatsCards.tsx
- [ ] TriggerAuditButton.tsx
- [ ] QuickNavCard.tsx
- [ ] MobileAnalyticsAccordion.tsx
- [ ] ReliabilityGaugesGrid.tsx
- [ ] ReliabilityQuickStats.tsx

### Items Module
- [ ] ItemsTableClient.tsx
- [ ] ItemDetailClient.tsx
- [ ] ItemDetailLayout.tsx
- [ ] IssuesBanner.tsx
- [ ] ThresholdsAccordion.tsx
- [ ] LinearMetricGauge.tsx
- [ ] ResponsiveGauges.tsx
- [ ] QuestionSection.tsx
- [ ] SwipeNavigator.tsx
- [ ] MobileItemHeader.tsx
- [ ] MobileAccordionWrapper.tsx
- [ ] MobileActionSheet.tsx

### Competencies Module
- [ ] CompetenciesTableClient.tsx
- [ ] CompetencyReliabilityCard.tsx
- [ ] CompactReliabilityCard.tsx
- [ ] CompactReliabilityList.tsx
- [ ] CompetencyHeroMobile.tsx
- [ ] CompetencyDetailAccordion.tsx
- [ ] AlphaInterpretationScale.tsx
- [ ] HeroAlphaGauge.tsx
- [ ] InsufficientDataGuidance.tsx
- [ ] ThresholdSummaryBadge.tsx
- [ ] AlphaIfDeletedList.tsx

### Big Five Module
- [ ] BigFiveTraitCard.tsx
- [ ] BigFiveClientWrapper.tsx
- [ ] BigFiveComparisonChart.tsx
- [ ] BigFiveComparisonChartLazy.tsx
- [ ] TraitDetailAccordion.tsx
- [ ] TraitDetailDrawer.tsx
- [ ] MobileTraitCarousel.tsx
- [ ] MobileTraitCarouselEnhanced.tsx
- [ ] MobileTraitCardSimple.tsx
- [ ] MobileVerticalBarChart.tsx

### Flagged Items Module
- [ ] FlaggedItemsClient.tsx
- [ ] FlaggedItemsTable.tsx
- [ ] FlaggedItemDetailClient.tsx
- [ ] MobileFlaggedItemLayout.tsx
- [ ] MobileFlaggedItemLayoutWrapper.tsx
- [ ] StickyActionBar.tsx
- [ ] MobileSeverityBanner.tsx
- [ ] MobileMetricPill.tsx

### Insights & Charts
- [ ] ActionableInsightCard.tsx
- [ ] SuggestedActionsCard.tsx
- [ ] ProgressiveCompetencyCard.tsx
- [ ] BatchActionToolbar.tsx
- [ ] ItemQualityScatter.tsx
- [ ] MetricDistributionChart.tsx
- [ ] ReliabilityGauge.tsx
- [ ] RadialReliabilityChart.tsx
- [ ] charts-lazy.tsx

### Page Files
- [ ] page.tsx (main dashboard)
- [ ] items/page.tsx
- [ ] items/[questionId]/page.tsx
- [ ] competencies/page.tsx
- [ ] competencies/[competencyId]/page.tsx
- [ ] flagged/page.tsx
- [ ] flagged/[itemId]/page.tsx
- [ ] big-five/page.tsx
- [ ] loading.tsx (all)
- [ ] error.tsx

---

## 6. Risk Mitigation Strategies

### Risk 1: Missing Translation Keys
**Mitigation**:
- Implement `fallbackOnEmptyString: true` in next-intl config
- All translation hooks have try-catch with formatted fallback
- Pre-migration audit generates exhaustive key list

### Risk 2: Broken Layout from Text Length Differences
**Mitigation**:
- Russian text is typically 20-30% longer than English
- Review all components for `truncate`, `line-clamp` utilities
- Test mobile layouts specifically for overflow
- Add flex-shrink/min-width constraints where needed

### Risk 3: Enum Value Mismatch
**Mitigation**:
- Type-safe enum translations via `useEnumTranslation`
- Backend enum values are used as translation keys
- Fallback formats raw enum to readable text

### Risk 4: Dynamic Content Interpolation Errors
**Mitigation**:
- Use ICU message format for pluralization
- Test interpolation with edge cases (0, 1, many)
- Log warnings for missing interpolation values

### Risk 5: Migration Breaks Existing Functionality
**Mitigation**:
- Phase-based approach allows isolated testing
- Each phase has explicit success criteria
- Git branches per phase for easy rollback
- Automated visual regression tests

### Risk 6: Performance Impact from Translation Loading
**Mitigation**:
- next-intl lazy loads only needed namespaces
- `psychometrics` namespace already small (~95 keys + new)
- Server components pre-render with translations

---

## 7. Compensation Logic (Rollback Procedures)

### Per-Phase Rollback

```bash
# Each phase creates a checkpoint branch
git checkout -b i18n/psychometrics/phase-{N}-checkpoint

# On failure, rollback to previous checkpoint
git checkout i18n/psychometrics/phase-{N-1}-checkpoint
git branch -D i18n/psychometrics/phase-{N}
```

### Partial Migration Handling

If a phase partially completes:

1. **Identify completed components**: Review git diff
2. **Revert incomplete components**: `git checkout HEAD~1 -- path/to/component.tsx`
3. **Keep translation keys**: Don't remove from messages files
4. **Document partial state**: Update inventory with completion status

### Emergency Rollback (Full)

```bash
# Tag current state for forensics
git tag i18n-psychometrics-failed-$(date +%Y%m%d)

# Rollback to pre-migration state
git checkout main
git branch -D i18n/psychometrics

# Or if already merged:
git revert --no-commit HEAD~{N}..HEAD
git commit -m "Revert: i18n psychometrics migration"
```

---

## 8. Execution Timeline

| Phase | Duration | Dependencies |
|-------|----------|--------------|
| 0. Analysis | 2 hours | None |
| 1. Key Extraction | 4 hours | Phase 0 |
| 2. Core Components | 3 hours | Phase 1 |
| 3. Dashboard | 4 hours | Phase 2 |
| 4. Items Module | 4 hours | Phase 3 |
| 5. Competencies | 3 hours | Phase 4 |
| 6. Big Five | 4 hours | Phase 5 |
| 7. Flagged Items | 3 hours | Phase 6 |
| 8. Insights & Charts | 3 hours | Phase 7 |
| 9. Testing & QA | 4 hours | All phases |

**Total Estimated**: ~34 hours (~4.5 dev days)

---

## 9. Success Criteria

| Metric | Target |
|--------|--------|
| Components migrated | 68/68 (100%) |
| Translation key coverage | ~503 new keys |
| Enum translation coverage | 5/5 enums (100%) |
| Locale support | EN + RU |
| Bundle size increase | < 20KB |
| Language switch latency | < 200ms |
| Test coverage | > 80% translation keys tested |

---

## Appendix A: Existing psychometrics Namespace Keys

The following keys already exist but are NOT USED in components:

```json
{
  "psychometrics": {
    "title": "Psychometrics",
    "description": "...",
    "itemsNotFound": "Items not found",
    "tryChangingFilters": "Try changing filters...",
    // ... 90+ more keys
  }
}
```

**Action**: Audit existing keys against components to identify:
1. Keys that can be reused as-is
2. Keys that need renaming to fit new convention
3. Keys that are obsolete and can be removed

---

**Document Prepared By:** Claude Code (workflow-architect)
**Last Updated:** 2026-01-01
