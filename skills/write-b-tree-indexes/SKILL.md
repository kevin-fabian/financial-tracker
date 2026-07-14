---
name: write-b-tree-indexes
description: Query-first workflow for designing effective B-tree indexes with practical equality, range, and join examples.
---

# B-Tree Index Design Skill

Primary goal: design indexes that match real query access paths, reduce full scans, and avoid redundant index sprawl.

## Core Principle

Design from **query shape**, not table shape:

1. Equality predicates first (`=` / `IN` small list)
2. Then range/inequality predicates (`>`, `<`, `BETWEEN`, `LIKE 'x%'`, `NOT IN`)
3. Then join/filter support columns used after filtering
4. Add ordering columns only when they can still be used by the optimizer

---

## Workflow

1. Write down one target query and split predicates by table alias.
2. Mark each predicate type per table: equality, range/inequality, join, order by.
3. Build one composite index candidate per heavily filtered table using the core principle.
4. Check whether existing indexes already cover the same leading columns.
5. Keep the minimum index set that supports the access path and constraints.
6. Validate with execution plan and realistic data.

---

## Practical Examples

### Example 1: Equality + inequality/range + join

```sql
SELECT fcas.former_customer_id
FROM wbdil_former_customer_asset_status fcas
JOIN wbdil_former_customers fc ON fc.id = fcas.former_customer_id
WHERE fcas.asset = :asset
  AND fcas.event_status NOT IN ('PUBLISHED')
  AND fc.end_date > :endDateFrom
ORDER BY fcas.event_status, fc.end_date;
```

Recommended index on `fcas`:

```sql
CREATE INDEX wbdil_fc_asset_status_asset_event_fc_idx
ON wbdil_former_customer_asset_status (asset, event_status, former_customer_id);
```

Why:
- `asset` is equality and highly selective in this path.
- `event_status` is inequality/range-like filter and also appears in ordering.
- `former_customer_id` supports efficient join lookup after filtering.

---

### Example 2: Multiple equalities + range

```sql
SELECT id, amount
FROM payment
WHERE tenant_id = :tenantId
  AND status = :status
  AND booking_date BETWEEN :fromDate AND :toDate;
```

Recommended index:

```sql
CREATE INDEX payment_tenant_status_booking_date_idx
ON payment (tenant_id, status, booking_date);
```

Why:
- `tenant_id`, `status` are equalities.
- `booking_date` is range and should come after equalities.

---

### Example 3: Join-driven lookup

```sql
SELECT o.id, o.total_amount
FROM orders o
JOIN customers c ON c.id = o.customer_id
WHERE c.segment = :segment
  AND o.created_at >= :fromTs;
```

Recommended indexes:

```sql
CREATE INDEX customers_segment_id_idx
ON customers (segment, id);

CREATE INDEX orders_customer_created_at_idx
ON orders (customer_id, created_at);
```

Why:
- `customers(segment, id)` finds matching customers quickly and provides join key.
- `orders(customer_id, created_at)` supports join equality then range filter.

---

## Best Practices

- Prefer one useful composite index over several overlapping partial indexes.
- Avoid duplicate leading-column coverage:
  - if `(a, b, c)` exists, a separate `(a, b)` is often redundant for the same workload.
- Keep unique/business-constraint indexes even if another index looks similar.
- Index foreign keys used in joins, especially high-volume tables.
- Avoid indexing low-selectivity columns alone (`status`, boolean flags) unless combined with a selective leading column.
- Re-check index usefulness after query changes (`WHERE`, `JOIN`, `ORDER BY`).
- Measure before and after using execution plans and runtime metrics.

---

## Oracle Validation Snippets

```sql
EXPLAIN PLAN FOR
SELECT ...
;

SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY);
```

What to verify:
- target index appears in access path
- estimated rows drop early
- fewer sort/full-scan operations for critical queries

---

## Anti-Patterns

- Creating indexes from intuition without a concrete target query.
- Putting range/inequality columns before equality columns in composite indexes.
- Adding many single-column indexes hoping the optimizer will combine them effectively.
- Keeping stale indexes after query refactors.
- Ignoring write overhead (every extra index adds insert/update/delete cost).

---

## Checklist

- [ ] Query predicates classified per table (equality, range, join, order)
- [ ] Composite index order follows equality -> range -> join/order support
- [ ] Existing overlapping indexes reviewed and reduced
- [ ] Unique/constraint indexes preserved
- [ ] Execution plan reviewed
- [ ] Performance checked with realistic data volume
