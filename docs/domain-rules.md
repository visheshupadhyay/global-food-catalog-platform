# Domain Rules

## Source of Truth
1. PostgreSQL is the authoritative store.
2. Base and outlet-local items share the menu_item table, with different owners.
3. Overrides contain only outlet-specific changes.
4. V1 calculates effective menus on request; later materialized state is derived, never authoritative.

## Menu Group Ownership
1. A RestaurantChain owns one or more MenuGroups.
2. A MenuGroup can serve many outlets.
3. An outlet has exactly one active MenuGroup assignment in V1.
4. A base menu item belongs to exactly one MenuGroup in V1.
## Base Items and Outlet-Local Items
1. Base and outlet-local items use the same menu_item table.
2. A base item has menu_group_id set and outlet_id null.
3. An outlet-local item has outlet_id set and menu_group_id null.
4. The database must enforce exactly one owner:
   menu_group_id XOR outlet_id.
5. A local item appears only in its owning outlet and never in sibling outlets.

## Menu Item Creation
1. A shared item is created under a menu group URL.
2. An outlet-local item is created under an outlet URL.
3. The service checks that the parent chain exists before checking the owner.
4. A menu group or outlet must belong to the same restaurant chain as the item.
5. New items start in `DRAFT` status with version `0`.
6. Business codes are unique within their restaurant chain.

These checks are made in the service for clear API errors and protected again by
PostgreSQL foreign keys and check constraints.
## Outlet Overrides
1. An OutletOverride connects one outlet to one inherited base menu item.
2. An outlet may create an override only for an item from the outlet’s currently active MenuGroup.
3. In V1, the allowed changed fields are price, availability, and visibility.
4. The override stores only fields that differ from the base item.
   At least one override field must have a value.
5. The same outlet cannot have two override records for the same base menu item.
6. Outlet-local items do not use OutletOverride because they already belong directly to the outlet.
## Override Precedence
1. If an outlet has an explicit override for a field, it wins over the base value for that field.
eg:
Base Big Burger price changes from €6.90 to €7.10.
Berlin has a €7.40 price override.
Berlin’s effective price remains €7.40.

2. If no override exists for a field, the current base value is inherited.
3. If an override is removed, that field immediately returns to the current base value.
4. A globally archived or globally hidden base item cannot be made visible again by an outlet.
## Availability and Visibility

1. A visible and available item is shown to customers and can be ordered.
2. A visible but unavailable item is shown as “Currently unavailable” and cannot be ordered.
3. A hidden item is excluded from the customer-facing effective menu.
4. In V1, a manual outlet availability override remains active until an authorized admin changes or removes it.

## Effective Menu Calculation

1. The V1 effective menu is calculated on request for a specific outlet.
2. The calculation loads base items from the outlet’s active MenuGroup.
3. It applies field-level outlet overrides to inherited base items.
4. It adds outlet-local items owned by that outlet.
5. The customer-facing response exposes only final effective values, not override provenance.
6. A later admin/debug view may expose base values, overrides, and provenance.

## Time and Timezones

1. Backend timestamps are stored and exchanged in UTC.
2. The backend JVM uses UTC.
3. Each outlet will later store its own IANA timezone.
4. Schedules will be evaluated in the outlet’s timezone, not the server timezone.

## V1 Scope and Deferred Capabilities

1. V1 includes shared base menus, one active MenuGroup per outlet, outlet-local items, price/availability/visibility overrides, calculated effective menus, PostgreSQL, Flyway, and integration tests.
2. V1 does not include schedules, nested modifiers/bundles, Redis, Kafka, transactional outbox, microservices, security, or a React UI.
3. Deferred capabilities will be introduced only after the V1 domain behavior is correct and tested.
