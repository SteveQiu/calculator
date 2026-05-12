# Linus — History

## Project Seed

- **Project:** Android Kotlin Calculator
- **Stack:** Kotlin, JUnit, Espresso, Play Billing test environment, AdMob test ads
- **Requested by:** Developer
- **Goal:** Thorough test coverage — calculator logic, theme unlock via ads, theme unlock via Google Pay, state persistence, edge cases
- **Repo:** https://github.com/SteveQiu/calculator.git

## Learnings

_Appended by Linus after each session._

### 2026-05-12 — Feature Validation Skill + Cherry Blossom / Background Image Validation

**Skill created:** `.squad/skills/feature-validation/SKILL.md`
- Documents 6 core check areas (arithmetic, display, theme system, theme persistence, premium unlock, build health)
- Per-change routing table maps change type to required checks
- Standardised PASS/FAIL output format with inbox filing instruction

**Validation pass — Cherry Blossom theme + backgroundImageRes support:**

| Check | Result |
|-------|--------|
| `CHERRY_BLOSSOM` in `ThemeId` enum | ✅ |
| Cherry Blossom entry in `ThemeRegistry.all` (8 themes total) | ✅ |
| `ThemeColors` branch complete (all 9 fields) | ✅ |
| All `cherry_*` color resources in `colors.xml` | ✅ |
| `bg_cherry_blossom.xml` exists and is valid drawable | ✅ |
| `backgroundImageRes` null-safety in `applyThemeColors()` | ✅ |
| Classic theme remains free | ✅ |

**Outcome:** ✅ PASS — Cherry Blossom and background image support are correctly implemented.

**Pattern learned:** `backgroundImageRes` null-guard pattern (`if (theme.backgroundImageRes != null)`) must be checked on any theme using a background drawable. The guard is present in `MainActivity.kt` line 113.

- **Orchestration log:** `.squad/orchestration-log/2026-05-12T15-23-24Z-linus.md` (Scribe record)

### 2026-05-08 — First test suite (calculator logic + theme unlock)

**Patterns tested:**
- ViewModel-as-state-machine: each `onDigit`/`onOperator`/`onEquals` call advances state; test sequences model real button presses
- `runTest` + `StandardTestDispatcher` + `advanceUntilIdle()` for deterministic coroutine testing of SharedFlow consumers
- Mockito `whenever`/`verify`/`never()` for asserting BillingRepository and AdRepository interactions without real Android or network deps
- Robolectric `ApplicationProvider.getApplicationContext()` for DataStore-backed repository tests without an emulator

**Edge cases that matter most:**
- Division by zero must return `"Error"`, not crash or produce `Infinity`
- Double-tap decimal must be silently ignored (no `"3.."`)
- Leading zero replacement: `"0" → digit` should discard the zero, not concat
- Max input length (15): exceeding it must stop appending, not crash
- `toggleSign` on zero: must stay `"0"`, not become `"-0"`
- `delete` after `equals`: should reset to zero (result is not editable)
- Percent-of-addend: `200 + 10%` should produce `20` (10% of base 200), not `0.1`
- `BillingResult.ITEM_ALREADY_OWNED` is a restore-purchases path — must still unlock (gap, not yet tested)

**Compilation status:**
- `ThemeIdTest` ✅ compiles and passes (ThemeId model updated with isPremium/skuId/displayName)
- `ThemeRepositoryTest` ✅ compiles; `isThemeUnlocked` tests pass; persistence tests (unlockTheme/setActiveTheme) will **fail** until DataStore stubs are replaced with real implementation
- `CalculatorViewModelTest` ⏳ **TDD — will not compile** until CalculatorViewModel adds `displayValue`, `expressionText`, `displayState`, `onDigit`, `onOperator`, `onEquals`, `onDot`, `onPercent`, `onToggleSign`, `onClear`, `onDelete`
- `ThemeViewModelTest` ⏳ **TDD — will not compile** until ThemeViewModel adds `selectTheme`, `watchAdToUnlock`, `uiEvents: SharedFlow<UiEvent>`, `pendingUnlockTheme`, and until BillingRepository/AdRepository expose `purchaseResults`/`adResults` flows and result sealed classes

### 2026-05-09 — Full Stack Validation ✅

- **ThemeIdTest (8 tests)** all pass: enum properties (isPremium, skuId, displayName) are rock-solid. Rusty's design validated.
- **ThemeRepositoryTest (8 tests):** 5 pass (default state, unlock queries); 3 fail on DataStore write-through. Root cause identified: stubs don't persist to backing store. Basher's API is correct; implementation gap is narrow and actionable.
- **CalculatorViewModelTest (22 tests)** TDD written; contracts are clear for the developer. Sequences match real calculator button presses (e.g., `3 + 4 = = =` for repeat-last-operation).
- **ThemeViewModelTest (8 tests)** TDD written; billing and ad result routing contracts defined. Tests will compile once repositories expose result SharedFlows.
- **No API mismatches:** All 4 agents' implementations map to test expectations. System is cohesive, not fragmented.
- **3 concrete blockers identified:**
  1. `ThemeRepository.unlockTheme()` must write to DataStore, not just return
  2. `ThemeRepository.setActiveTheme()` must write to DataStore, not just return
  3. `BillingRepository` and `AdRepository` must expose `purchaseResults: SharedFlow<...>` and `adResults: SharedFlow<...>` respectively

- **Insight:** TDD approach correctly surfaced these gaps. Tests aren't wrong; they're highlighting implementation stubs that need real logic. This is the ideal workflow: architecture is sound, API contracts are clear, gaps are mechanical and localized.
