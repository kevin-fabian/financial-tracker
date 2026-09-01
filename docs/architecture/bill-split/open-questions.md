# Open Questions

## BLOCKING

### Q2. Party table schema not in migrations

**Context**
The `PartyEntity`, `PartyMemberEntity`, `SharedItemEntity`, and `InvitationEntity` JPA entities exist, but no Liquibase migrations create the `parties`, `party_members`, `shared_items`, or `invitations` tables.

The bill-split feature depends on parties existing. The party schema migrations must be created first, or bill-split cannot function.

**Options**
- A. Create party schema migrations (V1.0.5) before implementing bill-split.
- B. Implement bill-split with in-memory party stubs and add migrations later.

**AI Recommendation**
A, because in-memory stubs break integration tests and prevent end-to-end validation.

**Decision**
Bill-split is not depending on the party feature. Bill-split is party agnostic.
Bill-split does not depend on parties.
AD-4, the data model (party_id columns), data flows (party-scoped queries), and use cases (UC-3 party membership validation) all reference parties, but the human decision is to make bill-split work without requiring the party schema.

**Contradiction**: AD-4 states "Splits and settlements are scoped to a Party" and "partyId is required." Q2 says "party agnostic." These are contradictory. The data model has `party_id` columns on both `splits` and `settlements` tables. The data flows reference `partyId` in every query. The use cases reference party membership validation.

**Resolution required**: Either (a) remove all party_id columns and party-scoped queries from the bill-split design, or (b) accept that bill-split implicitly depends on parties and create V1.0.5 party schema migrations. The human decision (party agnostic) contradicts the entire architecture.

**Status**
BLOCKING — Contradiction between AD-4 and Q2 must be resolved.

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
- `related_split_ids` type mismatch: data-model.md declares it as `List<String>` (JSON), but the codebase convention uses `List<UUID>` for IDs.

**Status**
BLOCKING — Schema and service impact must be resolved before implementation.

## IMPORTANT

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

**Resolution required**: Add a new command flow (CF-4: Patch Split) to data-flow.md. Add `PATCH /api/parties/{partyId}/splits/{splitId}` endpoint to architecture.md. Implement `PatchSplitCommand` and `SplitService.patchSplit()` with validation that the new total still equals the transaction amount.

**Status**
IMPORTANT — Missing from data flows and API surface.

### Q11. Split status lifecycle

**Context**
Splits could have states: `PENDING` → `CONFIRMED` → `PARTIALLY_SETTLED` → `SETTLED`.

**Options**
- A. Include status lifecycle in v1.
- B. Out of scope for v1 — splits are created in `CONFIRMED` state; settlements tracked separately.

**AI Recommendation**
B, because status tracking adds schema complexity (status column, transitions, history) without adding core value in v1.

**Decision**
We could support this by adding status in `settlements` but please also let me know what's the impact.

**Impact analysis**:
- **Schema**: No change to `splits` table. Settlements already track which splits are settled via `related_split_ids`. A settlement with `amount` covering a split's full amount implicitly marks that split as settled.
- **Balance computation**: No change. Balances are derived from splits minus settlements regardless of split status.
- **Query flows**: QF-3 (obligations) already filters to unsettled splits by checking which splits have related settlements. Adding a `status` column would make this a simple index lookup instead of a join + aggregation.
- **API surface**: No new endpoints needed. Existing settlement creation implicitly updates split status.
- **Risk**: Low. The implicit status tracking via settlements is sufficient for v1. Adding explicit status would be a v2 optimization.

**Recommendation**: Defer to v2. For v1, rely on `related_split_ids` in settlements to track which splits have been settled. A split is "settled" when the sum of related settlement amounts equals the split amount.

**Status**
IMPORTANT — Impact documented, decision pending.

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

### Q14. Split mutability — which fields can change?

**Context**
Q5 decision says splits can be modified. But not all fields should be mutable:
- `amount`: Can change (user corrected the split).
- `playerId`: Should NOT change (changing the participant changes who owes what).
- `splitType`: Should NOT change after creation (EQUAL vs CUSTOM is a fundamental distinction).
- `transactionId`: Should NOT change (split belongs to one transaction).

**Decision**
A. Only `amount` can be patched. Changes to `playerId`, `splitType`, or `transactionId` are rejected. If the patch would cause the total split amount to exceed the transaction amount, return `InvalidSplitException`.

**Resolution**: Added CF-4 (Patch Split) to data-flow.md. Only `amount` field is mutable via `PATCH /api/parties/{partyId}/splits/{splitId}`. Validation ensures `newAmount ≥ 0` and `sum(all splits for transaction) = transaction amount`.

**Status**
RESOLVED

## New Questions (from review/validation)

### Q12. Payee identification for obligations

**Context**
QF-3 (ObligationSummary) references `payeePlayerId ← TransactionEntity.addedBy`. However, `TransactionEntity` has `addedBy` as a `UUID` (not a `User` entity reference). The `SplitEntity` does not have a `payeePlayerId` column — the payee is inferred from the transaction.

This creates a query complexity: to compute obligations, the service must join splits → transactions to get `addedBy`, then enrich via `UserClient`. This is workable but not optimal.

**Options**
- A. Keep current design (join splits → transactions for payee). Simpler schema, slightly more complex query.
- B. Add `payee_player_id` column to `splits` table. Redundant with transaction's `addedBy`, but makes obligation queries simpler.

**AI Recommendation**
A for v1. The join is straightforward (one additional FK), and redundancy adds maintenance burden. Consider B in v2 if obligation queries become a performance bottleneck.

**Status**
IMPORTANT — Design decision needed before implementation.

### Q13. Settlement creates transaction — schema impact

**Context**
Q4 decision says "each settlement creates a transaction." This means the `settlements` table needs a `transaction_id` column (FK to `transactions`), and `SettlementService.createSettlement()` must call `TransactionService.addTransaction()` (or equivalent) to create a settlement transaction.

**Impact**:
- `SettlementEntity` needs a `transaction: TransactionEntity` ManyToOne relationship.
- `Settlement` domain record needs a `transactionId` field.
- `related_split_ids` type mismatch: data-model.md declares it as `List<String>` (JSON), but the codebase convention uses `List<UUID>` for IDs.
- Settlement transaction must use the payer's account (the account that paid the obligation).
- Settlement transaction description should reference the original split(s) and settlement memo.

**Status**
BLOCKING — Must be resolved before implementation.

### Q12. Payee identification for obligations

**Context**
QF-3 (ObligationSummary) references `payeePlayerId ← TransactionEntity.addedBy`. However, `TransactionEntity` has `addedBy` as a `UUID` (not a `User` entity reference). The `SplitEntity` does not have a `payeePlayerId` column — the payee is inferred from the transaction.

This creates a query complexity: to compute obligations, the service must join splits → transactions to get `addedBy`, then enrich via `UserClient`. This is workable but not optimal.

**Options**
- A. Keep current design (join splits → transactions for payee). Simpler schema, slightly more complex query.
- B. Add `payee_player_id` column to `splits` table. Redundant with transaction's `addedBy`, but makes obligation queries simpler.

**AI Recommendation**
A for v1. The join is straightforward (one additional FK), and redundancy adds maintenance burden. Consider B in v2 if obligation queries become a performance bottleneck.

**Status**
IMPORTANT — Design decision needed before implementation.
