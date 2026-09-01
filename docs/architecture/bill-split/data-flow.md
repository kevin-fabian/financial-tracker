# Bill Split Data Flow

## Command Flows

### CF-1: Create Equal Split

```
User ──creates transaction──→ TransactionController
                                ↓
                          (Request DTO → AddTransactionCommand)
                                ↓
                          TransactionService.addTransaction()
                                ↓
                          TransactionRepository.save()
                                ↓
                          [transaction saved, is_split = false]
                                ↓
User ──splits transaction──→ SplitController
                                ↓
                          (Request DTO → CreateSplitCommand)
                                ↓
                          SplitService.createSplit()
                                ├─► Validate: transaction exists & belongs to user's party
                                ├─► Validate: all selected players are party members
                                ├─► Calculate equal share: amount / participantCount
                                ├─► For each participant:
                                │     └─► Create SplitEntity (splitType = EQUAL)
                                ├─► Update transaction: is_split = true
                                └─► SplitRepository.saveAll()
                                ↓
                          [splits persisted, transaction marked as split]
```

**Field lineage**:
- `transaction.amount` (from AddTransactionCommand) ÷ `participantCount` → `Split.amount`
- `transaction.id` → `Split.transactionId`
- `party.id` → `Split.partyId`
- `partyMember.playerId` → `Split.playerId`
- `SplitType.EQUAL` → `Split.splitType`

### CF-2: Create Custom Split

```
User ──splits transaction──→ SplitController
                                ↓
                          (Request DTO → CreateSplitCommand)
                                ↓
                          SplitService.createSplit()
                                ├─► Validate: transaction exists & belongs to user's party
                                ├─► Validate: all selected players are party members
                                ├─► Validate: sum(custom amounts) ≤ transaction.amount
                                ├─► For each participant with explicit amount:
                                │     └─► Create SplitEntity (splitType = CUSTOM)
                                ├─► For remaining participant (owner):
                                │     └─► Auto-calculate: transaction.amount - sum(explicit amounts)
                                │           Create SplitEntity (splitType = EQUAL, representing remainder)
                                ├─► Update transaction: is_split = true
                                └─► SplitRepository.saveAll()
```

**Field lineage**:
- `request.participants[].amount` → `Split.amount`
- `transaction.amount` - Σ(request.participants[].amount) → `Split.amount` (owner's remainder)
- `SplitType.CUSTOM` → `Split.splitType` (explicit amounts)
- `SplitType.EQUAL` → `Split.splitType` (auto-calculated remainder)

### CF-3: Create Settlement

```
User (payer) ──settles──→ SettlementController
                            ↓
                      (Request DTO → CreateSettlementCommand)
                            ↓
                      SettlementService.createSettlement()
                            ├─► Validate: payer player ID is valid
                            ├─► Validate: payee player ID is valid
                            ├─► Validate: amount > 0
                            ├─► (Optional) Validate: amount ≤ outstanding balance
                            ├─► Create SettlementEntity
                            ├─► SettlementRepository.save()
                            ├─► Create SettlementTransaction (Q4: settlement creates transaction)
                            │     ├─► Use payer's account (the account that paid)
                            │     ├─► Description: "Settlement: {settlement.description}"
                            │     └─► TransactionService.addTransaction()
                            └─► Link settlement to transaction
                            ↓
                      [settlement persisted, settlement transaction created]
```

**Field lineage**:
- `jwt.subject` → `Settlement.payerPlayerId` (extracted from authentication)
- `request.payeePlayerId` → `Settlement.payeePlayerId`
- `request.amount` → `Settlement.amount`
- `request.description` → `Settlement.description`
- `request.splitIds` → `Settlement.relatedSplitIds`
- `party.id` (from payer's party membership, nullable) → `Settlement.partyId`
- `payer.accountId` → `SettlementTransaction.accountId` (Q4: settlement creates transaction)

### CF-4: Patch Split (Q5)

```
User ──PATCH /api/parties/{partyId}/splits/{splitId}──→ SplitController
                                                          ↓
                                                      (Request DTO → PatchSplitCommand)
                                                          ↓
                                                      SplitService.patchSplit(splitId, command)
                                                          ├─► Validate: split exists
                                                          ├─► Validate: split belongs to user's party (if partyId provided)
                                                          ├─► Validate: new amount ≥ 0
                                                          ├─► Validate: sum of all splits for transaction still equals transaction amount
                                                          ├─► Update SplitEntity.amount = command.newAmount
                                                          ├─► Update SplitEntity.updatedAt = NOW()
                                                          └─► SplitRepository.save(splitEntity)
                                                          ↓
                                                      [split updated]
```

**Field lineage**:
- `splitId` → `SplitEntity.id` (lookup)
- `command.newAmount` → `SplitEntity.amount`
- `NOW()` → `SplitEntity.updatedAt`

**Constraints** (Q14):
- Only `amount` field can be patched. Changes to `playerId`, `splitType`, or `transactionId` are rejected.
- If the patch would cause the total split amount to exceed the transaction amount, return `InvalidSplitException`.

## Query Flows

### QF-1: Get Splits for a Transaction

```
User ──GET /api/parties/{partyId}/transactions/{transactionId}/splits──→ SplitController
                                                                          ↓
                                                                      SplitService.getSplitsByTransaction(transactionId, userId)
                                                                          ↓
                                                                      SplitRepository.findByTransactionId(transactionId)
                                                                          ↓
                                                                      [List<SplitSummary>]
```

**Projection** (SplitSummary):
- `splitId` ← `Split.id`
- `playerId` ← `Split.playerId`
- `playerName` ← enriched via `UserClient` (service layer)
- `amount` ← `Split.amount`
- `splitType` ← `Split.splitType`

### QF-2: Get Party Balances

```
User ──GET /api/parties/{partyId}/balances──→ SplitController
                                                ↓
                                            SplitService.getPartyBalances(partyId, userId)
                                                ↓
                                            SplitRepository.findByPartyId(partyId)
                                                ↓
                                            [List<Split>]
                                                ↓
                                            SettlementRepository.findByPartyId(partyId)
                                                ↓
                                            [List<Settlement>]
                                                ↓
                                            [Compute balances in service layer]
                                                ↓
                                            [List<BalanceSummary>]
```

**Balance computation** (service layer):
```
For each party member pair (A, B):
  splitOwedToA = Σ(splits where playerId = A)
  splitOwedToB = Σ(splits where playerId = B)
  settledAtoB = Σ(settlements where payer = A AND payee = B)
  settledBtoA = Σ(settlements where payer = B AND payee = A)

  balance(A, B) = splitOwedToA - splitOwedToB - settledAtoB + settledBtoA
```

### QF-3: Get User's Outstanding Obligations

```
User ──GET /api/splits/obligations──→ SplitController
                                        ↓
                                    SplitService.getObligations(userId)
                                        ↓
                                    SplitRepository.findByPlayerIdAndNotSettled(userId)
                                        ↓
                                    [List<ObligationSummary>]
```

**ObligationSummary**:
- `transactionId` ← `Split.transactionId`
- `transactionDescription` ← from TransactionEntity
- `transactionDate` ← from TransactionEntity
- `amount` ← `Split.amount`
- `payeePlayerId` ← transaction owner (from TransactionEntity.addedBy)
- `payeeName` ← enriched via `UserClient`

### QF-4: Get Settlement History

```
User ──GET /api/parties/{partyId}/settlements──→ SettlementController
                                                    ↓
                                                SettlementService.getSettlements(partyId, userId)
                                                    ↓
                                                SettlementRepository.findByPartyId(partyId)
                                                    ↓
                                                [List<SettlementSummary>]
```

## End-to-End Flow: Split + Settle

```
┌──────────┐     create txn      ┌──────────────┐
│  Alice   │────────────────────→│ Transaction  │
│  (owner) │                     │  Service     │
└──────────┘                     └──────────────┘
                                      │
                                      │ txn: $90, is_split=false
                                      ↓
┌──────────┐     split equally       ┌──────────────┐
│  Alice   │───────────────────────→│  Split       │
│  (owner) │  3 participants        │  Service     │
└──────────┘                        └──────────────┘
                                        │
                                        │ splits: Alice=$30, Bob=$30, Carol=$30
                                        │ obligations: Bob owes Alice $30,
                                        │                   Carol owes Alice $30
                                        ↓
┌──────────┐     settle $30          ┌──────────────┐
│   Bob    │───────────────────────→ │ Settlement   │
│          │                         │  Service     │
└──────────┘                         └──────────────┘
                                        │
                                        │ settlement: Bob→Alice $30
                                        │ Bob's balance with Alice: $0
                                        ↓
┌──────────┐     view balances       ┌──────────────┐
│  Alice   │───────────────────────→ │ Split        │
│          │                         │  Service     │
└──────────┘                         └──────────────┘
                                        │
                                        │ balances:
                                        │   Bob ↔ Alice: $0 (settled)
                                        │   Carol ↔ Alice: $30 (Carol owes)
                                        │   Bob ↔ Carol: $0 (no interaction)
                                        └──────────────┘
```

## Data Validation Points

| Step | Validator | Rule | Error |
|------|-----------|------|-------|
| Split creation | SplitService | Transaction exists | `TransactionNotFoundException` |
| Split creation | SplitService | User is party member | `InsufficientAccessException` |
| Split creation | SplitService | Participants are party members | `InvalidSplitException` |
| Split creation | SplitService | Sum of custom amounts ≤ transaction amount | `InvalidSplitException` |
| Split creation | SplitService | At least one participant | `InvalidSplitException` |
| Settlement creation | SettlementService | Payer is party member | `InsufficientAccessException` |
| Settlement creation | SettlementService | Payee is party member (same party) | `InvalidSettlementException` |
| Settlement creation | SettlementService | Amount > 0 | `InvalidSettlementException` |
| Split patch (Q5) | SplitService | Split exists | `SplitNotFoundException` |
| Split patch (Q5) | SplitService | New amount ≥ 0 | `InvalidSplitException` |
| Split patch (Q5) | SplitService | Sum of all splits for transaction still equals transaction amount | `InvalidSplitException` |

## History

| Date | Change |
|------|--------|
| 2026-09-01 | Initial data flow draft |
| 2026-09-01 | CF-3 updated: settlement creates transaction flow (Q4 decision). Added CF-4 (Patch Split) for Q5 decision. Added constraints section to CF-4 (Q14: only amount field can be patched). |
