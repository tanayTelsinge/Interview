# Purpose-Built Databases — SAP-C02 Deep Dive

## Redshift · Neptune · DocumentDB · Keyspaces · Timestream · QLDB

---

## 1. The Problem — One Database Does Not Fit All

Relational databases were designed for OLTP (Online Transaction Processing) — short transactions, row-by-row access, ACID guarantees. When you try to use OLTP databases for other data models, performance collapses:

| Data model / workload | Using RDBMS (forced) | Purpose-built alternative |
|---|---|---|
| Analytics (read terabytes, aggregate) | Full table scans kill I/O | Redshift (columnar MPP) |
| Highly connected data (social graph, fraud) | 10+ JOIN chains, exponential cost | Neptune (graph DB) |
| JSON documents at scale | CLOB columns, no schema flexibility | DocumentDB |
| Wide-column Cassandra workloads | Schema too rigid, can't scale writes | Keyspaces |
| Time-series IoT / metrics | Bloated rows, no native compression | Timestream |
| Immutable audit trails | Mutable rows, no cryptographic proof | QLDB |

AWS's principle: **match your data model to the database engine designed for it**.

---

## 2. Amazon Redshift

### 2.1 The Problem Redshift Solves

OLTP databases store data in **rows** — each row is physically adjacent on disk. For a `SELECT SUM(revenue)` query scanning millions of rows but only one column, the database reads every column of every row regardless — massive wasted I/O.

```
Row-based storage (PostgreSQL/MySQL):
Row 1: [id=1, name="Alice", email="...", revenue=100, timestamp="2024-01-01", ...]
Row 2: [id=2, name="Bob",   email="...", revenue=200, timestamp="2024-01-01", ...]
→ Aggregate query reads ALL columns of ALL rows, even unused ones

Columnar storage (Redshift):
Column revenue: [100, 200, 150, 300, ...]   ← reads ONLY this column
Column timestamp: [2024-01-01, ...]          ← reads ONLY if needed
→ Aggregate query reads only relevant columns = 10-100x less I/O
```

### 2.2 What Redshift Is

**Amazon Redshift** is a fully managed petabyte-scale **columnar MPP (Massively Parallel Processing)** data warehouse. It executes analytical queries in parallel across multiple nodes, each storing a portion of the data in columnar format.

### 2.3 Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  Redshift Cluster                                            │
│                                                             │
│  ┌──────────────────────────────────────────┐              │
│  │  Leader Node                              │              │
│  │  - Receives queries from clients          │              │
│  │  - Creates execution plans                │              │
│  │  - Coordinates compute nodes              │              │
│  │  - Aggregates results                     │              │
│  └─────────────────┬────────────────────────┘              │
│                    │                                        │
│         ┌──────────┼──────────┐                            │
│         ▼          ▼          ▼                            │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                   │
│  │ Compute  │ │ Compute  │ │ Compute  │  ...up to 128     │
│  │ Node 1   │ │ Node 2   │ │ Node 3   │  nodes             │
│  │ Slices   │ │ Slices   │ │ Slices   │                   │
│  └──────────┘ └──────────┘ └──────────┘                   │
└─────────────────────────────────────────────────────────────┘
```

- **Leader node**: parses queries, builds execution plans, returns final results
- **Compute nodes**: execute query fragments in parallel, each holds a portion of data
- **Slices**: each compute node divided into slices (2 or 16 per node); slices process data in parallel
- Data is distributed across nodes according to **distribution style**

### 2.4 Node Types

| Type | Storage | Use Case |
|---|---|---|
| **DC2 (Dense Compute)** | Local SSD (up to 2.56 TB per node) | Fast I/O, predictable workloads, data fits in SSD |
| **RA3** | Managed storage (S3-backed), local SSD cache | Large datasets (PB scale), decouple compute from storage, Redshift Spectrum |
| **Redshift Serverless** | Fully managed capacity (RPU-based) | Variable workload, no cluster management |

**RA3** is the modern recommendation — you scale compute and storage independently. RA3 caches hot data on local SSD, cold data stays in managed S3-backed storage.

### 2.5 Redshift Serverless

- No cluster management — specify max **Redshift Processing Units (RPUs)**
- Auto-scales compute to match workload
- Pay per second of compute + storage
- Suitable for: variable/unpredictable analytics workloads, dev/test

### 2.6 Loading and Exporting Data

#### COPY Command (Load into Redshift)

```sql
-- Load from S3 (most common, most performant)
COPY orders
FROM 's3://my-data-bucket/orders/'
IAM_ROLE 'arn:aws:iam::123456789:role/RedshiftS3Role'
FORMAT AS PARQUET;

-- Load from DynamoDB
COPY orders
FROM 'dynamodb://orders-table'
IAM_ROLE 'arn:aws:iam::123456789:role/RedshiftRole'
READRATIO 50;  -- use 50% of DynamoDB capacity for read

-- Load from EMR HDFS
COPY orders
FROM 'emr://cluster-id/mydata'
IAM_ROLE 'arn:aws:iam::123456789:role/RedshiftRole';
```

> **COPY is mandatory for bulk loads.** Never use INSERT row-by-row for large datasets — it is 100x slower than COPY. COPY runs in parallel across all slices.

#### UNLOAD Command (Export from Redshift)

```sql
UNLOAD ('SELECT * FROM orders WHERE order_date >= ''2024-01-01''')
TO 's3://my-output-bucket/orders/'
IAM_ROLE 'arn:aws:iam::123456789:role/RedshiftRole'
FORMAT PARQUET
PARALLEL ON;
```

### 2.7 Redshift Spectrum

```
Redshift Cluster (compute)
        │
        │ query external tables
        ▼
Spectrum Layer (massively parallel)
        │
        ▼
S3 (raw data in Parquet/ORC/CSV/JSON/Avro)
```

- Query data **directly in S3** without loading it into Redshift
- Extends Redshift queries to exabytes of unstructured data
- Uses **Glue Data Catalog** (or Hive metastore) for table definitions
- Billed per TB scanned in S3
- Ideal for: data lake queries, cold historical data, avoid ETL loading

```sql
-- Create external schema pointing to Glue catalog
CREATE EXTERNAL SCHEMA spectrum_schema
FROM DATA CATALOG
DATABASE 'my_glue_db'
IAM_ROLE 'arn:aws:iam::123456789:role/RedshiftRole'
REGION 'us-east-1';

-- Query S3 data as if it were a Redshift table
SELECT year, SUM(revenue)
FROM spectrum_schema.sales_data_s3  -- data lives in S3
GROUP BY year;
```

### 2.8 Sort Keys and Distribution Styles

#### Sort Keys

Redshift stores data in sorted order on disk. Sort key determines physical order:

| Type | Description | Use When |
|---|---|---|
| **Compound sort key** | Sorts by columns in order (like composite index) | Frequent filters on first column; range queries |
| **Interleaved sort key** | Equal weight to each column | Multiple columns used independently for filtering |

```sql
-- Compound: queries filtering by order_date are fastest
CREATE TABLE orders (
    order_id INT,
    customer_id INT,
    order_date DATE,
    revenue DECIMAL(10,2)
) COMPOUND SORTKEY (order_date, customer_id);
```

#### Distribution Styles

How Redshift distributes rows across compute nodes determines join performance (data co-location avoids shuffling):

| Style | How it works | Use When |
|---|---|---|
| **EVEN** | Round-robin distribution | No join key; uniform distribution needed |
| **KEY** | Rows with same key value go to same node | Joining two tables on the same key (data co-location) |
| **ALL** | Full copy of table on every node | Small dimension tables; always joined to large fact tables |
| **AUTO** | Redshift decides (EVEN for small, KEY for large) | Default; let Redshift optimize |

```sql
-- Large fact table distributed by customer_id
CREATE TABLE orders (order_id INT, customer_id INT, ...)
DISTKEY (customer_id);

-- Small dimension table replicated everywhere (ALL)
CREATE TABLE customers (customer_id INT, name VARCHAR, ...)
DISTSTYLE ALL;

-- Join is fast: orders and customers with same customer_id are co-located
SELECT o.order_id, c.name
FROM orders o JOIN customers c ON o.customer_id = c.customer_id;
```

### 2.9 Additional Redshift Features

| Feature | Description |
|---|---|
| **Materialized Views** | Precomputed query results; incremental refresh; accelerates repeated complex queries |
| **Result Caching** | Leader node caches identical query results; subsequent identical queries return in milliseconds |
| **Concurrency Scaling** | Automatically adds transient compute capacity during bursts; first 1 hour/day free |
| **Data Sharing** | Share live data between Redshift clusters without copying; producer cluster → consumer cluster |
| **Automatic Table Optimization** | Redshift analyzes workload and automatically adjusts sort/dist keys |

### 2.10 Key Limits and Gotchas

| Parameter | Value |
|---|---|
| Max nodes per cluster | 128 |
| Max columns per table | 1,600 |
| Max databases per cluster | 60 |
| Max concurrent queries | 50 (use concurrency scaling for more) |
| Max VARCHAR | 65,535 bytes |
| Single row max size | ~4 MB |

**Redshift Gotchas:**
- **Not for OLTP** — row-by-row inserts/updates are slow; designed for bulk loads and analytical reads
- **No primary key enforcement** — PK is a hint for query optimizer, not enforced
- **Vacuum needed after deletes** — deleted rows are marked; `VACUUM` reclaims space and re-sorts
- **Analyze for statistics** — run `ANALYZE` after large data loads to update query planner statistics
- **Leader node sizing** — for large clusters, leader node can become a bottleneck; RA3 nodes mitigate this

---

## 3. Amazon Neptune

### 3.1 The Problem Neptune Solves

In relational databases, relationships are expressed via foreign keys and JOIN operations. For deeply connected data (social networks, fraud rings, knowledge graphs), JOIN chains become exponentially expensive:

```
"Find all friends-of-friends-of-friends who bought product X"
→ Relational: 3 self-joins on a users table with millions of rows
→ Cost: O(n³) in worst case, extreme I/O

"Is this transaction part of a fraud ring (connected via shared IP/device/card)?"
→ Relational: recursive CTEs, multiple hops, slow
→ Graph DB: traverse 10 hops in milliseconds (index-free adjacency)
```

**Index-free adjacency**: in a graph database, each vertex directly stores pointers to its neighbors — traversal is O(1) per hop regardless of graph size.

### 3.2 What Neptune Is

**Amazon Neptune** is a fully managed graph database engine supporting two graph models:
- **Property Graph** via **Gremlin** (Apache TinkerPop query language)
- **RDF (Resource Description Framework)** via **SPARQL** (W3C standard query language)

Storage: Aurora-like distributed storage — 6 copies across 3 AZs, automatic failover.

### 3.3 Graph Models

#### Property Graph (Gremlin)

```
Vertices (nodes) with properties:
  Alice: {type: Person, age: 30, city: "NYC"}
  Bob:   {type: Person, age: 25, city: "LA"}
  TechCo: {type: Company}

Edges with labels and properties:
  Alice --[KNOWS {since: 2020}]--> Bob
  Alice --[WORKS_AT {since: 2022}]--> TechCo
  Bob   --[APPLIED_TO]--> TechCo
```

Gremlin query:
```groovy
// Find all companies where Alice's friends work
g.V().has('name', 'Alice')
  .out('KNOWS')    // traverse to friends
  .out('WORKS_AT') // traverse to companies
  .values('name')  // return company names
```

#### RDF Graph (SPARQL)

```
Triples: subject → predicate → object
  <alice> <knows> <bob>
  <alice> <worksAt> <techco>
  <techco> <type> <Company>
```

SPARQL query:
```sparql
SELECT ?company WHERE {
  :alice :knows ?friend .
  ?friend :worksAt ?company .
  ?company a :Company .
}
```

### 3.4 Neptune Architecture

- Distributed storage: 6 copies across 3 AZs (same Aurora-like storage)
- Up to 15 read replicas
- Sub-10ms replica lag
- Automatic failover in 30 seconds
- Supports up to **billions of relationships**

### 3.5 Neptune Serverless

- Auto-scales Neptune Capacity Units (NCUs)
- Minimum 1 NCU (not zero — cannot scale to zero)
- Ideal for variable graph workloads, dev/test

### 3.6 Neptune Streams

- Change data capture for graph data
- Ordered log of all graph mutations
- Retain for up to 7 days
- Use case: event-driven graph processing, replicate graph to search engine, audit trail

### 3.7 Use Cases

| Use Case | Why Graph | Example query |
|---|---|---|
| **Social network** | Friends-of-friends traversal | "Recommend friends" |
| **Fraud detection** | Detect rings via shared attributes | "Find clusters sharing IP + card within 24hr" |
| **Knowledge graph** | Entity relationships at scale | "What is related to this concept?" |
| **Recommendation** | Collaborative filtering | "Users like you also bought..." |
| **Network/IT topology** | Dependency mapping | "What breaks if this server fails?" |
| **Identity graph** | Link same person across systems | "Is this email/phone/device the same person?" |

### 3.8 Neptune Gotchas

- **No SQL** — must learn Gremlin or SPARQL; cannot use standard SQL tools
- Neptune is a **graph database, not a relational database** — cannot replace RDS for OLTP
- **Pricing** is per instance-hour + I/O charges (similar to Aurora)
- Cannot mix Property Graph and RDF in the same Neptune cluster
- Gremlin and SPARQL are designed for different use cases — choose based on data model, not preference

---

## 4. Amazon DocumentDB

### 4.1 The Problem DocumentDB Solves

MongoDB is the dominant open-source document database — but running self-managed MongoDB at scale requires:
- Manual sharding configuration
- Replica set management
- Backup and restore operations
- Security patching

Amazon DocumentDB provides a **MongoDB-compatible** API running on Aurora's distributed storage infrastructure — the operational benefits of a managed cloud database with the MongoDB programming model.

### 4.2 What DocumentDB Is

**Amazon DocumentDB** is a fully managed document database service that emulates the MongoDB API. It stores JSON documents with flexible schema, supports nested documents and arrays, and uses MongoDB-compatible query syntax.

> **Critical: DocumentDB is NOT MongoDB.** It is AWS's re-implementation of the MongoDB API on a proprietary Aurora-like storage engine. There are API differences and some MongoDB features are not supported.

### 4.3 Architecture

- Same Aurora-like distributed storage: 6 copies across 3 AZs
- Auto-storage growth up to 64 TB
- Up to 15 read replicas
- Automatic failover
- Compatible with MongoDB 3.6, 4.0, 5.0 drivers

### 4.4 When to Use DocumentDB

| Use DocumentDB when | Don't use DocumentDB when |
|---|---|
| Migrating from MongoDB and want managed ops | You need exact MongoDB feature parity (some features missing) |
| JSON documents with nested structures at scale | Simple key-value access (use DynamoDB) |
| Flexible schema requirements | Full relational queries needed (use RDS) |
| Need MongoDB query language ($match, $lookup, aggregation pipeline) | Open-source MongoDB required (self-manage on EC2) |

### 4.5 DocumentDB Gotchas

- **NOT MongoDB** — AWS implements the MongoDB wire protocol but not the full engine. Features like `$text` search, some aggregation pipeline stages, MongoDB Atlas features, and Change Streams have differences or limitations
- **Billing model differs** from MongoDB Atlas — DocumentDB bills per instance-hour + I/O, not per cluster
- API compatibility level has improved over versions but test your application before migrating
- Backup and restore creates a new cluster (not in-place restore)
- Accessed within a VPC only — no public endpoints

---

## 5. Amazon Keyspaces (for Apache Cassandra)

### 5.1 The Problem Keyspaces Solves

Apache Cassandra is a wide-column NoSQL database optimized for:
- Write-heavy workloads at extreme scale
- Linear horizontal scalability (add nodes, get more capacity)
- Decentralized, no single point of failure
- Time-series and IoT patterns

Self-managing a Cassandra cluster requires significant operational expertise: node management, compaction tuning, repair operations, hardware replacement. Keyspaces removes this operational burden.

### 5.2 What Keyspaces Is

**Amazon Keyspaces** is a fully managed, serverless, Apache Cassandra-compatible database. It provides Cassandra Query Language (CQL) compatibility without managing Cassandra clusters.

### 5.3 Architecture and Key Features

| Feature | Details |
|---|---|
| Compatibility | Apache Cassandra CQL (v3.11) |
| Consistency | Eventually consistent (default); tunable to local quorum |
| Capacity modes | On-demand or provisioned (with auto scaling) |
| Storage | Automatically replicated 3 times across 3 AZs |
| Point-in-time recovery | Up to 35 days |
| Encryption | At-rest (AWS managed or CMK) + TLS in-transit |
| Serverless | No node management; auto-scales |

### 5.4 Wide-Column Model

```
Keyspace: analytics
Table: sensor_readings

Partition Key: device_id     ← all rows for same device stored together
Clustering Key: timestamp    ← rows sorted by time within partition

Row example:
device_id="sensor-001", timestamp="2024-01-15T10:00:00", temp=23.5, humidity=65
device_id="sensor-001", timestamp="2024-01-15T10:01:00", temp=23.6, humidity=64
device_id="sensor-002", timestamp="2024-01-15T10:00:00", temp=25.1, humidity=70
```

CQL query:
```sql
-- Get last 100 readings for a device
SELECT * FROM analytics.sensor_readings
WHERE device_id = 'sensor-001'
ORDER BY timestamp DESC
LIMIT 100;
```

### 5.5 When to Use Keyspaces

- Migrating existing Cassandra workloads to managed AWS service
- Wide-column access patterns (time-series, IoT, logs by partition key)
- Write-heavy workloads needing linear scale
- Teams already using CQL who don't want Cassandra operational overhead

### 5.6 Keyspaces Gotchas

- **Not full Cassandra** — some Cassandra features not supported (lightweight transactions have limited support, some driver features differ)
- **No cross-region replication** within a single Keyspaces table (unlike native Cassandra multi-datacenter)
- All data is in one AWS region (though you can create Keyspaces tables in multiple regions separately)
- Testing your application against Keyspaces before migration from self-managed Cassandra is essential

---

## 6. Amazon Timestream

### 6.1 The Problem Timestream Solves

Time-series data (IoT sensor readings, application metrics, server telemetry) has unique characteristics:
- **Append-only** — you almost never update a past reading
- **Time-ordered** — queries almost always filter by time range
- **High ingest rate** — millions of data points per second
- **Aging data** — recent data queried frequently; historical data rarely accessed
- **Duplicate timestamps** — need last-write-wins or deduplication

Using a relational database for time-series data:
- Bloated storage (metadata repeated per row)
- Index maintenance overhead at scale
- No native time-series functions
- No automatic tiering of old data to cheaper storage

### 6.2 What Timestream Is

**Amazon Timestream** is a fully managed, serverless time-series database optimized for IoT and operational analytics. It automatically tiers data between in-memory and magnetic (cold) storage based on age.

### 6.3 Architecture — Automatic Storage Tiering

```
Data write (sensor readings, metrics, traces)
        │
        ▼
┌───────────────────────────────────────────┐
│  Memory Store (recent data)                │
│  - Fast reads (sub-millisecond)            │
│  - Configurable retention (hours-days)     │
│  - Higher cost                             │
└─────────────────┬─────────────────────────┘
                  │ automatic tiering (based on age)
                  ▼
┌───────────────────────────────────────────┐
│  Magnetic Store (historical data)          │
│  - Columnar format for analytics           │
│  - Configurable retention (days-years)     │
│  - Low cost                                │
└───────────────────────────────────────────┘
```

A single query can span both stores transparently — no need to union across two tables.

### 6.4 Data Model

```
Database → Tables → Rows

Row structure:
  dimension_name = dimension_value  (metadata: device_id, region, az)
  time           = timestamp
  measure_name   = "temperature"
  measure_value  = 23.5
```

```sql
-- Timestream SQL query
SELECT device_id,
       avg(measure_value::double) AS avg_temp,
       bin(time, 1m) AS minute
FROM timeseries_db.sensor_readings
WHERE measure_name = 'temperature'
  AND time BETWEEN ago(1h) AND now()
GROUP BY device_id, bin(time, 1m)
ORDER BY minute DESC
```

### 6.5 Built-in Time-Series Functions

| Function | Description |
|---|---|
| `bin(time, interval)` | Group by time intervals (1m, 5m, 1h) |
| `interpolate_linear()` | Fill gaps in sparse time-series |
| `smooth()` | Moving average |
| `derivative()` | Rate of change |
| `series_to_array()` | Convert time-series to array |
| `ago()` | Relative time reference |

### 6.6 Use Cases

- IoT device telemetry (temperature, pressure, GPS)
- Application performance monitoring (latency, error rates, CPU)
- DevOps metrics (infrastructure health, deployment events)
- Financial tick data
- Real-time dashboards via Amazon Managed Grafana integration

### 6.7 Timestream Gotchas

- **Append-only** — updates are not supported; data is immutable once written
- **Schemaless for measures but dimensions are fixed per table** — changing dimension schema requires careful table design
- **Not for OLTP** — cannot model arbitrary relational data; strictly time-series
- Memory store retention must be <= magnetic store retention
- Write throughput is high but reads are optimized for time range queries, not point lookups

---

## 7. Amazon QLDB (Quantum Ledger Database)

### 7.1 The Problem QLDB Solves

In a traditional relational database, records can be updated or deleted — there is no immutable audit trail built into the database engine. Proving that a record has not been tampered with requires external mechanisms (application-layer audit tables, triggers, hashing).

For regulated industries (finance, healthcare, supply chain, legal), you need:
- **Immutable history** — once written, data cannot be changed or deleted
- **Cryptographic verification** — anyone can independently verify the data has not been altered
- **Transparent audit trail** — every change recorded with who, what, when

### 7.2 What QLDB Is

**Amazon QLDB** is a fully managed ledger database with a **cryptographically verifiable**, **immutable** transaction log. It maintains a complete, sequenced history of every change to every document, and the history can be verified using SHA-256 hashing (Merkle tree structure).

> **QLDB is NOT a blockchain.** It is **centralized** — AWS operates the ledger. Blockchain is decentralized with no trusted central authority. QLDB is appropriate when you need immutability + audit trail but with a trusted central operator (the company owning the AWS account). Use Managed Blockchain if you need decentralization and multiple untrusting parties.

### 7.3 Architecture — Immutable Journal

```
Application writes (INSERT/UPDATE/DELETE)
        │
        ▼
┌─────────────────────────────────────────────┐
│  QLDB Journal (immutable, append-only)       │
│  - Every transaction creates a new entry     │
│  - Entries are cryptographically hashed      │
│  - Hash chain: each block includes hash      │
│    of previous block (like blockchain)       │
│  - SHA-256 hash of entire history computable │
└───────────────────┬─────────────────────────┘
                    │ materialized view
                    ▼
┌─────────────────────────────────────────────┐
│  Current State (queryable via PartiQL)       │
│  - Latest version of each document          │
│  - Optimized for current-state queries       │
└─────────────────────────────────────────────┘
```

### 7.4 Cryptographic Verification

```python
# Get digest (cryptographic hash of entire journal)
digest = qldb_client.get_digest(Name='my-ledger')
digest_hash = digest['Digest']

# Get proof for a specific document revision
proof = qldb_client.get_revision(
    Name='my-ledger',
    BlockAddress=block_address,
    DocumentId=document_id,
    DigestTipAddress=digest['DigestTipAddress']
)

# Verify: compute hash chain from document to digest
# If computed hash matches digest_hash, document is authentic
verify_document(proof, digest_hash)
```

- Any external party can independently verify data integrity
- Even AWS employees cannot alter the journal without detection
- Audit regulators can verify records without database access

### 7.5 QLDB vs Traditional Audit Table

| Approach | Traditional Audit Table | QLDB |
|---|---|---|
| History completeness | Only if trigger fires correctly | Every change, automatically |
| Tamper evidence | No — DBA can DELETE audit rows | Yes — cryptographic proof |
| Query language | SQL | PartiQL (SQL-compatible) |
| Storage overhead | Extra tables + indexes | Managed by QLDB |
| Compliance proof | Application-layer, hard to prove | Built-in digest verification |

### 7.6 QLDB Data Model and PartiQL

Documents are stored as **Amazon Ion** format (superset of JSON):

```sql
-- Insert
INSERT INTO vehicles VALUE {
    'vin': 'VIN001',
    'make': 'Toyota',
    'model': 'Camry',
    'year': 2020,
    'owners': ['alice@example.com']
}

-- Update (creates new immutable revision; old revision preserved)
UPDATE vehicles
SET owners = ['alice@example.com', 'bob@example.com']
WHERE vin = 'VIN001'

-- Query history of a document
SELECT * FROM history(vehicles)
WHERE metadata.id = 'document-id-123'
-- Returns ALL revisions ever written to this document
```

### 7.7 Use Cases

| Use Case | Why QLDB | Example |
|---|---|---|
| **Financial transactions** | Immutable ledger of transfers; regulatory compliance | Bank transfers, stock trades |
| **Supply chain tracking** | Prove chain of custody; cannot alter delivery records | Pharmaceutical cold chain |
| **HR/payroll records** | Immutable salary history; audit compliance | Compensation changes audit |
| **Healthcare records** | Immutable patient record changes; HIPAA audit | EHR modification history |
| **Digital assets** | Track ownership history | NFT provenance (centralized) |

### 7.8 QLDB Gotchas

- **Centralized — not decentralized** — this is the biggest exam trap. "Multiple untrusting parties" → NOT QLDB → use Managed Blockchain
- **No multi-region** — QLDB is a regional service; no built-in cross-region replication
- **PartiQL queries current state fast; history queries are slower** — history table is full journal scan
- **Cannot delete history** — QLDB data cannot be deleted by the customer (data is immutable by design); plan your data retention accordingly
- **Streams to Kinesis** — QLDB can stream journal changes to Kinesis Data Streams for event-driven processing

---

## 8. Key Config and Limits

### Redshift

| Parameter | Value |
|---|---|
| Max nodes | 128 |
| RA3 storage max | 8 PB per cluster |
| DC2 max node storage | 2.56 TB SSD |
| Max concurrent queries | 50 (+ Concurrency Scaling) |
| Max columns per table | 1,600 |
| Spectrum: max external tables | Unlimited (S3) |

### Neptune

| Parameter | Value |
|---|---|
| Max read replicas | 15 |
| Storage auto-grow increment | 10 GB |
| Max storage | 64 TB |
| Failover time | ~30 seconds |
| Query languages | Gremlin + SPARQL |
| Streams retention | 7 days |

### DocumentDB

| Parameter | Value |
|---|---|
| Max read replicas | 15 |
| Max storage | 64 TB |
| Max document size | 16 MB |
| MongoDB version compatibility | 3.6, 4.0, 5.0 |
| Backup retention | 1-35 days |

### Keyspaces

| Parameter | Value |
|---|---|
| Max partition size | 10 GB |
| Max columns per table | 225,000 |
| PITR retention | 35 days |
| Max replication factor | 3 (automatic) |

### Timestream

| Parameter | Value |
|---|---|
| Memory store max retention | 12 months |
| Magnetic store max retention | 200 years |
| Max measure values per record | 256 |
| Write throughput | Millions of events/second |

### QLDB

| Parameter | Value |
|---|---|
| Max document size | 128 KB |
| Max documents per transaction | 40 |
| Max payload per transaction | 10 MB |
| History retention | Indefinite (immutable) |
| Digest algorithm | SHA-256 |

---

## 9. Decision Tree — Which Purpose-Built Database?

```
What is your primary data model and access pattern?
│
├─ Analytical queries on large datasets (TB-PB)?
│   Aggregate functions, GROUP BY, complex joins on historical data?
│   └─ Redshift (columnar MPP data warehouse)
│       ├─ Consistent large cluster → DC2 or RA3
│       ├─ Variable/unpredictable → Redshift Serverless
│       └─ Query S3 data lake without loading → Redshift Spectrum
│
├─ Highly connected data — relationships are the primary query?
│   "Find friends of friends", fraud rings, recommendation graphs?
│   └─ Neptune (graph database)
│       ├─ Property graph, flexible traversal → Gremlin
│       └─ Semantic/knowledge graph (RDF standard) → SPARQL
│
├─ JSON document model? Flexible schema? MongoDB API?
│   └─ DocumentDB
│       ├─ Migrating from MongoDB → DocumentDB (test for API compatibility)
│       └─ New greenfield document workload at scale → also DynamoDB (consider both)
│
├─ Wide-column model? Cassandra API? Write-heavy? Already using CQL?
│   └─ Keyspaces (managed Cassandra)
│
├─ Time-series data? IoT metrics? Append-only? Time-range queries?
│   └─ Timestream
│       └─ Automatic memory→magnetic tiering, native time functions
│
├─ Immutable audit trail? Cryptographic verification of history?
│   Need to PROVE data has not been tampered?
│   ├─ Centralized (single trusted party owns the ledger)?
│   │   └─ QLDB
│   └─ Decentralized (multiple untrusting parties)?
│       └─ Amazon Managed Blockchain (NOT QLDB)
│
└─ Key-value or relational?
    ├─ Millisecond latency at any scale, serverless → DynamoDB
    └─ Complex SQL, joins, ACID transactions → RDS / Aurora
```

---

## 10. Common Patterns

### Pattern 1: Redshift Data Warehouse Architecture

```
Data Sources:
  RDS (transactional) → DMS/Glue ETL → S3 (staging) → COPY → Redshift
  DynamoDB            → Export → S3 → COPY → Redshift
  S3 Data Lake        → Redshift Spectrum (no loading needed)
  Kafka/Kinesis       → Kinesis Firehose → S3 → COPY → Redshift

Consumers:
  Redshift → QuickSight (BI dashboards)
  Redshift → UNLOAD → S3 → Athena (ad-hoc queries)
  Redshift → Data Sharing → another Redshift cluster (analytics isolation)
```

### Pattern 2: Fraud Detection with Neptune

```
Transaction event → Lambda → Neptune
                                │
                     Graph write: new transaction node
                     Connect to: user node, IP node, device node, card node
                                │
                     Gremlin query: "find all transactions connected to this IP
                                     within 24 hours via shared device/card"
                                │
                     If cluster found → flag for fraud review
                                │
                     DynamoDB ← Lambda ← Neptune result (store fraud flag)
```

### Pattern 3: IoT Telemetry Pipeline (Timestream)

```
IoT Devices → AWS IoT Core → Kinesis Data Streams
                                      │
                               Lambda Function
                                      │
                               Timestream (write)
                               └── memory store: last 7 days
                               └── magnetic store: 5 years history
                                      │
                               Amazon Managed Grafana → real-time dashboards
```

### Pattern 4: Financial Audit Trail with QLDB

```
Payment Service (writes transfers to QLDB)
      │
      ▼
QLDB Journal (immutable, cryptographic hash chain)
      │
      ├── Current state → PartiQL queries → balance lookups
      │
      ├── History → SELECT * FROM history(transfers) → audit trail
      │
      ├── QLDB Streams → Kinesis → Lambda → OpenSearch (for search)
      │
      └── Digest verification → regulatory audit proof
```

### Pattern 5: MongoDB Migration to DocumentDB

```
Self-managed MongoDB (EC2 or on-prem)
          │
          │ mongodump / mongorestore  (or AWS DMS)
          ▼
Amazon DocumentDB
  - Test MongoDB application with DocumentDB endpoint
  - Verify all aggregation pipeline stages work
  - Update connection string
  - Decommission self-managed MongoDB
```

---

## 11. Gotchas — Cross-Service Exam Traps

### Redshift Gotchas
1. **Redshift is NOT for OLTP** — frequent small inserts/updates perform poorly; designed for bulk analytics
2. **No automatic VACUUM** — must schedule VACUUM/ANALYZE jobs after bulk deletes/updates to reclaim space and update statistics
3. **COPY, not INSERT** — always use COPY for bulk loads; single-row INSERT is an anti-pattern
4. **Sort key is physical sort order** — wrong sort key = full table scan even with WHERE clause
5. **Concurrency Scaling adds nodes** — costs extra; not free after first 1 hour per 24 hours per cluster

### Neptune Gotchas
1. **No SQL** — cannot use SQL query tools; must use Gremlin or SPARQL
2. **Graph traversals can be slow without proper indexes** — vertex and edge properties need indexes on frequently queried attributes
3. **Neptune Serverless minimum 1 NCU** — not zero; always billing when cluster is running
4. **Gremlin and SPARQL cannot query the same data** — choose one model per cluster

### DocumentDB Gotchas
1. **DocumentDB is NOT MongoDB** — some MongoDB features missing or behave differently; always test application compatibility
2. **No public endpoints** — VPC only; applications outside VPC must use VPN or Direct Connect
3. **Change streams work differently** from MongoDB change streams
4. **Exam trap:** "MongoDB-compatible" does not mean "identical to MongoDB"

### Keyspaces Gotchas
1. **Not full Cassandra** — some CQL features not supported; lightweight transactions (LWT) have restrictions
2. **No Cassandra multi-datacenter replication via CQL** — Keyspaces is regional
3. **On-demand vs provisioned** — same trade-off as DynamoDB; choose based on traffic predictability

### Timestream Gotchas
1. **Append-only** — cannot update a past measurement; new measurement = new row
2. **Memory store must have shorter or equal retention than magnetic store**
3. **Dimensions are metadata** — they should be low-cardinality (device_id, region); high-cardinality dimensions bloat storage
4. **Queries spanning both stores are slower** than memory-only queries

### QLDB Gotchas
1. **QLDB is CENTRALIZED** — the biggest exam trap. "Multiple untrusting parties" → Managed Blockchain, NOT QLDB
2. **Cannot delete journal data** — immutability means you cannot remove old entries; plan data lifecycle carefully
3. **History queries are full journal scans** — expensive for old ledgers with many revisions
4. **No multi-region** — single region only; no built-in DR replication (must use QLDB Streams to replicate to another region manually)
5. **QLDB uses Amazon Ion, not JSON** — drivers handle serialization but it is a distinct format

---

## 12. Hands-On Lab — Redshift Serverless + Quick Query

### Lab Goal
Create a Redshift Serverless namespace, load public data from S3, run analytical queries, and explore Redshift Spectrum.

### Prerequisites
- AWS account (Redshift Serverless has no free tier but cost is very low for small test queries; use minimum RPU)

---

### Step 1: Create Redshift Serverless Namespace

```bash
# Create namespace (database container)
aws redshift-serverless create-namespace \
  --namespace-name lab-namespace \
  --db-name labdb \
  --admin-username admin \
  --admin-user-password "LabPass123!" \
  --iam-roles "arn:aws:iam::ACCOUNT_ID:role/RedshiftS3Role"

# Create workgroup (compute)
aws redshift-serverless create-workgroup \
  --workgroup-name lab-workgroup \
  --namespace-name lab-namespace \
  --base-capacity 8 \
  --publicly-accessible \
  --security-group-ids sg-XXXXXXXX \
  --subnet-ids subnet-XXXXXXXX subnet-YYYYYYYY
```

### Step 2: Create IAM Role for S3 Access

```bash
# Create trust policy
cat > redshift-trust.json << 'EOF'
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {"Service": "redshift.amazonaws.com"},
    "Action": "sts:AssumeRole"
  }]
}
EOF

aws iam create-role \
  --role-name RedshiftS3Role \
  --assume-role-policy-document file://redshift-trust.json

aws iam attach-role-policy \
  --role-name RedshiftS3Role \
  --policy-arn arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess
```

### Step 3: Load Public Sample Data via COPY

```sql
-- Connect via Redshift Query Editor v2 in Console
-- Or use psql: psql -h <workgroup-endpoint> -U admin -d labdb -p 5439

-- Create table
CREATE TABLE tickets (
    ticketid    INTEGER NOT NULL,
    venueid     SMALLINT NOT NULL,
    eventid     INTEGER NOT NULL,
    dateid      SMALLINT NOT NULL,
    pricepaid   DECIMAL(8,2),
    saletime    TIMESTAMP
);

-- Load from AWS public sample data
COPY tickets
FROM 's3://awssampledbuswest2/tickit/sales_tab.txt'
IAM_ROLE 'arn:aws:iam::ACCOUNT_ID:role/RedshiftS3Role'
DELIMITER '\t'
TIMEFORMAT 'MM/DD/YYYY HH:MI:SS'
REGION 'us-west-2';
```

### Step 4: Run Analytical Queries

```sql
-- Total sales by month
SELECT
    EXTRACT(YEAR FROM saletime) AS year,
    EXTRACT(MONTH FROM saletime) AS month,
    COUNT(*) AS num_sales,
    SUM(pricepaid) AS total_revenue,
    AVG(pricepaid) AS avg_price
FROM tickets
GROUP BY year, month
ORDER BY year, month;

-- Top 10 events by revenue
SELECT eventid, SUM(pricepaid) AS total
FROM tickets
GROUP BY eventid
ORDER BY total DESC
LIMIT 10;

-- Distribution of ticket prices (histogram)
SELECT
    CASE
        WHEN pricepaid < 50  THEN '0-50'
        WHEN pricepaid < 100 THEN '50-100'
        WHEN pricepaid < 200 THEN '100-200'
        ELSE '200+'
    END AS price_bucket,
    COUNT(*) AS count
FROM tickets
GROUP BY price_bucket
ORDER BY price_bucket;
```

### Step 5: Check Query Performance (Result Cache)

```sql
-- Run the same query twice
SELECT COUNT(*), SUM(pricepaid) FROM tickets;
-- Second run: check execution time — should be near-instant (result cache hit)

-- Confirm cache hit
SELECT query, result_cache_hit, elapsed_time
FROM svl_query_summary
ORDER BY starttime DESC
LIMIT 5;
```

### Step 6: Create External Table with Spectrum

```sql
-- First create external schema pointing to Glue Data Catalog
CREATE EXTERNAL SCHEMA spectrum_schema
FROM DATA CATALOG
DATABASE 'spectrum_db'
IAM_ROLE 'arn:aws:iam::ACCOUNT_ID:role/RedshiftS3Role'
CREATE EXTERNAL DATABASE IF NOT EXISTS;

-- Create external table pointing to S3
CREATE EXTERNAL TABLE spectrum_schema.sales_history (
    ticketid   INT,
    pricepaid  DECIMAL(8,2),
    saletime   VARCHAR(20)
)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY '\t'
STORED AS TEXTFILE
LOCATION 's3://awssampledbuswest2/tickit/';

-- Query S3 data directly without COPY
SELECT COUNT(*) FROM spectrum_schema.sales_history;

-- Join Redshift table with S3 external table
SELECT
    t.eventid,
    COUNT(t.ticketid) AS redshift_count,
    COUNT(s.ticketid) AS spectrum_count
FROM tickets t
JOIN spectrum_schema.sales_history s ON t.ticketid = s.ticketid
GROUP BY t.eventid
LIMIT 10;
```

### Step 7: Cleanup

```bash
# Delete workgroup first, then namespace
aws redshift-serverless delete-workgroup --workgroup-name lab-workgroup
aws redshift-serverless delete-namespace \
  --namespace-name lab-namespace \
  --final-snapshot-name lab-final-snap
```

---

### Key Observations from Lab

1. **COPY parallelism** — COPY splits S3 files across slices for parallel load; a single large file is slower than multiple files
2. **Result cache** — identical queries (same SQL, same data, no intervening writes) return in <1ms from cache
3. **Spectrum vs COPY** — Spectrum is better for cold/archival S3 data you query rarely; COPY into Redshift is better for hot data you query frequently
4. **Leader node overhead** — Redshift Serverless abstracts this, but for large clusters the leader node does not hold data — only coordinates
5. **Distribution style impact** — try `EXPLAIN SELECT ...` to see if queries involve `DS_BCAST_INNER` (broadcast = inefficient join; consider DISTKEY or DISTSTYLE ALL on small tables)
