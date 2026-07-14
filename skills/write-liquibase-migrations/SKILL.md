---
name: write-liquibase-migrations
description: Database migration best practices using Liquibase with Oracle
---

# Liquibase Database Migration Skills

## Quick Reference

```bash
mvn liquibase:update                                    # Apply pending changes
mvn liquibase:status                                    # View pending changes
mvn liquibase:rollback -Dliquibase.rollbackTag=X.Y      # Rollback to tag
mvn liquibase:validate                                  # Validate changelog syntax
```

---

## Creating Migrations

### File Structure

```
db/
├── db.changelog.master.yaml          # Master changelog (includes all versions)
├── db.changelog.X.Y.description.yaml # Version-specific changelog
├── rollouts/
│   └── VX.Y.Z_description.sql        # Forward migration scripts
└── rollbacks/
    └── UX.Y.Z_description.sql        # Rollback scripts
```

### Changelog Template

```yaml
databaseChangeLog:
  - logicalFilePath: db.changelog.X.Y.description.yaml

  - changeSet:
      author: your.name
      id: X.Y.0
      tagDatabase:
        tag: 'X.Y'

  - changeSet:
      author: your.name
      id: X.Y.1
      sqlFile:
        dbms: oracle
        path: rollouts/VX.Y.1_description.sql
        relativeToChangelogFile: true
      rollback:
        sqlFile:
          dbms: oracle
          path: rollbacks/UX.Y.1_description.sql
          relativeToChangelogFile: true
```

### Include in Master

```yaml
databaseChangeLog:
  # ...existing includes...
  - include:
      file: db.changelog.X.Y.description.yaml
      relativeToChangelogFile: true
```

---

## Versioning & ID Rules

| Rule | Example | Reason |
|------|---------|--------|
| Use semantic versioning | `13.0.1`, `13.0.2` | Predictable ordering |
| Never reuse IDs | Each ID must be globally unique | Prevents collision errors |
| Never modify executed changesets | Create new changeset instead | Breaks checksum validation |
| Tag major versions | `tagDatabase: tag: '13.0'` | Enables targeted rollbacks |

**ChangeSet ID Format:** `{major}.{minor}.{patch}` (e.g., `13.0.1`, `13.0.2`)

---

## Best Practices

| ✅ DO | ❌ DON'T |
|-------|----------|
| Always create rollback scripts | Modify existing changesets |
| Test locally (apply + rollback) | Run production updates manually |
| Use `dbms: oracle` tag | Skip version numbers |
| Tag major versions | Mix DDL and DML in one changeset |
| Separate table creation from grants | Use hardcoded environment values |
| Use descriptive file names | Delete old changelogs |

---

## Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| **Changeset collision** | Duplicate ID or modified checksum | Use unique IDs; never modify executed changesets |
| **Rollback failed - no tag** | Tag not found | Run `mvn liquibase:history` to list available tags |
| **Connection refused** | Database not running | Verify connection: `docker ps \| grep oracle` |
| **Checksum mismatch** | Changeset content changed | Run `mvn liquibase:clearCheckSums` (use carefully) |
| **Lock stuck** | Previous run failed | Release lock via SQL (see below) |

---

## Reference Commands

### Maven Liquibase Goals

| Task | Command |
|------|---------|
| Apply pending changes | `mvn liquibase:update` |
| Preview SQL | `mvn liquibase:updateSQL` |
| Show pending | `mvn liquibase:status` |
| Rollback to tag | `mvn liquibase:rollback -Dliquibase.rollbackTag=X.Y` |
| Preview rollback SQL | `mvn liquibase:rollbackSQL -Dliquibase.rollbackTag=X.Y` |
| View history | `mvn liquibase:history` |
| Validate changelog | `mvn liquibase:validate` |
| Clear checksums | `mvn liquibase:clearCheckSums` |

### Oracle SQL Utilities

```sql
-- View applied changes
SELECT ID, AUTHOR, DATEEXECUTED, TAG FROM DATABASECHANGELOG ORDER BY DATEEXECUTED DESC;

-- Check for locks
SELECT * FROM DATABASECHANGELOGLOCK;

-- Release stuck lock
UPDATE DATABASECHANGELOGLOCK SET LOCKED = 0; COMMIT;
```

---

## Migration Checklist

- [ ] Created rollout SQL in `rollouts/` with unique version
- [ ] Created matching rollback SQL in `rollbacks/`
- [ ] Created YAML changelog with unique changeset IDs
- [ ] Added `tagDatabase` for major versions
- [ ] Included in `db.changelog.master.yaml`
- [ ] Tested `mvn liquibase:update` locally
- [ ] Tested `mvn liquibase:rollback` successfully
- [ ] Verified changes in database
