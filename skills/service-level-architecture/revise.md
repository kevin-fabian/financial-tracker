---
name: service-level-architecture-revise
description: Iterate on an existing service-level architecture by incorporating new requirements, decisions, and discoveries, propagating changes across affected architecture artifacts, and resolving or surfacing open questions while keeping all artifacts consistent.
---

# Revise Architecture

## Purpose

Iterate on an existing service-level architecture.

`/revise` updates all affected architecture artifacts when requirements, decisions, schema, APIs, implementation discoveries, or open questions change.

The goal is to keep the architecture artifacts synchronized without requiring manual updates across multiple files.

## Inputs

Read:

* `architecture.md`
* `data-model.md`
* `data-flow.md`
* `open-questions.md`
* Relevant requirements, schemas, APIs, code, tests, and existing documentation

Use the project source as evidence. Do not invent business rules or relationships.

## Core Rules

1. Preserve existing valid decisions and architecture history.
2. Do not blindly regenerate artifacts.
3. Determine the impact of every change before editing.
4. Update every affected artifact, not only the artifact explicitly mentioned.
5. Keep `architecture.md`, `data-model.md`, `data-flow.md`, and `open-questions.md` consistent.
6. Never silently resolve ambiguous business decisions.
7. Use `UNKNOWN` when information is unavailable.
8. Distinguish:

    * `CONFIRMED` — supported by requirements or project evidence
    * `INFERRED` — derived from existing evidence
    * `RECOMMENDED` — architectural recommendation
    * `UNKNOWN` — insufficient information
9. Do not generate implementation code.
10. Do not remove historical decisions unless they are explicitly obsolete.
11. Do not modify unrelated architecture sections.

## Revision Process

### 1. Establish Current State

Read the existing architecture artifacts and identify:

* Current primary resource
* Material supporting resources
* Source of truth
* Persistence decisions
* Relationships
* Business rules
* Data flows
* Transformations
* Joins
* Projections
* Enrichment
* Aggregations
* Outputs and field lineage
* Open and resolved questions

### 2. Identify Changes

Determine what has changed since the current architecture was produced.

Possible changes include:

* New or changed requirements
* Schema changes
* API changes
* New supporting resources
* Removed resources
* Changed relationships
* Changed persistence strategy
* Changed business rules
* New query or reporting requirements
* New transformations or enrichment
* New aggregation requirements
* Human decisions in `open-questions.md`
* Implementation discoveries

Do not treat unchanged information as a change.

### 3. Analyze Impact

For each change, determine affected areas.

Example:

```text
Category becomes persisted
        ↓
Transaction relationship affected
        ↓
Data model affected
        ↓
Transaction query affected
        ↓
Response enrichment affected
        ↓
Output lineage affected
```

Consider impact on:

* Resource boundaries
* Source of truth
* Persistence
* Relationships
* Constraints
* Data flow
* Queries and joins
* Projections
* Enrichment
* Transformations
* Aggregations
* Output models
* Field lineage
* Open questions

### 4. Propagate Changes

Update all affected artifacts.

#### `architecture.md`

Update when the change affects:

* Capability scope
* Resource responsibilities
* Resource boundaries
* Source of truth
* Architectural decisions
* Important dependencies
* Business rules
* Architecture history

Do not add detailed entity fields or detailed query flows here.

#### `data-model.md`

Update when the change affects:

* Entities/resources
* Fields
* Types
* Nullability
* Defaults
* Primary/foreign keys
* Relationships
* Constraints
* Indexes
* Ownership
* Persisted vs derived data

#### `data-flow.md`

Update when the change affects:

* Inputs
* Queries
* Joins
* Filtering
* Projection
* Enrichment
* Transformation
* Mapping
* Aggregation
* Persistence flow
* External calls
* Outputs
* Field lineage
* Record previews

Show how data shape changes when the revision introduces or changes a transformation.

For REST APIs, update relevant flows such as:

```text
API Request
    ↓
Request Model
    ↓
Domain/Application Model
    ↓
Entity
    ↓
Persistence
```

and/or:

```text
Database
    ↓
Query / Join
    ↓
Projection
    ↓
Enrichment / Aggregation
    ↓
Response Model
    ↓
REST API Response
```

Do not invent intermediate models that do not exist in the architecture.

#### `open-questions.md`

Update when the revision:

* Resolves an existing question
* Makes an existing question obsolete
* Introduces a new architectural decision
* Reveals missing business information
* Creates a dependency on an unresolved decision

Preserve resolved questions for history.

### 5. Re-evaluate Open Questions

After propagating changes:

1. Check every `OPEN` question.
2. Determine whether the new information resolves it.
3. Apply decisions marked by the human.
4. Identify questions invalidated by the revision.
5. Identify new questions caused by the revision.

Never choose a business decision merely to make the architecture complete.

If a decision is required, add it to `open-questions.md`.

### 6. Check Cross-Artifact Consistency

Before finishing, verify:

```text
architecture.md
      ↕
data-model.md
      ↕
data-flow.md
      ↕
open-questions.md
```

Check that:

* Resources have consistent names
* Relationships agree
* Source-of-truth decisions agree
* Persistence decisions agree
* Data flows use existing resources
* Outputs have valid lineage
* Aggregations use valid sources
* Joins use documented relationships
* Transformations match the documented models
* Resolved decisions are reflected everywhere they apply
* No stale information remains in affected sections

### 7. Record Architecture History

If the revision changes an architectural decision, add an entry to the architecture history.

Include:

* Date or revision identifier
* Decision/change
* Reason
* Affected area

Do not rewrite history to make the current architecture appear as if it was always designed that way.

## Record Preview

When a revision changes the shape of important data, update representative record previews.

Use consistent fictional IDs.

For example:

```text
Transaction Entity
{
  id: "txn-001",
  accountId: "acc-001",
  categoryId: "cat-001",
  amount: 450
}

        ↓ JOIN + ENRICHMENT

Transaction Projection
{
  transactionId: "txn-001",
  amount: 450,
  accountName: "Wallet",
  categoryName: "Food"
}

        ↓ MAPPING

Transaction Response
{
  id: "txn-001",
  amount: 450,
  account: "Wallet",
  category: "Food"
}
```

Only include fields supported by the architecture.

## Completion Criteria

A revision is complete when:

* All known changes are incorporated.
* All affected artifacts are synchronized.
* Existing human decisions are preserved.
* Resolved decisions are propagated.
* New ambiguities are recorded.
* No known cross-artifact contradictions remain.
* Architecture history is preserved.
* No unrelated sections were unnecessarily changed.

The architecture does not need to be considered final.

If further decisions are required, leave the relevant questions in `open-questions.md` and continue with `/revise` after the human provides the decisions.

## Output

Report:

```text
Revision Status: COMPLETE

Changes:
- <change>

Affected Artifacts:
- architecture.md
- data-model.md
- data-flow.md
- open-questions.md

Open Questions:
- <question>

Consistency:
- PASS / NEEDS REVIEW
```

Do not claim the architecture is implementation-ready. `/review` and `/validate` are responsible for final quality and readiness checks.
