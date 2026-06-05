# Server and Database Migration — MGN, DMS, SCT

## 1. The Problem

Traditional server and database migrations are slow and risky:

- **Servers**: Manually rebuilding an application server on EC2 means reinstalling OS, reconfiguring software, re-testing — days or weeks per server. A 500-server estate becomes a multi-year project.
- **Databases**: Copying a production Oracle database to Aurora requires schema conversion (Oracle SQL ≠ PostgreSQL SQL), data type mapping, stored procedure rewriting, and a cutover strategy that doesn't lose transactions.
- **Downtime**: Every migration has a cutover window — the smaller the better. Days of downtime is unacceptable for production.

AWS built dedicated services to automate each problem: MGN for servers, DMS+SCT for databases.

---

## 2. What AWS Built

| Service | Purpose |
|---------|---------|
| **Application Migration Service (MGN)** | Continuous block-level replication of entire servers → EC2; replaces SMS and CloudEndure |
| **Database Migration Service (DMS)** | Replicate database data between engines (same or different); supports CDC for near-zero downtime |
| **Schema Conversion Tool (SCT)** | Convert schema, stored procedures, views, triggers from one DB engine to another |
| **VM Import/Export** | One-time import of VMware/HyperV/VirtualBox images to AMI; export EC2 back to VM format |

---

## 3. How It Works

### AWS Application Migration Service (MGN)

MGN replaces both the old Server Migration Service (SMS) and CloudEndure Migration.

**Architecture:**
```
Source Server (physical/VMware/HyperV/other cloud)
      │
      │ TCP 443 + 1500 (data channel)
      ▼
  MGN Replication Agent (installed on source)
      │
      ▼
  Staging Area (your AWS VPC)
    ├── Replication Servers (auto-managed EC2)
    └── Staging EBS volumes (receive block-level changes)
      │
      ▼
  Launch Settings (configured by you)
    ├── Target instance type
    ├── Target subnet / security group
    └── Post-launch actions (SSM scripts)
      │
      ▼
  TEST Instance → validate app works
      │
      ▼
  CUTOVER → final sync + launch production instance
```

**Replication Process:**
1. **Install agent** on source server (Linux: `.deb`/`.rpm`; Windows: `.exe`)
2. Agent connects to MGN staging area via HTTPS (port 443) + data channel (port 1500)
3. **Initial sync**: full disk copy to staging EBS volumes (~hours depending on disk size)
4. **Continuous replication**: block-level changes replicated in near-real-time (RPO = seconds)
5. **Test cutover**: launch a test instance from replicated snapshot — validate without stopping source
6. **Final cutover**: stop source, trigger final sync, launch production instance
7. **Downtime window**: 15–30 minutes (final sync + boot + health check)

**Supported Sources:**
- Physical servers (bare metal)
- VMware vSphere
- Microsoft Hyper-V
- Other clouds (Azure, GCP, on-prem other)
- AWS regions (cross-region migration)

**Launch Settings:**
```json
{
  "instanceType": "m5.xlarge",
  "subnetId": "subnet-xxx",
  "securityGroupIds": ["sg-xxx"],
  "iamInstanceProfileName": "MigrationProfile",
  "postLaunchActions": {
    "ssmDocuments": ["AWSMigration-Bootstrap-Linux"]
  }
}
```

---

### AWS Database Migration Service (DMS)

**Architecture Components:**
```
Source Endpoint          Replication Instance          Target Endpoint
(Oracle, MySQL, etc.) ←→ (EC2-based, you choose size) ←→ (RDS, Aurora, etc.)
                              │
                         Replication Task
                         (defines: what to migrate, how)
```

**Replication Instance:**
- A managed EC2 instance (you select `dms.t3.medium`, `dms.r5.xlarge`, etc.)
- Multi-AZ option for HA
- Sits in your VPC, needs network access to both source and target
- Storage: default 50GB for caching LOB (large objects) and task logs

**Task Types:**
| Type | Description | Use Case |
|------|-------------|----------|
| **Full Load** | Copy all existing data | New migration, no downtime needed |
| **Full Load + CDC** | Copy existing + replicate ongoing changes | Live production migration (main approach) |
| **CDC Only** | Replicate changes only (assumes data already there) | Catch-up after initial bulk load |

**Change Data Capture (CDC):**
```
How CDC works per source:
  Oracle:    Uses LogMiner (reads redo logs) or Binary Reader
             Requires: ARCHIVELOG mode + supplemental logging enabled
  MySQL:     Reads binary log (binlog)
             Requires: binlog_format=ROW, binlog_row_image=FULL
  PostgreSQL: Uses logical replication slots
             Requires: wal_level=logical
  SQL Server: Uses MS-CDC or transaction log backup
             Requires: CDC enabled on source DB + tables

DMS CDC captures: INSERT, UPDATE, DELETE
DMS does NOT capture: DDL changes (schema modifications during migration)
```

**Source → Target Support:**
| Sources | Targets |
|---------|---------|
| Oracle | Amazon RDS (any engine) |
| SQL Server | Aurora (MySQL/PostgreSQL) |
| MySQL / MariaDB | Amazon Redshift |
| PostgreSQL | Amazon DynamoDB |
| MongoDB | Amazon S3 (CSV/Parquet) |
| Amazon S3 | Amazon OpenSearch |
| IBM Db2 | Amazon Kinesis |
| SAP ASE (Sybase) | Kafka (MSK) |
| IBM Informix | DocumentDB |
| Mainframe (IMS/VSAM via CDC) | Neptune |

**Homogeneous vs Heterogeneous:**
```
HOMOGENEOUS (same engine, e.g., MySQL → MySQL, Oracle → Oracle):
  → DMS alone handles data migration
  → No schema conversion needed
  → Full Load + CDC for near-zero downtime

HETEROGENEOUS (different engine, e.g., Oracle → Aurora PostgreSQL):
  → Step 1: SCT converts schema (tables, indexes, views, stored procs)
  → Step 2: DMS migrates data
  → Some SCT-converted objects may need manual adjustment
```

**DMS Pricing:**
- Replication instance: standard EC2 pricing (`dms.t3.medium` ~$0.045/hr)
- Data transfer: standard AWS data transfer rates
- Storage: $0.10/GB-month for replication instance storage

---

### AWS Schema Conversion Tool (SCT)

**What it does:**
- Desktop application (not a managed service — install on laptop/workstation)
- Connects to source and target databases
- Analyzes schema and estimates conversion complexity
- Automatically converts: tables, indexes, sequences, views, functions
- Flags items needing manual review: complex stored procedures, proprietary SQL

**Supported Conversions:**
```
OLTP:
  Oracle       → Aurora PostgreSQL, Aurora MySQL, PostgreSQL, MySQL
  SQL Server   → Aurora PostgreSQL, Aurora MySQL, PostgreSQL, MySQL
  MySQL        → Aurora PostgreSQL, PostgreSQL (limited use case)
  IBM Db2      → Aurora PostgreSQL, PostgreSQL

OLAP/DW:
  Oracle DW    → Amazon Redshift
  SQL Server   → Amazon Redshift
  Teradata     → Amazon Redshift
  Netezza      → Amazon Redshift
  Greenplum    → Amazon Redshift
  Vertica      → Amazon Redshift
```

**SCT Assessment Report:**
```
Output categories:
  Automatically converted (green)  → 60-80% of typical schema
  Converted with warnings (yellow) → Needs review, minor adjustments
  Cannot be converted (red)        → Manual rewrite required

Common red items:
  - Oracle CONNECT BY (hierarchical queries) → CTEs
  - SQL Server cursors → Set-based operations
  - Proprietary functions (ROWNUM, SYSDATE) → PostgreSQL equivalents
  - Complex package bodies with dynamic SQL
```

---

### VM Import/Export

Use case: one-time migration of VM images to AMI format (not continuous replication like MGN).

**Import:**
```bash
# Upload VMDK/VHD/OVA to S3 first, then:
aws ec2 import-image \
  --description "My VM Import" \
  --disk-containers Format=OVA,UserBucket="{S3Bucket=my-bucket,S3Key=my-vm.ova}"

# Check status:
aws ec2 describe-import-image-tasks --import-task-ids import-ami-xxx
```

**Export:**
```bash
aws ec2 create-instance-export-task \
  --instance-id i-xxx \
  --target-environment vmware \
  --export-to-s3-task DiskImageFormat=VMDK,ContainerFormat=OVA,S3Bucket=my-bucket
```

**Supported formats**: VMDK (VMware), VHD (Hyper-V), RAW, OVA
**Not for**: continuous sync, live migration — use MGN for those

---

## 4. Key Config & Limits

| Parameter | Value |
|-----------|-------|
| MGN cutover downtime | 15–30 minutes |
| MGN agent data channel port | TCP 1500 (or HTTPS 443 only mode, slower) |
| MGN RPO during replication | Seconds |
| DMS replication instance — minimum storage | 50 GB |
| DMS Full Load parallelism | 8 threads per table (configurable) |
| DMS CDC latency | Seconds to low minutes |
| DMS LOB (large object) handling | Full LOB mode (slower) vs Limited LOB mode (truncate at limit) |
| SCT — desktop app | Install on client machine, not managed service |
| VM Import max VMDK size | No hard limit, but larger = longer import |
| DMS endpoints — SSL support | Yes (require SSL between endpoints) |

---

## 5. Decision Tree

```
MIGRATING A SERVER (OS + Application):
│
├─ Ongoing live replication needed with minimal downtime?
│   └─ Use MGN (continuous block-level replication)
│
├─ One-time import of VMware/HyperV image?
│   └─ VM Import/Export
│
└─ Moving an entire VMware cluster without re-platforming?
    └─ VMware Cloud on AWS (Relocate strategy)


MIGRATING A DATABASE:
│
├─ Source and target are the SAME engine? (homogeneous)
│   └─ DMS alone (Full Load + CDC)
│
└─ Source and target are DIFFERENT engines? (heterogeneous)
    ├─ Step 1: SCT (schema conversion)
    └─ Step 2: DMS (data migration with CDC)


DMS TASK TYPE:
│
├─ One-time copy, downtime acceptable?
│   └─ Full Load only
│
├─ Production migration, minimize downtime?
│   └─ Full Load + CDC (replicate changes until cutover)
│
└─ Already loaded data separately (bulk load tool), need CDC?
    └─ CDC only
```

---

## 6. Common Patterns

### Pattern 1: Zero-Downtime DB Migration (Oracle → Aurora PostgreSQL)
```
Phase 1: ASSESSMENT (1-2 weeks)
  - Run SCT assessment report
  - Identify manual conversion items
  - Fix Oracle supplemental logging (ALTER DATABASE ADD SUPPLEMENTAL LOG DATA)

Phase 2: SCHEMA MIGRATION (1-2 weeks)
  - SCT converts tables, indexes, views → PostgreSQL
  - Manual fix: stored procedures, packages flagged red in SCT
  - Deploy converted schema to Aurora PostgreSQL

Phase 3: FULL LOAD + CDC (ongoing until cutover)
  - DMS task: Full Load + CDC
  - Monitor: replication lag in DMS console
  - Validate: row counts, sample data comparison

Phase 4: CUTOVER (maintenance window)
  - Stop application writes to Oracle source
  - Wait for DMS lag to reach 0 (all changes caught up)
  - Flip application connection string → Aurora
  - Verify application health
  - Keep Oracle read-only for N days (rollback option)

Downtime = maintenance window only (~minutes)
```

### Pattern 2: Server Farm Migration with MGN
```
Pre-migration:
  1. Install MGN agent on all servers
  2. Configure launch settings (instance type, subnet, SG)
  3. Wait for initial sync to complete

Testing phase:
  1. Launch test instances for each server
  2. Run application smoke tests
  3. Fix any issues in launch settings or post-launch scripts
  4. Terminate test instances (staging volumes still live)

Wave cutover:
  1. Notify stakeholders of maintenance window
  2. Stop application (graceful shutdown)
  3. MGN: trigger final sync for all servers in wave
  4. MGN: launch cutover instances
  5. DNS/load balancer: point to new EC2 instances
  6. Verify health
  7. Total downtime: 20-40 minutes per wave
```

### Pattern 3: DMS for S3 Data Lake Loading
```
Source: Existing relational DB (PostgreSQL)
Target: S3 (for data lake)

DMS task settings:
  Target format: Parquet (not CSV — better Athena performance)
  Partitioning: by date column

DMS → S3 creates:
  s3://my-datalake/schema/table/year=2024/month=01/data.parquet

Then: Glue Crawler discovers schema, Athena queries it
```

### Pattern 4: Mainframe Migration
```
IBM Mainframe (COBOL/PL1 on z/OS)
      ↓
AWS Mainframe Modernization Service
      ├── Replatform path: Managed COBOL runtime (Micro Focus)
      │     Run same COBOL code, AWS manages infrastructure
      └── Refactor path: Automated COBOL → Java (Blu Age)
            Convert COBOL programs to modern Java microservices

DMS handles: DB2 → Aurora PostgreSQL data migration
SCT handles: DB2 SQL → PostgreSQL schema conversion
```

---

## 7. Gotchas

| Gotcha | Detail |
|--------|--------|
| **DMS needs enough storage for LOBs** | Large object (BLOB/CLOB) migration requires LOB mode. Full LOB mode is slow — chunks LOBs in 64KB pieces. Undersize the replication instance and DMS tasks fail with storage errors. |
| **CDC requires source DB transaction log configuration** | MySQL: `binlog_format=ROW` must be set BEFORE migration starts. Oracle: supplemental logging must be enabled. Missed this → CDC doesn't capture changes. |
| **SCT is never 100% automated** | Even simple schemas have 10-20% manual items. Budget 2-4 weeks for stored procedure rewriting on complex Oracle schemas. SCT says "green" but code still may need tuning. |
| **MGN needs network connectivity source → AWS** | Agent must reach AWS endpoints over TCP 443/1500. Firewall teams often forget to open 1500. In restricted networks, use port 443-only mode (slower). |
| **DMS doesn't migrate sequences by default** | Oracle sequences → PostgreSQL sequences need manual creation or SCT handling. |
| **DMS LOB truncation by default** | Default "Limited LOB mode" truncates LOBs at 32KB. Check if your data has LOBs larger than this before migration. |
| **VM Import doesn't support all OS** | Certain OS versions with special kernel modules or proprietary drivers don't import cleanly. Always test import before relying on it. |
| **MGN test cutover doesn't stop source** | Test instances run alongside the live source. Terminate test instances when done — they incur EC2 costs. |
| **DMS task must be stopped then restarted after schema changes** | If you ALTER a table on the source during migration, the DMS task may fail or miss changes. Apply schema changes on both sides first. |

---

## 8. Hands-On Lab — Free Tier

**Goal:** Set up a DMS task to migrate a MySQL database to RDS MySQL (homogeneous = no SCT needed).

### Step 1: Create Source Database (simulate on-prem)
```sql
-- Launch an EC2 instance with MySQL (Free Tier: t2.micro)
-- Or use an existing RDS MySQL for source

-- Create test data:
CREATE DATABASE migration_test;
USE migration_test;

CREATE TABLE customers (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100),
  email VARCHAR(100),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO customers (name, email) VALUES
  ('Alice Smith', 'alice@example.com'),
  ('Bob Jones', 'bob@example.com'),
  ('Carol White', 'carol@example.com');

-- Enable binary logging (required for CDC):
-- Add to /etc/mysql/mysql.conf.d/mysqld.cnf:
--   log_bin = /var/log/mysql/mysql-bin.log
--   binlog_format = ROW
--   binlog_row_image = FULL
--   server_id = 1  (must be unique)
```

### Step 2: Create Target RDS MySQL
```
RDS Console → Create database
  Engine: MySQL 8.0
  Template: Free tier (db.t3.micro)
  DB identifier: migration-target
  Master username: admin
  VPC: same as your EC2 (or ensure connectivity)
  Public access: Yes (for lab only)
```

### Step 3: Create DMS Replication Instance
```
DMS Console → Replication instances → Create
  Name: migration-lab-ri
  Instance class: dms.t3.micro
  Engine version: latest
  VPC: same VPC as source + target
  Multi-AZ: No (lab)
  Storage: 50 GB

Wait: ~5 minutes for provisioning
```

### Step 4: Create Endpoints
```
DMS → Endpoints → Create endpoint

SOURCE endpoint:
  Type: Source
  Engine: MySQL
  Server: <EC2 private IP or hostname>
  Port: 3306
  Username: root (or migration user)
  Password: ***
  Database: migration_test
  → Test connection (must show "Successful")

TARGET endpoint:
  Type: Target
  Engine: MySQL
  Server: <RDS endpoint>
  Port: 3306
  Username: admin
  Password: ***
  Database: migration_test (create this DB on RDS first)
  → Test connection
```

### Step 5: Create Replication Task
```
DMS → Database migration tasks → Create task
  Task identifier: full-load-lab
  Replication instance: migration-lab-ri
  Source endpoint: (your source)
  Target endpoint: (your RDS)
  Migration type: Migrate existing data (Full Load)

Table mappings:
  {
    "rules": [{
      "rule-type": "selection",
      "rule-id": "1",
      "rule-name": "include-all",
      "object-locator": {
        "schema-name": "migration_test",
        "table-name": "%"
      },
      "rule-action": "include"
    }]
  }

→ Create task → Start task automatically: checked
```

### Step 6: Verify Migration
```sql
-- Connect to target RDS:
mysql -h <rds-endpoint> -u admin -p migration_test

SELECT * FROM customers;
-- Should see all 3 rows from source
SELECT COUNT(*) FROM customers;
```

### Step 7: Clean Up
```
DMS: Stop and delete task → Delete endpoints → Delete replication instance
RDS: Delete instance
EC2: Stop/terminate source instance
```

---

## Summary Reference Card

```
SERVER MIGRATION:
  MGN (Application Migration Service):
    - Continuous block-level replication
    - Agent on source → staging area → test → cutover
    - 15-30 min downtime at cutover
    - Supports: physical, VMware, HyperV, other clouds

DATABASE MIGRATION:
  Homogeneous (MySQL→MySQL): DMS alone
  Heterogeneous (Oracle→Aurora): SCT (schema) + DMS (data)

  DMS components: Replication Instance + Source Endpoint + Target Endpoint + Task
  Task types: Full Load | Full Load + CDC | CDC Only
  CDC requirements: source DB transaction log config (binlog, supplemental logging)

  SCT: Desktop app, converts schema + stored procs, never 100% automated

VM IMPORT: One-time VMDK/VHD/OVA → AMI import
```
