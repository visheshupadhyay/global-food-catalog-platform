# Engineering Decision Log

This is the agreed decision record for the Global Food Catalog Platform. It captures decisions made during mentoring discussions before and during implementation. A decision is changed only through a later explicit discussion; we do not silently rewrite it in code.

## 1. Project and architecture

### D-001 — Build two bounded microservices

**Decision:** Release 1.0 uses two independently runnable Spring Boot services:

- **Catalog Command Service** owns catalog writes, validation, PostgreSQL source-of-truth data, and the transactional outbox.
- **Effective Menu Service** owns its materialized effective-menu read data, Redis serving cache, and customer effective-menu reads.

**Rule:** services do not read or write each other's business tables. They communicate through versioned Kafka events.

### D-002 — Deliver a production-grade core before the full showcase

**Decision:** Release 1.0 is a high-quality microservices core. Release 1.1 adds remaining Challenge #2 depth such as advanced schedules, nested modifier/bundle behavior, POS workflows, UI, Kubernetes, and cloud deployment.

**Reason:** finish a correct, demonstrable distributed system before adding broad platform surface area.

### D-003 — PostgreSQL layout for local development

**Decision:** use one local PostgreSQL container with separate service-owned databases and separate database users:

- `catalog_command_db` for Catalog Command Service
- `effective_menu_db` for Effective Menu Service

**Rule:** this preserves ownership locally while allowing later movement to separate PostgreSQL instances without changing service boundaries.

## 2. Shared catalog and outlet ownership

### D-004 — Shared menus use inheritance, not copied rows

**Decision:** a `MenuGroup` owns a shared base menu and can be assigned to many outlets. An outlet inherits base content from its active menu group.

**Reason:** do not copy one chain menu into hundreds of outlet-specific menus.

### D-005 — One active menu-group assignment per outlet in V1

**Decision:** an outlet has exactly one active shared `MenuGroup` at a time.

### D-006 — Base and outlet-local items share one item model

**Decision:** base items and outlet-local items use the same conceptual `menu_item` model.

- Base item: owned by one `MenuGroup`.
- Local item: owned by one `Outlet`.

**Rule:** an item has exactly one owner; it cannot simultaneously be base and local.

### D-007 — Sparse outlet overrides

**Decision:** an override stores only the fields an outlet changes. In Release 1.0, this includes price, availability, visibility, and approved presentation-content fields.

**Rule:** a missing override field inherits the current base value.

### D-008 — Override precedence and removal

**Decision:** an explicit outlet field override wins over the corresponding base field. Removing that override immediately restores the current base value.

Example: if base price changes from EUR 6.90 to EUR 7.10 while Berlin has an explicit EUR 7.40 override, Berlin remains EUR 7.40.

### D-009 — Base archive and dormant overrides

**Decision:** when a base item is archived, related outlet overrides become dormant rather than being deleted.

**Rule:** the archived item is never customer-visible. If the same base item is explicitly restored later, its dormant overrides become active again.

### D-010 — Menu-group reassignment preserves outlet configuration

**Decision:** when an outlet moves to another menu group, old inherited-item overrides become dormant rather than being deleted.

**Rule:** an authorized, explicit permanent cleanup may archive/purge dormant configuration with an audit record.

### D-011 — Outlet-local items survive menu-group reassignment

**Decision:** outlet-local items remain outlet-owned when the outlet changes menu groups. They may continue to be sold with the new inherited menu.

## 3. Category and item lifecycle rules

### D-012 — Local-item category placement

**Decision:** an outlet-local item may be placed in either:

- a suitable inherited category; or
- an outlet-owned local category, such as `Berlin Specials`.

**Rule:** an item appears in exactly one category in V1. An outlet cannot rename or modify an inherited category.

### D-013 — Outlet-owned categories and ordering

**Decision:** an outlet may create its own local category and chooses its display position among inherited categories.

**Rule:** category ordering uses sparse local display positions, for example `100`, `150`, and `200`, so an outlet can insert a category without renumbering every sibling. The outlet does not reorder shared categories for other outlets. A controlled rebalance is allowed if position gaps are exhausted.

### D-014 — Fallback category after a menu-group switch

**Decision:** every outlet has a fallback local category such as `Berlin Specials`.

**Rule:** if an outlet-local item was placed in an inherited category that disappears after a menu-group switch, the system moves the local item to the fallback local category and records an audit/event entry.

### D-015 — Categories form a tree

**Decision:** categories can contain subcategories.

**Guardrails:** prevent cycles, allow a maximum of three category levels, and order siblings. A deeper hierarchy is rejected by validation.

### D-016 — Item lifecycle states

**Decision:** use distinct lifecycle states:

- `DRAFT`: incomplete; never customer-visible.
- `ACTIVE`: eligible to appear, subject to visibility, availability, and schedule rules.
- `HIDDEN`: temporarily not customer-visible; normal restore/edit flow is allowed.
- `ARCHIVED`: retired from normal business use; not customer-visible; retained for history and explicit restore only.

## 4. Price, availability, and content

### D-017 — Currency belongs to the menu group

**Decision:** each menu group has one currency, for example EUR for a German group and GBP for a UK group.

**Rule:** base prices, local items, and outlet price overrides inside that menu group use the same currency. Mixed-currency menus are not allowed.

### D-018 — Money representation and consumer price basis

**Decision:** store prices as `BigDecimal` plus ISO currency; never use floating-point types for money.

**Decision:** Release 1.0 customer-facing menu prices are tax-inclusive. The catalog carries a tax category for a future order/tax calculation capability; it does not implement a full tax engine.

### D-019 — Visibility versus availability

**Decision:**

- Visible and available: show and allow ordering.
- Visible and unavailable: show as currently unavailable; do not allow ordering.
- Hidden: do not show to customers.

**Rule:** hiding a category hides all its children. An outlet may hide individual items inside an otherwise visible category. A globally hidden/archived base item or category cannot be resurrected locally.

### D-020 — Content overrides are reviewed; operational overrides are immediate

**Decision:** outlet overrides for name, description, and image require review before becoming customer-visible. Price, availability, and visibility changes become active immediately after automatic validation.

**Rule:** a pending content revision never replaces the currently active content. It becomes active only after approval; rejection retains the prior active content.

**Review ownership:** head-office catalog reviewers approve or reject outlet content changes and new outlet-local items. A new local item stays `DRAFT` / `PENDING_REVIEW` until approved.

### D-021 — Independent field changes

**Decision:** a pending content review does not block an urgent operational change such as a price or availability update.

**Rule:** each field/change type has independent change records. Concurrent edits to the same field require conflict handling.

## 5. Time, concurrency, and API behavior

### D-022 — Outlet-local schedule timezone

**Decision:** schedules are evaluated in the outlet's IANA timezone, such as `Europe/Berlin` or `Europe/London`.

**Rule:** backend timestamps are stored/exchanged in UTC. Daylight-saving and overnight-window details remain a later schedule-design decision.

### D-023 — Schedule correctness and cache warming

**Decision:** a scheduled background job pre-warms or refreshes caches at schedule boundaries, but it is not the sole correctness mechanism.

**Rule:** serving logic checks that a cached effective menu is still valid at the current outlet-local time. If cache warming fails, an expired entry is not served.

### D-024 — Optimistic locking for concurrent writes

**Decision:** use optimistic locking for concurrent edits to the same business field.

**Rule:** users do not hold database locks while editing. A stale write is rejected with a conflict and must be refreshed/retried deliberately.

### D-025 — HTTP idempotency

**Decision:** write requests use an idempotency key.

**Rule:** repeating the same request with the same key returns the original result and does not create duplicate changes or outbox events.

## 6. Events, propagation, and effective-menu reads

### D-026 — Full-state, versioned catalog events

**Decision:** catalog events carry the current full relevant state plus an entity version rather than only a small field delta.

**Rule:** the Effective Menu Service applies only newer versions and ignores stale or duplicate events.

### D-027 — Eventual consistency after accepted writes

**Decision:** the Catalog Command Service returns success after its source-of-truth transaction commits; it does not wait for the Effective Menu Service.

**Rule:** the response exposes accepted/propagation status, catalog version, and correlation identifier. The customer read model may lag briefly while Kafka propagation completes.

### D-028 — Transactional outbox when Kafka is unavailable

**Decision:** if PostgreSQL is available but Kafka is temporarily unavailable, save the catalog mutation and outbox record in one database transaction.

**Rule:** the outbox publisher retries later. The customer read model catches up after event delivery.

### D-029 — One base-change event; read side owns fan-out

**Decision:** a base change publishes one Kafka business event, for example `BaseMenuItemChanged` for a menu group.

**Rule:** the Effective Menu Service finds affected outlets and creates/processes bounded internal rebuild work. It may skip a write when an outlet's final effective menu did not change because of an override.

### D-030 — Redis read path

**Decision:** Redis is a derived serving cache, never source of truth.

**Rule:** a valid cache hit returns without a database query. A miss, eviction, invalidation, or expiry at a schedule boundary falls back to the Effective Menu Service's own read-model database; it never performs normal customer reads from the Catalog Command Service database.

### D-031 — Pending content changes are superseded by newer content

**Decision:** only the newest pending content revision for the same outlet, item, and content field is reviewed.

**Rule:** a newer submission marks the older pending submission `SUPERSEDED`. The old request remains in audit history and is not customer-visible.

### D-032 — Rejected and expired review requests are retained

**Decision:** rejected local-item/content requests remain as retained drafts with the reviewer reason. A request that exceeds its review deadline becomes `EXPIRED`.

**Rule:** rejected or expired requests are never customer-visible. The outlet may correct and resubmit the same draft.

### D-033 — Kafka partition keys follow the owner of the change

**Decision:** a base-menu event is keyed by `menuGroupId`; an outlet-local/override event is keyed by `outletId`.

**Rule:** same-owner events retain Kafka partition order. Cross-owner arrival order is handled by source versions and reconciliation.

### D-034 — Retry and DLQ policy

**Decision:** retry transient event-processing failures with exponential backoff and small random jitter. Use at most three total processing attempts: original delivery, retry after five seconds, and retry after thirty seconds.

**Rule:** after the final failed attempt, move the event to a DLQ. An operator inspects the cause and explicitly re-drives one event or a controlled batch only after the issue is fixed.

### D-035 — Rebuild and reconciliation of derived state

**Decision:** never manually patch an incorrect Effective Menu Service read-model row as the recovery mechanism.

**Rule:** rebuild derived state from trusted catalog event history or a trusted catalog snapshot, resolve the menu again, and compare it with the Catalog Service reference resolver.

### D-036 — Redis invalidation after catalog propagation

**Decision:** do not wait solely for TTL expiry after a catalog change. The Effective Menu Service invalidates or replaces the affected versioned Redis menu entry after it processes the catalog event.

**Rule:** Redis is not a write-behind source of truth. PostgreSQL source data and the transactional outbox are durable first; Redis is a derived serving copy.

## 7. Schema integrity and effective-menu representation

### D-037 — Service-owned numeric primary keys

**Decision:** Catalog Service tables use PostgreSQL-generated `BIGINT` primary keys. Each table has its own sequence, so `outlet.id = 1` and `menu_item.id = 1` may both exist.

**Rule:** an ID is always used with its entity type. The Catalog Service persists a new entity before writing its outbox event, so the generated ID is available in the event.

### D-038 — Database-enforced relationships and item ownership

**Decision:** PostgreSQL foreign keys protect relationships within a service-owned database. PostgreSQL also enforces that each menu item has exactly one owner: either one menu group or one outlet, never neither or both.

**Rule:** services never create database foreign keys across service databases. Java validates business rules too, but PostgreSQL remains the final integrity gate for local data.

### D-039 — One category model for shared and outlet-local categories

**Decision:** shared and outlet-local categories use one `menu_category` model/table. A category is owned by exactly one menu group or exactly one outlet.

**Reason:** both forms share the same structure: name, lifecycle, parent, and display position.

### D-040 — Outlet-local categories may be children of shared categories

**Decision:** an outlet-local category may be nested below a shared category from that outlet's active menu group.

**Guardrails:** the parent must belong to the outlet's active menu group. If that shared parent is permanently removed, or the outlet changes menu group, move the local category and its local items to the outlet fallback category and record an audit/event entry.

### D-041 — Temporary parent hiding preserves local-category placement

**Decision:** if a shared parent category is temporarily hidden, its outlet-local child category is hidden too; it is not moved.

**Rule:** when the shared parent becomes visible again, the local child returns in the same place. Only permanent removal or a menu-group switch triggers the fallback move.

### D-042 — Unique category labels in an effective outlet menu

**Decision:** customer-visible sibling categories in one outlet's effective menu must not have duplicate names.

**Rule:** use PostgreSQL constraints where ownership permits and Effective Menu/Catalog validation where a shared-plus-local effective view is required.

### D-043 — Immutable item business codes

**Decision:** every menu item has an immutable, chain-scoped business code in addition to its database ID, for example `BER-CLASSIC-001`.

**Rule:** customer-facing names may change. The business code does not change and is used for integrations, support, and durable business references.

### D-044 — Transactional, append-only catalog audit history

**Decision:** Catalog Service stores an append-only audit record for every important catalog change in its PostgreSQL database, in the same transaction as the source change.

**Rule:** Kafka events deliver changes to downstream services but are not the sole audit source.

### D-045 — Prepared effective-menu documents

**Decision:** Effective Menu Service stores one customer-ready effective-menu document per outlet in its own database.

**Rule:** one document read returns the full outlet menu. Redis caches that derived document; Catalog Service remains the source of truth and the Effective Menu Service rebuilds documents from catalog events or trusted snapshots.

## 8. Completed follow-up decisions

### D-046 — Explicit schedule windows and outlet-local DST evaluation

**Decision:** Release 1.0 represents an overnight business period as two explicit windows (for example, `22:00–24:00` and `00:00–02:00`). Schedule evaluation uses the outlet's IANA timezone, with backend timestamps stored/exchanged in UTC.

**Reason:** this keeps the first schedule model easy to validate while preserving correct outlet-local behavior across daylight-saving transitions.

### D-047 — Top-level-only modifier and bundle overrides

**Decision:** an outlet may override top-level product or bundle fields in Release 1.0. Nested modifiers/options remain head-office owned.

**Reason:** nested overrides create a much larger consistency and approval surface; stable nested-node identifiers can be added later.

### D-048 — Product-level reference-context overrides

**Decision:** a product-level outlet override applies everywhere that shared product appears in the outlet's effective menu.

**Reason:** one product has one clear effective price/content per outlet instead of surprising placement-specific values.

### D-049 — Promotion applies after outlet override

**Decision:** promotion calculation uses the outlet-effective price after the outlet override. For example, `EUR 7.50 × 80% = EUR 6.00`.

**Guardrails:** enforce minimum-price and eligibility rules, retain price provenance, and audit rejected or ineligible promotions.

### D-050 — Immediate POS acknowledgement with per-outlet status

**Decision:** a multi-outlet command returns an acknowledgement when accepted, while each outlet's processing result is tracked separately.

**Rule:** accepted means the command was durably accepted, not that every outlet has already applied it.

### D-051 — Asynchronous bulk operations

**Decision:** bulk changes return a `jobId` immediately and execute asynchronously in bounded batches.

**Rule:** expose progress and per-outlet outcomes; make processing idempotent, resumable, retryable, and compatible with DLQ handling.

### D-052 — Defer Release 1.1 platform features

**Decision:** complete and stabilize Release 1.0 before selecting Release 1.1 features.

**Reason:** advanced catalog structure, scheduling depth, POS workflows, UI, and platform deployment must not distract from proving the core distributed system.

### D-053 - Explicit archive restore lifecycle

**Decision:** normal chain lifecycle transitions move only forward: `DRAFT -> ACTIVE -> ARCHIVED`. Restoring an archived chain uses a separate explicit restore operation and returns it to `ACTIVE`.

**Reason:** a generic status update must not accidentally reopen retired catalog data. A restore operation makes the business intent visible and gives the application a future place for authorization, audit, and validation.

### D-054 — Repeatable local Catalog Command Service configuration

**Decision:** commit `application-local.properties` with the non-secret local Docker connection defaults for Catalog Command Service: `127.0.0.1`, host port `5433`, database `catalog_command_db`, and user `catalog_app`.

**Rule:** the password is supplied only through `CATALOG_DB_PASSWORD`; it is never committed. Testcontainers remains the database provider for integration tests, so tests do not depend on the persistent local database.

### D-055 — Unique working menu-group names within a chain

**Decision:** a restaurant chain may have only one non-archived menu group with a given display name. Names are trimmed and compared case-insensitively, so `Burgers`, `BURGERS`, and ` burgers ` are the same name.

**Rule:** `DRAFT` and `ACTIVE` groups both reserve their name. An `ARCHIVED` group does not reserve it, allowing a replacement group to use the name. Restoring an archived group is rejected if another non-archived group in the same chain already uses that name.

**Reason:** this keeps the working menu unambiguous while retaining retired menu history and allowing a future seasonal or rebuilt menu section to reuse its familiar label.

### D-056 — Stored normalized menu-group name for uniqueness

**Decision:** PostgreSQL stores a generated normalized menu-group name derived from the trimmed display name and uses it for the working-name uniqueness rule.

**Rule:** the normalized value is generated by the database, not supplied by application code. `DRAFT` and `ACTIVE` rows participate in the unique rule; `ARCHIVED` rows do not.

**Reason:** an explicit normalized value makes the invariant visible and queryable while database generation prevents the display name and normalized value from drifting apart.

## Open decisions

The following Release 1.0 decision remains open:

- Whether the REST API disables Spring's Open Session in View default (`spring.jpa.open-in-view`).

Release 1.1 feature selection will be revisited after Release 1.0 is complete.

The exact count and calculation are maintained in `docs/decision-progress.md`.

## 9. Confirmed item-creation decisions

### D-057 - Separate creation endpoints for shared and outlet-local items

**Decision:** use separate REST operations for shared menu items and outlet-local menu items.

**Rule:** a shared item is created under a menu group; a local item is created under an outlet. The URL makes the ownership choice clear instead of asking one request body to describe two different ownership models.

### D-058 - Custom duplicate-menu-item exception

**Decision:** duplicate chain-scoped menu-item business codes throw `DuplicateMenuItemException`.

**Reason:** this expresses a business conflict clearly and can later map naturally to HTTP `409 Conflict`, consistent with duplicate menu-group handling.

### D-059 - Optional category for outlet-local item creation

**Decision:** `menuCategoryId` is optional when creating an outlet-local item.

**Rule:** when an administrator supplies a category ID, the service validates that the category belongs to the same outlet and is usable. When the ID is omitted, the service finds or creates the outlet's default local Specials category and assigns its generated ID to the item.

**Reason:** administrators can place an item in an existing category while still allowing a new local item to be created without requiring the caller to know a category ID in advance.

### D-060 - Outlet default category display name

**Decision:** the default local category is initially named using the outlet's display name followed by ` Specials`, for example `Berlin Mitte Specials`.

**Rule:** create this category only once and reuse it for later local items. The lookup must not depend only on the visible name, because an outlet display name may change.

**Status:** the stable internal lookup mechanism (category-purpose field versus a separate outlet mapping) remains an implementation decision and is not assumed here.
