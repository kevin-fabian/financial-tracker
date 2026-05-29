---
name: liquibase-migration
description: 'Implement or update Liquibase migrations using explicit master changelog includes, versioned changelog YAML files, paired rollout and rollback SQL scripts, and focused validation. Use when asked to add or change schema, constraints, indexes, grants, seed-like SQL, or other database migrations.'
argument-hint: 'What database change is needed? Include affected tables, columns, constraints, indexes, data backfills, grants, compatibility expectations, and rollback requirements.'
---

# Liquibase Migration

Use this skill to implement or update Liquibase migrations in services that follow the shared migration pattern used by this repository.

Before applying concrete paths, module names, profile names, database users, or build commands:

- use `.github/copilot-instructions.md` for the portable architecture and coding rules
- use `AGENTS.md` for the current repository's actual module layout, Liquibase paths, profile behavior, runtime user split, and validation conventions

## Clarify Up Front

Confirm or infer the following before making migration changes:

- [ ] exact schema or SQL change being requested
- [ ] whether the change is PostgreSQL-specific or must stay portable
- [ ] whether the change is additive, destructive, or requires a phased rollout
- [ ] whether existing application code, entities, repositories, or tests must change with the migration
- [ ] rollback expectations and whether a safe rollback is realistically possible
- [ ] whether data backfill, grants, indexes, or constraints are part of the change
- [ ] how the migration will be validated in this repository

---

## Shared Migration Pattern Reference

Use the shared project structure as a reference, then verify the concrete local layout before creating files:

```text
<module>/src/main/resources/db
├── db.changelog.master.yml                 <-- root changelog with explicit includes
├── db.changelog.<version>.<topic>.yml      <-- versioned Liquibase YAML changelog
├── rollouts/
│   └── V<version>_<description>.sql        <-- forward migration SQL
└── rollbacks/
    └── U<version>_<description>.sql        <-- rollback SQL
```

Typical flow:

`db.changelog.master.yml` → included version changelog YAML → `changeSet` → `sqlFile` in `rollouts/` → paired `rollback.sqlFile` in `rollbacks/`

Follow these repository-aligned conventions unless the local service already uses a different, verified Liquibase pattern:

- keep the master changelog explicit with `include`; do not introduce `includeAll` unless the repository already standardizes on it
- keep SQL in external files instead of large inline SQL blocks in YAML
- pair every rollout SQL file with a rollback SQL file whenever a meaningful rollback exists
- set `relativeToChangelogFile: true` on `sqlFile` references
- use `logicalFilePath` in version changelog YAML files
- scope database-specific SQL with `dbms` when the project uses engine-specific migrations

## Repository Pattern to Mirror

In this repository, Liquibase is organized around:

- a master changelog at `app/src/main/resources/db/db.changelog.master.yml`
- explicit includes of versioned changelog YAML files
- PostgreSQL-targeted SQL files under `app/src/main/resources/db/rollouts` and `app/src/main/resources/db/rollbacks`
- paired forward and rollback SQL referenced from YAML changeSets
- Liquibase Maven plugin configuration in the service module `pom.xml`

The current changelog pattern looks like this:

```yaml
databaseChangeLog:
  - logicalFilePath: db.changelog.<version>.<topic>.yml

  - changeSet:
      author: <author>
      id: <version>
      sqlFile:
        dbms: postgresql
        path: rollouts/V<version>_<description>.sql
        relativeToChangelogFile: true
      rollback:
        sqlFile:
          dbms: postgresql
          path: rollbacks/U<version>_<description>.sql
          relativeToChangelogFile: true
```

If the repository uses a release tag changeSet such as `tagDatabase`, keep that aligned with the existing release/versioning approach. Do not add tags casually to unrelated migrations.

## Workflow

1. Inspect the existing Liquibase slice before adding files.
   Reuse the current naming, version grouping, author format, include style, and SQL conventions.
2. Identify the correct module and resource path where Liquibase is configured.
   Verify the `changeLogFile` path and plugin/profile wiring in the module `pom.xml`.
3. Decide whether to extend an existing version changelog file or create a new one.
   - extend an existing version changelog when the repository groups related changeSets in one file and the new change belongs in that batch
   - create a new version changelog when starting a new migration batch or release grouping
4. Add the rollout SQL file under `rollouts/` using the existing versioned naming pattern.
   Keep the SQL idempotent when practical and follow the repository's SQL style.
5. Add the rollback SQL file under `rollbacks/`.
   Roll back only what the paired rollout introduced, and keep rollback behavior explicit.
6. Add or update the version changelog YAML.
   - set `logicalFilePath`
   - add a `changeSet` with the repository's author and id style
   - reference rollout and rollback with `relativeToChangelogFile: true`
   - set `dbms` when using engine-specific SQL
7. If a new version changelog file was created, add an explicit `include` entry to the master changelog.
   Keep include ordering intentional and deterministic.
8. Update the application code only when required by the schema change.
   Keep JPA entity changes, repository changes, and service/controller changes separate from the migration itself.
9. Validate the migration.
   At minimum, verify changelog structure and confirm the build or migration command appropriate for the module/profile used by the repository.
10. Run focused tests for any touched Java layers.
    Do not assume H2-based tests validate PostgreSQL-specific Liquibase SQL.

## Migration Authoring Rules

### Do

- prefer additive, backward-compatible changes first when the application may be deployed before and after the migration
- add indexes, constraints, grants, and data backfills in clearly named, isolated changeSets when that improves rollback and reviewability
- keep destructive changes phased when a safer two-step rollout is possible
- preserve the split between migration/database-owner users and runtime application users when the repository uses different credentials for each
- make file names and changeSet ids sortable and easy to review
- keep rollback SQL realistic; if rollback cannot safely restore data, say so clearly in the review notes and use the repository's established practice

### Don't

- don't hide many unrelated schema changes in one SQL file
- don't change the master changelog ordering casually
- don't mix application refactors and schema changes in a way that makes migration review difficult
- don't assume the default local profile runs Liquibase against the same database used in production-like flows
- don't rely on H2 entity auto-DDL as proof that a PostgreSQL Liquibase migration is correct
- don't omit rollback files when the repository pattern expects paired rollouts and rollbacks

## Validation Guidance

Use the repository's verified module, profile, and database setup from `AGENTS.md` and the relevant `pom.xml` before running commands.

### Liquibase Verification Commands

In this repository, the Liquibase Maven plugin is configured in the `app` module and wired through the `local` Maven profile in `app/pom.xml`.
Start with non-mutating verification commands from the repository root:

```zsh
./mvnw -pl app -P local liquibase:validate
./mvnw -pl app -P local liquibase:status
./mvnw -pl app -P local liquibase:updateSQL
```

Use them in this order:

- `liquibase:validate` to verify changelog structure, includes, changeSet definitions, and referenced files
- `liquibase:status` to confirm which changeSets are pending against the configured PostgreSQL database
- `liquibase:updateSQL` to preview the SQL Liquibase would run without mutating the database

Use `liquibase:update` only as an intentional final local verification step when you have a PostgreSQL environment ready for this repository's Liquibase configuration.

Workflow for applying changes locally:
```zsh
./mvnw -pl app -P local liquibase:update

-/mvwn -pl app -P local liquibase:rollback -Dliquibase.rollbackTag=<tag>

./mvnw -pl app -P local liquibase:update
```

Important cautions for this design pattern:

- the Maven Liquibase configuration uses the database settings from `app/pom.xml`, not the runtime datasource in `application-local.yaml`
- this repository's Liquibase flow targets PostgreSQL; H2-backed local/test profiles do not validate PostgreSQL-specific SQL
- Liquibase runs with the database-owner style credentials configured for migrations, while the app runtime uses a separate lower-privilege user
- if using the local Docker PostgreSQL setup, verify the database, schema, and credentials match the Liquibase Maven profile before running mutating commands

Typical validation checks:

- the new changelog file is explicitly included by the master changelog when required
- rollout and rollback file names match the referenced paths exactly
- `dbms` values match the SQL being written
- new SQL respects the repository's schema/user conventions
- the migration can run in the intended local or CI database environment
- any touched Java code still passes its focused tests

Repository-specific caution to carry across similar services using this design:

- local and test profiles may rely on H2 and Hibernate `ddl-auto`, while Liquibase SQL targets PostgreSQL
- Liquibase may run with a database-owner user, while the application runs with a less-privileged runtime user
- grant migrations should preserve that separation instead of broadening runtime privileges unnecessarily

## Review Checklist

- the change follows the existing `db/` directory layout
- master changelog includes remain explicit and ordered
- version changelog YAML uses `logicalFilePath`
- each new rollout has a paired rollback file when applicable
- changeSet ids, author values, and file names follow the existing convention
- SQL is scoped to the intended DB engine when necessary
- application-layer changes, if any, are aligned with the new schema
- validation was performed using the repository's actual module/profile setup

## Example Prompts

- Add a Liquibase migration to create a new table and rollback script using the existing PostgreSQL SQL-file pattern.
- Add an index and a grant migration following the repository's explicit master changelog include style.
- Create a phased Liquibase migration for renaming a column without breaking existing application code.
- Update the changelog structure so a new migration batch follows the same rollout and rollback conventions used by the service.
