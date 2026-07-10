# Caravan Feat Automation

**Status:** Draft  
**Target:** GestionCaravana  
**Type:** Feature specification

## 1. Summary

The application must resolve caravan feat automation deterministically in the backend so that passive, triggered, time-gated, selection-based, and manually asserted feat effects are applied consistently across all UI screens and all day-cycle resolutions.

This specification also defines how shared-job productivity bonuses are represented for feats such as **Trabajo En Equipo**, including the exact persistence model needed to keep fractional productivity from being lost on low-output jobs.

This specification defines:

- the automation contract for feat metadata,
- the state model required to resolve automation,
- the UI contract for surfacing automation state,
- the supported feat-by-feat automation requirements,
- the testability and persistence requirements for deterministic execution.

The rulebook in `docs/Reglas_de_Caravana.md` remains the source of truth for the narrative text. This specification defines how the application interprets the rulebook into executable behavior.

## 2. Problem Statement

Feat rules in the rulebook are written in natural language and mix permanent modifiers, triggered effects, time-gated effects, selection counts, and conditional effects that depend on caravan state.

Without a formal automation contract:

- the same feat can be interpreted differently across use cases,
- derived bonuses can drift between backend and frontend,
- repeatable feats can stack incorrectly,
- cooldowns and once-per-cycle limits can be lost after reloads,
- manual applicability can be treated as a hidden implementation detail,
- the player cannot inspect why a feat is active, blocked, or pending.

## 3. Goals

1. Automate every feat that the application can resolve deterministically.
2. Keep rulebook text as the canonical human-readable reference.
3. Make automation originate in the backend/application layer, not in the frontend.
4. Persist all state needed to reconstruct feat effects after reload.
5. Expose availability, blocking, and active-effect reasons in the API and UI.
6. Support manual applicability for feats that depend on external judgment or a sub-system not modeled here.
7. Keep automation testable with deterministic fixtures.
8. Preserve repeatable selection counts and selection-specific metadata.
9. Resolve shared-job productivity bonuses deterministically, including carry-over for fractional productivity when a job produces less than a full unit per resolution.

## 4. Non-Goals

This specification does **not** define:

- a combat engine,
- a spell engine,
- tactical positioning,
- world simulation outside the caravan model,
- user identity or multiplayer sync,
- free-form manual overrides of rule logic,
- alternate rulebooks or house rules.

This specification also does **not** require the frontend to recompute feat logic locally. The frontend may display derived state, but the backend remains authoritative.

## 5. Automation Model

### 5.1 Automation categories

Every feat belongs to one of these categories:

- **Passive** — the effect is applied automatically when the feat is active and its conditions are met.
- **Triggered** — the effect is applied when a specific caravan event occurs.
- **Action-driven** — the effect is applied only after an explicit user action.
- **Time-gated** — the effect is limited by a daily, weekly, monthly, or multi-month cooldown.
- **Selection-based** — each feat selection adds a new stack or a new selected option.
- **Manual** — the effect is not fully auto-evaluated, so the user asserts whether the current state satisfies the rule.

### 5.2 Source of truth

- The backend must calculate feat availability, blocking reasons, active status, and derived bonuses.
- The frontend must not invent or duplicate feat rule logic.
- The frontend may render the stored automation fields and derived summaries.

### 5.3 Required event model

The application must expose or reuse these deterministic events:

- caravan creation,
- caravan selection,
- caravan level change,
- caravan stat change,
- feat selection,
- feat update,
- feat removal,
- caravan state change,
- traveler assignment change,
- wagon assignment change,
- cargo assignment change,
- beast assignment change,
- day-cycle preview,
- day-cycle execution,
- settlement arrival,
- settlement departure,
- settlement day advancement,
- manual feat applicability toggle,
- user-triggered feat action,
- weekly cycle,
- monthly cycle,
- emergency or shortage state change.

### 5.4 Derived-state rules

1. Passive effects must be recomputed whenever caravan state changes.
2. Triggered effects must be applied exactly once per matching event instance.
3. Time-gated effects must persist their last execution or cooldown boundary.
4. Selection-based feats must persist their own stack count and selected options.
5. Manual feats must preserve the user-asserted applicability flag.
6. Blocking reasons must be explicit and stable so the UI can explain why a feat is unavailable or inactive.
7. Shared-job productivity bonuses must be derived from the current assignment graph and persisted when the resolved output can produce fractional carry-over.

### 5.5 Existing metadata contract

The current data model already exposes these fields for feat types and owned feats:

- `automationMode`
- `automationStateInputs`
- `automationExactAutomation`
- `manualApplies`
- `manualAppliesReason`
- `selectionIndex`
- `active`
- `blockedReason`
- `sharedJobProductivityState`

This specification requires those fields to remain compatible and to be populated meaningfully.

### 5.6 Manual applicability contract

For feats that cannot be fully resolved by the backend, the owned feat record must preserve:

- `manualApplies: boolean | null`
- `manualAppliesReason: string | null`

Rules:

1. When `manualApplies` is `null`, the feat is not under manual-contract evaluation.
2. When `manualApplies` is `false`, the feat contributes no effect.
3. When `manualApplies` is `true`, the backend applies the feat effect using the stored manual context.
4. The backend must never infer manual applicability from hidden heuristics.
5. The UI must show a clear toggle and an explanation.

### 5.7 Selection contract

For repeatable or stackable feats:

1. Each owned selection must have a stable `selectionIndex`.
2. The same feat type can appear multiple times only when the catalog says it is repeatable.
3. The application must enforce `selectionLimit`.
4. Selection-specific metadata must be stored per owned feat instance.
5. The UI must show the number of owned stacks and the remaining allowed selections.

## 6. Supported Feats and Exact Automation Requirements

### 6.1 Artesanos Colaborativos

Category: Manual / Out of scope for direct automation

Automation requirements:

1. The feat must be represented in the catalog and on the UI.
2. The application must not auto-grant the downstream feat produced by this rule.
3. The effect may be surfaced as descriptive text only.

### 6.2 Autonomía Extrema

Category: Passive / Time-gated

Automation requirements:

1. Batidores and farmers produce 50% more supplies.
2. The production modifier must be applied automatically to every supply-production resolution involving those roles.
3. The application must track a 3-month cooldown for the emergency-immunity effect.
4. Once every 3 months, the caravan may ignore shortage effects for 3 consecutive days.
5. The immunity must activate only when the caravan enters a valid emergency state or when a validated user-triggered action is executed.
6. The cooldown starts when the immunity is activated, not when the feat is selected.
7. The effect must not stack with itself.

### 6.3 Ayuno Intermitente

Category: Action-driven / Daily / Manual choice

Automation requirements:

1. The player must be able to enable or disable intermittent fasting for a given day-cycle resolution.
2. The application must validate the Determination check using the rulebook formula.
3. If the check succeeds, supply consumption for the affected day is reduced according to the rule text.
4. If the caravan is already in a shortage condition described by the rulebook, the failure branch must be used.
5. The consecutive fasting counter must persist across days and reset on any non-fasting day.
6. The effect must be resolved at most once per day.

### 6.4 Caravana Afortunada

Category: Passive

Automation requirements:

1. Double the caravan’s eligible reroll allowance associated with the feat.
2. The derived reroll count must update when feat ownership changes or when the underlying base allowance changes.
3. If the base allowance is zero, the derived allowance remains zero.
4. The doubling must not create synthetic rerolls beyond the derived value.

### 6.5 Caravana Armada

Category: Passive

Automation requirements:

1. If the caravan’s native attack bonus is lower than the cleric-equivalent value for the current level, the effective attack bonus becomes the cleric-equivalent value.
2. If the native bonus is higher, it must be preserved.
3. The effect must be derived and recomputed whenever level or attack changes.

### 6.6 Caravana Mejorada

Category: Passive / repeatable bonus

Automation requirements:

1. Each active instance grants 2 free main-stat points.
2. The bonus is added to the caravan's unassigned main-stat points.
3. The bonus stacks once per active instance.
4. The caravan statistics must be recalculated after every feat change.

### 6.7 Caravana De Renombre

Category: Triggered / Time-gated

Automation requirements:

1. When the caravan arrives at a settlement, the player may mark the fame state as active for the next settlement day.
2. If active, the next in-settlement day gets the rulebook-defined social bonus and contract discount.
3. The effect expires after one settlement day or when the caravan leaves the settlement, whichever happens first.
4. The activation must be explicit; it cannot persist automatically to another settlement.

### 6.8 Caravana Familiar

Category: Manual

Automation requirements:

1. The application must store whether the user asserts that the family/romance condition is currently satisfied.
2. When `manualApplies=true`, the application must expose the rulebook-defined morale benefit.
3. When `manualApplies=false`, the feat contributes no effect.
4. The backend must preserve the manual reason text.

### 6.9 Carroñeros

Category: Action-driven / Weekly

Automation requirements:

1. The player may execute scavenging once per week.
2. The application must resolve the Security check at the rulebook CD.
3. On success, the caravan gains repair-material cargo as specified by the rule.
4. Extra rewards based on margin of success must be computed deterministically.
5. Reuse must be blocked until the weekly boundary changes.

### 6.10 Carros Adicionales

Category: Passive / Selection-based

Automation requirements:

1. Each selection increases the wagon limit by the caravan level.
2. The effect stacks per selection.
3. The derived limit must be used in wagon-capacity validation and in summary displays.

### 6.11 Carros Protegidos

Category: Passive

Automation requirements:

1. Every owned wagon gains the rulebook-defined hardness bonus.
2. Every owned wagon gains the rulebook-defined hit-point bonus.
3. Future wagons must receive the same derived bonus when added.
4. The bonus must be derived from feat ownership, not copied into catalog data.

### 6.12 Celebración

Category: Action-driven / Triggered

Automation requirements:

1. The player may start a celebration action from the day-cycle context.
2. The player must choose the celebration intensity allowed by the rulebook.
3. The action doubles traveler supply consumption for the celebration day.
4. The action reduces discontent by the chosen intensity.
5. The action grants the temporary morale bonus described by the rulebook.
6. The effect must be persisted so it is not applied twice after reload.

### 6.13 Cuidado De Animales

Category: Action-driven / Rest-day

Automation requirements:

1. The action is available only on a rest day.
2. The action consumes the required supplies.
3. The action resolves the appropriate Security check.
4. On success, the caravan heals the rulebook-defined amount.
5. The healing allocation among draft animals must be represented deterministically.

### 6.14 Consumo Eficiente

Category: Passive / Selection-based

Automation requirements:

1. Caravan consumption decreases by the rulebook amount.
2. The reduction may stack per selection if allowed by the catalog.
3. The derived consumption cannot fall below zero.
4. The adjusted consumption must be visible in the caravan summary.

### 6.15 Defensa Rápida

Category: Triggered / Combat setup

Automation requirements:

1. On a valid surprise encounter, the application must expose the pre-combat movement window described by the rulebook.
2. If the encounter is foreseeable, the application must expose the preparation state described by the rulebook.
3. The effect is represented as combat-setup state, not a full tactical engine.

### 6.16 Dominio Del Terreno

Category: Passive / Selection-based

Automation requirements:

1. Each selection chooses one terrain type.
2. While traveling in that terrain, the caravan gains the rulebook-defined Security bonus.
3. The selected terrain bonuses stack independently.
4. The effect must be recomputed when the caravan enters a new region.

### 6.17 Dureza De La Caravana

Category: Passive / Selection-based

Automation requirements:

1. Each selection targets one wagon.
2. The selected wagon gains the rulebook-defined hit-point bonus.
3. If the wagon is destroyed, the feat benefit for that wagon is suspended until repair or replacement.
4. The chosen wagon must be stored per selection.

### 6.18 Entrenamiento Cruzado

Category: Passive / Triggered validation

Automation requirements:

1. Up to the rulebook-defined number of travelers may exercise a second job.
2. The application must validate eligibility automatically.
3. The effect must appear in role assignment screens and derived productivity summaries.
4. Each traveler must be counted once for the cap.

### 6.19 Esclavistas

Category: Passive

Automation requirements:

1. The caravan must not gain discontent from the suppressed slave-related sources described by the rulebook.
2. Determination checks that would increase discontent for those suppressed sources must be ignored.
3. The suppression must apply automatically while the feat is active.

### 6.20 Levantarse De La Nada

Category: Triggered / Defensive

Automation requirements:

1. The first time a wagon would be destroyed, the destruction must be prevented.
2. The wagon must remain at 0 hit points instead.
3. The wagon must receive the one-time rescue marker.
4. The wagon must become repairable according to the rulebook limit.
5. The effect may only trigger once per destruction sequence.

### 6.21 Mercado Ambulante

Category: Passive / Selection-based

Automation requirements:

1. The caravan receives the rulebook-defined commercial bonus.
2. The effect stacks per selection up to the catalog limit.
3. The summary must expose stack count and resulting bonus.

### 6.22 Maestría Mercantil

Category: Passive / Selection-based

Automation requirements:

1. The trade bonus must be computed from caravan level exactly as the rulebook defines.
2. The bonus stacks per selection up to the selection limit.
3. Any dependency on `Mercado Ambulante` must be validated before saving.

### 6.23 Oferta Gancho

Category: Action-driven / Daily / Selection-based

Automation requirements:

1. Once per day, the player may execute the hook-offer action in a settlement.
2. The action requires the prerequisite merchant-sale condition described by the rulebook.
3. The premium bonus must be `+10%` or `+20%` according to the selection-dependent rule.
4. The action must be blocked if the daily usage limit was already consumed.

### 6.24 Organización Impecable

Category: Passive / State-gated

Automation requirements:

1. Caravan cargo capacity increases by the rulebook-defined percentage.
2. If the discontent-free day counter reaches the rulebook threshold, wagon consumption decreases by the rulebook amount.
3. The counter must reset when discontent becomes nonzero.
4. The effect may stack up to the catalog limit.

### 6.25 Planificación De Emergencia

Category: Passive / Weekly reroll

Automation requirements:

1. While the caravan is in the declared crisis state, one reroll of a failed Security or Determination check is allowed per week.
2. The allowance resets on the weekly boundary.
3. The effect must not be available outside crisis state.

### 6.26 Reparaciones Eficientes

Category: Action-driven / Selection-based

Automation requirements:

1. Special Security repair checks gain the rulebook-defined bonus.
2. The effect stacks per selection up to the catalog limit.
3. The bonus must not affect unrelated Security checks.

### 6.27 Rituales De Fortuna

Category: Triggered

Automation requirements:

1. When a failed check is avoided using `Caravana Afortunada`, the caravan gains the rulebook-defined Moral bonus.
2. The bonus must be applied exactly once per avoided failure event.
3. No bonus is granted when the reroll does not change the outcome.

### 6.28 Rutas Conocidas

Category: Passive / Triggered

Automation requirements:

1. While traveling through a known region, the caravan gains the rulebook-defined Security bonus.
2. When an unwanted encounter would occur, the application must resolve or offer the reroll described by the rulebook.
3. The effect must be derived from route/exploration state.

### 6.29 Tripulación Valerosa

Category: Passive / Selection-based

Automation requirements:

1. The caravan gains the rulebook-defined Determination bonus against fear and fleeing effects.
2. The effect stacks per selection up to the repeatable limit.
3. The bonus must be visible in the caravan summary and in relevant checks.

### 6.30 Trabajo En Equipo

Category: Passive / Derived aggregation

Automation requirements:

1. For each shared job bucket, count the active travelers assigned to that bucket and apply a multiplier of `1.25` for two travelers or `1.50` for three travelers.
2. The bonus applies to the bucket’s base productivity and to any same-bucket numeric bonus that is emitted as part of that job resolution.
3. The effect must be recomputed from the current assignment graph only.
4. The bucket is capped at three travelers and must not count travelers outside the relevant job bucket.
5. If the resolved output is fractional, preserve the remainder in persistent shared-job state instead of truncating it, so low-output jobs still realize the full percentage bonus over time.
6. Batidores only count when they are in hunt mode; exploration mode contributes no production and therefore no teamwork bonus.

### 6.31 Venta De Esclavos

Category: Settlement action / Daily

Automation requirements:

1. The application must track the daily number of allowed actions per traveler and settlement.
2. Buying and selling modifiers must match the rulebook text.
3. The per-day and per-settlement limits must be enforced before committing the transaction.
4. The action log must persist enough information to avoid double application after reload.

### 6.32 Veloz

Category: Passive / Selection-based

Automation requirements:

1. Each selection increases travel speed by the rulebook-defined amount.
2. The speed bonus stacks up to the catalog limit.
3. The updated speed must flow into travel previews and summary statistics.

### 6.33 Viajeros Expertos

Category: Passive / Selection-based

Automation requirements:

1. The maximum bonus that traveler jobs can provide increases by the rulebook-defined amount per selection.
2. The adjusted cap must be used everywhere job-derived calculations are shown.
3. The effect stacks up to the catalog limit.

## 7. Conditional Feat Contract

The following feats are resolved through the `manualApplies` contract because the current application cannot fully infer their conditions without external judgement or an additional subsystem:

- `Artesanos Colaborativos`
- `Caravana Familiar`
- `Bendición Del Camino` if supported in the catalog
- `Caravana Bendecida` if supported in the catalog
- `Caravana Santificada` if supported in the catalog
- `Carros En Círculo` if supported in the catalog

Rules:

1. The UI must expose the manual toggle on the feat detail screen.
2. If the toggle is off, the feat contributes no automated effect.
3. If the toggle is on, the backend applies the stored effect text or derived modifier.
4. The application must preserve the manual reason text.
5. The UI must label the effect as user-asserted, not auto-inferred.

## 8. Data Model Requirements

### 8.1 feat type metadata

Feat catalog rows must store:

- feat code,
- name,
- description,
- prerequisites,
- benefit text,
- special text,
- notes,
- repeatable flag,
- selection limit,
- minimum caravan level,
- automation mode,
- automation state inputs,
- exact automation text.

### 8.2 owned feat data

Each owned feat must persist:

- caravan id,
- feat type code,
- acquisition source,
- acquisition level or cause,
- selection index,
- active flag,
- blocked reason,
- manual applies flag,
- manual applies reason,
- timestamps.
- derived shared-job productivity state when the feat affects a job bucket with fractional carry-over or other persisted progress.

### 8.3 cooldown and event tracking

The application must persist enough state to reconstruct:

- once-per-day usage,
- once-per-week usage,
- once-per-month usage,
- multi-month cooldowns,
- one-time triggered effects,
- the last event that consumed the effect.
- fractional shared-job productivity carry-over and the last resolved traveler set for each shared job bucket when a feat depends on persisted productivity progress.

## 9. API Requirements

The existing feat endpoints must continue to expose:

- catalog availability,
- owned feat list,
- feat detail,
- feat add/update operations,
- blocked reasons,
- automation metadata.

If automation state becomes richer, the API may extend the view model, but it must preserve backward compatibility for existing fields.

## 10. Feat-by-Feat Implementation Contract

The following matrix is the authoritative implementation contract for every feat in `docs/Reglas_de_Caravana.md`.

| Feat | Mode | State / Inputs | Exact automation |
|---|---|---|---|
| Artesanos Colaborativos | manual | `manualApplies:boolean`, `manualAppliesReason:string\|null` | If true, expose the caravan rule marker `Artesanía colaborativa` to all travelers while the feat remains active. The application must store only the boolean assertion and the reason; it must not attempt to infer whether the crafting workflow can actually benefit from the marker. |
| Autonomía Extrema | passive and time-gated | `cooldownEndsAt`, `immunityEndsAt`, `immunityUsesLog[]` | Batidores and farmers produce `50%` more supplies. Recompute the supply-production modifier whenever job productivity is resolved. The emergency immunity may be activated only when the caravan is in shortage or the user explicitly opens the feat action from a validated emergency context. On activation, set `immunityEndsAt = currentDay + 3 days` and `cooldownEndsAt = currentDay + 3 months`. While the immunity window is open, shortage penalties are suppressed but the production bonus remains independent. The effect may not overlap with a second activation; activation must be blocked until the cooldown expires. |
| Ayuno Intermitente | action-driven daily | `fastingStreak:int`, `lastFastingDay`, `currentFastingActionId`, `lastFastingResolvedDay` | The feat is resolved once per caravan day. When the user starts fasting for the day, compute `CD = 15 + currentDiscontent + fastingStreak`. On success, halve traveler supply consumption for that day and increase discontent by `1`. If supplies are insufficient and the previous day already had no supplies, increase discontent by `3` and do not apply the halving. The streak increases only on consecutive fasting days and resets on any non-fasting day. |
| Bendición Del Camino | manual and action-driven | `manualApplies:boolean`, `selectedDeityCode`, `selectedSpellName`, `spellLevel:int`, `lastBlessingDay` | When active, store the chosen level-1 spell and deity. The application does not resolve the spell effect itself; it records the selected spell, validates that the spell is level 1, applies the feat marker, and exposes the effect text `once per day, after prayer, all travelers receive the selected level-1 spell with caster level 25`. The once-per-day limit must be tracked by caravan day, not by real time. |
| Caravana Afortunada | passive | `weeklyDivinerRerolls:int`, `baseWeeklyDivinerRerolls:int` | Double the diviner reroll allowance automatically from the base reroll pool. The derived value is `baseWeeklyDivinerRerolls * 2`. Recalculate on feat add/remove or when the caravan level changes if the base allowance is level-dependent. If the base pool is zero, the result remains zero and no synthetic rerolls are created. |
| Caravana Armada | passive | `baseAttackBonus`, `clericEquivalentAttackBonus`, `effectiveAttackBonus` | Set effective caravan attack bonus to the cleric-equivalent value for caravan level if and only if the caravan’s own bonus is lower. Never reduce a higher native bonus. Recompute whenever caravan level or underlying attack bonus changes. The derived value must feed every downstream combat or stat summary that reads caravan attack bonus. |
| Caravana Mejorada | selection-based passive | `selectedMainStats[]`, one record per selection | Each selection chooses exactly two main stats. Increment each chosen stat by `1`, cap each at `+10`, persist the chosen pair on the owned feat instance, and recalculate caravan stats after every selection. If the same stat is chosen twice in one selection, it must still count as two increments and each increment remains capped individually. |
| Caravana Bendecida | manual | `manualApplies:boolean`, `selectedDeityCode`, `lastBlessingDay` | Record the chosen deity. When active, expose the effect: each day, the clerics of that deity can cast one level 1 or 2 spell without consuming resources. The application records the effect and the deity; spell resolution itself remains outside scope. The manual toggle must be the only gate that decides whether the daily free-cast marker is applied. |
| Caravana Santificada | manual | `manualApplies:boolean`, `selectedDeityCode`, `selectedAltars[]` | Record the chosen deity. When active, apply `+3` effective caster level to divine spells cast by that deity’s clerics within `200` feet of the caravan. The app does not resolve the spell, only the modifier state. The feat may be selected multiple times for different deities, and each selection must preserve its own deity binding and manual toggle. |
| Caravana De Renombre | triggered and timed | `fameAnnouncementPending:boolean`, `fameExpiresAt`, `currentSettlementId` | When the caravan arrives at a settlement, the user may mark fame as announced for the next day. If active, grant `+2` circumstance bonus to social checks and `10%` discount on contracted services for the next in-settlement day only. The effect must expire when the settlement day advances or the caravan leaves the settlement, whichever happens first. |
| Caravana Familiar | manual plus passive calculation | `manualApplies:boolean`, `manualAppliesReason:string\|null` | The user marks whether the relationship conditions currently hold. When `manualApplies=true`, compute morale as `+1` per qualifying family or romance link, with one link between the same pair counting at most once. If the involved travelers are assigned to a family wagon, double the contribution of that link. The backend must never infer the family state from the relationship graph; it only applies the bonus after the user asserts the condition. |
| Carroñeros | weekly action | `weeklyScavengeUsedAt`, `currentWeekKey`, `lastScavengeCheckResult` | Once per week, allow a Security check at `CD 15`. On success, add `1` repair-material cargo unit and add `1` more for each full `5` points above the CD. The action must be blocked until `currentWeekKey` changes. The result record must persist the resolved total so the UI can show success, margin, and reward. |
| Carros Adicionales | selection-based passive | `selectionCount:int` | Increase the wagon limit by the caravan level per selection. The new maximum must be reflected in wagon-capacity validation and in the caravan summary. Excess wagons are not destroyed; they are only flagged by the existing over-capacity rules. |
| Carros En Círculo | manual plus combat-setup | `manualApplies:boolean`, `manualAppliesReason:string\|null` | The user toggles the formation as applicable during combat setup. When active, apply `+4` AC, total cover for covered travelers, difficult terrain between wagons, and a max of one medium creature between two wagons. This spec only stores the user-asserted applicability and the effect marker; tactical positioning remains external. |
| Carros Protegidos | passive | none beyond wagon stats | Add `+2` hardness and `+10` max hit points to every owned wagon. Apply the bonus to future wagons as soon as they enter the caravan. The modifier must be derived from feat ownership and selection count, not duplicated onto wagon records. |
| Celebración | action-driven day event | `celebrationIntensity:1..5`, `celebrationEndsAt`, `celebrationAppliedDay` | The user selects celebration intensity from `1` to `5`. Double traveler supply consumption for the celebration day, reduce discontent by the selected intensity, and add a morale bonus of the same value for `5` days. If resting, double the resulting bonuses. The application must persist the celebration day key so a reload cannot apply the same celebration twice. |
| Cuidado De Animales | rest-day action | `restCareUsedAt`, `chosenHealingTargets[]`, `lastAnimalCareCheckResult` | On a rest day, allow one Security special check that costs `1` supply unit. On success, heal `15 × caravanLevel` hit points and distribute the healing among draft animals according to the chosen targets. The action is invalid on travel days; it must be blocked before rolling if the caravan is not resting. |
| Consumo Eficiente | passive and selection-based | `selectionCount:int`, `consumptionReduction:int` | Reduce caravan consumption by `2` while preserving the floor defined by wagon consumption. Each selection stacks. Recalculate daily consumption after every day-cycle change and after feat add/remove. The derived reduction must be exposed in the caravan summary as a final adjusted consumption value, not as a hidden modifier. |
| Defensa Rápida | combat trigger | `surpriseEncounterGranted:boolean`, `prePositionWindow:boolean`, `combatSetupLockId` | When a surprise encounter begins, allow up to two movement actions before combat. If an encounter is foreseeable with at least `30` seconds of warning, expose a pre-positioning state. No other combat automation is required here. The implementation must record a setup window so the extra movement can only be consumed once per encounter start. |
| Dominio Del Terreno | selection-based passive | `selectedTerrainCodes[]` | Each selection chooses one terrain type. While traveling through that terrain, add `+2` Security. Multiple selections on different terrains stack independently. The terrain bonus must be recomputed from the current route/region and must disappear immediately when the caravan leaves the selected terrain. |
| Dureza De La Caravana | selection-based passive | `selectedWagonIds[]` | Each selection targets one wagon. That wagon gains `+20` hit points. If the wagon is destroyed, suspend the benefit for that wagon until it is repaired or replaced by a wagon of the same type. The hit point bonus must be attached to the wagon selection record, not hard-coded to a wagon type. |
| Entrenamiento Cruzado | passive validation | `eligibleSecondJobTravelerIds[]`, `validatedSecondJobMappings[]` | Allow up to `4` travelers to hold a second job if they satisfy the job requirements. Validate eligibility automatically and include the second job in productivity calculations. A traveler counted for the cap must be one traveler-id, even if that traveler can satisfy multiple candidate second jobs. |
| Esclavistas | passive | `suppressedDiscontentSources[]` | Prevent discontent increases caused by slave wounds or slave death. Ignore Determination checks that would raise discontent because slaves exist. The suppression must apply automatically while the feat is active and the UI must show that the discontent source is suppressed by the feat. |
| Levantarse De La Nada | defensive trigger | `savedFromDestructionWagonId`, `used:boolean`, `savedWagonStateSnapshot` | The first time a wagon would be destroyed, set its hit points to `0` instead, mark it as `raised-from-nothing`, and allow repair up to `50%` of max hit points. Trigger only once per destruction sequence. The saved state marker must be cleared only when the wagon leaves the destroyed state by repair or replacement. |
| Mercado Ambulante | selection-based passive | `selectionCount:int`, `commercialCheckBonus:int` | Apply the commercial Determination bonus defined by the feat. Stack per selection up to the repeatable limit. The application must expose the current stack count and resulting bonus in the caravan summary. |
| Maestría Mercantil | selection-based passive | `selectionCount:int`, `requiredMercadoAmbulanteStacks:int` | Compute the trade bonus as one quarter of the caravan level. Enforce any dependency on `Mercado Ambulante` stacks if the catalog states it. Stack per selection up to the limit. If a prerequisite stack threshold exists, selection must be rejected before save when the threshold is not met. |
| Oferta Gancho | daily settlement action | `dailyOfferUsedAt`, `selectedTreasureItemId`, `premiumTier`, `merchantSoldGoodsToday:int` | Once per day in a settlement, allow selling one treasure item after a merchant sold at least one unit of merchandise. Apply `+10%` for the first tier and `+20%` for the second tier. The second tier additionally requires two units of merchandise sold by the merchant. The daily usage marker must be settlement-scoped, not global, so the action can be used again in a different settlement on the same campaign day only if the rules permit it. |
| Organización Impecable | passive and state-gated | `discontentFreeDays:int`, `selectionCount:int`, `cargoCapacityBonusPercent:int` | Increase cargo capacity by `10%`. When `discontentFreeDays >= 10`, reduce wagon consumption by `1` per wagon. Stack up to two selections. The day counter resets whenever discontent becomes nonzero and must be incremented during the end-of-day resolution after discontent is finalized. |
| Planificación De Emergencia | passive / weekly reroll | `weeklyEmergencyRerollUsedAt`, `crisisState:boolean`, `weeklyRerollWindowKey` | While the caravan is in a prolonged crisis, allow one reroll of a failed Security or Determination check per week. Reset the allowance at the next weekly cycle. The effect must not be available outside crisis state. The crisis flag is a precondition supplied by the campaign state; the feat only adds the reroll entitlement. |
| Reparaciones Eficientes | action-driven passive bonus | `selectionCount:int`, `repairBonusPerCheck:int` | Apply `+2` to each special Security repair check. Stack up to three selections. Only repair checks receive the bonus. The application must not affect non-repair Security checks, even if they use the same skill value. |
| Rituales De Fortuna | triggered | `lastGrantedFromLuckyCaravanEventId`, `moralGrantedByRerollEventIds[]` | When a failure is avoided by `Caravana Afortunada`, grant `+1` Moral exactly once per avoided failure event. The bonus must only be granted when the reroll changes the outcome from failure to success. If the reroll does not change the result, no moral is granted. |
| Rutas Conocidas | passive and trigger | `exploredRegionBonusActive:boolean`, `lastRouteRerollEncounterId`, `knownRegionCodes[]` | While traveling through previously explored regions, grant `+2` Security. If an unwanted encounter would occur, allow or resolve one reroll. The effect must be derived from the route/exploration state and the reroll must be consumed exactly once per encounter opportunity. |
| Tripulación Valerosa | selection-based passive | `selectionCount:int` | Grant `+2` Determination against fear and flee effects per selection, up to the repeatable limit. The bonus must be visible in the Determination summary and in checks against fear sources. |
| Trabajo En Equipo | passive derived aggregation | `sharedJobAssignments[]`, `sharedJobBonusByJobCode`, `sharedJobProductivityStateByJobCode`, `fractionalCarryoverByJobCode` | For each shared job bucket, count the active travelers assigned to that bucket and apply a multiplier of `1.25` for two travelers or `1.50` for three travelers. The bonus applies to the bucket’s base productivity and to any same-bucket numeric bonus that is emitted as part of that job resolution. Recompute from the current assignment graph only. The bucket is capped at three travelers. If the resolved output is fractional, preserve the remainder in persistent shared-job state instead of truncating it, so low-output jobs still realize the full percentage bonus over time. Batidores only count when they are in hunt mode; exploration mode contributes no production and therefore no teamwork bonus. |
| Venta De Esclavos | settlement daily action | `actionsUsedToday:int`, `settlementId`, `slaveMarketActionLog[]`, `actionsUsedByTravelerId[]` | In a settlement, each traveler with the `esclavista` job may contribute up to `5` buy/sell actions per day. Buying costs `75%` of price; selling in the same settlement pays `200%` of price. Track action count per day and settlement. The settlement scope must reset when the caravan changes settlement, and the per-day cap must be enforced before the transaction is committed. |
| Veloz | selection-based passive | `selectionCount:int`, `travelSpeedBonus:int` | Increase travel speed by `4` miles/day per selection, stacked up to the repeatable limit. The updated speed must be reflected in travel previews and route ETA calculations. |
| Viajeros Expertos | selection-based passive | `selectionCount:int`, `maxTravelerJobBonusIncrease:int` | Increase the maximum bonus that traveler jobs can provide by `+1` per selection, stacked up to the repeatable limit. The adjusted cap must be used in all job-derived calculations, including preview and final resolution. |

## 11. UI Requirements

The frontend feat screen must:

1. display automation mode and exact automation text when available,
2. display whether a feat is active or blocked,
3. display why it is blocked,
4. display manual applicability and its reason,
5. display owned count and selection limit for repeatable feats,
6. expose any user-action controls needed to toggle manual application or trigger time-gated actions.
7. display shared-job productivity bonus, team size, and any carried fractional progress for **Trabajo En Equipo** in the relevant caravan and day-cycle summaries.

## 12. Persistence and Reload Requirements

1. Reloading the app must not lose automation state.
2. Recomputable bonuses must be recalculated from stored state.
3. Time-gated and triggered effects must resume from persisted counters or event records.
4. No automation effect may rely on ephemeral frontend memory.
5. Shared-job productivity carry-over must survive save/load cycles and continue from the persisted fractional remainder.

## 13. Testing Requirements

### 12.1 Domain tests

The domain layer must have tests for:

- repeatable selection limits,
- manual applicability normalization,
- feat type metadata validation,
- derived blocking reasons where applicable.

### 12.2 Application tests

The application layer must test:

- passive bonus recomputation,
- time-gated usage limits,
- triggered effect idempotency,
- selection-specific persistence,
- manual applicability behavior,
- availability and blocked reasons.

### 12.3 API tests

HTTP tests must verify:

- the feat catalog endpoint,
- feat add/update endpoints,
- blocked reasons for unavailable feats,
- manual fields survive round-trips,
- automation fields are serialized correctly.

### 12.4 Frontend checks

The frontend must typecheck and must have at least smoke coverage for the feat management flow once the automation UI is expanded.

## 14. Acceptance Criteria

1. Every supported feat in the rulebook that the app can resolve deterministically is represented in the feat catalog.
2. Every owned feat exposes active/inactive state, selection index, and blocking reason when applicable.
3. Manual-feat behavior survives save/load cycles.
4. Repeatable feats enforce their limit and stack correctly.
5. Time-gated feats cannot be reused before the cooldown expires.
6. Triggered effects are applied exactly once per matching event.
7. The frontend can display automation metadata without duplicating business logic.
8. Tests exist for the core automation behaviors and remain deterministic.
9. Shared-job productivity bonuses for **Trabajo En Equipo** are visible in both the caravan summary and the day-cycle resolution, including persisted carry-over where applicable.

## 15. Implementation Notes

- Keep automation logic in application services and domain helpers, not in Vue components.
- Prefer explicit state over implicit inference for cooldowns and triggered effects.
- Keep the rulebook text visible as descriptive content, but do not use it as the only runtime contract.
- If a feat cannot be modeled deterministically, keep it in the manual contract instead of approximating it.
