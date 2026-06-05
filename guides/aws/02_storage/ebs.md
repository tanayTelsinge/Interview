# Amazon EBS — Elastic Block Store
## SAP-C02 Deep Dive

---

## 1. The Problem

EC2 instances are ephemeral — when an instance terminates, the local instance store is gone. Applications need:
- **Persistence**: data survives instance stop/start/termination
- **Low latency**: block-level access, not object or file
- **Flexibility**: resize, snapshot, encrypt, move between instances
- **Boot volumes**: OS must live somewhere persistent

EC2 instance stores (local NVMe/SSD) are physically attached and fast, but you cannot detach them, snapshot them, or keep them alive if the instance dies.

EBS was built to be the network-attached block device that behaves like a local disk but with cloud-native properties (durability, snapshots, encryption, resizing).

---

## 2. What AWS Built

Amazon EBS provides **persistent, block-level storage volumes** that are created in a specific Availability Zone and attached to EC2 instances over the AWS internal network.

Key properties:
- Block device — formatted with any filesystem (ext4, xfs, NTFS)
- Network-attached (not physically local)
- AZ-scoped — a volume lives in exactly one AZ
- Can be detached and reattached to another instance in the same AZ
- Snapshots stored in S3 (regionally available)
- Independent lifecycle from the EC2 instance

---

## 3. How It Works

### Architecture

```
EC2 Instance (us-east-1a)
    │
    │ (AWS internal network — NVMe over TCP)
    │
EBS Volume (us-east-1a)
    │
    │ (incremental backup)
    ▼
S3 (us-east-1) — Snapshot storage
```

EBS uses **NVMe over TCP** internally. The latency is typically sub-millisecond for io2 volumes. The network path is dedicated and not shared with general instance network traffic when the instance is EBS-optimized.

### EBS-Optimized Instances

EBS-optimized instances provide **dedicated network bandwidth** between the instance and EBS, separate from the instance's general network traffic.

- Most modern instance types are EBS-optimized by default (no extra charge)
- Older instance types: opt-in, additional cost
- Without EBS-optimized: EBS competes with network traffic for bandwidth

### How Volumes Are Physically Stored

EBS data is **replicated within the same AZ** across multiple physical servers. This is why EBS has 99.8%–99.999% durability (depending on volume type) without you doing anything. The replication is transparent.

---

## 4. Volume Types — Complete Specifications

### SSD-Based (IOPS-optimized)

#### gp3 — General Purpose SSD v3 (Recommended Default)

| Spec | Value |
|---|---|
| Baseline IOPS | **3,000 IOPS (always, regardless of size)** |
| Max IOPS | **16,000 IOPS** |
| Max throughput | **1,000 MB/s** |
| IOPS:volume size coupling | **None** (independent) |
| Size range | 1 GiB – 16 TiB |
| Latency | Single-digit ms |
| Cost | ~$0.08/GB/month |
| Extra IOPS cost | ~$0.005/IOPS (above 3,000) |
| Extra throughput cost | ~$0.04/MB/s (above 125 MB/s) |

gp3 is the go-to choice. You get 3,000 IOPS free with every volume, and can scale IOPS independently of size (unlike gp2).

#### gp2 — General Purpose SSD v2 (Legacy)

| Spec | Value |
|---|---|
| Baseline IOPS | **3 IOPS per GiB** (min 100, max 16,000) |
| Burst IOPS | Up to **3,000 IOPS** for volumes < 1 TiB |
| Burst duration | Depends on credit bucket |
| Max throughput | **250 MB/s** (for volumes > ~334 GiB) |
| Credit bucket | Refills at 3 IOPS/GiB/sec baseline rate |
| Credit bucket size | 5.4 million I/O credits |
| Size range | 1 GiB – 16 TiB |

**gp2 Credit Bucket mechanics:**

```
Credit bucket capacity: 5.4 million credits
Earn rate: 3 * volume_size_GiB credits per second
Burst rate: 3,000 IOPS (costs 3,000 credits/second)
Baseline rate: 3 * GiB IOPS

A 100 GiB volume:
  Baseline: 300 IOPS (earns 300 credits/sec)
  Burst: 3,000 IOPS for 5,400,000 / (3000 - 300) = ~2,000 seconds (~33 min)
  If burst depletes: throttled to 300 IOPS

A 334 GiB volume:
  Baseline: 1,002 IOPS (above burst threshold)
  No bursting needed — steady 1,002 IOPS

A 1000 GiB volume:
  Baseline: 3,000 IOPS (maximum burst = baseline)
  Consistent 3,000 IOPS, no credit concept needed
```

**Migrate gp2 to gp3**: almost always cheaper and better. gp3 costs less per GB and provides 3,000 IOPS baseline regardless of size.

#### io2 Block Express — Provisioned IOPS SSD (Highest Performance)

| Spec | Value |
|---|---|
| Max IOPS | **256,000 IOPS** |
| Max throughput | **4,000 MB/s** |
| IOPS:GB ratio | Up to 1,000:1 |
| Latency | Sub-millisecond (< 1ms) |
| Durability | **99.999% (5 nines)** |
| Multi-Attach | Supported (up to 16 instances) |
| Size range | 4 GiB – 64 TiB |
| Cost | ~$0.125/GB/month + $0.065/provisioned IOPS |

Required for: SAP HANA, large Oracle databases, mission-critical workloads.

#### io1 — Provisioned IOPS SSD (Previous Generation)

| Spec | Value |
|---|---|
| Max IOPS | **64,000 IOPS** |
| Max throughput | **1,000 MB/s** |
| IOPS:GB ratio | Up to 50:1 |
| Durability | 99.8%–99.9% |
| Multi-Attach | Supported |
| Size range | 4 GiB – 16 TiB |

io1 is superseded by io2 Block Express. Use io2 for new deployments.

### HDD-Based (Throughput-optimized)

#### st1 — Throughput Optimized HDD

| Spec | Value |
|---|---|
| Max throughput | **500 MB/s** |
| Max IOPS | 500 (1 MB I/Os) |
| Burst throughput | 250 MB/s per TB, max 500 MB/s |
| Baseline | 40 MB/s per TiB |
| Size range | 125 GiB – 16 TiB |
| Cost | ~$0.045/GB/month |
| Use case | Big data, data warehouses, log processing |

Cannot be boot volume. Sequential workloads only.

#### sc1 — Cold HDD (Cheapest)

| Spec | Value |
|---|---|
| Max throughput | **250 MB/s** |
| Max IOPS | 250 (1 MB I/Os) |
| Burst throughput | 80 MB/s per TiB, max 250 MB/s |
| Baseline | 12 MB/s per TiB |
| Size range | 125 GiB – 16 TiB |
| Cost | ~$0.015/GB/month |
| Use case | Infrequently accessed data, lowest cost |

Cannot be boot volume. Coldest/cheapest block storage AWS offers.

---

## 5. EBS vs Instance Store

| Property | EBS | Instance Store |
|---|---|---|
| **Persistence** | Persists independently of instance | Lost on stop/terminate/failure |
| **Durability** | Replicated within AZ | Single physical device |
| **Detachable** | Yes | No |
| **Snapshots** | Yes | No (must image the entire instance) |
| **Encryption** | KMS (at rest) | Varies by instance type |
| **Performance** | Up to 256K IOPS (io2 BE) | Up to millions of IOPS (NVMe local) |
| **Latency** | Sub-ms (io2) to low ms | Microseconds (truly local) |
| **Cost** | Per GB/month + IOPS | Included in instance price |
| **Boot volume** | Yes | Supported on select instances |
| **Max size** | 64 TiB (io2) | Fixed by instance type |
| **Use case** | Databases, OS, general purpose | Caches, temp data, buffers, Hadoop HDFS |

Instance store is ideal for:
- ElastiCache nodes (if you don't need persistence)
- Kafka brokers with replication elsewhere
- Hadoop/Spark temp data
- Gaming leaderboards (ephemeral)

---

## 6. Snapshots

### How Snapshots Work

EBS snapshots are **incremental** and stored in S3 (managed by AWS — you don't see the S3 bucket).

```
Day 1: Full snapshot (10 GB used) → stores 10 GB
Day 2: Changed 2 GB → stores only 2 GB delta
Day 3: Changed 1 GB → stores only 1 GB delta

Total stored: 13 GB (not 30 GB)
But each snapshot is self-sufficient for restore
```

### Snapshot Operations

```bash
# Create snapshot
aws ec2 create-snapshot \
  --volume-id vol-0123456789abcdef0 \
  --description "Pre-deployment backup $(date +%Y-%m-%d)"

# Copy snapshot to another region
aws ec2 copy-snapshot \
  --source-region us-east-1 \
  --source-snapshot-id snap-0123456789abcdef0 \
  --destination-region us-west-2 \
  --description "DR copy"

# Share snapshot with another account
aws ec2 modify-snapshot-attribute \
  --snapshot-id snap-0123456789abcdef0 \
  --attribute createVolumePermission \
  --operation-type add \
  --user-ids 987654321098

# Create volume from snapshot (in different AZ)
aws ec2 create-volume \
  --snapshot-id snap-0123456789abcdef0 \
  --availability-zone us-east-1b \
  --volume-type gp3
```

### Fast Snapshot Restore (FSR)

By default, volumes created from snapshots have **lazy loading** — data is pulled from S3 on first access, causing initial I/O latency. FSR pre-populates the volume cache.

| Property | Value |
|---|---|
| Initialization time | Minutes to hours (without FSR) |
| FSR initialization | Immediate full performance |
| FSR cost | $0.75 per DSU (DSU = enabled AZ per snapshot) |
| Max FSR enables | 50 per region |

```bash
aws ec2 enable-fast-snapshot-restores \
  --availability-zones us-east-1a us-east-1b \
  --source-snapshot-ids snap-0123456789abcdef0
```

Without FSR, you can pre-warm volumes with `fio` or `dd` to force data hydration from S3.

### Amazon Data Lifecycle Manager (DLM)

Automates snapshot creation and retention:

```json
{
  "ExecutionRoleArn": "arn:aws:iam::123:role/AWSDataLifecycleManagerDefaultRole",
  "Description": "Daily snapshots",
  "State": "ENABLED",
  "PolicyDetails": {
    "ResourceTypes": ["VOLUME"],
    "TargetTags": [{"Key": "Backup", "Value": "daily"}],
    "Schedules": [{
      "Name": "Daily",
      "CreateRule": {"Interval": 24, "IntervalUnit": "HOURS", "Times": ["03:00"]},
      "RetainRule": {"Count": 7},
      "CopyTags": true
    }]
  }
}
```

---

## 7. Encryption

### At-Rest Encryption

EBS encryption uses KMS (AWS-managed key `aws/ebs` by default, or your own CMK).

What is encrypted:
- Data at rest on the volume
- Snapshots created from the volume
- Volumes created from encrypted snapshots
- Data in transit between instance and volume (always encrypted regardless)

```bash
# Create encrypted volume
aws ec2 create-volume \
  --size 100 \
  --availability-zone us-east-1a \
  --volume-type gp3 \
  --encrypted \
  --kms-key-id arn:aws:kms:us-east-1:123:key/abc-123
```

### Enable Account-Level Encryption by Default

```bash
aws ec2 enable-ebs-encryption-by-default --region us-east-1
aws ec2 get-ebs-default-kms-key-id
```

Once enabled, all new EBS volumes in the region are encrypted automatically.

### How to Encrypt an Existing Unencrypted Volume

There is no direct in-place encryption. The procedure:

```
Step 1: Create snapshot of unencrypted volume
        ↓
Step 2: Copy snapshot with encryption enabled
        (aws ec2 copy-snapshot --encrypted --kms-key-id ...)
        ↓
Step 3: Create new encrypted volume from encrypted snapshot
        ↓
Step 4: Stop instance, detach old volume, attach new volume
        ↓
Step 5: Start instance, verify, delete old volume + old snapshot
```

```bash
# Step 1: Snapshot
SNAP=$(aws ec2 create-snapshot --volume-id vol-unencrypted \
  --query SnapshotId --output text)

# Wait for snapshot to complete
aws ec2 wait snapshot-completed --snapshot-ids $SNAP

# Step 2: Copy with encryption
ENC_SNAP=$(aws ec2 copy-snapshot \
  --source-region us-east-1 \
  --source-snapshot-id $SNAP \
  --encrypted \
  --kms-key-id alias/aws/ebs \
  --query SnapshotId --output text)

aws ec2 wait snapshot-completed --snapshot-ids $ENC_SNAP

# Step 3: Create new volume
aws ec2 create-volume \
  --snapshot-id $ENC_SNAP \
  --availability-zone us-east-1a \
  --volume-type gp3
```

### In-Transit Encryption

All data between EC2 and EBS is encrypted at the hardware level, **regardless of whether volume encryption is enabled**. This is transparent and cannot be disabled.

---

## 8. Multi-Attach

Allows a single EBS volume to be attached to **up to 16 Nitro-based EC2 instances simultaneously**, all in the same AZ.

### Requirements and Restrictions

| Property | Value |
|---|---|
| Volume types | **io1 or io2 only** (not gp2/gp3/st1/sc1) |
| Max instances | 16 |
| AZ constraint | All instances must be in the **same AZ** |
| Filesystem | Must use **cluster-aware filesystem** (e.g., GFS2, OCFS2, Veritas CFS) |
| OS | Linux only |
| Boot volume | NOT supported |

### Why Cluster-Aware Filesystem?

Standard filesystems (ext4, xfs) assume single-writer exclusivity. With Multi-Attach, multiple instances write simultaneously — without coordination, you get filesystem corruption. Cluster-aware filesystems implement distributed locking to coordinate writes.

Common use cases:
- Oracle RAC (Real Application Clusters)
- High-availability clustered Linux applications
- Custom distributed databases

```bash
# Attach volume to first instance
aws ec2 attach-volume \
  --volume-id vol-io2-multiattach \
  --instance-id i-instance1 \
  --device /dev/sdf

# Attach same volume to second instance
aws ec2 attach-volume \
  --volume-id vol-io2-multiattach \
  --instance-id i-instance2 \
  --device /dev/sdf
```

---

## 9. Throughput vs IOPS — Key Distinction

| Metric | Definition | Bottleneck When |
|---|---|---|
| **IOPS** | I/O operations per second | Many small random reads/writes (e.g., database transactions) |
| **Throughput** | MB/s of data transferred | Large sequential reads/writes (e.g., video streaming, analytics) |

**Relationship**: `Throughput (MB/s) = IOPS × I/O block size`

```
Example: gp3 at 16,000 IOPS with 64 KB blocks
Throughput = 16,000 × 64 KB = 1,024,000 KB/s = 1,000 MB/s ✓ (hits throughput cap)

Example: gp3 at 3,000 IOPS with 64 KB blocks
Throughput = 3,000 × 64 KB = 192,000 KB/s = 187 MB/s (well under cap)
```

For databases with small 4-8 KB I/Os: IOPS is the constraint.
For log ingestion, data warehouses with large 512 KB–1 MB I/Os: throughput is the constraint.

---

## 10. Decision Tree — Volume Type Selection

```
What workload are you running?
│
├─ Boot volume or general workload?
│   └─ gp3 (almost always the right answer)
│
├─ Need > 16,000 IOPS or > 1,000 MB/s?
│   ├─ Yes, highest performance + 99.999% durability?
│   │   └─ io2 Block Express
│   └─ Yes, need up to 64,000 IOPS (legacy)?
│       └─ io1 (prefer io2)
│
├─ Large sequential throughput, cost-sensitive?
│   ├─ Frequent access (data warehouse, Kafka)
│   │   └─ st1 (500 MB/s max)
│   └─ Infrequent access (cold backups on block)
│       └─ sc1 (250 MB/s, cheapest)
│
├─ Need Multi-Attach (shared volume)?
│   └─ io1 or io2 only
│
└─ Currently on gp2?
    └─ Migrate to gp3 (same or better perf, lower cost)
```

### Quick Rules

- Default new volume → **gp3**
- IOPS intensive (Oracle, SQL Server) → **io2 Block Express**
- Big data, Kafka, log files → **st1**
- Cold archive on block → **sc1**
- Never: gp2 (use gp3), io1 (use io2)

---

## 11. Common Patterns

### Pattern 1: Web Server Boot Volume

```
gp3, 30 GiB, 3,000 IOPS (default), 125 MB/s (default)
Cost: ~$2.40/month

Notes:
- Don't over-provision boot volumes
- Most web servers never exceed 1,000 IOPS
- Enable encryption by default
```

### Pattern 2: High-Performance Database (RDS-style)

```
Instance: r6i.4xlarge (EBS-optimized, 9,500 Mbps dedicated)
Volume: io2 Block Express, 1 TiB, 50,000 IOPS
Filesystem: xfs
Mount options: noatime,nodiratime (reduces metadata IOPS)

Backup strategy:
  DLM policy: snapshot every 6 hours, retain 7 days
  Cross-region snapshot copy for DR
```

### Pattern 3: Cost-Optimized Data Lake Ingest Node

```
Hot cache: gp3, 500 GiB, 10,000 IOPS
  └─ Active working set for processing

Cold buffer: st1, 4 TiB, sequential writes
  └─ Raw ingested data before S3 tiering

Lifecycle:
  After processing: data moves to S3 Standard → S3-IA → Glacier
  EBS costs ~$55/month vs S3 costs pennies/GB
```

### Pattern 4: Oracle RAC with Multi-Attach

```
Two EC2 instances (same AZ)
  ├─ Instance A (primary)
  └─ Instance B (standby)
         │ Both attached to:
         ▼
io2 Block Express, 2 TiB, 100,000 IOPS
  + Cluster-aware filesystem (OCFS2 or GFS2)
  + Oracle RAC handles distributed locking

Benefit: sub-ms failover (no volume detach/reattach needed)
```

### Pattern 5: RAID on EBS for Higher Performance

```
RAID 0 (striping) — double throughput and IOPS:
  2× gp3 at 16,000 IOPS each → 32,000 IOPS combined
  2× gp3 at 1,000 MB/s each → 2,000 MB/s combined
  Risk: one volume fails = data loss (take snapshots)

RAID 1 (mirroring) — redundancy:
  2× volumes, identical data
  Rarely needed (EBS already replicated within AZ)

Note: AWS does not recommend RAID 5/6 (write hole problem + parity overhead)
```

---

## 12. Gotchas

### Exam Traps

| Gotcha | Reality |
|---|---|
| gp2 has consistent 3,000 IOPS | Only with burst — small volumes throttle to 3×size IOPS baseline |
| EBS volumes can be attached cross-AZ | FALSE — AZ-locked. Must snapshot and create new volume in target AZ |
| Encrypted volume in-transit is only if you enable encryption | FALSE — in-transit EBS is always encrypted regardless |
| Multi-Attach works with any volume type | FALSE — io1/io2 only |
| Multi-Attach works with any filesystem | FALSE — must be cluster-aware FS |
| Snapshot charges only for changed data | FALSE — you are charged for all data blocks referenced (shared between snapshots doesn't mean free) |
| FSR (Fast Snapshot Restore) is free | FALSE — $0.75/DSU/hour |
| gp3 is always better than gp2 | TRUE for almost all cases — lower cost, better baseline IOPS, independent scaling |
| EBS snapshots are AZ-specific | FALSE — snapshots are regional (can create volumes in any AZ in the region) |

### Production Pitfalls

1. **gp2 IOPS burst depletion**: A 20 GiB gp2 boot volume has a 60 IOPS baseline. Sustained disk activity (OS updates, log processing) drains the credit bucket. Switch to gp3 for consistent performance.

2. **Volume modification throttling**: AWS limits how frequently you can modify a volume (once every 6 hours). Plan ahead for production changes.

3. **Snapshot costs accumulate**: Incremental snapshots reference unchanged blocks from previous snapshots. Deleting the oldest snapshot doesn't free space if blocks are referenced by newer snapshots. Use DLM with retention counts, not manual snapshot management.

4. **EBS AZ lock + Auto Scaling**: If you attach an EBS volume to an Auto Scaling group, all instances must be in the same AZ as the volume. This defeats multi-AZ Auto Scaling. Use EFS instead for shared state, or application-level replication.

5. **io2 Block Express instance requirements**: Requires Nitro-based instances (most modern types). Not all instance families support the full 256K IOPS — check the instance's dedicated EBS bandwidth spec.

6. **Detaching root volumes**: Must stop the instance first (cannot hot-detach the root volume). Non-root volumes can be hot-detached but unmount the filesystem first.

7. **Encryption KMS key deletion**: If you delete the KMS key used to encrypt a volume, that volume's data is permanently inaccessible. Enable key deletion protection and set long deletion windows (30 days).

8. **Multi-Attach shared writes without cluster FS = corruption**: Simply enabling Multi-Attach and mounting ext4 on two instances will corrupt the filesystem. The OS kernel is not designed for concurrent external writes.

---

## 13. Hands-On Lab (Free Tier)

### Lab: EBS Volume Types, Snapshots, and Encryption

**Estimated cost**: $0 (30-minute lab, small volumes)
**Time**: 45 minutes

#### Step 1: Launch EC2 and Create Volumes

```bash
# Get current region
REGION=$(aws configure get region)
AZ="${REGION}a"

# Create a gp3 volume
VOL_GP3=$(aws ec2 create-volume \
  --size 10 \
  --volume-type gp3 \
  --availability-zone $AZ \
  --iops 3000 \
  --throughput 125 \
  --tag-specifications 'ResourceType=volume,Tags=[{Key=Name,Value=lab-gp3}]' \
  --query VolumeId --output text)
echo "gp3 volume: $VOL_GP3"

# Verify specs
aws ec2 describe-volumes --volume-ids $VOL_GP3 \
  --query "Volumes[0].{Type:VolumeType,IOPS:Iops,Throughput:Throughput,Size:Size}"
```

#### Step 2: Attach Volume to EC2 Instance

```bash
# Get your running instance ID
INSTANCE_ID=$(aws ec2 describe-instances \
  --filters "Name=instance-state-name,Values=running" \
  --query "Reservations[0].Instances[0].InstanceId" \
  --output text)

# Attach volume
aws ec2 attach-volume \
  --volume-id $VOL_GP3 \
  --instance-id $INSTANCE_ID \
  --device /dev/sdf

# Wait for attachment
aws ec2 wait volume-in-use --volume-ids $VOL_GP3
```

#### Step 3: Format and Mount (on the EC2 instance via SSM or SSH)

```bash
# On the EC2 instance:
# Find the device (NVMe naming on Nitro instances)
lsblk

# Format with xfs
sudo mkfs.xfs /dev/nvme1n1  # or /dev/sdf depending on instance type

# Create mount point and mount
sudo mkdir /data
sudo mount /dev/nvme1n1 /data

# Write test data
sudo sh -c "dd if=/dev/urandom of=/data/testfile bs=1M count=100"
df -h /data
```

#### Step 4: Take Snapshot and Measure Performance

```bash
# From your local machine — take a snapshot
SNAP=$(aws ec2 create-snapshot \
  --volume-id $VOL_GP3 \
  --description "Lab snapshot $(date +%Y%m%d)" \
  --query SnapshotId --output text)
echo "Snapshot: $SNAP"

# Wait for completion
aws ec2 wait snapshot-completed --snapshot-ids $SNAP

# Check snapshot size
aws ec2 describe-snapshots --snapshot-ids $SNAP \
  --query "Snapshots[0].{SnapshotId:SnapshotId,Size:VolumeSize,State:State}"
```

#### Step 5: Create Encrypted Volume from Snapshot

```bash
# Copy snapshot with encryption
ENC_SNAP=$(aws ec2 copy-snapshot \
  --source-region $REGION \
  --source-snapshot-id $SNAP \
  --encrypted \
  --kms-key-id alias/aws/ebs \
  --description "Encrypted copy" \
  --query SnapshotId --output text)

aws ec2 wait snapshot-completed --snapshot-ids $ENC_SNAP

# Create encrypted volume from snapshot
ENC_VOL=$(aws ec2 create-volume \
  --snapshot-id $ENC_SNAP \
  --availability-zone $AZ \
  --volume-type gp3 \
  --query VolumeId --output text)

# Verify encryption
aws ec2 describe-volumes --volume-ids $ENC_VOL \
  --query "Volumes[0].{Encrypted:Encrypted,KmsKeyId:KmsKeyId}"
```

#### Step 6: Modify Volume (gp3 IOPS increase)

```bash
# Increase IOPS from 3000 to 6000 (hot, no downtime)
aws ec2 modify-volume \
  --volume-id $VOL_GP3 \
  --iops 6000

# Monitor modification state
aws ec2 describe-volumes-modifications \
  --volume-ids $VOL_GP3 \
  --query "VolumesModifications[0].{State:ModificationState,Progress:Progress}"

# On the instance, extend filesystem after modification completes
# (Only needed if you increased size, not IOPS)
# sudo xfs_growfs /data
```

#### Step 7: Benchmark with fio

```bash
# On the EC2 instance — install fio
sudo yum install fio -y  # Amazon Linux

# Random read IOPS test
sudo fio \
  --name=randread \
  --ioengine=libaio \
  --rw=randread \
  --bs=4k \
  --direct=1 \
  --numjobs=4 \
  --size=1G \
  --runtime=60 \
  --filename=/data/fiotest \
  --group_reporting

# Sequential throughput test
sudo fio \
  --name=seqread \
  --ioengine=libaio \
  --rw=read \
  --bs=1M \
  --direct=1 \
  --numjobs=1 \
  --size=4G \
  --runtime=60 \
  --filename=/data/fiotest \
  --group_reporting
```

#### Step 8: Clean Up

```bash
# Unmount and detach (on instance: sudo umount /data)
aws ec2 detach-volume --volume-id $VOL_GP3
aws ec2 wait volume-available --volume-ids $VOL_GP3

# Delete resources
aws ec2 delete-volume --volume-id $VOL_GP3
aws ec2 delete-volume --volume-id $ENC_VOL
aws ec2 delete-snapshot --snapshot-id $SNAP
aws ec2 delete-snapshot --snapshot-id $ENC_SNAP
```

---

## Quick Reference Card

```
gp3:  3,000 IOPS baseline / 16,000 max / 1,000 MB/s / independent scaling
gp2:  3 IOPS/GiB / 3,000 burst / 250 MB/s / credit bucket
io2:  256,000 IOPS / 4,000 MB/s / 99.999% / sub-ms
io1:  64,000 IOPS / 1,000 MB/s / 99.9%
st1:  500 MB/s max / sequential only / no boot
sc1:  250 MB/s max / cheapest / no boot

EBS = AZ-scoped (always)
Snapshots = Regional (can restore to any AZ)
Encryption = In-transit ALWAYS, at-rest OPT-IN (but default account-wide possible)

To encrypt existing volume:
  Snapshot → Copy with encryption → New volume → Swap

Multi-Attach:
  io1/io2 only + same AZ + max 16 instances + cluster-aware FS required

gp2 → gp3 migration: almost always worth it
```
