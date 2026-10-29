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
