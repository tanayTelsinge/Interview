# Big Data Stack — What, Why, Where

## What is Big Data?

Data that is too large, too fast, or too varied for traditional databases (MySQL, PostgreSQL) to handle.

The 3 Vs:
- **Volume** — TBs to PBs of data
- **Velocity** — data arriving in real-time (millions of events/sec)
- **Variety** — structured (tables), semi-structured (JSON/logs), unstructured (images, text)

Traditional DB breaks under these conditions. Big Data tools are built specifically for this.

---

## The Stack — 3 Layers

```
┌────────────────────────────────────────────────┐
│            COMPUTE / PROCESSING                │
│     Spark | MapReduce | Flink | Spark Streaming│
├────────────────────────────────────────────────┤
│            RESOURCE MANAGEMENT                 │
│              YARN | Kubernetes                 │
├────────────────────────────────────────────────┤
│            DISTRIBUTED STORAGE                 │
│              HDFS | S3 | GCS                   │
└────────────────────────────────────────────────┘
```

---

## Tool-by-Tool Breakdown

---

### 1. HDFS — Hadoop Distributed File System

**What:**
A distributed file system that splits large files into 128MB blocks and stores replicas across multiple machines.

**Why:**
- No single machine can store petabytes
- If one machine dies, replicated blocks survive (fault tolerance)
- Data lives close to compute — reduces network transfer (data locality)

**Where Used:**
- On-prem Hadoop clusters at banks, telecom companies
- Foundation of early data lakes (pre-cloud era)
- Now largely replaced by cloud object stores (S3, GCS) in modern stacks

**Fintech Use Case:**
> HDFS stores 5 years of raw transaction logs (JSON files) across a 50-node cluster. Each file is replicated 3x for durability.

---

### 2. Hadoop MapReduce

**What:**
A distributed compute model: split data → Map (transform each record independently) → Reduce (aggregate results).

**Why:**
- Before Spark, this was the only way to process data distributed across HDFS
- Fault tolerant — if a node dies mid-job, only that task is retried

**Where Used:**
- Legacy batch jobs at large enterprises
- Mostly replaced by Spark today
- Still seen in old Hadoop clusters at banks and telcos

**Fintech Use Case:**
> Monthly statement generation — Map reads each transaction for a user, Reduce aggregates totals per account.

**Why It Died:**
- Writes intermediate data to disk between every Map and Reduce step
- A 10-step job = 9 disk writes = extremely slow
- Spark replaced it by keeping intermediate data in memory

---

### 3. Apache Spark

**What:**
A distributed in-memory compute engine. Processes data across a cluster, keeps intermediate results in RAM (not disk).

**Why:**
- 100x faster than MapReduce for iterative jobs
- Unified engine — batch, SQL, ML, streaming in one framework
- Rich APIs in Java, Scala, Python (PySpark), R

**Core Concepts:**
- **RDD (Resilient Distributed Dataset)** — low-level distributed collection
- **DataFrame / Dataset** — high-level SQL-like API (use this in practice)
- **Lazy evaluation** — Spark builds a DAG of transformations, executes only on action (`.show()`, `.collect()`, `.write()`)
- **Driver + Executors** — Driver plans the job, Executors run tasks on worker nodes

**Where Used:**
- Uber: trip data processing, surge pricing models
- Netflix: recommendation model training
- Razorpay / Paytm: daily settlement batch jobs
- Airbnb: pricing analytics

**Fintech Use Cases:**

| Job | Description |
|---|---|
| Daily Settlement | Process all transactions of the day, compute net payables per merchant |
| Fraud Pattern Mining | Scan 1B historical transactions to find fraud patterns |
| Chargeback Analytics | Identify merchants with high dispute rates |
| Regulatory Reporting | Aggregate transaction data for RBI/SEBI reporting |
| ML Feature Engineering | Generate features (avg txn value, velocity) for fraud ML model |

**When to Use Spark:**
- Batch jobs on large datasets (GBs to TBs)
- ETL pipelines (Extract, Transform, Load)
- ML model training on distributed data
- SQL analytics on data lake

---

### 4. Apache Kafka

**What:**
A distributed, durable, high-throughput event streaming platform. Think of it as a persistent, scalable message queue where events are stored as an ordered log.

**Why:**
- Decouples producers (your app) from consumers (fraud engine, analytics)
- Handles millions of events/second
- Events are durable — stored on disk, replayable
- Multiple consumers can independently read the same event

**Core Concepts:**
- **Topic** — category of events (e.g., `payment-events`)
- **Partition** — topic split into partitions for parallelism
- **Offset** — position of a message in a partition (consumers track this)
- **Consumer Group** — multiple consumers share load across partitions
- **Retention** — events stored for N days (default 7), replayable anytime

**Where Used:**
- LinkedIn (invented Kafka): activity tracking, 7 trillion messages/day
- Uber: real-time ride events, driver location tracking
- Stripe / Visa: payment event streaming
- Your Spring Boot app → Kafka → downstream services

**Fintech Use Cases:**

| Use Case | Producer | Consumer |
|---|---|---|
| Real-time fraud detection | Payment service | Fraud engine (Flink/Spark) |
| Transaction notifications | Payment service | Notification service |
| Audit log streaming | All services | Audit/compliance store |
| Settlement pipeline trigger | Reconciliation job | Settlement service |
| Risk scoring | Transaction events | Risk scoring engine |

**When to Use Kafka:**
- Real-time data pipelines between services
- Event-driven microservices
- When you need replay/reprocessing capability
- High-throughput event ingestion

---

### 5. Apache Flink

**What:**
A distributed stream processing engine. Processes events one-by-one as they arrive (true streaming), not in mini-batches.

**Why:**
- Sub-second latency — critical for fraud detection, live dashboards
- Stateful processing — can maintain state per user/session across events
- Exactly-once guarantees — no duplicate processing even on failure

**Flink vs Spark Streaming:**

| | Spark Streaming | Flink |
|---|---|---|
| Model | Micro-batch (e.g., every 1s) | True event-by-event |
| Latency | Seconds | Milliseconds |
| State management | Limited | First-class |
| Complexity | Simpler | Harder |

**Where Used:**
- Alibaba: real-time risk scoring for Alipay (trillions of transactions)
- Uber: real-time marketplace pricing
- Netflix: real-time anomaly detection

**Fintech Use Case:**
> A payment arrives on Kafka. Flink reads it, looks up the user's last 10 transactions (stateful), computes velocity score, and blocks the transaction in <200ms if score exceeds threshold.

**When to Use Flink:**
- Sub-second fraud detection
- Live dashboards that update per event
- Complex event processing (e.g., detect pattern: 3 failed txns in 60s)

---

### 6. Hive

**What:**
A SQL interface on top of HDFS/S3. Translates SQL queries into MapReduce or Spark jobs.

**Why:**
- Data analysts can't write Spark code — they know SQL
- Hive lets them query petabyte-scale data with standard SQL
- Defines schema over raw files (schema-on-read)

**Where Used:**
- Data warehousing on Hadoop clusters
- Large enterprises with legacy Hadoop setups
- Now mostly replaced by Presto/Trino for interactive queries

**Fintech Use Case:**
> Analytics team runs `SELECT merchant_id, SUM(amount) FROM transactions WHERE date = '2024-01-01' GROUP BY merchant_id` — Hive executes this across 10TB of data on HDFS.

---

### 7. Presto / Trino

**What:**
Distributed SQL query engine. Queries data directly from S3, HDFS, databases — without moving it. Interactive speed (seconds, not hours).

**Why:**
- Hive is slow (MapReduce underneath)
- Presto/Trino is MPP (Massively Parallel Processing) — queries run in-memory across workers
- Can federate queries across multiple data sources simultaneously

**Where Used:**
- Facebook (invented Presto): queries over 300PB data warehouse
- Uber, Twitter, Netflix: ad-hoc analytics at scale
- Modern data platforms replacing Hive

**When to Use:**
- Interactive SQL analytics on data lake
- Ad-hoc queries by data/product teams
- Federated queries across multiple databases

---

### 8. YARN (Yet Another Resource Negotiator)

**What:**
Cluster resource manager in Hadoop. Allocates CPU and memory across jobs running on the cluster.

**Why:**
- Multiple jobs run on the same cluster (Spark, MapReduce, Hive)
- YARN ensures fair resource allocation, no job starves another

**Modern Alternative:**
- Kubernetes is replacing YARN for running Spark jobs in cloud-native setups

---

## Data Lake vs Data Warehouse vs Lakehouse

| | Data Lake | Data Warehouse | Lakehouse |
|---|---|---|---|
| Storage | Raw files (S3, HDFS) | Proprietary (Snowflake, Redshift) | Open format (Parquet on S3) |
| Schema | On read | On write | Both |
| Data type | Any (raw, unstructured) | Structured only | Structured + semi-structured |
| Cost | Very cheap | Expensive | Cheap |
| Query speed | Slow (without optimization) | Fast | Fast (with Delta/Iceberg) |
| Tools | Spark, Hive | SQL clients | Spark + Delta Lake |
| Best for | Raw storage, ML | BI dashboards, reporting | Unified analytics platform |

**Modern trend:** Lakehouse (Delta Lake, Apache Iceberg, Apache Hudi) — combines cheap storage of data lake with performance of data warehouse.

---

## Real-World End-to-End Pipeline (Fintech)

```
┌─────────────────┐
│  Spring Boot     │  ← Payment service (your domain)
│  Payment API     │
└────────┬────────┘
         │ publishes events
         ▼
┌─────────────────┐
│     Kafka        │  ← payment-events topic
│  (Event Stream)  │
└────────┬────────┘
         │
         ├─────────────────────────────────────┐
         │                                     │
         ▼                                     ▼
┌─────────────────┐                  ┌──────────────────┐
│     Flink        │                  │   S3 / GCS       │
│ (Real-time fraud │                  │  (Raw Data Lake)  │
│  scoring <200ms) │                  │  Parquet files   │
└─────────────────┘                  └────────┬─────────┘
         │                                    │
         ▼                                    ▼
┌─────────────────┐                  ┌──────────────────┐
│  Block / Allow  │                  │     Spark         │
│  Transaction    │                  │  (Batch Jobs)     │
└─────────────────┘                  │  - Daily reports  │
                                     │  - ML training    │
                                     │  - Reconciliation │
                                     └────────┬─────────┘
                                              │
                                              ▼
                                     ┌──────────────────┐
                                     │  Trino / Hive     │
                                     │  (SQL Analytics)  │
                                     └────────┬─────────┘
                                              │
                                              ▼
                                     ┌──────────────────┐
                                     │  BI Dashboard /   │
                                     │  Data Team SQL    │
                                     └──────────────────┘
```

---

## GCP Managed Equivalents (Your Stack)

| Open Source | GCP Managed |
|---|---|
| HDFS | Google Cloud Storage (GCS) |
| Spark | Dataproc (managed Spark/Hadoop) |
| Kafka | Pub/Sub or Confluent on GCP |
| Flink | Dataflow (Apache Beam) |
| Hive / Presto | BigQuery |
| YARN | Dataproc / GKE |
| Delta Lake / Iceberg | BigLake |

---

## When to Use What — Decision Guide

| Scenario | Tool |
|---|---|
| Stream events between microservices | Kafka |
| Real-time fraud detection (<1s) | Flink + Kafka |
| Daily batch ETL job (GBs-TBs) | Spark |
| SQL analytics on data lake | Trino / BigQuery |
| Store raw event data cheaply | S3 / GCS |
| Train ML model on historical data | Spark MLlib / PySpark |
| Analyst team needs self-serve SQL | Hive / Trino / BigQuery |
| Complex event patterns in stream | Flink CEP |

---

## Interview Cheat Sheet

**Q: How is Spark different from MapReduce?**
> Spark processes data in-memory and builds a DAG of operations, avoiding disk I/O between steps. MapReduce writes to disk after every Map and Reduce phase. Result: Spark is ~100x faster for iterative workloads.

**Q: How does Kafka guarantee no data loss?**
> Kafka persists events to disk with configurable replication (typically 3x). Producers get an ack only after all replicas confirm the write. Consumers track offsets independently, so even if a consumer crashes, it resumes from last committed offset.

**Q: When would you choose Flink over Spark Streaming?**
> When you need true per-event processing with sub-second latency and stateful operations (e.g., fraud detection maintaining per-user session state). Spark Streaming uses micro-batching which adds latency (seconds). Flink processes events as they arrive (milliseconds).

**Q: What is a Data Lakehouse?**
> A Lakehouse combines the cheap storage of a data lake (raw files on S3/GCS) with the ACID transactions, schema enforcement, and query performance of a data warehouse. Tools like Delta Lake and Apache Iceberg enable this by adding metadata layers on top of Parquet files.