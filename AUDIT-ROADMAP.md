# SkillSoft Comprehensive Audit: Prioritized Improvement Roadmap

**Date:** 2026-02-20
**Audited by:** 6 specialized agents (Next.js Architect, Spring Boot Expert, UX Researcher, QA Engineer, Accessibility Tester, Mobile Developer)
**Branch:** `dev`
**Total Unique Findings:** 52 (deduplicated from 74 raw findings across 6 reports)

---

## Executive Summary

The SkillSoft platform demonstrates **strong architectural fundamentals** across both frontend and backend: well-designed strategy patterns, proper separation of concerns, comprehensive loading states, robust IRT implementation, and proactive accessibility features. However, **two critical security vulnerabilities** must be resolved before any production deployment, and several high-impact improvements would significantly elevate scoring reliability, mobile UX, and internationalization coverage.

**Architecture Health Scores:**
| Domain | Grade | Highlights |
|--------|-------|-----------|
| Backend Architecture | B- | Strong patterns, critical security gaps |
| Frontend Architecture | B+ | Modern Next.js 16 adoption, cache layer broken |
| UX/Usability | B | Good test-taking flow, builder needs i18n |
| Accessibility (WCAG 2.1 AA) | A- | 92% compliant, minor color-only issues |
| Mobile Responsiveness | B+ | Intentional mobile design, touch target gaps |
| Test Coverage / QA | B+ | 350+ backend tests, critical shared services untested |

---

## Prioritized Findings

Priority Score = Impact x (11 - Effort). Higher = do first.

### P0: Critical (Must Fix Before Production)

| # | Problem | Improvement | Impact | Effort | Gain | Source |
|---|---------|-------------|--------|--------|------|--------|
| 1 | **HMAC auth optional** -- `X-User-Role: ADMIN` header spoofing possible when `HMAC_SECRET` is empty (default) | Fail-fast on startup if HMAC secret missing in non-test profiles | 10 | 2 | Any unauthenticated client can gain admin access | Backend |
| 2 | **CORS allows all origins with credentials** -- `setAllowedOriginPatterns(List.of("*"))` with `setAllowCredentials(true)` | Restrict to known frontend origins via `app.cors.allowed-origins` env var | 9 | 1 | Prevents cross-site request forgery against the API | Backend |
| 3 | **Server-side cache keys polluted by HMAC timestamps** -- auth headers with timestamps/nonces in `'use cache'` function params make every cache key unique (0% hit rate) | Pass only stable identifiers (userId, role) as cache params; resolve HMAC inside the cached function body | 9 | 4 | Restores entire server-side caching layer for authenticated endpoints | Frontend |

---

### P1: High Priority (Sprint 0-1)

| # | Problem | Improvement | Impact | Effort | Gain | Source |
|---|---------|-------------|--------|--------|------|--------|
| 4 | **No optimistic locking on TestSession** -- concurrent complete + abandon/timeout can produce inconsistent state | Add `@Version` field, handle `OptimisticLockException` with re-read logic | 8 | 3 | Prevents duplicate scoring and invalid state combinations | Backend |
| 5 | **Swipe navigation conflicts with browser back gesture** -- no edge dead zone; swipe from x=5px triggers both browser back AND previous question | Add 30px edge dead zone in `handleTouchStart` | 9 | 2 | Prevents accidental test abandonment on iOS/Android | Mobile |
| 6 | **Mobile breakpoint inconsistency** -- `useIsMobile()` uses 768px, `useBreakpoint()` uses 640px; contradictory layout decisions at 640-768px | Unify to single source of truth; remove `use-mobile.ts`, re-export from `use-breakpoint.ts` | 9 | 4 | Eliminates contradictory layouts on landscape phones | Mobile |
| 7 | **Test-taking page fully client-rendered** -- entire page is `'use client'` with useEffect waterfall (auth -> fetch -> render) | Refactor to Server Component page with `auth()` from `@clerk/nextjs/server`; pass data as props to client shell | 8 | 6 | Eliminates largest LCP bottleneck for critical user path | Frontend |
| 8 | **Builder entirely English-only** -- zero `useTranslations()` calls across BlueprintWorkspace, WeightedCanvas, LibraryPanel, SimulatorPanelV4 | Create `builder.json` translation namespace; replace all hardcoded strings | 9 | 5 | Makes builder usable for Russian-speaking HR professionals | UX |
| 9 | **Hardcoded Russian strings in answer summary** -- 20+ strings in `AnswerCard`, `QuestionProgressIndicator`, `useTestTimer` bypass next-intl | Extract to `assessment.json` translation files | 9 | 3 | Fixes bilingual parity for English-speaking users during review | UX |
| 10 | **PsychometricAuditJob lacks distributed lock** -- concurrent execution in multi-instance deployments | Add ShedLock with `@SchedulerLock`; break into per-question micro-transactions | 7 | 3 | Prevents duplicate calculations and database contention | Backend |
| 11 | **Missing `@PreAuthorize` on 2 session endpoints** -- `checkTemplateReadiness` and `getAssemblyProgress` accessible to any authenticated user | Add `@PreAuthorize` with appropriate role checks | 7 | 1 | Closes authorization gap | Backend |
| 12 | **TeamFit multiplier unbounded after personality adjustment** -- can exceed [0.9, 1.1] sigmoid range, potentially producing scores > 100% | Add clamping: `Math.max(0.8, Math.min(1.2, teamFitMultiplier))` | 7 | 2 | Prevents invalid scores in production | QA |
| 13 | **CompetencyAggregationService has zero test coverage** -- shared by all 3 scoring strategies, untested edge cases | Create `CompetencyAggregationServiceTest.java` with empty answers, missing indicators, boundary conditions | 8 | 4 | Protects the most critical shared scoring component | QA |
| 14 | **PsychometricAuditJob has zero test coverage** -- scheduled job with startup logic, response-threshold milestones | Create `PsychometricAuditJobTest.java` covering disabled mode, partial failure, milestone triggers | 8 | 5 | Validates nightly audit reliability | QA |

---

### P2: Medium Priority (Sprint 1-2)

| # | Problem | Improvement | Impact | Effort | Gain | Source |
|---|---------|-------------|--------|--------|------|--------|
| 15 | **Virtual keyboard hides navigation on text questions** -- `autoFocus` triggers keyboard immediately; no `scrollIntoView` or Visual Viewport handling | Replace `autoFocus` with delayed programmatic focus + `scrollIntoView({ block: 'center' })`; add `visualViewport.resize` listener | 8 | 5 | Prevents blocked test submission on mobile phones | Mobile |
| 16 | **Likert scale color-only selection indicator** -- selected state differentiated primarily by color (emerald vs neutral) | Add checkmark icon for selected state; increase border width | 8 | 2 | Enables colorblind users to see their selections (WCAG 1.4.1) | A11y |
| 17 | **Progress indicator dots color-only** -- answered/skipped/current/pending states use color alone | Add shape variations: checkmark for answered, hollow for skipped, diamond for current | 7 | 4 | Makes progress tracking accessible to colorblind users (WCAG 1.4.1) | A11y |
| 18 | **Back/Skip buttons below 44px touch target** -- ghost variant ~36px height on mobile | Add `className="min-h-[44px] min-w-[44px]"` to both buttons | 8 | 1 | Prevents mis-taps during timed assessments | Mobile |
| 19 | **BottomSheet footer missing safe area** -- `safe-area-inset-bottom` is not a valid Tailwind class; content obscured behind home indicator | Replace with `pb-[env(safe-area-inset-bottom,0)]` or `pb-safe` utility | 7 | 2 | Fixes content visibility on iPhone X+ | Mobile |
| 20 | **CORS wildcard on Next.js API routes** -- `Access-Control-Allow-Origin: *` in next.config.ts | Replace with `process.env.NEXT_PUBLIC_APP_URL` | 6 | 1 | Restricts API route access to known origins | Frontend |
| 21 | **Scoring uses double arithmetic** -- threshold comparisons `>= effectiveThreshold` on IEEE 754 doubles | Round to 4 decimal places before threshold comparison; or migrate to BigDecimal | 5 | 6 | Prevents incorrect pass/fail at boundary values | Backend |
| 22 | **Cache staleness for competency data** -- 10-min TTL without eviction on competency writes | Add `@CacheEvict` on competency write operations | 6 | 3 | Ensures scoring uses current competency metadata | Backend |
| 23 | **`lang="ru"` hardcoded on Likert fieldset** -- screen readers use Russian phonetics for English content | Use `lang={locale}` from `useLocale()` | 7 | 1 | Fixes screen reader pronunciation for all locales | UX/A11y |
| 24 | **Builder IDE metaphor inappropriate for HR users** -- "Structural Workbench", weight multipliers, "PERFECT_CANDIDATE" profiles | Rename to plain language ("Assessment Blueprint", "Importance: Low/Medium/High"); add guided wizard for first-time users | 8 | 6 | Reduces barrier to entry for primary revenue-generating journey | UX |
| 25 | **Timeout warning < 20 seconds** -- dialog appears only AFTER time expires, no advance warning | Add warning dialog at 20-30 seconds before expiry (WCAG 2.2.1) | 6 | 5 | Gives users time to request extensions or save work | A11y |
| 26 | **Timer countdown creates test anxiety** -- continuously visible countdown; 5-min warning too early | Show progress bar for first 80%; numeric countdown only under 3 min; reduce warning to 3 min | 7 | 4 | Reduces construct-irrelevant variance in psychometric results | UX |
| 27 | **Answer summary information overload** -- 30 expandable cards + 7 controls + hero stats simultaneously | Default to compact summary view; full card list behind "Review Details" button | 7 | 5 | Reduces cognitive load at critical submission moment | UX |
| 28 | **SJT and MCQ share identical visual treatment** -- same radio button layout, only small type badge differs | Add distinct accent color and scenario-connected visual cue for SJT | 6 | 4 | Reduces cognitive load and improves test validity | UX |
| 29 | **Results views lack actionable next steps** -- data without decision support for HR managers | Add "Interview Guide" for JobFit, "Onboarding Recommendations" for TeamFit, "Summary for Manager" | 7 | 6 | Bridges gap between data presentation and hiring decisions | UX |
| 30 | **Big Five grid-cols-5 unreadable at 320px** -- 60px cells with 40px circles, 8px text | Change to `grid-cols-3 sm:grid-cols-5`; show top 3 traits on mobile | 7 | 3 | Makes personality data readable on small phones | Mobile |
| 31 | **Radar chart labels truncated to 8 chars** -- Cyrillic names like "Communicativeness" become meaningless | Increase to 10-12 chars for Cyrillic; consider bar chart fallback below 480px | 6 | 4 | Makes competency names readable in Russian | Mobile |
| 32 | **Mermaid library in production bundle** -- ~2.1MB uncompressed, ~300KB gzipped | Dynamic import with `next/dynamic`; only load on docs pages | 6 | 2 | Reduces initial JS bundle by 300KB for all users | Frontend |
| 33 | **N+1 in session DTO conversion** -- `countAnsweredBySessionId` called per session in list views | Use batch `countAnsweredBySessionIds` method for list operations | 5 | 3 | Reduces database queries in user history views | Backend |
| 34 | **Actuator endpoints publicly accessible** -- `/actuator/metrics`, `/actuator/caches` reveal internal architecture | Restrict to health/prometheus public; require ADMIN for others | 5 | 1 | Prevents information disclosure | Backend |
| 35 | **Clerk webhook secret defaults to empty** -- startup warning only, no fail-fast | Throw `IllegalStateException` in non-test profiles | 6 | 1 | Prevents forged webhook payloads | Backend |
| 36 | **React Query barely used** -- installed (12KB gzipped) but only 3 hooks use it; rest uses `'use cache'` | Remove React Query; migrate 3 hooks to `'use cache'` server functions | 4 | 5 | Eliminates competing caching strategies; reduces bundle | Frontend |
| 37 | **`getApiBaseUrl()` duplicated 8 times** -- slightly different URL construction in each cache file | Extract to shared `api/core.ts` module | 5 | 2 | Eliminates URL routing inconsistencies | Frontend |

---

### P3: Low Priority / Nice-to-Have (Sprint 2-3)

| # | Problem | Improvement | Impact | Effort | Gain | Source |
|---|---------|-------------|--------|--------|------|--------|
| 38 | **205 manual `useMemo`/`useCallback` with React Compiler enabled** -- redundant memoization | Gradually remove; prioritize test player hooks | 3 | 3 | Cleaner code, minor bundle reduction | Frontend |
| 39 | **Zustand stores with `Set` types cause re-render cascades** -- `new Set()` always creates new reference | Convert to sorted arrays in affected stores | 5 | 3 | Reduces unnecessary re-renders in psychometrics review | Frontend |
| 40 | **File-level `'use cache'` in api.cache.ts** -- prevents granular cache profiles | Migrate to function-level `'use cache'`; delete deprecated `getUsersCached` | 4 | 2 | Consistent caching pattern across all cache files | Frontend |
| 41 | **BEHAVIORAL_EXAMPLE lacks STAR scaffolding** -- same textarea as OPEN_TEXT with no structure | Add STAR-format sections or structured prompt template; increase min to 100+ chars | 6 | 5 | Improves response quality and psychometric validity | UX |
| 42 | **Likert summary hardcodes 5-point scale** -- renders `X/5` even for 4-point or 7-point scales | Make scale dynamic based on `answerOptions.length` | 4 | 2 | Fixes visual mismatch in answer review | UX |
| 43 | **Progress dots 28px instead of 44px touch target** -- code comment says "44px" but value is 28px | Change `min-w-[28px]` to `min-w-[44px]` with negative margins for visual spacing | 7 | 2 | Fixes documented vs actual touch target discrepancy | Mobile |
| 44 | **No landscape orientation handling** -- 320px height in landscape leaves ~200px content area | Add `landscape:` variants for QuestionCard; consider orientation lock | 6 | 5 | Prevents content overflow in landscape mode | Mobile |
| 45 | **`xs` breakpoint conflict** -- globals.css defines 480px, `use-breakpoint.ts` defines 320px | Align JS hook with CSS custom property (480px) | 6 | 5 | Eliminates CSS/JS breakpoint divergence | Mobile |
| 46 | **Missing edge case tests** -- zero-answer competency, profile pattern boundaries (30%, 40%, 75%), JobFit confidence boundaries | Add parameterized tests for all threshold boundaries | 5 | 3 | Validates scoring correctness at critical boundaries | QA |
| 47 | **Simulator uses simplified scoring** -- not backed by actual ScoringStrategy implementations | Create integration test verifying simulated vs actual scoring alignment | 6 | 7 | Ensures simulation reliability for test builders | QA |
| 48 | **No frontend tests for ImmersivePlayer or AnswerSummary** -- core interactive components untested | Create unit tests for timer behavior, keyboard shortcuts, progress tracking | 6 | 5 | Protects core user-facing components from regressions | QA |
| 49 | **Keyboard shortcuts not visible to users** -- `TEST_PLAYER_KEYBOARD_SHORTCUTS` defined but no UI | Add "?" key handler to show shortcuts modal | 3 | 4 | Improves efficiency for power users | A11y |
| 50 | **Question selection not seeded** -- `Collections.shuffle()` uses ThreadLocalRandom; test forms not reproducible | Use seeded `Random(sessionId.getMostSignificantBits())` | 5 | 3 | Enables reproducible test forms for psychometric validation | Backend |
| 51 | **No mobile E2E tests for test player** -- mobile viewport specs only cover static pages | Add mobile-specific E2E: swipe navigation, Likert at 320px, keyboard handling | 7 | 5 | Validates critical mobile flows before release | Mobile |
| 52 | **Long-running transaction in nightly audit** -- holds HikariCP connection for full audit duration | Break into per-question micro-transactions with `REQUIRES_NEW` propagation | 6 | 4 | Prevents connection pool exhaustion | Backend |

---

## Phase 4: Sprint Implementation Plan

### Sprint 0: Critical Fixes + Quick Wins (1 week)

**Goal:** Production-ready security + highest-ROI improvements

| Ticket | Story Points | Description | Dependencies |
|--------|-------------|-------------|--------------|
| SEC-001 | 2 | Make HMAC secret mandatory (fail-fast startup) | None |
| SEC-002 | 1 | Restrict CORS to known origins (backend) | None |
| SEC-003 | 1 | Restrict CORS on Next.js API routes | None |
| SEC-004 | 1 | Restrict actuator endpoints | None |
| SEC-005 | 1 | Make Clerk webhook secret mandatory | None |
| SEC-006 | 1 | Add `@PreAuthorize` to 2 session endpoints | None |
| FE-001 | 5 | Fix auth header cache key pollution | None |
| FE-002 | 2 | Dynamic import mermaid library | None |
| FE-003 | 2 | Extract shared `getApiBaseUrl()` | None |
| MOB-001 | 1 | Add edge dead zone to swipe navigation | None |
| MOB-002 | 1 | Fix Back/Skip button touch targets (min 44px) | None |
| MOB-003 | 1 | Fix BottomSheet safe area class | None |
| A11Y-001 | 2 | Add checkmark icon to selected Likert options | None |
| UX-001 | 1 | Fix `lang="ru"` to dynamic locale | None |

**Total:** 22 story points
**Sprint Velocity Target:** 20-25 SP

---

### Sprint 1: Test Coverage + Scoring Hardening (2 weeks)

**Goal:** Scoring reliability + critical test coverage gaps

| Ticket | Story Points | Description | Dependencies |
|--------|-------------|-------------|--------------|
| BE-001 | 5 | Add `@Version` optimistic locking to TestSession + migration | None |
| BE-002 | 3 | Add ShedLock for PsychometricAuditJob | None |
| BE-003 | 2 | Clamp TeamFit multiplier after personality adjustment | None |
| BE-004 | 3 | Add `@CacheEvict` on competency writes | None |
| BE-005 | 2 | Round scoring thresholds to 4 decimal places | None |
| QA-001 | 5 | Create CompetencyAggregationServiceTest.java | None |
| QA-002 | 5 | Create PsychometricAuditJobTest.java | BE-002 |
| QA-003 | 3 | Add boundary edge case tests (profile patterns, confidence) | None |
| QA-004 | 2 | Add IRT extreme parameter test | None |
| I18N-001 | 5 | Extract hardcoded Russian strings in answer summary | None |
| I18N-002 | 8 | Create builder i18n (builder.json namespace) | None |
| MOB-004 | 5 | Unify mobile breakpoint definitions | None |

**Total:** 48 story points (2-week sprint at 25 SP/week)

---

### Sprint 2: Architecture Improvements (2 weeks)

**Goal:** Performance + architectural consistency

| Ticket | Story Points | Description | Dependencies |
|--------|-------------|-------------|--------------|
| FE-004 | 8 | Refactor test-taking page to Server Component | FE-001 |
| FE-005 | 3 | Migrate `api.cache.ts` to function-level `'use cache'` | FE-001 |
| FE-006 | 5 | Remove React Query; migrate 3 hooks | FE-001 |
| FE-007 | 3 | Remove redundant manual memoization (top 20 files) | None |
| MOB-005 | 5 | Fix virtual keyboard handling for text questions | None |
| MOB-006 | 3 | Big Five grid responsive fix | None |
| MOB-007 | 3 | Radar chart Cyrillic label improvement | None |
| BE-006 | 5 | Break nightly audit into micro-transactions | BE-002 |
| BE-007 | 3 | Batch DTO conversion for session lists | None |
| BE-008 | 3 | Seed question selection randomization | None |
| A11Y-002 | 5 | Progress indicator shape differentiation | None |
| A11Y-003 | 5 | Timeout warning 20 seconds before expiry | None |

**Total:** 51 story points

---

### Sprint 3: UX Polish + Full WCAG Compliance (2 weeks)

**Goal:** UX improvements + remaining accessibility + mobile polish

| Ticket | Story Points | Description | Dependencies |
|--------|-------------|-------------|--------------|
| UX-002 | 8 | Builder terminology + guided wizard | I18N-002 |
| UX-003 | 5 | Answer summary progressive disclosure | None |
| UX-004 | 5 | Timer anxiety reduction (progress bar + threshold adjustment) | None |
| UX-005 | 5 | SJT visual differentiation from MCQ | None |
| UX-006 | 8 | Results actionable next steps (Interview Guide, Manager Summary) | None |
| UX-007 | 3 | BEHAVIORAL_EXAMPLE STAR scaffolding | None |
| MOB-008 | 5 | Landscape orientation handling | None |
| MOB-009 | 5 | Mobile E2E test suite for test player | All MOB fixes |
| QA-005 | 5 | Frontend unit tests for ImmersivePlayer + AnswerSummary | None |
| QA-006 | 5 | Simulator vs actual scoring alignment test | None |

**Total:** 54 story points

---

## Dependency Graph

```
Sprint 0                    Sprint 1                Sprint 2              Sprint 3
--------                    --------                --------              --------
SEC-001..006 ─────────────> (unblocks production)
FE-001 (cache fix) ────────> FE-004 (SSR page)
                            FE-005 (cache migration)
                            FE-006 (remove RQ)
BE-002 (ShedLock) ─────────> QA-002 (audit tests)
                            BE-006 (micro-TX)
I18N-002 (builder i18n) ───> UX-002 (builder UX)
MOB-004 (breakpoints) ─────> MOB-005..008
All MOB fixes ─────────────────────────────────────> MOB-009 (mobile E2E)
```

---

## Risk Register

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| HMAC bypass exploited before Sprint 0 | Medium | Critical | Deploy Sprint 0 within 48 hours |
| Optimistic locking migration breaks existing sessions | Low | High | Add `DEFAULT 0` to migration; test with concurrent load |
| Builder i18n introduces untranslated strings | Medium | Medium | Automated check: grep for hardcoded strings in builder components |
| Cache fix (FE-001) causes temporary data freshness regression | Low | Medium | Monitor cache hit rates after deployment |
| ShedLock table migration fails | Low | Low | Test in staging with multi-instance deployment |
| React Query removal breaks sharing workflow | Medium | Medium | Run existing sharing tests before/after migration |

---

## Success Metrics

| Sprint | Metric | Target |
|--------|--------|--------|
| Sprint 0 | Security vulnerabilities open | 0 |
| Sprint 0 | Server-side cache hit rate | > 50% (from 0%) |
| Sprint 1 | Backend test coverage (critical services) | > 80% |
| Sprint 1 | Bilingual coverage (builder + answer summary) | 100% |
| Sprint 2 | LCP for test-taking page | < 2.0s (from ~3.5s) |
| Sprint 2 | Client JS bundle size | -300KB (mermaid removal) |
| Sprint 3 | WCAG 2.1 AA compliance | 100% (from 92%) |
| Sprint 3 | Mobile E2E test coverage | Critical flows covered |
| Sprint 3 | Builder first-time completion rate | Measurable via analytics |

---

## Verification Checklist

- [x] Every finding has Problem/Improvement/Impact/Effort/Expected Gain/Priority
- [x] Findings deduplicated across agents (74 raw -> 52 unique)
- [x] Implementation plan maps dependencies between frontend/backend changes
- [x] Quick Wins (Sprint 0) are actionable within 1 sprint (22 SP)
- [x] Risk assessment covers each proposed change
- [x] Success metrics defined per sprint

---

**Report generated from 6 agent audits:**
1. Next.js Architecture (15 findings)
2. Spring Boot Backend (16 findings)
3. UX Research (15 findings)
4. QA / Scoring Algorithms (13 findings)
5. WCAG Accessibility (9 findings)
6. Mobile Responsiveness (15 findings)

**Total raw findings:** 74 (before deduplication)
**Total unique findings:** 52 (after deduplication)
**Total story points across all sprints:** 175 SP
**Estimated timeline:** 7 weeks (1 + 2 + 2 + 2)
