# Open Questions

## BLOCKING

### Q1. Who is the payee in a split?

**Context**
When Alice pays $90 at a restaurant and splits it 3 ways with Bob and Carol, who does Bob owe? Alice (the transaction owner/account holder)? Or does the system track obligations more generally?

The payee determination affects how `SplitEntity` is designed. If the payee is always the transaction owner, the model is simple. If splits can involve arbitrary payees (e.g., Bob paid $50 and Carol paid $40 on the same receipt), the model needs a `payeePlayerId` on `Split`.

**Options**
- A. Payee = transaction owner (account holder). Simplest, covers 90% of real-world cases.
- B. Payee = explicit field on split. More flexible, more complex.

**AI Recommendation**
A, unless multi-payer receipts are a common v1 requirement.

**Decision**
<!-- HUMAN: Add your decision here. -->
A.

**Status**
PENDING

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
<!-- HUMAN: Add your decision here. -->
Bill-split is not depending on the party feature. Bill-split is party agnostic.

**Status**
PENDING

## IMPORTANT

### Q3. Should splits support partial splits?

**Context**
A $100 transaction where only 2 of 4 party members participated (e.g., Alice and Bob went to dinner, Carol was not there).

If supported, the split amount sum must equal the transaction amount, but not all party members need a split record. Non-participants simply have no split (or a $0 split).

**Options**
- A. Support partial splits — only explicitly selected participants get split records.
- B. Require all party members to have a split record (even if $0).

**AI Recommendation**
A, because it matches real-world behavior and avoids cluttering the data model with zero-value records.

**Decision**
<!-- HUMAN: Add your decision here. -->
A.

**Status**
PENDING

### Q4. Should settlements support partial amounts?

**Context**
Bob owes Alice $30 but only pays $10 now.

**Options**
- A. Support partial settlements — a $10 settlement reduces Bob's outstanding balance to $20.
- B. Require full settlements only.

**AI Recommendation**
A, because the current model already supports this naturally (a settlement row with amount=$10). No schema change needed.

**Decision**
<!-- HUMAN: Add your decision here. -->
A, each settlement creates a transaction as well. This will deduct user balance automatically.

**Status**
PENDING

### Q5. Split modification after creation

**Context**
Alice creates a split but realizes she assigned the wrong amount to Bob.

**Options**
- A. Include `PatchSplitCommand` and `SplitService.patchSplit()` in initial implementation.
- B. Defer to a later iteration.

**AI Recommendation**
A, because split correction is a natural follow-up to split creation and the service-layer validation (total still equals transaction amount) is straightforward.

**Decision**
<!-- HUMAN: Add your decision here. -->
A.

**Status**
PENDING

### Q6. Can a transaction be un-split?

**Context**
Alice marks a transaction as split, then changes her mind.

**Options**
- A. Allow un-split only if no related settlements exist. Otherwise return an error.
- B. Never allow un-split once splits are created.
- C. Always allow un-split, even if settlements exist (breaks audit trail).

**AI Recommendation**
A, because it preserves audit integrity while allowing correction when no money has changed hands.

**Decision**
<!-- HUMAN: Add your decision here. -->
A.

**Status**
PENDING

### Q7. Multi-currency splits

**Context**
Party members use different currencies. Alice pays in USD, Bob owes in PHP.

**Options**
- A. Out of scope for v1 — all splits must use the transaction's currency.
- B. Support currency conversion at split creation time.

**AI Recommendation**
A, because currency conversion adds significant complexity (exchange rate source, rounding, historical rates) that is orthogonal to the core split/settle flow.

**Decision**
<!-- HUMAN: Add your decision here. -->
A.

**Status**
PENDING

## OPTIONAL

### Q8. Split approval workflow

**Context**
Should split participants approve the split before it becomes binding?

**Options**
- A. Include approval workflow in v1.
- B. Out of scope for v1 — splits are created directly by the transaction owner.

**AI Recommendation**
B, because approval adds a state machine and notification surface that delays core functionality.

**Decision**
<!-- HUMAN: Add your decision here. -->
B.

**Status**
PENDING

### Q9. Split reminders/notifications

**Context**
Automatically remind Bob that he owes Alice $30.

**Options**
- A. Include in v1.
- B. Out of scope for v1.

**AI Recommendation**
B, because reminders require a notification channel (email, push, in-app) that is a separate feature.

**Decision**
<!-- HUMAN: Add your decision here. -->
B, for now, this will be included for v2.

**Status**
PENDING

### Q10. External payment integration

**Context**
Link settlements to Venmo, PayPal, or bank transfers.

**Options**
- A. Include in v1.
- B. Out of scope for v1.

**AI Recommendation**
B, because external payment integration is a large surface area that should be a standalone feature.

**Decision**
<!-- HUMAN: Add your decision here. -->
B.

**Status**
PENDING

### Q11. Split status lifecycle

**Context**
Splits could have states: `PENDING` → `CONFIRMED` → `PARTIALLY_SETTLED` → `SETTLED`.

**Options**
- A. Include status lifecycle in v1.
- B. Out of scope for v1 — splits are created in `CONFIRMED` state; settlements tracked separately.

**AI Recommendation**
B, because status tracking adds schema complexity (status column, transitions, history) without adding core value in v1.

**Decision**
<!-- HUMAN: Add your decision here. -->
We could support this by adding status in `settlements` but please also let me know what's the impact.

**Status**
PENDING
