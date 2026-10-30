# Financial Tracker

A multi-currency personal finance application built with Spring Boot 3, supporting accounts, transactions, categories, statistics, budgets, recurring transactions, shopping lists, and household collaboration.

## Quick Start

### 1. Start PostgreSQL Database

```bash
cd docker-compose
docker compose up -d
```

### 2. Create Database Users & Schema

```bash
# Create users and database
docker exec -i $(docker ps --filter "ancestor=postgres" -q | head -1) psql -U postgres -d postgres < docker-compose/db/init/01-create-users.sql

# Create schema
docker exec -i $(docker ps --filter "ancestor=postgres" -q | head -1) bash < docker-compose/db/init/02-create-schema.sh
```

**Verify schema creation:**

```bash
# List tables in identity_service_schema (or financial_tracker_schema)
docker exec -i $(docker ps --filter "ancestor=postgres" -q | head -1) psql -U postgres -d identity_service_db -c "SET SEARCH_PATH TO identity_service_schema; SELECT table_name FROM information_schema.tables WHERE table_schema = 'identity_service_schema' ORDER BY table_name;"
```

### 3. Build Docker Image

```bash
# Build the application and create Docker image
./mvnw clean package -pl app && docker build -t financial-tracker:0.0.1-SNAPSHOT -f app/Dockerfile .
```

### 4. Deploy Application

```bash
# Start the application container
docker compose -f ./docker-compose/docker-compose-app.yaml up --force-recreate financial-tracker
```

**Access the API at:** `http://localhost:8050`

---

## Database Management

### Liquibase Commands

```bash
# Apply pending changesets
./mvnw liquibase:update -pl app

# Drop all database objects (development only!)
./mvnw liquibase:dropAll -pl app

# Rollback to a specific tag
./mvnw liquibase:rollback -Dliquibase.rollbackTag=1.x -pl app
./mvnw liquibase:rollback -Dliquibase.rollbackTag=1.1 -pl app

# Clear checksums (fix mismatched checksums)
./mvnw liquibase:clearCheckSums -pl app
```

### Manual Database Setup

If Liquibase fails or you need to reset the database manually:

```bash
# 1. Drop all objects
./mvnw liquibase:dropAll -pl app

# 2. Recreate users and schema
docker exec -i $(docker ps --filter "ancestor=postgres" -q | head -1) psql -U postgres -d postgres < docker-compose/db/init/01-create-users.sql
docker exec -i $(docker ps --filter "ancestor=postgres" -q | head -1) bash < docker-compose/db/init/02-create-schema.sh

# 3. Apply migrations
./mvnw liquibase:update -pl app
```

### Verify Database State

```bash
# List all tables in financial_tracker_schema
docker exec -i $(docker ps --filter "ancestor=postgres" -q | head -1) psql -U postgres -d financial_tracker_db -c "SET SEARCH_PATH TO financial_tracker_schema; SELECT table_name FROM information_schema.tables WHERE table_schema = 'financial_tracker_schema' ORDER BY table_name;"

# Check Liquibase changelog status
./mvnw liquibase:status -pl app

# Show detailed changelog report
./mvnw liquibase:changelogReport -pl app
```

---

## Testing

```bash
# Run all tests
./mvnw test -pl app

# Run specific test class
./mvnw test -pl app -Dtest=CategoryControllerSpringBootTest

# Filter by method
./mvnw test -pl app -Dtest=TransactionServiceTest#testCreateTransaction
```

---

## Architecture

- **Layered Hexagonal**: `controllers -> services -> repositories -> jpa_repositories -> entities`
- **Domain Models**: Immutable records in `models/` package with `@Builder(toBuilder = true)`
- **Security**: OAuth2 resource server with JWT authentication
- **API Versioning**: `X-API-VERSION` header (default `v1`)
- **Database**: PostgreSQL with Liquibase migrations

### Key Components

| Component | Path |
|-----------|------|
| Entry Point | `app/src/main/java/com/fabiankevin/app/App.java` |
| Controllers | `controllers/` |
| Services | `services/` |
| Repositories | `persistence/` |
| Domain Models | `models/` |
| JPA Entities | `persistence/entities/` |
| Liquibase | `app/src/main/resources/db/` |

---

## API Endpoints

| Controller | Path | Auth Required |
|-----------|------|---------------|
| `AccountController` | `/api/accounts` | USER |
| `CategoryController` | `/api/categories` | USER |
| `TransactionController` | `/api/transactions` | USER |
| `StatsController` | `/api/stats` | USER |
| `BudgetController` | `/api/budgets` | USER |
| `RecurringTransactionController` | `/api/recurring-transactions` | USER |
| `HouseholdController` | `/api/parties` | USER |
| `InvitationController` | `/api/parties/invitations` | USER |
| `UserCreatedEventController` | `/api/users` | user:provision |

**Public Endpoints**: `/actuator/health`, `/actuator/info`, `/actuator/prometheus`, `/swagger-ui/**`, `/v3/api-docs/**`

---

## Configuration

- **Default Profile**: `local` (uses H2 file-based database for development)
- **PostgreSQL Profile**: `local-pg`
- **Application Port**: 8050
- **Adminer**: http://localhost:8880 (database administration interface)
- **Swagger UI**: http://localhost:8050/swagger-ui.html

---

## Troubleshooting

### Liquibase Checksum Mismatch

```bash
./mvnw liquibase:clearCheckSums -pl app
./mvnw liquibase:update -pl app
```

### Database Connection Issues

1. Verify PostgreSQL is running: `docker ps | grep postgres`
2. Check logs: `docker logs postgres`
3. Test connection: `docker exec -i postgres psql -U postgres -d postgres -c "SELECT 1;"`

### Schema Not Found

Ensure users and schema are created:
```bash
docker exec -i $(docker ps --filter "ancestor=postgres" -q | head -1) psql -U postgres -d postgres < docker-compose/db/init/01-create-users.sql
docker exec -i $(docker ps --filter "ancestor=postgres" -q | head -1) bash < docker-compose/db/init/02-create-schema.sh
```
