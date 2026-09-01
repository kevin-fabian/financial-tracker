# Bill Split Architecture

## Overview

Bill Split enables users to split transactions across multiple participants and settle outstanding obligations. A single transaction (e.g., a dinner paid by one person) is allocated across participants via **splits**, and each participant's net obligation is tracked through **settlements**.

This feature does **not involve the party system**. Participants are identified by `user_id` (plain UUIDs from JWT `sub` claims or downstream user service); no party membership validation is performed.

## Primary Resource

**Split** — an allocation of a transaction's cost across participants.

| Attribute | Detail |
|-----------|--------|
| Purpose | Record how much each participant owes for a shared transaction |
| Source of truth | `splits` database table |
| Inputs | Transaction reference, participant (userId), share amount, split type |
| Outputs | Per-member obligation, aggregate balance, settlement targets |
| Business rules | Sum of shares ≤ transaction amount; at least one participant required |
| Relationships | References one `Transaction`; each participant has zero or more splits |

## Supporting Resources

### Settlement

| Attribute | Detail |
|-----------|--------|
| Purpose | Record when one participant pays another to reduce their obligation |
| Source of truth | `settlements` database table |
| Inputs | Payer (userId), payee (userId), amount, optional description, optional related split IDs |
| Outputs | Updated net balance between payer and payee |
| Business rules | amount > 0; settlement reduces the payer's net obligation to the payee |
| Relationships | References two `UserId`s (payer, payee); optionally references splits; optionally references a transaction (Q4) |

### SplitTransaction (extension of Transaction)

| Attribute | Detail |
|-----------|--------|
| Purpose | Mark a transaction as split and link to its splits |
| Source of truth | Existing `transactions` table (new column) + `splits` table |
| Inputs | `is_split` flag on transaction; split records in `splits` table |
| Outputs | Transaction participates in split calculations |
| Business rules | A split transaction must have at least one split record; total split amount ≤ transaction amount |
| Relationships | One-to-many with `splits` |

### Balance (derived, not persisted)

| Attribute | Detail |
|-----------|--------|
| Purpose | Show net amount one member owes or is owed by another |
| Source of truth | Computed from `splits` and `settlements` tables |
| Calculation | `balance(payer, payee) = sum(splits where payer is participant) - sum(settlements where payer paid payee)` |
| Business rules | Positive = payer is owed money; Negative = payer owes money |
| Relationships | Derived; no persistence |

## Architecture Decisions

### AD-1: Separate `splits` table vs. JSON in transactions

**Decision**: Create a dedicated `splits` table.

**Why**:
- Querying per-member obligations requires row-level access (filter by participant, sum by player).
- Settlements reference individual splits — relational foreign keys are cleaner than JSON array lookups.
- Future extensibility: partial settlements, split status transitions, split adjustments.

**Alternatives considered**:
- JSON column in `transactions` for split array — rejected: harder to query, no FK integrity.
- Denormalized columns on `transactions` (e.g., `split_count`, `split_total`) — rejected: adds complexity without benefit.

### AD-2: Settlements as first-class entities

**Decision**: Persist every settlement as a `settlements` row.

**Why**:
- Audit trail: users need to see when/why money changed hands.
- Reconciliation: settlements can reference specific splits for transparency.
- Balance computation: balances are derived from splits minus settlements, not stored.

**Alternatives considered**:
- On-the-fly balance adjustment without persistence — rejected: no audit trail, no reconciliation.

### AD-3: Split type — equal vs. custom amounts

**Decision**: Support both via a `SplitType` enum (`EQUAL`, `CUSTOM`).

**Why**:
- Equal splits are the most common case (e.g., dinner split 3 ways).
- Custom splits handle real-world scenarios (e.g., one person ordered steak, another had salad).
- When `EQUAL`, the system auto-calculates each participant's share from the transaction amount minus any explicitly specified custom shares.

**Trade-off**: Equal splits are a convenience layer on top of custom splits — the system still creates individual `splits` rows.

### AD-4: No party feature involvement

**Decision**: Splits and settlements do not involve the party system. No `party_id` columns. No party membership validation.

**Why**:
- Human decision: bill-split works independently of the party system.
- Participants are identified by `user_id` (plain UUIDs) directly.
- Splits reference transactions (which have an `addedBy` user) for payee identification.
- Settlements reference `user_id`s directly.

**How it works**:
- No `party_id` columns in `splits` or `settlements` tables.
- No party membership validation for split participants or settlement parties.
- Payee identification comes from the transaction's `addedBy` field (Q1 resolved).
- Participants are identified by `user_id` — plain UUIDs from JWT `sub` claims or downstream user service.

### AD-5: Transaction ownership vs. split ownership

**Decision**: The original transaction retains its existing `addedBy` owner. Splits are a separate concern.

**Why**:
- The transaction's payer (the account owner) is distinct from the split participants (who owe money).
- Example: Alice pays $100 at a restaurant (transaction owner = Alice). The split allocates $33.33 each to Alice, Bob, and Carol. Bob and Carol each owe Alice $33.33.
- Settlements flow from non-owning participants back to the transaction owner (or between any two participants).

## Use Cases

### UC-1: Split a transaction equally

1. User creates a transaction (e.g., $90 dinner).
2. User marks the transaction as split and selects participants to include.
3. System creates equal splits: $30 each for 3 participants (including the owner).
4. Non-owner participants now owe the owner their share.

### UC-2: Split a transaction with custom amounts

1. User creates a transaction ($100 grocery bill).
2. User manually assigns amounts: Owner $40, Bob $35, Carol $25.
3. System validates: sum ($100) ≤ transaction amount ($100).
4. Splits are created with custom amounts.

### UC-3: Settle an obligation

1. Bob views his outstanding balance (owes Alice $30).
2. Bob settles $30 with Alice.
3. System creates a settlement record: payer=Bob, payee=Alice, amount=$30.
4. Bob's balance with Alice is updated to $0.

### UC-4: View balances

1. User requests balance summary.
2. System computes net balance for each pair of participants.
3. Result: who owes whom, and how much.

### UC-5: View split history

1. User requests splits for a transaction.
2. System returns split records with participant, amount, and status.

## Boundaries

### In scope

- Split creation (equal and custom amounts).
- Split listing (by transaction, by participant).
- Split modification (adjust amounts before settlement).
- Settlement creation and listing.
- Balance computation (derived, not persisted).

### Out of scope (future)

- Partial settlements (settle only $10 of a $30 obligation).
- Split status transitions (e.g., pending → confirmed → settled) — deferred to v2 (Q11).
- Multi-currency splits (all amounts in the transaction's currency).
- Split reminders/notifications.
- External payment integration (Venmo, PayPal, etc.).
- Split approval workflow (no approval gate — splits are created directly).
- Party-scoped split/settlement queries.

## Source of Truth Summary

| Data | Owned by | Stored in |
|------|----------|-----------|
| Transaction | Account owner | `transactions` (existing) |
| Split allocation | Split creator | `splits` (new) |
| Settlement | Payer | `settlements` (new) |
| Balance | Derived | Computed from splits + settlements |

## Architecture Decisions (continued)

### AD-6: Split amount uses `BigDecimal` (not `double`)

**Decision**: All amount fields use `BigDecimal` via the project's `Amount` model convention.

**Why**: Floating-point `double` introduces rounding errors in financial calculations. The existing `Transaction` model uses `Amount` (backed by `BigDecimal`); splits and settlements must match.

**Alternatives considered**:
- `double` — rejected: rounding errors in currency arithmetic.
- `NUMERIC` mapped to `double` in JPA — rejected: same precision loss.

### AD-7: Settlement always creates a transaction

**Decision**: Every settlement row creates a corresponding `TransactionEntity` record (expense type, payer's account, description referencing the settlement).

**Why**:
- Audit trail: the transaction ledger must reflect all money movements.
- Balance consistency: the user's account balance decreases when they settle an obligation.
- Existing `TransactionService` handles the creation; `SettlementService` delegates to it.

**Trade-off**: Introduces a dependency from `SettlementService` → `TransactionService`. This is acceptable because settlement is a higher-level operation that composes transaction creation.

### AD-8: `user_id` is a plain UUID (no FK constraint)

**Decision**: `splits.user_id` and `settlements.payer_user_id` / `settlements.payee_user_id` are plain `UUID` columns with no database-level foreign key.

**Why**:
- No party feature: these UUIDs reference external identity providers (JWT `sub` claims, downstream user service).
- No `party_id` scope means no meaningful FK to `party_members`.
- Application-level validation ensures the UUIDs are valid user identifiers.

**Trade-off**: No database referential integrity. Relies on application validation and the downstream user service for identity resolution.

## History

| Date | Change |
|------|--------|
| 2026-09-01 | Initial architecture draft |
| 2026-09-01 | Review pass: 14 findings. AD-4 updated to reflect human decision (no party feature). Contradictions documented. Missing resources identified (SplitType enum, exception classes, SettlementEntity.updatedAt, settlement creates transaction). |
| 2026-09-01 | Validation pass: 3 blocking issues, 9 inconsistencies, 6 missing information, 5 unresolved decisions, 7 warnings. Party schema not in migrations confirmed. Settlement creates transaction impact documented. |
| 2026-09-01 | Q14 resolved: Only `amount` field can be patched. CF-4 (Patch Split) added to data-flow.md. Validation constraints documented. |
| 2026-09-01 | Analysis pass: Q2 resolved (remove party_id columns). Q11 resolved (defer to v2). Q12 resolved (keep current design). Q13 consolidated into Q4. AD-4 updated to remove all party references. Data model, data flows, use cases, and boundaries updated to reflect no-party design. |
| 2026-09-01 | Artifact consistency pass: Removed all party_id columns from data-model.md. Removed party membership validation from data flows. Fixed API paths to no-party. Added AD-6 (BigDecimal), AD-7 (settlement creates transaction), AD-8 (user_id plain UUID). Fixed entity amount type from double to BigDecimal. Clarified user_id semantics. |
| 2026-09-01 | Naming pass: Replaced all `playerId`/`payerPlayerId`/`payeePlayerId` with `userId`/`payerUserId`/`payeeUserId`. Replaced "party-agnostic" with "no party feature". Updated AD-4 title and description. |

## Implementation Readiness: NOT READY

Blocking issues:
1. `transactions` table needs `is_split` column (BOOLEAN, NOT NULL, DEFAULT FALSE) — not yet in V1.0.4 schema.
2. New tables (`splits`, `settlements`) require Liquibase migration (V1.0.5).

See open-questions.md for full status.
