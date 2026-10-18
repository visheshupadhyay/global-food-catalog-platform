# Roadmap Gap Analysis — Covered vs. Missing
### Global Restaurant Catalog & Outlet Override Platform vs. Delivery Hero's "World's Greatest Food Catalog" blog

> **Goal of this file:** one place that lists everything the roadmap **already covers**, everything it **only half-covers**, and everything it **does not include**, with a simple "toy-store" example for each so anyone can understand it.

---

## How to read this file

| Symbol | Meaning |
|---|---|
| ✅ | **Covered** — the roadmap already has this |
| ⚠️ | **Underspecified** — the roadmap mentions it, but the exact rule is not decided yet. You will be forced to decide when you write code |
| ❌ | **Not included** — the roadmap does not have this at all |
| 🚫 | **Deliberate non-goal** — intentionally left out, and that is fine |

### The toy-store story used in every example

> **Burger House** is a chain of shops. **Head office** writes one price list (the *base menu*). Each **shop** (outlet) can change a few things on its own (an *override*). The **customer** only ever sees the final result: the *effective menu*.

```text
Effective Menu  =  Head-office price list  +  Shop's own changes  +  Clock/schedule rules
```

---

# PART A — ✅ What the roadmap already covers (Challenge #2 core logic)

| # | Capability | Kid-simple example | Where in the roadmap |
|---|---|---|---|
| A1 | **Shared menu for many outlets** (Menu Group) | Head office writes the price list **once**. 500 shops read the same list. Nobody photocopies it 500 times. | Phase 1, `MenuGroup`, 500-outlet fixture |
| A2 | **Field-level overrides** (price / availability / visibility) | Berlin shop says "Big Burger costs €7.40". Only the *price* is changed. The name, photo and description still come from head office. | Phase 4 |
| A3 | **Parent change keeps outlet customization** | Head office changes the burger from €6.90 to €7.10. Berlin already set its own €7.40, so Berlin **stays** at €7.40. Other shops move to €7.10. | Phase 4, acceptance scenarios |
| A4 | **Revert to base** | Berlin removes its special price. It instantly goes back to whatever head office says right now. | Phase 4, `DELETE override` |
| A5 | **Outlet-only (local) items** | Only the Kreuzberg shop sells a "Currywurst Burger". No other shop ever sees it. | `LOCAL_ITEM`, `outlet_local_item` |
| A6 | **Seasonal / scheduled items** | A Halloween burger shows up Oct 31 at one shop only. | `schedule_rule`, time zone rule |
| A7 | **Hidden base item can't be brought back by a shop** | Head office retires "Cola Zero". A shop cannot secretly turn it back on. | Release 1.0 business rules |
| A8 | **Nested products, modifiers, bundles** | A "Combo" has a "Choose your side" group, which has "Fries", which has "Size". Many layers deep. | Gate 3 |
| A9 | **Safety limits on nesting** | Max 8 layers deep, no loops (a combo can't contain itself). | Depth / cycle guardrails + ADR |
| A10 | **Effective state calculated correctly first, then made fast** | First, a slow-but-correct "answer key" (the reference resolver). Then a fast cached version that is *checked against* the answer key. | Phase 5, Phase 27 |
| A11 | **Redis cache with versions and stampede protection** | A "ready menu" is kept on a shelf for quick grabbing. If 1,000 customers ask at once, only one goes to the kitchen to rebuild it. | Phase 6 |
| A12 | **Command + acknowledgement protocol** | Head office sends a letter: "change price". The shop replies with exactly one answer: **SUCCESS**, **FAILURE** (something broke, try again) or **REJECTED** (a rule says no). | Phase 7, 11 |
| A13 | **REJECTED is never auto-retried, FAILED is** | "No, we have a sale on, you can't change that" is a final answer. "Phone line was busy" is worth another try. | Phase 7, 11 |
| A14 | **Idempotency** (same message twice = applied once) | The postman delivers the same letter twice. The shop only changes the price once. | Phase 8 |
| A15 | **Transactional outbox** | Write the change **and** the "tell everyone" note in the same notebook page, so you can never save one and forget the other. | Section 34 |
| A16 | **Retries, DLQ, replay** | A letter nobody can read goes in a "problem box" so it doesn't block the others. Someone can fix and resend it later. | Phase 9 |
| A17 | **Optimistic locking** | Two admins edit the same price. The second one is told: "Someone changed it while you were looking. Look again." | Phase 10 |
| A18 | **POS import boundary** | A cash register system sends a menu change. It carries its name, an ID, and a timestamp. Duplicates and stale messages are handled. | POS Import Boundary section |
| A19 | **Business validation** (negative price, huge jump, schedule conflict, promo lock) | Price of −€5? Rejected. Price jumps from €7 to €70? Flagged. | Phase 12 |
| A20 | **Pending commands have timeouts** | A letter waiting too long raises an alarm. | Phase 11 |
| A21 | **Reconciliation** | A teacher re-checks the fast menu against the answer key to catch mistakes. | Gate 8 |
| A22 | **Why SQL (Postgres) instead of NoSQL** | The data is naturally Group → Outlets → Menus → Products, which tables handle well. (Delivery Hero moved from DynamoDB to Aurora SQL for the same reason.) | Phase 2 ADR |
| A23 | **Observability, load testing, failure experiments** | Dashboards, speed tests, and "what if Redis dies?" drills. | Phases 13–17 |

---

# PART B — ⚠️ Logic gaps: mentioned, but the rule is not decided yet

These are the **most important** items. They are the places where, once you start coding the resolver, you will have to make a decision that the roadmap has not made for you.

---

## ⚠️ G1. Time changes the menu, but nobody sends an event

**Priority: 🔴 High**

**The problem**
The roadmap rebuilds the effective menu **when an event arrives**. But schedules change what customers see **just because the clock moves**, with no edit and no event.

**Kid example**
The Halloween candy should appear at **midnight on Oct 31**. At 11:59 PM the computer saved a "ready menu" with no candy. At midnight, **nobody edited anything**, so no message was sent. The saved menu still has no candy. Customers don't see it for hours.

**Why it matters**
Cached / materialized menus will be silently wrong at every schedule boundary (lunch menu start, breakfast menu end, seasonal start/end).

**Suggested fix (pick one)**
1. Store the schedule rules **inside** the saved menu and check "what time is it now?" on every read, or
2. Run a scheduled job that rebuilds affected outlets exactly when a schedule starts or ends.

**Also decide**
- Overnight windows (e.g. 22:00 → 02:00)
- Daylight-saving changes (the 02:30 that doesn't exist, or happens twice)
- Date-bounded windows (Oct 25 → Nov 1)

**Test to write first:** *"At 23:59 the candy is hidden; at 00:00 it is visible, with no event in between."*

---

## ⚠️ G2. A product used inside another product needs a third layer of rules

**Priority: 🔴 High**

**The problem**
The blog says modifiers can **point to an existing product** and **override some of its properties in the context of the modifier group they're in**. The roadmap's precedence has only two layers:

```text
Outlet override  >  Base value
```

But real menus need three:

```text
Outlet override  >  "Inside-this-combo" special rule  >  The product's own value
```

**Kid example**
Fries cost **€3.20** alone. Inside the "Burger Combo" the combo says **"Fries: only +€1"**.
Now the Berlin shop sets **"Fries cost €3.50."**
- Fries sold alone → clearly €3.50.
- Fries inside the combo → still **+€1**? Or does Berlin's change also touch the combo?

**Second question: where does a shop's change apply?**
Fries appear in 3 places (alone, in the combo, in the kids meal). If Berlin changes "Fries", does it change **all 3** or just **one**? The roadmap's `target_node_id` cannot say "just this one place".

**Suggested fix**
- Add a **reference-context override** layer (override stored on the *reference*, not on the product).
- Decide: does an override target a **product** (applies everywhere) or a **reference path** (applies only there)? Support both, or choose one and write it down.
- The roadmap already lists *"override of a modifier/product reference without mutating the shared referenced product"* as a test case, but the data model and precedence do not support it yet.

**Test to write first:** *"Berlin sets Fries to €3.50. Standalone Fries = €3.50. Fries in Combo = +€1 (or the rule you decided)."*

---

## ⚠️ G3. A change that is not approved yet — what does the customer see?

**Priority: 🔴 High**

**The problem**
In the roadmap's flow, a change is **saved first**, then sent out, then **acknowledged** (SUCCESS / FAILURE / REJECTED) later. Between "saved" and "acknowledged", the roadmap does not say:
1. Does the effective menu **include** the not-yet-approved change?
2. If the answer later is **REJECTED**, what happens to what was saved?
3. What if someone makes a **second** change while the first is still waiting?

**Kid example**
The shop owner changes the burger price to **€7.40**. It is saved but is still **waiting for head office to say OK**. Should customers see €7.40 right now? And if head office says **NO**, is €7.40 still in the notebook?

**Why it matters**
Without a rule, a **rejected** price can leak onto the menu, or the database can disagree with what the brand catalog accepted.

**Suggested fix**
- Give every override a status: `PENDING` → `ACTIVE` or `REJECTED`.
- The resolver reads **only `ACTIVE`** overrides.
- Update the "one active override per outlet/target/field" constraint so a `PENDING` row doesn't clash with an `ACTIVE` one.
- Add a rule from the blog: **a pending command blocks further changes to the same item**, or a newer *non-structural* change **cancels and replaces** the older pending one.

**Test to write first:** *"Override is PENDING → menu shows old price. Override becomes ACTIVE → menu shows new price. Override is REJECTED → menu still shows old price and the override row is not active."*

---

## ⚠️ G4. If one needed piece is missing, the whole thing should disappear

**Priority: 🟠 Medium-High**

**The problem**
The roadmap resolves each node's **own** availability. It does not say that availability should **travel up and down the tree**.

**Kid example 1 — Required choice with nothing left**
"Make-your-own pizza" forces you to pick a crust. The shop is out of **every** crust. The pizza is still marked "available". A customer taps it and **can't finish ordering**.

**Kid example 2 — Combo with a missing part**
"Burger Combo" = burger + fries + drink. The fryer is broken, so fries are unavailable. The combo still shows as orderable.

**Kid example 3 — Hiding a whole shelf**
"Breakfast" category is hidden after 11 AM. Do all the items inside also disappear? (They should. Hidden parents hide children.)

**Suggested fix**
Add derived-availability rules:
- Hidden/unavailable **parent** → children are not shown.
- **Required** modifier group with **zero** available options → the product is not orderable.
- Bundle with an unavailable **required** component → the bundle is not orderable.

**Test to write first:** *"All crusts unavailable → pizza is not in the effective menu."*

---

## ⚠️ G5. Two cooks writing the same recipe

**Priority: 🟠 Medium-High**

**The problem**
- The **Catalog Service** has a "reference resolver" (the answer key) in its tests.
- The **Effective Menu Service** "applies the inheritance rules" on its own.

That is **two separate implementations of the same logic**, and over time they will drift apart.

Also, the Effective Menu Service **cannot read** the Catalog database. So it can only work with what the events carry.

**Kid example**
Cook A and Cook B each write down "how to make the final menu". After a few months each edits his copy slightly. Now the same order gives two different menus.

And: Cook B can't peek in Cook A's notebook. A message says "price went up", but **from what to what?** If two messages arrive in the wrong order, Cook B gets confused.

**Why it matters**
Events are partitioned by `menuGroupId` for base changes and `outletId` for outlet changes. Those can arrive **in a different order** at the Effective Menu Service.

**Suggested fix**
1. Use **one shared resolver module** (a library both services use), **or** keep both implementations but run the **same golden test files** against both.
2. Make events carry **full state + version**, not just "what changed".
3. Apply the rule: *"Only apply an event if its version is newer than what I already have for that thing."*
4. Give the Catalog Service an **internal API** that returns the reference-resolved menu, so reconciliation has something to compare against. (This respects "no service reads another service's tables".)

**Test to write first:** *"Feed the same events in two different orders → the final effective menu is identical."*

---

## ⚠️ G6. What happens to leftovers? (orphan rules)

**Priority: 🟡 Medium**

**The problem**
The roadmap lists these as cases to test, but never says what the **correct behavior** is.

**Kid example A — Base item deleted**
Berlin has a special price for "Big Burger". Head office **deletes** Big Burger. The special price now points at **nothing**.

**Kid example B — Shop switches groups**
Berlin moves from the "Burger House" group to the "Pizza House" group. Its old special prices point at burgers that **don't exist** in the new group.

**Suggested fix (decide, then write it in `docs/domain-rules.md`)**

| Situation | Option 1 | Option 2 |
|---|---|---|
| Base item archived while overrides exist | Keep override **dormant** (comes back if item is restored) | Delete overrides |
| Outlet moves to a different menu group | **Drop** all old overrides | **Remap** by a mapping table |

Without a written rule, every programmer will invent a different one.

**Test to write first:** *"Archive Big Burger → Berlin override is dormant and the item is not visible. Restore Big Burger → override is active again."*

---

## ⚠️ G7. One big change must become many small commands

**Priority: 🟡 Medium**

**The problem**
The roadmap dashboard shows:

```text
Big Burger  €6.90 → €7.20
22 outlets → SUCCESS
 1 outlet  → REJECTED
 1 outlet  → PENDING
```

But one command can only have **one** status. The schema (`menu_change`, `change_acknowledgement`) does not show how one chain-level change becomes 24 different answers.

**Kid example**
Head office sends **one** letter to 24 shops: "Burger now costs €7.20". Shop 1 says "done", Shop 2 says "no, we have a sale", Shop 3 hasn't answered. A single letter can't hold three different answers.

**Suggested fix**
Split the big change into **24 small commands**, one per shop, all tied to the same **parent change ID**. Then each shop has its own status, and the parent can show a summary (22 / 1 / 1).

**Test to write first:** *"One base change creates N child commands; each child can end in a different state; the parent summary adds up to N."*

---

# PART C — ❌ Not included: things the blog describes that the roadmap does not have

---

## ❌ M1. Human review step before a change goes live

**Blog:** Changes pass through quality control, and agents can **approve, reject or edit** based on local business and legal rules before the command is published.
**Roadmap:** Only **automatic** validation (price, schedule, etc.).

**Kid example**
A shop types a price of "€0.01" for a steak. The computer says "that's a positive number, OK!" A human reviewer would say "that looks wrong, please check."

**Suggested fix:** Add a `PENDING_REVIEW` state with `approve` and `reject` actions. Only approved changes continue to the outbox.

---

## ❌ M2. Pending changes block new changes to the same item

*(Also covered as a part of G3. Listed here because the blog states it as an explicit rule.)*

**Blog:** Pending commands have timeouts **and currently block further changes**. Sometimes a new change can cancel an old one.
**Roadmap:** Timeouts exist, but no blocking / cancel / supersede rule.

**Kid example**
You ask your teacher to change your seat. Before she answers, you ask to change it again. Does the second request wait, replace the first, or get refused?

**Suggested fix:** Write an ADR with a rule: *"One pending command per entity. A new non-structural change may supersede it; a structural change must wait."*

---

## ❌ M3. Business-health measurements (KPIs)

**Blog:** Tracks photo coverage, description coverage, translation coverage, item conversion, availability, "zombie items" (items nobody orders), and also **command round-trip time** and **time-to-validate**.
**Roadmap:** Tracks engineering signals (Kafka lag, cache hit rate, P95/P99, DLQ size), but not these.

**Kid example**
A shop has 100 items but only 20 have photos. A "menu health score" would say "only 20% have photos". Without it, nobody notices.

**Suggested fix (cheap and high value):**
- Metric: **command round-trip time** (sent → acknowledged)
- Metric: **time-to-validate**
- Optional: a simple **photo/description coverage %** per outlet

---

## ❌ M4. Safe migration by running old and new side by side

**Blog:** DynamoDB and Aurora ran **in parallel for about a month**, both filled from the stream, and validated by **replaying the full history**.
**Roadmap:** Has replay and reconciliation, but no "rebuild the read model from scratch and compare" exercise.

**Kid example**
You build a new, better notebook. For a month you write in **both** the old and new notebooks, and compare them every day. Only when they match do you throw the old one away.

**Suggested fix (Release 1.1 demo):** A command that **deletes and rebuilds** the Effective Menu read model from Kafka history, then compares it with the live one. If they match, the demo passes.

---

## ❌ M5. Bulk operations

**Blog:** "Bulk operations for managing hundreds of items at once across many outlets" is a stated next step.
**Roadmap:** Only single-item endpoints (`PUT /overrides/{itemId}`).

**Kid example**
Head office wants to raise **every** price by 5% in **200** shops. Without bulk, someone clicks 200 × 500 = **100,000** times.

**Suggested fix:** A bulk endpoint like `POST /outlets/overrides:bulk` with validation, partial-success reporting, and an idempotency key.

---

## ❌ M6. Menu content fields (photos, descriptions, translations)

**Blog:** Photos, descriptions and translations are a big part of the catalog.
**Roadmap:** The item model does not mention them.

**Kid example**
A menu with no pictures, no descriptions and only one language is just a list of names and numbers. Nobody wants to order from it.

**Suggested fix:** Add `description`, `image_url`, `sort_order`, tags (vegan, spicy), allergens, and a `translations` structure to the item model. They cost very little and make the demo feel real.

---

## ❌ M7. Regional / country-level deployment

**Blog:** Easily configurable **regional or country-level** deployments to reduce latency and avoid bottlenecks.
**Roadmap:** Kubernetes is planned, but not per-region deployment.

**Kid example**
One huge warehouse in Germany serves the whole world. A customer in Korea waits much longer than a customer in Berlin. Small local warehouses fix that.

**Suggested fix:** Don't build it. Instead, write an answer for the interview question *"What would you change at 100× scale?"* that includes **per-region deployments**.

---

# PART D — 🚫 Deliberate non-goals (leaving these out is fine)

| # | Blog topic | Kid example | Why leaving it out is OK |
|---|---|---|---|
| N1 | **Unifying 7 food taxonomies into one** | 7 shops call the same thing "combo", "value menu", "bundle", "set". Someone has to create one common word. | A huge cross-company effort. Roadmap states it as a non-goal. |
| N2 | **Menu digitization with ML/LLM** (scan a paper menu) | Take a photo of a paper menu, and the computer types it in for you. | A different skill set (ML), not Challenge #2. |
| N3 | **Menu Intelligence** (price/photo/description suggestions) | The app says "Shops like yours sell burgers at €7.50". | Needs real market data. |
| N4 | **Agent backoffice tools** | Special screens for support staff to review shop content. | Out of scope for a backend portfolio. (M1 gives a tiny version.) |
| N5 | **RFC process with all platform teams** | Before changing the message format, every team must agree in writing. | Your ADRs + event-schema evolution are the solo version of this. |
| N6 | **Real POS vendor protocols** | A real cash register speaks its own secret language. | The simulated POS adapter proves the same idea. |

---

# PART E — Summary

## All gaps at a glance

| ID | Gap | Status | Priority | One-sentence kid summary |
|---|---|---|---|---|
| G1 | Schedule changes with no event | ⚠️ | 🔴 High | The clock matters, even when nobody edits anything. |
| G2 | Third precedence layer for references | ⚠️ | 🔴 High | A product inside a combo can have its own special rule. |
| G3 | Pending vs. approved changes | ⚠️ | 🔴 High | Don't show a change that isn't approved yet. |
| G4 | Derived availability through the tree | ⚠️ | 🟠 Med-High | If a needed piece is missing, hide the whole thing. |
| G5 | Two copies of the resolver logic | ⚠️ | 🟠 Med-High | Use one recipe, and send full facts with version numbers. |
| G6 | Orphan overrides (delete / regroup) | ⚠️ | 🟡 Medium | Decide what happens to leftover special prices. |
| G7 | Fan-out: 1 change → N commands | ⚠️ | 🟡 Medium | One big change needs one small letter per shop. |
| M1 | Human review step | ❌ | 🟡 Medium | A person double-checks weird changes. |
| M2 | Pending blocks / cancels new changes | ❌ | 🟡 Medium | What happens if you ask twice before an answer? |
| M3 | Business KPIs + round-trip time | ❌ | 🟢 Low (cheap) | Measure how healthy the menu is. |
| M4 | Parallel-run migration / rebuild demo | ❌ | 🟢 Low | Run old and new side by side, then compare. |
| M5 | Bulk operations | ❌ | 🟢 Low | Change 200 shops without 100,000 clicks. |
| M6 | Photos / descriptions / translations | ❌ | 🟢 Low (cheap) | A menu needs pictures and words. |
| M7 | Regional deployments | ❌ | 🟢 Low | Put small warehouses near customers. |

## Recommended order of work

1. **Before writing the resolver:** add rules for **G1, G2, G3** to `docs/domain-rules.md`. These three cause the most rework if discovered late.
2. **While building the resolver:** settle **G4, G6**, and design events for **G5**.
3. **When building commands/acknowledgements:** design **G7** and **M2** together.
4. **Cheap wins for realism:** **M3, M6**.
5. **Release 1.1 extras:** **M1, M4, M5**, and write about **M7**.

## Suggested new rules for `docs/domain-rules.md`

```text
Rule 9   (G1)  Effective visibility depends on the clock. Schedules are evaluated at read time
               (or rebuilt at boundaries). DST and overnight windows are defined.
Rule 10  (G2)  Precedence is: outlet override > reference-context override > product value.
               Overrides target either a product or a specific reference path.
Rule 11  (G3)  Only ACTIVE overrides affect the effective menu. PENDING and REJECTED never do.
               One pending command per entity; newer non-structural changes may supersede it.
Rule 12  (G4)  Hidden/unavailable parents hide children. A required modifier group with no
               available options makes its product unorderable.
Rule 13  (G6)  Archived base items keep overrides dormant. Moving an outlet to another
               menu group drops (or remaps) its overrides.
Rule 14  (G7)  A chain-level change creates one child command per outlet under a parent change id.
```

## Test list (write these first)

| Gap | Test sentence |
|---|---|
| G1 | At 23:59 the candy is hidden. At 00:00 it is visible. No event in between. |
| G2 | Berlin sets Fries to €3.50 → alone = €3.50, in combo = the decided rule. |
| G3 | PENDING override → old price. ACTIVE → new price. REJECTED → old price. |
| G4 | All crusts unavailable → the pizza is not in the menu. |
| G5 | The same events in two different orders → the same final menu. |
| G6 | Archive base item → override dormant. Restore → override active again. |
| G7 | One base change → N child commands → each can end differently → parent summary adds up. |

---

*Sources compared: the project roadmap (`DeliveryHero_Project_Roadmap.md`) and Delivery Hero Tech's blog post "We're building the world's greatest Food Catalog" (April 2026).*
