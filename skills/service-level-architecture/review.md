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

Use the structured format documented in `SKILL.md`:

```markdown
### Q1. <Question title>

**Context**
...

**Options**
- A. ...
- B. ...

**AI Recommendation**
A, with rationale.

**Decision**
<!-- HUMAN: Add your decision here. -->

**Status**
PENDING
```

When adding questions to `open-questions.md` during review, classify them as BLOCKING, IMPORTANT, or OPTIONAL.

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
