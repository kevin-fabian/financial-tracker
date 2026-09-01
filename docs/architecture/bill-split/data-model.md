# Bill Split Data Model

## New Tables

### `splits`

Allocates a transaction's cost across participants. No party involvement.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PRIMARY KEY, `gen_random_uuid()`, NOT NULL | Split record identifier |
| `transaction_id` | UUID | NOT NULL, FK → `transactions(id)` | The transaction being split |
| `user_id` | UUID | NOT NULL | The participant who owes this share (plain UUID, no FK) |
| `amount` | NUMERIC(12, 2) | NOT NULL, CHECK ≥ 0 | The share amount this member owes |
| `split_type` | VARCHAR(16) | NOT NULL, CHECK IN ('EQUAL', 'CUSTOM') | How the share was calculated |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | When the split was created |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Last modification timestamp |

**Indexes**:
- `idx_splits_transaction_id` on `transaction_id` — fast lookup of splits for a transaction.
- `idx_splits_user_id` on `user_id` — participant's obligation queries.
- `idx_splits_transaction_user` on `(transaction_id, user_id)` — unique constraint guard.

**Unique constraint**:
- `uk_splits_transaction_user` on `(transaction_id, user_id)` — a participant has at most one split per transaction.

**Foreign keys**:
- `fk_splits_transaction_id` → `transactions(id)` — cascade restricted (split survives if transaction is patched, deleted via service layer).

### `settlements`

Records when one participant pays another. No party involvement.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PRIMARY KEY, `gen_random_uuid()`, NOT NULL | Settlement record identifier |
| `payer_user_id` | UUID | NOT NULL | The participant who paid |
| `payee_user_id` | UUID | NOT NULL | The participant who was paid |
| `amount` | NUMERIC(12, 2) | NOT NULL, CHECK > 0 | Settlement amount |
| `description` | TEXT | NULL | Optional memo |
| `related_split_ids` | JSON | NULL, default `'[]'` | References to specific splits being settled |
| `transaction_id` | UUID | NULL, FK → `transactions(id)` | Settlement transaction (created per AD-7) |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | When the settlement was recorded |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Last modification timestamp |

**Indexes**:
- `idx_settlements_payer_id` on `payer_user_id` — payer's outgoing settlements.
- `idx_settlements_payee_id` on `payee_id` — payee's incoming settlements.
- `idx_settlements_payer_payee` on `(payer_user_id, payee_user_id)` — balance computation.
- `idx_settlements_transaction_id` on `transaction_id` — settlement-to-transaction lookup.

**Foreign keys**:
- `fk_settlements_transaction_id` → `transactions(id)` — cascade restricted (created per AD-7).

## Modified Tables

### `transactions` (existing)

Add one column:

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `is_split` | BOOLEAN | NOT NULL, DEFAULT FALSE | Whether this transaction has splits |

**Index**:
- `idx_transactions_is_split` on `is_split` — filter split vs. non-split transactions.

## Entity Model (Java)

### `SplitEntity` (new)

```
SplitEntity
├── id: UUID
├── transaction: TransactionEntity (ManyToOne, LAZY)
├── userId: UUID
├── amount: BigDecimal
├── splitType: SplitType (enum)
├── createdAt: Instant
└── updatedAt: Instant
```

**Notes**:
- `transaction` uses LAZY fetch to avoid N+1 queries in repository lookups.
- `amount` is `BigDecimal` (mapped to `NUMERIC(12,2)` column) — never `double`.
- `userId` is a plain `UUID` column (no `@ManyToOne` — AD-8).

### `SettlementEntity` (new)

```
SettlementEntity
├── id: UUID
├── transaction: TransactionEntity (ManyToOne, LAZY)  // AD-7: settlement creates transaction
├── payerUserId: UUID
├── payeeUserId: UUID
├── amount: BigDecimal
├── description: String
├── relatedSplitIds: List<UUID> (JSON)
├── createdAt: Instant
└── updatedAt: Instant
```

**Notes**:
- `relatedSplitIds` stored as JSON array via `@JdbcTypeCode(SqlTypes.JSON)` (Hibernate 6 convention).
- `transaction` is LAZY; created via `TransactionService` on settlement creation (AD-7).
- No `party` relationship — no party feature (AD-4).

### Domain Model Records (new)

#### `Split`

```java
record Split(
    UUID id,
    UUID transactionId,
    UUID userId,       // participant who owes this share
    BigDecimal amount,
    SplitType splitType, // EQUAL or CUSTOM
    Instant createdAt,
    Instant updatedAt
)
```

**Notes**:
- No `partyId` — no party feature (AD-4).
- `amount` is `BigDecimal` — financial precision (AD-6).

#### `Settlement`

```java
record Settlement(
    UUID id,
    UUID transactionId,  // AD-7: settlement creates transaction
    UUID payerUserId,
    UUID payeeUserId,
    BigDecimal amount,
    String description,
    List<UUID> relatedSplitIds,
    Instant createdAt,
    Instant updatedAt
)
```

**Notes**:
- No `partyId` — no party feature (AD-4).
- `amount` is `BigDecimal` — financial precision (AD-6).
- `transactionId` is nullable because the settlement record exists before the transaction is created (service-layer ordering).

#### `BalanceSummary` (derived, immutable)

```java
record BalanceSummary(
    UUID fromUserId,    // who owes
    UUID toUserId,      // who is owed
    double netAmount    // positive = from owes to; negative = from is owed by to
)
```

#### `PartyBalance` (derived, aggregate)

```java
record PartyBalance(
    UUID userId,
    Map<UUID, Double> balances  // userId → netAmount with each other member
)
```

## Relationships

```
transactions (existing)
  ├── is_split (new column)
  │
  └──[1:N]── splits (new)
                ├── transaction_id → transactions(id)
                ├── user_id (UUID, plain — AD-8, no FK)
                ├── amount (BigDecimal)
                └── split_type (EQUAL | CUSTOM)

settlements (new)
  ├──[N:1]── transactions (existing) via transaction_id (AD-7)
  ├── payer_user_id (UUID, plain)
  ├── payee_user_id (UUID, plain)
  ├── amount (BigDecimal)
  ├── related_split_ids (JSON array of UUIDs)
  └── description (TEXT, nullable)
```

## Balance Derivation

Balances are **computed**, not persisted. The formula:

```
balance(payer, payee) = Σ(splits where userId = payer) − Σ(settlements where payer = payer AND payee = payee)
```

Simplified: the transaction owner (identified via `TransactionEntity.addedBy`) is always the payee for split obligations. Non-owner participants owe the owner their share. Settlements flow from non-owning participants back to the owner.

## Exception Classes (new)

| Exception | When thrown | HTTP status |
|-----------|-------------|-------------|
| `TransactionNotFoundException` | Split creation references non-existent transaction | 404 |
| `SplitNotFoundException` | Patch/delete references non-existent split | 404 |
| `InvalidSplitException` | Sum of splits > transaction amount; invalid amount; invalid splitType | 400 |
| `InvalidSettlementException` | Settlement amount ≤ 0; invalid payer/payee pair | 400 |
| `InsufficientAccessException` | Player is not a valid participant (application-level validation) | 403 |

## History

| Date | Change |
|------|--------|
| 2026-09-01 | Initial data model draft |
| 2026-09-01 | Artifact consistency pass: Removed `party_id` from splits and settlements tables. Removed `party` relationships from entities and domain models. Fixed `amount` type from `double` to `BigDecimal`. Changed `SplitEntity.transaction` from EAGER to LAZY. Added AD-6, AD-7, AD-8 references. Added exception class table. Clarified balance derivation formula. |

## Constraints Summary

| Constraint | Table / Layer | Column(s) | Rule |
|------------|---------------|-----------|------|
| Unique split per transaction | `splits` | `(transaction_id, user_id)` | One split record per participant per transaction |
| Amount non-negative | `splits` | `amount` | ≥ 0 |
| Split type valid | `splits` | `split_type` | IN ('EQUAL', 'CUSTOM') |
| Settlement amount positive | `settlements` | `amount` | > 0 |
| Split total ≤ transaction amount | Service layer | — | Sum of splits for a transaction ≤ transaction amount |
| Participant is valid player | Service layer | `splits.user_id` | Must be a valid player identifier (application-level validation, AD-8) |
| Settlement parties are valid players | Service layer | `settlements.payer_user_id`, `settlements.payee_user_id` | Both must be valid player identifiers (application-level validation, AD-8) |
