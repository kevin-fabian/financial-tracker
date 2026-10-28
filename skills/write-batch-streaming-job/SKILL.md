---
name: write-batch-streaming-job
description: Procedure for implementing a memory-efficient batch job that streams records from the database, processes them in batches, and bulk-inserts results — for CRON-triggered background processing (project)
---

# Batch Streaming Job (CRON-triggered)

Pattern for implementing a background job that processes large datasets without loading everything into memory. Uses Spring Data JPA `Stream` with fetch-size hinting for reading, accumulates into batches in the service layer, and bulk-inserts via `saveAll`. **Includes flush + clear to prevent persistence-context OOM.**

## When to use

- CRON/scheduled job that iterates over a potentially large table
- Each source record maps to one or more output records that must be inserted
- You want to avoid `List<T>` of the entire source table in memory
- Output records must be inserted in batches, not one-by-one

---

## Hidden trap: persistence context L1 cache (read this first)

A common, silent failure mode: the job runs fine on dev data (hundreds of records) but throws `OutOfMemoryError` in production (hundreds of thousands to millions of records). Root cause — **every entity loaded or persisted stays in the JPA persistence context (L1 cache) until the transaction commits.**

Two accumulation paths in a streaming batch job:

| Path | What accumulates | Why |
|------|------------------|-----|
| **Read** (streaming source) | Every source entity + eagerly-fetched associations loaded from the stream | Stream is open within a transaction; Hibernate holds each loaded entity in the context |
| **Write** (batch insert) | Every target entity passed to `saveAll()` | `saveAll()` calls `persist()`, which makes the entity managed |

Neither `saveAll()` nor `saveAndFlush()` clears the context. `saveAndFlush()` pushes SQL to the database but **does not detach** entities. Without explicit clearing, memory grows linearly with total record count.

### The fix: flush + clear after every batch

Expose a `flush()` method on the target domain repository that does **both**:

```java
// Target repository — domain repository interface
void flush();
```

```java
// Target repository — Default implementation
@Override
public void flush() {
    jpaTransactionRepository.flush();   // push pending SQL to DB
    entityManager.clear();             // detach ALL managed entities, release memory
}
```

`entityManager.clear()` detaches every managed entity — both the target entities you just saved **and** the source entities loaded earlier in the stream. One call per batch resets the entire context.

Call `flush()` after **every** batch `saveAll()`, including the final remainder:

```java
if (batch.size() >= BATCH_SIZE) {
    transactionRepository.saveAll(new ArrayList<>(batch));
    batch.clear();
    transactionRepository.flush();   // <-- flush + clear
}
// ...
if (!batch.isEmpty()) {
    transactionRepository.saveAll(batch);
    transactionRepository.flush();   // <-- flush + clear for remainder
}
```

This bounds memory to roughly one batch size regardless of total record count.

### Why not alternatives?

| Alternative | Why it doesn't work here |
|-------------|--------------------------|
| `saveAllAndFlush()` | Flushes SQL but does **not** clear the context — entities still accumulate |
| `TransactionTemplate` per batch | Incompatible with streaming — can't hold a `ResultSet` stream open across transaction boundaries |
| Hibernate `StatelessSession` | Bypasses persistence context but breaks lazy loading, cascading, dirty checking; leaks Hibernate types across hexagonal boundaries; requires manual association resolution |

`flush()` + `clear()` is the standard Spring approach: minimal, framework-portable, works with streaming, and keeps `EntityManager` confined to the persistence adapter.

---

## Ripple points

| Layer | Change |
|-------|--------|
| `Jpa*Repository` (source) | Add `Stream<Entity> streamXxx(@Param("now") ZonedDateTime now)` with `@QueryHints(@QueryHint(name = "org.hibernate.fetchSize", value = "50"))` |
| Domain repository interface (source) | Mirror the `Stream` method signature |
| Default repository impl (source) | Map entity stream to model stream via `.map(Entity::toModel)` |
| Domain repository interface (target) | Add `List<TargetModel> saveAll(List<TargetModel>)` + `void flush()` |
| Default repository impl (target) | Convert to entities, call `JpaRepository.saveAll(entities)`, map back; implement `flush()` |
| Service interface | Add `void processXxx()` |
| Service impl | Inject target repository; implement streaming + batching + bulk insert + flush per batch |

---

## JPA streaming — fetch size hint

```java
@QueryHints(value = @QueryHint(name = "org.hibernate.fetchSize", value = "50"))
@Query("""
        SELECT rt FROM RecurringTransactionEntity rt
        WHERE rt.nextOccurrenceDate < :now
          AND rt.variableAmount = false
          AND rt.status = com.fabiankevin.app.models.recurring_transactions.RecurringTransactionStatus.ACTIVE
        """)
Stream<RecurringTransactionEntity> streamDueRecurringTransactions(@Param("now") ZonedDateTime now);
```

The `fetchSize` hint tells the JDBC driver to fetch rows in chunks of 50 rather than pulling the entire `ResultSet` at once. The `Stream` is backed by an open `ResultSet` — you **must** consume it inside a `try-with-resources` or a `@Transactional` method to keep the JDBC connection open.

---

## Service layer — batch accumulation + flush

```java
private static final int BATCH_SIZE = 50;

@Transactional
@Override
public void processDueRecurringTransactions() {
    ZonedDateTime now = ZonedDateTime.now();
    LocalDate today = LocalDate.now();
    Instant instantNow = now.toInstant();
    List<Transaction> batch = new ArrayList<>(BATCH_SIZE);

    try (Stream<RecurringTransaction> stream =
                 recurringTransactionRepository.streamDueRecurringTransactions(now)) {
        stream.forEach(recurring -> {
            Transaction transaction = Transaction.builder()
                    .account(recurring.account())
                    .category(recurring.category())
                    .type(recurring.category().type())
                    .amount(Amount.of(recurring.amount(), recurring.account().currency()))
                    .description(recurring.description())
                    .transactionDate(today)
                    .recurringTransactionId(recurring.id())
                    .createdAt(instantNow)
                    .updatedAt(instantNow)
                    .build();
            batch.add(transaction);

            if (batch.size() >= BATCH_SIZE) {
                transactionRepository.saveAll(new ArrayList<>(batch));
                batch.clear();
                transactionRepository.flush();
            }
        });
    }

    if (!batch.isEmpty()) {
        transactionRepository.saveAll(batch);
        transactionRepository.flush();
    }
}
```

Key points:
- `try-with-resources` closes the stream (and underlying JDBC `ResultSet`) deterministically
- `new ArrayList<>(batch)` passed to `saveAll` so the cleared `batch` list doesn't alias the argument
- `flush()` after every batch **and** after the final remainder — keeps persistence context bounded
- Final flush after the loop handles the remainder (< `BATCH_SIZE`)
- `BATCH_SIZE` is a `private static final int` constant on the service class

---

## Target repository — batch insert + flush

Domain repository:

```java
List<Transaction> saveAll(List<Transaction> transactions);
void flush();
```

Default implementation:

```java
private final JpaTransactionRepository jpaTransactionRepository;
private final EntityManager entityManager;

@Override
public List<Transaction> saveAll(List<Transaction> transactions) {
    List<TransactionEntity> entities = transactions.stream()
            .map(TransactionEntity::from)
            .toList();
    return jpaTransactionRepository.saveAll(entities).stream()
            .map(TransactionEntity::toModel)
            .toList();
}

@Override
public void flush() {
    jpaTransactionRepository.flush();
    entityManager.clear();
}
```

`JpaRepository<T, ID>` already provides `saveAll(Iterable<S>)` returning `List<S>` — no custom JPQL needed. `EntityManager` is injected via `@PersistenceContext` or constructor and stays within the persistence adapter — it does not leak into the domain or service layer.

---

## Test patterns

Use `@ExtendWith(MockitoExtension.class)` with `@Mock` for both the source and target repositories, `@InjectMocks` for the service.

Stub the stream:

```java
when(recurringTransactionRepository.streamDueRecurringTransactions(any()))
        .thenReturn(Stream.of(recurring1, recurring2));
when(transactionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
```

Stub an empty stream:

```java
when(recurringTransactionRepository.streamDueRecurringTransactions(any()))
        .thenReturn(Stream.empty());
```

Verify batch boundaries with `ArgumentCaptor<List<Transaction>>` and `times(2)`:

```java
@SuppressWarnings("unchecked")
ArgumentCaptor<List<Transaction>> captor = ArgumentCaptor.forClass(List.class);
verify(transactionRepository, times(2)).saveAll(captor.capture());
List<List<Transaction>> batches = captor.getAllValues();

assertEquals(50, batches.get(0).size(), "first batch should be full (50)");
assertEquals(25, batches.get(1).size(), "second batch should contain remaining 25");

verify(transactionRepository, times(2)).flush();
```

Verify flush is called once per batch (including remainder):

```java
verify(transactionRepository, times(1)).flush();  // single batch + remainder
```

Verify no insert when source is empty:

```java
verify(transactionRepository, never()).saveAll(any());
verify(transactionRepository, never()).flush();
```

---

## Checklist

- [ ] JPA method returns `Stream<Entity>` with `@QueryHints` fetch size
- [ ] Stream query filters to only the rows that need processing
- [ ] Domain repository (source) exposes the `Stream` method
- [ ] Default repository (source) maps entity stream to model stream
- [ ] Target domain repository has `saveAll(List<T>)` for batch insert
- [ ] Target domain repository has `void flush()` for persistence context management
- [ ] Target default repository implements `flush()` with `jpaRepository.flush()` + `entityManager.clear()`
- [ ] Service opens stream in `try-with-resources`
- [ ] Service accumulates into a `List` and flushes at `BATCH_SIZE`
- [ ] Service calls `flush()` after every batch save AND after the final remainder
- [ ] Service flushes remainder after the loop
- [ ] `@Transactional` on the processing method (keeps JDBC connection open for the stream)
- [ ] Tests cover: happy path (creates records), empty source (no insert), batch boundaries (N × BATCH_SIZE + remainder), flush invocation count
