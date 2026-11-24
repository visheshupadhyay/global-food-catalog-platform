# Catalog Command Service

The Catalog Command Service owns restaurant chains, outlets, menu groups, menu items, overrides, and the transactional outbox.

## Local run

Start PostgreSQL and Kafka from `deliveryhero-platform-integration`, then run. The standalone local database uses port `5433`; the integration Compose stack maps Catalog PostgreSQL to host port `5435`.

```powershell
$env:CATALOG_DB_URL = "jdbc:postgresql://127.0.0.1:5433/catalog_command_db"
$env:CATALOG_DB_USERNAME = "catalog_app"
$env:CATALOG_DB_PASSWORD = "catalog_password"
$env:CATALOG_KAFKA_BOOTSTRAP_SERVERS = "127.0.0.1:9092"
mvn spring-boot:run
```

The service runs on `http://localhost:8080`. Flyway owns schema changes; Hibernate validates the schema.

Health endpoint: `http://localhost:8080/actuator/health`.

### Scheduled menu availability

Schedules are recurring weekly windows. Base schedules may have multiple
non-overlapping windows per weekday; an outlet can provide sparse weekday overrides.
Unspecified weekdays inherit the base schedule. Windows use the outlet's IANA
timezone. Outside an active window the item remains visible but unavailable, while
`HIDDEN`, `DRAFT`, and `ARCHIVED` lifecycle rules still control visibility.

### Seasonal availability

An item can also have one optional base date range or one outlet-specific override.
The range is evaluated in the outlet's IANA timezone as `[startDate, endDate)`: start
is included and end is excluded. Catalog availability, the weekly schedule (when
configured), and the seasonal range (when configured) must all be active.

Creating, replacing, or deleting a schedule writes schedule rows and its schedule
change outbox event in one transaction. Controllers never publish directly to Kafka.

### Bundles and modifiers

Catalog models modifier groups/options separately from products and bundles. A modifier
configuration event contains the complete replacement group list. A bundle event is
`BUNDLE_CONFIGURATION_CHANGED` and carries the complete, resolved child tree for each
affected root/outlet. This lets Effective Menu replace stale child summaries safely
after a bundle edit or after a reusable child changes. Bundles use their own fixed
price; children contain ordered ID/name/quantity summaries for customer display.

#### Bundle admin API

Create a meal or combo by sending `"menuItemKind": "BUNDLE"` when creating a
menu item. Omitting the field remains backward compatible and creates a normal
`PRODUCT`.

Replace a bundle's complete direct-child list with:

```http
PUT /api/v1/restaurant-chains/{restaurantChainId}/menu-items/{bundleMenuItemId}/bundle-children
```

```json
{
  "expectedVersion": 0,
  "children": [
    { "childMenuItemId": 10, "quantity": 1, "displayOrder": 0 },
    { "childMenuItemId": 11, "quantity": 1, "displayOrder": 1 }
  ]
}
```

Read the current child list and its version before preparing a later replacement:

```http
GET /api/v1/restaurant-chains/{restaurantChainId}/menu-items/{bundleMenuItemId}/bundle-children
```

This is complete replacement, not a partial patch: omitted children are removed,
listed children are retained or added, and the response returns the next bundle
version for the following optimistic-locking request. The child relationships and
their `BUNDLE_CONFIGURATION_CHANGED` outbox event commit or roll back together.

A newly created bundle starts in `DRAFT`, just like any new menu item. Configure
its children first, then activate it with the returned composition version. This
ensures the first customer-visible bundle event already contains a complete tree.

### Health and readiness

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health/liveness
Invoke-RestMethod http://localhost:8080/actuator/health/readiness
Invoke-RestMethod http://localhost:8080/actuator/health
```

Liveness reports that the process is alive. Readiness includes PostgreSQL and Kafka;
an outage of either dependency makes readiness `DOWN`. Kafka is checked by the custom
`kafkaHealthIndicator` using `spring.kafka.bootstrap-servers`.

## Event flow

```text
Admin command
    -> Catalog Command Service
    -> PostgreSQL transaction + transactional outbox
    -> Kafka: catalog.menu-item.changed
    -> Effective Menu Service
    -> customer-ready outlet snapshot
    -> Customer Menu API
```
