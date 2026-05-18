# Spark Deep Dive

## Why Spark Exists

MapReduce solved distributed compute but had one fatal flaw — every step wrote to disk.

A Berkeley PhD student (Matei Zaharia, 2009) asked:
> "What if instead of writing to disk between steps, we kept data in RAM?"

That was Spark. Same distributed idea as MapReduce. Everything else in memory.
Iterative jobs went from hours to minutes.

---

## The Mental Model

You write code as if processing a small dataset. Spark splits it across 100 machines and runs it in parallel.

```
You write:
    df.filter(amount > 1000).groupBy("merchant").sum("amount")

Spark does:
    Machine 1: filter its partition → partial sum for Amazon
    Machine 2: filter its partition → partial sum for Flipkart
    Machine 3: filter its partition → partial sum for Amazon
    ...
    Combine all partial results → final answer
```

You never think about distribution. Spark handles it.

---

## Architecture

```
┌─────────────────────────────────────────────┐
│                 Driver                       │
│  - Your main() runs here                    │
│  - Builds execution plan (DAG)              │
│  - Coordinates everything                   │
│  - Lives on one machine                     │
└──────────────────┬──────────────────────────┘
                   │ sends tasks
        ┌──────────┼──────────┐
        ▼          ▼          ▼
   ┌─────────┐ ┌─────────┐ ┌─────────┐
   │Executor1│ │Executor2│ │Executor3│
   │ Task1   │ │ Task2   │ │ Task3   │
   │(RAM+CPU)│ │(RAM+CPU)│ │(RAM+CPU)│
   └─────────┘ └─────────┘ └─────────┘
     Worker      Worker      Worker
     Node 1      Node 2      Node 3
```

- **Driver** — your program. Plans the job, divides work, collects results.
- **Executor** — worker process on each machine. Runs tasks, stores data in memory.
- **Task** — smallest unit of work. One task = one partition of data.
- **Cluster Manager** — YARN or Kubernetes. Allocates machines to Spark.

---

## The 3 Abstractions

Spark evolved through 3 generations. Know all three.

### RDD — Resilient Distributed Dataset (Gen 1, 2012)

Low-level. A distributed collection of objects. No schema.

```java
JavaRDD<String> lines = sc.textFile("hdfs://transactions.csv");
JavaRDD<String> filtered = lines.filter(line -> line.contains("Amazon"));
```

- Resilient = lost partition is recomputed from lineage, not replicated copy
- Use when: unstructured data, need full control
- Avoid when: DataFrame can do the job (it's faster due to optimizer)

### DataFrame (Gen 2, 2013)

Like a DB table — rows + named typed columns. Spark optimizes automatically.

```java
Dataset<Row> df = spark.read().csv("transactions.csv");
df.filter(col("merchant").eq("Amazon"))
  .groupBy("merchant")
  .sum("amount")
  .show();
```

- Has schema — Spark's Catalyst optimizer rewrites your query for efficiency
- 90% of real Spark code uses this

### Dataset (Gen 3, 2015)

DataFrame + compile-time type safety. Java/Scala only.

```java
Dataset<Transaction> txns = spark.read()
    .parquet("s3://data/transactions/")
    .as(Encoders.bean(Transaction.class));

txns.filter(t -> t.getAmount() > 1000)
    .groupByKey(Transaction::getMerchant, Encoders.STRING())
    .count()
    .show();
```

- Compile error if you use a wrong column name — catches bugs early
- Slightly more overhead than DataFrame due to serialization

**Which to use:**
```
Python  → DataFrame only (no Dataset in PySpark)
Java    → Dataset for type safety, DataFrame for quick ad-hoc jobs
Scala   → Dataset preferred
```

---

## Lazy Evaluation — The Most Important Concept

```java
Dataset<Row> df = spark.read().csv("2TB_transactions.csv");  // nothing runs
Dataset<Row> filtered = df.filter(col("amount").gt(1000));    // nothing runs
Dataset<Row> grouped = filtered.groupBy("merchant");          // nothing runs
Dataset<Row> result = grouped.sum("amount");                  // nothing runs

result.show();   // ← everything runs here
```

Spark does nothing until you call an **action**.

Why? So Spark can see the full pipeline and optimize before running:

```
Without lazy eval:
  Read 2TB → write 1.8TB temp → group → write 900GB temp → sum

With lazy eval (sees full plan first):
  Read → filter+group+sum in one pass, filter pushed to read step
  Reads far less data from disk
```

---

## Transformations vs Actions

| Type | What it does | Examples | Triggers execution? |
|---|---|---|---|
| Transformation | Describes what to do, returns new Dataset | `filter` `map` `groupBy` `join` `select` | No |
| Action | Triggers execution, returns a result | `show` `count` `collect` `write` `save` | Yes |

Every action triggers a full DAG execution from scratch — unless you cache.

---

## DAG — How Spark Plans a Job

When you call an action, Spark builds a DAG of all transformations, then splits it into **Stages** at shuffle boundaries.

```
Your code:
    df.filter(...).groupBy("merchant").sum(...).show()

Spark's DAG:

Stage 1 (no data movement):
    Read Parquet from S3
        │
    filter (pushed down — skips bad rows at read time)
        │
    partial sum per partition (each machine locally)
        │
    ↓↓↓ SHUFFLE — data moves across network ↓↓↓

Stage 2 (no data movement):
    final sum per merchant group
        │
    show
```

**Shuffle is the expensive step** — it moves data between machines over the network.
Every `groupBy`, `join`, `distinct` triggers a shuffle. Minimize them.

---

## Partitions — How Data Is Split

Each partition = one task = one core doing work.

```
2TB file → ~16,000 partitions of 128MB each
           16,000 tasks run in parallel
```

**Too few:** cores sit idle.
**Too many:** scheduling overhead kills throughput.

Rule of thumb: **2–4 partitions per CPU core** in your cluster.

```java
df.rdd().getNumPartitions();   // check current count

df.repartition(200);    // increase — triggers shuffle (expensive)
df.coalesce(10);        // decrease — no shuffle (just merges partitions)
```

Use `coalesce` before writing output — avoids writing thousands of tiny files to S3.

---

## Caching

```java
Dataset<Row> filtered = df.filter(col("amount").gt(1000)).cache();

filtered.count();                         // executes full plan, stores in RAM
filtered.show();                          // reads from cache — instant
filtered.groupBy("merchant").sum().show();  // reads from cache — instant
```

Without cache: every action re-reads 2TB from S3 and re-runs the filter.
With cache: filter runs once, lives in RAM.

**Cache when:** dataset used more than once in the same job.
**Don't cache when:** dataset used only once, or too large to fit in executor RAM (spills to disk — worse than not caching).

Storage levels:
```java
df.persist(StorageLevel.MEMORY_ONLY());         // default — RAM only
df.persist(StorageLevel.MEMORY_AND_DISK());     // spills to disk if RAM full
df.persist(StorageLevel.DISK_ONLY());           // disk only — rarely useful
```

---

## Data Skew — The Silent Performance Killer

GroupBy shuffles all same-key records to one executor.

If 80% of transactions are from "Amazon":
```
Executor 1 (Amazon): processes 800GB  ← bottleneck, others wait
Executor 2 (Flipkart): processes 50GB ← done in 2 min, idle
Executor 3 (Myntra): processes 30GB   ← done in 1 min, idle

Job takes as long as Executor 1.
```

**Fix — Salting:**
```java
// Add random salt to key before groupBy
Dataset<Row> salted = df.withColumn("salted_merchant",
    concat(col("merchant"), lit("_"), (rand().multiply(10).cast("int"))));

// First aggregation (distributed)
Dataset<Row> partial = salted.groupBy("salted_merchant").sum("amount");

// Strip salt, second aggregation (combines partials)
Dataset<Row> result = partial
    .withColumn("merchant", split(col("salted_merchant"), "_").getItem(0))
    .groupBy("merchant").sum("amount");
```

---

## Broadcast Join — Avoiding Shuffle for Small Tables

Normal join = both tables shuffle across network (expensive).

If one table is small (< a few hundred MB), broadcast it to every executor:

```java
Dataset<Row> txns = spark.read().parquet("s3://transactions/");      // 2TB
Dataset<Row> merchants = spark.read().parquet("s3://merchants/");    // 10MB

// Broadcast small table — no shuffle
Dataset<Row> result = txns.join(
    broadcast(merchants),
    txns.col("merchant_id").eq(merchants.col("id"))
);
```

Each executor has a local copy of the merchants table. No data movement.
**This is the single biggest Spark optimization in practice.**

---

## Real Job — Daily Merchant Settlement (Your Domain)

```java
SparkSession spark = SparkSession.builder()
    .appName("DailySettlement")
    .getOrCreate();

// Read yesterday's transactions (only that partition — not full history)
Dataset<Row> txns = spark.read()
    .parquet("s3://payments/transactions/date=2024-01-15/");

// Read merchant config (small — broadcast it)
Dataset<Row> merchantConfig = spark.read()
    .parquet("s3://config/merchants/");

// Compute net payable per merchant
Dataset<Row> settlement = txns
    .filter(col("status").eq("SUCCESS"))
    .join(broadcast(merchantConfig), "merchant_id")
    .groupBy("merchant_id", "merchant_name", "bank_account")
    .agg(
        sum("amount").as("gross_amount"),
        sum("platform_fee").as("total_fees"),
        count("txn_id").as("txn_count")
    )
    .withColumn("net_payable", col("gross_amount").minus(col("total_fees")));

// Write results to Postgres (small output — few thousand rows)
settlement.write()
    .format("jdbc")
    .option("url", jdbcUrl)
    .option("dbtable", "merchant_settlement_2024_01_15")
    .option("batchsize", "5000")
    .mode(SaveMode.Overwrite)
    .save();
```

Input: 2TB raw transactions. Output: ~10K rows. Payout service reads those 10K rows.

---

## Spark vs MapReduce — The Real Difference

| | MapReduce | Spark |
|---|---|---|
| Intermediate data | Written to disk | Kept in RAM |
| Multi-step jobs | N steps = N-1 disk writes | All steps in one DAG, one pass |
| API | Verbose Java (Mapper/Reducer classes) | Concise DataFrame/SQL API |
| ML support | None built-in | MLlib (built-in) |
| Streaming | No | Spark Streaming (micro-batch) |
| Speed | Baseline | 10–100x faster |

---

## Interview Questions

**Q1 (Basic): What is lazy evaluation in Spark and why does it matter?**

Strong answer:
> Spark doesn't execute transformations immediately. It builds a DAG of all operations and executes only when an action is called. This allows the Catalyst optimizer to see the full pipeline and reorder/combine operations — for example, pushing filters to the data source so less data is read. Without lazy eval, each step would execute immediately with no opportunity for optimization.

---

**Q2 (Medium): What is a shuffle and why is it expensive?**

Strong answer:
> A shuffle happens when data needs to be redistributed across executors — triggered by groupBy, join, distinct. It involves serializing partition data, sending it over the network to the right executor, and deserializing it. This is the most expensive operation in Spark — it involves disk I/O, network I/O, and serialization. In a 100-node cluster processing 1TB, a shuffle can move hundreds of GBs across the network. Optimize by: filtering before grouping, using broadcast joins for small tables, and avoiding unnecessary distinct/repartition calls.

---

**Q3 (Hard): Your Spark job takes 4 hours. Walk me through how you debug it.**

Strong answer — in order:
1. **Check Spark UI** — look at stage timeline. Which stage is slowest?
2. **Data skew** — one task taking 10x longer than others? One key dominates. Fix with salting.
3. **Shuffle size** — large shuffle = too much data moving. Filter earlier, use broadcast joins.
4. **Partition count** — too few partitions = idle cores. Too many = scheduling overhead. Check with `getNumPartitions()`.
5. **Spill to disk** — executors running out of memory, writing to disk. Fix: increase executor memory or reduce partition size.
6. **Small files problem** — reading thousands of tiny S3 files. Each file = one task overhead. Fix: compact files or use `wholeTextFiles`.
7. **GC pressure** — executor GC time high in Spark UI. Fix: use Kryo serializer, reduce object creation, use off-heap memory.