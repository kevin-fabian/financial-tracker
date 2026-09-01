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
                          (Request DTO → CreateEqualSplitCommand)
                                ↓
                          SplitService.createEqualSplit()
                                ├─► Validate: transaction exists? → TransactionNotFoundException
                                ├─► Validate: participants list non-empty? → InvalidSplitException
                                ├─► Validate: sum of shares ≤ transaction amount? → InvalidSplitException
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
- `jwt.subject` (or downstream user service) → `Split.userId`
- `SplitType.EQUAL` → `Split.splitType`

**API**: `POST /api/splits/equal`

**Validation**:
- Transaction exists → `TransactionNotFoundException`
- At least one participant → `InvalidSplitException`
- Sum of shares ≤ transaction amount → `InvalidSplitException`
- No party membership validation (no party feature, AD-4)

### CF-2: Create Custom Split

```
User ──splits transaction──→ SplitController
                                ↓
                          (Request DTO → CreateCustomSplitCommand)
                                ↓
                          SplitService.createCustomSplit()
                                ├─► Validate: transaction exists? → TransactionNotFoundException
                                ├─► Validate: participants list non-empty? → InvalidSplitException
                                ├─► Validate: sum(custom amounts) ≤ transaction.amount? → InvalidSplitException
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
- `jwt.subject` (or downstream user service) → `Split.userId`
- `SplitType.CUSTOM` → `Split.splitType` (explicit amounts)
- `SplitType.EQUAL` → `Split.splitType` (auto-calculated remainder)

**API**: `POST /api/splits/custom`

**Validation**:
- Transaction exists → `TransactionNotFoundException`
- At least one participant → `InvalidSplitException`
- Sum of custom amounts ≤ transaction amount → `InvalidSplitException`

### CF-3: Create Settlement

```
User (payer) ──settles──→ SettlementController
                            ↓
                      (Request DTO → CreateSettlementCommand)
                            ↓
                      SettlementService.createSettlement()
                            ├─► Validate: payer user ID is valid
                            ├─► Validate: payee user ID is valid
                            ├─► Validate: amount > 0 → InvalidSettlementException
                            ├─► (Optional) Validate: amount ≤ outstanding balance
                            ├─► Create SettlementEntity
                            ├─► SettlementRepository.save()
                            ├─► Create SettlementTransaction (AD-7: settlement creates transaction)
                            │     ├─► Use payer's account (the account that paid)
                            │     ├─► Description: "Settlement: {settlement.description}"
                            │     └─► TransactionService.addTransaction()
                            └─► Link settlement to transaction
                            ↓
                      [settlement persisted, settlement transaction created]
```

**Field lineage**:
- `jwt.subject` → `Settlement.payerUserId` (extracted from authentication)
- `request.payeeUserId` → `Settlement.payeeUserId`
- `request.amount` → `Settlement.amount` (BigDecimal)
- `request.description` → `Settlement.description`
- `request.relatedSplitIds` → `Settlement.relatedSplitIds` (JSON array)
- `payer.accountId` → `SettlementTransaction.accountId` (AD-7: settlement creates transaction)

**API**: `POST /api/settlements`

### CF-4: Patch Split (Q5, Q14)

```
User ──PATCH /api/splits/{splitId}──→ SplitController
                                        ↓
                                    (Request DTO → PatchSplitCommand)
                                        ↓
                                    SplitService.patchSplit(splitId, command)
                                        ├─► Validate: split exists → SplitNotFoundException
                                        ├─► Validate: new amount ≥ 0 → InvalidSplitException
                                        ├─► Validate: sum of all splits for transaction still equals transaction amount → InvalidSplitException
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
- Only `amount` field can be patched. Changes to `userId`, `splitType`, or `transactionId` are rejected.
- If the patch would cause the total split amount to exceed the transaction amount, return `InvalidSplitException`.

## Query Flows

### QF-1: Get Splits for a Transaction

```
User ──GET /api/transactions/{transactionId}/splits──→ SplitController
                                                            ↓
                                                        SplitService.getSplitsByTransaction(transactionId, userId)
                                                            ↓
                                                        SplitRepository.findByTransactionId(transactionId)
                                                            ↓
                                                        [List<SplitSummary>]
```

**Projection** (`SplitSummary` — JPQL DTO projection):
- `splitId` ← `SplitEntity.id`
- `userId` ← `SplitEntity.userId`
- `userName` ← enriched via `UserClient` (service layer)
- `amount` ← `SplitEntity.amount`
- `splitType` ← `SplitEntity.splitType`

### QF-2: Get User Balances

```
User ──GET /api/splits/balances──→ SplitController
                                      ↓
                                  SplitService.getBalances(userId)
                                      ↓
                                  SplitRepository.findByUserId(userId)
                                      ↓
                                  [List<Split>]
                                      ↓
                                  SettlementRepository.findByPayerOrPayee(userId)
                                      ↓
                                  [List<Settlement>]
                                      ↓
                                  [Compute balances in service layer]
                                      ↓
                                  [List<BalanceSummary>]
```

**Balance computation** (service layer):
```
For each user pair (A, B) where A has splits or settlements with B:
  splitOwedByA = Σ(splits where userId = A)
  splitOwedByB = Σ(splits where userId = B)
  settledAtoB = Σ(settlements where payerUserId = A AND payeeUserId = B)
  settledBtoA = Σ(settlements where payerUserId = B AND payeeUserId = A)

  balance(A, B) = splitOwedByA - splitOwedByB - settledAtoB + settledBtoA
```

**Projection** (`BalanceSummary`):
- `fromUserId` ← computed participant ID
- `toUserId` ← computed participant ID
- `netAmount` ← computed balance (BigDecimal)

### QF-3: Get User's Outstanding Obligations

```
User ──GET /api/splits/obligations──→ SplitController
                                        ↓
                                    SplitService.getObligations(userId)
                                        ↓
                                    SplitRepository.findByUserIdAndNotSettled(userId)
                                        ↓
                                    [List<ObligationSummary>]
```

**ObligationSummary** (JPQL DTO projection):
- `transactionId` ← `SplitEntity.transactionId`
- `transactionDescription` ← `TransactionEntity.description`
- `transactionDate` ← `TransactionEntity.date`
- `amount` ← `SplitEntity.amount`
- `payeeUserId` ← `TransactionEntity.addedBy` (transaction owner)
- `payeeName` ← enriched via `UserClient` (service layer)

**Note**: Per Q12, payee is inferred from the transaction's `addedBy` field, not stored on the split. This requires a join `splits → transactions` in the query.

### QF-4: Get Settlement History

```
User ──GET /api/settlements──→ SettlementController
                                  ↓
                              SettlementService.getSettlements(userId)
                                  ↓
                              SettlementRepository.findByPayerOrPayee(userId)
                                  ↓
                              [List<SettlementSummary>]
```

**Projection** (`SettlementSummary` — JPQL DTO projection):
- `settlementId` ← `SettlementEntity.id`
- `payerUserId` ← `SettlementEntity.payerUserId`
- `payeeUserId` ← `SettlementEntity.payeeUserId`
- `amount` ← `SettlementEntity.amount`
- `description` ← `SettlementEntity.description`
- `relatedSplitIds` ← `SettlementEntity.relatedSplitIds`
- `createdAt` ← `SettlementEntity.createdAt`

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
                                        │ settlement transaction created (AD-7)
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
|------|-----------|------|------|
| Split creation | SplitService | Transaction exists | `TransactionNotFoundException` |
| Split creation | SplitService | Participants list non-empty | `InvalidSplitException` |
| Split creation | SplitService | Sum of custom amounts ≤ transaction amount | `InvalidSplitException` |
| Split creation | SplitService | At least one participant | `InvalidSplitException` |
| Settlement creation | SettlementService | Payer user ID is valid | `InvalidSettlementException` |
| Settlement creation | SettlementService | Payee user ID is valid | `InvalidSettlementException` |
| Settlement creation | SettlementService | Amount > 0 | `InvalidSettlementException` |
| Split patch (Q5) | SplitService | Split exists | `SplitNotFoundException` |
| Split patch (Q5) | SplitService | New amount ≥ 0 | `InvalidSplitException` |
| Split patch (Q5) | SplitService | Sum of all splits for transaction still equals transaction amount | `InvalidSplitException` |

## Record Preview

### Domain Model Records

#### `Split`

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "transactionId": "11111111-2222-3333-4444-555555555555",
  "userId": "bbbbbbbb-cccc-dddd-eeee-ffffffffffff",
  "amount": 30.00,
  "splitType": "EQUAL",
  "createdAt": "2026-09-01T10:30:00Z",
  "updatedAt": null
}
```

#### `Settlement`

```json
{
  "id": "d4e5f6a7-b8c9-0123-defa-123456789abc",
  "transactionId": "11111111-2222-3333-4444-555555555555",
  "payerUserId": "bbbbbbbb-cccc-dddd-eeee-ffffffffffff",
  "payeeUserId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "amount": 30.00,
  "description": "Dinner split — Bob's share",
  "relatedSplitIds": [
    "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
  ],
  "createdAt": "2026-09-01T18:00:00Z",
  "updatedAt": null
}
```

### Query Projection Records

#### `SplitSummary`

```json
{
  "splitId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "userId": "bbbbbbbb-cccc-dddd-eeee-ffffffffffff",
  "userName": "Bob",
  "amount": 30.00,
  "splitType": "EQUAL"
}
```

#### `BalanceSummary`

```json
{
  "fromUserId": "bbbbbbbb-cccc-dddd-eeee-ffffffffffff",
  "toUserId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "netAmount": 0.00
}
```

#### `ObligationSummary`

```json
{
  "transactionId": "11111111-2222-3333-4444-555555555555",
  "transactionDescription": "Dinner at Mario's",
  "transactionDate": "2026-09-01T12:00:00Z",
  "amount": 30.00,
  "payeeUserId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "payeeName": "Alice"
}
```

#### `SettlementSummary`

```json
{
  "settlementId": "d4e5f6a7-b8c9-0123-defa-123456789abc",
  "payerUserId": "bbbbbbbb-cccc-dddd-eeee-ffffffffffff",
  "payeeUserId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "amount": 30.00,
  "description": "Dinner split — Bob's share",
  "relatedSplitIds": [
    "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
  ],
  "createdAt": "2026-09-01T18:00:00Z"
}
```

## History

| Date | Change |
|------|--------|
| 2026-09-01 | Initial data flow draft |
| 2026-09-01 | CF-3 updated: settlement creates transaction flow (Q4 decision). Added CF-4 (Patch Split) for Q5 decision. Added constraints section to CF-4 (Q14: only amount field can be patched). |
| 2026-09-01 | Artifact consistency pass: Removed all party_id references from API paths. Changed paths from `/api/parties/{partyId}/...` to `/api/splits/...` and `/api/settlements/...`. Removed party membership validation from CF-1, CF-2, CF-4. Removed `party.id` from field lineage. Updated QF-2 from "Get Party Balances" to "Get User Balances" (no party feature). Updated QF-4 from party-scoped to user-scoped. Updated validation points table to remove party checks. |
| 2026-09-01 | Naming pass: Replaced all `playerId`/`payerPlayerId`/`payeePlayerId` with `userId`/`payerUserId`/`payeeUserId`. Replaced "party-agnostic" with "no party feature". Updated repository method names (`findByPlayerId` → `findByUserId`). Updated projection field names. Updated validation point descriptions. |
