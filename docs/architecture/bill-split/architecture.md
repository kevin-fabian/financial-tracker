# Bill Split Architecture

## Overview

Bill Split enables party members to split transactions across multiple users and settle outstanding obligations. A single transaction (e.g., a dinner paid by one person) is allocated across party members via **splits**, and each member's net obligation is tracked through **settlements**.

## Primary Resource

**Split** — an allocation of a transaction's cost across party members.

| Attribute | Detail |
|-----------|--------|
| Purpose | Record how much each party member owes for a shared transaction |
| Source of truth | `splits` database table |
| Inputs | Transaction reference, participant (playerId), share amount, split type |
| Outputs | Per-member obligation, aggregate balance, settlement targets |
| Business rules | Sum of shares ≤ transaction amount; participant must be a party member; owner auto-included if not explicitly split |
| Relationships | Belongs to one `Party`; references one `Transaction`; each participant has zero or more splits |

## Supporting Resources

### Settlement

| Attribute | Detail |
|-----------|--------|
| Purpose | Record when one party member pays another to reduce their obligation |
| Source of truth | `settlements` database table |
| Inputs | Payer (playerId), payee (playerId), amount, optional description, optional related split IDs |
| Outputs | Updated net balance between payer and payee |
| Business rules | Both must be party members; amount > 0; settlement reduces the payer's net obligation to the payee |
| Relationships | Belongs to one `Party`; references two `PlayerId`s (payer, payee); optionally references splits |

### SplitTransaction (extension of Transaction)

| Attribute | Detail |
|-----------|--------|
| Purpose | Mark a transaction as split and link to its splits |
| Source of truth | Existing `transactions` table (new column) + `splits` table |
| Inputs | `is_split` flag on transaction; split records in `splits` table |
| Outputs | Transaction participates in split calculations |
| Business rules | A split transaction must have at least one split record; total split amount ≤ transaction amount |
| Relationships | One-to-many with `splits`; belongs to one `Party` (inferred from first split's party) |

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

### AD-4: Party boundary

**Decision**: Splits and settlements are party-agnostic (not scoped to a `Party`).

**Why**:
- Human decision: bill-split is party agnostic. The feature works without requiring the party schema.
- The `party_id` columns exist in the data model for future party-scoped queries, but are not required for core functionality.
- Splits reference transactions (which have an `addedBy` user), not party memberships.
- Settlements reference player IDs directly.

**Contradiction note**: AD-4 as originally written stated splits/settlements are scoped to a Party. This contradicts the human decision that bill-split is party agnostic. The data model (party_id columns), data flows (party-scoped queries), and use cases (UC-3 party membership validation) all reference parties, but the human decision is to make bill-split work without requiring the party schema.

**How it works**:
- `party_id` columns exist in `splits` and `settlements` tables for referential integrity but are nullable.
- Split participants are validated against party membership only if a party_id is provided.
- Settlements can be created between any two player IDs (party membership validation is optional).
- When party schema migrations (V1.0.5) are created, party-scoped queries can be enabled.

### AD-5: Transaction ownership vs. split ownership

**Decision**: The original transaction retains its existing `addedBy` owner. Splits are a separate concern.

**Why**:
- The transaction's payer (the account owner) is distinct from the split participants (who owe money).
- Example: Alice pays $100 at a restaurant (transaction owner = Alice). The split allocates $33.33 each to Alice, Bob, and Carol. Bob and Carol each owe Alice $33.33.
- Settlements flow from non-owning participants back to the transaction owner (or between any two party members).

## Use Cases

### UC-1: Split a transaction equally

1. User creates a transaction (e.g., $90 dinner).
2. User marks the transaction as split and selects party members to include.
3. System creates equal splits: $30 each for 3 members (including the owner).
4. Non-owner members now owe the owner their share.

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

### UC-4: View party balances

1. User requests party balance summary.
2. System computes net balance for each pair of party members.
3. Result: who owes whom, and how much.

### UC-5: View split history

1. User requests splits for a transaction or for the party.
2. System returns split records with participant, amount, and status.

## Boundaries

### In scope

- Split creation (equal and custom amounts).
- Split listing (by transaction, by party, by participant).
- Split modification (adjust amounts before settlement).
- Settlement creation and listing.
- Balance computation (derived, not persisted).
- Party-scoped split/settlement queries.

### Out of scope (future)

- Partial settlements (settle only $10 of a $30 obligation).
- Split status transitions (e.g., pending → confirmed → settled).
- Multi-currency splits (all amounts in the transaction's currency).
- Split reminders/notifications.
- External payment integration (Venmo, PayPal, etc.).
- Split approval workflow (no approval gate — splits are created directly).

## Source of Truth Summary

| Data | Owned by | Stored in |
|------|----------|-----------|
| Transaction | Account owner | `transactions` (existing) |
| Split allocation | Split creator | `splits` (new) |
| Settlement | Payer | `settlements` (new) |
| Balance | Derived | Computed from splits + settlements |
| Party membership | Party leader | `party_members` (existing) |

## History

| Date | Change |
|------|--------|
| 2026-09-01 | Initial architecture draft |
| 2026-09-01 | Review pass: 14 findings. AD-4 updated to reflect human decision (party agnostic). Contradictions documented. Missing resources identified (SplitType enum, exception classes, party schema migrations, SettlementEntity.updatedAt, settlement creates transaction). |
| 2026-09-01 | Validation pass: 3 blocking issues, 9 inconsistencies, 6 missing information, 5 unresolved decisions, 7 warnings. Party schema not in migrations confirmed. Settlement creates transaction impact documented. |
| 2026-09-01 | Q14 resolved: Only `amount` field can be patched. CF-4 (Patch Split) added to data-flow.md. Validation constraints documented. |

## Implementation Readiness: NOT READY

Blocking issues:
1. Party schema migrations (V1.0.5) not created — party_id columns reference non-existent tables.
2. Q4 decision: settlements create transactions — SettlementEntity needs `transaction_id` column, SettlementService needs transaction creation logic.
3. Q2 contradiction: AD-4 states party-scoped but human decision is party agnostic — data model and data flows still reference parties extensively.

See open-questions.md for full status.
