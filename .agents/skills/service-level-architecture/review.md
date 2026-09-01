---

name: service-level-architecture-review
description: Review an existing service-level architecture and identify gaps, contradictions, assumptions, and architectural decisions requiring human review.
---

# Review

## Objective

Challenge the existing architecture before implementation.

Input:

```text
/review <capability>
```

Read:

```text
docs/architecture/<capability>/
├── architecture.md
├── data-model.md
├── data-flow.md
└── open-questions.md
```

Also inspect relevant requirements, code, schema, APIs, and tests when available.

---

## Review Areas

### Business

Check:

* Capability scope
* Primary resource
* Responsibilities
* Business rules
* Missing use cases

### Resources

Check:

* Missing supporting resources
* Incorrect dependencies
* Ownership
* Resource boundaries
* Resource lifecycle

### Source of Truth

Check:

* Important data has an authoritative owner.
* Derived data is not incorrectly treated as authoritative.
* Cached/denormalized data has an explicit rationale.

### Persistence

Check:

* Important data that should be persisted is identified.
* Unnecessary persistence is identified.
* Fields have sources.
* Relationships and constraints are defined.
* Persistence decisions are justified.

### Data Flow

Check:

* Inputs are defined.
* Validation is represented.
* Transformations are represented.
* Persistence/query paths are represented.
* Aggregations are represented.
* Outputs are represented.
* Important lineage is traceable.

### Aggregation

Check:

* Sources are correct.
* Filters are correct.
* Grouping is correct.
* Calculations are explicit.
* Persistence strategy is explicit.

### Consistency

Compare:

```text
architecture.md
      ↕
data-model.md
      ↕
data-flow.md
      ↕
open-questions.md
```

Look for contradictions.

---

## Human Decisions

Identify issues that require the user to decide.

Example:

```text
BLOCKING

1. Account balance source of truth is undefined.

Options:
A. Calculate from transactions.
B. Persist balance on Account.

Impact:
A favors consistency and simpler writes.
B favors read performance but requires balance maintenance.

Decision: HUMAN
```

Do not automatically select an option for ambiguous business semantics.

---

## Output

Do not rewrite the architecture automatically.

Produce:

```text
Review Status: PASS | NEEDS REVIEW

Blocking Issues:
- ...

Important Issues:
- ...

Assumptions:
- ...

Contradictions:
- ...

Decisions Required:
- ...

Recommendations:
- ...
```

If appropriate, add identified questions to `open-questions.md`, but do not mark them as resolved.

The review should help the human make architectural decisions.
