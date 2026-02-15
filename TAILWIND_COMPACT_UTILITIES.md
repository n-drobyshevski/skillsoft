# Tailwind CSS Compact Design Utilities

## Custom Utility Classes for Compact Layouts

Add these to your Tailwind configuration for reusable compact layout utilities.

### 1. Tailwind Config Extensions

```js
// tailwind.config.js
module.exports = {
  theme: {
    extend: {
      spacing: {
        // Micro spacing for compact layouts
        '0.25': '0.0625rem',  // 1px
        '0.75': '0.1875rem',  // 3px
        '1.25': '0.3125rem',  // 5px
        '2.5': '0.625rem',    // 10px
        '3.5': '0.875rem',    // 14px
      },
      fontSize: {
        // Compact text sizes
        'xxs': '0.625rem',    // 10px
        '2xs': '0.6875rem',   // 11px
      },
      height: {
        // Compact component heights
        '13': '3.25rem',      // 52px
        '15': '3.75rem',      // 60px
      },
      gap: {
        // Tight gaps
        '0.5': '0.125rem',    // 2px
        '1.5': '0.375rem',    // 6px
      }
    }
  },
  plugins: [
    // Custom compact utilities plugin
    function({ addUtilities }) {
      addUtilities({
        // Compact padding utilities
        '.p-compact': {
          padding: '0.5rem 0.75rem', // 8px 12px
        },
        '.py-compact': {
          paddingTop: '0.5rem',
          paddingBottom: '0.5rem',
        },
        '.px-compact': {
          paddingLeft: '0.75rem',
          paddingRight: '0.75rem',
        },

        // Compact spacing
        '.space-y-compact > * + *': {
          marginTop: '0.5rem',
        },
        '.gap-compact': {
          gap: '0.5rem',
        },

        // Compact border radius
        '.rounded-compact': {
          borderRadius: '0.375rem', // 6px
        },

        // Compact shadows
        '.shadow-compact': {
          boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
        },
        '.shadow-compact-md': {
          boxShadow: '0 2px 4px 0 rgba(0, 0, 0, 0.05), 0 1px 2px 0 rgba(0, 0, 0, 0.03)',
        },

        // Compact transitions
        '.transition-compact': {
          transition: 'all 150ms cubic-bezier(0.4, 0, 0.2, 1)',
        },
      })
    }
  ]
}
```

### 2. Compact Layout Patterns

#### Pattern: Inline Flex with Gap

```tsx
// Instead of: <div className="flex space-x-4">
<div className="flex items-center gap-2">
  {/* 8px gap instead of 16px */}
</div>
```

#### Pattern: Tight Grid

```tsx
// Instead of: <div className="grid gap-4">
<div className="grid grid-cols-2 md:grid-cols-4 gap-2">
  {/* 8px gap instead of 16px */}
</div>
```

#### Pattern: Compact Card

```tsx
// Instead of: <Card className="p-6">
<Card className="p-3 md:p-4">
  {/* 12-16px padding instead of 24px */}
</Card>
```

#### Pattern: Reduced Margins

```tsx
// Instead of: <div className="space-y-6">
<div className="space-y-3">
  {/* 12px gaps instead of 24px */}
</div>
```

### 3. Responsive Spacing Strategy

#### Mobile-First Tight Spacing

```tsx
// Progressive spacing - tighter on mobile, more spacious on desktop
<div className="space-y-2 md:space-y-3 lg:space-y-4">
  {/* 8px → 12px → 16px as screen grows */}
</div>

<div className="p-3 md:p-4 lg:p-5">
  {/* 12px → 16px → 20px padding */}
</div>

<div className="gap-2 md:gap-3 lg:gap-4">
  {/* 8px → 12px → 16px gap */}
</div>
```

#### Container Padding

```tsx
// Minimal container padding for more screen space
<div className="container mx-auto py-4 md:py-6 max-w-7xl">
  {/* 16px → 24px vertical padding */}
</div>
```

### 4. Typography Optimization

#### Compact Headings

```tsx
// Instead of: text-3xl md:text-4xl
<h1 className="text-xl md:text-2xl font-bold">
  {/* 20px → 24px instead of 30px → 36px */}
</h1>

// Instead of: text-lg
<h2 className="text-base md:text-lg font-semibold">
  {/* 16px → 18px instead of 18px */}
</h2>

// Instead of: text-base
<p className="text-sm md:text-base">
  {/* 14px → 16px for body text */}
</p>
```

#### Secondary Text

```tsx
// Ultra-compact secondary text
<p className="text-xs text-muted-foreground">
  {/* 12px with reduced opacity */}
</p>

// Even smaller metadata
<span className="text-xxs text-muted-foreground">
  {/* 10px for tiny labels */}
</span>
```

### 5. Component-Specific Utilities

#### Compact Badge

```tsx
<Badge className="h-5 px-2 text-xs font-medium">
  {/* 20px height, minimal padding, small text */}
  Excellent
</Badge>
```

#### Compact Button

```tsx
<Button size="sm" className="h-7 px-3 text-xs">
  {/* 28px height, tight padding */}
  Action
</Button>
```

#### Compact Icon

```tsx
// Instead of: h-5 w-5 (20px)
<Icon className="h-4 w-4" />
{/* 16px icons for compact layouts */}

// Instead of: h-6 w-6 (24px)
<Icon className="h-5 w-5" />
{/* 20px for slightly larger icons */}
```

### 6. Progress Bar Variations

#### Compact Progress

```tsx
// Instead of: h-3 (12px)
<div className="h-2 w-full bg-muted/30 rounded-full overflow-hidden">
  {/* 8px height for inline progress bars */}
  <div className="h-full bg-green-500 transition-all duration-1000" style={{ width: '75%' }} />
</div>
```

#### Micro Progress

```tsx
// Ultra-thin progress indicator
<div className="h-1 w-full bg-muted/20 rounded-full">
  {/* 4px height for subtle indicators */}
  <div className="h-full bg-primary" style={{ width: '85%' }} />
</div>
```

### 7. Card Optimization

#### Compact Card Header

```tsx
<CardHeader className="pb-2 md:pb-3">
  {/* 8px → 12px bottom padding instead of default 24px */}
  <CardTitle className="text-base md:text-lg">Title</CardTitle>
  <CardDescription className="text-xs">Description</CardDescription>
</CardHeader>
```

#### Compact Card Content

```tsx
<CardContent className="pt-2 space-y-2">
  {/* 8px top padding, 8px item spacing */}
  {items}
</CardContent>
```

### 8. Hover & Focus States

#### Subtle Hover

```tsx
<div className="transition-colors hover:bg-accent/50">
  {/* Lighter background on hover */}
</div>
```

#### Lift Effect (Compact)

```tsx
<div className="transition-all hover:shadow-md hover:-translate-y-0.5">
  {/* Subtle lift instead of large transform */}
</div>
```

#### Focus Rings

```tsx
<button className="focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-ring">
  {/* Tighter focus ring */}
</button>
```

### 9. Flexbox Utilities

#### Compact Flex Row

```tsx
<div className="flex items-center gap-2">
  {/* Horizontal layout with minimal gap */}
</div>
```

#### Compact Flex Column

```tsx
<div className="flex flex-col gap-1">
  {/* Vertical layout with tight gap */}
</div>
```

#### Space Between (Compact)

```tsx
<div className="flex items-center justify-between gap-3">
  {/* Space between with minimum gap for wrapping */}
</div>
```

### 10. Responsive Visibility

#### Hide on Mobile for Compact View

```tsx
<div className="hidden md:block">
  {/* Show only on tablet+ */}
</div>

<div className="hidden lg:block">
  {/* Show only on desktop */}
</div>
```

#### Show Only on Mobile

```tsx
<div className="block md:hidden">
  {/* Mobile-only element */}
</div>
```

### 11. Width Utilities for Inline Elements

#### Compact Width Constraints

```tsx
// Progress bar widths
<div className="w-24 md:w-32 lg:w-48">
  {/* 96px → 128px → 192px */}
</div>

// Icon container
<div className="w-8 h-8 flex items-center justify-center">
  {/* 32px square */}
</div>

// Badge container
<div className="min-w-[4rem]">
  {/* Minimum 64px width */}
</div>
```

### 12. Truncation Utilities

#### Text Truncation

```tsx
<p className="truncate max-w-full">
  {/* Single line with ellipsis */}
  Very long competency name that will be truncated
</p>
```

#### Multi-line Truncation

```tsx
<p className="line-clamp-2">
  {/* Show max 2 lines with ellipsis */}
  Long description that spans multiple lines but we want to limit to two lines maximum
</p>
```

### 13. Z-Index Management

```tsx
// Collapsible content
<div className="relative z-10">

// Overlay
<div className="fixed inset-0 z-50 bg-background/80">

// Modal
<div className="fixed z-50 top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2">
```

### 14. Performance Optimizations

#### Will-Change for Animations

```tsx
<div className="will-change-transform transition-transform hover:-translate-y-1">
  {/* GPU-accelerated transform */}
</div>
```

#### Contain for Layout

```tsx
<div className="contain-layout">
  {/* Isolate layout calculations */}
</div>
```

### 15. Practical Examples

#### Compact Stat Badge

```tsx
<div className="flex items-center gap-2 px-3 py-2 rounded-md border bg-blue-500/10 text-blue-700 border-blue-500/20">
  <Clock className="h-4 w-4 flex-shrink-0" />
  <div className="flex-1 min-w-0">
    <div className="text-sm font-bold tabular-nums">12:45</div>
    <div className="text-xs opacity-80">Time</div>
  </div>
</div>
```

#### Compact Competency Row

```tsx
<div className="group flex items-center gap-3 py-2 px-3 -mx-3 rounded-md hover:bg-accent/50 transition-colors">
  {/* Name + Icon */}
  <div className="flex-1 min-w-0">
    <div className="flex items-center gap-2">
      <span className="text-sm font-medium truncate">Communication</span>
      <span className="text-base">🎯</span>
    </div>
    <p className="text-xs text-muted-foreground font-mono">O*NET 2.A.1.a</p>
  </div>

  {/* Progress */}
  <div className="hidden md:block w-32">
    <div className="h-2 w-full bg-muted/30 rounded-full overflow-hidden">
      <div className="h-full bg-emerald-500 transition-all duration-1000" style={{ width: '85%' }} />
    </div>
  </div>

  {/* Score */}
  <div className="flex items-center gap-2 flex-shrink-0">
    <span className="text-lg font-bold tabular-nums text-emerald-600">85%</span>
    <Badge variant="secondary" className="text-xs hidden sm:inline-flex">Excellent</Badge>
  </div>
</div>
```

#### Compact Hero

```tsx
<div className="flex flex-col md:flex-row items-center gap-4 p-4 md:p-5 rounded-lg border bg-gradient-to-r from-green-500/5 to-transparent">
  {/* Icon + Score */}
  <div className="flex items-center gap-4">
    <div className="rounded-full p-3 bg-green-500/10 border border-green-500/30">
      <Trophy className="h-8 w-8 text-green-600" />
    </div>
    <div className="flex flex-col items-center md:items-start">
      <div className="flex items-baseline gap-2">
        <span className="text-5xl font-bold text-green-600">85%</span>
        <Badge>Passed</Badge>
      </div>
      <p className="text-xs text-muted-foreground">12.8 / 15.0 points</p>
    </div>
  </div>

  {/* Metadata */}
  <div className="flex-1 text-center md:text-left">
    <h1 className="text-xl font-bold">Professional Skills Assessment</h1>
    <p className="text-xs text-muted-foreground">
      Completed Dec 12, 2024 · 92nd percentile
    </p>
  </div>
</div>
```

## Quick Reference: Spacing Scale

| Class | Size | Use Case |
|-------|------|----------|
| `gap-1` | 4px | Tight inline elements |
| `gap-2` | 8px | Compact grids, badges |
| `gap-3` | 12px | Standard compact spacing |
| `gap-4` | 16px | Comfortable spacing |
| `p-2` | 8px | Minimal padding |
| `p-3` | 12px | Compact padding |
| `p-4` | 16px | Standard padding |
| `space-y-2` | 8px | Tight vertical stacking |
| `space-y-3` | 12px | Compact vertical spacing |

## Typography Scale

| Class | Size | Use Case |
|-------|------|----------|
| `text-xs` | 12px | Labels, metadata |
| `text-sm` | 14px | Body text (compact) |
| `text-base` | 16px | Body text (standard) |
| `text-lg` | 18px | Section headings |
| `text-xl` | 20px | Page titles (compact) |
| `text-2xl` | 24px | Hero text |

## Icon Scale

| Class | Size | Use Case |
|-------|------|----------|
| `h-3 w-3` | 12px | Tiny icons (badges) |
| `h-4 w-4` | 16px | Compact icons (stat badges) |
| `h-5 w-5` | 20px | Standard icons |
| `h-6 w-6` | 24px | Large icons |
| `h-8 w-8` | 32px | Hero icons (compact) |
