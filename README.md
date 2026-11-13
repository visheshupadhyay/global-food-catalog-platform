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

Creating, replacing, or deleting a schedule writes schedule rows and its schedule
change outbox event in one transaction. Controllers never publish directly to Kafka.

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
