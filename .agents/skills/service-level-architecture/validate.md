---
name: service-level-architecture-validate
description: Validate service-level architecture artifacts for consistency, completeness, traceability, and implementation readiness.
---

# Validate

## Objective

Verify that the architecture is internally consistent and sufficiently defined for implementation.

Input:

```text
/validate <capability>
```

Read:

```text
docs/architecture/<capability>/
├── architecture.md
├── data-model.md
├── data-flow.md
└── open-questions.md
```

Also compare against available requirements, schema, APIs, code, and tests.

---

## Validation

### 1. Requirement Consistency

Verify the architecture addresses known requirements.

### 2. Resource Consistency

Verify:

* Primary resource is defined.
* Material supporting resources are included.
* Relationships are consistent.

### 3. Source-of-Truth Consistency

Verify important data has an authoritative owner.

### 4. Persistence Consistency

Verify `data-model.md` agrees with `architecture.md`.

Check:

* Fields
* Ownership
* Relationships
* Constraints
* Persisted vs derived data

### 5. Data-Flow Consistency

Verify `data-flow.md` agrees with:

* Resources
* Persistence
* Inputs
* Outputs
* Aggregations

### 6. Lineage

Verify important outputs can be traced to their source.

### 7. Aggregations

Verify every important aggregate has:

```text
Source
Filter
Grouping
Calculation
Persistence strategy
```

### 8. Open Questions

Verify:

* Blocking questions are unresolved.
* Resolved questions are reflected in the architecture.
* Important decisions are not contradicted elsewhere.

### 9. Architecture History

Verify meaningful architectural changes are reflected in the current artifacts.

---

## Validation Result

Return:

```text
Architecture Validation: PASS
```

or:

```text
Architecture Validation: FAIL
```

Use:

```text
Blocking Issues:
- ...

Inconsistencies:
- ...

Missing Information:
- ...

Unresolved Decisions:
- ...

Warnings:
- ...
```

Do not modify architecture decisions to make validation pass.

---

## Readiness

Only report:

```text
Implementation Readiness: READY
```

when:

* Primary resource is defined.
* Material supporting resources are analyzed.
* Source of truth is established.
* Persistence is understood.
* Relationships are defined.
* Important aggregates are defined.
* Outputs are defined.
* Important lineage is established.
* Business rules are sufficiently defined.
* `architecture.md`, `data-model.md`, and `data-flow.md` are consistent.
* No BLOCKING questions remain.

Otherwise:

```text
Implementation Readiness: NOT READY
```
