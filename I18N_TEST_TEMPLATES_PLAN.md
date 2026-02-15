# i18n Extension Plan: test-templates/[id]/

## Executive Summary

**Scope:** Extend internationalization coverage for the test-templates/[id]/ route
**Target:** ~80 hardcoded strings across 9 files
**Languages:** English (EN) + Russian (RU)
**Framework:** next-intl v4.7.0 (already configured)

---

## Current State Analysis

### Already Implemented
- `next-intl` properly configured with App Router
- Message files exist: `messages/en.json`, `messages/ru.json`
- Namespaces established: `template.hub.*`, `template.metadata.*`
- Working components: NavTabs.tsx, QuickStatsGrid.tsx (partially)

### Gaps Identified

| Component | Location | Hardcoded Strings |
|-----------|----------|-------------------|
| ConfigurationCard.tsx | (overview)/_components/ | 15 |
| OverviewHero.tsx | (overview)/_components/ | 8 |
| SharingSummaryCard.tsx | (overview)/_components/ | 10 |
| RecentActivityList.tsx | (overview)/_components/ | 8 |
| Results page.tsx | results/ | 12 |
| Access page.tsx | access/ | 2 |
| Settings page.tsx | settings/ | 2 |
| Helper functions | Multiple files | 8+ (time formatting) |

---

## Namespace Architecture

```
template:
  hub:
    tabs: { ... }                    # EXISTS
    status: { ... }                  # EXISTS
    stats: { ... }                   # EXISTS

    overview:                        # NEW
      hero:
        noDescription
        goToBuilder
        createdAt: "Created: {date}"
        updatedAt: "Updated: {date}"

      configuration:
        title: "Configuration"
        description: "Test settings and behavior options"
        goToBuilder
        viewInBuilder
        timeLimit
        passScore
        questions
        competencies
        competenciesSelected: "{count, plural, one {# competency} other {# competencies}} selected"
        testBehavior
        shuffleQuestions
        shuffleOptions
        allowSkipping
        allowBackNav
        showResults

      recentActivity:
        title: "Recent Activity"
        viewAll
        noActivity
        noActivityDescription

      sharing:
        title: "Sharing"
        share
        peopleShared: "{count, plural, one {# person} other {# people}} shared"
        linksActive: "{count, plural, one {# link} other {# links}} active"
        copyLink
        linkCopied
        copyFailed
        linkUnavailable
        manageAccess
        noActiveLinks

      performance

    results:                         # NEW
      title: "Test Sessions"
      description
      stats: { total, done, active, avgScore }
      searchPlaceholder
      filters: { allStatuses, completed, inProgress, abandoned }
      export

    access:                          # NEW
      title
      description

    settings:                        # NEW
      title
      description
```

---

## Implementation Phases

### Phase 1: Message Files (Foundation)

**Priority:** HIGH - Must complete first

#### Task 1.1: Update messages/en.json

Add to `template.hub`:

```json
{
  "template": {
    "hub": {
      "overview": {
        "hero": {
          "noDescription": "No description",
          "goToBuilder": "Go to Builder",
          "createdAt": "Created: {date}",
          "updatedAt": "Updated: {date}"
        },
        "configuration": {
          "title": "Configuration",
          "description": "Test settings and behavior options",
          "goToBuilder": "Go to Builder",
          "viewInBuilder": "View in Builder",
          "timeLimit": "Time Limit",
          "passScore": "Pass Score",
          "questions": "Questions",
          "competencies": "Competencies",
          "competenciesSelected": "{count, plural, one {# competency} other {# competencies}} selected",
          "testBehavior": "Test Behavior",
          "shuffleQuestions": "Shuffle questions",
          "shuffleOptions": "Shuffle answer options",
          "allowSkipping": "Allow skipping questions",
          "allowBackNav": "Allow back navigation",
          "showResults": "Show results immediately"
        },
        "recentActivity": {
          "title": "Recent Activity",
          "viewAll": "View All",
          "noActivity": "No test activity yet",
          "noActivityDescription": "Activity will appear here once candidates complete the test"
        },
        "sharing": {
          "title": "Sharing",
          "share": "Share",
          "peopleShared": "{count, plural, one {# person} other {# people}} shared",
          "linksActive": "{count, plural, one {# link} other {# links}} active",
          "copyLink": "Copy Link",
          "linkCopied": "Link copied to clipboard",
          "copyFailed": "Failed to copy link",
          "linkUnavailable": "Link URL not available",
          "manageAccess": "Manage access settings",
          "noActiveLinks": "No active share links available"
        },
        "performance": "Performance"
      },
      "results": {
        "title": "Test Sessions",
        "description": "All candidates who have taken or are taking this test",
        "stats": {
          "total": "Total",
          "done": "Done",
          "active": "Active",
          "avgScore": "Avg Score"
        },
        "searchPlaceholder": "Search candidates...",
        "filters": {
          "allStatuses": "All Statuses",
          "completed": "Completed",
          "inProgress": "In Progress",
          "abandoned": "Abandoned"
        },
        "export": "Export"
      },
      "access": {
        "title": "Access",
        "description": "Manage who can view and use this template"
      },
      "settings": {
        "title": "Settings",
        "description": "Configure all template settings and behavior options"
      }
    }
  }
}
```

#### Task 1.2: Update messages/ru.json

Russian translations with proper pluralization (one/few/many):

```json
{
  "template": {
    "hub": {
      "overview": {
        "hero": {
          "noDescription": "Описание отсутствует",
          "goToBuilder": "Перейти в конструктор",
          "createdAt": "Создано: {date}",
          "updatedAt": "Обновлено: {date}"
        },
        "configuration": {
          "title": "Конфигурация",
          "description": "Настройки теста и параметры поведения",
          "goToBuilder": "Перейти в конструктор",
          "viewInBuilder": "Открыть в конструкторе",
          "timeLimit": "Ограничение времени",
          "passScore": "Проходной балл",
          "questions": "Вопросы",
          "competencies": "Компетенции",
          "competenciesSelected": "{count, plural, one {# компетенция} few {# компетенции} many {# компетенций} other {# компетенций}} выбрано",
          "testBehavior": "Поведение теста",
          "shuffleQuestions": "Перемешивать вопросы",
          "shuffleOptions": "Перемешивать варианты ответов",
          "allowSkipping": "Разрешить пропуск вопросов",
          "allowBackNav": "Разрешить возврат к предыдущим",
          "showResults": "Показывать результаты сразу"
        },
        "recentActivity": {
          "title": "Недавняя активность",
          "viewAll": "Смотреть все",
          "noActivity": "Активности пока нет",
          "noActivityDescription": "Здесь появится активность после прохождения теста кандидатами"
        },
        "sharing": {
          "title": "Доступ",
          "share": "Поделиться",
          "peopleShared": "{count, plural, one {# человек} few {# человека} many {# человек} other {# человек}}",
          "linksActive": "{count, plural, one {# ссылка} few {# ссылки} many {# ссылок} other {# ссылок}} активно",
          "copyLink": "Копировать ссылку",
          "linkCopied": "Ссылка скопирована",
          "copyFailed": "Не удалось скопировать",
          "linkUnavailable": "Ссылка недоступна",
          "manageAccess": "Управление доступом",
          "noActiveLinks": "Нет активных ссылок"
        },
        "performance": "Результативность"
      },
      "results": {
        "title": "Сессии тестирования",
        "description": "Все кандидаты, прошедшие или проходящие этот тест",
        "stats": {
          "total": "Всего",
          "done": "Завершено",
          "active": "Активно",
          "avgScore": "Средний балл"
        },
        "searchPlaceholder": "Поиск кандидатов...",
        "filters": {
          "allStatuses": "Все статусы",
          "completed": "Завершено",
          "inProgress": "В процессе",
          "abandoned": "Прервано"
        },
        "export": "Экспорт"
      },
      "access": {
        "title": "Доступ",
        "description": "Управление доступом к шаблону"
      },
      "settings": {
        "title": "Настройки",
        "description": "Настройка параметров шаблона"
      }
    }
  }
}
```

---

### Phase 2: Shared Utilities

**Priority:** HIGH - Centralizes relative time logic

#### Task 2.1: Extend useFormattedDates hook

**File:** `src/hooks/useFormattedDates.ts`

Add `formatRelativeTimeShort()` for compact display and ensure all time formatting uses `users.time` namespace.

```typescript
// Add to existing hook
const formatRelativeTimeShort = useCallback((dateString: string): string => {
  const now = new Date();
  const date = new Date(dateString);
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);
  const diffWeeks = Math.floor(diffDays / 7);

  if (diffMins < 1) return t('time.justNow');
  if (diffMins < 60) return t('time.minutesAgoShort', { count: diffMins });
  if (diffHours < 24) return t('time.hoursAgoShort', { count: diffHours });
  if (diffDays < 7) return t('time.daysAgoShort', { count: diffDays });
  return t('time.weeksAgoShort', { count: diffWeeks });
}, [t]);
```

---

### Phase 3: Overview Tab Components

**Priority:** MEDIUM-HIGH - Most visible to users

#### Task 3.1: ConfigurationCard.tsx

**File:** `app/(workspace)/test-templates/[id]/(overview)/_components/ConfigurationCard.tsx`

```tsx
'use client';
import { useTranslations } from 'next-intl';

export function ConfigurationCard({ template }: Props) {
  const t = useTranslations('template.hub.overview.configuration');

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t('title')}</CardTitle>
        <CardDescription>{t('description')}</CardDescription>
      </CardHeader>
      <CardContent>
        {/* Time Limit */}
        <div>
          <span className="text-muted-foreground">{t('timeLimit')}</span>
          <span>{template.settings.timeLimit}</span>
        </div>

        {/* Pass Score */}
        <div>
          <span className="text-muted-foreground">{t('passScore')}</span>
          <span>{template.settings.passingScore}%</span>
        </div>

        {/* Test Behavior Section */}
        <h4>{t('testBehavior')}</h4>
        <ul>
          <li>{t('shuffleQuestions')}: {template.settings.shuffleQuestions ? '✓' : '✗'}</li>
          <li>{t('shuffleOptions')}: {template.settings.shuffleOptions ? '✓' : '✗'}</li>
          <li>{t('allowSkipping')}: {template.settings.allowSkipping ? '✓' : '✗'}</li>
          <li>{t('allowBackNav')}: {template.settings.allowBackNav ? '✓' : '✗'}</li>
          <li>{t('showResults')}: {template.settings.showResults ? '✓' : '✗'}</li>
        </ul>

        {/* Competencies */}
        <div>
          <span>{t('competencies')}</span>
          <span>{t('competenciesSelected', { count: template.competencies.length })}</span>
        </div>

        <Button variant="outline">{t('viewInBuilder')}</Button>
      </CardContent>
    </Card>
  );
}
```

#### Task 3.2: OverviewHero.tsx

```tsx
'use client';
import { useTranslations } from 'next-intl';
import { useFormattedDates } from '@/hooks/useFormattedDates';

export function OverviewHero({ template }: Props) {
  const t = useTranslations('template.hub.overview.hero');
  const tStatus = useTranslations('template.hub.status');
  const { formatDate } = useFormattedDates();

  return (
    <div>
      <Badge>{tStatus(template.status.toLowerCase())}</Badge>
      <h1>{template.title}</h1>
      <p>{template.description || t('noDescription')}</p>
      <span>{t('createdAt', { date: formatDate(template.createdAt) })}</span>
      <span>{t('updatedAt', { date: formatDate(template.updatedAt) })}</span>
      <Button>{t('goToBuilder')}</Button>
    </div>
  );
}
```

#### Task 3.3: RecentActivityList.tsx

```tsx
'use client';
import { useTranslations } from 'next-intl';
import { useFormattedDates } from '@/hooks/useFormattedDates';

export function RecentActivityList({ activities }: Props) {
  const t = useTranslations('template.hub.overview.recentActivity');
  const tStatus = useTranslations('status');
  const { formatRelativeTime } = useFormattedDates();

  if (!activities.length) {
    return (
      <div>
        <h3>{t('noActivity')}</h3>
        <p>{t('noActivityDescription')}</p>
      </div>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t('title')}</CardTitle>
        <Link href="./activity">{t('viewAll')}</Link>
      </CardHeader>
      <CardContent>
        {activities.map(activity => (
          <div key={activity.id}>
            <span>{activity.candidateName}</span>
            <Badge>{tStatus(activity.status.toLowerCase())}</Badge>
            <span>{formatRelativeTime(activity.timestamp)}</span>
          </div>
        ))}
      </CardContent>
    </Card>
  );
}
```

#### Task 3.4: SharingSummaryCard.tsx

```tsx
'use client';
import { useTranslations } from 'next-intl';

export function SharingSummaryCard({ sharing }: Props) {
  const t = useTranslations('template.hub.overview.sharing');

  const handleCopyLink = async () => {
    try {
      await navigator.clipboard.writeText(sharing.shareUrl);
      toast.success(t('linkCopied'));
    } catch {
      toast.error(t('copyFailed'));
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t('title')}</CardTitle>
        <Button size="sm">{t('share')}</Button>
      </CardHeader>
      <CardContent>
        <div>
          <span>{t('peopleShared', { count: sharing.peopleCount })}</span>
          <span>{t('linksActive', { count: sharing.linksCount })}</span>
        </div>

        {sharing.shareUrl ? (
          <Button onClick={handleCopyLink}>{t('copyLink')}</Button>
        ) : (
          <p>{t('noActiveLinks')}</p>
        )}

        <Link href="./access">{t('manageAccess')}</Link>
      </CardContent>
    </Card>
  );
}
```

---

### Phase 4: Results Page

**File:** `app/(workspace)/test-templates/[id]/results/page.tsx`

```tsx
import { getTranslations } from 'next-intl/server';

export default async function ResultsPage({ params }: Props) {
  const t = await getTranslations('template.hub.results');

  return (
    <div>
      <h1>{t('title')}</h1>
      <p>{t('description')}</p>

      {/* Stats Row */}
      <div className="grid grid-cols-4 gap-4">
        <StatCard label={t('stats.total')} value={stats.total} />
        <StatCard label={t('stats.done')} value={stats.done} />
        <StatCard label={t('stats.active')} value={stats.active} />
        <StatCard label={t('stats.avgScore')} value={stats.avgScore} />
      </div>

      {/* Filters */}
      <Input placeholder={t('searchPlaceholder')} />
      <Select>
        <SelectItem value="all">{t('filters.allStatuses')}</SelectItem>
        <SelectItem value="completed">{t('filters.completed')}</SelectItem>
        <SelectItem value="in_progress">{t('filters.inProgress')}</SelectItem>
        <SelectItem value="abandoned">{t('filters.abandoned')}</SelectItem>
      </Select>

      <Button variant="outline">{t('export')}</Button>
    </div>
  );
}
```

---

### Phase 5: Access & Settings Pages

#### Task 5.1: access/page.tsx

```tsx
import { getTranslations } from 'next-intl/server';

export default async function AccessPage() {
  const t = await getTranslations('template.hub.access');

  return (
    <div>
      <h1>{t('title')}</h1>
      <p>{t('description')}</p>
      <AccessPageContent />
    </div>
  );
}
```

#### Task 5.2: settings/page.tsx

```tsx
import { getTranslations } from 'next-intl/server';

export default async function SettingsPage() {
  const t = await getTranslations('template.hub.settings');

  return (
    <div>
      <h1>{t('title')}</h1>
      <p>{t('description')}</p>
      <SettingsForm template={template} />
    </div>
  );
}
```

---

## Testing & Verification

### Task 6.1: TypeScript Validation

```bash
cd frontend-app
npm run type-check
```

### Task 6.2: Visual Verification Checklist

For each component:
- [ ] Load page in English locale
- [ ] Switch to Russian locale
- [ ] Verify all strings display correctly
- [ ] Test pluralization: 0, 1, 2, 5, 21 items
- [ ] Verify relative time formatting

### Task 6.3: Unit Tests

```tsx
// Example: ConfigurationCard.test.tsx
import { renderWithIntl } from '@/tests/utils';

describe('ConfigurationCard', () => {
  it('renders English translations', () => {
    renderWithIntl(<ConfigurationCard {...props} />, { locale: 'en' });
    expect(screen.getByText('Configuration')).toBeInTheDocument();
  });

  it('renders Russian translations', () => {
    renderWithIntl(<ConfigurationCard {...props} />, { locale: 'ru' });
    expect(screen.getByText('Конфигурация')).toBeInTheDocument();
  });

  it('handles pluralization', () => {
    renderWithIntl(<ConfigurationCard {...props} competencyCount={1} />);
    expect(screen.getByText('1 competency selected')).toBeInTheDocument();

    renderWithIntl(<ConfigurationCard {...props} competencyCount={5} />);
    expect(screen.getByText('5 competencies selected')).toBeInTheDocument();
  });
});
```

---

## Execution Summary

| Phase | Tasks | Files Modified | Priority |
|-------|-------|----------------|----------|
| 1 | Message Files | 2 | HIGH |
| 2 | Shared Utilities | 1 | HIGH |
| 3 | Overview Components | 5 | MEDIUM-HIGH |
| 4 | Results Page | 1 | MEDIUM |
| 5 | Access & Settings | 2 | LOW |
| 6 | Testing | - | HIGH |

### Success Criteria

- [ ] All ~80 strings are translatable
- [ ] No hardcoded English text in UI
- [ ] Russian locale displays correctly with proper pluralization
- [ ] TypeScript compilation passes
- [ ] Unit tests pass
- [ ] Visual verification complete for both locales

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Russian pluralization errors | Use ICU MessageFormat with one/few/many/other |
| Missing translation keys | TypeScript will catch at compile time |
| Hydration mismatch | Use proper client/server component boundaries |
| Performance impact | Use selective message loading with `pick()` |

---

*Generated: 2026-01-09*
*Framework: next-intl v4.7.0*
*Languages: EN, RU*
