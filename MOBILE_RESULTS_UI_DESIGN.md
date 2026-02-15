# Mobile-Optimized UI Design for Test Results Page

This document provides specific Tailwind classes, component structure changes, and spacing adjustments for mobile optimization of the test results page components. All changes target mobile screens (320px-640px) and use responsive utility classes to preserve desktop behavior.

---

## 1. Hero Cards (All Three Types)

### Current Issues
- Score circles at 24-36px may be too small for touch
- 2x2 stats grid can overflow on 320px screens
- Icon sizes may be inconsistent
- Padding too generous on small screens

### Mobile-Specific Changes

#### A. HeroStat Component Improvements

```tsx
// BEFORE
<div className="flex flex-col gap-1.5 p-3 rounded-lg border bg-card/50 backdrop-blur-sm">
  <div className="w-7 h-7 rounded-md flex items-center justify-center border bg-primary/10 text-primary border-primary/20">
    {icon}
  </div>
  <div className="flex flex-col">
    <span className="text-lg sm:text-xl font-bold tabular-nums leading-none">
      {value}
    </span>
    {subtitle && (
      <span className="text-[10px] text-muted-foreground mt-0.5">{subtitle}</span>
    )}
  </div>
  <p className="text-[10px] font-medium text-muted-foreground uppercase tracking-wide">
    {label}
  </p>
</div>

// AFTER - Mobile Optimized
<div className="flex flex-col gap-1 p-2 sm:p-3 rounded-lg border bg-card/50 backdrop-blur-sm min-w-0">
  <div className="w-6 h-6 sm:w-7 sm:h-7 rounded-md flex items-center justify-center border bg-primary/10 text-primary border-primary/20 shrink-0">
    {/* Icon with responsive size */}
    <IconComponent className="h-3 w-3 sm:h-4 sm:w-4" />
  </div>
  <div className="flex flex-col min-w-0">
    <span className="text-base sm:text-lg md:text-xl font-bold tabular-nums leading-none truncate">
      {value}
    </span>
    {subtitle && (
      <span className="text-[9px] sm:text-[10px] text-muted-foreground mt-0.5 truncate">{subtitle}</span>
    )}
  </div>
  <p className="text-[9px] sm:text-[10px] font-medium text-muted-foreground uppercase tracking-wide truncate">
    {label}
  </p>
</div>
```

**Key Changes:**
- `p-2 sm:p-3` - Reduced padding on mobile
- `gap-1 sm:gap-1.5` - Tighter spacing on mobile
- `w-6 h-6 sm:w-7 sm:h-7` - Smaller icon container on mobile
- `text-base sm:text-lg md:text-xl` - Progressive text scaling
- `text-[9px] sm:text-[10px]` - Smaller labels on mobile
- `min-w-0 truncate` - Prevent text overflow

#### B. Mobile Stats Grid

```tsx
// BEFORE
<div className="sm:hidden grid grid-cols-2 gap-3 mt-4">

// AFTER - 320px safe
<div className="sm:hidden grid grid-cols-2 gap-2 mt-3 px-0">
```

**Changes:**
- `gap-2` instead of `gap-3` - Tighter grid gap
- `mt-3` instead of `mt-4` - Reduced top margin
- `px-0` - Let parent control horizontal padding

#### C. Score Circle (Mobile)

```tsx
// BEFORE - 24px circle on mobile (w-24 h-24)
<div className="sm:hidden relative">
  <svg className="w-24 h-24 transform -rotate-90" viewBox="0 0 100 100">

// AFTER - Slightly larger for touch, but container-aware
<div className="sm:hidden relative flex-shrink-0">
  <svg className="w-20 h-20 xs:w-24 xs:h-24 transform -rotate-90" viewBox="0 0 100 100">
    <circle
      cx="50"
      cy="50"
      r="42"
      fill="none"
      stroke="currentColor"
      strokeWidth="5"  {/* Reduced from 6 for smaller circle */}
      className="text-muted/20"
    />
    {/* ... progress circle ... */}
  </svg>
  <div className="absolute inset-0 flex items-center justify-center">
    <span className="text-xl xs:text-2xl font-bold ...">
      {percentScore}%
    </span>
  </div>
</div>
```

**Note:** Add custom `xs` breakpoint to Tailwind config if not present:
```js
// tailwind.config.js
theme: {
  screens: {
    'xs': '375px',
    'sm': '640px',
    // ...
  }
}
```

#### D. Hero Card Main Container

```tsx
// BEFORE
<CardContent className="p-6 relative">
  <div className="grid grid-cols-1 lg:grid-cols-[auto_1fr] gap-6 items-center">

// AFTER - Tighter mobile spacing
<CardContent className="p-3 sm:p-4 md:p-6 relative">
  <div className="grid grid-cols-1 lg:grid-cols-[auto_1fr] gap-3 sm:gap-4 md:gap-6 items-center">
```

#### E. Hero Title Section

```tsx
// BEFORE
<h1 className="text-xl sm:text-2xl font-bold ...">
<p className="text-sm text-muted-foreground line-clamp-1">

// AFTER
<h1 className="text-lg sm:text-xl md:text-2xl font-bold leading-tight ...">
<p className="text-xs sm:text-sm text-muted-foreground line-clamp-1">
```

---

## 2. Tabs Component (Profile Charts)

### Current Issues
- Tab text can overflow on narrow screens
- Tab icons may be too large
- Tab list doesn't handle 320px well

### Mobile-Specific Changes

#### A. TabsList Wrapper

```tsx
// BEFORE
<TabsList className="w-full mb-4">
  <TabsTrigger value="competency" className="flex-1 gap-1.5">
    <Target className="h-4 w-4" />
    <span className="hidden sm:inline">Competencies</span>
    <span className="sm:hidden">Skills</span>
  </TabsTrigger>

// AFTER - Mobile optimized with scrollable fallback
<TabsList className="w-full mb-3 sm:mb-4 h-auto min-h-9 flex-wrap xs:flex-nowrap">
  <TabsTrigger value="competency" className="flex-1 gap-1 sm:gap-1.5 px-2 sm:px-3 py-1.5">
    <Target className="h-3.5 w-3.5 sm:h-4 sm:w-4 shrink-0" />
    <span className="hidden xs:inline text-xs sm:text-sm truncate">Skills</span>
    <span className="xs:hidden text-xs">1</span>  {/* Icon-only with number fallback */}
  </TabsTrigger>
  <TabsTrigger value="personality" className="flex-1 gap-1 sm:gap-1.5 px-2 sm:px-3 py-1.5">
    <Brain className="h-3.5 w-3.5 sm:h-4 sm:w-4 shrink-0" />
    <span className="hidden xs:inline text-xs sm:text-sm truncate">Big 5</span>
    <span className="xs:hidden text-xs">2</span>
  </TabsTrigger>
  <TabsTrigger value="mapping" className="flex-1 gap-1 sm:gap-1.5 px-2 sm:px-3 py-1.5">
    <GitCompareArrows className="h-3.5 w-3.5 sm:h-4 sm:w-4 shrink-0" />
    <span className="hidden xs:inline text-xs sm:text-sm truncate">Map</span>
    <span className="xs:hidden text-xs">3</span>
  </TabsTrigger>
</TabsList>
```

**Key Changes:**
- `gap-1 sm:gap-1.5` - Smaller gap on mobile
- `px-2 sm:px-3` - Reduced horizontal padding
- `h-3.5 w-3.5 sm:h-4 sm:w-4` - Smaller icons on mobile
- `text-xs sm:text-sm` - Smaller text on mobile
- Use very short labels or numbers on smallest screens

#### B. Alternative: Segmented Control Style

For even smaller screens, consider a segmented control with icons only:

```tsx
// Icon-only tabs for xs screens
<TabsList className="w-full mb-3 grid grid-cols-3 h-10">
  <TabsTrigger value="competency" className="flex items-center justify-center" title="Competencies">
    <Target className="h-4 w-4" />
    <span className="sr-only">Competencies</span>
  </TabsTrigger>
  {/* ... */}
</TabsList>
```

---

## 3. Charts Section

### Current Issues
- 280-340px min-height may push content off screen
- Radar charts need significant space for labels
- Charts may overflow horizontally

### Mobile-Specific Changes

#### A. Chart Container

```tsx
// BEFORE
<div className="flex justify-center items-center min-h-[280px] md:min-h-[340px]">

// AFTER - More aggressive mobile reduction
<div className="flex justify-center items-center min-h-[200px] xs:min-h-[240px] sm:min-h-[280px] md:min-h-[340px] w-full overflow-hidden">
```

#### B. BigFiveRadarSimple Component Updates

```tsx
// In BigFiveRadar.tsx - update BigFiveRadarSimple
export const BigFiveRadarSimple = React.memo<...>(({
  profile,
  showLegend = false,
  height = 300
}) => {
  const isMobile = useIsMobile();
  const colors = useComputedColors();
  const chartData = React.useMemo(() => bigFiveToArray(profile), [profile]);

  // AFTER - More aggressive mobile sizing
  const responsiveHeight = isMobile ? Math.min(height, 180) : height;  // Was 220
  const fontSize = isMobile ? 8 : 12;   // Was 10
  const radiusFontSize = isMobile ? 6 : 10;  // Was 8

  // Shorter trait labels for mobile
  const mobileChartData = isMobile
    ? chartData.map(d => ({
        ...d,
        trait: MOBILE_TRAIT_LABELS[d.trait] || d.trait.slice(0, 4)
      }))
    : chartData;

  // ... rest of component
});

// Add at top of file
const MOBILE_TRAIT_LABELS: Record<string, string> = {
  'Openness': 'Open',
  'Conscientiousness': 'Consc',
  'Extraversion': 'Extra',
  'Agreeableness': 'Agree',
  'Emotional Stability': 'Stable'
};
```

#### C. GapAnalysisBarChart Mobile Optimization

```tsx
// In GapAnalysisBarChart.tsx
export default function GapAnalysisBarChart({ data }: GapAnalysisBarChartProps) {
  const isMobile = useIsMobile();

  // AFTER - More aggressive mobile sizing
  const height = isMobile ? 180 : 300;  // Was 220
  const fontSize = isMobile ? 9 : 12;   // Was 10
  const yAxisWidth = isMobile ? 60 : 150;  // Was 80 - shorter names
  const barSize = isMobile ? 10 : 20;   // Was 14
  const margin = isMobile
    ? { top: 2, right: 5, left: 0, bottom: 2 }  // Tighter margins
    : { top: 5, right: 30, left: 20, bottom: 5 };

  // Truncate names more aggressively on mobile
  const tickFormatter = (value: string) => {
    if (!isMobile) return value;
    return value.length > 8 ? `${value.slice(0, 8)}...` : value;  // Was 12
  };

  return (
    <div className="w-full overflow-hidden" style={{ height }}>
      {/* ... */}
    </div>
  );
}
```

#### D. Alternative: Simplified Mobile Charts

For very small screens, consider replacing complex charts with simpler visualizations:

```tsx
// MobileChartFallback.tsx - Simple horizontal bars instead of radar
export function MobileChartFallback({ data }: { data: Array<{ name: string; value: number }> }) {
  return (
    <div className="space-y-2 p-2">
      {data.slice(0, 5).map((item) => (
        <div key={item.name} className="space-y-1">
          <div className="flex justify-between text-xs">
            <span className="truncate max-w-[60%]">{item.name}</span>
            <span className="font-bold tabular-nums">{item.value}%</span>
          </div>
          <div className="h-2 bg-muted/30 rounded-full overflow-hidden">
            <div
              className="h-full bg-primary rounded-full transition-all"
              style={{ width: `${item.value}%` }}
            />
          </div>
        </div>
      ))}
    </div>
  );
}

// Usage in result views
{isMobile && competencyRadarData.length > 0 ? (
  <MobileChartFallback data={competencyRadarData.map(d => ({ name: d.subject, value: d.A }))} />
) : (
  <CompetencyRadarChart data={competencyRadarData} ... />
)}
```

---

## 4. Insights Cards

### Current Issues
- Too much text on mobile
- Badges may overflow
- Sections need collapsing

### Mobile-Specific Changes

#### A. Key Insights Card

```tsx
// BEFORE
<Card className="h-full">
  <CardHeader className="pb-3">
    <CardTitle className="text-base flex items-center gap-2">

// AFTER
<Card className="h-full">
  <CardHeader className="pb-2 sm:pb-3 px-3 sm:px-6">
    <CardTitle className="text-sm sm:text-base flex items-center gap-1.5 sm:gap-2">
```

#### B. Profile Summary Section

```tsx
// BEFORE
<div className="p-3 bg-primary/5 rounded-lg border border-primary/10">
  <h4 className="font-semibold text-sm text-primary mb-1.5 flex items-center gap-2">
    <Lightbulb className="h-3.5 w-3.5" />
    Your Profile Summary
  </h4>
  <p className="text-xs text-muted-foreground leading-relaxed">
    This assessment provides a comprehensive view of your competencies...
  </p>
</div>

// AFTER - Collapsible on mobile
<details className="group">
  <summary className="p-2 sm:p-3 bg-primary/5 rounded-lg border border-primary/10 cursor-pointer list-none">
    <div className="flex items-center justify-between">
      <h4 className="font-semibold text-xs sm:text-sm text-primary flex items-center gap-1.5">
        <Lightbulb className="h-3 w-3 sm:h-3.5 sm:w-3.5" />
        Your Profile Summary
      </h4>
      <ChevronDown className="h-4 w-4 text-primary transition-transform group-open:rotate-180" />
    </div>
  </summary>
  <div className="p-2 sm:p-3 pt-2">
    <p className="text-[11px] sm:text-xs text-muted-foreground leading-relaxed">
      {/* Shortened text on mobile */}
      <span className="sm:hidden">View your competencies across {count} areas.</span>
      <span className="hidden sm:inline">This assessment provides a comprehensive view...</span>
    </p>
  </div>
</details>
```

#### C. Trait Descriptions - Mobile Truncation

```tsx
// BEFORE
{topTraits.map(trait => (
  <div key={trait.key} className="p-2.5 bg-muted/30 rounded-lg">
    <div className="flex items-center justify-between mb-1">
      <span className="text-sm font-medium">{trait.label}</span>
      <span className="text-sm font-bold text-primary tabular-nums">
        {trait.value}%
      </span>
    </div>
    <p className="text-xs text-muted-foreground">
      {getTraitDescription(trait.label, trait.value)}
    </p>
  </div>
))}

// AFTER - Compact on mobile
{topTraits.map(trait => (
  <div key={trait.key} className="p-2 sm:p-2.5 bg-muted/30 rounded-lg">
    <div className="flex items-center justify-between gap-2">
      <span className="text-xs sm:text-sm font-medium truncate">{trait.label}</span>
      <span className="text-xs sm:text-sm font-bold text-primary tabular-nums shrink-0">
        {trait.value}%
      </span>
    </div>
    <p className="text-[11px] sm:text-xs text-muted-foreground line-clamp-2 sm:line-clamp-none mt-1">
      {getTraitDescription(trait.label, trait.value)}
    </p>
  </div>
))}
```

#### D. Competency Badges/Tags

```tsx
// BEFORE
<div className="flex flex-wrap gap-2">
  {result.competencyScores
    .filter(c => c.percentage >= 70)
    .slice(0, 4)
    .map(c => (
      <span
        key={c.competencyId}
        className="text-xs px-2 py-1 bg-primary/10 text-primary rounded-full"
      >
        {c.competencyName}
      </span>
    ))}
</div>

// AFTER - Scrollable on mobile, limit visible
<div className="flex gap-1.5 sm:gap-2 overflow-x-auto pb-1 -mx-1 px-1 sm:flex-wrap sm:overflow-visible sm:pb-0">
  {result.competencyScores
    .filter(c => c.percentage >= 70)
    .slice(0, isMobile ? 3 : 4)  // Show fewer on mobile
    .map(c => (
      <span
        key={c.competencyId}
        className="text-[11px] sm:text-xs px-1.5 sm:px-2 py-0.5 sm:py-1 bg-primary/10 text-primary rounded-full whitespace-nowrap shrink-0"
      >
        {truncateCompetencyName(c.competencyName, isMobile ? 12 : 20)}
      </span>
    ))}
  {result.competencyScores.filter(c => c.percentage >= 70).length > (isMobile ? 3 : 4) && (
    <span className="text-[11px] sm:text-xs px-1.5 sm:px-2 py-0.5 sm:py-1 bg-muted text-muted-foreground rounded-full whitespace-nowrap shrink-0">
      +{result.competencyScores.filter(c => c.percentage >= 70).length - (isMobile ? 3 : 4)} more
    </span>
  )}
</div>

// Helper function
function truncateCompetencyName(name: string, maxLength: number): string {
  return name.length > maxLength ? `${name.slice(0, maxLength)}...` : name;
}
```

---

## 5. CompetencyDetailAccordion

### Current Issues
- Progress bars may be too wide
- Indicator rows can overflow
- Accordion content needs tighter spacing

### Mobile-Specific Changes

#### A. Accordion Trigger

```tsx
// BEFORE
<AccordionTrigger className="hover:no-underline py-3">
  <div className="flex items-center justify-between w-full pr-4">
    <div className="flex items-center gap-3 flex-1 min-w-0">
      <span className="text-sm font-medium truncate">
        {competency.competencyName}
      </span>
      {competency.competencyCategory && (
        <Badge variant="outline" className="text-xs shrink-0">
          {competency.competencyCategory}
        </Badge>
      )}
    </div>
    <div className="flex items-center gap-3 shrink-0">
      <div className="w-24 sm:w-32 h-2 bg-muted/30 rounded-full overflow-hidden">

// AFTER
<AccordionTrigger className="hover:no-underline py-2 sm:py-3">
  <div className="flex items-center justify-between w-full pr-2 sm:pr-4 gap-2">
    <div className="flex flex-col sm:flex-row sm:items-center gap-0.5 sm:gap-3 flex-1 min-w-0">
      <span className="text-xs sm:text-sm font-medium truncate">
        {competency.competencyName}
      </span>
      {competency.competencyCategory && (
        <Badge variant="outline" className="text-[10px] sm:text-xs shrink-0 w-fit hidden xs:inline-flex">
          {competency.competencyCategory}
        </Badge>
      )}
    </div>
    <div className="flex items-center gap-2 sm:gap-3 shrink-0">
      <div className="w-16 xs:w-20 sm:w-24 md:w-32 h-1.5 sm:h-2 bg-muted/30 rounded-full overflow-hidden">
        {/* progress bar */}
      </div>
      <span className="text-xs sm:text-sm font-bold tabular-nums w-10 sm:w-12 text-right">
        {percentage}%
      </span>
    </div>
  </div>
</AccordionTrigger>
```

#### B. Accordion Content (Indicator Breakdown)

```tsx
// BEFORE
<div className="pl-4 space-y-3">
  {/* Score summary */}
  <div className="flex items-center justify-between p-3 bg-muted/30 rounded-lg">
    <div className="flex items-center gap-2">
      <span className="text-lg">{config.icon}</span>
      <span className="text-sm font-medium">{config.label}</span>
    </div>
    <div className="text-right">
      <div className="text-xl font-bold ...">
        {percentage}%
      </div>
      <div className="text-xs text-muted-foreground">
        {competency.score.toFixed(1)} / {competency.maxScore.toFixed(1)} points
      </div>
    </div>
  </div>

// AFTER
<div className="pl-2 sm:pl-4 space-y-2 sm:space-y-3">
  {/* Score summary - more compact on mobile */}
  <div className="flex items-center justify-between p-2 sm:p-3 bg-muted/30 rounded-lg gap-2">
    <div className="flex items-center gap-1.5 sm:gap-2">
      <span className="text-base sm:text-lg">{config.icon}</span>
      <span className="text-xs sm:text-sm font-medium">{config.label}</span>
    </div>
    <div className="text-right shrink-0">
      <div className="text-lg sm:text-xl font-bold ...">
        {percentage}%
      </div>
      <div className="text-[10px] sm:text-xs text-muted-foreground">
        {competency.score.toFixed(1)}/{competency.maxScore.toFixed(1)} pts
      </div>
    </div>
  </div>
```

#### C. Indicator Rows

```tsx
// BEFORE
{competency.indicatorScores.map(indicator => (
  <div key={indicator.indicatorId} className="flex items-center gap-3 text-sm">
    <div className="flex-1 min-w-0">
      <p className="text-xs text-muted-foreground truncate">
        {indicator.indicatorTitle}
      </p>
    </div>
    <div className="flex items-center gap-2 shrink-0">
      <div className="w-20 h-1.5 bg-muted/30 rounded-full overflow-hidden">
        {/* bar */}
      </div>
      <span className="text-xs font-semibold tabular-nums w-10 text-right">
        {Math.round(indicator.percentage)}%
      </span>
    </div>
  </div>
))}

// AFTER - Stack on mobile
{competency.indicatorScores.map(indicator => (
  <div key={indicator.indicatorId} className="flex flex-col xs:flex-row xs:items-center gap-1 xs:gap-2 py-1 border-b border-muted/20 last:border-b-0">
    <div className="flex-1 min-w-0">
      <p className="text-[11px] sm:text-xs text-muted-foreground truncate">
        {indicator.indicatorTitle}
      </p>
    </div>
    <div className="flex items-center gap-2 shrink-0">
      <div className="w-16 xs:w-20 h-1 xs:h-1.5 bg-muted/30 rounded-full overflow-hidden">
        <div
          className="h-full rounded-full ..."
          style={{ width: `${indicator.percentage}%` }}
        />
      </div>
      <span className="text-[11px] sm:text-xs font-semibold tabular-nums w-8 sm:w-10 text-right">
        {Math.round(indicator.percentage)}%
      </span>
    </div>
  </div>
))}
```

---

## 6. ActionButtonsBar

### Current Issues
- Buttons may overflow on narrow screens
- Text too long for mobile

### Mobile-Specific Changes

```tsx
// BEFORE
<div className="flex flex-col sm:flex-row gap-2 pt-6 border-t">
  <Button asChild variant="outline" size="sm" className="flex-1 sm:flex-initial">
    <Link href="/test-templates">
      <Home className="h-4 w-4 mr-2" />
      Back to Tests
    </Link>
  </Button>
  {actions.includes('retake') && (
    <Button asChild size="sm" className="flex-1 sm:flex-initial">
      <Link href={`/test-templates/${templateId}/start`}>
        <RotateCcw className="h-4 w-4 mr-2" />
        Retake Test
      </Link>
    </Button>
  )}
  {/* Other actions... */}
</div>

// AFTER
<div className="flex flex-col gap-2 pt-4 sm:pt-6 border-t">
  {/* Primary actions row */}
  <div className="flex gap-2">
    <Button asChild variant="outline" size="sm" className="flex-1">
      <Link href="/test-templates">
        <Home className="h-3.5 w-3.5 sm:h-4 sm:w-4 sm:mr-2" />
        <span className="hidden xs:inline">Back</span>
      </Link>
    </Button>
    {actions.includes('retake') && (
      <Button asChild size="sm" className="flex-1">
        <Link href={`/test-templates/${templateId}/start`}>
          <RotateCcw className="h-3.5 w-3.5 sm:h-4 sm:w-4 sm:mr-2" />
          <span className="hidden xs:inline">Retake</span>
        </Link>
      </Button>
    )}
  </div>

  {/* Secondary actions row - only on mobile if more than 2 actions */}
  {actions.filter(a => a !== 'retake' && a !== 'back_to_list').length > 0 && (
    <div className="flex gap-2">
      {actions
        .filter(action => action !== 'retake' && action !== 'back_to_list')
        .map(action => {
          const config = ACTION_CONFIG[action];
          if (!config) return null;
          const Icon = config.icon;

          return (
            <Button
              key={action}
              variant={config.variant}
              size="sm"
              className="flex-1"
              onClick={() => handleAction(action)}
            >
              <Icon className="h-3.5 w-3.5 sm:h-4 sm:w-4 sm:mr-2" />
              <span className="hidden xs:inline text-xs sm:text-sm">{config.label}</span>
            </Button>
          );
        })}
    </div>
  )}
</div>
```

---

## 7. Main Layout Container

### Current Issues
- Container padding may be too wide
- Space between sections excessive

### Mobile-Specific Changes

```tsx
// BEFORE
<div className="min-h-screen bg-muted/30 py-4 md:py-8">
  <div className="container max-w-7xl mx-auto px-4 space-y-4">

// AFTER
<div className="min-h-screen bg-muted/30 py-3 sm:py-4 md:py-8">
  <div className="container max-w-7xl mx-auto px-2 sm:px-4 space-y-3 sm:space-y-4">
```

---

## 8. TeamFit Big Five Personality Grid

### Current Issues
- 5-column grid doesn't work on mobile
- Circle SVGs may be too large

### Mobile-Specific Changes

```tsx
// BEFORE
<div className="grid grid-cols-1 md:grid-cols-5 gap-4">
  {bigFiveData.map(({ trait, value }) => (
    <div
      key={trait}
      className="text-center p-4 bg-muted/30 rounded-lg border"
    >
      <div className="text-xs font-medium text-muted-foreground uppercase tracking-wide mb-2">
        {trait}
      </div>
      <div className="relative w-16 h-16 mx-auto mb-2">

// AFTER - Horizontal scroll on mobile, 2-column on small screens
<div className="grid grid-cols-2 xs:grid-cols-3 sm:grid-cols-5 gap-2 sm:gap-4">
  {bigFiveData.map(({ trait, value }) => (
    <div
      key={trait}
      className="text-center p-2 sm:p-4 bg-muted/30 rounded-lg border"
    >
      <div className="text-[10px] sm:text-xs font-medium text-muted-foreground uppercase tracking-wide mb-1 sm:mb-2 truncate">
        {MOBILE_TRAIT_LABELS[trait] || trait}
      </div>
      <div className="relative w-12 h-12 sm:w-16 sm:h-16 mx-auto mb-1 sm:mb-2">
        <svg className="w-12 h-12 sm:w-16 sm:h-16 transform -rotate-90" viewBox="0 0 64 64">
          <circle
            cx="32"
            cy="32"
            r="28"
            fill="none"
            stroke="currentColor"
            strokeWidth="3"  {/* Reduced from 4 */}
            className="text-muted/30"
          />
          {/* ... */}
        </svg>
        <div className="absolute inset-0 flex items-center justify-center">
          <span className="text-sm sm:text-lg font-bold ...">
            {value}
          </span>
        </div>
      </div>
      <div className="text-[10px] sm:text-xs text-muted-foreground">
        {value >= 70 ? 'High' : value >= 40 ? 'Med' : 'Low'}
      </div>
    </div>
  ))}
</div>
```

---

## 9. BigFiveMappingInsights Mobile

### Current Issues
- Cards too wide
- Contribution rows need stacking

### Mobile-Specific Changes

```tsx
// TraitCard - BEFORE
<button
  type="button"
  onClick={onToggle}
  className="w-full text-left p-3 sm:p-4 focus:outline-none ..."
>
  <div className="flex items-start justify-between gap-3">
    <div className="flex-1 min-w-0">
      <div className="flex items-center gap-2 mb-1">
        <div className="w-3 h-3 rounded-full shrink-0" ... />
        <h4 className="font-semibold text-sm sm:text-base">
          {info.short}
        </h4>
        <span className="text-sm font-bold tabular-nums" ...>
          {score}%
        </span>
      </div>
      <p className="text-xs text-muted-foreground line-clamp-2">
        {info.description}
      </p>
      <p className="text-xs text-muted-foreground mt-1">
        {contributionCount} contributing competencies
      </p>
    </div>

// TraitCard - AFTER
<button
  type="button"
  onClick={onToggle}
  className="w-full text-left p-2.5 sm:p-3 md:p-4 focus:outline-none ..."
>
  <div className="flex items-start justify-between gap-2 sm:gap-3">
    <div className="flex-1 min-w-0">
      <div className="flex items-center gap-1.5 sm:gap-2 mb-1">
        <div className="w-2.5 h-2.5 sm:w-3 sm:h-3 rounded-full shrink-0" ... />
        <h4 className="font-semibold text-xs sm:text-sm md:text-base truncate">
          {info.short}
        </h4>
        <span className="text-xs sm:text-sm font-bold tabular-nums shrink-0" ...>
          {score}%
        </span>
      </div>
      <p className="text-[11px] sm:text-xs text-muted-foreground line-clamp-1 sm:line-clamp-2">
        {info.description}
      </p>
      <p className="text-[10px] sm:text-xs text-muted-foreground mt-0.5 sm:mt-1">
        {contributionCount} {contributionCount === 1 ? 'skill' : 'skills'}
      </p>
    </div>
```

---

## 10. CSS Utility Classes Summary

Add these utility classes to your global CSS or Tailwind config:

```css
/* globals.css additions */

/* Prevent horizontal overflow on mobile */
@layer utilities {
  .mobile-safe {
    @apply max-w-full overflow-hidden;
  }

  .mobile-scroll-x {
    @apply overflow-x-auto -mx-2 px-2 sm:mx-0 sm:px-0 sm:overflow-visible;
  }

  /* Ensure charts don't overflow */
  .chart-container {
    @apply w-full overflow-hidden relative;
  }

  /* Touch-friendly minimum sizes */
  .touch-target {
    @apply min-h-[44px] min-w-[44px];
  }
}
```

---

## 11. Responsive Breakpoint Strategy

Use this consistent breakpoint strategy:

| Breakpoint | Class Prefix | Width | Target |
|------------|--------------|-------|--------|
| Default    | (none)       | 0px   | Very small phones (320px) |
| xs         | `xs:`        | 375px | Standard phones |
| sm         | `sm:`        | 640px | Large phones / Small tablets |
| md         | `md:`        | 768px | Tablets |
| lg         | `lg:`        | 1024px | Desktop |

**Add xs breakpoint to tailwind.config.js:**

```js
module.exports = {
  theme: {
    screens: {
      'xs': '375px',
      'sm': '640px',
      'md': '768px',
      'lg': '1024px',
      'xl': '1280px',
      '2xl': '1536px',
    },
  },
};
```

---

## 12. Testing Checklist

Before deploying mobile changes, test on:

1. **320px width** - iPhone SE, Galaxy S8
2. **375px width** - iPhone 12/13/14
3. **390px width** - iPhone 14 Pro
4. **412px width** - Pixel 7

Test scenarios:
- [ ] No horizontal scrolling on any screen
- [ ] All text readable (minimum 11px)
- [ ] Touch targets minimum 44x44px
- [ ] Charts render without overflow
- [ ] Accordions expand/collapse smoothly
- [ ] Tabs are navigable
- [ ] Badges don't wrap awkwardly
- [ ] Progress bars visible and accurate
- [ ] Action buttons are tappable

---

## Implementation Priority

1. **High Priority (Do First)**
   - Hero card mobile padding/spacing
   - Chart container overflow fixes
   - Action buttons mobile layout

2. **Medium Priority**
   - Tabs mobile optimization
   - Accordion content stacking
   - Badge truncation/scrolling

3. **Lower Priority**
   - Add xs breakpoint to Tailwind
   - Alternative mobile chart visualizations
   - Collapsible insight sections
