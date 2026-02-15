# Lens System Guide

## Overview

### What is the Lens System?

The **Lens System** is a role-based view switching mechanism that transforms how you interact with the SkillSoft platform. Think of it as having different "modes" or "views" of the same system, each optimized for a specific type of work you need to accomplish.

Inspired by modern cloud platforms like Wiz, the Lens System addresses a common challenge in enterprise software: **how do you serve both casual users and power users without overwhelming anyone?**

Instead of showing every feature to every user all at once, the Lens System lets you choose the right "lens" for your current task. Each lens reveals a tailored interface with only the tools, navigation items, and information relevant to that specific role or workflow.

### Why Does It Exist?

**The Problem:**
Traditional HR systems often suffer from one of two extremes:
- **Too Simple:** Limited interfaces that frustrate power users who need advanced features
- **Too Complex:** Feature-rich dashboards that overwhelm employees who just want to view their profile or take a test

**The Solution:**
The Lens System provides the best of both worlds:
- **Focused Experience:** See only what you need for your current task
- **Flexible Access:** Power users can switch between simple and advanced views
- **Progressive Disclosure:** Features appear when needed, not all at once
- **Context-Aware Interface:** The system adapts to your role and workflow

### How It Works

When you log into SkillSoft, the system automatically selects the most appropriate lens based on your role. If you have permissions for multiple lenses, you'll see a **Lens Switcher** in the sidebar that lets you change your view with a single click.

The active lens determines:
- Which menu items appear in your navigation sidebar
- Which widgets and statistics you see on your dashboard
- Which features and actions are available on each page
- The overall visual theme and color scheme

Your lens selection is remembered, so the next time you log in, you'll start exactly where you left off.

---

## The Three Lenses

### 1. Personal Lens (Личное)

**Visual Identity:**
- **Icon:** User profile silhouette
- **Color:** Emerald green
- **Theme:** Clean, personal, focused

**Purpose:**
The Personal Lens is designed for individual employees who want to engage with the assessment system as participants. This is your personal workspace for professional development.

**What You See:**

**Navigation:**
- Dashboard (personal view)
- Test Templates (available assessments)

**Dashboard Widgets:**
- **My Progress:** Track your assessment completion and skill development over time
- **Recent Activity:** See your latest test sessions and results

**Available Features:**
- View your personal profile and settings
- Browse available test templates
- Take assigned or self-selected assessments
- Review your historical test results
- Track your competency development journey
- Update personal preferences

**When to Use:**
- Taking a soft skills assessment
- Reviewing your assessment history
- Checking your competency scores
- Tracking personal development progress
- Managing your profile settings

**Who Has Access:**
All users have access to the Personal Lens, regardless of their role in the system.

---

### 2. Content Lens (Контент)

**Visual Identity:**
- **Icon:** Pencil/Edit tool
- **Color:** Blue
- **Theme:** Professional, organized, editorial

**Purpose:**
The Content Lens is the workspace for HR content managers, assessment designers, and competency framework editors. This lens focuses on creating and maintaining the assessment content library.

**What You See:**

**Navigation:**
- Dashboard (content management view)
- Tests (template management)
- HR Library
  - Competencies (skill definitions)
  - Behavioral Indicators (observable behaviors)
  - Assessment Questions (measurement instruments)
- Skill Mapper (competency mapping tools)

**Dashboard Widgets:**
- **Overview:** High-level statistics about your content library
- **Content Statistics:** Metrics on competencies, indicators, and questions
- **Pending Reviews:** Items awaiting approval or revision
- **Recent Edits:** Latest changes to the content library

**Available Features:**
- **View and Browse:** Explore the complete competency framework
- **Create Content:** Design new competencies, indicators, and questions
- **Edit Content:** Refine and improve existing assessment materials
- **Content Management:** Organize and categorize assessment content
- **Review Workflow:** Move items through approval stages
- **Quality Control:** Ensure assessment content meets psychometric standards

**When to Use:**
- Creating new competency frameworks
- Designing assessment questions
- Reviewing and approving content submissions
- Editing behavioral indicators
- Managing the HR knowledge library
- Mapping competencies to international standards
- Building new test templates

**Who Has Access:**
Users with EDITOR or ADMIN roles can access the Content Lens.

---

### 3. Admin Lens (Админ)

**Visual Identity:**
- **Icon:** Shield (protection/authority)
- **Color:** Violet
- **Theme:** Powerful, comprehensive, authoritative

**Purpose:**
The Admin Lens provides complete system oversight and control. This is the command center for system administrators who need full visibility and management capabilities across the entire platform.

**What You See:**

**Navigation:**
- Dashboard (system administration view)
- All routes from Content Lens
- User Management (additional admin-only section)
- System Settings (platform configuration)

**Dashboard Widgets:**
- **Overview:** Platform-wide statistics and health metrics
- **System Statistics:** Usage metrics, performance indicators
- **User Activity:** Login patterns, active sessions, user engagement
- **Content Statistics:** Library growth and quality metrics
- **Security Alerts:** Authentication issues, suspicious activity, system warnings

**Available Features:**
- **Full CRUD Operations:** Create, read, update, and delete all entities
- **User Management:**
  - View all users and their roles
  - Assign and revoke permissions
  - Monitor user activity
  - Manage authentication settings
- **System Administration:**
  - Platform configuration
  - Security settings
  - Integration management
  - Audit logs
- **Advanced Analytics:**
  - Platform usage reports
  - Performance monitoring
  - Data export and backup
- **All Content Lens Features:** Complete access to content management
- **All Personal Lens Features:** Full user experience visibility

**When to Use:**
- Managing user accounts and permissions
- Monitoring platform health and security
- Configuring system-wide settings
- Investigating user issues or reports
- Performing administrative tasks
- Analyzing platform usage patterns
- Handling security and compliance matters

**Who Has Access:**
Only users with the ADMIN role can access the Admin Lens.

---

## How to Use the Lens Switcher

### Finding the Lens Switcher

The Lens Switcher appears in your navigation sidebar, typically near the top or bottom depending on your platform configuration. It's only visible if you have permission to access multiple lenses.

**Visual Appearance:**
The switcher displays your currently active lens with its distinctive icon and color:
- Emerald green for Personal Lens
- Blue for Content Lens
- Violet for Admin Lens

### Switching Between Lenses

**Step 1: Click the Lens Switcher**
Click on the lens indicator in your sidebar. A menu will appear showing all available lenses you have permission to access.

**Step 2: Select Your Desired Lens**
Each lens option displays:
- The lens name (Personal, Content, or Admin)
- A descriptive subtitle explaining its purpose
- The distinctive icon and color

**Step 3: Instant Transition**
When you select a lens:
- The interface smoothly transitions with a color-coded animation
- The sidebar navigation updates to show lens-appropriate menu items
- The dashboard reloads with relevant widgets
- The color theme shifts to match the active lens

**Step 4: Continue Your Work**
Your lens selection is automatically saved. The next time you log in, you'll start in the last lens you used.

### Smart Redirects

If you're viewing a page that isn't available in your newly selected lens, the system automatically redirects you to the dashboard for that lens. This prevents confusion and ensures you always land on a valid page.

**Example:** You're in Admin Lens viewing the User Management page. When you switch to Personal Lens (which doesn't have user management), the system smoothly redirects you to your Personal Dashboard.

---

## Benefits and Use Cases

### 1. Reduced Cognitive Load

**The Challenge:** Modern software platforms can overwhelm users with too many options, menus, and features they don't need for their current task.

**The Solution:** By showing only relevant features, the Lens System reduces mental overhead. Employees taking assessments don't see content management tools. Content editors don't see administrative dashboards unless they need them.

**Real-World Impact:**
- Faster task completion (less searching for the right feature)
- Lower training requirements (simpler interfaces are easier to learn)
- Fewer user errors (can't accidentally access features you shouldn't)

---

### 2. Role-Appropriate Workflows

**The Challenge:** Different roles need different tools, but a one-size-fits-all interface serves no one well.

**The Solution:** Each lens is optimized for a specific workflow:
- **Personal Lens:** Assessment participation and personal development tracking
- **Content Lens:** Content creation, editing, and quality management
- **Admin Lens:** System oversight, user management, and platform administration

**Real-World Impact:**
- HR content managers focus on framework design without system administration distractions
- Administrators get comprehensive oversight without cluttered content editing tools
- Employees enjoy a clean, focused assessment experience

---

### 3. Progressive Complexity

**The Challenge:** Power users need advanced features, but exposing them by default intimidates less technical users.

**The Solution:** The Lens System implements progressive disclosure. New users or those with limited permissions see simplified interfaces. As users gain expertise or permissions, they can "upgrade" to more powerful lenses.

**Real-World Impact:**
- Smooth onboarding experience for new employees
- Natural growth path as users develop expertise
- Power users aren't constrained by beginner-friendly limitations

---

### 4. Context Switching Made Easy

**The Challenge:** HR professionals often wear multiple hats. An HR manager might need to take an assessment one moment, then design new competency frameworks the next, and finally review user permissions.

**The Solution:** Instead of navigating complex menus or separate applications, users with multiple roles can switch lenses instantly to access the right toolset for each task.

**Real-World Impact:**
- Faster context switching between roles
- Single platform for multiple responsibilities
- Consistent experience across different workflows

---

### Use Case Examples

#### Example 1: The New Employee

**Scenario:** Maria just joined the company and needs to complete her onboarding assessments.

**Experience:**
- Maria logs in and sees the **Personal Lens** (emerald green)
- Her dashboard shows assigned test templates
- Navigation is simple: Dashboard and Test Templates
- She completes assessments without seeing advanced features she doesn't need
- Clean, focused experience reduces anxiety and complexity

---

#### Example 2: The Content Manager

**Scenario:** Alexei is an HR specialist responsible for maintaining the company's competency framework.

**Experience:**
- Alexei has EDITOR permissions
- He typically works in the **Content Lens** (blue)
- Dashboard shows pending reviews and recent content edits
- Full access to HR Library for creating/editing competencies
- Can switch to **Personal Lens** when taking assessments himself
- Doesn't see admin features (user management, system settings)
- Focused workspace for content development without administrative distractions

---

#### Example 3: The System Administrator

**Scenario:** Elena is the platform administrator who manages users, monitors security, and oversees the entire system.

**Experience:**
- Elena has ADMIN permissions
- She primarily works in the **Admin Lens** (violet)
- Dashboard displays system health, user activity, and security alerts
- Full access to user management and system configuration
- Can switch to **Content Lens** to review competency framework quality
- Can switch to **Personal Lens** to experience the platform as end-users do
- Complete flexibility to work at any level of the system

---

#### Example 4: The Multi-Role HR Manager

**Scenario:** Dmitry is an HR manager who designs assessments, takes them himself, and manages some administrative tasks.

**Experience:**
- Dmitry has ADMIN permissions but doesn't always need them
- **Morning:** Switches to **Content Lens** to design new competency questions
- **Lunch:** Switches to **Personal Lens** to complete his own quarterly assessment
- **Afternoon:** Switches to **Admin Lens** to review user access requests
- Each lens provides exactly what he needs for that specific task
- No clutter, no confusion, just the right tools at the right time

---

## Permission Model

### Role-Based Lens Access

The Lens System uses a hierarchical permission model that aligns with your organizational role:

```
USER Role:
└── Personal Lens only

EDITOR Role:
├── Personal Lens
└── Content Lens

ADMIN Role:
├── Personal Lens
├── Content Lens
└── Admin Lens
```

### Access Rules

**1. Automatic Lens Selection**
When you first log in, the system automatically selects the most appropriate lens:
- **USER role:** Starts in Personal Lens (no other options)
- **EDITOR role:** Starts in Content Lens (can switch to Personal)
- **ADMIN role:** Starts in Admin Lens (can switch to any lens)

**2. Lens Switcher Visibility**
- Users with only one available lens don't see the switcher (no need for it)
- Users with multiple lens options see the switcher in the sidebar
- The switcher shows all lenses you have permission to access

**3. Permission Inheritance**
Higher permission levels inherit access to lower-level lenses:
- **ADMIN** inherits all EDITOR and USER capabilities
- **EDITOR** inherits all USER capabilities
- **USER** has base-level access

**4. Dynamic Access Control**
Each lens enforces its own access rules:
- Navigation items appear/disappear based on active lens
- Dashboard widgets change to match lens context
- Page access is validated (redirect if page not available in current lens)
- Features and actions are enabled/disabled based on lens permissions

### Security Considerations

**Principle of Least Privilege:**
Even if you have ADMIN permissions, working in Personal or Content Lens restricts your access to match that lens's capabilities. This follows the security principle of "least privilege" - only accessing the permissions you need for your current task.

**Example:**
An ADMIN user working in Personal Lens cannot access user management features, even though they have the underlying permission. They must explicitly switch to Admin Lens to access those features.

**Benefits:**
- Reduces accidental misuse of administrative features
- Provides audit trail clarity (lens selection logged)
- Encourages intentional use of elevated permissions
- Minimizes security risk from credential compromise

---

## Frequently Asked Questions

### Can I change my default lens?

Your last-used lens becomes your default. If you always want to start in a specific lens, simply switch to it before logging out, and you'll return to it on your next login.

### What happens if I navigate to a page that's not available in my current lens?

The system automatically redirects you to the dashboard for your current lens. For example, if you're in Personal Lens and somehow navigate to a competency editing page, you'll be smoothly redirected to your Personal Dashboard.

### Why can't I see the Lens Switcher?

If you don't see the Lens Switcher, it means you only have permission for one lens (typically Personal Lens for regular users). Contact your system administrator if you believe you should have access to additional lenses.

### Do different lenses show different data?

No, all lenses access the same underlying data. The difference is in which features and views are available. For example, all lenses can view competencies, but only Content and Admin lenses can edit them.

### Can my lens permissions change?

Yes, if your organizational role changes and an administrator updates your system permissions, you may gain or lose access to certain lenses. Changes take effect on your next login.

### Is there a performance difference between lenses?

No, lens selection is purely a UI/UX feature. All lenses perform identically in terms of speed and responsiveness.

---

## Best Practices

### For Regular Users (Personal Lens)

- Use the Personal Lens for all assessment participation
- Review your progress regularly through the My Progress widget
- Keep your profile information up to date
- Focus on completing assessments without distraction

### For Content Managers (Content Lens)

- Work in Content Lens when designing or editing assessment materials
- Use the Pending Reviews widget to stay on top of approval workflows
- Switch to Personal Lens when you need to test assessments from a user perspective
- Regularly review Content Statistics to monitor library health

### For Administrators (Admin Lens)

- Use Admin Lens for user management and system configuration
- Switch to Content Lens to review framework quality and content issues
- Switch to Personal Lens to experience the platform as end-users do
- Monitor Security Alerts widget daily for potential issues
- Leverage User Activity widget to understand platform adoption

### General Tips

- **Choose the right lens for your task:** Don't work in Admin Lens if you only need to edit content
- **Leverage lens switching for context shifts:** Quickly move between roles without mental overhead
- **Remember your lens selection persists:** You'll start where you left off
- **Use lens-appropriate workflows:** Each lens is optimized for specific tasks
- **Explore all available lenses:** Understand what each offers to maximize productivity

---

## Conclusion

The Lens System represents a modern approach to enterprise software design: **adaptive interfaces that serve multiple user types without compromise**. By providing role-specific views that users can switch between at will, SkillSoft delivers both simplicity and power.

Whether you're an employee taking assessments, an HR professional designing competency frameworks, or a system administrator managing the platform, the Lens System ensures you see exactly what you need - nothing more, nothing less.

This focused approach reduces complexity, improves productivity, and creates a more enjoyable user experience for everyone who interacts with the SkillSoft platform.

---

**Document Version:** 1.0
**Last Updated:** December 2025
**Applies to:** SkillSoft Platform v1.x

---

# Technical Implementation Guide

This section provides comprehensive technical documentation for developers who need to understand, maintain, extend, or debug the Lens System implementation.

## Architecture Overview

### Core Components

The Lens System is implemented as a React Context-based state management solution with three main components:

**1. LensContext (src/context/LensContext.tsx) - 385 lines**
- Central state management for lens selection
- Route visibility filtering logic
- Feature flag system
- localStorage persistence with SSR safety
- Role-based permission enforcement

**2. LensSwitcher (src/components/layout/lens-switcher.tsx) - 233 lines**
- UI component for lens selection
- Dropdown menu with animations
- Lens indicator for collapsed sidebar
- Accessibility features

**3. AppSidebar (src/components/layout/app-sidebar.tsx) - 515 lines**
- Sidebar navigation integration
- Route filtering based on active lens
- Dynamic navigation items
- Lens change animations

### Design Principles

**Configuration-Driven Design:**
All lens configurations are centralized in the `LENS_CONFIGS` object, making it easy to modify lens behavior without changing implementation logic.

**Type Safety:**
TypeScript exhaustive switches ensure all lens types are handled correctly throughout the codebase.

**SSR Compatibility:**
Careful handling of browser-only APIs (localStorage) with Next.js 16 Server Components.

**Performance Optimized:**
Extensive use of React memoization (useMemo, useCallback) and lazy initialization to prevent unnecessary re-renders.

**Animation System:**
Smooth lens transitions with hardware-accelerated CSS animations and respect for reduced motion preferences.

---

## Implementation Details

### Lens Configuration Structure

Each lens is defined with a comprehensive configuration object:

```typescript
export interface LensConfig {
  id: LensType;                  // "user" | "editor" | "admin"
  name: string;                  // Display name (e.g., "Личное")
  description: string;           // Subtitle shown in switcher
  icon: string;                  // Icon identifier
  color: string;                 // Tailwind color class for text
  bgColor: string;              // Tailwind background color
  borderColor: string;          // Tailwind border color
  visibleRoutes: string[];      // Routes accessible in this lens
  dashboardWidgets: string[];   // Dashboard components to show
  features: string[];           // Feature flags enabled
}

export const LENS_CONFIGS: Record<LensType, LensConfig> = {
  user: {
    id: "user",
    name: "Личное",
    description: "Ваш профиль и настройки",
    icon: "user",
    color: "text-emerald-600 dark:text-emerald-400",
    bgColor: "bg-emerald-50 dark:bg-emerald-950/40",
    borderColor: "border-emerald-200/60 dark:border-emerald-800/60",
    visibleRoutes: ["/dashboard", "/test-templates"],
    dashboardWidgets: ["my-progress", "recent-activity"],
    features: ["view-profile", "take-tests"],
  },
  editor: {
    id: "editor",
    name: "Контент",
    description: "Управление компетенциями и содержимым",
    icon: "edit",
    color: "text-blue-600 dark:text-blue-400",
    bgColor: "bg-blue-50 dark:bg-blue-950/40",
    borderColor: "border-blue-200/60 dark:border-blue-800/60",
    visibleRoutes: [
      "/dashboard",
      "/test-templates",
      "/hr/competencies",
      "/hr/behavioral-indicators",
      "/hr/assessment-questions",
      "/skill-mapper",
    ],
    dashboardWidgets: [
      "overview",
      "content-stats",
      "pending-reviews",
      "recent-edits",
    ],
    features: [
      "view-competencies",
      "edit-competencies",
      "create-competencies",
      "edit-indicators",
      "create-indicators",
      "edit-questions",
      "create-questions",
    ],
  },
  admin: {
    id: "admin",
    name: "Админ",
    description: "Полное управление системой",
    icon: "shield",
    color: "text-violet-600 dark:text-violet-400",
    bgColor: "bg-violet-50 dark:bg-violet-950/40",
    borderColor: "border-violet-200/60 dark:border-violet-800/60",
    visibleRoutes: [
      "/dashboard",
      "/test-templates",
      "/hr/competencies",
      "/hr/behavioral-indicators",
      "/hr/assessment-questions",
      "/skill-mapper",
      "/admin/users",
    ],
    dashboardWidgets: [
      "overview",
      "system-stats",
      "user-activity",
      "content-stats",
      "security-alerts",
    ],
    features: [
      "view-competencies",
      "edit-competencies",
      "create-competencies",
      "delete-competencies",
      "view-indicators",
      "edit-indicators",
      "create-indicators",
      "delete-indicators",
      "view-questions",
      "edit-questions",
      "create-questions",
      "delete-questions",
      "manage-users",
      "system-settings",
    ],
  },
};
```

### LensContext API

The context provides these functions and values:

```typescript
interface LensContextProps {
  // Current state
  activeLens: LensType;           // Currently selected lens
  lensConfig: LensConfig;         // Configuration for active lens
  userRole: UserRole;             // User's actual role from Clerk
  availableLenses: LensType[];    // Lenses user can access

  // Actions
  setLens: (lens: LensType) => void;              // Switch lens
  setUserRole: (role: UserRole) => void;          // Update user role

  // Queries
  isRouteVisible: (route: string) => boolean;     // Check route access
  hasFeature: (feature: string) => boolean;       // Check feature flag
}
```

### Lens Switching Mechanism

The lens switching process involves several steps:

**1. User Interaction:**
```typescript
// In LensSwitcher component
const handleLensSelect = useCallback(
  (lensId: LensType) => () => setLens(lensId),
  [setLens]
);
```

**2. Permission Validation:**
```typescript
// In LensContext
const setLens = useCallback((lens: LensType) => {
  const availableLenses = getAvailableLenses(userRoleState);

  // Only allow switching to lenses user has permission for
  if (availableLenses.includes(lens)) {
    setActiveLens(lens);

    // Check if current route is accessible in new lens
    const newLensConfig = getLensConfig(lens);
    const normalizedPath = pathname.replace(/\/$/, "") || "/";
    const isRouteAccessible = newLensConfig.visibleRoutes.some(
      (route) =>
        normalizedPath === route ||
        normalizedPath.startsWith(route + "/") ||
        route.startsWith(normalizedPath + "/")
    );

    // Redirect if current route not accessible
    if (!isRouteAccessible && isMountedRef.current) {
      const fallbackRoute = "/dashboard";
      router.push(fallbackRoute);
    }
  }
}, [userRoleState, pathname, router]);
```

**3. State Update:**
```typescript
// Active lens state updated
setActiveLens(lens);

// Effective lens computed (handles invalid states)
const effectiveLens = useMemo(() => {
  const availableLenses = getAvailableLenses(userRoleState);
  if (availableLenses.includes(activeLens)) {
    return activeLens;
  }
  return getDefaultLens(userRoleState);
}, [userRoleState, activeLens]);
```

**4. Persistence:**
```typescript
// Persist to localStorage (only after mount to avoid SSR issues)
useEffect(() => {
  if (isMountedRef.current) {
    localStorage.setItem(LENS_STORAGE_KEY, effectiveLens);
  }
}, [effectiveLens]);
```

**5. UI Update:**
The context update triggers re-renders in consuming components (sidebar navigation, dashboard widgets, etc.).

### Route Visibility Filtering Algorithm

Routes are filtered using a flexible matching algorithm:

```typescript
const isRouteVisible = useCallback(
  (route: string) => {
    // Normalize route (remove trailing slash)
    const normalizedRoute = route.replace(/\/$/, "") || "/";

    return lensConfig.visibleRoutes.some(
      (visibleRoute) =>
        // Exact match
        normalizedRoute === visibleRoute ||
        // Route is a child of visible route
        normalizedRoute.startsWith(visibleRoute + "/") ||
        // Visible route is a child of current route (parent route)
        visibleRoute.startsWith(normalizedRoute + "/")
    );
  },
  [lensConfig]
);
```

**Matching Examples:**

| Current Path | Visible Routes | Match? | Reason |
|-------------|---------------|--------|--------|
| /dashboard | ["/dashboard"] | Yes | Exact match |
| /dashboard/stats | ["/dashboard"] | Yes | Child of visible route |
| /hr/competencies/123 | ["/hr/competencies"] | Yes | Child of visible route |
| /admin/users | ["/dashboard"] | No | Not in visible routes |

**Usage in Sidebar:**
```typescript
// Filter navigation items
const visibleNavItems = navData.navMain.filter((item) =>
  isRouteVisible(item.url)
);

const visibleLibraryItems = navData.navLibrary.filter((item) =>
  isRouteVisible(item.url)
);
```

### Feature Flag System

Feature flags control component-level permissions:

```typescript
// Check if feature is enabled in current lens
const hasFeature = useCallback(
  (feature: string) => {
    return lensConfig.features.includes(feature);
  },
  [lensConfig]
);

// Usage in components
export function useHasFeature(feature: string): boolean {
  const { hasFeature } = useLens();
  return hasFeature(feature);
}
```

**Example Usage:**
```typescript
// In a competency editing component
const canEdit = useHasFeature("edit-competencies");
const canDelete = useHasFeature("delete-competencies");

return (
  <div>
    {canEdit && <EditButton />}
    {canDelete && <DeleteButton />}
  </div>
);
```

### Permission Model Integration

Lens availability is determined by user role:

```typescript
function getAvailableLenses(role: UserRole): LensType[] {
  switch (role) {
    case UserRole.ADMIN:
      return ["user", "editor", "admin"];  // All lenses
    case UserRole.EDITOR:
      return ["user", "editor"];           // User + Editor
    case UserRole.USER:
    default:
      return ["user"];                     // User only
  }
}

function getDefaultLens(role: UserRole): LensType {
  switch (role) {
    case UserRole.ADMIN:
      return "admin";    // Start in most powerful lens
    case UserRole.EDITOR:
      return "editor";   // Start in content management
    case UserRole.USER:
    default:
      return "user";     // Only option for regular users
  }
}
```

**Role Upgrade Behavior:**
When a user's role is upgraded (e.g., from USER to EDITOR), the system automatically selects the role-appropriate default lens:

```typescript
const setUserRole = useCallback((role: UserRole) => {
  setUserRoleState(role);

  // Auto-select role-appropriate default lens when role is upgraded
  // Only if user didn't have a stored lens preference
  if (
    hadStoredLensRef.current === false &&  // No stored preference
    !appliedRoleDefaultRef.current &&      // Not already applied
    role !== UserRole.USER                 // Role was upgraded
  ) {
    appliedRoleDefaultRef.current = true;
    const defaultLens = getDefaultLens(role);
    setActiveLens(defaultLens);
  }
}, []);
```

### localStorage Persistence Strategy

Lens selection persists across sessions using localStorage with careful SSR handling:

```typescript
const LENS_STORAGE_KEY = "skillsoft-active-lens";

// 1. Initialize from localStorage with lazy initializer
const [activeLens, setActiveLens] = useState<LensType>(() => {
  // SSR guard
  if (typeof window === "undefined") return "user";

  const storedLens = localStorage.getItem(LENS_STORAGE_KEY) as LensType | null;
  if (storedLens) {
    const availableLenses = getAvailableLenses(UserRole.USER);
    // Validate stored lens is still accessible
    if (availableLenses.includes(storedLens)) {
      return storedLens;
    }
  }
  return "user";
});

// 2. Track if user had a stored preference
const hadStoredLensRef = useRef<boolean | null>(null);

useEffect(() => {
  isMountedRef.current = true;
  const storedLens = localStorage.getItem(LENS_STORAGE_KEY) as LensType | null;
  hadStoredLensRef.current = !!storedLens;
}, []);

// 3. Persist lens changes (only after mount)
useEffect(() => {
  if (isMountedRef.current) {
    localStorage.setItem(LENS_STORAGE_KEY, effectiveLens);
  }
}, [effectiveLens]);
```

**Why This Approach:**
- **Lazy initialization** prevents extra re-renders
- **SSR guard** avoids "window is not defined" errors
- **Mount tracking** prevents hydration mismatches
- **Validation** ensures stored lens is still valid for current user role

### Automatic Fallback/Redirect Logic

When switching lenses or navigating to inaccessible routes:

```typescript
// Check if current route is accessible in the new lens
const newLensConfig = getLensConfig(lens);
const normalizedPath = pathname.replace(/\/$/, "") || "/";
const isRouteAccessible = newLensConfig.visibleRoutes.some(
  (route) =>
    normalizedPath === route ||
    normalizedPath.startsWith(route + "/") ||
    route.startsWith(normalizedPath + "/")
);

// Redirect to appropriate fallback page if current route is inaccessible
if (!isRouteAccessible && isMountedRef.current) {
  const fallbackRoute = "/dashboard";
  router.push(fallbackRoute);
}
```

**Fallback Decision Tree:**
1. Check if current path is accessible in new lens
2. If not accessible → redirect to /dashboard
3. Mount check prevents SSR redirects
4. Uses Next.js router for smooth navigation

---

## Next.js 16 Integration

### App Router Compatibility

The Lens System is fully integrated with Next.js 16 App Router:

**Client Component Boundaries:**
```typescript
// LensContext.tsx
"use client";  // Required for state management and hooks

import { useRouter, usePathname } from "next/navigation";
```

**Provider Setup:**
```typescript
// In layout.tsx or provider wrapper
import { LensProvider } from "@/context/LensContext";

export default function Layout({ children }) {
  return (
    <LensProvider>
      {children}
    </LensProvider>
  );
}
```

### usePathname and useRouter Usage

**usePathname** - Get current route for visibility checks:
```typescript
const pathname = usePathname();

// Normalize path for comparison
const normalizedPath = pathname.replace(/\/$/, "") || "/";

// Check accessibility
const isRouteAccessible = newLensConfig.visibleRoutes.some(
  (route) =>
    normalizedPath === route ||
    normalizedPath.startsWith(route + "/")
);
```

**useRouter** - Programmatic navigation:
```typescript
const router = useRouter();

// Redirect when route not accessible
if (!isRouteAccessible && isMountedRef.current) {
  router.push("/dashboard");
}
```

### SSR Considerations

**localStorage Access:**
Always guard with SSR checks:
```typescript
// WRONG - will crash during SSR
const stored = localStorage.getItem(key);

// RIGHT - SSR safe
if (typeof window !== "undefined") {
  const stored = localStorage.getItem(key);
}
```

**Lazy State Initialization:**
```typescript
// Executes only once, client-side
const [lens, setLens] = useState<LensType>(() => {
  if (typeof window === "undefined") return "user";
  // ... localStorage logic
});
```

**Mount Tracking:**
```typescript
const isMountedRef = useRef(false);

useEffect(() => {
  isMountedRef.current = true;
}, []);

// Only perform browser-only operations after mount
if (isMountedRef.current) {
  localStorage.setItem(key, value);
}
```

### Client vs Server Components

**LensContext and LensSwitcher:** Client components (use state, effects, browser APIs)

**Dashboard Pages:** Can be server components, consume context via client wrappers

**Example Pattern:**
```typescript
// page.tsx (Server Component)
import { DashboardClientWrapper } from "./client-wrapper";

export default function DashboardPage() {
  return <DashboardClientWrapper />;
}

// client-wrapper.tsx (Client Component)
"use client";

import { useLens } from "@/context/LensContext";

export function DashboardClientWrapper() {
  const { lensConfig } = useLens();
  // ... use context
}
```

---

## Performance Optimizations

### useMemo Patterns

**Effective Lens Computation:**
Prevents re-calculation on every render:
```typescript
const effectiveLens = useMemo(() => {
  const availableLenses = getAvailableLenses(userRoleState);
  if (availableLenses.includes(activeLens)) {
    return activeLens;
  }
  return getDefaultLens(userRoleState);
}, [userRoleState, activeLens]);
```

**Lens Options Memoization:**
```typescript
// In LensSwitcher
const lensOptions = useMemo(
  () => availableLenses.map((id) => ({ id, config: getLensConfig(id) })),
  [availableLenses]
);
```

### useCallback Patterns

**Event Handlers:**
```typescript
const handleLensSelect = useCallback(
  (lensId: LensType) => () => setLens(lensId),
  [setLens]
);
```

**Context API Functions:**
```typescript
const setUserRole = useCallback((role: UserRole) => {
  setUserRoleState(role);
  // ... logic
}, []);

const isRouteVisible = useCallback(
  (route: string) => {
    // ... logic
  },
  [lensConfig]
);

const hasFeature = useCallback(
  (feature: string) => {
    return lensConfig.features.includes(feature);
  },
  [lensConfig]
);
```

**Why useCallback:**
- Prevents unnecessary re-renders of child components
- Stable function references for dependency arrays
- Essential for context providers (prevents all consumers re-rendering)

### Ref Tracking

Three key refs prevent unnecessary updates:

**1. Mount State Tracking:**
```typescript
const isMountedRef = useRef(false);

useEffect(() => {
  isMountedRef.current = true;
}, []);

// Prevents SSR/hydration issues
if (isMountedRef.current) {
  // Browser-only operations
}
```

**2. Lens Change Tracking:**
```typescript
const prevLensRef = useRef(activeLens);

useEffect(() => {
  if (prevLensRef.current !== activeLens) {
    // Lens changed - trigger animation
    prevLensRef.current = activeLens;
  }
}, [activeLens]);
```

**3. Stored Preference Tracking:**
```typescript
const hadStoredLensRef = useRef<boolean | null>(null);

useEffect(() => {
  const storedLens = localStorage.getItem(LENS_STORAGE_KEY);
  hadStoredLensRef.current = !!storedLens;
}, []);

// Determines if we should auto-select default lens on role upgrade
```

### Lazy Initialization

**State Initializer Functions:**
```typescript
// Runs ONCE on mount, not on every render
const [activeLens, setActiveLens] = useState<LensType>(() => {
  if (typeof window === "undefined") return "user";
  // ... expensive localStorage logic
  return storedLens || "user";
});
```

**Why This Matters:**
- Without lazy init: initialization code runs on every render
- With lazy init: runs once, better performance
- Especially important for operations involving localStorage access

### Avoiding Hydration Mismatches

**The Problem:**
If server-rendered HTML differs from client-rendered HTML, React throws hydration errors.

**The Solution:**
```typescript
// 1. Use ClientOnly wrapper for client-specific content
<ClientOnly fallback={<LoadingState />}>
  <LensSwitcher />
</ClientOnly>

// 2. Use same initial state on server and client
const [lens, setLens] = useState<LensType>(() => {
  // Always return "user" during SSR
  if (typeof window === "undefined") return "user";
  // Client can read from localStorage
  return getStoredLens() || "user";
});

// 3. Update state after mount (useEffect)
useEffect(() => {
  // This runs only on client, after hydration
  const storedLens = getStoredLens();
  if (storedLens) setLens(storedLens);
}, []);
```

---

## Developer Guide

### How to Add a New Lens

Follow this step-by-step process to add a new lens to the system:

**Step 1: Update Type Definition**
```typescript
// In src/context/LensContext.tsx
export type LensType = "user" | "editor" | "admin" | "newlens";
```

**Step 2: Define Lens Configuration**
```typescript
export const LENS_CONFIGS: Record<LensType, LensConfig> = {
  // ... existing lenses
  newlens: {
    id: "newlens",
    name: "New Lens",
    description: "Description for new lens",
    icon: "icon-name",  // Must match icon in LensIcon component
    color: "text-green-600 dark:text-green-400",
    bgColor: "bg-green-50 dark:bg-green-950/40",
    borderColor: "border-green-200/60 dark:border-green-800/60",
    visibleRoutes: [
      "/dashboard",
      "/newlens/specific-route",
    ],
    dashboardWidgets: [
      "newlens-widget-1",
      "newlens-widget-2",
    ],
    features: [
      "newlens-feature-1",
      "newlens-feature-2",
    ],
  },
};
```

**Step 3: Update Icon Component**
```typescript
// In src/components/layout/lens-switcher.tsx
import { NewLensIcon } from "lucide-react";

function LensIcon({ type, className }: { type: string; className?: string }) {
  switch (type) {
    case "newlens":
      return <NewLensIcon className={className} />;
    // ... other cases
  }
}
```

**Step 4: Update Permission Mapping**
```typescript
// In src/context/LensContext.tsx
function getAvailableLenses(role: UserRole): LensType[] {
  switch (role) {
    case UserRole.NEWROLE:  // If adding new role
      return ["user", "newlens"];
    case UserRole.ADMIN:
      return ["user", "editor", "admin", "newlens"];  // Add to existing roles
    // ... other cases
  }
}

function getDefaultLens(role: UserRole): LensType {
  switch (role) {
    case UserRole.NEWROLE:
      return "newlens";  // Default for new role
    // ... other cases
  }
}
```

**Step 5: Update Safe Getter Functions**
```typescript
// In src/context/LensContext.tsx
function getLensConfig(lens: LensType): LensConfig {
  switch (lens) {
    case "newlens":
      return LENS_CONFIGS.newlens;
    // ... other cases
  }
}

// In src/components/layout/lens-switcher.tsx
function getLensConfig(lensId: LensType) {
  switch (lensId) {
    case "newlens":
      return LENS_CONFIGS.newlens;
    // ... other cases
  }
}
```

**Step 6: Add Animation Classes (Optional)**
```typescript
// In src/components/layout/app-sidebar.tsx
const LENS_GLOW_CLASSES = {
  user: "animate-lens-glow-emerald",
  editor: "animate-lens-glow-blue",
  admin: "animate-lens-glow-violet",
  newlens: "animate-lens-glow-green",  // Add new animation
} as const;

function getLensGlowClass(lens: keyof typeof LENS_GLOW_CLASSES): string {
  switch (lens) {
    case "newlens":
      return LENS_GLOW_CLASSES.newlens;
    // ... other cases
  }
}
```

**Step 7: Add Tailwind Animation (If Custom)**
```typescript
// In tailwind.config.ts
module.exports = {
  theme: {
    extend: {
      animation: {
        'lens-glow-green': 'lens-glow-green 0.7s ease-out',
      },
      keyframes: {
        'lens-glow-green': {
          '0%, 100%': {
            boxShadow: '0 0 0 0 rgba(34, 197, 94, 0)'
          },
          '50%': {
            boxShadow: '0 0 20px 5px rgba(34, 197, 94, 0.3)'
          },
        },
      },
    },
  },
};
```

**Step 8: Test**
- Switch to new lens via dropdown
- Verify route filtering works
- Check feature flags
- Test persistence (refresh page)
- Verify animations
- Test with different user roles

### How to Add Routes/Features to Existing Lenses

**Adding a Route:**
```typescript
// In src/context/LensContext.tsx
export const LENS_CONFIGS: Record<LensType, LensConfig> = {
  editor: {
    // ... existing config
    visibleRoutes: [
      "/dashboard",
      "/hr/competencies",
      "/new/route",  // Add here
    ],
  },
};
```

**Adding a Feature Flag:**
```typescript
export const LENS_CONFIGS: Record<LensType, LensConfig> = {
  editor: {
    // ... existing config
    features: [
      "edit-competencies",
      "new-feature",  // Add here
    ],
  },
};
```

**Using the New Feature Flag:**
```typescript
// In any component
const canDoNewThing = useHasFeature("new-feature");

return (
  <div>
    {canDoNewThing && <NewFeatureButton />}
  </div>
);
```

**Adding a Dashboard Widget:**
```typescript
export const LENS_CONFIGS: Record<LensType, LensConfig> = {
  editor: {
    // ... existing config
    dashboardWidgets: [
      "overview",
      "new-widget",  // Add here
    ],
  },
};

// In dashboard page
const { lensConfig } = useLens();
const shouldShowWidget = lensConfig.dashboardWidgets.includes("new-widget");

return (
  <div>
    {shouldShowWidget && <NewWidget />}
  </div>
);
```

### Common Debugging Issues and Solutions

**Issue 1: Lens Switcher Not Appearing**

**Symptoms:** No dropdown menu visible in sidebar

**Causes:**
- User only has access to one lens
- LensProvider not wrapping component tree
- ClientOnly wrapper missing

**Solution:**
```typescript
// Check available lenses
const { availableLenses } = useLens();
console.log("Available lenses:", availableLenses);

// Verify LensProvider is present
// In layout.tsx or app root
<LensProvider>
  <AppSidebar />
</LensProvider>

// Use ClientOnly wrapper
<ClientOnly fallback={null}>
  <LensSwitcher />
</ClientOnly>
```

**Issue 2: Routes Not Filtering Correctly**

**Symptoms:** Wrong navigation items visible in sidebar

**Causes:**
- Route not added to visibleRoutes
- Route path mismatch (trailing slashes, etc.)
- Route normalization issue

**Solution:**
```typescript
// 1. Check lens config
console.log("Current lens config:", lensConfig);
console.log("Visible routes:", lensConfig.visibleRoutes);

// 2. Test route visibility
const isVisible = isRouteVisible("/your/route");
console.log("Is route visible:", isVisible);

// 3. Ensure route paths match exactly
// In config: "/hr/competencies"
// In navigation: "/hr/competencies" (not "/hr/competencies/")

// 4. Debug route matching
const normalizedRoute = route.replace(/\/$/, "") || "/";
console.log("Normalized route:", normalizedRoute);
```

**Issue 3: Hydration Mismatch Errors**

**Symptoms:** React hydration errors, content flash on page load

**Causes:**
- Different initial state on server vs client
- localStorage accessed during SSR
- Missing ClientOnly wrapper

**Solution:**
```typescript
// 1. Always use lazy initialization
const [lens, setLens] = useState<LensType>(() => {
  if (typeof window === "undefined") return "user";
  return getStoredLens() || "user";
});

// 2. Wrap client-specific components
<ClientOnly fallback={<Skeleton />}>
  <LensSwitcher />
</ClientOnly>

// 3. Use mount ref
const isMountedRef = useRef(false);
useEffect(() => {
  isMountedRef.current = true;
}, []);

if (isMountedRef.current) {
  // Client-only operations
}
```

**Issue 4: Lens Selection Not Persisting**

**Symptoms:** Lens resets to default on page refresh

**Causes:**
- localStorage not writing
- Incorrect storage key
- Effect running before mount

**Solution:**
```typescript
// 1. Check mount state
console.log("Is mounted:", isMountedRef.current);

// 2. Verify storage write
useEffect(() => {
  console.log("Saving lens:", effectiveLens);
  if (isMountedRef.current) {
    localStorage.setItem(LENS_STORAGE_KEY, effectiveLens);
  }
}, [effectiveLens]);

// 3. Verify storage read
const storedLens = localStorage.getItem(LENS_STORAGE_KEY);
console.log("Stored lens:", storedLens);
```

**Issue 5: Infinite Redirect Loop**

**Symptoms:** Page keeps redirecting, browser hangs

**Causes:**
- Fallback route not accessible in lens
- Redirect triggering on every render
- Missing mount check in redirect logic

**Solution:**
```typescript
// 1. Ensure fallback is always accessible
const fallbackRoute = "/dashboard";  // Must be in all lens configs

// 2. Only redirect after mount
if (!isRouteAccessible && isMountedRef.current) {
  router.push(fallbackRoute);
}

// 3. Add redirect tracking
const hasRedirectedRef = useRef(false);
if (!isRouteAccessible && !hasRedirectedRef.current) {
  hasRedirectedRef.current = true;
  router.push(fallbackRoute);
}
```

### Maintenance Best Practices

**1. Keep Lens Configs DRY:**
```typescript
// Define shared route groups
const HR_ROUTES = [
  "/hr/competencies",
  "/hr/behavioral-indicators",
  "/hr/assessment-questions",
];

// Reuse in configs
export const LENS_CONFIGS: Record<LensType, LensConfig> = {
  editor: {
    visibleRoutes: ["/dashboard", ...HR_ROUTES],
  },
  admin: {
    visibleRoutes: ["/dashboard", ...HR_ROUTES, "/admin/users"],
  },
};
```

**2. Use Type-Safe Exhaustive Switches:**
```typescript
// TypeScript will error if a lens case is missing
function getLensConfig(lens: LensType): LensConfig {
  switch (lens) {
    case "user":
      return LENS_CONFIGS.user;
    case "editor":
      return LENS_CONFIGS.editor;
    case "admin":
      return LENS_CONFIGS.admin;
    // If you add a new LensType and forget this case, TypeScript errors
    default:
      const _exhaustive: never = lens;
      return LENS_CONFIGS.user;
  }
}
```

**3. Centralize Route Constants:**
```typescript
// Define once
const ROUTE_DASHBOARD = "/dashboard";
const ROUTE_COMPETENCIES = "/hr/competencies";

// Use everywhere
visibleRoutes: [ROUTE_DASHBOARD, ROUTE_COMPETENCIES],
```

**4. Document Feature Flags:**
```typescript
// Create a central feature flag registry
export const FEATURE_FLAGS = {
  // Competency features
  VIEW_COMPETENCIES: "view-competencies",
  EDIT_COMPETENCIES: "edit-competencies",
  CREATE_COMPETENCIES: "create-competencies",
  DELETE_COMPETENCIES: "delete-competencies",

  // Indicator features
  VIEW_INDICATORS: "view-indicators",
  // ... etc
} as const;

// Use in configs
features: [
  FEATURE_FLAGS.VIEW_COMPETENCIES,
  FEATURE_FLAGS.EDIT_COMPETENCIES,
],
```

**5. Test Lens Changes:**
```typescript
// Write tests for lens switching logic
describe("LensContext", () => {
  it("should allow ADMIN to access all lenses", () => {
    const lenses = getAvailableLenses(UserRole.ADMIN);
    expect(lenses).toEqual(["user", "editor", "admin"]);
  });

  it("should redirect when route not accessible", () => {
    // ... test redirect logic
  });
});
```

**6. Monitor Performance:**
```typescript
// Use React DevTools Profiler
// Check for unnecessary re-renders
// Ensure memoization is working

// Add performance logging (dev only)
if (process.env.NODE_ENV === "development") {
  console.log("Lens changed:", activeLens);
  console.log("Visible routes:", lensConfig.visibleRoutes);
}
```

**7. Version Control Lens Configs:**
When updating lens configurations, document the change:
```typescript
// CHANGELOG.md
// 2025-12-12: Added /skill-mapper route to editor lens
// 2025-12-10: Added delete-competencies feature to admin lens
```

### Code Conventions

**Naming:**
- Lens types: lowercase (`"user"`, `"editor"`, `"admin"`)
- Feature flags: kebab-case (`"edit-competencies"`)
- Route constants: UPPER_SNAKE_CASE (`ROUTE_DASHBOARD`)
- Component names: PascalCase (`LensSwitcher`)
- Hooks: camelCase with `use` prefix (`useLens`)

**File Organization:**
```
src/
├── context/
│   └── LensContext.tsx          # Core logic
├── components/
│   └── layout/
│       ├── lens-switcher.tsx    # UI component
│       └── app-sidebar.tsx      # Integration
└── types/
    └── user.ts                  # UserRole enum
```

**Comments:**
- Document "why" not "what"
- Explain non-obvious business logic
- Mark SSR-specific code with comments
- Document performance optimizations

---

## Animation System

### Lens Switch Animations

**Sidebar Glow Effect:**
When switching lenses, the sidebar briefly glows with the new lens color:

```typescript
// Lens-specific glow animations
const LENS_GLOW_CLASSES = {
  user: "animate-lens-glow-emerald",
  editor: "animate-lens-glow-blue",
  admin: "animate-lens-glow-violet",
} as const;

// Trigger flash on lens change
const [showFlash, setShowFlash] = useState(false);
const prevLensRef = useRef(activeLens);

useEffect(() => {
  if (prevLensRef.current !== activeLens) {
    setShowFlash(true);
    const timer = setTimeout(() => setShowFlash(false), 700);
    prevLensRef.current = activeLens;
    return () => clearTimeout(timer);
  }
}, [activeLens]);

// Apply glow class conditionally
<Sidebar
  className={cn(
    "transition-all duration-300",
    showFlash && getLensGlowClass(activeLens)
  )}
/>
```

**Tailwind Keyframes:**
```typescript
// In tailwind.config.ts
keyframes: {
  'lens-glow-emerald': {
    '0%, 100%': { boxShadow: '0 0 0 0 rgba(16, 185, 129, 0)' },
    '50%': { boxShadow: '0 0 20px 5px rgba(16, 185, 129, 0.3)' },
  },
  'lens-glow-blue': {
    '0%, 100%': { boxShadow: '0 0 0 0 rgba(37, 99, 235, 0)' },
    '50%': { boxShadow: '0 0 20px 5px rgba(37, 99, 235, 0.3)' },
  },
  'lens-glow-violet': {
    '0%, 100%': { boxShadow: '0 0 0 0 rgba(139, 92, 246, 0)' },
    '50%': { boxShadow: '0 0 20px 5px rgba(139, 92, 246, 0.3)' },
  },
}
```

### Nav Item Stagger Animation

Navigation items animate in with a staggered delay when lens changes:

```typescript
// Track lens changes
const [isLensChanging, setIsLensChanging] = useState(false);

useEffect(() => {
  if (prevLensRef.current !== activeLens) {
    setIsLensChanging(true);
    const timer = setTimeout(() => setIsLensChanging(false), 400);
    return () => clearTimeout(timer);
  }
}, [activeLens]);

// Apply stagger animation to each nav item
{visibleNavItems.map((item, index) => (
  <SidebarMenuItem
    key={item.title}
    className={cn(
      "transition-all duration-300",
      isLensChanging && "animate-in fade-in-0 slide-in-from-left-3"
    )}
    style={isLensChanging ? {
      animationDelay: `${index * 50}ms`,
      animationDuration: "300ms",
      animationFillMode: "both"
    } : undefined}
  >
    {/* ... */}
  </SidebarMenuItem>
))}
```

**Stagger Calculation:**
Each item delays by `index * 50ms`, creating a cascading effect.

### Dropdown Animations

**Opening/Closing:**
```typescript
<DropdownMenuContent
  className={cn(
    // Slide and fade animation
    "data-[state=open]:animate-in data-[state=closed]:animate-out",
    "data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0",
    "data-[state=closed]:slide-out-to-bottom-2 data-[state=open]:slide-in-from-bottom-2",
    "duration-150"
  )}
>
```

**Item Stagger:**
```typescript
{lensOptions.map(({ id, config }, index) => (
  <DropdownMenuItem
    style={{ animationDelay: `${index * 30}ms` }}
  >
    {/* ... */}
  </DropdownMenuItem>
))}
```

### Micro-Interactions

**Icon Scale on Hover:**
```typescript
const ANIM = {
  base: "transition-all duration-200 ease-out motion-reduce:transition-none",
  transform: "transition-transform duration-200 ease-out motion-reduce:transition-none",
} as const;

<div className={cn(ANIM.base, "group-hover/lens:scale-105")}>
  <LensIcon className={cn("size-4", ANIM.transform)} />
</div>
```

**Check Indicator Animation:**
```typescript
{isActive && (
  <Check
    className={cn(
      "size-4",
      "animate-in zoom-in-50 duration-200 motion-reduce:animate-none"
    )}
  />
)}
```

**Chevron Rotation:**
```typescript
<ChevronDown
  className={cn(
    ANIM.transform,
    "group-data-[state=open]/lens:rotate-180"
  )}
/>
```

### Accessibility: Reduced Motion

All animations respect user's motion preferences:

```typescript
const ANIM = {
  base: "transition-all duration-200 ease-out motion-reduce:transition-none",
  // ...
};

// Users with prefers-reduced-motion: reduce see no animations
```

**CSS Media Query:**
```css
@media (prefers-reduced-motion: reduce) {
  .motion-reduce\:transition-none {
    transition: none !important;
  }
  .motion-reduce\:animate-none {
    animation: none !important;
  }
}
```

---

## Integration Points

### Clerk Authentication Integration

**User Role Sync:**
The Lens System integrates with Clerk for role-based access:

```typescript
// In AppSidebar or layout component
import { useUser } from "@clerk/nextjs";

const { user: clerkUser } = useUser();
const { setUserRole } = useLens();

// Sync Clerk user metadata to Lens System
useEffect(() => {
  if (clerkUser?.publicMetadata?.role) {
    const role = clerkUser.publicMetadata.role as UserRole;
    setUserRole(role);
  }
}, [clerkUser, setUserRole]);
```

**User Metadata Structure:**
```typescript
// Clerk user.publicMetadata
{
  role: "ADMIN" | "EDITOR" | "USER",
  // ... other metadata
}
```

**Role Retrieval:**
```typescript
// Backend endpoint or Clerk webhook sets role in metadata
// Frontend reads and syncs to LensContext
const userRole = clerkUser?.publicMetadata?.role || UserRole.USER;
setUserRole(userRole);
```

### Sidebar Navigation Filtering

**Dynamic Navigation Data:**
```typescript
// In AppSidebar
const isPersonalView = activeLens === "user";

const navData = {
  navMain: isPersonalView
    ? [
        { title: "My Hub", url: "/dashboard", icon: BarChart3 },
        { title: "Assessment Center", url: "/test-templates", icon: ClipboardCheck },
      ]
    : [
        { title: "Hiring Overview", url: "/dashboard", icon: BarChart3 },
        { title: "Studio", url: "/test-templates", icon: ClipboardCheck },
      ],
  // ...
};
```

**Filtering Navigation Items:**
```typescript
const visibleNavItems = navData.navMain.filter((item) =>
  isRouteVisible(item.url)
);

const visibleLibraryItems = navData.navLibrary.filter((item) =>
  isRouteVisible(item.url)
);

const visibleAdminItems = navData.navAdmin.filter((item) =>
  isRouteVisible(item.url)
);
```

**Conditional Sections:**
```typescript
{/* Only show Library section if user has access to library routes */}
{visibleLibraryItems.length > 0 && (
  <SidebarGroup>
    <SidebarGroupLabel>Библиотека</SidebarGroupLabel>
    <SidebarMenu>
      {visibleLibraryItems.map((item) => (
        <SidebarMenuItem key={item.title}>
          {/* ... */}
        </SidebarMenuItem>
      ))}
    </SidebarMenu>
  </SidebarGroup>
)}
```

### Feature Flag Usage in Components

**Component-Level Permissions:**
```typescript
// In a competency card component
const canEdit = useHasFeature("edit-competencies");
const canDelete = useHasFeature("delete-competencies");

return (
  <Card>
    <CardHeader>
      <CardTitle>{competency.name}</CardTitle>
    </CardHeader>
    <CardContent>
      {/* ... */}
    </CardContent>
    <CardFooter>
      {canEdit && (
        <Button onClick={handleEdit}>
          <Pencil className="mr-2 h-4 w-4" />
          Edit
        </Button>
      )}
      {canDelete && (
        <Button variant="destructive" onClick={handleDelete}>
          <Trash className="mr-2 h-4 w-4" />
          Delete
        </Button>
      )}
    </CardFooter>
  </Card>
);
```

**Dashboard Widget Filtering:**
```typescript
// In dashboard page
const { lensConfig } = useLens();

const shouldShowWidget = (widgetId: string) =>
  lensConfig.dashboardWidgets.includes(widgetId);

return (
  <div className="dashboard-grid">
    {shouldShowWidget("overview") && <OverviewWidget />}
    {shouldShowWidget("content-stats") && <ContentStatsWidget />}
    {shouldShowWidget("user-activity") && <UserActivityWidget />}
    {shouldShowWidget("security-alerts") && <SecurityAlertsWidget />}
  </div>
);
```

**Page-Level Access Control:**
```typescript
// In a protected page component
import { useLens } from "@/context/LensContext";
import { useRouter } from "next/navigation";
import { useEffect } from "react";

export function AdminOnlyPage() {
  const { isRouteVisible } = useLens();
  const router = useRouter();

  useEffect(() => {
    // Redirect if user doesn't have access
    if (!isRouteVisible("/admin/users")) {
      router.push("/dashboard");
    }
  }, [isRouteVisible, router]);

  return <div>{/* Admin content */}</div>;
}
```

---

## Summary

The Lens System provides a robust, type-safe, and performant solution for role-based view management in the SkillSoft platform. Key technical highlights:

- **Configuration-driven** design for easy maintenance
- **Type-safe exhaustive switches** prevent runtime errors
- **SSR-compatible** with Next.js 16 App Router
- **Performance optimized** with memoization and lazy initialization
- **Smooth animations** with accessibility support
- **Flexible permission model** supporting role upgrades
- **Automatic route protection** with intelligent fallbacks

By following the developer guide and best practices in this document, you can confidently extend, debug, and maintain the Lens System to meet evolving product requirements.

---

**Technical Documentation Version:** 1.0
**Last Updated:** December 2025
**Maintainer:** SkillSoft Platform Team
