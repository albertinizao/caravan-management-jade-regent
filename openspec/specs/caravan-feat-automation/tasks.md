# Caravan Feat Automation — Tasks

**Status:** Draft  
**Spec:** `openspec/specs/caravan-feat-automation/spec.md`

## Objective

Implement deterministic feat automation in the backend, persist the state required to reconstruct it after reload, and expose the resulting automation state in the API and frontend without duplicating business rules in the UI.

## Execution Strategy

Deliver in this order:

1. domain and application state model
2. automation engine and event/cooldown tracking
3. API exposure and persistence wiring
4. frontend surfacing
5. tests and verification

Each task is written so it can be executed independently once its prerequisites are complete.

---

## Task 1 — Extend feat automation state model

### Goal
Add the persisted and derived state required to represent real feat automation beyond descriptive metadata.

### Scope
- Add explicit state objects for:
  - activation state
  - cooldown boundary / last-used markers
  - once-per-day / once-per-week / once-per-month usage markers
  - trigger consumption markers
  - selection-specific payloads
- Keep compatibility with the current metadata fields:
  - `automationMode`
  - `automationStateInputs`
  - `automationExactAutomation`
  - `manualApplies`
  - `manualAppliesReason`
  - `selectionIndex`
  - `active`
  - `blockedReason`

### Deliverables
- Domain model additions
- Persistence entity or embedded state additions
- Migration/seed adjustments if needed
- Backward-compatible deserialization for existing saved data

### Done when
- Existing feat records still load correctly
- New automation state can be persisted and read back
- No frontend/API contract breaks for current feat endpoints

---

## Task 2 — Introduce a feat automation engine in the application layer

### Goal
Centralize feat effect resolution in backend use cases and helpers.

### Scope
- Add a deterministic automation service that can:
  - recompute passive effects
  - evaluate trigger conditions
  - enforce cooldowns and usage windows
  - apply selection-based stacking
  - respect manual applicability
- Keep rule evaluation out of the frontend
- Make the service reusable by statistics/day-cycle/assignment flows

### Deliverables
- Automation service or helper component
- Per-feat resolver strategy or rule registry
- Shared result model for active/blocked/available automation state

### Done when
- Feat effects are computed from backend state only
- The same rules are used across all relevant use cases
- The service is testable in isolation

---

## Task 3 — Split feat automation into rule registry entries

### Goal
Represent each supported feat explicitly in code so automation is auditable and testable.

### Scope
Implement rule entries for:
- passive modifiers
- selection-based stacking
- manual-contract feats
- triggered and time-gated feats
- weekly/monthly cooldown feats

### Deliverables
- Rule registry keyed by feat code
- Per-feat metadata describing:
  - category
  - state inputs
  - exact automation behavior
  - prerequisites or dependencies

### Done when
- Each supported feat in the spec has a code-level automation entry
- Unsupported feats remain visible but clearly marked out of automation scope

---

## Task 4 — Wire automation into caravan state changes

### Goal
Recompute feat outcomes whenever caravan state changes.

### Scope
Hook automation into the use cases that mutate or derive caravan state:
- caravan creation and update
- level changes
- traveler assignment changes
- wagon assignment changes
- cargo assignment changes
- beast assignment changes
- day-cycle preview and execution
- settlement state transitions

### Deliverables
- Recalculation hooks in relevant use cases
- Cached or derived statistics refresh as needed
- Idempotent application of triggered effects

### Done when
- A state change updates feat results consistently
- Triggered effects are not double-applied on repeated reads

---

## Task 5 — Persist cooldowns and event markers

### Goal
Make time-gated and triggered effects reload-safe.

### Scope
- Persist last-used timestamps or cycle keys
- Persist one-time trigger markers
- Persist current week/month/cooldown keys where needed
- Persist any per-feat usage counters

### Deliverables
- Repository support for automation markers
- Serialization contract for cycle keys and event ids
- Reload-safe resolution logic

### Done when
- Restarting the app does not reset feat limits incorrectly
- Previously used triggers remain consumed
- Cooldowns survive reload

---

## Task 6 — Expose automation state in feat API models

### Goal
Let the UI inspect automation status without reimplementing the rules.

### Scope
- Extend owned feat and catalog item API models with automation state fields as needed
- Add explicit fields for:
  - current automation category
  - active/blocked state
  - blocking reason
  - remaining uses or cooldown state
  - selection summaries
  - manual applicability explanation

### Deliverables
- Updated backend response models
- Mappers updated to populate automation state
- Backward-compatible JSON shape where possible

### Done when
- Feat endpoints serialize all required automation data
- Existing consumers continue to work

---

## Task 7 — Surface automation state in the frontend feat screen

### Goal
Show automation state clearly without duplicating backend logic.

### Scope
- Display automation category / mode
- Display active vs blocked state
- Display blocking reason
- Display manual applicability controls and reason
- Display selection counts and remaining limit
- Display cooldown/usage state if present

### Deliverables
- Feats view updates
- Type updates in `frontend/src/types/feat.ts`
- Service updates if API payloads expand

### Done when
- Users can inspect why a feat is active or blocked
- Manual toggle and reason are visible and editable where applicable

---

## Task 8 — Implement automation for passive feat categories

### Goal
Make passive feat bonuses derive from state automatically.

### Scope
Cover feats that should update immediately from caravan state, including but not limited to:
- stat-based modifiers
- wagon bonuses
- cargo capacity modifiers
- travel speed bonuses
- role/productivity modifiers
- discontent or morale modifiers

### Deliverables
- Backend rule implementations for passive feats
- Statistics recalculation updates
- Tests for each passive effect category

### Done when
- Passive effects are reflected in caravan summaries without manual intervention

---

## Task 9 — Implement automation for triggered and action-driven feats

### Goal
Resolve explicit actions and event-triggered feats exactly once.

### Scope
- Day-cycle actions
- Settlement actions
- Weekly rerolls
- Triggered bonuses on encounter or state transition
- One-time defensive effects

### Deliverables
- Action command handlers or day-cycle hooks
- Usage tracking per event window
- Guardrails against duplicate application

### Done when
- Repeating the same event does not double-apply an effect
- Action-driven feats respect their usage windows

---

## Task 10 — Implement repeatable feat stacking rules

### Goal
Respect repeatable selection limits and per-selection payloads.

### Scope
- Enforce repeat limits
- Track selection index per owned feat
- Persist selection-specific choices
- Compute stacked totals deterministically

### Deliverables
- Validation in add/update feat use cases
- Selection-specific persistence support
- Stacking-aware summary calculations

### Done when
- Repeatable feats stack correctly and stop at their limit
- Selection-specific metadata survives reload

---

## Task 11 — Implement manual-contract feats explicitly

### Goal
Keep partially automated feats honest and visible.

### Scope
- Preserve `manualApplies` and `manualAppliesReason`
- Mark manual feats as not auto-applied when the toggle is off
- Apply stored effect text or derived modifier only when manual applicability is asserted

### Deliverables
- Manual-contract validation
- Frontend toggle and explanation handling
- API round-trip support

### Done when
- Manual-contract feats remain inspectable and predictable
- The backend does not infer hidden conditions

---

## Task 12 — Add deterministic backend tests for all supported automation behaviors

### Goal
Protect the automation rules with fast, deterministic tests.

### Scope
- Domain tests for metadata and selection rules
- Application tests for passive, triggered, and time-gated effects
- API tests for automation serialization and round-trips

### Deliverables
- New or expanded test classes
- Fixtures for key feat state combinations
- Regression coverage for cooldowns, stacking, and manual applicability

### Done when
- Each supported automation category has at least one failing-first test that would catch a regression

---

## Task 13 — Add frontend smoke coverage for feat automation UI

### Goal
Prevent accidental regressions in the exposed automation state.

### Scope
- Add basic UI tests or component smoke coverage once the testing stack is chosen
- Cover feat list, detail state, manual toggle, and blocked reason display

### Deliverables
- Frontend test setup if absent
- Smoke tests for feat automation UI

### Done when
- Frontend changes to feat automation can be verified automatically

---

## Task 14 — Document automation coverage and unsupported cases

### Goal
Make the implementation understandable for future contributors.

### Scope
- Mark supported vs manual vs unsupported feats
- Document where the rulebook is authoritative and where code is the source of truth
- Record any intentional non-goals or partial implementations

### Deliverables
- Updated OpenSpec notes
- README updates if needed
- Inline comments only where they add real value

### Done when
- A new contributor can tell which feats are automated, manual, or out of scope

---

## Recommended Implementation Order

1. Task 1
2. Task 2
3. Task 3
4. Task 4
5. Task 5
6. Task 6
7. Task 7
8. Task 8
9. Task 9
10. Task 10
11. Task 11
12. Task 12
13. Task 13
14. Task 14

## Verification Gate

Before closing the change:

- backend tests must pass,
- frontend typecheck must pass,
- feat API responses must remain backward-compatible where possible,
- automation state must survive reload.
