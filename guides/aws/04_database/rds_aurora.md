# Amazon RDS + Aurora — SAP-C02 Deep Dive

---

## 1. The Problem — Why Managed Databases Were Needed

Running a relational database on EC2 means owning every layer of the stack:

| Concern | Self-Managed on EC2 | RDS Managed |
|---|---|---|
| OS patching | You | AWS |
| DB engine patching | You | AWS (maintenance windows) |
| Storage provisioning | You (EBS sizing) | AWS (auto-scaling optional) |
| Backups + PITR | You (cron + scripts) | AWS (automated 1-35 days) |
| High availability | You (replication setup) | AWS (Multi-AZ one click) |
| Failover | You (DNS update, monitoring) | AWS (auto within 1-2 min) |
| Hardware failure | You (re-provision) | AWS (transparent) |

The operational burden of managing replication, failover, backup windows, storage growth, and patching on EC2 drove the need for a fully managed relational database service.

---

## 2. What AWS Built — Core Definition

**Amazon RDS (Relational Database Service)** is a managed service that runs industry-standard relational database engines on pre-provisioned infrastructure. AWS handles provisioning, patching, backups, monitoring, and failover.

**Amazon Aurora** is AWS's proprietary cloud-native relational engine — MySQL and PostgreSQL compatible — redesigned around a distributed shared-storage architecture that decouples compute from storage for higher durability, throughput, and scaling.

---

## 3. How It Works — Internals and Components

### 3.1 RDS Supported Engines

| Engine | Notes |
|---|---|
| MySQL | Most common open-source choice |
| PostgreSQL | Feature-rich; strong JSON support |
| MariaDB | MySQL fork; slightly different performance characteristics |
| Oracle | Bring Your Own License (BYOL) or License Included (LI) |
| Microsoft SQL Server | BYOL or LI; Express/Web/Standard/Enterprise editions |
| IBM Db2 | Added 2023; BYOL |

### 3.2 RDS Architecture

```
                     ┌─────────────────────────────────┐
                     │         RDS Primary Instance      │
                     │  (EC2-backed, EBS storage)        │
                     │  Handles reads + writes           │
                     └──────────────┬──────────────────┘
                                    │ Synchronous
                                    │ Block-level
                                    │ Replication
                     ┌──────────────▼──────────────────┐
                     │      RDS Standby Instance         │
                     │  (Different AZ, same region)      │
                     │  CANNOT serve reads               │
                     │  Promoted on failover             │
                     └─────────────────────────────────┘
```

- Primary instance writes to EBS in its AZ
- Standby instance is a **synchronous** mirror in a different AZ
- Standby is **NOT** accessible for reads — it exists solely for failover
- On failure, AWS flips the DNS CNAME to the standby (1-2 minutes)
- Storage is EBS — you choose General Purpose (gp2/gp3) or Provisioned IOPS (io1/io2)

### 3.3 Multi-AZ Deployment

```
AZ-A                             AZ-B
┌─────────────────┐              ┌─────────────────┐
│  PRIMARY        │ ──sync──────▶│  STANDBY        │
│  Reads + Writes │              │  No traffic      │
│  EBS Volume     │              │  EBS Volume      │
└─────────────────┘              └─────────────────┘
        ▲
        │
  Application DNS endpoint (same endpoint throughout)
  (DNS flips to standby on failover)
```

Key facts:
- **Synchronous** replication — no data loss on failover (RPO = 0 in theory)
- **Automatic failover** in 1-2 minutes
- Standby is in the **same region** (different AZ)
- **Standby CANNOT serve reads** — it is purely a hot standby
- Failover is triggered by: AZ failure, primary instance failure, DB engine crash, manual reboot with failover, OS maintenance
- Failover is **NOT** triggered by: high CPU utilization, high memory usage, slow queries (critical exam gotcha)

### 3.4 Read Replicas

```
        ┌─────────────────────────────────────────┐
        │           PRIMARY                        │
        │        (reads + writes)                  │
        └──────────────┬──────────────────────────┘
                       │ Asynchronous Replication
              ┌────────┴──────────┐
              ▼                   ▼
    ┌──────────────────┐ ┌──────────────────┐
    │  Read Replica 1  │ │  Read Replica 2  │
    │  (same region)   │ │  (cross-region)  │
    │  Serves SELECTs  │ │  Serves SELECTs  │
    └──────────────────┘ └──────────────────┘
```

Key facts:
- **Asynchronous** replication — slight lag is possible (eventual consistency for replicas)
- Can serve read traffic — offload reporting queries from primary
- Up to **5 Read Replicas** for RDS engines
- Up to **15 Read Replicas** for Aurora
- Can be promoted to a standalone DB (breaks replication, becomes independent primary)
- Can be in **different regions** (cross-region replication)
- Cross-region Read Replicas incur **data transfer costs**
- Within the same region, replication is **free**

### 3.5 Multi-AZ vs Read Replica — Critical Distinction

| Feature | Multi-AZ | Read Replica |
|---|---|---|
| Primary purpose | High availability / DR | Read scaling / reporting |
| Replication type | **Synchronous** | **Asynchronous** |
| Can serve reads? | **No** | **Yes** |
| Failover? | **Automatic** (1-2 min) | Manual promotion only |
| Cross-region? | **No** (same region only) | **Yes** |
| Number of copies | 1 standby | Up to 5 (RDS) / 15 (Aurora) |
| Data loss on failure | Near zero (RPO~0) | Possible (replication lag) |
| Cost trigger | Higher instance cost | Additional instance cost |
| When to choose | Production HA | Read-heavy workloads, reporting |

> **Exam tip:** "Improve availability" → Multi-AZ. "Improve read performance / scale reads" → Read Replica. You can use both simultaneously.

### 3.6 Storage Auto Scaling

- Enabled per instance; set a **Maximum Storage Threshold**
- Auto-scales storage when free space < 10% AND low-space condition persists >5 minutes AND 6 hours since last scaling
- Scales in 5 GB or 10% increments (whichever is larger)
- **Cannot scale down** — storage is one-directional (you can only grow)
- Supported for all RDS engines
- gp3 storage decouples IOPS from storage size (provision IOPS independently)

### 3.7 Backups

| Type | Details |
|---|---|
| Automated Backups | Daily full snapshot + transaction logs every 5 minutes |
| Retention | 1-35 days (default 7; set to 0 to disable) |
| PITR | Restore to any second within retention window |
| Manual Snapshots | User-initiated, retained **indefinitely** (no expiry) |
| Snapshot Restore | Creates a **new RDS instance** (not in-place restore) |
| Cross-region copy | Manual snapshots can be copied to another region |
| Encrypted snapshots | Copies of encrypted DB snapshots are encrypted |

> Disabling automated backups (retention=0) **deletes all existing automated backups**.

### 3.8 RDS Proxy

```
Lambda Functions (1000s concurrent)
         │
         ▼
┌─────────────────────────────┐
│        RDS Proxy             │
│  - Connection Pooling        │
│  - IAM Authentication        │
│  - Secrets Manager           │
│  - Failover routing          │
└─────────────┬───────────────┘
              │ Fewer, long-lived connections
              ▼
        ┌──────────────┐
        │  RDS / Aurora │
        └──────────────┘
```

- Maintains a **pool of connections** to the database
- Lambda opens thousands of short-lived connections → RDS Proxy multiplexes them into fewer DB connections
- Integrates with **Secrets Manager** for credential rotation without connection disruption
- Supports **IAM authentication** — no plaintext passwords in connection strings
- Reduces failover time for Multi-AZ (routes to new primary faster — ~66% faster failover)
- **VPC-only** — not accessible from public internet
- Supported engines: MySQL, PostgreSQL, MariaDB, Oracle, SQL Server, Aurora MySQL, Aurora PostgreSQL

> **Exam gotcha:** RDS Proxy **requires Secrets Manager**. If a question says IAM auth or RDS Proxy is needed, Secrets Manager must be in the architecture.

### 3.9 Parameter Groups vs Option Groups

| Feature | Parameter Group | Option Group |
|---|---|---|
| Purpose | DB engine configuration variables | Add optional engine features |
| Examples | `max_connections`, `innodb_buffer_pool_size`, `log_bin` | Oracle APEX, Oracle OEM agent, MSSQL TDE |
| Applies to | All engines | Oracle, SQL Server, MySQL, MariaDB |
| Default | Default parameter group (cannot modify) | Default option group (cannot modify) |
| Modification | Create custom, attach to instance | Create custom, attach to instance |
| Restart required? | Dynamic vs static parameters (static needs reboot) | Depends on option |

### 3.10 Security and Encryption

**Encryption at rest (KMS):**
- Enabled at instance creation — **cannot add encryption to an existing unencrypted instance directly**
- Encrypts storage, snapshots, automated backups, and Read Replicas
- Uses AWS KMS (CMK or AWS-managed key)

**Encrypting an existing unencrypted DB — required procedure:**

```
1. Take a snapshot of the unencrypted DB
2. Copy the snapshot → enable encryption on the copy
3. Restore the encrypted snapshot → new encrypted DB instance
4. Update application connection string
5. Delete old unencrypted instance
```

**Encryption in transit (SSL/TLS):**
- All engines support SSL/TLS connections
- For MySQL: use `REQUIRE SSL` in user grants
- For PostgreSQL: set `rds.force_ssl=1` parameter
- Download AWS RDS CA certificate bundle

**IAM Database Authentication:**
- Supported: MySQL, PostgreSQL (RDS + Aurora)
- App generates a short-lived **authentication token** (15-minute TTL) using IAM credentials
- Token replaces the database password
- Requires SSL connection
- Useful for: Lambda functions, ECS tasks — avoid storing DB passwords

### 3.11 Monitoring

| Tool | What It Monitors | Granularity |
|---|---|---|
| CloudWatch Metrics | CPU, FreeStorageSpace, DatabaseConnections, ReadIOPS, WriteIOPS | 1-minute (default 5-min) |
| Enhanced Monitoring | OS-level metrics: processes, threads, memory breakdown | 1-60 seconds (agent in DB instance) |
| Performance Insights | SQL-level: top waits, top SQL, active sessions (ASH-like) | 1 second |
| CloudWatch Logs | Error logs, slow query logs, audit logs, general logs | Near-real-time |

> **Exam distinction:** Enhanced Monitoring = OS metrics (what processes are running). Performance Insights = DB-level metrics (which queries are slow, what wait states).

---

## 4. Aurora Architecture

Aurora fundamentally separates compute from storage — this is the key architectural difference.

### 4.1 Aurora Shared Distributed Storage

```
       Writer Instance          Reader 1      Reader 2 ... Reader 15
           │                      │               │
           └──────────────────────┴───────────────┘
                                  │
                    ┌─────────────▼──────────────────┐
                    │    Aurora Shared Storage Volume   │
                    │  6 copies across 3 AZs           │
                    │  AZ-1: copy 1, copy 2            │
                    │  AZ-2: copy 3, copy 4            │
                    │  AZ-3: copy 5, copy 6            │
                    │                                  │
                    │  Write quorum: 4/6               │
                    │  Read  quorum: 3/6               │
                    └──────────────────────────────────┘
```

Key facts:
- Storage is **shared** — all compute instances read from the same storage volume
- **6 copies** across **3 AZs** automatically
- Write quorum: **4 out of 6** copies must acknowledge
- Read quorum: **3 out of 6** copies must acknowledge
- Can tolerate **2 AZ failures** for reads, **1 AZ + 1 node failure** for writes
- Storage auto-grows in **10 GB increments** up to **128 TiB**
- **Billed per GB-month** of storage actually used (not pre-allocated blocks)
- No pre-provisioning — pay for what you use

### 4.2 Aurora Replicas

- Up to **15 Aurora Replicas** (vs 5 for RDS)
- Sub-**10ms replication lag** (because replicas read from shared storage, not replication stream)
- Replicas used for **automatic failover** — promoted in seconds if writer fails
- Readers have their own endpoint; **Reader Endpoint** load-balances across all replicas
- Can set failover priority (tier 0-15) per replica

**Aurora Endpoints:**

| Endpoint | Purpose |
|---|---|
| Cluster Endpoint | Always points to current Writer — use for writes |
| Reader Endpoint | Load-balanced across all Readers — use for reads |
| Instance Endpoint | Direct connection to specific instance |
| Custom Endpoint | User-defined subset of instances (e.g., large instances for analytics) |

### 4.3 Aurora Serverless v2

- Scales in fine-grained **Aurora Capacity Units (ACUs)** — 1 ACU = ~2 GiB memory + proportional CPU/network
- Minimum: **0.5 ACU** (not zero — always has a base cost when running)
- Maximum: up to 128 ACU
- Scales **within milliseconds** (unlike v1 which took minutes)
- Ideal for: unpredictable workloads, dev/test, multi-tenant SaaS
- Can mix Serverless v2 readers with provisioned writers

> **Exam gotcha:** Aurora Serverless v2 minimum is **0.5 ACU, not 0**. It does NOT scale to zero (unlike Lambda). If the question asks about scaling to zero, that is Aurora Serverless v1 (deprecated for most use cases) or a different service.

### 4.4 Aurora Global Database

```
Primary Region (us-east-1)          Secondary Region (eu-west-1)
┌──────────────────────────┐         ┌──────────────────────────┐
│  Writer Cluster           │ ──────▶ │  Read-Only Cluster        │
│  (reads + writes)         │  <1s    │  (reads only)             │
│                           │  repl.  │                           │
└──────────────────────────┘         └──────────────────────────┘
```

- **1 primary region** (read/write) + up to **5 secondary regions** (read-only)
- Cross-region replication lag: **< 1 second** (storage-level replication)
- **RPO < 1 second**, **RTO < 1 minute**
- Promotion of secondary to primary: **< 1 minute** (for DR)
- Ideal for: globally distributed apps, disaster recovery, low-latency local reads

### 4.5 Aurora Multi-Master

- **Multiple writer nodes** in the same region — all nodes can accept writes
- Conflict resolution: optimistic locking — if two nodes write to the same row, one gets a rollback error
- Use case: continuous write availability even during writer failure (zero-downtime writes)
- Not the same as Global Database (which is cross-region, read-only secondaries)
- Currently limited to Aurora MySQL

### 4.6 Aurora Backtrack (MySQL Only)

- **Rewind the cluster** to a previous point in time **without creating a new DB**
- Does NOT create a new cluster — it rewinds in-place (different from PITR restore)
- Backtrack window: configurable (up to 72 hours)
- Use case: "oops I dropped a table" — fast recovery, seconds to minutes
- **MySQL only** — not available for Aurora PostgreSQL

### 4.7 Aurora Machine Learning Integration

| Service | Integration |
|---|---|
| Amazon SageMaker | Call ML inference from SQL via `aws_ml.invoke_endpoint()` |
| Amazon Comprehend | Sentiment analysis on text columns via SQL functions |

Example SQL using Comprehend:
```sql
SELECT product_id,
       aws_comprehend.detect_sentiment(review_text, 'en') AS sentiment
FROM product_reviews;
```

---

## 5. Key Config and Limits

| Parameter | Value |
|---|---|
| Multi-AZ failover time | 1-2 minutes |
| RDS Read Replicas max | 5 |
| Aurora Read Replicas max | 15 |
| Aurora replication lag | < 10 ms |
| Aurora Global DB replication lag | < 1 second |
| Aurora Global DB RPO | < 1 second |
| Aurora Global DB RTO | < 1 minute |
| Aurora storage max | 128 TiB |
| Aurora storage increment | 10 GB auto |
| Aurora Serverless v2 min | 0.5 ACU |
| Aurora Serverless v2 max | 128 ACU |
| Automated backup retention | 1-35 days |
| Manual snapshot retention | Indefinite |
| PITR granularity | 5 minutes (transaction logs) |
| RDS Proxy connection reduction | Up to 99% fewer DB connections |
| IAM auth token TTL | 15 minutes |
| Aurora Backtrack window max | 72 hours |
| Aurora Multi-Master writers | Multiple (same region) |
| Aurora Global DB secondary regions | Up to 5 |

---

## 6. Decision Tree

```
Need a relational database?
│
├─ Already using Oracle or SQL Server?
│   └─ Use RDS Oracle / SQL Server (BYOL or LI)
│
├─ Open source relational (MySQL/PostgreSQL)?
│   │
│   ├─ Need max compatibility with vanilla MySQL/PostgreSQL?
│   │   └─ Use RDS MySQL / RDS PostgreSQL
│   │
│   ├─ Need higher throughput, auto-storage, up to 15 replicas?
│   │   └─ Use Aurora MySQL / Aurora PostgreSQL
│   │
│   ├─ Workload is unpredictable / spiky / dev-test?
│   │   └─ Use Aurora Serverless v2
│   │
│   ├─ Need cross-region DR with < 1s RPO?
│   │   └─ Use Aurora Global Database
│   │
│   └─ Need continuous write availability (zero-downtime writes)?
│       └─ Use Aurora Multi-Master (MySQL)
│
├─ Need to reduce DB connection pressure (Lambda / microservices)?
│   └─ Add RDS Proxy in front of RDS or Aurora
│
├─ Need High Availability in same region?
│   └─ Enable Multi-AZ (not Read Replicas)
│
└─ Need read scale-out?
    └─ Add Read Replicas (up to 5 RDS / 15 Aurora)
```

---

## 7. Common Patterns

### Pattern 1: Standard Production Web App

```
Application (EC2 / ECS)
         │
         ├── Write → Cluster Endpoint → Aurora Writer
         │
         └── Read  → Reader Endpoint → Aurora Readers (x3)
                           │
                    Auto-scaled, sub-10ms lag
```

### Pattern 2: Lambda-to-RDS (Serverless Spike Protection)

```
API Gateway → Lambda (thousands concurrent)
                   │
                   ▼
           ┌─────────────┐
           │  RDS Proxy   │  ← connection pooling
           └──────┬───────┘
                  │ (few dozen connections)
                  ▼
          RDS PostgreSQL (Multi-AZ)
```

Config:
```
RDS Proxy → Secrets Manager (DB credentials)
          → IAM role (for Lambda auth)
          → VPC (same as RDS)
```

### Pattern 3: Cross-Region DR with Aurora Global

```
us-east-1 (Primary)
  Aurora Cluster (Writer + 2 Readers)
        │
        │ < 1 second replication
        ▼
eu-west-1 (Secondary — read-only)
  Aurora Cluster (3 Readers)
  [Can be promoted to Primary in < 1 minute if us-east-1 fails]
```

### Pattern 4: Analytics Read Offloading

```
OLTP Application ──writes──▶ Aurora Writer
                                   │
                                   │ replicated
                                   ▼
                         Custom Endpoint (large r6g.4xlarge instances)
                                   │
                         Analytics / Reporting queries
```

### Pattern 5: Encrypting an Existing Unencrypted RDS Instance

```
Unencrypted RDS
      │
      ▼ Create snapshot
Unencrypted Snapshot
      │
      ▼ Copy with encryption enabled (KMS key)
Encrypted Snapshot
      │
      ▼ Restore
New Encrypted RDS Instance
      │
      ▼ Update app connection string
      │
      ▼ Delete old unencrypted instance
```

---

## 8. Gotchas — Exam Tricks and Production Pitfalls

### Gotcha 1: Multi-AZ failover is NOT triggered by high CPU
- High CPU, OOM, slow queries, table locks → do NOT trigger failover
- Failover triggers: AZ outage, primary instance crash, OS failure, manual reboot-with-failover, network loss

### Gotcha 2: Read Replica lag is asynchronous — data can be stale
- Never use a Read Replica for reads that require the absolute latest data (e.g., "check account balance after deposit")
- For those cases: route to primary, or use Aurora with strongly consistent reads to writer endpoint

### Gotcha 3: Aurora storage is billed per GB-month (not pre-allocated)
- You cannot "waste" storage by over-provisioning with Aurora
- But you also cannot predict costs as precisely as provisioned EBS
- Contrast with RDS where you pre-allocate EBS storage

### Gotcha 4: Aurora Serverless v2 minimum is 0.5 ACU — NOT zero
- It does not scale to zero; you always pay at least for 0.5 ACU when cluster is running
- If you need scale-to-zero, consider Aurora Serverless v1 (older, limited) or a different approach

### Gotcha 5: RDS Proxy requires Secrets Manager
- You cannot use RDS Proxy without storing credentials in Secrets Manager
- If a question involves RDS Proxy or IAM DB auth through a proxy, Secrets Manager is mandatory

### Gotcha 6: You cannot encrypt an existing unencrypted RDS instance in-place
- Must go through: snapshot → encrypted copy → restore to new instance
- The original instance itself cannot be encrypted; encryption is set at creation

### Gotcha 7: Standby (Multi-AZ) CANNOT be used for read offloading
- A common distractor in exam questions: "use Multi-AZ to serve reads" — WRONG
- Only Read Replicas serve reads

### Gotcha 8: Cross-region Read Replicas cost money for data transfer
- Intra-region replication is free; cross-region incurs data transfer charges

### Gotcha 9: Aurora Global Database is storage-level replication — not logical
- It is much faster than DB-level logical replication
- Secondaries share the same storage infrastructure replication mechanism

### Gotcha 10: Promoting an Aurora Read Replica for Global DB failover
- Detach secondary cluster → promotes to independent cluster (old primary becomes orphan)
- Must update application connection strings manually after promotion

### Gotcha 11: Aurora Backtrack only works for MySQL, not PostgreSQL
- A question about "quickly undoing accidental DELETE without creating a new cluster" → Aurora Backtrack → MySQL only

### Gotcha 12: Parameter Groups — static parameters require reboot
- Dynamic parameters apply without restart; static parameters require instance reboot
- "Why didn't my max_connections change take effect?" → Was it a static parameter?

---

## 9. Hands-On Lab — Free Tier Step-by-Step

### Lab Goal
Create an RDS MySQL instance with Multi-AZ, add a Read Replica, test failover, then enable RDS Proxy.

### Prerequisites
- AWS account (free tier: db.t3.micro, 20 GB storage, 750 hours/month for single-AZ)
- Note: Multi-AZ is NOT free tier — use single-AZ for free, or accept small cost for Multi-AZ testing

---

### Step 1: Create VPC and Security Group

```bash
# Create security group for RDS (allow MySQL 3306 from your IP)
aws ec2 create-security-group \
  --group-name rds-lab-sg \
  --description "RDS Lab Security Group" \
  --vpc-id vpc-XXXXXXXX

aws ec2 authorize-security-group-ingress \
  --group-id sg-XXXXXXXX \
  --protocol tcp \
  --port 3306 \
  --cidr YOUR_IP/32
```

### Step 2: Create RDS MySQL (Free Tier — Single AZ)

```bash
aws rds create-db-instance \
  --db-instance-identifier lab-mysql \
  --db-instance-class db.t3.micro \
  --engine mysql \
  --engine-version 8.0 \
  --master-username admin \
  --master-user-password LabPassword123! \
  --allocated-storage 20 \
  --storage-type gp2 \
  --no-multi-az \
  --publicly-accessible \
  --vpc-security-group-ids sg-XXXXXXXX \
  --backup-retention-period 7 \
  --tags Key=Environment,Value=Lab
```

Wait for instance to become `available`:
```bash
aws rds wait db-instance-available --db-instance-identifier lab-mysql
```

### Step 3: Connect and Create Test Data

```bash
# Get endpoint
ENDPOINT=$(aws rds describe-db-instances \
  --db-instance-identifier lab-mysql \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text)

# Connect
mysql -h $ENDPOINT -u admin -pLabPassword123!

# Create test database
mysql> CREATE DATABASE labdb;
mysql> USE labdb;
mysql> CREATE TABLE users (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
mysql> INSERT INTO users (name) VALUES ('Alice'), ('Bob'), ('Charlie');
mysql> SELECT * FROM users;
```

### Step 4: Create a Read Replica

```bash
aws rds create-db-instance-read-replica \
  --db-instance-identifier lab-mysql-replica \
  --source-db-instance-identifier lab-mysql \
  --db-instance-class db.t3.micro

aws rds wait db-instance-available --db-instance-identifier lab-mysql-replica
```

Verify replica lag:
```bash
# Connect to replica and check replication status
REPLICA_ENDPOINT=$(aws rds describe-db-instances \
  --db-instance-identifier lab-mysql-replica \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text)

mysql -h $REPLICA_ENDPOINT -u admin -pLabPassword123!
mysql> SHOW SLAVE STATUS\G
# Look for Seconds_Behind_Master: 0 (means caught up)
```

### Step 5: Enable Storage Autoscaling

```bash
aws rds modify-db-instance \
  --db-instance-identifier lab-mysql \
  --max-allocated-storage 100 \
  --apply-immediately
```

### Step 6: Create a Manual Snapshot

```bash
aws rds create-db-snapshot \
  --db-instance-identifier lab-mysql \
  --db-snapshot-identifier lab-mysql-manual-snap-$(date +%Y%m%d)
```

### Step 7: Enable Performance Insights

```bash
aws rds modify-db-instance \
  --db-instance-identifier lab-mysql \
  --enable-performance-insights \
  --performance-insights-retention-period 7 \
  --apply-immediately
```

Then in the Console: RDS → lab-mysql → Performance Insights tab
- View "DB Load" chart
- See top SQL statements by waits

### Step 8: Store Credentials in Secrets Manager (for RDS Proxy)

```bash
aws secretsmanager create-secret \
  --name rds/lab-mysql/admin \
  --description "RDS lab credentials" \
  --secret-string '{"username":"admin","password":"LabPassword123!"}'
```

### Step 9: Create RDS Proxy

```bash
# First create IAM role for proxy
# (Full IAM setup omitted for brevity — use Console wizard which auto-creates the role)

# Via Console:
# RDS → Proxies → Create Proxy
# - Proxy identifier: lab-mysql-proxy
# - Engine: MySQL
# - Target: lab-mysql
# - Secrets Manager secret: rds/lab-mysql/admin
# - VPC + security group: same as RDS
```

### Step 10: Cleanup (Avoid Costs)

```bash
# Delete Read Replica first
aws rds delete-db-instance \
  --db-instance-identifier lab-mysql-replica \
  --skip-final-snapshot

# Delete Proxy
aws rds delete-db-proxy --db-proxy-name lab-mysql-proxy

# Delete primary (save a final snapshot)
aws rds delete-db-instance \
  --db-instance-identifier lab-mysql \
  --final-db-snapshot-identifier lab-mysql-final-snap

# Delete Secrets Manager secret
aws secretsmanager delete-secret \
  --secret-id rds/lab-mysql/admin \
  --force-delete-without-recovery
```

---

### Key Observations from Lab

1. **Read Replica creation** — you can write to primary while replica is being created; it catches up via binary log
2. **Replica endpoint is different** — applications must explicitly route reads to the replica endpoint
3. **Promoting a replica** — use `aws rds promote-read-replica`; it becomes a standalone DB with no replication
4. **Multi-AZ vs Read Replica** — in the Console you can enable both on the same instance; they serve different purposes
5. **RDS Proxy latency** — adds ~1ms overhead but dramatically reduces connection exhaustion under Lambda bursts
