# EFS and FSx — Shared File Systems

## The Problem
EBS attaches to one EC2 in one AZ. Many workloads require multiple servers reading/writing the same files simultaneously: web servers sharing uploads, ML training jobs reading the same dataset from multiple GPUs, Windows apps requiring SMB shares, HPC clusters on shared storage.

S3 can store files but doesn't support POSIX filesystem semantics (file locking, append, directory ops). You need a real shared filesystem.

---

## What AWS Built
Managed shared filesystems: EFS (Linux NFS), FSx for Windows (SMB/NTFS), FSx for Lustre (HPC/ML), FSx for NetApp ONTAP (multi-protocol enterprise).

---

## Amazon EFS — Elastic File System

### How It Works
```
EC2 (AZ-a), EC2 (AZ-b), EC2 (AZ-c), Lambda, ECS Fargate
  └── all mount via NFS v4.1 ──→ EFS (multi-AZ distributed storage)
```
Auto-scales storage. No provisioning.

### Performance Modes
| Mode | Latency | Max IOPS | Use Case |
|---|---|---|---|
| **General Purpose** (default) | Lower | 35,000 | Web, CMS, containers |
| **Max I/O** (legacy) | Higher | Unlimited | Massively parallel big data |

AWS recommends General Purpose + Elastic Throughput for all new workloads.

### Throughput Modes
| Mode | How It Works |
|---|---|
| **Bursting** | Scales with storage size, burst credits |
| **Provisioned** | Fixed throughput regardless of storage |
| **Elastic** (recommended) | Auto-scales up/down based on demand |

### Storage Tiers
```
Standard      → Frequently accessed        $0.30/GB
Standard-IA   → Infrequently (30+ days)    $0.025/GB
Archive       → Rarely (90+ days)          $0.008/GB

Lifecycle management moves files automatically between tiers.
```

### Mounting
```bash
sudo yum install -y amazon-efs-utils
sudo mount -t efs -o tls,iam fs-xxxxxxxx:/ /mnt/efs
# /etc/fstab: fs-xxxxxxxx:/ /mnt/efs efs defaults,tls,iam 0 0
```

---

## FSx for Windows File Server

**Problem:** Windows apps need SMB/CIFS + NTFS + Active Directory.

```
Windows EC2 / on-prem Windows ──── SMB ────→ FSx for Windows (AD-joined)
```

**Key Features:**
- AD integration (domain-joined, Windows ACLs)
- DFS Namespaces (aggregate shares under single namespace)
- Shadow Copies (VSS point-in-time snapshots)
- Multi-AZ option (Active + Standby, automatic failover)
- SMB encryption

**Use when:** Lift-and-shift Windows file servers, SharePoint, SQL Server UNC paths, Windows home directories.

---

## FSx for Lustre

**Problem:** HPC and ML jobs need hundreds of GB/s throughput. EFS max ~10 GB/s isn't enough.

```
100s of GPU nodes ──── parallel NFS ────→ FSx for Lustre
                                              ↕ optional
                                         S3 data lake
```

### Deployment Types
| Type | Data Persistence | Use Case |
|---|---|---|
| **Scratch 1/2** | Lost on failure | Temporary jobs, cheapest |
| **Persistent 1/2** | HA, replicated within AZ | Production ML, ongoing |

### S3 Integration
Files loaded lazily from S3 on first access. Results can be exported back to S3. Bidirectional sync via Data Repository Associations.

**Performance:** Up to 1,000 MB/s per TiB, sub-millisecond latency, scales to hundreds of TB.

**Use when:** ML training (SageMaker, GPU clusters), HPC simulation, video rendering.

---

## FSx for NetApp ONTAP

Multi-protocol (NFS + SMB + iSCSI) on same data. Features: snapshots, SnapMirror replication, FlexClone (instant zero-copy clones), compression/dedup, tiering to S3.

**Use when:** Migrating on-prem NetApp, multi-protocol workloads, need FlexClone for dev/test data management.

---

## FSx for OpenZFS

Linux NFS + advanced ZFS features (snapshots, clones). Sub-ms latency, up to 12.5 GB/s. Use for ZFS migrations or high-performance NFS with data management.

---

## Decision Tree

```
Protocol / OS?
├── Linux NFS, standard workloads → EFS (General Purpose + Elastic)
├── Linux NFS, HPC/ML, need >10 GB/s → FSx for Lustre
├── Windows, SMB, Active Directory → FSx for Windows File Server
├── Multi-protocol (NFS + SMB + iSCSI) → FSx for NetApp ONTAP
└── Linux NFS + ZFS features → FSx for OpenZFS
```

---

## Key Limits

| Service | Key Limit |
|---|---|
| EFS General Purpose max IOPS | 35,000 |
| EFS Elastic max throughput | 10 GB/s |
| FSx Lustre max throughput | ~1,000 MB/s per TiB |
| FSx Windows max throughput | 2 GB/s |

---

## Gotchas
1. **EFS is Linux/NFS only** — don't try to mount on Windows
2. **FSx Lustre Scratch loses data on failure** — use Persistent for production
3. **EFS bursting limited for small filesystems** — use Elastic throughput if small data + high throughput
4. **EFS charges for all tiers** — set lifecycle policies to move cold data to Archive ($0.008/GB)
5. **Mount target per AZ** — EFS needs a mount target (ENI) in each AZ, allow NFS port 2049

---

## Hands-On Lab (Free Tier)

```bash
# Create EFS
aws efs create-file-system --performance-mode generalPurpose \
  --throughput-mode elastic --encrypted \
  --tags Key=Name,Value=MyEFS

# Create mount target in subnet
aws efs create-mount-target --file-system-id fs-xxx \
  --subnet-id subnet-xxx --security-groups sg-xxx

# Mount on EC2
sudo yum install -y amazon-efs-utils
sudo mkdir /efs
sudo mount -t efs -o tls fs-xxx:/ /efs

# Verify: write from one EC2, read from another in different AZ
echo "test" > /efs/test.txt
# On second EC2: cat /efs/test.txt

# Clean up
sudo umount /efs
aws efs delete-mount-target --mount-target-id fsmt-xxx
aws efs delete-file-system --file-system-id fs-xxx
```
