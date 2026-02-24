# SkillSoft Design System

> Single source of truth for visual and interaction design decisions.
> Canonical references: `/profile` (Document mode), `/dashboard` (Dashboard mode).

## Design Philosophy

SkillSoft uses a **flat foundation with contextual elevation** — Linear/Notion at its core, enhanced with subtle depth and color where data density demands it.

- **Flat foundation** — borders over shadows, content over chrome, information over decoration
- **Content-dense** — pages read like database views, not marketing sites
- **Scannable hierarchy** — uppercase section labels, property-row grids, tight spacing
- **Warm minimalism** — OKLCH color system with subtle warm tints (hue ~85-90)
- **Contextual elevation** — shadows, gradients, and motion only where they serve function
- **Progressive disclosure** — show essentials first, reveal detail on interaction

### Page Modes

The design system operates in two modes sharing a common foundation:

| Mode | Pages | Container | Cards | Shadows |
|------|-------|-----------|-------|---------|
| **Document** | Profile, Settings | `max-w-4xl` single column | Flat (`shadow-none`) | None on cards |
| **Dashboard** | Dashboard, Analytics | `max-w-[1600px]` bento grid | Elevated (`shadow-sm`) | `hover:shadow-md` on widgets |

**Document mode** feels like a Notion page — flat cards, tight spacing, pure content.
**Dashboard mode** feels like Linear's overview — widget cards with subtle depth, colored variants, animated entry.

---

## Color System

### Core Palette

| Token | Light | Dark | Usage |
|-------|-------|------|-------|
| `--background` | `oklch(0.98 0.003 90)` | `oklch(0.145 0 0)` | Page background |
| `--foreground` | `oklch(0.25 0.01 264)` | `oklch(0.985 0 0)` | Primary text |
| `--card` | `oklch(0.985 0.002 85)` | `oklch(0.205 0 0)` | Card surfaces |
| `--border` | `oklch(0.9 0.006 70)` | `oklch(1 0 0 / 10%)` | All borders |
| `--primary` | `oklch(0.35 0.02 264)` | `oklch(0.922 0 0)` | CTAs, links |
| `--muted` | `oklch(0.94 0.004 80)` | `oklch(0.269 0 0)` | Subtle backgrounds |
| `--muted-foreground` | `oklch(0.48 0.015 260)` | `oklch(0.708 0 0)` | Secondary text |
| `--destructive` | `oklch(0.58 0.18 27)` | `oklch(0.704 0.191 22.216)` | Errors |

All tokens defined in `frontend-app/app/globals.css` using OKLCH color space.

### Semantic Color Assignments

**Role badges** (from `ProfileBadges.tsx`):

| Role | Background | Text | Border |
|------|-----------|------|--------|
| Admin | `bg-amber-100 dark:bg-amber-900/30` | `text-amber-700 dark:text-amber-400` | `border-amber-200 dark:border-amber-800` |
| Editor | `bg-violet-100 dark:bg-violet-900/30` | `text-violet-700 dark:text-violet-400` | `border-violet-200 dark:border-violet-800` |
| User | `bg-emerald-100 dark:bg-emerald-900/30` | `text-emerald-700 dark:text-emerald-400` | `border-emerald-200 dark:border-emerald-800` |

**Big Five personality traits** (from `PersonalityPassportCard.tsx`):

| Trait | Bar | Text | Background |
|-------|-----|------|------------|
| Openness | `bg-violet-500` | `text-violet-600` | `bg-violet-100` |
| Conscientiousness | `bg-blue-500` | `text-blue-600` | `bg-blue-100` |
| Extraversion | `bg-amber-500` | `text-amber-600` | `bg-amber-100` |
| Agreeableness | `bg-emerald-500` | `text-emerald-600` | `bg-emerald-100` |
| Emotional Stability | `bg-cyan-500` | `text-cyan-600` | `bg-cyan-100` |

**Score ranges** (from `TopCompetenciesCard.tsx`):

| Range | Text | Bar |
|-------|------|-----|
| 80-100 (excellent) | `text-emerald-600 dark:text-emerald-400` | `bg-emerald-500` |
| 60-79 (good) | `text-blue-600 dark:text-blue-400` | `bg-blue-500` |
| 0-59 (needs work) | `text-amber-600 dark:text-amber-400` | `bg-amber-500` |

### Dark Mode Convention

Every named-palette class MUST pair with a `dark:` variant:

```
bg-{color}-100     → dark:bg-{color}-900/30
text-{color}-700   → dark:text-{color}-400
border-{color}-200 → dark:border-{color}-800
```

### Gradients

Gradients are **accent only** — never on standard content cards.

| Use case | Pattern |
|----------|---------|
| CTA / promotional card | `bg-linear-to-br from-primary/5 via-transparent to-purple-500/5` |
| Score visualization | `bg-linear-to-br from-emerald-500 to-emerald-600` |
| Decorative glow | `absolute opacity-30 blur-3xl bg-{color}-500/20` (positioned behind content) |

### Rules

- DO use semantic tokens (`text-muted-foreground`, `bg-card`) for theme-aware surfaces
- DO NOT hardcode hex/rgb — use OKLCH via CSS custom properties or Tailwind palette
- DO pair all named-palette classes with `dark:` variants
- DO NOT use gradients on standard content cards — flat backgrounds only

---

## Typography

### Font Stack

- **Primary**: Inter (latin + cyrillic subsets), CSS variable `--font-inter`
- **OpenType features**: `"cv02", "cv03", "cv04", "cv11"`
- **Rendering**: `-webkit-font-smoothing: antialiased`

### Type Scale

| Role | Classes | Size | Usage |
|------|---------|------|-------|
| Dashboard heading | `text-xl sm:text-2xl md:text-3xl font-bold tracking-tight` | 20/24/30px | Dashboard page title |
| Page title | `text-lg sm:text-xl font-semibold tracking-tight` | 18/20px | User name, document page headers |
| Card title | `text-base font-semibold leading-none` | 16px | Dashboard widget titles |
| Section header | `text-xs font-medium uppercase tracking-wider text-muted-foreground` | 12px | Document section labels |
| Body primary | `text-sm` | 14px | Labels, list items, descriptions |
| Body secondary | `text-xs text-muted-foreground` | 12px | Timestamps, metadata |
| Body minimum | `text-xs-safe` | `max(0.6875rem, 11px)` | Badge text, smallest readable (WCAG AA) |
| Stat value | `text-2xl font-bold tracking-tight tabular-nums` | 24px | Widget numbers, large metrics |
| Scores/numbers | `font-bold tabular-nums` | inherit | Percentages, counts |
| Micro label | `text-[10px]` | 10px | Avatar initials, tiny badges |

### Rules

- `tracking-tight` on headings `text-lg` and above
- `tracking-wider` with `uppercase` section labels
- `tabular-nums` on any numerical data for column alignment
- `truncate` on user-generated text inside flex layouts

---

## Spacing & Layout

### Page Layout

**Document mode** (profile, settings):
```
Container:  mx-auto max-w-4xl
Padding:    px-4 sm:px-6 py-6 sm:py-8
Card stack: space-y-3
```

**Dashboard mode** (dashboard, analytics):
```
Container:  mx-auto max-w-[1600px] w-full
Padding:    p-4 sm:p-6 lg:p-8
Section gap: gap-6 sm:gap-8
Bento grid:  grid-cols-2 md:grid-cols-8 lg:grid-cols-12 gap-2.5 sm:gap-3
```

### Spacing Tokens

| Element | Classes | Value |
|---------|---------|-------|
| Section internal padding | `py-6` | 24px |
| Section header → content | `mb-4` | 16px |
| Content rows | `space-y-2` | 8px |
| Icon + text inline | `gap-1.5 sm:gap-2` | 6/8px |
| Flex row items | `gap-2 sm:gap-3` | 8/12px |
| Avatar → info | `gap-4` | 16px |
| Property grid columns | `gap-y-3 gap-x-8` | 12/32px |

### Grid Patterns

```tsx
// Property grid (key-value pairs)
<dl className="grid grid-cols-1 sm:grid-cols-2 gap-y-3 gap-x-8">

// Stats grid
<div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
```

---

## Card & Section Pattern

### Document Card (flat)

Every content section on document-style pages:

```tsx
<Card className="gap-0 py-0 rounded-lg shadow-none">
  <CardContent>
    <section className="py-6" role="region" aria-label="Section Name">
      <h2 className="text-xs font-medium uppercase tracking-wider text-muted-foreground mb-4">
        SECTION TITLE
      </h2>
      {/* Content */}
    </section>
  </CardContent>
</Card>
```

**Why we override the default Card:**
- Default shadcn Card: `gap-6 py-6 rounded-xl shadow-sm`
- Our override: `gap-0 py-0 rounded-lg shadow-none`
- Section's `py-6` handles vertical rhythm; Card is just a border container
- `shadow-none` creates a flat, document-like feel

### Section with Header Action

```tsx
<section className="py-6" role="region" aria-label={t('sectionName')}>
  <div className="flex items-center justify-between mb-1">
    <h2 className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
      SECTION TITLE
    </h2>
    <Button
      variant="ghost"
      size="sm"
      asChild
      className="text-xs h-8 px-2 text-muted-foreground hover:text-foreground min-h-[44px] sm:min-h-0 touch-manipulation group"
    >
      <Link href="/destination">
        View all
        <ChevronRight className="h-3.5 w-3.5 ml-0.5 transition-transform group-hover:translate-x-0.5" />
      </Link>
    </Button>
  </div>
  <p className="text-xs text-muted-foreground mb-3">Subtitle text</p>
  {/* Content */}
</section>
```

### Dashboard Card (elevated)

Widget cards on dashboard/analytics pages use default shadcn styling with hover elevation:

```tsx
<Card className="hover:shadow-md hover:border-primary/30 hover:-translate-y-px transition-all duration-200">
  <CardHeader className="pb-2">
    <div className="flex items-center gap-3">
      <div className="w-9 h-9 rounded-lg bg-muted flex items-center justify-center">
        <Icon className="w-4 h-4 text-muted-foreground" />
      </div>
      <CardTitle>{title}</CardTitle>
    </div>
  </CardHeader>
  <CardContent>
    {/* Widget content */}
  </CardContent>
</Card>
```

### Tinted Card (emphasis)

For highlighted states (pending assessments, active alerts):

```tsx
<Card className="border-emerald-200 dark:border-emerald-800 bg-emerald-50/50 dark:bg-emerald-950/20">
  {/* Colored border + tinted background */}
</Card>
```

### CTA Card (promotional)

For call-to-action sections, gradients are allowed:

```tsx
<Card className="bg-linear-to-br from-primary/5 via-transparent to-purple-500/5 border-primary/20">
  {/* Gradient background for visual emphasis */}
</Card>
```

### Do / Don't

| Do | Don't |
|----|-------|
| `shadow-none` on Document mode cards | Use `shadow-sm` on document/profile pages |
| `shadow-sm` + `hover:shadow-md` on Dashboard widgets | Use heavy shadows (`shadow-lg`, `shadow-xl`) |
| `rounded-lg` (8px) for Document cards | Use `rounded-xl` on Document pages |
| `rounded-xl` (12px) for Dashboard widgets | Use `rounded-lg` on Dashboard cards |
| Tinted backgrounds at `/50` opacity | Full-saturation colored card backgrounds |
| Section `h2` with `text-xs uppercase` (Document) | `CardTitle` on Document pages |
| `CardTitle` for Dashboard widget headers | Uppercase section labels on Dashboard |
| `border-border` (Card default) | Remove borders entirely |

---

## Widget Variants

Dashboard widgets use a 5-variant color system for semantic meaning:

| Variant | Border | Background | Icon bg | Icon text |
|---------|--------|-----------|---------|-----------|
| default | `border-border` | — | `bg-muted` | `text-muted-foreground` |
| success | `border-emerald-200 dark:border-emerald-800` | `bg-emerald-50/50 dark:bg-emerald-950/20` | `bg-emerald-100 dark:bg-emerald-900/30` | `text-emerald-600 dark:text-emerald-400` |
| warning | `border-amber-200 dark:border-amber-800` | `bg-amber-50/50 dark:bg-amber-950/20` | `bg-amber-100 dark:bg-amber-900/30` | `text-amber-600 dark:text-amber-400` |
| info | `border-blue-200 dark:border-blue-800` | `bg-blue-50/50 dark:bg-blue-950/20` | `bg-blue-100 dark:bg-blue-900/30` | `text-blue-600 dark:text-blue-400` |
| destructive | `border-red-200 dark:border-red-800` | `bg-red-50/50 dark:bg-red-950/20` | `bg-red-100 dark:bg-red-900/30` | `text-red-600 dark:text-red-400` |

Use variants to communicate state at a glance — green for healthy metrics, amber for warnings, etc.

---

## Icon Containers

Dashboard widgets prefix each card with a rounded-square icon container:

```tsx
// Default variant
<div className="w-9 h-9 rounded-lg bg-muted flex items-center justify-center">
  <Icon className="w-4 h-4 text-muted-foreground" />
</div>

// Colored variant (success example)
<div className="w-9 h-9 rounded-lg bg-emerald-100 dark:bg-emerald-900/30 flex items-center justify-center">
  <Icon className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
</div>
```

**Mobile size**: `w-8 h-8` (32px) instead of `w-9 h-9` (36px).

Icon containers are used on **Dashboard mode only**. Document mode uses inline icons without containers.

---

## Interactive States

### Hover

| Element | Classes | Mode |
|---------|---------|------|
| List row / trait bar | `hover:bg-muted/50 transition-colors` | Both |
| Bordered result card | `hover:bg-muted/40 transition-colors` | Document |
| Ghost button | `hover:text-foreground hover:bg-background/50` | Both |
| Chevron arrow | `group-hover:translate-x-0.5 transition-transform` | Both |
| Dashboard widget | `hover:shadow-md hover:border-primary/30 hover:-translate-y-px transition-all duration-200` | Dashboard |
| Action row | `hover:translate-x-0.5 active:scale-[0.99] transition-all duration-200` | Dashboard |
| Reveal-on-hover icon | `opacity-0 group-hover:opacity-100 transition-opacity` | Dashboard |

### Focus

All focusable elements:

```
focus-visible:ring-2 focus-visible:ring-primary/20 focus-visible:ring-offset-2
```

### Active / Press

```
active:scale-[0.98]
```

Always pair with `touch-manipulation` on interactive elements.

### Transitions & Animation

Three animation systems, strictly delineated:

| System | Purpose | Usage |
|--------|---------|-------|
| CSS transitions | Hover, focus, micro-interactions | Both modes |
| tw-animate-css | Radix `data-[state=*]` transitions | Dialog/Sheet open/close |
| Framer Motion (`motion/react`) | Widget entrance, gauges, stagger | Dashboard mode |

**CSS transitions** (lightweight state changes):

| Type | Classes |
|------|---------|
| Color changes | `transition-colors duration-200` |
| All properties | `transition-all duration-200` |
| Progress bars | `transition-all duration-500` |
| Empty state stagger | `animate-in fade-in-0 slide-in-from-bottom-2 duration-300 delay-{75,100,150}` |

**Framer Motion** (dashboard widget entrance):

```tsx
// Widget fade-in with vertical movement
const fadeInUp = {
  hidden: { opacity: 0, y: 16 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.4, ease: "easeOut" } }
};

// Staggered children cascade
const staggerContainer = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: { staggerChildren: 0.06, delayChildren: 0.08 }
  }
};
```

Always add `motion-reduce:transition-none` for accessibility on animated elements.

---

## Responsive Strategy

### Breakpoints

| Breakpoint | Width | Usage |
|------------|-------|-------|
| (default) | 0-479px | Mobile base (320px minimum) |
| `xs:` | 480px+ | Custom breakpoint (`--breakpoint-xs`) |
| `sm:` | 640px+ | **Primary** — most layout shifts happen here |
| `md:` | 768px+ | Tablet adjustments |
| `lg:` | 1024px+ | Desktop (rarely used; max-w-4xl caps here) |

### Mobile-First Patterns

Always write mobile first (unprefixed), then enhance with `sm:`:

```tsx
// Layout shifts
className="grid grid-cols-1 sm:grid-cols-2"
className="flex-col sm:flex-row"

// Spacing increases
className="px-4 sm:px-6"
className="gap-1.5 sm:gap-2"

// Typography scaling
className="text-lg sm:text-xl"

// Visibility toggles
className="hidden sm:inline"           // Desktop-only label
className="sm:hidden"                  // Mobile-only element

// Touch targets (relax on desktop)
className="min-h-[44px] sm:min-h-0"
```

### Responsive Catalog

| Pattern | Mobile | Desktop |
|---------|--------|---------|
| Badge text | Icon-only (`sr-only` label) | Icon + visible label |
| Action buttons | Full-width (`w-full`) | Auto-width |
| Property grid | 1 column, justify-between | 2 columns, fixed dt width |
| Tab filters | Horizontal scroll + fade edges | Inline fit |
| "View All" link | Full-width button below | Ghost button in header |

---

## Accessibility

### WCAG 2.1 AAA Targets

| Requirement | Implementation |
|-------------|---------------|
| Touch targets | `min-h-[44px]` on mobile for all interactive elements |
| Minimum font | `text-xs-safe` = `max(0.6875rem, 11px)` |
| Color contrast | OKLCH tokens tuned for 4.5:1 minimum |
| Zoom support | Viewport `maximumScale: 5, userScalable: true` |
| Keyboard nav | `focus-visible:ring-2` on all focusable elements |
| Screen readers | `sr-only` labels, `aria-hidden` on decorative icons |
| Motion | `prefers-reduced-motion` respected via tw-animate-css |

### Section ARIA

```tsx
<section role="region" aria-label="Descriptive section name">
```

### List ARIA

```tsx
<div role="list" aria-label="List name">
  <div role="listitem">...</div>
</div>
```

### Icon Rules

- Decorative: `aria-hidden="true"` on the icon element
- Icon-only buttons: `aria-label="Action description"` on `<Button>`
- Responsive labels: `<span className="sr-only sm:not-sr-only">{label}</span>`

---

## Component Usage

### shadcn/ui Overrides

| Component | Default | Our Override | When |
|-----------|---------|-------------|------|
| Card | `gap-6 py-6 rounded-xl shadow-sm` | `gap-0 py-0 rounded-lg shadow-none` | Document mode pages |
| Card | `gap-6 py-6 rounded-xl shadow-sm` | Use default + add hover elevation | Dashboard mode pages |
| Badge | `rounded-full` | No override | Always |
| Button (ghost) | Standard | Add `min-h-[44px] touch-manipulation` | Mobile contexts |
| Button (outline) | Standard | Add `active:scale-[0.98]` | Primary actions |
| Progress | Standard | `className="h-1.5"` | Profile completeness |
| Tooltip | Standard | `delayDuration={300}` | Hover details |
| Skeleton | Standard | Match exact loaded-content layout | Always |

### Preferred Components

| Need | Use | Not |
|------|-----|-----|
| Navigation link | `Button asChild` + `<Link>` | Raw `<a>` |
| Status indicator | `Badge` with semantic colors | Colored text alone |
| Modal (responsive) | `ResponsiveModal` | `Dialog` or `Sheet` alone |
| Notification | `Sonner` (toast) | `Alert` |
| Key-value data | `<dl>` grid with `dt`/`dd` | `<Table>` for pairs |

### Icon Guidelines

- Library: **Lucide React** (exclusive)
- Inline: `h-3.5 w-3.5` (14px)
- Button: `h-4 w-4` (16px)
- Section/feature: `h-5 w-5` (20px)
- Always add `shrink-0` in flex layouts
- Always add `aria-hidden="true"` on decorative icons

---

## Border Radius & Shadows

### Radius

| Element | Class | Value | Mode |
|---------|-------|-------|------|
| Cards (document) | `rounded-lg` | 8px | Document |
| Cards (dashboard) | `rounded-xl` | 12px | Dashboard |
| Icon containers | `rounded-lg` | 8px | Dashboard |
| Buttons | `rounded-md` | 6px | Both |
| Badges | `rounded-full` | pill | Both |
| Progress bars | `rounded-full` | pill | Both |
| Competency rows | `rounded-xl` | 12px | Document |
| Avatars | `rounded-full` | circle | Both |
| Empty state icons | `rounded-2xl` | 16px | Both |

### Shadows

| Level | Class | Usage |
|-------|-------|-------|
| None | `shadow-none` | Document-style cards |
| Extra small | `shadow-xs` | Outline buttons |
| Small | `shadow-sm` | Dashboard cards (resting), popovers, active tabs |
| Medium (hover) | `hover:shadow-md` | Dashboard cards (interactive hover) |

---

## Empty States

### Layout

1. Icon container: `w-16 h-16 rounded-2xl` with 30% opacity background
2. Icon: `w-8 h-8` with semantic color
3. Title: `text-base font-semibold max-w-[280px]`
4. Description: `text-sm text-muted-foreground max-w-[280px] leading-relaxed`
5. CTA button: `variant="default" size="sm" min-h-[44px] touch-manipulation`

### Animation Sequence

Staggered fade-in with increasing delay:

```tsx
className="animate-in fade-in-0 zoom-in-95 duration-300"   // Icon
className="animate-in fade-in-0 duration-300 delay-75"     // Title
className="animate-in fade-in-0 duration-300 delay-100"    // Description
className="animate-in fade-in-0 duration-300 delay-150"    // CTA
```

### Variant Colors

| Variant | Icon | Background |
|---------|------|------------|
| no-tests | `text-blue-500` | `bg-blue-100` |
| no-competencies | `text-amber-500` | `bg-amber-100` |
| no-passport | `text-purple-500` | `bg-purple-100` |
| no-results | `text-muted-foreground` | `bg-muted` |
| new-user | `text-emerald-500` | `bg-emerald-100` |

---

## Skeleton Loading

### Rules

1. Every async section MUST have a skeleton mirroring exact DOM structure
2. Skeletons prevent layout shift (CLS) by matching dimensions precisely
3. Skeleton wrappers: `aria-hidden="true"` — Suspense boundary handles accessibility

### Pattern

```tsx
export function SectionSkeleton() {
  return (
    <section className="py-6" aria-hidden="true">
      <Skeleton className="h-3 w-24 mb-4" />
      <div className="space-y-2">
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="flex items-center gap-3 py-2">
            <Skeleton className="h-4 w-28 shrink-0" />
            <Skeleton className="h-2 flex-1 rounded-full" />
            <Skeleton className="h-4 w-10 shrink-0" />
          </div>
        ))}
      </div>
    </section>
  );
}
```
