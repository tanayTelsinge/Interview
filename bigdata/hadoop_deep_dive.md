# Hadoop Deep Dive

---

## Design Decision — When to Use Hadoop

### Start With One Question

> **Can your database handle it?**

```
Data < 100GB, queries < 10s?
    → Stay with Postgres/MySQL. Don't over-engineer.

Data in TBs, queries take minutes/hours?
    → You need distributed storage + compute. Enter Hadoop/Spark.
```

---

### The 3 Scenarios Where You Reach for Hadoop

**Scenario 1 — Your data outgrows your database**

You're at a payments company. 3 years in, transactions table hits 500GB. Queries slow down. You add indexes, read replicas, partitioning. Buys you time.

2 more years. 3TB. Nothing helps.

That's the trigger. Move historical data to HDFS/S3. Run Spark for analytics. Keep only recent 3 months in Postgres for your live API.

```
Postgres       → hot data (last 3 months, fast API queries)
HDFS/S3+Spark  → cold data (full history, batch analytics)
```

**Scenario 2 — Your processing is too slow for one machine**

You need to train a fraud ML model on 5 years of transactions.

Single machine: loads 2TB → crashes. Or takes 12 hours.

Spark on 50 machines: each processes 40GB → done in 20 minutes.

Trigger: job runtime is unacceptable on a single machine.

**Scenario 3 — You have a firehose of events with multiple consumers**

10M payment events/day. You need fraud detection in real-time, daily merchant reports, and ML model retraining — all from the same data.

One pipeline can't do all three. You need a distributed event + processing layer.

Trigger: high-velocity data with multiple consumers needing different processing.

---

### The Decision Tree

```
Data > 1TB  OR  job takes > 30min on one machine?
        │
       YES
        │
        ▼
Need real-time (<1s) results?
    │               │
   YES              NO
    │               │
    ▼               ▼
Kafka + Flink    On-prem or cloud?
(real-time)       │           │
               On-prem      Cloud
                  │           │
                  ▼           ▼
               Hadoop      S3 + Spark
             (HDFS+Spark)  (no cluster needed)
```

---

### What Hadoop Is NOT Good For

| Use case | Don't use Hadoop — use this instead |
|---|---|
| Low-latency queries (<100ms) | Redis, Elasticsearch |
| Transactional data (ACID) | Postgres, MySQL |
| Small datasets (<100GB) | Postgres + good indexing |
| Real-time streaming | Kafka + Flink |
| Simple file storage | S3/GCS directly |

Hadoop's sweet spot is **large-scale batch processing**. Outside that, it's the wrong tool.

---

### In Your Fintech World — Concrete Decisions

| Problem | Wrong choice | Right choice |
|---|---|---|
| Merchant dashboard (last 30 days) | Spark job on every request | Postgres with indexes |
| Monthly RBI compliance report (5yr data) | Postgres query | Spark batch job on S3 |
| Fraud detection on live transaction | Hadoop MapReduce | Kafka + Flink |
| Retraining fraud ML model weekly | Single machine script | PySpark on Dataproc/EMR |
| Raw transaction archive (7 years) | Postgres | S3/HDFS — cheap, durable |

**The pattern: Hadoop/Spark for big batch. Kafka/Flink for real-time. Postgres for your API.**

---

## System Design Questions (Hadoop Context)

---

**Q1: Design a merchant analytics platform for a payments company**
> Data: 10M transactions/day. Merchants need daily/weekly/monthly reports.

Strong answer:
```
Transactions → Kafka → S3 (raw storage, Parquet format)
                           │
                        Spark (nightly batch job)
                        - aggregates per merchant
                        - computes daily/weekly/monthly rollups
                           │
                        Postgres (pre-computed results)
                           │
                        Spring Boot API ← merchant hits dashboard
```
Key decisions to mention:
- Why not query S3 directly on every request? Latency — Spark jobs take minutes.
- Why Parquet? Columnar format — Spark reads only the columns it needs, 10x faster.
- Why Postgres for final results? API needs <100ms — Spark can't do that.

---

**Q2: Design a fraud detection system processing 10M transactions/day**
> Real-time blocking required. Also needs ML model retrained weekly.

Strong answer:
```
Payment API → Kafka (payment-events topic)
                │
                ├──► Flink (real-time, stateful)
                │    - checks velocity (5 txns in 60s from same card?)
                │    - scores against ML model loaded in memory
                │    - blocks transaction in <200ms
                │
                └──► S3 (raw event archive)
                          │
                       Spark (weekly batch)
                       - generates training features
                       - retrains ML model
                       - pushes new model to Flink
```
Key decisions to mention:
- Why Flink not Spark for real-time? Flink is true streaming (<200ms). Spark Streaming is micro-batch (seconds).
- Why keep raw events in S3? Model retraining needs historical data. Also for audit/compliance.
- Stateful in Flink: maintains per-card transaction history in memory for velocity checks.

---

**Q3: Design a data pipeline for RBI regulatory reporting**
> 7 years of transaction history. Monthly reports. Strict audit trail required.

Strong answer:
```
All transactions → append to S3 (immutable, partitioned by date)
                       │
                    Spark job (runs on 1st of every month)
                    - reads last month's data
                    - aggregates: txn volume, value, category
                    - generates report in required format
                       │
                    Output → S3 (archived) + sent to RBI portal
```
Key decisions to mention:
- Immutable storage on S3: never overwrite raw data — audit requirement.
- Partition by date: `s3://data/transactions/year=2024/month=01/` — Spark reads only relevant partition, not 7 years of data.
- Why not a database for 7 years? Cost + query patterns — S3 is 10x cheaper, and you only query monthly.

---

**Q4 (Hard): Your Spark job that processes daily transactions is taking 4 hours. How do you debug and fix it?**

Strong answer — walk through in this order:
1. **Check for data skew** — one partition processing 80% of data while others idle. Fix: repartition by a better key or use salting.
2. **Check shuffle size** — large shuffles mean too much data moving across network. Fix: filter/aggregate before shuffle, use broadcast joins for small tables.
3. **Check partition count** — too few partitions = underutilized cluster. Rule of thumb: 2-4 partitions per CPU core. Fix: `repartition(n)`.
4. **Check for small files** — thousands of tiny files on S3/HDFS kills performance (metadata overhead). Fix: coalesce before writing.
5. **Check spill to disk** — executors running out of memory, spilling to disk = slow. Fix: increase executor memory or reduce partition size.

---

## Where It Fits for a Fullstack Dev

You build the system that **produces** data.
Hadoop is the system that **stores and processes** that data at scale.

```
Your Spring Boot API  ──── writes ────► HDFS / S3  ────► Spark/MapReduce  ────► back to your API
```

You'll care about this in:
- System design interviews ("design a fraud detection system for 10M txns/day")
- When your team has a data pipeline and your service feeds into it
- When your API needs to serve analytics computed on massive datasets

---

## The Origin Story

2004. Google is crawling the entire internet.

Data so big, no single machine can store it. Even if it could — processing it would take months.

Google publishes two papers:
- **GFS** — how to store files across hundreds of machines
- **MapReduce** — how to process those files in parallel

Yahoo engineers read them, build open-source versions:
- GFS → **HDFS**
- MapReduce → **MapReduce**

Bundle them → **Hadoop** (named after a toy elephant).

---

## HDFS — The Story

You have a 1TB file of payment transactions. One machine can't hold it.

HDFS says: *"Cut it into 128MB chunks. Scatter them across 100 machines."*

But what if one machine crashes?

*"Copy every chunk to 3 different machines. Machine dies? Two copies survive."*

Who tracks which chunk is where?

*"One special machine — the NameNode — acts as the index. It holds the map of every block to every machine, entirely in RAM."*

```
1TB file → split into blocks
    │
    ├── Block 1 (128MB) → machines 4, 7, 23   (3 copies)
    ├── Block 2 (128MB) → machines 1, 9, 44   (3 copies)
    ├── Block 3 (128MB) → machines 2, 8, 31   (3 copies)
    └── ...
```

**The one risk:** NameNode is a single machine. It dies → nobody knows where anything is.
Modern Hadoop fixes this with a hot standby NameNode + ZooKeeper.

### How a Write Actually Flows

```
1. Your app → NameNode: "I want to write a 500MB file"
2. NameNode → Your app: "Write block1 to DN5, block2 to DN12, block3 to DN3"
3. Your app writes to DN5 → DN5 replicates to DN12 → DN12 replicates to DN3
4. ACK comes back → NameNode updates its index
```

NameNode never touches actual data. It only manages the index.

---

## MapReduce — The Story

Data is spread across 100 machines. You want: *"Total transaction amount per merchant."*

Old way: Pull all 1TB to one machine, loop through it. Takes hours.

MapReduce says: *"Don't move data to the code. Move the code to the data."*

Send a small program to every machine. Each runs on its local chunks only.

**Step 1 — Map** *(every machine does this in parallel, on its own blocks)*
```
Machine 4 sees its local data and emits key-value pairs:
  "Amazon, ₹500"   → ("Amazon", 500)
  "Flipkart, ₹200" → ("Flipkart", 200)
  "Amazon, ₹300"   → ("Amazon", 300)
```

**Step 2 — Shuffle** *(framework groups by key automatically)*
```
All "Amazon" pairs   → sent to Reducer R1
All "Flipkart" pairs → sent to Reducer R2
```

**Step 3 — Reduce** *(aggregate)*
```
R1: ("Amazon", [500, 300, 700...])   → ("Amazon", ₹2.1Cr)
R2: ("Flipkart", [200, 400...])      → ("Flipkart", ₹80L)
```

Parallel across 100 machines. What took hours now takes minutes.

### Why MapReduce Died

Every step writes to disk:

```
Map → 💾 disk → Shuffle → 💾 disk → Reduce → 💾 disk → done
```

A fraud ML model needs 50 training iterations:
```
50 rounds × disk write of TBs = job runs for 8 hours
```

This is why Spark was born — it keeps intermediate data in RAM.

### MapReduce in Java (why nobody writes it anymore)

```java
// You need a Mapper class...
public class TxnMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
    public void map(LongWritable key, Text value, Context context) {
        String[] fields = value.toString().split(",");
        context.write(new Text(fields[2]), new IntWritable(1));
    }
}

// ...a Reducer class...
public class TxnReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
    public void reduce(Text key, Iterable<IntWritable> values, Context context) {
        int sum = 0;
        for (IntWritable val : values) sum += val.get();
        context.write(key, new IntWritable(sum));
    }
}

// ...a Job config class
// ...key/value type declarations
// ~60 lines total
```

Spark does the exact same thing in 2 lines:

```java
// Spark (Java)
dataset.groupBy("merchant").sum("amount").show();

// PySpark
df.groupBy("merchant").sum("amount").show()
```

Same distributed Map → Shuffle → Reduce happening under the hood.
You just don't see it. Spark handles it — and keeps everything in memory.

---

## YARN — One Line Explanation

Before YARN: MapReduce owned the entire cluster. Nothing else could run.

After YARN: Cluster is a shared pool. Spark, Hive, Flink all share one cluster.

YARN = the OS scheduler for a Hadoop cluster.

---

## Is Hadoop Dead?

| Component | Status |
|---|---|
| HDFS | Alive on-prem. Being replaced by S3/GCS in cloud. |
| MapReduce | Dead. Spark killed it. |
| YARN | Alive on-prem. Being replaced by Kubernetes. |

**Why learn it anyway:**
- Banks and telcos still run on-prem Hadoop clusters
- Every modern tool (Spark, Hive, Flink) was born in this ecosystem — knowing Hadoop explains their design
- HDFS internals + MapReduce flow are asked in data engineering interviews

---

## Fullstack Dev — Real Touch Points

**1. Your service feeds the pipeline**
```java
// You publish a payment event → Kafka → Spark reads it → computes analytics
kafkaTemplate.send("payment-events", new PaymentEvent(txnId, amount, merchant));
```

**2. Your API reads results computed by Spark**
```java
// Spark ran a nightly batch job, wrote merchant summaries to Postgres
// Your API just reads them
@GetMapping("/merchants/{id}/analytics")
public MerchantAnalytics getAnalytics(@PathVariable String id) {
    return analyticsRepository.findByMerchantId(id);
}
```

**3. System design — you architect the full pipeline**
```
Spring Boot API
    │
    ▼
Kafka
    ├──► Flink (real-time fraud scoring, <200ms)
    └──► S3 / HDFS (raw storage)
              │
              ▼
           Spark (nightly batch: reports, ML retraining)
              │
              ▼
           Postgres / Redis  ◄── your API reads from here
```

---

## Interview Questions

**Q1: What is the role of NameNode vs DataNode?**
> NameNode = index. Stores metadata (what blocks exist, where) in RAM. Never holds actual data.
> DataNode = storage. Holds actual blocks. Sends heartbeat every 3s.
> NameNode dies → cluster inaccessible. Fixed with Active/Standby NameNode + ZooKeeper.

Common mistake: Saying NameNode stores data.

---

**Q2: Why did Spark replace MapReduce?**
> MapReduce writes intermediate results to disk after every phase. A 10-step job = 9 disk writes of TBs.
> Spark keeps intermediate data in RAM across all steps. Same distributed logic, no disk in between.
> Result: ~100x faster for iterative jobs. ML training goes from hours to minutes.

---

**Q3 (Hard): What is the data skew problem in MapReduce/Spark and how do you fix it?**
> If 80% of transactions belong to "Amazon", the Shuffle sends 80% of data to one Reducer.
> That Reducer becomes the bottleneck — all other machines finish and wait for it.
> Fix: Salting — add a random prefix to the key ("Amazon_1", "Amazon_2"...) to spread load across reducers, then do a second-pass aggregation to combine results.
