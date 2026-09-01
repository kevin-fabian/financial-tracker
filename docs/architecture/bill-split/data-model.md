# Bill Split Data Model

## New Tables

### `splits`

Allocates a transaction's cost across party members.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PRIMARY KEY, `gen_random_uuid()`, NOT NULL | Split record identifier |
| `transaction_id` | UUID | NOT NULL, FK → `transactions(id)` | The transaction being split |
| `party_id` | UUID | NOT NULL, FK → `parties(id)` | Party scope (for referential integrity) |
| `player_id` | UUID | NOT NULL | The party member who owes this share |
| `amount` | NUMERIC(12, 2) | NOT NULL, CHECK ≥ 0 | The share amount this member owes |
| `split_type` | VARCHAR(16) | NOT NULL, CHECK IN ('EQUAL', 'CUSTOM') | How the share was calculated |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | When the split was created |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Last modification timestamp |

**Indexes**:
- `idx_splits_transaction_id` on `transaction_id` — fast lookup of splits for a transaction.
- `idx_splits_party_id` on `party_id` — party-scoped queries.
- `idx_splits_player_id` on `player_id` — participant's obligation queries.
- `idx_splits_transaction_player` on `(transaction_id, player_id)` — unique constraint guard.

**Unique constraint**:
- `uk_splits_transaction_player` on `(transaction_id, player_id)` — a participant has at most one split per transaction.

**Foreign keys**:
- `fk_splits_transaction_id` → `transactions(id)` — cascade restricted (split survives if transaction is patched, deleted via service layer).
- `fk_splits_party_id` → `parties(id)` — cascade restricted.

### `settlements`

Records when one party member pays another.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PRIMARY KEY, `gen_random_uuid()`, NOT NULL | Settlement record identifier |
| `party_id` | UUID | NULL, FK → `parties(id)` | Party scope (nullable — party agnostic per human decision) |
| `payer_player_id` | UUID | NOT NULL | The member who paid |
| `payee_player_id` | UUID | NOT NULL | The member who was paid |
| `amount` | NUMERIC(12, 2) | NOT NULL, CHECK > 0 | Settlement amount |
| `description` | TEXT | NULL | Optional memo |
| `related_split_ids` | JSON | NULL, default `[]` | References to specific splits being settled |
| `transaction_id` | UUID | NULL, FK → `transactions(id)` | Settlement transaction (created per Q4 decision) |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | When the settlement was recorded |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Last modification timestamp |

**Indexes**:
- `idx_settlements_party_id` on `party_id` — party-scoped queries (nullable column).
- `idx_settlements_payer_id` on `payer_player_id` — payer's outgoing settlements.
- `idx_settlements_payee_id` on `payee_player_id` — payee's incoming settlements.
- `idx_settlements_payer_payee` on `(payer_player_id, payee_player_id)` — balance computation.
- `idx_settlements_transaction_id` on `transaction_id` — settlement-to-transaction lookup.

**Foreign keys**:
- `fk_settlements_party_id` → `parties(id)` — cascade restricted.
- `fk_settlements_transaction_id` → `transactions(id)` — cascade restricted (created per Q4 decision).

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
├── transaction: TransactionEntity (ManyToOne, EAGER)
├── party: PartyEntity (ManyToOne, EAGER)
├── playerId: UUID
├── amount: double
├── splitType: SplitType (enum)
├── createdAt: Instant
└── updatedAt: Instant
```

### `SettlementEntity` (new)

```
SettlementEntity
├── id: UUID
├── party: PartyEntity (ManyToOne, LAZY)
├── transaction: TransactionEntity (ManyToOne, LAZY)  // Q4: settlement creates transaction
├── payerPlayerId: UUID
├── payeePlayerId: UUID
├── amount: double
├── description: String
├── relatedSplitIds: List<UUID> (JSON)  // Type correction: List<UUID> not List<String> per codebase convention
├── createdAt: Instant
└── updatedAt: Instant
```

### Domain Model Records (new)

#### `Split`

```java
record Split(
    UUID id,
    UUID transactionId,
    UUID partyId,
    UUID playerId,       // party member who owes
    double amount,
    SplitType splitType, // EQUAL or CUSTOM
    Instant createdAt,
    Instant updatedAt
)
```

#### `Settlement`

```java
record Settlement(
    UUID id,
    UUID partyId,
    UUID transactionId,  // Q4: settlement creates transaction
    UUID payerPlayerId,
    UUID payeePlayerId,
    double amount,
    String description,
    List<UUID> relatedSplitIds,  // Type correction: List<UUID> not List<String> per codebase convention
    Instant createdAt,
    Instant updatedAt
)
```

#### `BalanceSummary` (derived, immutable)

```java
record BalanceSummary(
    UUID fromPlayerId,    // who owes
    UUID toPlayerId,      // who is owed
    double netAmount      // positive = from owes to; negative = from is owed by to
)
```

#### `PartyBalance` (derived, aggregate)

```java
record PartyBalance(
    UUID playerId,
    Map<UUID, Double> balances  // playerId → netAmount with each other member
)
```

## Relationships

```
transactions (existing)
  ├── is_split (new column)
  │
  └──[1:N]── splits (new)
                ├── transaction_id → transactions(id)
                ├── party_id → parties(id)
                ├── player_id (UUID, not FK to users — references party_members.player_id)
                ├── amount
                └── split_type (EQUAL | CUSTOM)

parties (existing)
  ├──[1:N]── splits (new)
  │
  └──[1:N]── settlements (new)
                    ├── party_id → parties(id)
                    ├── payer_player_id (UUID)
                    └── payee_player_id (UUID)
```

## Balance Derivation

Net balance between two players is computed as:

```
balance(payer, payee) =
    (sum of splits where payer = player_id)
  - (sum of splits where payee = player_id)
  - (sum of settlements where payer paid payee)
  + (sum of settlements where payee paid payer)
```

Simplified: for each pair (A, B):

```
balance(A, B) = A's total obligations to B - B's total obligations to A - A's settlements to B + B's settlements to A
```

Where:
- "A's obligations to B" = sum of splits where A is a participant and B is the transaction owner (or more generally, the party member who fronted the cost).

**Simplification**: For the initial implementation, we assume the transaction owner (account holder) is always the payee. Splits create obligations from non-owner participants to the owner. Settlements then reduce these obligations.

## Constraints Summary

| Constraint | Table | Column(s) | Rule |
|------------|-------|-----------|------|
| Unique split per transaction | `splits` | `(transaction_id, player_id)` | One split record per participant per transaction |
| Amount non-negative | `splits` | `amount` | ≥ 0 |
| Split type valid | `splits` | `split_type` | IN ('EQUAL', 'CUSTOM') |
| Settlement amount positive | `settlements` | `amount` | > 0 |
| Split total ≤ transaction | enforced in service | — | Sum of splits for a transaction ≤ transaction amount |
| Participant is party member | enforced in service | `splits.player_id` | Must exist in `party_members` for the party |
| Settlement parties are members | enforced in service | `settlements.payer_player_id`, `payee_player_id` | Both must be party members of the same party |

## History

| Date | Change |
|------|--------|
| 2026-09-01 | Initial data model draft |
| 2026-09-01 | Review pass: Added `transaction_id` to settlements table (Q4: settlement creates transaction). Added `updated_at` to settlements table. Changed `party_id` to nullable (party agnostic). Fixed `relatedSplitIds` type from `List<String>` to `List<UUID>` per codebase convention. Added `transaction` relationship to SettlementEntity. |
| 2026-09-01 | Validation pass: Confirmed entity fetch strategy corrections (SplitEntity.transaction/party = EAGER, SettlementEntity.party = LAZY). Documented type mismatch for relatedSplitIds. |
