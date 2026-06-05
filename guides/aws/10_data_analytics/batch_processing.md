# Data Analytics — Batch Processing

## 1. The Problem

Running analytics queries directly against OLTP databases (RDS, Aurora) causes:

- Query competition with live transactions → production slowdowns
- OLTP schemas are normalized for write performance, not read analytics
- No historical data (deleted records, aggregate trends) — OLTP only has current state
- Complex multi-table joins on billions of rows time out on OLTP engines
- Compliance: analysts don't need production DB credentials

The solution: a **data lake architecture** — separate storage (S3) from compute (Athena, Redshift, EMR), with a metadata catalog (Glue) connecting them.

```
Data Sources → ETL/Ingestion → Data Lake (S3) → Analytics Layer → BI/Reporting
  RDS, apps      Glue, DMS      Raw/Curated      Athena, Redshift   QuickSight
  IoT, logs      Kinesis        Parquet/ORC       EMR, Spark
```

---

## 2. What AWS Built

| Service | Role |
|---------|------|
| **AWS Glue** | Serverless ETL, Data Catalog, schema discovery |
| **Amazon Athena** | Serverless SQL queries directly on S3 |
| **Amazon Redshift** | Columnar MPP data warehouse |
| **Amazon EMR** | Managed Hadoop/Spark/Hive cluster |
| **AWS Batch** | Managed batch job scheduling (non-Hadoop) |

---

## 3. How It Works

### AWS Glue

**Glue Data Catalog:**
The central metadata repository — a Hive-compatible metastore that stores table definitions, schemas, and partition information.

```
Who uses the Glue Data Catalog?
  Athena      → reads catalog to know table schema/location
  Redshift Spectrum → reads catalog for external tables
  EMR         → reads catalog (Hive metastore compatible)
  Glue jobs   → source and target schemas
  Lake Formation → access control layer on top of catalog
```

**Glue Crawlers:**
```
Glue Crawler Process:
  1. Point at S3 location (or JDBC database)
  2. Crawler samples files, detects schema
  3. Creates/updates table definitions in Data Catalog
  4. Detects partitions automatically

Run: on schedule (hourly/daily) or on-demand
Supports: JSON, CSV, Parquet, ORC, Avro, XML, JDBC (RDS, Redshift)
```

**Glue ETL Jobs:**
```
Job types:
  Spark jobs  → distributed processing (Python/Scala)
  Python Shell → single node Python (pandas, scipy)
  Streaming  → Kinesis or MSK source, Spark Streaming

Pricing: DPU (Data Processing Unit) × hours
  1 DPU = 4 vCPU, 16 GB RAM
  Minimum: 2 DPUs for Spark jobs
  Price: ~$0.44/DPU-hour

Auto-scaling: Glue 3.0+ can auto-scale workers up/down
```

**Glue Studio:** Visual drag-and-drop ETL builder — generates Glue Spark code
**Glue DataBrew:** Visual data preparation (no-code) for data analysts — 250+ transforms
**Glue Elastic Views:** Replicate/combine data across stores with SQL (materialized views across services)

**Glue Connections:**
```python
# Glue job reading from RDS MySQL:
datasource = glueContext.create_dynamic_frame.from_catalog(
    database="my_catalog_db",
    table_name="mysql_customers",
    transformation_ctx="datasource"
)

# Write to S3 as Parquet:
glueContext.write_dynamic_frame.from_options(
    frame=datasource,
    connection_type="s3",
    connection_options={"path": "s3://my-datalake/customers/"},
    format="parquet"
)
```

---

### Amazon Athena

**Core concept:** Presto-based serverless SQL engine. No clusters, no provisioning. Point at S3 data → run SQL → pay per TB scanned.

**Pricing:** **$5 per TB scanned** (not per query, not per row — per bytes read from S3)

**Performance & Cost Optimization:**
```
Technique             Impact on cost     Impact on speed
─────────────────────────────────────────────────────────
Partitioning          90%+ reduction     Huge (skip irrelevant data)
Columnar format       60-90% reduction   Significant (Parquet/ORC)
Compression           50-70% reduction   Moderate (snappy/zstd)
S3 Select            Reduces scan        Moderate

Partitioning example:
  s3://datalake/events/year=2024/month=01/day=15/data.parquet
  Query: WHERE year=2024 AND month=01
  → Athena reads ONLY year=2024/month=01/* (skips 11/12 of data)

Columnar format:
  SELECT id, name FROM events    ← only reads 2 columns
  Row format (CSV): reads ALL columns even though 2 requested
  Parquet/ORC: reads ONLY the 2 requested column segments → 80% savings
```

**Athena Federated Query:**
```sql
-- Query RDS without loading to S3:
SELECT * FROM lambda_connector.postgres_db.public.orders
WHERE order_date > '2024-01-01';

-- Federated sources via Lambda connectors:
--   RDS (MySQL, PostgreSQL), DynamoDB, CloudWatch Logs,
--   DocumentDB, HBase on EMR, Redis, JDBC (custom)
```

**Athena Workgroups:**
```
Use for: cost control, access isolation, query result encryption

Workgroup settings:
  - Max data scanned per query: 1 GB (fail if exceeded)
  - Max data scanned per workgroup per day: 100 GB
  - Results bucket: separate per team
  - Encryption: SSE-S3, SSE-KMS, CSE-KMS
```

**Athena for Spark:**
- Run Spark notebooks in Athena console (no EMR cluster)
- Serverless Spark on managed infrastructure
- Good for: data exploration, ML prep, not heavy production ETL

---

### Amazon Redshift

**Core concept:** Columnar, MPP (Massively Parallel Processing) data warehouse. Data distributed across multiple nodes. Designed for complex analytics on hundreds of GB to petabytes.

**Cluster Types:**
| Type | Best For | Storage |
|------|----------|---------|
| **RA3** | Flexible workloads, most use cases | Managed storage (S3-backed, scales independently of compute) |
| **DC2** | Pure compute (legacy), high-performance with local SSD | Local SSD (fixed, compute+storage coupled) |
| **Redshift Serverless** | Variable/unpredictable workloads | Auto-scale, pay per RPU-second |

**RA3 vs DC2:**
```
RA3: Compute and storage scale independently
  - RA3.4xlarge: 12 vCPU, 96 GB RAM, managed storage up to 128 TB per node
  - Best for: most new deployments

DC2: Legacy (fast SSD, but storage capped at compute choice)
  - DC2.8xlarge: 32 vCPU, 244 GB RAM, 2.56 TB SSD
  - Use when: compute-intensive, small data, need SSD speed
```

**Redshift Serverless:**
```
No cluster management
  - Capacity: RPU (Redshift Processing Units) — auto-scales 8-512 RPUs
  - Billing: per RPU-second when active
  - Base RPU: configure minimum (default 128 RPU)
  - Best for: dev/test, variable workloads, infrequent queries

vs Provisioned cluster: use provisioned for sustained/predictable heavy workloads
```

**Data Loading:**
```sql
-- COPY command (fastest — parallel load from S3):
COPY orders
FROM 's3://my-bucket/orders/'
IAM_ROLE 'arn:aws:iam::xxx:role/RedshiftRole'
FORMAT AS PARQUET;

-- COPY is 10-100x faster than INSERT statements
-- Use manifest file for specific files:
COPY orders
FROM 's3://my-bucket/orders/manifest.json'
IAM_ROLE '...'
MANIFEST;
```

**Redshift Spectrum:**
```sql
-- Query S3 data directly without loading it:
-- External table defined in Glue Data Catalog

CREATE EXTERNAL TABLE spectrum.events (
  event_id BIGINT,
  user_id INT,
  event_type VARCHAR(50),
  event_time TIMESTAMP
)
STORED AS PARQUET
LOCATION 's3://my-datalake/events/';

-- Join Redshift table + S3 external table:
SELECT r.customer_name, COUNT(s.event_id) as event_count
FROM redshift_customers r
JOIN spectrum.events s ON r.customer_id = s.user_id
GROUP BY r.customer_name;
```

**Sort Keys:**
```
COMPOUND sort key: columns in order (col1, col2, col3)
  - Best for: queries that filter on leading columns
  - Like a B-tree index
  - Maintenance: VACUUM to re-sort after heavy deletes/inserts

INTERLEAVED sort key: equal weight to all columns
  - Best for: queries on any subset of key columns
  - Higher VACUUM overhead
  - Prefer Compound for most cases; Interleaved for ad-hoc analytics
```

**Distribution Styles:**
```
EVEN:    Rows distributed round-robin across slices
  → Good: even storage distribution, no obvious key
  → Bad: joins require data movement between nodes

KEY:     Rows with same key value go to same slice
  → Good: collocate large fact + dimension tables for fast joins
  → Best: join key should be the distribution key on both tables

ALL:     Full copy on every node
  → Good: small dimension tables (< 10M rows)
  → Bad: large tables — massive storage overhead, slow writes
  → Best: small, rarely updated lookup tables

AUTO:    Redshift decides (starts ALL for small tables, switches to KEY as it grows)
```

**Concurrency Scaling:** automatically adds clusters when query queue exceeds threshold. Extra clusters only bill per second when active.

**Data Sharing:** share live data between Redshift clusters/accounts without copying data.

---

### Amazon EMR

**Core concept:** Managed Hadoop ecosystem. You define the cluster; EMR provisions, configures, and monitors it.

**Supported Frameworks:**
```
Processing:   Apache Spark, Apache Hadoop MapReduce
Query:        Apache Hive, Presto/Trino
Storage:      Apache HBase, Apache Phoenix
Streaming:    Apache Flink, Apache Spark Streaming
ML:           Apache MXNet, TensorFlow (on EMR)
Messaging:    Apache Kafka (on EMR, not MSK)
```

**Cluster Types:**
| Mode | Use Case |
|------|----------|
| **Transient cluster** | Start → run job → terminate. Pay only for job duration. S3 as storage (EMRFS). |
| **Persistent cluster** | Always on. Use for: interactive Hive/Presto queries, HBase (random access). |
| **EMR Serverless** | Submit Spark/Hive jobs without managing cluster. Auto-scales workers. |
| **EMR on EKS** | Run EMR workloads on your EKS cluster. Share cluster with other apps. |

**HDFS vs EMRFS (S3):**
```
HDFS (local to cluster):
  + Fastest: data on local disks
  - Lost when cluster terminates (transient cluster = data gone)
  - Use for: intermediate Spark shuffle data

EMRFS (S3):
  + Persistent: survives cluster termination
  + Decoupled storage/compute (separate cluster scaling)
  - Slower than HDFS for small files or many random reads
  - Use for: input/output data, permanent storage

Best practice: Input/output from S3 (EMRFS), intermediate data on HDFS
```

**EMR Spot Integration:**
```
EMR cluster node types + pricing strategy:
  Master node:   On-Demand (never lose this)
  Core nodes:    On-Demand (HDFS data lives here — losing = data loss)
  Task nodes:    Spot (compute only — no HDFS data, safe to interrupt)

Instance Fleets:
  Specify multiple instance types per node group
  EMR picks available Spot capacity → reduces interruption risk
```

---

### AWS Batch

**Core concept:** Managed batch job scheduling — you define jobs (container, vCPU, memory), Batch provisions and terminates compute automatically.

```
AWS Batch components:
  Job Definition  → Docker image, vCPU, memory, retry, timeout
  Job Queue       → priority queue, linked to compute environments
  Compute Environment → On-Demand or Spot EC2, or Fargate

Workflow:
  Submit job → Queue → Batch provisions EC2/Fargate → Run container → Terminate
```

**Multi-Node Parallel Jobs (MPI):**
```
For HPC-style workloads (weather modeling, genomics, financial simulation):
  - Batch launches N instances simultaneously
  - One main node + N-1 child nodes
  - MPI framework for inter-node communication (via EFA for low-latency)
  - AWS ParallelCluster is alternative for full HPC stack
```

**AWS Batch vs Lambda vs EMR:**
| | AWS Batch | Lambda | EMR |
|-|-----------|--------|-----|
| Max runtime | No limit | 15 min | No limit |
| Container support | Yes | Yes (container image) | No (JAR/scripts) |
| Startup latency | Minutes | Seconds | Minutes (cluster) |
| Spot integration | Yes (Spot compute env) | No | Yes |
| MPI / multi-node | Yes | No | No (Spark handles it) |
| Use case | Container batch jobs | Event-driven, short | Hadoop/Spark ecosystem |

---

## 4. Key Config & Limits

| Service | Parameter | Value |
|---------|-----------|-------|
| Athena | Price | $5/TB scanned |
| Athena | Query results retention | Up to 45 days in S3 |
| Athena | Max concurrent DML queries | 20 per account (soft limit) |
| Redshift | RA3 max nodes | 128 per cluster |
| Redshift | Serverless RPU range | 8–512 RPUs |
| Redshift | COPY source | S3, DynamoDB, EMR, SSH, Kinesis |
| Glue | DPU size | 4 vCPU, 16 GB RAM |
| Glue | Minimum Spark job | 2 DPUs |
| Glue | Crawler schedule | Minimum 5 minutes |
| EMR | Supported Hadoop versions | 2.x, 3.x |
| AWS Batch | Max job timeout | No hard limit |
| AWS Batch | Multi-node parallel max nodes | 1000 |

---

## 5. Decision Tree

```
ANALYTICS WORKLOAD DECISION:

Is the data already in S3 and you need ad-hoc SQL?
├─ YES → ATHENA ($5/TB, serverless, no cluster management)
└─ NO
   └─ Complex data warehouse with many concurrent analysts?
      ├─ YES → REDSHIFT (MPP, concurrency scaling, complex joins)
      └─ NO
         └─ Need Hadoop ecosystem (Hive, Spark, HBase, Flink)?
            ├─ YES → EMR (managed Hadoop, transient or persistent)
            └─ NO
               └─ ETL jobs to transform and load data?
                  ├─ Serverless Spark/Python → GLUE
                  └─ Container batch jobs, HPC → AWS BATCH

ATHENA vs REDSHIFT:
  Athena:   Pay per query, no cluster, best for occasional queries, < 10TB
  Redshift: Pay for cluster (or RPU-sec serverless), best for high-concurrency,
            frequent queries, complex joins, loaded data

GLUE vs EMR:
  Glue:     Serverless, less control, simpler ETL, no cluster management
  EMR:      Full control, any Hadoop framework, streaming with Flink, persistent HBase
```

---

## 6. Common Patterns

### Pattern 1: Classic S3 Data Lake
```
Raw Data → S3 Raw Zone (as-is)
    │
    ▼
Glue ETL Job (transform, clean, enrich)
    │
    ▼
S3 Curated Zone (Parquet, partitioned)
    │
    ├── Athena (ad-hoc SQL queries by analysts)
    ├── Redshift Spectrum (join with Redshift tables)
    └── EMR (machine learning, complex processing)
    │
    ▼
QuickSight (BI dashboards) / Jupyter Notebooks (data science)
```

### Pattern 2: Redshift + Spectrum Hybrid
```
Hot data (last 2 years): loaded in Redshift cluster (fast queries)
Cold data (2+ years):    in S3 as Parquet (queried via Spectrum)

Cost: Redshift cluster for hot data + S3 for cold = optimal
Query:
  SELECT year, SUM(revenue)
  FROM (
    SELECT year, revenue FROM redshift_sales          -- hot: in cluster
    UNION ALL
    SELECT year, revenue FROM spectrum.sales_archive  -- cold: S3 via Spectrum
  )
  GROUP BY year;
```

### Pattern 3: Nightly Batch ETL Pipeline
```
11 PM: EventBridge scheduled rule triggers Glue workflow
  Job 1: Glue crawler → update Data Catalog schema
  Job 2: Glue Spark ETL → raw S3 → curated S3 (Parquet)
  Job 3: Redshift COPY → load curated data to Redshift
  Job 4: Redshift stored procedure → run aggregations, update mart tables
  6 AM: Data ready for business analysts in Redshift / QuickSight
```

### Pattern 4: EMR Transient Cluster for ML
```
1. EMR cluster: r5.4xlarge × 10 Spot instances
2. Steps: Spark ML training job (reads from S3 EMRFS)
3. Output: trained model artifacts → S3
4. Cluster terminates automatically → cost = job duration only

Cost: 10 × r5.4xlarge Spot (~$0.25/hr each) × 4 hours = $10
vs persistent EMR: $0.25 × 10 × 24 × 30 = $1,800/month
```

---

## 7. Gotchas

| Gotcha | Detail |
|--------|--------|
| **Athena charges per TB scanned — not per row** | A `SELECT *` from a 10 TB unpartitioned CSV costs $50. Add partitions and convert to Parquet first. |
| **Redshift is not for OLTP** | Single-row INSERT/UPDATE/DELETE is slow (columnar not optimized for this). Use RDS/DynamoDB for transactional, Redshift for analytics. |
| **Glue Catalog is shared with Athena** | They share the same metastore. Glue crawler schema changes immediately affect Athena queries — test in dev first. |
| **EMR HDFS is lost on cluster termination** | Transient clusters using HDFS for output = data loss. Always write final output to S3 (EMRFS). Intermediate shuffle data can use HDFS. |
| **AWS Batch startup latency is minutes** | Batch provisions EC2 (or Fargate) on demand — 2-5 minute startup is normal. For sub-minute jobs, Lambda is better. |
| **Redshift VACUUM is required** | After heavy INSERTs and DELETEs, run VACUUM to reclaim space and re-sort data. Skipping VACUUM degrades query performance. |
| **Glue DPU pricing adds up** | A large Spark job with 50 DPUs running 4 hours = 50 × $0.44 × 4 = $88. Monitor DPU usage and right-size jobs. |
| **Athena doesn't support UPDATE/DELETE** | Athena is read-only (except CTAS/INSERT INTO). For mutable data lake, use Iceberg/Delta Lake tables (Athena v3 supports MERGE). |
| **Redshift Spectrum reads from S3 in same region** | Cross-region Spectrum is not supported. Ensure S3 bucket is in same region as Redshift cluster. |

---

## 8. Hands-On Lab — Free Tier

**Goal:** Build a serverless analytics pipeline with Glue + Athena.

### Step 1: Create S3 Buckets
```bash
aws s3 mb s3://analytics-lab-raw-$(date +%s) --region us-east-1
aws s3 mb s3://analytics-lab-results-$(date +%s) --region us-east-1
```

### Step 2: Upload Sample Data
```bash
# Create sample CSV data
cat > /tmp/orders.csv << 'EOF'
order_id,customer_id,product,amount,order_date
1,101,Widget A,29.99,2024-01-15
2,102,Widget B,49.99,2024-01-16
3,101,Widget C,19.99,2024-02-01
4,103,Widget A,29.99,2024-02-15
5,102,Widget B,49.99,2024-03-01
EOF

aws s3 cp /tmp/orders.csv s3://analytics-lab-raw-XXXXX/orders/orders.csv
```

### Step 3: Create Glue Database and Crawler
```
Glue Console → Databases → Add database
  Name: analytics_lab

Glue Console → Crawlers → Create crawler
  Name: orders-crawler
  Data source: S3: s3://analytics-lab-raw-XXXXX/orders/
  IAM role: Create new (AWSGlueServiceRole-lab)
  Target database: analytics_lab
  Schedule: On demand

→ Run crawler → Wait for "Succeeded"
→ Glue Console → Tables → should see "orders" table
```

### Step 4: Query with Athena
```sql
-- Athena Console → Settings → set results bucket: s3://analytics-lab-results-XXXXX/

-- Query 1: View all orders
SELECT * FROM analytics_lab.orders;

-- Query 2: Revenue by product
SELECT product,
       COUNT(*) as order_count,
       SUM(amount) as total_revenue
FROM analytics_lab.orders
GROUP BY product
ORDER BY total_revenue DESC;

-- Query 3: Monthly revenue
SELECT SUBSTR(order_date, 1, 7) as month,
       SUM(amount) as monthly_revenue
FROM analytics_lab.orders
GROUP BY SUBSTR(order_date, 1, 7)
ORDER BY month;
```

### Step 5: Convert to Parquet with Athena CTAS
```sql
-- Create optimized Parquet version:
CREATE TABLE analytics_lab.orders_parquet
WITH (
  format = 'PARQUET',
  parquet_compression = 'SNAPPY',
  partitioned_by = ARRAY['order_month'],
  external_location = 's3://analytics-lab-raw-XXXXX/orders_parquet/'
) AS
SELECT order_id, customer_id, product, amount, order_date,
       SUBSTR(order_date, 1, 7) as order_month
FROM analytics_lab.orders;

-- Query the Parquet version (cheaper — less data scanned):
SELECT product, SUM(amount)
FROM analytics_lab.orders_parquet
WHERE order_month = '2024-01'
GROUP BY product;
```

### Step 6: Clean Up
```bash
# Delete Glue crawler, database, tables via console
aws s3 rm s3://analytics-lab-raw-XXXXX --recursive
aws s3 rm s3://analytics-lab-results-XXXXX --recursive
aws s3 rb s3://analytics-lab-raw-XXXXX
aws s3 rb s3://analytics-lab-results-XXXXX
```

---

## Summary Reference Card

```
BATCH ANALYTICS SERVICES:
  Glue:     Serverless ETL (Spark/Python) + Data Catalog (shared w/ Athena)
  Athena:   Serverless SQL on S3 ($5/TB). Optimize: partition + Parquet + compress
  Redshift: MPP data warehouse. RA3 (separate compute/storage) or Serverless
            COPY from S3 is fastest load method. Sort keys + distribution styles matter.
  EMR:      Managed Hadoop/Spark ecosystem. Transient (job) or persistent cluster.
            HDFS = fast but ephemeral. EMRFS (S3) = persistent.
  Batch:    Container batch jobs. Multi-node MPI. Spot integration.

DECISION:
  Ad-hoc SQL on S3?          → Athena
  High-concurrency warehouse? → Redshift
  Hadoop ecosystem?           → EMR
  Serverless ETL?             → Glue
  Container batch jobs?       → AWS Batch
```
