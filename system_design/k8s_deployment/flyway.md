# Flyway — Complete Notes

## What is Flyway?
Database migration tool that tracks and runs SQL scripts in order. Solves the problem of keeping DB schema in sync across environments (dev, staging, prod).

---

## How it Works Internally

On app startup:
1. Spring Boot creates `DataSource` bean
2. Detects Flyway on classpath
3. Passes `DataSource` to Flyway
4. `Flyway.migrate()` runs automatically
5. Flyway gets a JDBC connection, checks `flyway_schema_history`
6. Scans `db/migration/` for pending scripts
7. Runs them in order, records in history table
8. Releases connection

**No proxy. Just JDBC + ordered SQL execution + a history table.**

---

## Migration Types

| Type | Prefix | Behaviour |
|---|---|---|
| Versioned | `V1__name.sql` | Runs once, never again |
| Undo | `U1__name.sql` | Reverses V1 — paid feature |
| Repeatable | `R__name.sql` | Runs every time file changes |

> For most projects you only use **V migrations**.

---

## Naming Convention — Strict

```
V{version}__{description}.sql
```

- Double underscore between version and description
- Version can be `1`, `1.1`, `1.2`, `2` etc.
- Examples:
  - `V1__create_users.sql`
  - `V2__create_employees.sql`
  - `V1.1__add_email_index.sql`

---

## flyway_schema_history Table

Flyway auto-creates this in your database. You never touch it.

| Column | Description |
|---|---|
| `installed_rank` | Execution order |
| `version` | V1, V2 etc |
| `description` | Part after `__` in filename |
| `script` | Full filename |
| `checksum` | Hash of file — detects tampering |
| `installed_by` | DB user who ran it |
| `installed_on` | Timestamp |
| `execution_time` | ms taken |
| `success` | true/false |

---

## Migration States

| State | Meaning |
|---|---|
| Pending | Script exists, not yet run |
| Applied | Ran successfully |
| Failed | Ran but failed — must fix before app starts |
| Missing | Ran before but file is now deleted — dangerous |

---

## Critical Rules

```
❌ Never edit an already-run migration — checksum will fail, app won't start
❌ Never delete a migration file that already ran
✅ Always add a new migration file for schema changes
✅ Treat migration files as immutable once committed
```

---

## Adding Flyway to a New Project

### 1. Add dependency (Gradle)
```groovy
implementation 'org.flywaydb:flyway-core'
implementation 'org.flywaydb:flyway-database-postgresql'  // for PostgreSQL
runtimeOnly 'org.postgresql:postgresql'
```

### 2. Configure datasource in `application.yml`
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: postgres
    password: secret
  flyway:
    enabled: true
    locations: classpath:db/migration
```

### 3. Create migration files
```
src/main/resources/db/migration/
├── V1__create_users.sql
└── V2__create_employees.sql
```

### 4. Write your SQL
```sql
-- V1__create_users.sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);
```

Spring Boot auto-runs Flyway on startup. No extra code needed.

---

## Adding Flyway to an Existing Project (Legacy)

### The Problem
Legacy DB already has tables. Flyway expects to manage schema from scratch — conflict.

### Solution — Baseline

Tell Flyway "everything before this point already exists":

```yaml
spring:
  flyway:
    baseline-on-migrate: true
    baseline-version: 1
```

Or via CLI:
```bash
flyway baseline -baselineVersion=1
```

- Creates `flyway_schema_history` with a baseline entry
- All future migrations run from V2 onwards
- V1 is assumed already applied — Flyway won't touch existing schema

### Steps for legacy migration
1. Enable `baseline-on-migrate: true`
2. Write `V1__baseline.sql` as a snapshot of current schema (for documentation)
3. All new changes go in V2, V3 onwards
4. Flyway tracks from V2 forward

---

## Multiple Datasources

Flyway creates a separate `flyway_schema_history` in each DB it manages.

```yaml
spring:
  flyway:
    url: jdbc:postgresql://localhost:5432/employeedb
    locations: classpath:db/migration/employeedb
```

Separate migration folders per DB:
```
db/migration/
├── employeedb/
│   ├── V1__create_users.sql
│   └── V2__create_employees.sql
└── auditdb/
    └── V1__create_audit_logs.sql
```

---

## Flyway vs Other Tools

| Tool | Format | Rollback | Complexity | Best For |
|---|---|---|---|---|
| **Flyway** | SQL only | Paid | Low | Spring Boot, simple SQL |
| **Liquibase** | XML/YAML/JSON/SQL | Free | Medium | Complex branching, rollback needed |
| **Hibernate ddl-auto** | None (auto) | None | Zero | Local dev only — never production |
| **Redgate** | Visual | Yes | High | Enterprise, compliance |

### When to use what
- Spring Boot + SQL + simple → **Flyway**
- Need rollback + complex branching → **Liquibase**
- Local dev only → **Hibernate ddl-auto: create-drop**
- Enterprise, audit compliance → **Redgate**

---

## Common Interview Questions

### Conceptual

**Q: What happens if you modify an already-applied migration?**
Flyway stores a checksum of every file. On next startup it recalculates checksums and compares. If mismatch → throws `FlywayException`, app fails to start. Fix: restore original file or use `flyway repair`.

**Q: What is `flyway repair` used for?**
Fixes `flyway_schema_history` table — removes failed migration entries or recalculates checksums after manual DB changes. Use carefully.

**Q: Difference between Flyway and Liquibase?**
Flyway is SQL-first, simpler, less features. Liquibase supports XML/YAML, has free rollback, better for complex multi-branch workflows. Flyway is easier to reason about.

**Q: How does Flyway handle concurrent deployments?**
Flyway uses a DB-level lock on `flyway_schema_history` during migration. Only one instance runs migrations at a time — safe for multi-pod Kubernetes deployments.

**Q: What is a repeatable migration?**
`R__name.sql` — runs every time its checksum changes. Useful for views, stored procedures, functions that you want to redefine without versioning.

### System Design

**Q: How would you handle DB migrations in a zero-downtime deployment?**
Key principle: migrations must be backward compatible with the old version of the app.
- Never drop a column in the same release that stops using it
- Add columns as nullable first
- Use expand-contract pattern:
  1. **Expand** — add new column (nullable), deploy new code that writes to both old and new
  2. **Migrate** — backfill data
  3. **Contract** — remove old column in a future release

**Q: How do you manage Flyway in a microservices architecture?**
Each service owns its own DB and its own Flyway migrations. Never share a DB between services. Each service's migration folder is independent.

**Q: What happens if a migration fails halfway through?**
Depends on DB:
- PostgreSQL — DDL is transactional, so partial migration is rolled back automatically
- MySQL — DDL is not transactional, partial changes remain, manual cleanup needed
- Failed entry recorded in `flyway_schema_history` with `success=false`
- App won't start until fixed — use `flyway repair` after fixing

**Q: How would you introduce Flyway into a 5-year-old legacy monolith?**
1. Take a DB snapshot as `V1__baseline.sql`
2. Set `baseline-on-migrate: true`, `baseline-version: 1`
3. All future schema changes go in V2+
4. Enforce in CI — no manual DB changes allowed going forward
5. Gradually document existing schema through migration files

**Q: How do you test migrations?**
- Run against a real PostgreSQL instance in CI (use Testcontainers)
- Never test against H2 — SQL dialects differ
- Test both the migration itself and rollback strategy
- Use `@FlywayTest` or reset DB state between test runs with Testcontainers

---

## Useful Config Properties

```yaml
spring:
  flyway:
    enabled: true                          # toggle on/off
    locations: classpath:db/migration      # where to find scripts
    baseline-on-migrate: true             # for legacy projects
    baseline-version: 1                   # baseline version number
    out-of-order: false                   # disallow out-of-order migrations
    validate-on-migrate: true             # checksum validation (default true)
    clean-disabled: true                  # ALWAYS true in prod — prevents drop all
    table: flyway_schema_history          # custom history table name
```

> ⚠️ `spring.flyway.clean-disabled=true` — always set this in production. `flyway clean` drops all objects in the schema.
