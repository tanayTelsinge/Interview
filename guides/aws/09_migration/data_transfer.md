# Data Transfer Services — DataSync, Snowball, Transfer Family, Storage Gateway

## 1. The Problem

Moving petabytes of data to AWS over the internet is not always feasible:

- A 100 TB dataset over a 1 Gbps link takes **~10 days** (theoretical) — more like 3–4 weeks in practice
- A 10 PB dataset over the same link takes **~1 year**
- Metered internet connections make large transfers prohibitively expensive
- Ongoing hybrid workloads need low-latency access to on-premises file shares and cloud storage simultaneously
- B2B partners still exchange files via SFTP — migrating to S3 can't break these workflows

AWS built a family of data transfer services matched to data volume, connectivity, and ongoing vs one-time needs.

---

## 2. What AWS Built

| Service | Use Case | Transfer Type |
|---------|----------|---------------|
| **DataSync** | Online scheduled/incremental file sync | Online (network-based) |
| **Snow Family** | Petabyte-scale offline transfer + edge computing | Offline (physical device) |
| **Transfer Family** | Managed SFTP/FTPS/FTP/AS2 into S3/EFS | Online (protocol gateway) |
| **Storage Gateway** | Hybrid cloud storage — on-prem → cloud bridge | Online (ongoing hybrid) |

---

## 3. How It Works

### AWS DataSync

**Purpose:** Online data transfer for NFS/SMB shares and cloud-to-cloud transfers. Automates scheduling, integrity verification, bandwidth throttling, and encryption.

**Architecture:**
```
On-premises NFS/SMB share
      │
  DataSync Agent (VMware/HyperV/KVM VM or EC2 for cloud-to-cloud)
      │
      │ TLS-encrypted, port 443
      ▼
  AWS DataSync Service (managed, serverless transfer engine)
      │
      ▼
  Destination: S3 | EFS | FSx for Windows | FSx for Lustre
```

**Key Capabilities:**
- **Incremental transfers**: only changed files after first full sync
- **Scheduling**: hourly/daily/weekly or on-demand
- **Bandwidth throttling**: limit transfer to avoid saturating production links
- **Integrity verification**: checksum verification of every file (can disable for speed)
- **Encryption**: in-transit TLS, at-rest destination encryption
- **Performance**: up to **10 Gbps per task** (with multiple parallel streams)
- **Metadata preservation**: timestamps, permissions, symlinks

**Supported Paths:**
```
NFS (on-premises)  → S3
SMB (on-premises)  → S3
NFS (on-premises)  → EFS
SMB (on-premises)  → FSx for Windows
EFS               → EFS (cross-region/cross-account)
S3                → S3 (cross-region/cross-account)
S3 → EFS (cloud consolidation)
Azure Blob / GCS  → S3 (agent-based, cloud-to-cloud)
```

**DataSync vs Storage Gateway (ongoing hybrid):**
| | DataSync | Storage Gateway |
|-|----------|----------------|
| Use case | One-time or scheduled migration | Ongoing hybrid — on-prem apps need cloud storage |
| Protocol | NFS/SMB source only | NFS/SMB/iSCSI/SFTP presented to on-prem apps |
| Latency for reads | Not designed for on-prem reads | Local cache for low-latency reads |
| Direction | Primarily source → AWS | Bidirectional (apps read/write locally) |
| Data lives | Fully in AWS after sync | In AWS (local cache on gateway) |

---

### Snow Family

When the network is the bottleneck, ship data physically.

#### AWS Snowcone
| Attribute | Value |
|-----------|-------|
| Storage | 8 TB HDD or 14 TB SSD |
| Compute | 2 vCPU, 4 GB RAM |
| Weight | 4.5 lbs (2 kg) |
| Power | Battery-powered (field deployable), DC adapter |
| Special | DataSync agent built-in — sync directly to AWS when connectivity available |
| Use case | Remote/harsh environments, field data collection, edge computing, IoT |

#### Snowball Edge Storage Optimized
| Attribute | Value |
|-----------|-------|
| Storage | 80 TB usable (HDD) |
| Compute | 40 vCPU, 80 GB RAM |
| Storage network | 25 Gbps (between units in cluster) |
| Clustering | 5–10 nodes for petabyte-scale |
| Use case | Bulk data transfer, high-capacity edge storage |

#### Snowball Edge Compute Optimized
| Attribute | Value |
|-----------|-------|
| Storage | 42 TB HDD + 7.68 TB NVMe SSD |
| Compute | 52 vCPU, 208 GB RAM |
| GPU option | NVIDIA V100 (optional) |
| Use case | Edge ML inference, video processing, industrial data collection |
| Runs | EC2 instances, Lambda functions, AWS IoT Greengrass |

#### AWS Snowmobile
| Attribute | Value |
|-----------|-------|
| Capacity | 100 PB per truck |
| Form factor | 45-foot shipping container on an 18-wheeler |
| Transfer speed | Up to 1 Tbps (from customer data center) — needs 1 Gbps+ connection to truck |
| Use case | Data center decommission, exabyte-scale migration |
| Security | GPS tracked, always-on security, dedicated AWS personnel |

**Snow Family Transfer Process:**
```
1. ORDER → AWS Console: request device(s), specify S3 destination bucket
2. SHIP → AWS ships device to your location (1-3 business days)
3. RECEIVE → Connect device to network, authenticate with unlock code + manifest
4. LOAD → Use OpsHub GUI or CLI to copy data to device
            (can also run EC2/Lambda during this phase for Compute variants)
5. SHIP BACK → Return device to AWS (pre-paid label)
6. INGEST → AWS receives device, validates tamper-evident seals, ingests to S3
7. SANITIZE → Device securely wiped (NIST 800-88 compliant)
Total time: ~1 week each way shipping + loading time
```

**OpsHub:** GUI application for Snowball management (replaces old CLI-only experience). Available for Windows/Mac/Linux.

**Decision by Data Size and Bandwidth:**
```
Available bandwidth → Time to transfer over network:
  10 Mbps  → 1 TB = 12 days  | 10 TB = 4 months  | Use Snow if > 2 weeks
  100 Mbps → 1 TB = 1.2 days | 10 TB = 12 days   | Use Snow if > few TB
  1 Gbps   → 1 TB = 3 hours  | 10 TB = 30 hours  | Use Snow if > 10 TB
  10 Gbps  → 1 TB = 20 min   | 100 TB = 33 hours | Use Snow if > 100 TB

Rule of thumb: If network transfer takes > 1 week → consider Snow
Snowcone:   < 10 TB
Snowball:   10 TB – 10 PB (multiple devices)
Snowmobile: > 10 PB (exabyte-scale)
```

---

### AWS Transfer Family

**Purpose:** Managed SFTP, FTPS, FTP, and AS2 protocol gateway — clients connect with standard protocols, files land in S3 or EFS.

```
External Partner / Client
  │ SFTP (TCP 22) / FTPS / FTP / AS2 (HTTP)
  ▼
Transfer Family Server (AWS-managed)
  │
  ├── Identity Provider:
  │     Service-managed (Transfer stores users/keys)
  │     Active Directory (AWS Managed AD or on-prem AD)
  │     Custom Lambda authorizer (call your auth system)
  │
  ▼
S3 Bucket or EFS File System
```

**Protocols:**
| Protocol | Port | Security | Use Case |
|----------|------|----------|----------|
| **SFTP** | 22 | SSH key or password | Standard secure file transfer |
| **FTPS** | 990 (implicit) / 21 (explicit) | TLS certificates | Legacy partners requiring FTP+TLS |
| **FTP** | 21 | None (plaintext) | Internal networks only, never internet |
| **AS2** | HTTP/HTTPS | Digital signatures + encryption | EDI/B2B (retail, healthcare, finance) |

**Use Case: B2B Partner File Exchange:**
```
Scenario: Partner EDI system sends purchase orders via SFTP daily
  Partner → SFTP to Transfer Family endpoint
  Files land in: s3://company-edi-bucket/incoming/partner-name/
  EventBridge rule triggers Lambda: process purchase order
  Response file written to: s3://company-edi-bucket/outgoing/
  Partner SFTP pulls response

No EC2 instances, no SFTP server to manage
Cost: ~$0.30/hr per protocol endpoint + $0.04/GB transferred
```

---

### AWS Storage Gateway

**Purpose:** Bridge between on-premises applications and AWS cloud storage. Apps use familiar protocols (NFS, SMB, iSCSI); data lives in S3/EBS/Glacier.

**Gateway Types:**

#### S3 File Gateway
```
On-premises apps (NFS/SMB) → S3 File Gateway VM → S3

- Local cache: frequently accessed files cached on gateway
- Remote files: stored in S3 (all storage classes available)
- Metadata: visible as objects in S3 directly
- Use case: file share backed by S3, on-prem → S3 migration while apps still use NFS
```

#### FSx File Gateway
```
On-premises apps (SMB) → FSx File Gateway VM → FSx for Windows File Server

- Local cache for low-latency Windows file access
- Full SMB/NTFS feature set (ACLs, DFS)
- Use case: Windows file shares where data is in FSx but workers need low-latency local access
```

#### Volume Gateway
```
On-premises servers (iSCSI) → Volume Gateway VM → EBS Snapshots in S3

Two modes:
  CACHED volumes:
    - Primary data in S3
    - Frequently accessed data cached locally
    - Volume size: up to 32 TB
    - Cache: up to 16 TB local

  STORED volumes:
    - Primary data stored locally (low latency)
    - Async backup to S3 as EBS snapshots
    - Volume size: up to 16 TB
    - Use case: apps needing low latency (databases) with cloud backup
```

#### Tape Gateway
```
On-premises backup software (Veeam, Veritas, Commvault) → Tape Gateway VM
  → Virtual tapes stored in S3
  → Archived tapes moved to Glacier/Deep Archive

Replaces physical tape library with virtual tapes
Cost: dramatically cheaper than physical tape management
```

---

## 4. Key Config & Limits

| Service | Parameter | Value |
|---------|-----------|-------|
| DataSync | Max throughput per task | 10 Gbps |
| DataSync | Agent location | On-premises VM or EC2 for cloud-to-cloud |
| Snowcone | Storage | 8 TB HDD / 14 TB SSD |
| Snowball Edge Storage | Usable storage | 80 TB |
| Snowball Edge Compute | Compute | 52 vCPU, 208 GB RAM |
| Snowmobile | Capacity | 100 PB |
| Snowmobile | Requires | 1 Gbps+ connection to truck |
| Snow shipping | Each way | ~1 week |
| Transfer Family | Cost | ~$0.30/hr per endpoint protocol |
| Transfer Family | Data transfer | $0.04/GB (in/out) |
| S3 File Gateway | Cache | Up to 64 TB (hardware appliance) |
| Volume Gateway (Cached) | Max volume | 32 TB |
| Volume Gateway (Stored) | Max volume | 16 TB |

---

## 5. Decision Tree

### Online vs Snow by Data Size/Bandwidth
```
How much data to transfer? How long can it take?
│
├─ < 1 TB and good bandwidth (100 Mbps+)?
│   └─ DataSync (online, scheduled)
│
├─ 1–10 TB and 1 Gbps?
│   └─ DataSync (completes in hours to days)
│
├─ 10–80 TB or limited bandwidth (< 100 Mbps)?
│   └─ Snowball Edge Storage Optimized (single device)
│
├─ 80 TB – 10 PB?
│   └─ Multiple Snowball Edge devices (up to 10 in cluster)
│
└─ > 10 PB (data center decommission)?
    └─ Snowmobile
```

### Which Storage Gateway Type?
```
What protocol does the on-prem application use?
│
├─ NFS → S3 File Gateway
├─ SMB (Windows) accessing FSx? → FSx File Gateway
├─ SMB (Windows) → S3 → S3 File Gateway
├─ iSCSI block storage
│   ├─ Low latency required (DB)? → Volume Gateway STORED mode
│   └─ Cost optimization, cloud primary? → Volume Gateway CACHED mode
└─ Tape backup software → Tape Gateway
```

### DataSync vs Transfer Family vs Storage Gateway
```
Do on-premises applications need to READ/WRITE to cloud storage in real-time?
├─ YES → Storage Gateway (local cache, bidirectional)
└─ NO
   └─ Do external partners need SFTP/FTPS/FTP/AS2 access?
      ├─ YES → Transfer Family
      └─ NO
         └─ Scheduled/one-time file migration from NFS/SMB to S3/EFS?
            └─ DataSync
```

---

## 6. Common Patterns

### Pattern 1: Data Center to S3 Migration with DataSync
```
1. Deploy DataSync agent VM on-premises (VMware/HyperV)
2. Create DataSync source location: NFS share (nfs://fileserver/share)
3. Create DataSync destination location: S3 bucket
4. Create DataSync task:
   - Schedule: daily at 2 AM
   - Bandwidth limit: 500 Mbps (protect production)
   - Verification: full (validate checksums)
5. Run initial full sync → Monitor in CloudWatch
6. Subsequent runs: incremental (only changed files)
7. Final cutover: last sync + redirect app to S3/S3 File Gateway
```

### Pattern 2: Snowball for Data Center Migration
```
1. AWS ships 4x Snowball Edge Storage Optimized (4 × 80 TB = 320 TB)
2. Connect to data center switch (10 Gbps)
3. Use OpsHub to copy:
   - File servers → Snowball /mnt/shares/
   - NAS volumes → Snowball
4. DataSync built-in handles checksums
5. Ship back → AWS ingests to S3 in target region
6. Verify: compare checksums, spot-check files
7. DataSync online sync captures any changes made during shipping window
```

### Pattern 3: B2B Partner File Exchange (Transfer Family)
```
Architecture:
  Partner1 ──SFTP──┐
  Partner2 ──SFTP──┤
  Partner3 ──AS2───┤→ Transfer Family → S3 → EventBridge → Lambda
                                          ↑
                              S3 lifecycle → Glacier (archive)

Setup:
  1. Transfer Family server (SFTP endpoint)
  2. Service-managed users: one per partner, SSH key authentication
  3. S3 bucket with per-partner prefix (home directory mapping)
  4. EventBridge rule: S3:ObjectCreated → Lambda for processing
  5. Custom domain: sftp.company.com → Transfer Family endpoint
```

### Pattern 4: Hybrid Storage with Storage Gateway
```
Branch office scenario:
  Branch file server (NFS) → S3 File Gateway VM → S3 Standard
                                                  ↓
                                            S3 Lifecycle
                                                  ↓
                                         Glacier (after 90 days)

Benefits:
  - Branch users see normal NFS share (fast local cache)
  - Data durably stored in S3 (11 9s durability)
  - Old files auto-archived to Glacier
  - DR: any branch can restore files from S3
  - Cost: S3 << NAS storage costs
```

---

## 7. Gotchas

| Gotcha | Detail |
|--------|--------|
| **Snowball 1 week each way** | Plan for 2 weeks in-transit + loading time. For migrations with a hard deadline, order early. You can have multiple devices in flight. |
| **DataSync agent VM needs resources** | Minimum: 4 vCPU, 32 GB RAM for full performance. Under-sized agent = throughput bottleneck, not network. |
| **Transfer Family ~$0.30/hr per endpoint** | Each protocol (SFTP, FTPS) is a separate endpoint — costs add up. ~$216/month per endpoint even with zero transfers. |
| **Storage Gateway VM sizing matters** | Gateway cache is local disk. Under-provision and read cache misses spike — all reads go to S3 with latency. |
| **Snowmobile needs 1 Gbps to truck** | If your data center doesn't have 1 Gbps+ available, Snowmobile's loading speed is limited by your network to the truck. |
| **DataSync doesn't preserve all metadata on S3** | S3 is object storage — POSIX permissions don't translate. File Gateway preserves metadata in S3 object metadata but S3 itself ignores them. |
| **Snow devices don't work in all countries** | AWS Snowball is available in specific countries. Check availability before ordering for international offices. |
| **Transfer Family FTP is unencrypted** | FTP (not FTPS) sends credentials and data in plaintext. Never expose FTP endpoint to the public internet. |
| **Volume Gateway Stored mode = local primary** | Stored volumes keep primary data on-prem. If the local storage fails before S3 snapshot, data in that window is lost. Snapshot frequency matters. |

---

## 8. Hands-On Lab — Free Tier

**Goal:** Deploy a DataSync agent on EC2 and sync files between two S3 buckets (cross-region).

### Step 1: Create Source and Destination S3 Buckets
```bash
# Source bucket (us-east-1)
aws s3 mb s3://datasync-lab-source-$(date +%s) --region us-east-1

# Destination bucket (us-west-2)
aws s3 mb s3://datasync-lab-dest-$(date +%s) --region us-west-2

# Upload test files to source
echo "test file 1" > /tmp/test1.txt
echo "test file 2" > /tmp/test2.txt
aws s3 cp /tmp/test1.txt s3://datasync-lab-source-XXXXX/
aws s3 cp /tmp/test2.txt s3://datasync-lab-source-XXXXX/
```

### Step 2: Deploy DataSync Agent on EC2 (AMI-based)
```
EC2 Console → Launch Instance
  AMI: Search "DataSync" in AWS Marketplace AMIs
       Use: "AWS DataSync" official AMI
  Instance type: m5.2xlarge (recommended for production; t3.micro for lab testing)
  VPC: Default VPC, public subnet
  Security Group: Allow inbound TCP 80 (activation) from your IP
  IAM Role: Role with DataSync permissions

# After launch, note the EC2 private IP
```

### Step 3: Activate the DataSync Agent
```
DataSync Console → Agents → Create agent
  Activation method: Automatically get key from agent
  Agent address: <EC2 private IP or public IP>

  (DataSync contacts agent on port 80 to get activation key)

  Agent name: lab-agent-01
  → Click "Create agent"
  → Status should become "Online"
```

### Step 4: Create Source Location (S3)
```
DataSync Console → Locations → Create location
  Location type: Amazon S3
  Region: us-east-1
  S3 bucket: datasync-lab-source-XXXXX
  S3 storage class: Standard
  IAM role: Create new (DataSync will create role with S3 permissions)
  Folder: / (root)
```

### Step 5: Create Destination Location (S3)
```
DataSync Console → Locations → Create location
  Location type: Amazon S3
  Region: us-west-2
  S3 bucket: datasync-lab-dest-XXXXX
  S3 storage class: Standard
  IAM role: Create new
```

### Step 6: Create and Run DataSync Task
```
DataSync Console → Tasks → Create task
  Source: (your source S3 location)
  Agent: lab-agent-01 (for S3-to-S3 in same account, agent is optional)
  Destination: (your dest S3 location)

  Task settings:
    Verify data: Check integrity during transfer
    Log level: Basic (CloudWatch Logs)
    Bandwidth limit: No limit (lab)

  Data transfer configuration:
    Objects to transfer: All

→ Create task → Start → Start with defaults

Monitor: Task execution → Status: Running → Success
```

### Step 7: Verify
```bash
aws s3 ls s3://datasync-lab-dest-XXXXX/
# Should see test1.txt and test2.txt
```

### Step 8: Clean Up
```bash
# DataSync: Delete task → Delete locations → Delete agent
# EC2: Terminate agent instance
aws s3 rm s3://datasync-lab-source-XXXXX --recursive
aws s3 rm s3://datasync-lab-dest-XXXXX --recursive
aws s3 rb s3://datasync-lab-source-XXXXX
aws s3 rb s3://datasync-lab-dest-XXXXX
```

---

## Summary Reference Card

```
DATA TRANSFER DECISION:
  Online sync (files, scheduled): DataSync (agent-based, up to 10Gbps)
  Physical bulk transfer: Snow Family
    Snowcone:   < 10 TB, ruggedized/remote
    Snowball:   10 TB – 10 PB
    Snowmobile: > 10 PB (data center scale)
  Partner file exchange (SFTP/FTPS/FTP/AS2): Transfer Family
  Hybrid on-prem+cloud: Storage Gateway
    S3 File Gateway: NFS/SMB → S3
    FSx File Gateway: SMB → FSx for Windows
    Volume Gateway: iSCSI block → EBS snapshots
    Tape Gateway: virtual tapes → S3/Glacier

RULE OF THUMB (Snow vs DataSync):
  > 1 week over network → consider Snow
```
