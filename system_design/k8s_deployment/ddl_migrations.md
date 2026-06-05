# DDL Migration Patterns

DB and code deploy separately — old and new code run simultaneously. Every pattern ensures neither breaks.

---

## ADD Column

```
Nullable / static default  → 1 release
NOT NULL, no default       → 3 releases
```

```sql
-- 1 release
ALTER TABLE users ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'active';

-- 3 releases: add nullable → backfill → constrain
ALTER TABLE users ADD COLUMN phone VARCHAR(20);
UPDATE users SET phone = 'unknown' WHERE phone IS NULL;
ALTER TABLE users ALTER COLUMN phone SET NOT NULL;
```

```java
// 1 release
@Column(nullable = false)
private String status = "active";

// 3 releases: R1 no annotation → R2 add @NotNull
@NotNull @Column(nullable = false)
private String phone;
```

---

## RENAME Column

```
R1 → ADD new column + dual write (old + new) in Java
R2 → backfill old → new in DB + read from new in Java
R3 → DROP old column + remove old field in Java
```

```sql
ALTER TABLE users ADD COLUMN username VARCHAR(100); -- R1
UPDATE users SET username = user_name;              -- R2
ALTER TABLE users DROP COLUMN user_name;            -- R3
```

```java
// R1 — dual write
public void setName(String name) {
    this.userName = name;
    this.username = name;
}

// R3 — only new field remains
@Column(name = "username")
private String username;
```

---

## CHANGE Type (e.g. INT → BIGINT)

```
R1 → ADD new column (new type) + dual write in Java
R2 → backfill → DROP old → RENAME new → old + use new type in Java
```

```sql
ALTER TABLE users ADD COLUMN age_new BIGINT;         -- R1
UPDATE users SET age_new = CAST(age AS BIGINT);      -- R2
ALTER TABLE users DROP COLUMN age;                   -- R2
ALTER TABLE users RENAME COLUMN age_new TO age;      -- R2
```

```java
// R1 — dual write
public void setAge(Integer age) {
    this.age = age;
    this.ageNew = age.longValue();
}

// R2 — single field, new type
private Long age;
```

---

## DELETE Column

```
R1 → remove field from Java
R2 → DROP column from DB
```

```sql
ALTER TABLE users DROP COLUMN phone;  -- R2 only
```

---

## DML (Data Changes)

Low risk — changes data, not structure.

```
INSERT / SELECT → always safe
UPDATE / DELETE → safe, but batch large operations
```

```sql
-- never do this on large tables
UPDATE orders SET status = 'active';

-- batch instead
UPDATE orders SET status = 'active' WHERE id IN (
  SELECT id FROM orders WHERE status IS NULL LIMIT 1000
);
```

---

## Golden Rules

```
Adding   → DB first, then Java
Deleting → Java first, then DB
Renaming → always 3 releases, never direct rename
Changing → add new column, never alter directly
Batching → chunk large UPDATE/DELETE, never one query
Flyway   → never edit a migration that already ran
```
