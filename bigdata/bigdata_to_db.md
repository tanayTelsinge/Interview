# Big Data → Database — Storage Patterns & System Design

## The Core Confusion

People hear "Spark processes 2TB" and think:
> "That 2TB has to go somewhere... Postgres is going to die."

Wrong mental model.

> Spark processes 2TB of raw data. The **output** is a few thousand aggregated rows.

```
INPUT → Spark → OUTPUT
2TB raw transactions    →    merchant_id | total_txns | total_amount
10M rows                     amazon      | 50,000     | ₹2.1Cr
                             flipkart    | 30,000     | ₹80L
                             ...
                             ~5,000 rows. Postgres handles this fine.
```

Spark is a reduction machine. Raw data in, small summarized results out.

---

## The Two Worlds — Never Compete

```
Big Data Stack                    Your App Stack
──────────────                    ──────────────
Stores raw data        vs.        Stores results only
Processes TBs          vs.        Serves KBs
Runs in minutes        vs.        Responds in ms
Batch / async          vs.        Real-time / sync
```

They hand off — Spark does heavy lifting offline, your DB serves users fast.

---

## The Pre-Computation Pattern

The foundation of almost every analytics system:

```
S3/HDFS (raw, 2TB)
    │
    ▼
Spark job (runs nightly at 2am)
    │  processes, aggregates, reduces
    ▼
Postgres / Redis / Cassandra (stores summary rows)
    │
    ▼
Spring Boot API (reads fast, <10ms)
```

Your API never touches the 2TB. It reads a pre-computed row.

This is how every fintech dashboard works — Razorpay, Stripe, Paytm.
The "live" numbers you see are almost always pre-computed minutes or hours ago.

---

## Choosing the Right DB for Spark Output

Not everything goes to Postgres. Rule of thumb:

| Output size | Access pattern | Right DB | Why |
|---|---|---|---|
| < 5M rows | SQL queries, joins | Postgres | Simple, already in your stack |
| High read throughput, key lookups | `GET merchant:123:summary` | Redis | Sub-millisecond, handles 100K req/s |
| 50M–1B rows, wide rows | Timeseries, per-user data | Cassandra | Scales horizontally, fast writes |
| BI dashboards, ad-hoc SQL | Analysts writing SQL | BigQuery / Redshift | Columnar, fast aggregations |
| Search + filter + aggregations | Full-text, faceted search | Elasticsearch | Built for search patterns |
| Append-only events | Time-ordered, immutable | ClickHouse | Analytical queries on raw events |

---

## How Spark Writes to Each DB

**Postgres (JDBC)**
```java
df.write()
  .format("jdbc")
  .option("url", "jdbc:postgresql://host:5432/payments")
  .option("dbtable", "merchant_daily_summary")
  .option("batchsize", "10000")   // bulk insert, not row by row
  .mode(SaveMode.Overwrite)
  .save();
```

**Redis (via Spark-Redis connector)**
```java
// Each row becomes a Redis hash
df.write()
  .format("org.apache.spark.sql.redis")
  .option("table", "merchant_summary")
  .option("key.column", "merchant_id")
  .save();
// Stored as: merchant_summary:amazon → {total_txns: 50000, amount: 210000000}
```

**Cassandra**
```java
df.write()
  .format("org.apache.spark.sql.cassandra")
  .option("keyspace", "payments")
  .option("table", "merchant_summary")
  .save();
```

**BigQuery (GCP — your stack)**
```java
df.write()
  .format("bigquery")
  .option("table", "payments.merchant_summary")
  .save();
```

---

## Production Write Patterns

Even with summarized data, a Spark job writing 500K rows can spike your DB.
These are the patterns production systems actually use:

---

### Pattern 1 — Staging Table Swap (Zero Downtime)

```
Spark writes to merchant_summary_staging (shadow table, no live traffic)
    │
    ▼
Write completes (takes 5 min, nobody cares)
    │
    ▼
Atomic rename in Postgres:

BEGIN;
  ALTER TABLE merchant_summary RENAME TO merchant_summary_old;
  ALTER TABLE merchant_summary_staging RENAME TO merchant_summary;
  DROP TABLE merchant_summary_old;
COMMIT;
```

Your API reads from `merchant_summary` throughout. Zero interruption.
Used by: almost every analytics pipeline in production.

---

### Pattern 2 — Write-Through Cache

```
Spark job finishes
    │
    ├──► Write summary rows to Postgres (source of truth)
    └──► Write same data to Redis (cache for API)

API always reads Redis first:
    Redis hit  → return immediately (<1ms)
    Redis miss → read Postgres, populate Redis, return
```

Cache TTL matches your batch frequency. If Spark runs every hour, TTL = 1 hour.

---

### Pattern 3 — Dual Write with Feature Flag

When migrating from old DB to new DB (e.g. Postgres → Cassandra):

```
Spark writes to both Postgres and Cassandra
    │
API reads from Postgres (old)
    │
Gradually shift traffic to Cassandra via feature flag
    │
Once confident → cut over, stop writing to Postgres
```

No big-bang migration. Rollback is just flipping the flag.

---

### Pattern 4 — Event-Driven Refresh (Instead of Scheduled)

Instead of running Spark every night at 2am:

```
Payment completed → Kafka event → triggers Spark micro-job
                                  (processes only that merchant's data)
                                      │
                                  Updates only that merchant's row in Postgres
```

More complex, but results are near real-time instead of next-day.
Used when business requires "updated in last 15 minutes" not "updated last night".

---

## System Design Questions

---

**Q1: Design Razorpay's merchant dashboard — shows revenue, transaction count, success rate for any date range**

```
Write path:
Payment API → Kafka → Spark (hourly job)
                      - aggregates by merchant + date
                      - computes success rate
                      → Postgres (merchant_daily_stats table)

Read path:
Merchant hits dashboard
    → API queries Postgres:
      SELECT * FROM merchant_daily_stats
      WHERE merchant_id = ? AND date BETWEEN ? AND ?
    → Cached in Redis (key: merchant:123:stats:2024-01)
```

Key decisions:
- Pre-aggregate by day in Spark. Don't let API query raw transactions.
- Redis cache per merchant per month. Dashboard loads in <50ms.
- Postgres partitioned by date — range queries fast even at 100M rows.
- Spark runs hourly not nightly — data fresh enough for merchants.

---

**Q2: Design a system to detect and report high-value transaction patterns for RBI compliance**

```
All transactions → S3 (immutable, partitioned by year/month/day)
                       │
                   Spark job (runs on 1st of month)
                   - reads previous month's partition only (not 7 years)
                   - identifies: txns > ₹10L, cross-border, suspicious velocity
                   - generates report in RBI's required format
                       │
                   Output:
                   ├── S3 archive (immutable, audit trail)
                   └── Postgres (report_status table — sent/pending/failed)
                           │
                       Compliance dashboard API reads this
```

Key decisions:
- Raw data on S3, never Postgres — 7 years of data can't live in a DB cheaply.
- Partition by date — Spark reads only 1 month, not full 7 years.
- Immutable S3 raw data — audit requirement, never overwrite.
- Report metadata in Postgres — small, fast, auditable.

---

**Q3: Design a real-time leaderboard for a UPI cashback campaign (top merchants by transaction volume)**

```
Payment event → Kafka
                  │
                  ▼
               Flink (stateful, real-time)
               - maintains running count per merchant in memory
               - updates Redis sorted set every 5 seconds:
                 ZADD leaderboard <txn_count> <merchant_id>
                  │
                  ▼
               Redis Sorted Set
               ZREVRANGE leaderboard 0 9 → top 10 merchants instantly

API:
GET /leaderboard → reads Redis → <1ms response
```

Also runs nightly Spark job to persist final leaderboard to Postgres for historical records.

Key decisions:
- Redis sorted set is perfect for leaderboards — O(log N) updates, O(log N + K) top-K reads.
- Flink aggregates in memory, pushes to Redis every 5s — not per-event (would overwhelm Redis).
- Spark for historical — Redis is ephemeral, Postgres is the record.

---

**Q4 (Hard): Your nightly Spark job writes 2M rows to Postgres. After the job, API latency spikes from 20ms to 800ms for 30 minutes. Why and how do you fix it?**

Root causes to walk through:
1. **Table bloat** — Overwrite mode drops + recreates table. Postgres autovacuum runs, locks pages. Fix: use staging table swap instead of overwrite.
2. **Index rebuild** — 2M inserts invalidate indexes. Postgres rebuilds them post-insert, high I/O. Fix: drop indexes before bulk insert, recreate after. Or use COPY instead of INSERT.
3. **Connection pool exhaustion** — Spark opens many JDBC connections simultaneously. Fix: limit Spark executor JDBC connections, use a connection pool like PgBouncer in front of Postgres.
4. **Shared buffer contention** — Bulk write floods Postgres shared_buffers, evicts hot API query pages. Fix: schedule Spark writes during true off-peak, or use a read replica for API traffic.

Best fix in production: **API always reads from read replica. Spark writes to primary. They never compete.**

```
Spark job → writes to → Postgres Primary
                              │
                         replication
                              │
                              ▼
API reads from → Postgres Read Replica  (unaffected by writes)
```
