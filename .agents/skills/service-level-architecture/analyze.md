---
name: service-level-architecture-analyze
description: Analyze a business capability or primary resource and create or update the service-level architecture artifacts.
---

# Analyze

## Objective

Build the initial data-first architecture or update an existing architecture with newly discovered information.

Input:

```text
/analyze <business capability or primary resource>
```

Example:

```text
/analyze Reporting
```

---

## Process

### 1. Establish Context

Identify:

* Business capability
* Primary resource
* Existing resources
* Existing requirements
* Existing schema
* Existing APIs
* Existing code
* Existing tests
* Existing architecture artifacts

Use available project information before making assumptions.

---

### 2. Identify Primary Resource

Determine:

* Purpose
* Responsibilities
* Persistence status
* Source of truth
* Inputs
* Outputs
* Business rules
* Relationships

---

### 3. Discover Supporting Resources

Identify material supporting resources.

For each material resource determine:

```text
Purpose
Source of truth
Origin / incoming data
Persistence
Relationships
Transformations
Aggregates / derived data
Usage / outputs
Lineage
Business rules
```

Do not treat supporting resources as names only.

---

### 4. Map Data

Trace:

```text
Source
  ↓
Input
  ↓
Validation
  ↓
Transformation
  ↓
Persistence
  ↓
Query
  ↓
Aggregation
  ↓
Projection
  ↓
Output
```

Identify important field-level lineage.

---

### 5. Analyze Persistence

Determine:

* What is persisted?
* What is not persisted?
* Why is each important field stored?
* Which resource owns each datum?
* What are the relationships?
* What constraints exist?
* What is derived?

---

### 6. Analyze Aggregates

For every important calculated value determine:

```text
Source
Filter
Grouping
Calculation
Persistence strategy
```

---

### 7. Analyze Outputs

Define important output structures and trace important fields to their sources.

---

### 8. Identify Decisions

Record:

* Architectural decisions
* Alternatives
* Trade-offs
* Blocking questions
* Important unknowns

Do not silently resolve ambiguous business semantics.

---

## Generate Artifacts

Always create/update:

```text
docs/architecture/<capability>/
├── architecture.md
└── open-questions.md
```

If the primary resource is persisted **and** has material supporting resources, also create/update:

```text
docs/architecture/<capability>/
├── architecture.md
├── data-model.md
├── data-flow.md
└── open-questions.md
```

### `architecture.md`

Contains the overall architecture, decisions, source of truth, boundaries, use cases, business rules, lineage summary, history, and readiness.

### `data-model.md`

Contains the detailed persistence model and relationships for the primary and material supporting resources.

### `data-flow.md`

Contains detailed command/query, record preview and end-to-end data flows.

### `open-questions.md`

Contains unresolved questions classified as BLOCKING, IMPORTANT, or OPTIONAL.

---

## Existing Artifacts

If artifacts already exist:

* Read them first.
* Preserve human decisions.
* Update only affected sections.
* Detect contradictions.
* Do not regenerate blindly.
* Add meaningful changes to Architecture History.
* Move resolved questions out of the unresolved list.
* Keep all artifacts synchronized.

---

## Completion

Set architecture status to:

```text
DRAFT
```

unless the user explicitly indicates that the architecture has already been reviewed and approved.

End with:

```text
Implementation Readiness: READY | NOT READY
```

Do not mark `READY` if blocking questions remain.
