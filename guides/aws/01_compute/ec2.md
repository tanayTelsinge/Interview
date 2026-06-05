# Amazon EC2 — Elastic Compute Cloud

## The Problem
Before EC2 (2006), launching a server meant:
1. Submitting a hardware purchase order
2. Waiting 4-8 weeks for physical delivery
3. Racking and cabling in a data center
4. Installing OS, configuring network
5. Committing CapEx budget years in advance

You over-provisioned because under-provisioning meant going back to step 1. Utilization rates of 10-20% were normal — paying for servers that sat idle most of the time.

Startups couldn't afford the CapEx. Enterprises couldn't respond to demand spikes. Everyone wasted money on idle hardware.

---

## What AWS Built
Virtual machines (EC2 instances) that launch in **minutes**, billed **per second**, scalable from 1 to thousands — with no long-term hardware commitment. You pick the size, the OS, the storage — and give it back when you're done.

---

## How It Works

### Virtualization Layer
AWS runs on the **Nitro hypervisor** — a purpose-built, lightweight hypervisor that offloads most virtualization functions to dedicated hardware (Nitro cards). This gives near-bare-metal performance with the isolation of VMs.

```
Physical Host (Nitro System)
├── Nitro Card (handles I/O, EBS, VPC networking)
├── Nitro Security Chip (hardware root of trust)
└── Nitro Hypervisor (lightweight, minimal attack surface)
    ├── EC2 Instance (your VM)
    ├── EC2 Instance (another customer's VM — isolated)
    └── ...
```

### AMI (Amazon Machine Image)
An AMI is a snapshot of a server — OS + software + configuration. It's the template for launching instances.

```
AMI contains:
  - Root volume snapshot (OS, pre-installed software)
  - Launch permissions (who can use it)
  - Block device mapping (what EBS volumes to attach)

Types:
  - AWS-provided (Amazon Linux 2023, Ubuntu, Windows Server)
  - AWS Marketplace (pre-configured: Palo Alto NGFW, Kali Linux, etc.)
  - Community AMIs (public, use cautiously)
  - Custom (your own golden AMI with your apps pre-installed)
```

### Instance Lifecycle
```
Pending → Running → Stopping → Stopped → Terminated
                 └→ Rebooting (stays Running, just reboots OS)
                 └→ Hibernating → Stopped (RAM saved to EBS)
```
- **Stopped** instance: EBS root volume persists, instance retains its instance ID, public IP may change (unless Elastic IP)
- **Terminated** instance: deleted permanently. EBS root volume deleted by default (unless `DeleteOnTermination=false`)
- **Instance Store**: data is LOST on stop/terminate/host failure — ephemeral, not persisted

---

## Instance Families

| Family | Optimized For | Typical Use Cases |
|---|---|---|
| **T** (t3, t4g) | Burstable CPU (credit-based) | Dev/test, low-traffic web, microservices |
| **M** (m6i, m7g) | Balanced CPU + Memory | General purpose apps, mid-size DBs |
| **C** (c6i, c7g) | Compute (high CPU:memory ratio) | Web servers, batch, video encoding, HPC |
| **R** (r6i, r7g) | Memory (high memory:CPU ratio) | In-memory DBs, Redis, SAP HANA |
| **X** (x2iedn) | Extreme Memory | SAP HANA, in-memory analytics (up to 24TB RAM) |
| **I** (i4i) | Storage (local NVMe SSD) | NoSQL DBs, data warehousing (Cassandra, MongoDB) |
| **D** (d3) | Dense HDD storage | Distributed file systems, Hadoop |
| **P** (p4d, p5) | GPU (NVIDIA A100/H100) | ML training, deep learning |
| **G** (g5) | GPU (NVIDIA A10G) | ML inference, graphics rendering |
| **Inf** (inf2) | AWS Inferentia chip | Cost-optimized ML inference |
| **Trn** (trn1) | AWS Trainium chip | Cost-optimized ML training |
| **F** | FPGA | Custom hardware acceleration |
| **HPC** (hpc7g) | HPC networking (EFA) | Tightly-coupled HPC workloads |

**Naming convention:** `m6i.2xlarge`
- `m` = family (general purpose)
- `6` = generation (6th)
- `i` = processor (i = Intel, a = AMD, g = AWS Graviton ARM)
- `2xlarge` = size

**Graviton (ARM) instances:** ~40% better price/performance for many workloads. Available across most families (m7g, c7g, r7g, etc.).

---

## Storage: Instance Store vs EBS

| | Instance Store | EBS |
|---|---|---|
| Type | Physical NVMe on host | Network-attached block storage |
| Persistence | Ephemeral (lost on stop/terminate/failure) | Persistent |
| Performance | Highest (direct NVMe) | Very high (io2 up to 256K IOPS) |
| Cost | Included in instance price | Separate charge |
| Use case | Temp files, cache, buffers | OS, databases, persistent data |

---

## Placement Groups

Control WHERE instances are placed on physical hardware:

### Cluster Placement Group
```
Single AZ, same rack (or nearby racks), low-latency networking
  - Sub-10ms latency between instances
  - Up to 100Gbps bandwidth (with enhanced networking)
  - Risk: single AZ failure takes all instances
  Use case: HPC, tightly-coupled distributed computing, low-latency ML training
```

### Spread Placement Group
```
Each instance on DIFFERENT underlying hardware (different racks)
  - Max HA: independent failure domains
  - Limit: 7 instances per AZ per placement group
  Use case: small critical instances that must not share hardware (primary + secondary DB nodes)
```

### Partition Placement Group
```
Instances in groups (partitions), each partition = different rack
  - Up to 7 partitions per AZ
  - Hundreds of instances per partition
  - Instances know which partition they're in (for Hadoop/Kafka rack awareness)
  Use case: HDFS, HBase, Cassandra, Kafka — large distributed systems needing rack awareness
```

---

## EC2 Pricing Models

### On-Demand
- Pay per second (Linux) or per hour (Windows)
- No commitment, most expensive per unit
- Use for: unpredictable workloads, dev/test, short-term jobs

### Reserved Instances (RI)
- 1 or 3 year term, up to 72% discount
- **Standard RI:** Fixed instance family + size + region. Biggest discount. Can sell on RI Marketplace.
- **Convertible RI:** Can change family/OS/tenancy during term. ~54% discount. Cannot sell.
- **Zonal RI:** Specific AZ + capacity reservation
- **Regional RI:** Any AZ in region, no capacity reservation
- Use for: steady-state, predictable workloads (production web servers, DBs)

### Savings Plans
- Commit to $/hour spend, not specific instances
- **Compute Savings Plans:** Any EC2 family/size/region/OS + Fargate + Lambda. ~66% discount.
- **EC2 Instance Savings Plans:** Specific family + region. ~72% discount. Simpler than RI.
- Use for: flexible commitment — like RIs but you don't need to specify instance type upfront

### Spot Instances
- Bid on unused EC2 capacity. Up to 90% discount.
- Can be **interrupted with 2-minute warning** when AWS needs capacity back
- **Spot Fleet:** Request across multiple instance types/AZs to maintain target capacity
- **EC2 Fleet:** Mix On-Demand + Reserved + Spot in one request
- Use for: fault-tolerant batch jobs, stateless web, CI/CD, ML training checkpointing

### Dedicated Instances
- Your instances run on hardware dedicated to your account
- AWS manages placement, you don't control which physical host
- More expensive than On-Demand (~10%)
- Use for: compliance requiring physical isolation

### Dedicated Hosts
- Full physical server allocated to you with **visibility and control over placement**
- Billing per host, not per instance
- Required for **Bring Your Own License (BYOL)** with per-socket/per-core licensing (Oracle, Windows Server, SQL Server)
- Use for: BYOL compliance, regulatory requirements for physical server control

---

## Decision Tree: Pricing Model

```
What's the workload pattern?
│
├── Unpredictable, short-term, or new (can't predict yet)?
│   └── On-Demand
│
├── Steady-state, runs 24/7, 1-3 year commitment possible?
│   ├── Flexible on instance type? → Compute Savings Plans
│   └── Specific instance family needed? → EC2 Instance Savings Plans or Standard RI
│
├── Fault-tolerant, stateless, batch, can handle interruptions?
│   └── Spot Instances (EC2 Fleet or Spot Fleet for capacity reliability)
│
├── Need BYOL (Oracle, Windows per-socket licensing)?
│   └── Dedicated Hosts
│
└── Compliance needs physical isolation (but not BYOL)?
    └── Dedicated Instances
```

---

## Decision Tree: Instance Family

```
What is the bottleneck / primary resource need?
│
├── General web app, balanced CPU/memory?
│   └── M family (m7g for cost, m6i for Intel)
│
├── CPU-bound (video encoding, web server, HPC)?
│   └── C family
│
├── Memory-bound (in-memory DB, SAP HANA, large caches)?
│   ├── < 4TB memory → R family
│   └── > 4TB (SAP HANA, in-memory analytics) → X family
│
├── High-throughput local disk I/O (Cassandra, MongoDB)?
│   └── I family (local NVMe)
│
├── GPU workload?
│   ├── ML training → P family (P4d/P5 for NVIDIA A100/H100)
│   ├── ML inference, graphics → G family
│   └── Cost-optimized inference → Inf family (AWS Inferentia)
│
├── Variable/burstable CPU (dev/test, intermittent)?
│   └── T family (watch for CPU credit exhaustion under sustained load)
│
└── HPC (MPI, tightly-coupled)?
    └── HPC family with EFA
```

---

## Decision Tree: Placement Group

```
What is the priority?
│
├── Lowest latency between instances (HPC, ML training)?
│   └── Cluster Placement Group (same AZ, same rack)
│
├── Maximum isolation of individual instances (critical nodes)?
│   └── Spread Placement Group (different hardware per instance, max 7/AZ)
│
└── Fault domain isolation for large distributed system (Kafka, Hadoop)?
    └── Partition Placement Group (rack-level isolation, 100s of instances)
```

---

## Key Config & Limits

| Parameter | Limit / Default |
|---|---|
| On-Demand vCPU limit (new account) | 32 vCPUs (request increase) |
| Max EBS volumes per instance | 28 (varies by family) |
| Spot interruption notice | 2 minutes |
| User Data script size | 16KB |
| EC2 metadata URL | 169.254.169.254 |
| IMDSv2 token TTL | 1 second to 21,600 seconds |
| Max instances in Spread PG per AZ | 7 |

---

## User Data & Instance Metadata

**User Data:** Script that runs on first boot (or every boot with `--mime-multi-part`)
```bash
#!/bin/bash
yum update -y
yum install -y httpd
systemctl start httpd
systemctl enable httpd
echo "<h1>Hello from $(hostname)</h1>" > /var/www/html/index.html
```

**Instance Metadata Service (IMDS):**
```bash
# IMDSv1 (deprecated, less secure — no auth required)
curl http://169.254.169.254/latest/meta-data/instance-id

# IMDSv2 (required: get token first)
TOKEN=$(curl -X PUT "http://169.254.169.254/latest/api/token" \
  -H "X-aws-ec2-metadata-token-ttl-seconds: 21600")
curl -H "X-aws-ec2-metadata-token: $TOKEN" \
  http://169.254.169.254/latest/meta-data/instance-id

# Common metadata paths:
# /latest/meta-data/instance-id
# /latest/meta-data/instance-type
# /latest/meta-data/public-ipv4
# /latest/meta-data/iam/security-credentials/<role-name>
# /latest/user-data
```

**Enforce IMDSv2** (security best practice):
```bash
aws ec2 modify-instance-metadata-options \
  --instance-id i-1234567890abcdef0 \
  --http-tokens required \
  --http-endpoint enabled
```

---

## Auto Scaling

**Launch Templates** (preferred over Launch Configurations):
- Specify: AMI, instance type, key pair, security groups, user data, IAM role, storage

**Scaling Policies:**
```
Target Tracking  → "Keep CPU at 50%" — simplest, AWS manages scale-in/out
Step Scaling     → "If CPU > 70% add 2; if CPU > 90% add 5" — more control
Scheduled        → "Add 10 instances every Monday 8am" — predictable patterns
Predictive       → ML forecasts load, scales proactively before it arrives
```

**Lifecycle Hooks:** Pause instance launch or termination for custom actions
```
Launch hook: pause before "In Service" → run custom health check / install software
Terminate hook: pause before termination → drain connections / save state / send logs
```

**Warm Pools:** Pool of pre-initialized stopped instances. On scale-out, start from warm pool instead of cold launch (much faster, avoids cold start delay).

---

## Common Patterns

### Stateless Web Tier
```
Route53 → ALB → ASG (EC2 instances in 3 AZs) → RDS Multi-AZ
                     ↓ sessions in ElastiCache Redis
```

### Golden AMI Pipeline
```
Base AMI → CodeBuild (install + harden) → Custom AMI → ASG Launch Template
                                             ↑ quarterly refresh
```

### Spot + On-Demand Mix (Cost Optimization)
```
EC2 Fleet:
  On-Demand base: 20% (always running, stable)
  Spot capacity: 80% (cost-optimized, fault-tolerant app tier)
  Spot Fleet → multiple instance types → reduces interruption probability
```

---

## Gotchas

1. **T-series CPU credits:** T instances burst above baseline using credits. Under sustained high CPU load, credits exhaust → CPU throttled to baseline. Use T4g Unlimited for burst without credit exhaustion (extra cost).

2. **Stopped ≠ Terminated:** Stopped instances still charge for EBS volumes attached. Terminated instances delete EBS root volume by default.

3. **Public IP changes on stop/start:** Public IPv4 is ephemeral. Use Elastic IP for a static public address (1 free per running instance).

4. **Dedicated Instance ≠ Dedicated Host:** Dedicated Instance = dedicated hardware per account (no placement control). Dedicated Host = dedicated physical server with placement control → required for BYOL, not Dedicated Instance.

5. **IMDSv1 security risk:** IMDSv1 requires no auth token. Any SSRF (Server-Side Request Forgery) vulnerability in your app can leak IAM credentials from metadata service. **Always enforce IMDSv2.**

6. **Instance Store is gone on stop:** If you stop (not just reboot) an instance with instance store volumes, data is lost. Great for temp files/cache; catastrophic for anything you need to keep.

7. **EBS is AZ-locked:** An EBS volume can only attach to instances in the same AZ. To move data cross-AZ or cross-region, snapshot → copy → create new volume.

8. **Reserved Instance scope:** Regional RI applies to any AZ in the region. Zonal RI applies only to the specific AZ but includes capacity reservation.

---

## Hands-On Lab (Free Tier)

**Goal:** Launch EC2, configure with user data, test metadata, explore pricing.

```bash
# 1. Launch a t2.micro (free tier) via CLI
aws ec2 run-instances \
  --image-id ami-0abcdef1234567890 \  # replace with latest Amazon Linux 2023 AMI
  --instance-type t2.micro \
  --key-name MyKeyPair \
  --security-group-ids sg-xxxxxxxx \
  --subnet-id subnet-xxxxxxxx \
  --user-data '#!/bin/bash
yum update -y
yum install -y httpd
systemctl start httpd
echo "<h1>Hello AWS</h1>" > /var/www/html/index.html' \
  --metadata-options HttpTokens=required \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=MyTestInstance}]'

# 2. SSH into instance and test IMDSv2
ssh -i MyKeyPair.pem ec2-user@<public-ip>
TOKEN=$(curl -s -X PUT "http://169.254.169.254/latest/api/token" \
  -H "X-aws-ec2-metadata-token-ttl-seconds: 21600")
curl -H "X-aws-ec2-metadata-token: $TOKEN" \
  http://169.254.169.254/latest/meta-data/instance-type
curl -H "X-aws-ec2-metadata-token: $TOKEN" \
  http://169.254.169.254/latest/meta-data/instance-id

# 3. Stop and restart — observe new public IP assigned
aws ec2 stop-instances --instance-ids i-xxxxx
aws ec2 start-instances --instance-ids i-xxxxx
aws ec2 describe-instances --instance-ids i-xxxxx \
  --query 'Reservations[0].Instances[0].PublicIpAddress'

# 4. Check CPU credits (T-series)
# Console → EC2 → Instances → Select instance → Monitoring tab → CPUCreditBalance

# 5. Terminate when done (avoid EBS charges)
aws ec2 terminate-instances --instance-ids i-xxxxx
```

**Next:** Create an Auto Scaling Group with the same instance, a Launch Template, and a Target Tracking policy at 50% CPU.
