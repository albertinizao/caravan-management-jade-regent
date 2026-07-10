# Caravan Daily Supply Cycle

**Status:** Draft  
**Target:** GestionCaravana  
**Type:** Feature specification

## 1. Summary

The application must allow the active caravan to **advance exactly one day** and resolve food consumption, food generation, supply conversion, perishable decay, and cargo reassignment in one authoritative backend operation.

This slice focuses on the **food economy** of the caravan:

- caravan consumption,
- pre-food role activity that affects the daily economy,
- hunter-scout food generation,
- food stored inside `supplies` and `perishable supplies`,
- cook and portable-kitchen bonuses when converting supplies into food,
- leftover food conversion into perishable supplies,
- deterministic cargo redistribution back into wagons,
- and a modal day-pass simulation from the Caravan view.

## 2. Verified Rule Sources

This specification is based on:

- `C:\Users\Alberto\workspace\GestionCaravana\docs\Reglas_de_Caravana.md`
- the product rules provided by the user in this session

Verified rule excerpts reflected here:

- Batidores do not count toward caravan consumption.
- A batidor focused on hunting provides 2 provisions for that day.
- An agricultor generates 1 supply unit every 2 days.
- A boticario generates 5 po/day in alchemical products.
- An artesano works on crafting activity and can be surfaced in the daily summary.
- A cook turns one spent supply unit into 15 food instead of 10.
- A portable kitchen lets one cook double that improvement for one use, producing 20 food instead of 15 for that consumed supply.
- A sirviente increases the effectiveness of another role by 50%, and by an additional 50% if the servant also qualifies for that same role.
- Perishable supplies are worth 10 food by default and lose 1 food every 2 days in the source rules. For implementation, this spec models that decay as **0.5 food per day** so fractional food can be preserved explicitly, which matches the user’s requested behavior and stays equivalent over 2 days.

## 3. Problem Statement

The current product can model travelers, wagons, cargo, and derived caravan data, but it does not yet define a canonical, step-by-step rule engine for resolving the caravan’s daily food cycle.

Without this spec:

- consumption can be displayed but not spent,
- stored food may be split across wagons without a canonical resolution order,
- perishable food decay can be applied inconsistently,
- cook and kitchen bonuses can be miscounted,
- and the frontend would be forced to improvise rules that belong in the backend.

That is NOT acceptable. The day pass must be deterministic, auditable, and owned by the backend.

## 4. Goals

1. Advance one caravan day in a single atomic operation.
2. Calculate caravan food consumption correctly.
3. Exclude active batidores from caravan consumption.
4. Resolve agricultor, boticario, and artesano pre-food activity before the food calculation begins.
5. Resolve hunter food generation before spending stored supplies.
6. Resolve perishable food as fractional food.
7. Spend regular supplies only when generated + perishable food is insufficient.
8. Apply cook, servant, and portable-kitchen bonuses while converting supplies into food.
9. Convert leftover food back into perishable supply cargo.
10. Reassign resulting cargo back into wagons deterministically.
11. Produce warnings when food is insufficient or cargo cannot be fully reassigned.
12. Show the user a clear before/after summary and an ordered simulation of the full resolution.

## 5. Non-Goals

This specification does not define:

- full travel progression beyond one day pass,
- agriculturist production,
- servant amplification,
- feast or fasting decisions,
- discontent changes,
- weather systems,
- spoilage prevention from ice or refrigeration improvements,
- or non-food cargo processing.

Those can be added later, but this spec defines the canonical base food cycle first.

## 5. User Experience Requirements

### 5.1 Entry point

From the **Caravan** view, the UI must expose a button labeled:

- `Pasar el día`

When the user clicks that button, the application must open a modal window for the daily-cycle preview.

### 5.2 Modal content

Before the user confirms the day pass, the modal must show the caravan state **before** the daily calculation:

- total number of `supplies` units currently carried by the caravan
- total number of `perishable supplies` units currently carried by the caravan
- total food contained in all `perishable supplies` units, including decimals

Then the modal must show:

- whether caravan consumption is covered or not
- the resulting number of `supplies` units after the day pass
- the resulting number of `perishable supplies` units after the day pass
- the resulting total food stored in `perishable supplies` after the day pass

The modal is a **simulation only**.

Rules:

- opening the modal must not persist any caravan changes
- generating or refreshing the preview must not persist any caravan changes
- closing or cancelling the modal must not persist any caravan changes
- caravan state changes become definitive only after explicit user confirmation from the modal

### 5.3 Ordered simulation log

The modal must include an ordered simulation of how the backend resolved the process.

The simulation must show, in order:

1. pre-food role activity summary
2. each batidor contribution
3. each consumed supply conversion
4. the leftover food result, if any

### 5.4 Batidor simulation entry

For each batidor shown in the simulation, the modal must display:

- traveler name
- that the traveler acted as `batidor`
- food generated by that batidor

If the product later distinguishes hunt vs scout explicitly in the UI, the entry should also show the chosen mode.

### 5.5 Supply conversion simulation entry

For each `supplies` unit converted during the process, the modal must show:

- that one supply unit was converted
- whether a cook was applied
- whether a servant affected that cook
- whether the servant qualified as cook, if present
- whether a portable kitchen was applied
- the final amount of food obtained from that unit

### 5.6 Leftover food simulation entry

If food remains after covering caravan consumption, the modal must show:

- the final leftover food amount
- and, if converted, how that leftover food was turned into `perishable supplies`

## 6. Core Domain Definitions

### 6.1 Consumption

`Caravan consumption` is the food cost to feed the caravan for one day.

Base rule:

- consumption = total traveler consumption + total wagon consumption

Adjustment:

- every traveler assigned as `batidor` has **consumption 0** while assigned as batidor

This spec assumes the existing statistics engine already knows each wagon’s consumption contribution. The daily cycle must reuse the same canonical consumption source, not recalculate wagon rules differently.

### 6.2 Food-bearing cargo

Two cargo types participate in this cycle:

- `supplies`
- `perishable supplies`

Base value:

- 1 unit of `supplies` = 10 food
- 1 unit of `perishable supplies` = 10 food by default

Removal rule:

- whenever a `supplies` or `perishable supplies` unit reaches `food <= 0`, that cargo unit is deleted

### 6.3 Perishable decay model

Perishable supplies must track their current `foodAmount` as a decimal value.

Implementation rule for this slice:

- each day pass reduces every perishable-supply unit by `0.5 food`
- after the reduction, any unit with `foodAmount <= 0` is deleted

This is the explicit implementation chosen for the product because the source rule of “lose 1 food every 2 days” becomes auditable, fractional, and compatible with the requested temporary-inventory workflow.

### 6.4 Servant amplification

Servants (`sirvientes`) modify the effectiveness of other roles.

Verified base rule from the manual:

- if a traveler performs a role that grants a bonus, an assigned servant increases that role’s effectiveness by `+50%`
- if that servant also qualifies for the same role, effectiveness gains an additional `+50%`

For this feature, servant amplification must be applied with **role-specific operational formulas** defined in this specification.

## 7. Day Pass Algorithm

## 7.0 Preview/confirm execution model

This feature must be implemented as two distinct backend flows:

1. `preview`
2. `confirm`

### Preview

The preview flow must:

- read the latest persisted caravan state
- build an in-memory simulation snapshot
- execute the day-pass algorithm against that in-memory snapshot
- return the before/after summary and ordered simulation log required by the modal
- persist **nothing** as definitive caravan state

### Confirm

The confirm flow must:

- read the latest persisted caravan state again
- validate that the base state is still compatible with the preview being confirmed
- re-run the algorithm from persisted state
- persist the resulting definitive caravan state
- persist the final day-pass result record

The confirm flow must be transactional.

### Required anti-staleness rule

Because the modal preview is not definitive, the backend must protect against confirming stale simulations.

At confirm time, the backend must reject the operation if the caravan changed after the preview was generated.

Accepted implementation strategies include:

- a preview fingerprint/state hash
- a version field / optimistic locking strategy
- a deterministic state signature based on the relevant updated timestamps and counters

If the state changed, the backend must return a conflict response and require a new preview.

## 7.1 Preconditions

- there must be an active caravan
- the backend must load the full active state for:
  - travelers and their roles,
  - wagons and remaining cargo capacity,
  - cargo units assigned to wagons,
  - and applicable day-pass resources such as cooks and portable kitchens

If there is no active caravan, the operation must fail with a user-facing validation error.

## 7.1A Pre-food role activity

Before beginning the food calculation itself, the backend must resolve these role activities in order:

1. agricultors
2. boticarios
3. artesanos

### Agricultors

Each agricultor advances personal work progress by `0.5`.

If that agricultor has an assigned servant:

- `+0.5` more progress if the servant also qualifies as agricultor
- `+0.25` more progress if the servant does not qualify as agricultor

While the agricultor has `workProgress >= 1`:

1. subtract `1` from that agricultor’s work progress
2. create `1` unit of `supplies`
3. place that supply unit into the temporary inventory
4. repeat while `workProgress >= 1`

The daily summary must show how many `supplies` units were generated by agricultors.

### Boticarios

Each boticario generates `5 po` in alchemical products before the food calculation begins.

If that boticario has an assigned servant:

- `+5 po` if the servant also qualifies as boticario
- `+2.5 po` if the servant does not qualify as boticario

The backend must sum all generated alchemical-product value and include the total in the daily summary.

### Artesanos

Before the food calculation begins, the backend must list every traveler acting as `artesano`.

For each artesano, the summary/simulation must indicate:

- traveler name
- that the traveler is acting as `artesano`
- whether the artesano has an assigned servant
- if a servant exists, whether that servant also qualifies as `artesano`

This slice does not yet convert artesano work into a persisted crafting result. It only records the pre-food activity in the preview/result summary.

## 7.2 Step 1 — Calculate caravan consumption

The backend must calculate the caravan’s food consumption for the day.

Rules:

- count all travelers except those currently assigned as batidores
- add all wagon consumption
- the result may be integer or decimal only if future rules introduce fractions; for this slice it is expected to be an integer

The result must be included in the response breakdown as `requiredConsumption`.

## 7.3 Step 2 — Start generated food bucket

The backend must initialize:

- `generatedFood = 0`

This is the working food pool used during resolution.

## 7.4 Step 3 — Add hunter food

For each batidor assigned to hunting on that day:

- add 2 food to `generatedFood`

If that batidor has an assigned servant:

- add `+2 food` more if the servant also qualifies as batidor
- add `+1 food` more if the servant does not qualify as batidor

Batidores assigned to scouting:

- add 0 food
- still keep consumption 0 because they remain batidores

The result breakdown must show hunter contributions by traveler.

## 7.5 Step 4 — Add food from all perishable supplies

The backend must find **all** cargo units of type `perishable supplies` across **all** wagons and sum their entire current `foodAmount`, including decimals.

Rules:

- add the summed value to `generatedFood`
- remove those perishable units from wagon cargo before continuing
- place those units into the temporary inventory for later rebuild

At this point, perishable food is treated as already available food for today’s resolution.

## 7.6 Step 5 — If needed, extract all regular supplies into temporary inventory

If `generatedFood >= requiredConsumption`, skip to Step 7.

If `generatedFood < requiredConsumption`:

- remove **all** `supplies` cargo units from **all** wagons
- place them into a `temporary inventory`

The temporary inventory is an in-memory working collection used only during the day-pass transaction.

## 7.7 Step 6 — Consume supplies until food is enough or supplies run out

While both conditions are true:

- `generatedFood < requiredConsumption`
- temporary inventory still contains at least 1 unit of `supplies`

repeat:

1. consume exactly 1 unit of `supplies`
2. remove that supply unit from temporary inventory
3. convert it into food using the cook/kitchen rules from Section 8
4. add the produced food to `generatedFood`

Stop when:

- `generatedFood >= requiredConsumption`, or
- there are no `supplies` units left in temporary inventory

## 7.8 Step 7 — Pay caravan consumption

After the conversion loop finishes:

- `remainingFood = generatedFood - requiredConsumption`

Outcomes:

- if `remainingFood < 0`, the backend must mark the day as **insufficient food**
- if `remainingFood == 0`, the day resolves successfully with no leftover food
- if `remainingFood > 0`, the day resolves successfully and leftover food must be converted into perishable supplies

If food is insufficient:

- the response must include a warning/alert indicating the caravan could not cover its consumption
- no synthetic negative cargo may be created

## 7.9 Step 8 — Convert leftover food into perishable supplies

If `remainingFood > 0`, convert it into `perishable supplies` inside the temporary inventory.

Rules:

1. while `remainingFood >= 10`
   - create 1 `perishable supplies` unit with `foodAmount = 10`
   - add it to temporary inventory
   - subtract 10 from `remainingFood`
2. when `0 < remainingFood < 10`
   - create 1 `perishable supplies` unit with `foodAmount = remainingFood`
   - add it to temporary inventory
   - set `remainingFood = 0`

If `remainingFood == 0`, no extra perishable unit is created.

## 7.10 Step 9 — Rebuild cargo from temporary inventory

When rebuilding wagon cargo, process temporary inventory in this strict order:

1. all `supplies`
2. all `perishable supplies`

### Reassigning regular supplies

Assign supply units one by one to wagons using the wagon-selection rules in Section 9.

### Reassigning perishable supplies

Assign perishable units one by one, but before assigning each unit:

- subtract `0.5 food` from that unit

After the subtraction:

- if the unit reaches `foodAmount <= 0`, delete it and do not assign it
- otherwise assign it using the wagon-selection rules in Section 9

This second `0.5` reduction is part of the requested operational flow and represents the per-day degradation that occurs during the day pass before the perishable unit is stored again.

## 7.11 Step 10 — Finish the transaction

The operation must persist:

- updated cargo distribution,
- updated perishable `foodAmount` values,
- any removed empty cargo units,
- and the day-pass result record

The operation must be atomic: either the whole day pass is applied, or nothing is.

## 8. Supply Consumption Bonuses

## 8.1 Cooks

When consuming 1 `supplies` unit during Step 6:

- base result = 10 food
- a cook contributes a **bonus of +5 food** over that base result

For this feature, servant and portable-kitchen effects apply to the **cook bonus**, not to the whole 10 food base.

Limits:

- each cook may apply this benefit **once per day**
- only one cook may affect the same consumed supply unit

## 8.2 Portable kitchens

If a cook upgrades a supply conversion and at least 1 unused `portable kitchen` is available, the kitchen adds `+100%` to the cook bonus multiplier.

This is intentionally applied on top of the cook bonus model so that kitchen + servant combinations do not become explosively large.

Limits:

- each portable kitchen may be used **once per day**
- only one portable kitchen can affect a given consumed supply unit
- a portable kitchen does nothing without a cook

## 8.3 Cook servant amplification

If a cook has an assigned servant:

- add `+50%` to the cook bonus multiplier
- if the servant also qualifies as cook, add another `+50%`

All applicable bonus percentages must be summed **before** multiplying the cook bonus.

The operational formula is:

- `foodProduced = 10 + (5 * cookBonusMultiplier)`

Where:

- no extras => `cookBonusMultiplier = 1`
- servant only => `1.5`
- kitchen only => `2`
- servant + kitchen => `2.5`
- servant-qualified-as-cook => `2`
- servant-qualified-as-cook + kitchen => `3`

## 8.4 Cook activation priority

When choosing which cooks to consume first, the backend must activate cooks in this strict priority order:

1. cooks with servant who also qualifies as cook
2. cooks with servant who does not qualify as cook
3. cooks without servant

Within the same priority group, use a deterministic stable order.

## 8.5 Allocation strategy

During Step 6, the backend must maximize food output automatically.

For each consumed supply unit, the engine must pick the highest-priority still-unused cook, if any, and then compute food using the additive cook-bonus model.

If a cook is used:

1. determine whether that cook has a servant
2. determine whether the servant qualifies as cook
3. if at least 1 unused portable kitchen exists, assign one kitchen use
4. calculate the cook bonus multiplier
5. produce the resulting food
6. mark the cook as used
7. if a kitchen was used, mark that kitchen as used

If no cook is available, produce `10 food`.

This greedy strategy is correct because cook bonuses are single-use per day and higher-multiplier cooks must be consumed first.

## 8.6 Verified cook examples

The engine must satisfy these per-unit conversions:

| Has servant | Servant qualifies as cook | Has portable kitchen | Percentage applied to cook bonus | Food produced per supply unit |
|---|---|---|---:|---:|
| No | No | No | +0% (`*1`) | 15 |
| No | No | Yes | +100% (`*2`) | 20 |
| Yes | No | No | +50% (`*1.5`) | 17.5 |
| Yes | No | Yes | +150% (`*2.5`) | 22.5 |
| Yes | Yes | No | +100% (`*2`) | 20 |
| Yes | Yes | Yes | +200% (`*3`) | 25 |

## 8.7 Aggregate examples

The engine must satisfy these examples:

| Supplies consumed | Cooks | Portable kitchens | Food produced |
|---|---:|---:|---:|
| 3 | 0 | 0 | 30 |
| 3 | 1 | 0 | 35 |
| 3 | 3 | 0 | 45 |
| 3 | 0 | 1 | 30 |
| 3 | 1 | 1 | 40 |
| 3 | 1 | 2 | 40 |
| 3 | 2 | 2 | 45 |
| 3 | 3 | 2 | 50 |
| 3 | 3 | 3 | 60 |
| 3 | 4 | 4 | 60 |

These aggregate examples assume the previously defined activation priority and no servant-specific overrides beyond the product rules above.

## 9. Wagon Assignment Rules

When assigning a `supplies` or `perishable supplies` unit back to cargo:

1. find the **Supply Wagon** with the greatest available cargo capacity, as long as it has at least 1 free slot
2. if no Supply Wagon has space, find any other wagon with the greatest available cargo capacity, as long as it has at least 1 free slot
3. if no wagon has available capacity:
   - stop the assignment process immediately
   - report all remaining temporary-inventory items to the user
   - discard the remaining temporary inventory

Tie-breaking rule:

- if multiple wagons have the same greatest available capacity, use a stable deterministic order such as wagon creation order or wagon id ascending

The same tie-breaker must be used every time.

## 10. Data Model Expectations

## 10.0 Legacy model replacement rule

The previous day-pass implementation is considered unreliable legacy and must not be reused as the semantic base for the new flow.

Existing old artifacts may remain temporarily in the codebase, but they are to be treated as **replaceable legacy**, not as trusted design anchors.

The new implementation may delete them, replace them, or recreate them with new semantics.

At minimum, the following existing artifacts must be considered legacy and **must not be reused blindly**:

- `C:\Users\Alberto\workspace\GestionCaravana\src\main\java\com\gestioncaravana\application\model\CaravanDayPreviewView.java`
- `C:\Users\Alberto\workspace\GestionCaravana\src\main\java\com\gestioncaravana\application\model\CaravanDayResolutionView.java`
- `C:\Users\Alberto\workspace\GestionCaravana\src\main\java\com\gestioncaravana\domain\CaravanDayResolution.java`
- `C:\Users\Alberto\workspace\GestionCaravana\src\main\java\com\gestioncaravana\adapter\out\persistence\CaravanDayResolutionJpaEntity.java`
- `C:\Users\Alberto\workspace\GestionCaravana\src\main\java\com\gestioncaravana\adapter\out\persistence\CaravanDayResolutionRepositoryAdapter.java`
- `C:\Users\Alberto\workspace\GestionCaravana\src\main\java\com\gestioncaravana\application\port\out\CaravanDayResolutionRepositoryPort.java`

These files can be removed and recreated with new code if that is the cleanest path.

### Replacement guidance

The new implementation should prefer a clean set of types with explicit semantics, for example:

- `DayPassPreviewUseCase`
- `ConfirmDayPassUseCase`
- `DayPassSimulationEngine`
- `DayPassSimulationSnapshot`
- `DayPassSimulationResult`
- `DayPassSimulationLogEntry`
- `ConfirmedDayPassResult`

Exact naming may change, but the key rule is that the new flow must be designed from the required behavior, not constrained by the legacy classes above.

## 10.1 Perishable cargo

`PerishableSupplyCargoUnit`

Suggested fields:

- `cargoUnitId`
- `wagonId`
- `foodAmount`
- `createdOnDay`
- `updatedOnDay`

## 10.2 Day pass result

`CaravanDayPassResult`

Suggested fields:

- `caravanId`
- `resolvedDay`
- `requiredConsumption`
- `generatedSuppliesFromAgricultors`
- `generatedAlchemyValueFromBoticarios`
- `hunterFoodGenerated`
- `perishableFoodCollected`
- `foodProducedFromSupplies`
- `totalGeneratedFood`
- `remainingFoodAfterConsumption`
- `insufficientFood`
- `consumedSupplyUnits`
- `cookUses`
- `portableKitchenUses`
- `reassignedSupplies`
- `reassignedPerishableSupplies`
- `discardedTemporaryInventoryItems`
- `warnings`

## 10.3 Temporary inventory item

`TemporaryCargoItem`

Suggested fields:

- `sourceType` (`supplies` or `perishable supplies`)
- `foodAmount` optional for perishable
- `sourceWagonId` optional
- `sequence`

The temporary inventory is transactional and must not persist beyond the day-pass operation.

## 11. API Expectations

Suggested backend use cases:

- build the modal preview for the Caravan view
- preview daily food resolution
- advance one day and resolve the food cycle
- inspect the last food-cycle result

Suggested endpoints:

- `POST /api/caravans/{caravanId}/daily-cycle/food/preview`
- `POST /api/caravans/{caravanId}/daily-cycle/food/confirm`
- `GET /api/caravans/{caravanId}/daily-cycle/food/last-result`

Exact endpoint naming may change, but the backend must remain the canonical rules engine.

The preview response must include enough information for the modal to render:

- pre-calculation cargo counts
- pre-calculation total perishable food
- projected consumption coverage result
- post-calculation cargo counts
- post-calculation total perishable food
- agricultor-generated supply count
- boticario-generated alchemical value total
- artesano activity list
- ordered simulation entries
- preview fingerprint / state token or equivalent anti-staleness value

The confirm request must include the preview fingerprint/state token, or another equivalent concurrency mechanism required by the chosen backend design.

## 12. Acceptance Criteria

### AC-0 Modal preview

- From the Caravan view, the user can click `Pasar el día` and open a modal preview.
- The modal shows the caravan’s `supplies` and `perishable supplies` counts before the calculation.
- The modal shows the total food stored in perishable supplies before the calculation.
- Opening the modal or refreshing the preview does not persist definitive caravan changes.

### AC-1 Consumption

- The backend calculates caravan consumption for the active caravan.
- Travelers assigned as batidores contribute 0 traveler consumption while assigned as batidores.

### AC-1B Pre-food activities

- Agricultors advance work before the food calculation starts.
- Agricultor-created supply units are added to temporary inventory before food resolution proceeds.
- Boticarios generate daily alchemical value before the food calculation starts.
- Artesanos are listed in the daily summary with servant qualification status if applicable.

### AC-2 Perishable collection

- All perishable-supply cargo food is summed before regular supplies are converted.
- Decimal food values are preserved.

### AC-3 Supply conversion

- Regular supplies are only consumed if generated food plus perishable food is insufficient.
- Each consumed supply unit yields the correct food amount according to cook, servant, and kitchen availability.
- Cooks are activated in priority order: cook-qualified servant first, then servant-assisted cook, then cook without servant.

### AC-4 Insufficient food

- If food is still below caravan consumption after all available supplies are consumed, the result contains an alert/warning.

### AC-5 Leftover conversion

- Positive leftover food is converted into perishable-supply cargo units of 10 food each, plus a final remainder unit if needed.

### AC-6 Perishable decay

- Perishable supplies lose 0.5 food during the day-pass storage cycle.
- Any perishable or regular supply unit with food <= 0 is removed.

### AC-7 Cargo reassignment

- Cargo reassignment prefers Supply Wagons with the greatest free capacity.
- If none exist, reassignment falls back to the non-supply wagon with the greatest free capacity.
- If no wagon can accept more cargo, the remaining temporary inventory is reported and discarded.

### AC-8 Determinism

- Running the same state through the day-pass algorithm produces the same result every time.
- Tie-breaking between wagons is deterministic.

### AC-9 Simulation trace

- The modal shows an ordered simulation of the resolution.
- Each batidor entry shows name, batidor role, and generated food.
- Each converted supply entry shows whether a cook and/or portable kitchen was used and the resulting food gained.
- If leftover food exists, the modal shows the leftover amount.
- The summary shows how many supply units were generated by agricultors.
- The summary shows the total alchemical value generated by boticarios.
- Artesanos appear in the daily summary with servant qualification details.

### AC-10 Confirmation boundary

- No definitive caravan changes are persisted until the user confirms from the modal.
- Confirming the modal persists the final state in one transaction.
- Confirming a stale preview is rejected and requires generating a new preview.

## 13. Edge Cases

1. The caravan has consumption 0.
2. The caravan has only perishable supplies and no regular supplies.
3. The caravan has only regular supplies and no perishable supplies.
4. The caravan has no supplies at all.
5. Leftover food is exactly a multiple of 10.
6. Leftover food is between 0 and 10.
7. A perishable unit reaches exactly 0 after the `0.5` reduction.
8. More cooks exist than supplies consumed.
9. More portable kitchens exist than cooks.
10. More portable kitchens exist than consumed supplies.
11. No Supply Wagon has free space, but another wagon does.
12. No wagon has free space for rebuilt cargo.
13. Two wagons tie for highest available capacity.
14. A batidor hunts and therefore produces food while still counting as 0 consumption.
15. An agricultor crosses more than one full work unit because of accumulated progress and servant amplification.
16. A cook with cook-qualified servant and portable kitchen converts one supply into 25 food.
17. A boticario with non-qualified servant generates 7.5 po in the summary.

## 14. Open Questions

These points need product confirmation before implementation:

1. Should perishable decay happen exactly once per day or twice in the operational flow?  
   This spec currently follows the user-requested workflow, which effectively applies decay when perishable food is rebuilt into cargo after the day pass.
2. Should hunter/scout mode be chosen explicitly by the user during day pass, or inferred from the traveler’s current assignment?
3. Should future rules such as `Autonomía Extrema`, `Ayuno Intermitente`, `Consumo Eficiente`, `Hielo`, and refrigeration improvements be added in this same use case now, or in a follow-up spec increment?
4. When food is insufficient, should the system only show an alert, or also persist a structured shortage state for later downstream effects?
5. Should the legacy `CaravanDayResolution` persistence be deleted entirely in this slice, or replaced in-place with a new data contract under the same name?
6. Should artesano activity remain summary-only in this slice, or should a later increment persist crafting progress explicitly?

## 15. Implementation Notes

- Keep the algorithm in the backend application layer as a dedicated use case.
- Do NOT spread this logic across frontend components.
- Reuse the current cargo and wagon aggregates as the source of truth.
- Persist fractional food explicitly for perishable supply units.
- Treat preview as an in-memory simulation, not as a provisional persisted mutation.
- Do NOT implement preview by cloning or copying database tables.
- Prefer replacing the old day-pass types completely over bending the new semantics to fit legacy contracts.
- Persist agricultor work progress across days as explicit state; do not infer it transiently from the last preview.
- Model servant amplification with role-specific formulas, not with a single generic post-processing multiplier.
- Add focused tests for:
  - batidor consumption exclusion,
  - agricultor work progression and supply creation,
  - boticario value summary generation,
  - artesano summary reporting,
  - cook conversion,
  - cook servant priority,
  - portable kitchen conversion,
  - leftover-to-perishable conversion,
  - wagon reassignment priority,
  - preview non-persistence,
  - and stale-preview rejection on confirm.

## 16. Related Files

- `C:\Users\Alberto\workspace\GestionCaravana\docs\Reglas_de_Caravana.md`
- `C:\Users\Alberto\workspace\GestionCaravana\openspec\specs\caravan-cargo\spec.md`
- `C:\Users\Alberto\workspace\GestionCaravana\openspec\specs\caravan-travelers\spec.md`
- `C:\Users\Alberto\workspace\GestionCaravana\openspec\specs\caravan-wagons\spec.md`
