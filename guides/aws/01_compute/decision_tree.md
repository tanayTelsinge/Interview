# Compute Decision Tree — Full Reference

> Use this file when you know what you need to do but need to pick the right AWS compute service, pricing model, or configuration.

---

## Decision Tree 1: Which Compute Service?

```
What are you running?
│
├── A function / piece of code triggered by an event?
│   ├── Runs in < 15 minutes?
│   │   └── Lambda ✓
│   │       ├── Need to eliminate cold starts for latency-sensitive path?
│   │       │   └── Lambda + Provisioned Concurrency
│   │       ├── Need to run at the CDN edge (closest to user)?
│   │       │   ├── Complex logic, auth, A/B tests → Lambda@Edge
│   │       │   └── Simple rewrites, header manipulation → CloudFront Functions
│   │       └── Processing SQS/Kinesis/DDB streams? → Lambda with Event Source Mapping
│   └── Runs > 15 minutes → Lambda won't work, use containers or EC2
│
├── A containerized workload?
│   ├── Need full Kubernetes API (Helm, Istio, ArgoCD, multi-cluster)?
│   │   └── EKS
│   │       ├── Want AWS to manage EC2 nodes? → Managed Node Groups
│   │       └── Want serverless (no nodes)? → EKS Fargate Profiles
│   │
│   └── AWS-native orchestration is fine?
│       └── ECS
│           ├── Need GPU / specific instance type / Spot on EC2?
│           │   └── ECS on EC2 Launch Type
│           ├── Don't want to manage servers?
│           │   └── ECS Fargate
│           └── Simple web API, minimal config, scales to zero?
│               └── App Runner
│
├── A long-running, stateful, or OS-level workload?
│   └── EC2
│       ├── Variable load, scale in/out? → EC2 + Auto Scaling Group
│       ├── Need GPU? → P/G instance family
│       ├── Need extreme memory (SAP HANA)? → X instance family
│       └── Need bare metal? → EC2 bare metal (m5.metal etc.)
│
├── A batch / HPC job?
│   ├── Need managed job scheduling (queues, job dependencies)?
│   │   └── AWS Batch
│   │       ├── Uses EC2 on-demand + spot under the hood
│   │       └── Fargate compute environment also available
│   └── Need HPC interconnect (MPI, tightly-coupled)?
│       └── EC2 HPC instances + EFA + Placement Group (Cluster)
│
└── A multi-step workflow / orchestration?
    └── Step Functions + Lambda (for short steps) or ECS/Batch (for longer steps)
```

---

## Decision Tree 2: EC2 Instance Family

```
What is the PRIMARY resource bottleneck?
│
├── Balanced CPU + Memory (web server, app server, mid-tier DB)?
│   └── M family
│       ├── Intel? → m6i, m7i
│       ├── AMD? → m6a, m7a
│       └── ARM Graviton (best price/perf)? → m7g ✓ (recommended default)
│
├── CPU-bound (video encoding, rendering, web workers, HPC compute)?
│   └── C family
│       ├── Intel → c6i, c7i
│       ├── AMD → c6a, c7a
│       └── Graviton → c7g ✓
│
├── Memory-bound (in-memory DB, Redis, large caches, real-time analytics)?
│   ├── Up to ~768GB → R family (r6i, r7g)
│   └── Terabytes (SAP HANA, in-memory data warehouse) → X family (x2iedn)
│
├── Variable / bursty CPU (dev, test, low-traffic web)?
│   └── T family (t3, t4g)
│       ├── Consistent low CPU with occasional bursts
│       ├── T4g (Graviton) is ~20% cheaper than T3
│       └── WARNING: sustained high CPU exhausts credits → throttled to baseline
│           Fix: enable T Unlimited mode (small extra cost)
│
├── High I/O (local NVMe, Cassandra, MongoDB, Elasticsearch, HDFS datanodes)?
│   ├── NVMe SSD, I/O intensive → I family (i4i, i3en)
│   └── High throughput HDD (Hadoop, MapReduce, log processing) → D family (d3)
│
├── GPU workloads?
│   ├── ML Training (NVIDIA A100, H100) → P family (p4d, p5)
│   ├── ML Inference / Graphics (NVIDIA A10G) → G family (g5)
│   ├── Cost-optimized ML inference → Inf family (AWS Inferentia)
│   └── Cost-optimized ML training → Trn family (AWS Trainium)
│
└── HPC (MPI, tightly-coupled scientific computing)?
    └── HPC family (hpc7g) with EFA (Elastic Fabric Adapter)
```

---

## Decision Tree 3: EC2 Pricing Model

```
What is the workload pattern?
│
├── NEW or UNPREDICTABLE workload (can't commit yet)?
│   └── On-Demand
│       Tip: Run 1-2 weeks on On-Demand, use Cost Explorer to see patterns,
│       then buy Reserved/Savings Plans
│
├── STEADY-STATE production (24/7, runs for 1+ years)?
│   ├── Need flexibility on instance type/family?
│   │   └── Compute Savings Plans (66% discount, any family/size/OS/region)
│   └── Specific instance family in specific region?
│       ├── Want biggest discount + don't need flexibility?
│       │   └── EC2 Instance Savings Plans (72% discount)
│       └── Want to sell unused on Marketplace?
│           └── Standard Reserved Instance (72% discount, can sell/modify)
│
├── FAULT-TOLERANT, stateless, can be interrupted?
│   └── Spot Instances (90% discount)
│       ├── Single job → Spot Instance
│       ├── Need capacity across multiple types → Spot Fleet
│       └── Mix On-Demand base + Spot burst → EC2 Fleet
│
├── COMPLIANCE requiring physical server isolation?
│   ├── BYOL licensing (Oracle, Windows per-socket) → Dedicated Host
│   └── Just physical isolation, no placement control → Dedicated Instance
│
└── SHORT-TERM (days/weeks) but predictable?
    └── On-Demand (no commitment penalty)
```

**Discount comparison:**

| Model | Max Discount | Commitment | Flexibility |
|---|---|---|---|
| On-Demand | 0% | None | Any instance |
| Compute Savings Plans | ~66% | 1-3 yr $/hr | Any family/size/region |
| EC2 Instance Savings Plans | ~72% | 1-3 yr $/hr | Specific family + region |
| Standard RI | ~72% | 1-3 yr | Specific family/size/region/OS |
| Convertible RI | ~54% | 1-3 yr | Can change family during term |
| Spot | ~90% | None | Interruptible |

---

## Decision Tree 4: Placement Group

```
What is the priority for your instances?
│
├── Lowest possible latency between instances (HPC, ML training cluster)?
│   └── Cluster Placement Group
│       ├── Same AZ, same physical rack (or nearby)
│       ├── Up to 100Gbps bandwidth between instances
│       └── Risk: single point of failure if rack/AZ fails
│           Acceptable for HPC where you restart the job anyway
│
├── Maximum fault isolation for individual instances?
│   └── Spread Placement Group
│       ├── Each instance on completely different physical hardware
│       ├── Limit: 7 instances per AZ per group
│       └── Use for: primary + standby DB nodes, NAT instances, critical single instances
│
├── Large distributed system needing rack-awareness (Hadoop, Kafka, Cassandra)?
│   └── Partition Placement Group
│       ├── Up to 7 partitions per AZ
│       ├── Each partition = separate rack of servers
│       ├── Hundreds of instances per partition
│       └── Instances can query which partition they're in
│           (apps use this for Kafka rack assignment, HDFS rack topology)
│
└── No specific requirement (standard web tier, ASG)?
    └── No placement group needed
        ASG spreads across AZs automatically — that's sufficient HA
```

---

## Decision Tree 5: Auto Scaling Policy Type

```
How does load arrive?
│
├── Can define a target metric to maintain (CPU%, request rate)?
│   └── Target Tracking Scaling (RECOMMENDED default)
│       Example: "Keep average CPU at 50%"
│       - Simplest to configure
│       - AWS manages scale-in/out automatically
│       - Works well for most web applications
│
├── Need different scale amounts at different alarm thresholds?
│   └── Step Scaling
│       Example: "If CPU > 60% add 2 instances; if CPU > 80% add 5 instances"
│       - More control over scale amount
│       - Requires CloudWatch alarm setup
│       - Use when scaling response should be proportional to severity
│
├── Load is PREDICTABLE by time (known patterns)?
│   └── Scheduled Scaling
│       Example: "Add 10 instances every Monday 8am, remove at 8pm"
│       - Set specific capacity at specific times
│       - Use FOR KNOWN PATTERNS (office hours, batch windows)
│       - Combine with Target Tracking for unexpected spikes
│
└── Load patterns are complex but can be learned from history?
    └── Predictive Scaling (ML-based)
        - AWS analyzes 2 weeks of load history
        - Proactively scales BEFORE load arrives
        - Combine with Target Tracking as a safety net
```

---

## Quick Reference Table

| Need | Service | Key Reason |
|---|---|---|
| Function < 15 min, event-driven | Lambda | Serverless, scales to 0 |
| Container, no infra management | ECS Fargate | Serverless containers |
| Container, Kubernetes ecosystem | EKS | Full K8s API |
| Container, self-managed cluster | ECS on EC2 | GPU/Spot/custom instance |
| Simple web container, scale to 0 | App Runner | Easiest setup |
| Long-running server, stateful | EC2 + ASG | Full OS control |
| Batch jobs, job queues | AWS Batch | Managed job scheduling |
| HPC, MPI, tightly-coupled | EC2 + EFA + Cluster PG | Sub-ms networking |
| ML training (large GPU) | P4/P5 instances | NVIDIA A100/H100 |
| Cost-optimized ML inference | Inf2 instances | AWS Inferentia chip |
| Code at the CDN edge | Lambda@Edge / CF Functions | Lowest latency |
| Multi-step workflow | Step Functions | State management + retry |
