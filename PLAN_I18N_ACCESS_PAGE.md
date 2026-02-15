# i18n Extension Plan: Test Template Access Page

## Overview

Extend internationalization support to the `test-templates/[id]/access` page, converting all hardcoded English text to bilingual (EN/RU) translations using the existing `next-intl` infrastructure.

**Scope:** ~80 translation keys across 7 component files

---

## Current State Analysis

### Already Implemented
- Page metadata uses `getTranslations('template.metadata')` in `page.tsx`
- Project has mature i18n setup with `next-intl`
- Translation files: `messages/{en,ru}.json` (~4,400 keys each)
- Existing hooks: `useEnumTranslation`, `useFormattedDates`

### Gaps Identified
- All UI text in client components is hardcoded English
- Domain helpers (`getVisibilityDisplayText`, `getPermissionDisplayText`) return English
- Date formatting in ShareLinkManager uses date-fns without locale
- Toast notifications use hardcoded strings

---

## Implementation Phases

### Phase 1: Translation Foundation

#### Task 1.1: Add `template.access.*` to `en.json`

Add the following namespace structure:

```json
{
  "template": {
    "access": {
      "title": "Access",
      "description": "Manage who can view and use this template",

      "visibility": {
        "title": "Visibility",
        "description": "Control who can discover and access this template",
        "options": {
          "private": "Private",
          "privateDesc": "Only you and people you share with can access",
          "public": "Public",
          "publicDesc": "Anyone in the organization can view and use",
          "link": "Anyone with link",
          "linkDesc": "Anyone with the link can access (anonymous allowed)"
        },
        "revokeDialog": {
          "title": "Revoke Active Share Links?",
          "message": "Changing visibility from Link will revoke {count, plural, =1 {# active share link} other {all # active share links}}.",
          "warning": "Anyone with these links will no longer be able to access this template.",
          "confirm": "Revoke & Change"
        }
      },

      "people": {
        "title": "People with access",
        "description": "Share this template with specific users or teams",
        "form": {
          "emailPlaceholder": "Enter email address",
          "addButton": "Add",
          "validation": {
            "invalidEmail": "Valid email required"
          }
        },
        "list": {
          "usersHeader": "{count, plural, =0 {No users} =1 {# User} other {# Users}}",
          "teamsHeader": "{count, plural, =0 {No teams} =1 {# Team} other {# Teams}}",
          "unknownUser": "Unknown User",
          "unknownTeam": "Unknown Team",
          "teamBadge": "Team",
          "expiredBadge": "Expired"
        },
        "emptyState": {
          "title": "No users or teams have access",
          "hint": "Share this template with users or teams using the form above"
        },
        "removeDialog": {
          "title": "Remove Access",
          "message": "Are you sure you want to remove access for {name}? They will no longer be able to access this template.",
          "confirm": "Remove Access"
        }
      },

      "links": {
        "title": "Share Links",
        "description": "Create shareable links that don't require adding specific users",
        "subheader": "Share Links",
        "usage": "{active} of {max} links used",
        "createButton": "Create Link",
        "form": {
          "permissionLabel": "Permission",
          "permissionDesc": "What can link users do?",
          "expiresLabel": "Expires In",
          "expiresDesc": "Expires {date}",
          "maxUsesLabel": "Max Uses (Optional)",
          "maxUsesPlaceholder": "Unlimited",
          "maxUsesDesc": "0 = unlimited uses",
          "labelLabel": "Label (Optional)",
          "labelPlaceholder": "e.g., Interview candidates",
          "labelDesc": "Help identify this link",
          "cancelButton": "Cancel",
          "submitButton": "Create Link"
        },
        "expiration": {
          "day1": "1 day",
          "day7": "7 days",
          "day14": "14 days",
          "day30": "30 days",
          "day90": "90 days",
          "year1": "1 year"
        },
        "listItem": {
          "usageDisplay": "{count}{max, select, 0 {} other { / {max}}} uses",
          "expired": "Expired",
          "expires": "Expires {time}"
        },
        "emptyState": {
          "title": "No active share links",
          "hint": "Create a link to share this template without adding specific users"
        },
        "limitWarning": "Maximum link limit reached. Revoke existing links to create new ones.",
        "revokeDialog": {
          "title": "Revoke Share Link",
          "message": "Are you sure you want to revoke this link? Anyone with this link will no longer be able to access the template.",
          "confirm": "Revoke Link"
        }
      },

      "toast": {
        "sharedWith": "Shared with {email}",
        "shareFailed": "Failed to share with user",
        "permissionUpdated": "Permission updated",
        "updateFailed": "Failed to update permission",
        "accessRemoved": "Removed access for {name}",
        "revokeFailed": "Failed to revoke access",
        "linkCreated": "Share link created",
        "linkCreateFailed": "Failed to create link",
        "linkRevoked": "Link revoked",
        "linkRevokeFailed": "Failed to revoke link"
      }
    }
  }
}
```

#### Task 1.2: Add Russian translations to `ru.json`

Key considerations:
- Use Cyrillic characters properly encoded
- Russian pluralization uses 3 forms: `one`, `few`, `many`

```json
{
  "template": {
    "access": {
      "title": "Доступ",
      "description": "Управление доступом к шаблону",

      "visibility": {
        "title": "Видимость",
        "description": "Контролируйте, кто может обнаружить и получить доступ к этому шаблону",
        "options": {
          "private": "Приватный",
          "privateDesc": "Доступ только у вас и тех, с кем вы поделились",
          "public": "Публичный",
          "publicDesc": "Любой в организации может просматривать и использовать",
          "link": "По ссылке",
          "linkDesc": "Любой со ссылкой может получить доступ (анонимно)"
        },
        "revokeDialog": {
          "title": "Отозвать активные ссылки?",
          "message": "Изменение видимости отзовёт {count, plural, one {# активную ссылку} few {# активные ссылки} many {# активных ссылок} other {# активных ссылок}}.",
          "warning": "Пользователи с этими ссылками больше не смогут получить доступ к шаблону.",
          "confirm": "Отозвать и изменить"
        }
      },

      "people": {
        "title": "Люди с доступом",
        "description": "Поделитесь шаблоном с конкретными пользователями или командами",
        "form": {
          "emailPlaceholder": "Введите email адрес",
          "addButton": "Добавить",
          "validation": {
            "invalidEmail": "Введите корректный email"
          }
        },
        "list": {
          "usersHeader": "{count, plural, =0 {Нет пользователей} one {# пользователь} few {# пользователя} many {# пользователей} other {# пользователей}}",
          "teamsHeader": "{count, plural, =0 {Нет команд} one {# команда} few {# команды} many {# команд} other {# команд}}",
          "unknownUser": "Неизвестный пользователь",
          "unknownTeam": "Неизвестная команда",
          "teamBadge": "Команда",
          "expiredBadge": "Истёк"
        },
        "emptyState": {
          "title": "Нет пользователей или команд с доступом",
          "hint": "Поделитесь шаблоном с помощью формы выше"
        },
        "removeDialog": {
          "title": "Удалить доступ",
          "message": "Вы уверены, что хотите удалить доступ для {name}? Они больше не смогут получить доступ к этому шаблону.",
          "confirm": "Удалить доступ"
        }
      },

      "links": {
        "title": "Ссылки доступа",
        "description": "Создавайте ссылки без добавления конкретных пользователей",
        "subheader": "Ссылки доступа",
        "usage": "Использовано {active} из {max} ссылок",
        "createButton": "Создать ссылку",
        "form": {
          "permissionLabel": "Разрешение",
          "permissionDesc": "Что могут делать пользователи по ссылке?",
          "expiresLabel": "Истекает через",
          "expiresDesc": "Истекает {date}",
          "maxUsesLabel": "Макс. использований (опционально)",
          "maxUsesPlaceholder": "Без ограничений",
          "maxUsesDesc": "0 = неограниченно",
          "labelLabel": "Метка (опционально)",
          "labelPlaceholder": "напр., Кандидаты на собеседование",
          "labelDesc": "Поможет идентифицировать ссылку",
          "cancelButton": "Отмена",
          "submitButton": "Создать ссылку"
        },
        "expiration": {
          "day1": "1 день",
          "day7": "7 дней",
          "day14": "14 дней",
          "day30": "30 дней",
          "day90": "90 дней",
          "year1": "1 год"
        },
        "listItem": {
          "usageDisplay": "{count}{max, select, 0 {} other { / {max}}} использований",
          "expired": "Истёк",
          "expires": "Истекает {time}"
        },
        "emptyState": {
          "title": "Нет активных ссылок",
          "hint": "Создайте ссылку для доступа без добавления конкретных пользователей"
        },
        "limitWarning": "Достигнут лимит ссылок. Отзовите существующие ссылки для создания новых.",
        "revokeDialog": {
          "title": "Отозвать ссылку",
          "message": "Вы уверены, что хотите отозвать эту ссылку? Пользователи с этой ссылкой больше не смогут получить доступ к шаблону.",
          "confirm": "Отозвать ссылку"
        }
      },

      "toast": {
        "sharedWith": "Доступ предоставлен {email}",
        "shareFailed": "Не удалось предоставить доступ",
        "permissionUpdated": "Разрешение обновлено",
        "updateFailed": "Не удалось обновить разрешение",
        "accessRemoved": "Доступ удалён для {name}",
        "revokeFailed": "Не удалось отозвать доступ",
        "linkCreated": "Ссылка создана",
        "linkCreateFailed": "Не удалось создать ссылку",
        "linkRevoked": "Ссылка отозвана",
        "linkRevokeFailed": "Не удалось отозвать ссылку"
      }
    }
  }
}
```

#### Task 1.3: Add/Verify `enums.permission` keys

```json
// en.json
"enums": {
  "permission": {
    "view": "Viewer",
    "edit": "Editor",
    "manage": "Manager",
    "view_description": "Can view and take tests",
    "edit_description": "Can modify test content",
    "manage_description": "Full control including sharing"
  }
}

// ru.json
"enums": {
  "permission": {
    "view": "Просмотр",
    "edit": "Редактор",
    "manage": "Управляющий",
    "view_description": "Может просматривать и проходить тесты",
    "edit_description": "Может изменять содержимое теста",
    "manage_description": "Полный контроль, включая доступ"
  }
}
```

#### Task 1.4: Add/Verify `enums.visibility` keys

```json
// en.json
"enums": {
  "visibility": {
    "private": "Private",
    "public": "Public",
    "link": "Anyone with link"
  }
}

// ru.json
"enums": {
  "visibility": {
    "private": "Приватный",
    "public": "Публичный",
    "link": "По ссылке"
  }
}
```

---

### Phase 2: Component Modifications

#### Task 2.1: `page.tsx` - Server Component

**File:** `app/(workspace)/test-templates/[id]/access/page.tsx`

```tsx
// Extend existing getTranslations usage
const t = await getTranslations('template.access');

// Replace hardcoded strings:
// Line 78: "Access" → {t('title')}
// Line 79-81: description → {t('description')}
```

#### Task 2.2: `AccessPageContent.tsx` - Client Component

**File:** `app/(workspace)/test-templates/[id]/access/AccessPageContent.tsx`

```tsx
'use client';
import { useTranslations } from 'next-intl';

export function AccessPageContent({ template }: Props) {
  const t = useTranslations('template.access');

  return (
    <PageHeader
      title={t('title')}
      description={t('description')}
    />
    // ...
  );
}
```

#### Task 2.3: VisibilitySection + VisibilitySelector

**Files:**
- `VisibilitySection.tsx`
- `VisibilitySelector.tsx`

```tsx
'use client';
import { useTranslations } from 'next-intl';

function VisibilitySection() {
  const t = useTranslations('template.access.visibility');

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t('title')}</CardTitle>
        <CardDescription>{t('description')}</CardDescription>
      </CardHeader>
      {/* ... */}
    </Card>
  );
}

function VisibilitySelector() {
  const t = useTranslations('template.access.visibility');

  // Use t('options.private'), t('options.privateDesc'), etc.
  // Use t('revokeDialog.title'), t('revokeDialog.message', { count }), etc.
}
```

#### Task 2.4: PeopleSection + UserShareList

**Files:**
- `PeopleSection.tsx`
- `UserShareList.tsx` (or inline component)

```tsx
'use client';
import { useTranslations } from 'next-intl';

function PeopleSection() {
  const t = useTranslations('template.access.people');
  const tToast = useTranslations('template.access.toast');

  // Form: t('form.emailPlaceholder'), t('form.addButton')
  // Toast: tToast('sharedWith', { email }), tToast('shareFailed')
  // List: t('list.usersHeader', { count }), t('list.teamsHeader', { count })
  // Empty: t('emptyState.title'), t('emptyState.hint')
  // Dialog: t('removeDialog.title'), t('removeDialog.message', { name })
}
```

#### Task 2.5: LinksSection + ShareLinkManager

**Files:**
- `LinksSection.tsx`
- `ShareLinkManager.tsx`

```tsx
'use client';
import { useTranslations } from 'next-intl';
import { useFormattedDates } from '@/hooks/useFormattedDates';

function ShareLinkManager() {
  const t = useTranslations('template.access.links');
  const tToast = useTranslations('template.access.toast');
  const { formatRelativeTime, formatDateTime } = useFormattedDates();

  // Section: t('title'), t('description')
  // Counter: t('usage', { active: activeCount, max: maxLinks })
  // Form labels: t('form.permissionLabel'), t('form.expiresLabel'), etc.
  // Expiration options: t('expiration.day1'), t('expiration.day7'), etc.
  // Date display: formatRelativeTime(expiresAt) instead of date-fns format
  // Empty state: t('emptyState.title'), t('emptyState.hint')
  // Limit warning: t('limitWarning')
  // Toast: tToast('linkCreated'), tToast('linkRevoked'), etc.
}
```

#### Task 2.6: PermissionSelect Component

```tsx
'use client';
import { useEnumTranslation } from '@/hooks/useEnumTranslation';

function PermissionSelect({ value, onChange }) {
  const { getSelectOptions } = useEnumTranslation<Permission>('permission');
  const options = getSelectOptions();

  return (
    <Select value={value} onValueChange={onChange}>
      {options.map(opt => (
        <SelectItem key={opt.value} value={opt.value}>
          {opt.label}
        </SelectItem>
      ))}
    </Select>
  );
}
```

---

### Phase 3: Domain Layer Integration

#### Task 3.1: Refactor `getVisibilityDisplayText`

**Current (in `src/types/domain.ts`):**
```typescript
export function getVisibilityDisplayText(visibility: Visibility): string {
  return {
    private: 'Private',
    public: 'Public',
    link: 'Anyone with link'
  }[visibility];
}
```

**Option A: Create React hook (recommended)**
```typescript
// src/hooks/useVisibilityTranslation.ts
import { useEnumTranslation } from './useEnumTranslation';

export function useVisibilityTranslation() {
  return useEnumTranslation<Visibility>('visibility');
}
```

**Option B: Keep helper + pass translator**
```typescript
export function getVisibilityDisplayText(
  visibility: Visibility,
  t: (key: string) => string
): string {
  return t(visibility);
}
```

#### Task 3.2: Refactor `getPermissionDisplayText`

Same pattern as visibility - use `useEnumTranslation<Permission>('permission')`.

#### Task 3.3: Date Formatting with Locale

Replace all `format()` calls from date-fns with `useFormattedDates()` hook:

```tsx
// Before
import { format, formatDistanceToNow } from 'date-fns';
const expires = format(expiresAt, 'MMM d, yyyy');
const relative = formatDistanceToNow(expiresAt);

// After
import { useFormattedDates } from '@/hooks/useFormattedDates';
const { formatDateTime, formatRelativeTime } = useFormattedDates();
const expires = formatDateTime(expiresAt);
const relative = formatRelativeTime(expiresAt);
```

---

### Phase 4: Testing & Verification

#### Task 4.1: Run i18n Test Suite

```bash
cd frontend-app
npm run test -- src/__tests__/i18n/
```

Tests should verify:
- All new keys exist in both locales
- No empty values
- Interpolation placeholders match between languages
- Pluralization forms are correct

#### Task 4.2: Manual EN Locale Testing

1. Set browser to English
2. Navigate to `/test-templates/[id]/access`
3. Verify all sections display English text
4. Test all interactive elements (buttons, toasts, dialogs)
5. Test form validation messages
6. Verify date formatting

#### Task 4.3: Manual RU Locale Testing

1. Switch language via language switcher
2. Navigate to `/test-templates/[id]/access`
3. Verify Cyrillic displays correctly
4. Test pluralization:
   - 1 user → "1 пользователь"
   - 2 users → "2 пользователя"
   - 5 users → "5 пользователей"
5. Verify date formatting in Russian

#### Task 4.4: Edge Case Testing

- [ ] Empty state (no shares, no links)
- [ ] Maximum links limit reached
- [ ] Expired links display
- [ ] Long names truncation
- [ ] Network error toasts
- [ ] Revoke confirmation dialogs

---

## Files Modified Summary

| File | Type | Changes |
|------|------|---------|
| `messages/en.json` | Translation | +80 keys |
| `messages/ru.json` | Translation | +80 keys |
| `page.tsx` | Server Component | Extend `getTranslations` |
| `AccessPageContent.tsx` | Client Component | Add `useTranslations` |
| `VisibilitySection.tsx` | Client Component | i18n integration |
| `VisibilitySelector.tsx` | Client Component | i18n integration |
| `PeopleSection.tsx` | Client Component | i18n integration |
| `UserShareList.tsx` | Client Component | i18n integration |
| `LinksSection.tsx` | Client Component | i18n integration |
| `ShareLinkManager.tsx` | Client Component | i18n integration |
| `src/types/domain.ts` | Domain | Refactor helpers |
| `src/hooks/useVisibilityTranslation.ts` | Hook | New file (optional) |

---

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Russian pluralization incorrect | Medium | Test with values 1, 2, 5, 11, 21 |
| Breaking domain helpers | High | Search for all usages before refactoring |
| Missing interpolation | Low | i18n tests catch placeholder mismatches |
| Performance (bundle size) | Low | next-intl tree-shakes unused namespaces |

---

## Success Criteria

- [ ] Zero hardcoded English strings in access page
- [ ] All 80+ keys have both EN and RU versions
- [ ] Russian pluralization works correctly (one/few/many)
- [ ] Date formatting respects user locale
- [ ] All toast messages are translated
- [ ] Existing i18n tests pass
- [ ] No visual regressions in either locale

---

**Created:** 2026-01-09
**Author:** Claude (Principal Architect)
**Status:** Ready for Implementation
