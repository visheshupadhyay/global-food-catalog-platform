# Effective Menu Event Flow

The Catalog Command Service owns the real catalog data. The Effective Menu Service owns a customer-ready copy for each outlet.

```text
Catalog Command Service
        |
        | Save the menu change and outbox event in one transaction
        v
Transactional Outbox
        |
        | Publisher sends the event
        v
Kafka: catalog.menu-item.changed
        |
        | Effective Menu Service consumes and validates it
        v
Effective Menu Service
        |
        | Update only the event's outlet snapshot
        v
Read Model PostgreSQL Database
        |
        | Customer API reads the snapshot
        v
Customer Menu API
```

## Event handling rules

- Every new catalog event is outlet-specific.
- The event's `outletId` identifies which snapshot may change.
- A missing or non-positive `outletId` is rejected before repository access.
- Duplicate event IDs are ignored after the first successful processing.
- An event with a version older than or equal to the snapshot version is stale and is not applied.
- A newer event changes only fields supplied in the event; missing fields remain unchanged.

## Visibility and availability

- `visibility = false` means the item is hidden and should not be shown to customers.
- `availability = false` means the item remains visible but cannot currently be ordered.
- `DRAFT` and `HIDDEN` items remain in the read model for operational consistency, but are removed from the customer response.
- `ARCHIVED` removes the item from the outlet snapshot. An event referring to an absent item is rejected, so an archived item cannot be recreated accidentally.

## Lifecycle event example

```text
Catalog item status: ACTIVE
        |
        | MenuItemChangedEvent(status = HIDDEN)
        v
Effective Menu snapshot: item remains, visibility = false
        |
        v
Customer API: item is not returned
```

An unavailable item follows a different rule: it remains in the customer response with `availability = false`, so the client can show it as sold out.

The read model is derived state. The Catalog Command Service remains the source of truth, while retry and DLQ handling protect the read model from malformed or failed events.
