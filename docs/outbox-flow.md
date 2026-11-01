# Transactional Outbox Flow

The Catalog Command Service changes the catalog in PostgreSQL first. Kafka shares that
accepted change with the Effective Menu Service, but Kafka is not the source of truth.

## What happens when an admin changes an item?

```text
Admin request
    ↓
MenuItemService validates the request
    ↓
Menu item is updated and its version increases
    ↓
MenuItemChangedEventFactory creates an event
    ↓
Menu item and outbox event are saved in one database transaction
    ↓
Outbox worker reads PENDING events
    ↓
Kafka publisher sends the event and waits for broker acknowledgement
    ↓
PUBLISHED, or retry later, or DLQ after the final failure
```

The menu-item update and outbox insert succeed or fail together. If the transaction
fails, neither change remains. This prevents a changed menu item from existing without
an event that can notify the read side.

## Example

Suppose item `701` changes from EUR 10.00 to EUR 12.00:

```text
menu_item.id                   = 701
menu_item.item_price           = EUR 12.00
menu_item.version              = 4 → 5
outbox_event.aggregate_id      = 701
outbox_event.aggregate_version = 5
outbox_event.event_type        = MENU_ITEM_CHANGED
outbox_event.kafka_key         = 701
outbox_event.payload           = changed item state as JSON
```

The version lets consumers ignore an older or duplicate event. The Kafka key keeps
events for the same menu item ordered within one partition.

## Delivery outcomes

- `PENDING`: saved safely, but not delivered yet or waiting for another attempt.
- `PUBLISHED`: Kafka acknowledged the event.
- `DLQ`: all configured attempts failed; an operator must inspect the failure.

The publisher waits for Kafka acknowledgement before the delivery service marks an
event as `PUBLISHED`. A temporary failure keeps the event retryable. After the final
failed attempt, the event moves to the DLQ with its error message, retry count, and
failure timestamp.

## Replay

An operator can replay a DLQ event after fixing the underlying problem. A replay gets a
new `eventId`, keeps the failed event's ID as `originalEventId`, and starts with retry
count zero. This gives the replay its own delivery history while preserving the link to
the original failure.

## Why Redis is not involved here

Redis is a derived read cache. PostgreSQL and the outbox are durable first. Redis is
updated or invalidated only after the downstream read model processes the Kafka event;
it is never a write-behind source of truth.

## Operator recovery guide

### Check delivery state

```sql
SELECT id, event_id, status, retry_count, next_attempt_at, last_error
FROM outbox_event
ORDER BY id DESC;
```

`PENDING` waits for delivery or retry, `PROCESSING` is currently claimed, `PUBLISHED`
was acknowledged by Kafka, and `DLQ` requires operator investigation.

### Replay a DLQ event

Replay only after fixing the underlying problem. Replay creates a new event ID, stores
the failed event ID as `originalEventId`, resets the retry count, and starts as `PENDING`.
The original DLQ row is never modified.

### Reconcile a menu item

```text
GET /api/v1/reconciliation/menu-items/{menuItemId}/outlets/{outletId}
```

The result identifies a matching, missing, stale, or mismatched snapshot. Reconciliation
diagnoses drift; it does not patch the read model directly.

### Check propagation health

```text
GET /actuator/outbox
GET /actuator/effective-menu
```

These endpoints expose pending/retry/DLQ counts, consumer failures, duplicate and stale
events, processed events, and propagation lag.
