---
name: service-level-architecture
description: Analyze a business capability or primary resource and produce a data-first service architecture before implementation. Identify source-of-truth data, incoming data, persistence, relationships, transformations, aggregates, outputs, lineage, decisions, and unresolved questions.
---

# Service-Level Architecture

## Purpose

Design the service from an end-to-end data perspective before implementation.

The architecture must make this visible:

```text
Source
  ↓
Incoming Data
  ↓
Validation
  ↓
Transformation
  ↓
Persistence
  ↓
Aggregation / Derivation
  ↓
Output
```

Do not implement code during this analysis.

---

# Input

Input is one of:

* Business capability
* Primary resource
* Service
* Feature
* Use case

Example:

```text
Analyze Reporting.
Existing resources:
- Transactions
- Accounts
- Categories
- Budgets
- Recurring Transactions
```

Inspect existing requirements, code, schema, APIs, events, and tests when available.

Do not invent information.

Classify findings as:

```text
CONFIRMED
INFERRED
RECOMMENDED
UNKNOWN
```

---

# Core Principles

1. Start with the business capability or primary resource.
2. Discover supporting resources from actual dependencies.
3. Identify the **source of truth** for important data.
4. Distinguish source-of-truth, derived, cached, and denormalized data.
5. Explicitly identify what is persisted and what is not.
6. Trace important output fields back to their source.
7. Define every important aggregate and calculation.
8. Separate business decisions from implementation choices.
9. Surface ambiguity instead of silently deciding it.
10. Do not consider the architecture ready while blocking decisions remain unresolved.

---

# Analysis

## 1. Business Capability

Document:

* Purpose
* Scope
* Responsibilities
* Non-responsibilities

## 2. Primary Resource

Identify the central resource or concept.

Determine whether it is:

* Persisted
* Derived
* Aggregated
* Read-only
* External

Do not assume a conceptual resource requires a database table.

## 3. Supporting Resources

Identify resources involved in the capability.

For each resource, classify its role:

* Source
* Owner
* Reference
* Enrichment
* Aggregation source
* External dependency

Create a dependency graph.

```text
Account
   ↓
Transaction
   ↓
Category
```

For reporting:

```text
Transactions ──┐
Accounts ──────┤
Categories ────┤
Budgets ───────┤
Recurring Txns ┘
       ↓
   Reporting
```

## 4. Source of Truth

For important data, identify the authoritative owner.

| Data               | Source of Truth | Notes                 |
| ------------------ | --------------- | --------------------- |
| Transaction amount | Transaction     | Financial fact        |
| Category name      | Category        | Dimension             |
| Budget allocation  | Budget          | Planned value         |
| Report total       | Derived         | Not a source of truth |

Never treat a derived or cached value as authoritative without an explicit decision.

---

# 5. Use Cases

List relevant commands and queries.

For each use case identify:

```text
Input
  ↓
Validation
  ↓
Dependencies
  ↓
Transformation
  ↓
Persistence / Query
  ↓
Aggregation
  ↓
Output
```

Only include steps that actually apply.

---

# 6. Incoming Data

Document request/input fields.

```text
ReportQuery
├── from
├── to
├── accountId
└── categoryId
```

For important fields record:

| Field | Type | Required | Source | Notes |
| ----- | ---- | -------: | ------ | ----- |

Identify:

* Required/optional fields
* Defaults
* System-generated values
* Validation requirements

---

# 7. Data Transformation

Document meaningful transformations.

Common types:

```text
Pass-through
Rename
Normalize
Validate
Enrich
Derive
Aggregate
```

Example:

```text
Transaction.amount
      ↓
filter EXPENSE
      ↓
SUM(amount)
      ↓
Report.totalExpense
```

Do not document trivial mappings unless they affect architecture.

---

# 8. Persistence

Explicitly answer:

> What data is stored?

Document the persistence model.

```text
Transaction
├── id
├── account_id
├── category_id
├── amount
├── type
├── transaction_date
└── created_at
```

For important fields:

| Field        | Persist? | Source       | Reason          |
| ------------ | -------: | ------------ | --------------- |
| amount       |      Yes | Request      | Source of truth |
| categoryName |       No | Category     | Owned elsewhere |
| totalExpense |       No | Transactions | Derived         |

Identify:

* Primary keys
* Foreign keys
* Nullability
* Unique constraints
* Important indexes
* Delete/update behavior

Do not create persistence merely because a value appears in an output.

---

# 9. Relationships

Document important relationships and ownership.

```text
Account 1 ─── * Transaction
Category 1 ── * Transaction
Budget 1 ──── 1 Category
```

Distinguish:

* Ownership
* Reference
* Dependency
* Aggregation

Do not infer relationships solely from similar fields.

---

# 10. Aggregates and Derived Data

List important calculated fields.

| Field           | Sources               | Calculation        | Stored? |
| --------------- | --------------------- | ------------------ | ------: |
| totalIncome     | Transactions          | SUM(income)        |      No |
| totalExpense    | Transactions          | SUM(expense)       |      No |
| netCashFlow     | Income + Expense      | income - expense   |      No |
| budgetRemaining | Budget + Transactions | allocated - actual |      No |

Every important aggregate must identify:

* Source data
* Filters
* Grouping
* Calculation
* Persistence strategy

---

# 11. Output

Document the output model.

```json
{
  "totalIncome": 50000,
  "totalExpense": 32000,
  "netCashFlow": 18000
}
```

For important output fields:

| Output | Source | Derived? | Notes |
| ------ | ------ | -------: | ----- |

Do not return persistence models directly unless that is an explicit architectural decision.

---

# 12. Data Lineage

Trace important data end-to-end.

```text
Transaction.amount
      ↓
Expense filter
      ↓
Date filter
      ↓
SUM
      ↓
Report.totalExpense
      ↓
API response
```

Lineage should connect:

```text
Input → Source → Persistence → Transformation → Aggregate → Output
```

Not every trivial field requires a full lineage diagram.

Prioritize:

* Financial values
* IDs
* Dates
* Statuses
* Aggregates
* Calculated fields

---

# 13. End-to-End Data Flow

Provide one concise diagram showing the complete flow.

Example:

```text
Client
  │
  │ ReportQuery
  ▼
Reporting
  │
  ├──► Transactions
  ├──► Accounts
  ├──► Categories
  └──► Budgets
          │
          ▼
     Filter / Join
          │
          ▼
      Aggregate
          │
          ▼
       Project
          │
          ▼
     ReportResponse
```

The diagram should emphasize **data movement**, not merely service names.

---

# 14. Business Rules and Errors

Document important rules only.

Examples:

```text
Account must belong to the current user.
Transaction amount must be positive.
Category must exist.
```

Document significant failure paths:

```text
Invalid input → Validation error
Unauthorized → Authorization error
Missing resource → Not found
Business conflict → Conflict
Persistence failure → Error / rollback
```

Do not enumerate generic framework errors.

---

# 15. Architecture Decisions

Record decisions that materially affect the architecture.

```text
Decision:
Reporting does not persist report results.

Reason:
Reports are derived from existing financial data.

Alternative:
Create a reporting read model if query performance
becomes insufficient.
```

Include:

* Decision
* Reason
* Important alternatives
* Impact

---

# 16. Open Questions

Explicitly list unresolved decisions.

```text
BLOCKING
- Is RecurringTransaction a template or actual transaction?

IMPORTANT
- Is account balance persisted or calculated?

OPTIONAL
- Should reports support custom fiscal periods?
```

Never silently resolve ambiguous business semantics.

---

# 17. Architecture History

Maintain meaningful architectural changes.

```text
## Architecture History

| Date | Change | Reason |
|---|---|---|
| 2026-09-01 | Reports are not persisted | Derived data |
| 2026-09-01 | Recurring transactions treated as templates | Business clarification |
```

Record architectural changes, not formatting or typo fixes.

When changing a previous decision, preserve the reason and impact.

---

# 18. Review and Validation

The generated architecture is initially:

```text
Status: DRAFT
```

Human review should verify:

* Business interpretation
* Resource relationships
* Source of truth
* Persistence
* Aggregations
* Output
* Data lineage
* Business rules
* Architectural decisions

After human changes, perform a validation pass.

Validation must check:

```text
Requirements
    ↓
Resources
    ↓
Source of Truth
    ↓
Persistence
    ↓
Relationships
    ↓
Transformations
    ↓
Aggregations
    ↓
Outputs
    ↓
Lineage
```

Report:

```text
PASS
```

or:

```text
FAIL

Blocking issues:
- ...

Inconsistencies:
- ...

Missing decisions:
- ...
```

Do not silently modify the architecture to make validation pass.

---

# 19. Implementation Readiness

End with:

```text
## Implementation Readiness

Status: READY | NOT READY

Confirmed:
- ...

Blocking:
- ...

Important:
- ...
```

`READY` requires:

* Primary resource identified
* Supporting resources identified
* Source of truth established
* Persistence understood
* Relationships understood
* Aggregations defined
* Outputs defined
* Important lineage established
* Business rules established
* No blocking questions

---

# Required Artifact

Create:

```text
docs/architecture/<capability>/architecture.md
```

Keep the architecture in one file by default.

Only create additional artifacts when necessary:

```text
architecture.md
data-model.md
data-flow.md
```

Do not create unnecessary documentation.

---

# Architecture Lifecycle

Use this cycle:

```text
Requirements
     ↓
AI Analysis
     ↓
architecture.md
     ↓
Human Review
     ↓
Human Decisions / Corrections
     ↓
AI Validation
     ↓
   ┌───────┐
   │ PASS  │──────► APPROVED
   └───────┘
       │
      FAIL
       ↓
Resolve Issues
       ↓
AI Validation
```

After approval:

```text
Approved Architecture
        ↓
Specification
        ↓
Implementation
        ↓
Tests
```

Keep architecture and specification separate.

---

# Rules

* Analyze before implementing.
* Start from the capability or primary resource.
* Identify supporting resources from real dependencies.
* Identify source of truth.
* Explicitly identify persisted vs derived data.
* Define important aggregates.
* Trace important output fields to their source.
* Distinguish ownership from reference.
* Distinguish source-of-truth, derived, cached, and denormalized data.
* Do not invent business rules or relationships.
* Mark uncertain information as `UNKNOWN`.
* Separate confirmed facts, inferences, and recommendations.
* Surface blocking decisions.
* Preserve architecture history.
* Validate after significant changes.
* Do not silently resolve contradictions.
* Do not generate implementation code.
* Prefer concise diagrams and tables over repetitive prose.
* Keep architecture focused on data, structure, decisions, and flow.
* Do not turn the architecture artifact into a behavioral specification.

---

# Completion Test

Before marking the architecture complete, confirm that a developer can answer:

1. What are we building?
2. What is the primary resource?
3. What resources are involved?
4. Who owns each important piece of data?
5. What is the source of truth?
6. What enters the service?
7. What gets persisted?
8. What is derived?
9. What is aggregated?
10. How are aggregates calculated?
11. What is returned?
12. Where did the important output fields come from?
13. What are the important business rules?
14. What decisions were made?
15. What remains unknown?
16. Has the architecture been validated?
17. Can implementation begin?