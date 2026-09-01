---
name: service-level-architecture
description: Data-first service architecture workflow. Analyze a business capability or primary resource, review the resulting architecture, and validate its consistency before implementation.
---

# Service-Level Architecture

## Purpose

Design a service from an end-to-end data perspective before implementation.

The architecture should make this visible:

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

The goal is to establish a reviewed and validated architecture before writing implementation code.

---

## Operations

Use one of:

```text
/analyze
/review
/validate
```

### `/analyze`

Discover and create/update the architecture.

```text
Requirements
    ↓
Primary Resource
    ↓
Supporting Resources
    ↓
Source of Truth
    ↓
Persistence
    ↓
Data Flow
    ↓
Aggregations
    ↓
Outputs
```

### `/review`

Challenge the current architecture.

Find:

* Missing information
* Ambiguous business rules
* Incorrect assumptions
* Missing source-of-truth definitions
* Persistence concerns
* Missing relationships
* Missing data lineage
* Aggregation problems
* Contradictions
* Architectural decisions requiring human input

Do not silently make business decisions.

### `/validate`

Verify the architecture after analysis or human review.

Check consistency across:

```text
Requirements
    ↓
Architecture
    ↓
Data Model
    ↓
Data Flow
    ↓
Open Questions
```

Return:

```text
PASS
```

or:

```text
FAIL
```

with blocking issues and inconsistencies.

---

## Artifacts

Architecture artifacts live at:

```text
docs/architecture/<capability>/
├── architecture.md
├── data-model.md
├── data-flow.md
└── open-questions.md
```

### architecture.md

The central architectural source of truth.

Contains:

* Capability
* Primary resource
* Supporting resources
* Responsibilities and boundaries
* Source of truth
* Use cases
* Business rules
* Architectural decisions
* Data lineage summary
* Architecture history
* Implementation readiness

### data-model.md

The persistence model.

Contains:

* Entities/resources
* Fields
* Types
* Nullability
* Primary keys
* Foreign keys
* Relationships
* Constraints
* Important indexes
* Ownership
* Persisted vs derived data
* Persistence decisions

### data-flow.md

The data movement model.

Contains:

* Inputs
* Validation
* Transformation
* Resource interaction
* Persistence
* Queries
* Joins
* Aggregations
* Projection
* Outputs
* Events/external systems
* End-to-end flows

### open-questions.md

The unresolved decision backlog.

Classify questions as:

```text
BLOCKING
IMPORTANT
OPTIONAL
```

---

## Artifact Ownership

`architecture.md` is the architectural decision source of truth.

`data-model.md` is the persistence-detail source of truth.

`data-flow.md` is the data-movement source of truth.

`open-questions.md` is the unresolved-decision source of truth.

The four artifacts must remain consistent.

---

## Primary and Supporting Resources

Start from the primary resource.

Then discover supporting resources from actual dependencies.

A supporting resource is material when it:

* Owns data required by the primary resource
* Is directly persisted and related
* Participates in business rules
* Supplies data to persistence
* Supplies data to outputs
* Participates in aggregation
* Determines ownership or authorization
* Affects lifecycle or consistency

Material supporting resources must be analyzed to the same data depth as the primary resource.

```text
Resource
├── Purpose
├── Source of Truth
├── Origin / Incoming Data
├── Persistence
├── Relationships
├── Transformations
├── Aggregates / Derived Data
├── Usage / Output
├── Lineage
└── Business Rules
```

Do not deeply analyze unrelated resources.

---

## Source of Truth

Identify the authoritative owner of important data.

Distinguish:

```text
Source of truth
Derived data
Cached data
Denormalized data
```

Do not treat derived or cached data as authoritative without an explicit architectural decision.

---

## Persistence

Explicitly identify what is stored.

For every material persisted resource document:

* Fields
* Types
* Nullability
* Primary key
* Foreign keys
* Constraints
* Important indexes
* Source of each important field
* Reason for persistence

Explicitly identify data that is **not** persisted and why.

---

## Aggregation

Every important aggregate must define:

* Source
* Filters
* Grouping
* Calculation
* Whether persisted or calculated at query time

Example:

```text
Transactions
    ↓
filter EXPENSE
    ↓
group by Category
    ↓
SUM(amount)
    ↓
CategoryExpense
```

---

## Data Lineage

Important output fields must be traceable back to their source.

```text
Input
  ↓
Source
  ↓
Persistence
  ↓
Transformation
  ↓
Aggregation
  ↓
Projection
  ↓
Output
```

Prioritize lineage for:

* Financial values
* IDs
* Dates
* Statuses
* Aggregates
* Calculated values

---

## Architecture History

Record meaningful architectural changes in `architecture.md`.

```text
## Architecture History

| Date | Change | Reason |
|---|---|---|
| YYYY-MM-DD | ... | ... |
```

Do not record formatting or typo corrections.

When changing a previous architectural decision, preserve the reason and impact.

---

## Lifecycle

```text
/analyze
    ↓
Human Review
    ↓
/review
    ↓
Human Decisions / Corrections
    ↓
/validate
    ↓
   ┌──────┐
   │ PASS │ → Architecture Approved
   └──────┘
       │
      FAIL
       ↓
Resolve Issues
       ↓
/validate
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

Architecture and behavioral specification are separate concerns.

---

## Rules

* Analyze before implementing.
* Start from the capability or primary resource.
* Discover supporting resources from real dependencies.
* Analyze material supporting resources deeply.
* Identify source of truth.
* Explicitly identify persisted vs derived data.
* Define important aggregates.
* Trace important output fields to their source.
* Keep all four artifacts consistent.
* Do not invent business rules or relationships.
* Mark uncertain information as `UNKNOWN`.
* Separate confirmed facts, inference, and recommendations.
* Surface ambiguous decisions.
* Preserve architecture history.
* Do not silently overwrite human decisions.
* Do not generate implementation code.
* Prefer concise diagrams and tables.
* Keep architecture separate from behavioral specification.
