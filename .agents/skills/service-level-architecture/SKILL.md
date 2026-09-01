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

# Principles

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

# Anti-Patterns to Avoid

* Persist derived values as source of truth
* Leak infrastructure models across service boundaries
* Create storage for transient or aggregated data
* Treat a derived or cached value as authoritative without an explicit decision
* Infer relationships solely from similar field names
* Create persistence merely because a value appears in an output
* Conflate reference with ownership

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

### Field Inventory

For every supporting resource that will be persisted, document all fields:

| Field | Type | Nullable | Constraints | Notes |
|-------|------|----------|-------------|-------|
| id | Identifier | NO | Primary key | |
| name | String (128) | YES | | |
| owner_id | Identifier | YES | Reference to owner | |

Required fields per resource:

- Identifier strategy
- All fields with type, nullability, length, and constraints
- References to other resources and their targets
- Enum/choice fields and their allowed values
- Audit fields: creation time, modification time, creator, last modifier
- Soft-delete or active flags
- System vs. user-owned flags
- Collection relationships: one-to-many, many-to-one, many-to-many
- Complex types: JSON, arrays, key-value pairs

### Enum Inventory

Document every enum or choice list used in the domain:

| Enum | Values | Used By |
|------|--------|--------|
| TransactionType | INPUT, EXPENSE | Category, Transaction |

Prefer stable, named values over positional/ordinal values so that additions or reorderings do not break existing data.

### Read Model Inventory

Document read-optimized models used for query responses:

| Read Model | Fields | Used By Query |
|------------|--------|--------------|
| AccountSummary | id, name, balance | Account listing |

Read models should use simple types (primitives, strings, identifiers) and provide sensible defaults for numeric fields. Never expose internal persistence models in query responses.

### Schema Evolution Trace

For every persisted resource, identify the migration or schema change that created or last modified it:

| Resource | Table | Migration |
|----------|-------|-----------|
| Category | categories | V1.x.x_... |

---

## 4. Source of Truth

For important data, identify the authoritative owner.

| Data | Source of Truth | Notes |
| ---- | --------------- | ----- |
| Transaction amount | Transaction | Financial fact |
| Category name | Category | Dimension |
| Budget allocation | Budget | Planned value |
| Report total | Derived | Not a source of truth |

Never treat a derived or cached value as authoritative without an explicit decision.

---

## 5. Data Lineage

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

Prioritize lineage for:

* Financial values
* IDs
* Dates
* Statuses
* Aggregates
* Calculated fields

Not every trivial field requires a full lineage diagram.

---

## 6. Use Cases

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

## 7. Incoming Data

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

## 8. Data Transformation

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

### Aggregates and Derived Data

List important calculated fields as a subsection of transformation:

| Field | Sources | Filters | Grouping | Calculation | Stored? |
| ----- | ------- | ------- | -------- | ----------- | ------: |
| totalIncome | Transactions | type=INPUT | none | SUM(amount) | No |
| totalExpense | Transactions | type=EXPENSE | none | SUM(amount) | No |
| netCashFlow | totalIncome, totalExpense | none | none | income - expense | No |
| budgetRemaining | Budget, Transactions | by category | category | allocated - SUM(spent) | No |

Every important aggregate must identify:

* Source data
* Filters
* Grouping
* Calculation
* Persistence strategy

---

## 9. Persistence

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

| Field | Persist? | Source | Reason |
| ----- | -------: | ------ | ------ |
| amount | Yes | Request | Source of truth |
| categoryName | No | Category | Owned elsewhere |
| totalExpense | No | Transactions | Derived |

Identify:

* Identifier strategy
* References to other resources
* Nullability
* Uniqueness constraints
* Important indexes
* Delete/update behavior
* Cascade behavior on relationships
* Data access strategy (direct query, read model, aggregation)

Do not create persistence merely because a value appears in an output.

---

## 10. Relationships

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

## 11. Output

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

## 12. End-to-End Data Flow

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

## 13. Business Rules and Errors

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

## 14. Architecture Decisions

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

## 15. Open Questions

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

## 16. Architecture History

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

## 17. Validation and Readiness

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

### Implementation Readiness

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
* Supporting resources identified with field inventory
* Source of truth established
* Persistence understood
* Relationships understood
* Aggregations defined
* Outputs defined
* Important lineage established
* Business rules established
* No blocking questions

---

# Required Artifacts

## Artifact Selection

**Always required:**

* `architecture.md` — the analysis document produced by this skill

**Required when primary resource has persisted sub-resources:**

* `data-model.md` — resource schema, relationships, indexes, constraints,
  field-level persistence decisions for every sub-resource

**Required when data crosses system boundaries:**

* `data-flow.md` — external APIs, message queues, event schemas

Keep the architecture in one file by default. Only create additional artifacts
when the selection criteria above are met. Do not create unnecessary documentation.

---

# Architecture Lifecycle

Use this cycle:

```text
Requirements
     ↓
AI Analysis
     ↓
architecture.md (+ data-model.md if sub-resources)
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

# Completion Checklist

Before marking the architecture complete, confirm every item:

- [ ] Section 1 (Capability) filled
- [ ] Section 2 (Primary Resource) classified
- [ ] Section 3 (Supporting Resources) with field inventory, enum inventory, read model inventory, schema evolution trace
- [ ] Section 4 (Source of Truth) confirmed
- [ ] Section 5 (Data Lineage) for financial values, IDs, dates, statuses, aggregates
- [ ] Section 6 (Use Cases) for each command/query
- [ ] Section 7 (Incoming Data) with required/optional/defaults
- [ ] Section 8 (Data Transformation) with aggregates subsection
- [ ] Section 9 (Persistence) — every field answered
- [ ] Section 10 (Relationships) documented
- [ ] Section 11 (Output) model defined
- [ ] Section 12 (End-to-End Data Flow) diagram drawn
- [ ] Section 13 (Business Rules) — non-trivial rules listed
- [ ] Section 14 (Architecture Decisions) recorded
- [ ] Section 15 (Open Questions) — no BLOCKING items remaining
- [ ] Section 16 (Architecture History) maintained
- [ ] Section 17 (Validation) performed
- [ ] `data-model.md` created (if sub-resources persisted)
- [ ] Section 17 (Readiness) = READY
