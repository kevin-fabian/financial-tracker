# Open Questions

## BLOCKING

### Q4. Settlement creates transaction — schema impact

**Context**
Bob owes Alice $30 but only pays $10 now.

**Options**
- A. Support partial settlements — a $10 settlement reduces Bob's outstanding balance to $20.
- B. Require full settlements only.

**AI Recommendation**
A, because the current model already supports this naturally (a settlement row with amount=$10). No schema change needed.

**Decision**
A, each settlement creates a transaction as well. This will deduct user balance automatically.

**Impact**:
- `SettlementEntity` needs a `transaction: TransactionEntity` ManyToOne relationship (FK to `transactions`).
- `Settlement` domain record needs a `transactionId` field.
- `SettlementService.createSettlement()` must call `TransactionService.addTransaction()` (or equivalent) to create a settlement transaction.
- Settlement transaction must use the payer's account (the account that paid the obligation).
- Settlement transaction description should reference the original split(s) and settlement memo.
- `related_split_ids` type: must be `List<UUID>` per codebase convention (data-model.md has a mismatch).
- `transactions` table needs `is_split` column (BOOLEAN, NOT NULL, DEFAULT FALSE) — not yet in V1.0.4 schema.

**AD-7**: Settlement always creates a transaction. Introduces dependency from `SettlementService` → `TransactionService`.

**Status**
BLOCKING — Schema and service impact must be resolved before implementation.

### Q15. Party schema migrations must exist before bill-split implementation

**Context**
The `PartyEntity`, `PartyMemberEntity`, `SharedItemEntity`, and `InvitationEntity` JPA entities exist, but no Liquibase migrations create the `parties`, `party_members`, `shared_items`, or `invitations` tables.

Bill-split is party-agnostic (AD-4) and does not require the party schema for its own tables. However, the service-layer validation that "participants are valid player identifiers" may need the party schema to resolve player IDs against `party_members.player_id` when splits are used within a party context.

**Options**
- A. Create party schema migrations (V1.0.5) before implementing bill-split.
- B. Implement bill-split with application-level UUID validation only (no party membership check).

**AI Recommendation**
B for v1 bill-split implementation. The party schema can be added later; bill-split works with plain UUIDs (AD-8).

**Decision**
B. Bill-split is party-agnostic. No party schema dependency.

**Status**
IMPORTANT — Party schema migrations are a separate concern. Bill-split can proceed with plain UUID validation.

## Resolved

### Q1. Who is the payee in a split?

**Context**
When Alice pays $90 at a restaurant and splits it 3 ways with Bob and Carol, who does Bob owe? Alice (the transaction owner/account holder)? Or does the system track obligations more generally?

**Decision**
A. Payee = transaction owner (account holder).

**Resolution**: The transaction's `addedBy` field identifies the payee. Splits create obligations from non-owner participants to the owner. Settlements flow from non-owning participants back to the transaction owner. This is reflected in QF-3 (ObligationSummary.payeePlayerId ← TransactionEntity.addedBy).

**Status**
RESOLVED

### Q2. Party table schema not in migrations — party-agnostic design

**Context**
The `PartyEntity`, `PartyMemberEntity`, `SharedItemEntity`, and `InvitationEntity` JPA entities exist, but no Liquibase migrations create the `parties`, `party_members`, `shared_items`, or `invitations` tables.

The bill-split feature was initially designed with party-scoped queries and `party_id` columns on `splits` and `settlements` tables.

**Decision**
Party-agnostic design (AD-4). Remove all `party_id` columns from `splits` and `settlements` tables. Remove party membership validation from command flows. Remove party-scoped queries. Participants are identified by plain UUIDs (AD-8).

**Resolution**: AD-4 updated to reflect party-agnostic design. `party_id` columns removed from data-model.md. API paths changed from `/api/parties/{partyId}/...` to `/api/splits/...` and `/api/settlements/...`. Party membership validation removed from CF-1, CF-2, CF-4. QF-2 changed from "Get Party Balances" to "Get User Balances".

**Status**
RESOLVED — All artifacts updated to reflect party-agnostic design.

### Q3. Should splits support partial splits?

**Context**
A $100 transaction where only 2 of 4 party members participated.

**Decision**
A. Support partial splits — only explicitly selected participants get split records.

**Resolution**: The data model already supports this. The unique constraint `(transaction_id, player_id)` means non-participants simply have no split record. The sum of split amounts must equal the transaction amount (not all party members need a record).

**Status**
RESOLVED

### Q5. Split modification after creation

**Context**
Alice creates a split but realizes she assigned the wrong amount to Bob.

**Options**
- A. Include `PatchSplitCommand` and `SplitService.patchSplit()` in initial implementation.
- B. Defer to a later iteration.

**AI Recommendation**
A, because split correction is a natural follow-up to split creation and the service-layer validation (total still equals transaction amount) is straightforward.

**Decision**
A.

**Resolution required**: Add a new command flow (CF-4: Patch Split) to data-flow.md. Add `PATCH /api/splits/{splitId}` endpoint to architecture.md. Implement `PatchSplitCommand` and `SplitService.patchSplit()` with validation that the new total still equals the transaction amount.

**Status**
RESOLVED — CF-4 added to data-flow.md. API path updated to party-agnostic.

### Q6. Can a transaction be un-split?

**Context**
Alice marks a transaction as split, then changes her mind.

**Decision**
A. Allow un-split only if no related settlements exist. Otherwise return an error.

**Resolution**: Add a `DeleteSplitCommand` and `SplitService.deleteSplit()` that checks for related settlements before removing splits and resetting `is_split = false` on the transaction.

**Status**
RESOLVED

### Q7. Multi-currency splits

**Context**
Party members use different currencies.

**Decision**
A. Out of scope for v1 — all splits must use the transaction's currency.

**Resolution**: No schema or model changes needed. Service-layer validation ensures all split amounts are in the transaction's currency.

**Status**
RESOLVED

### Q8. Split approval workflow

**Context**
Should split participants approve the split before it becomes binding?

**Decision**
B. Out of scope for v1.

**Resolution**: No schema or model changes needed. Splits are created directly by the transaction owner.

**Status**
RESOLVED

### Q9. Split reminders/notifications

**Context**
Automatically remind Bob that he owes Alice $30.

**Decision**
B. Out of scope for v1. Included for v2.

**Resolution**: No schema or model changes needed.

**Status**
RESOLVED

### Q10. External payment integration

**Context**
Link settlements to Venmo, PayPal, or bank transfers.

**Decision**
B. Out of scope for v1.

**Resolution**: No schema or model changes needed. Settlements are internal records only.

**Status**
RESOLVED

### Q11. Split status lifecycle

**Context**
Splits could have states: `PENDING` → `CONFIRMED` → `PARTIALLY_SETTLED` → `SETTLED`.

**Options**
- A. Include status lifecycle in v1.
- B. Out of scope for v1 — splits are created in `CONFIRMED` state; settlements tracked separately.

**AI Recommendation**
B, because status tracking adds schema complexity (status column, transitions, history) without adding core value in v1.

**Decision**
Proceed with `related_split_ids` approach (B). Defer explicit status lifecycle to v2.

**Impact analysis**:
- **Schema**: No change to `splits` table. Settlements already track which splits are settled via `related_split_ids`. A settlement with `amount` covering a split's full amount implicitly marks that split as settled.
- **Balance computation**: No change. Balances are derived from splits minus settlements regardless of split status.
- **Query flows**: QF-3 (obligations) already filters to unsettled splits by checking which splits have related settlements. Adding a `status` column would make this a simple index lookup instead of a join + aggregation.
- **API surface**: No new endpoints needed. Existing settlement creation implicitly updates split status.
- **Risk**: Low. The implicit status tracking via settlements is sufficient for v1. Adding explicit status would be a v2 optimization.

**Status**
RESOLVED — Deferred to v2.

### Q12. Payee identification for obligations

**Context**
QF-3 (ObligationSummary) references `payeePlayerId ← TransactionEntity.addedBy`. However, `TransactionEntity` has `addedBy` as a `UUID` (not a `User` entity reference). The `SplitEntity` does not have a `payeePlayerId` column — the payee is inferred from the transaction.

This creates a query complexity: to compute obligations, the service must join splits → transactions to get `addedBy`, then enrich via `UserClient`. This is workable but not optimal.

**Options**
- A. Keep current design (join splits → transactions for payee). Simpler schema, slightly more complex query.
- B. Add `payee_player_id` column to `splits` table. Redundant with transaction's `addedBy`, but makes obligation queries simpler.

**AI Recommendation**
A for v1. The join is straightforward (one additional FK), and redundancy adds maintenance burden. Consider B in v2 if obligation queries become a performance bottleneck.

**Decision**
A. Keep current design for v1.

**Status**
RESOLVED

### Q13. Settlement payee identification

**Context**
Settlements reference `payerPlayerId` and `payeePlayerId` directly. When a settlement is created, how is the payee determined? Is it always the transaction owner? Can a settlement be between two non-owner participants?

**Decision**
Settlements can be between any two valid player IDs. The transaction owner is the default payee for split obligations, but settlements are not restricted to owner↔non-owner pairs. This allows participants to settle obligations between themselves (e.g., Bob pays Carol directly if Carol fronted a shared expense).

**Status**
RESOLVED

### Q14. Split mutability — which fields can change?

**Context**
Q5 decision says splits can be modified. But not all fields should be mutable:
- `amount`: Can change (user corrected the split).
- `playerId`: Should NOT change (changing the participant changes who owes what).
- `splitType`: Should NOT change after creation (EQUAL vs CUSTOM is a fundamental distinction).
- `transactionId`: Should NOT change (split belongs to one transaction).

**Decision**
A. Only `amount` can be patched. Changes to `playerId`, `splitType`, or `transactionId` are rejected. If the patch would cause the total split amount to exceed the transaction amount, return `InvalidSplitException`.

**Resolution**: Added CF-4 (Patch Split) to data-flow.md. Only `amount` field is mutable via `PATCH /api/splits/{splitId}`. Validation ensures `newAmount ≥ 0` and `sum(all splits for transaction) = transaction amount`.

**Status**
RESOLVED

### Q13. Settlement payee identification

**Context**
Settlements reference `payerPlayerId` and `payeePlayerId` directly. When a settlement is created, how is the payee determined? Is it always the transaction owner? Can a settlement be between two non-owner participants?

**Decision**
Settlements can be between any two valid player IDs. The transaction owner is the default payee for split obligations, but settlements are not restricted to owner↔non-owner pairs. This allows participants to settle obligations between themselves (e.g., Bob pays Carol directly if Carol fronted a shared expense).

**Status**
RESOLVED

### Q14. Split mutability — which fields can change?

**Context**
Q5 decision says splits can be modified. But not all fields should be mutable:
- `amount`: Can change (user corrected the split).
- `playerId`: Should NOT change (changing the participant changes who owes what).
- `splitType`: Should NOT change after creation (EQUAL vs CUSTOM is a fundamental distinction).
- `transactionId`: Should NOT change (split belongs to one transaction).

**Decision**
A. Only `amount` can be patched. Changes to `playerId`, `splitType`, or `transactionId` are rejected. If the patch would cause the total split amount to exceed the transaction amount, return `InvalidSplitException`.

**Resolution**: Added CF-4 (Patch Split) to data-flow.md. Only `amount` field is mutable via `PATCH /api/splits/{splitId}`. Validation ensures `newAmount ≥ 0` and `sum(all splits for transaction) = transaction amount`.

**Status**
RESOLVED

## Resolved

### Q1. Who is the payee in a split?

**Context**
When Alice pays $90 at a restaurant and splits it 3 ways with Bob and Carol, who does Bob owe? Alice (the transaction owner/account holder)? Or does the system track obligations more generally?

**Decision**
A. Payee = transaction owner (account holder).

**Resolution**: The transaction's `addedBy` field identifies the payee. Splits create obligations from non-owner participants to the owner. Settlements flow from non-owning participants back to the transaction owner. This is reflected in QF-3 (ObligationSummary.payeePlayerId ← TransactionEntity.addedBy).

**Status**
RESOLVED

### Q3. Should splits support partial splits?

**Context**
A $100 transaction where only 2 of 4 party members participated.

**Decision**
A. Support partial splits — only explicitly selected participants get split records.

**Resolution**: The data model already supports this. The unique constraint `(transaction_id, player_id)` means non-participants simply have no split record. The sum of split amounts must equal the transaction amount (not all party members need a record).

**Status**
RESOLVED

### Q5. Split modification after creation

**Context**
Alice creates a split but realizes she assigned the wrong amount to Bob.

**Options**
- A. Include `PatchSplitCommand` and `SplitService.patchSplit()` in initial implementation.
- B. Defer to a later iteration.

**AI Recommendation**
A, because split correction is a natural follow-up to split creation and the service-layer validation (total still equals transaction amount) is straightforward.

**Decision**
A.

**Resolution required**: Add a new command flow (CF-4: Patch Split) to data-flow.md. Add `PATCH /api/splits/{splitId}` endpoint to architecture.md. Implement `PatchSplitCommand` and `SplitService.patchSplit()` with validation that the new total still equals the transaction amount.

**Status**
RESOLVED — CF-4 added to data-flow.md. API path updated to party-agnostic.

### Q6. Can a transaction be un-split?

**Context**
Alice marks a transaction as split, then changes her mind.

**Decision**
A. Allow un-split only if no related settlements exist. Otherwise return an error.

**Resolution**: Add a `DeleteSplitCommand` and `SplitService.deleteSplit()` that checks for related settlements before removing splits and resetting `is_split = false` on the transaction.

**Status**
RESOLVED

### Q7. Multi-currency splits

**Context**
Party members use different currencies.

**Decision**
A. Out of scope for v1 — all splits must use the transaction's currency.

**Resolution**: No schema or model changes needed. Service-layer validation ensures all split amounts are in the transaction's currency.

**Status**
RESOLVED

### Q8. Split approval workflow

**Context**
Should split participants approve the split before it becomes binding?

**Decision**
B. Out of scope for v1.

**Resolution**: No schema or model changes needed. Splits are created directly by the transaction owner.

**Status**
RESOLVED

### Q9. Split reminders/notifications

**Context**
Automatically remind Bob that he owes Alice $30.

**Decision**
B. Out of scope for v1. Included for v2.

**Resolution**: No schema or model changes needed.

**Status**
RESOLVED

### Q10. External payment integration

**Context**
Link settlements to Venmo, PayPal, or bank transfers.

**Decision**
B. Out of scope for v1.

**Resolution**: No schema or model changes needed. Settlements are internal records only.

**Status**
RESOLVED

### Q11. Split status lifecycle

**Context**
Splits could have states: `PENDING` → `CONFIRMED` → `PARTIALLY_SETTLED` → `SETTLED`.

**Options**
- A. Include status lifecycle in v1.
- B. Out of scope for v1 — splits are created in `CONFIRMED` state; settlements tracked separately.

**AI Recommendation**
B, because status tracking adds schema complexity (status column, transitions, history) without adding core value in v1.

**Decision**
Proceed with `related_split_ids` approach (B). Defer explicit status lifecycle to v2.

**Impact analysis**:
- **Schema**: No change to `splits` table. Settlements already track which splits are settled via `related_split_ids`. A settlement with `amount` covering a split's full amount implicitly marks that split as settled.
- **Balance computation**: No change. Balances are derived from splits minus settlements regardless of split status.
- **Query flows**: QF-3 (obligations) already filters to unsettled splits by checking which splits have related settlements. Adding a `status` column would make this a simple index lookup instead of a join + aggregation.
- **API surface**: No new endpoints needed. Existing settlement creation implicitly updates split status.
- **Risk**: Low. The implicit status tracking via settlements is sufficient for v1. Adding explicit status would be a v2 optimization.

**Status**
RESOLVED — Deferred to v2.

### Q12. Payee identification for obligations

**Context**
QF-3 (ObligationSummary) references `payeePlayerId ← TransactionEntity.addedBy`. However, `TransactionEntity` has `addedBy` as a `UUID` (not a `User` entity reference). The `SplitEntity` does not have a `payeePlayerId` column — the payee is inferred from the transaction.

This creates a query complexity: to compute obligations, the service must join splits → transactions to get `addedBy`, then enrich via `UserClient`. This is workable but not optimal.

**Options**
- A. Keep current design (join splits → transactions for payee). Simpler schema, slightly more complex query.
- B. Add `payee_player_id` column to `splits` table. Redundant with transaction's `addedBy`, but makes obligation queries simpler.

**AI Recommendation**
A for v1. The join is straightforward (one additional FK), and redundancy adds maintenance burden. Consider B in v2 if obligation queries become a performance bottleneck.

**Decision**
A. Keep current design for v1.

**Status**
RESOLVED

### Q13. Settlement payee identification

**Context**
Settlements reference `payerPlayerId` and `payeePlayerId` directly. When a settlement is created, how is the payee determined? Is it always the transaction owner? Can a settlement be between two non-owner participants?

**Decision**
Settlements can be between any two valid player IDs. The transaction owner is the default payee for split obligations, but settlements are not restricted to owner↔non-owner pairs. This allows participants to settle obligations between themselves (e.g., Bob pays Carol directly if Carol fronted a shared expense).

**Status**
RESOLVED

### Q14. Split mutability — which fields can change?

**Context**
Q5 decision says splits can be modified. But not all fields should be mutable:
- `amount`: Can change (user corrected the split).
- `playerId`: Should NOT change (changing the participant changes who owes what).
- `splitType`: Should NOT change after creation (EQUAL vs CUSTOM is a fundamental distinction).
- `transactionId`: Should NOT change (split belongs to one transaction).

**Decision**
A. Only `amount` can be patched. Changes to `playerId`, `splitType`, or `transactionId` are rejected. If the patch would cause the total split amount to exceed the transaction amount, return `InvalidSplitException`.

**Resolution**: Added CF-4 (Patch Split) to data-flow.md. Only `amount` field is mutable via `PATCH /api/splits/{splitId}`. Validation ensures `newAmount ≥ 0` and `sum(all splits for transaction) = transaction amount`.

**Status**
RESOLVED
