# Migration Strategies — The 7 Rs

## 1. The Problem

Enterprises running hundreds of on-premises applications face a fundamental question: which applications to move to the cloud, and how? A bank with 400 apps can't treat all of them the same:

- Some apps are scheduled for decommission — migrating them wastes money
- Some are mission-critical mainframe systems — moving them incorrectly kills the business
- Some are shrink-wrapped vendor software — there's a SaaS alternative that eliminates infrastructure ops
- Some need cloud-native capabilities (serverless, managed DB) — but refactoring everything at once is impossible

Without a structured framework, migration becomes a chaotic, expensive lift-and-shift project that recreates the same on-premises problems in the cloud. The 7 Rs provide a decision language.

---

## 2. What AWS Built

The **7 Rs Migration Framework** — a taxonomy of migration strategies from least to most cloud-native. AWS pairs this with the **Cloud Adoption Framework (CAF)** and a structured **three-phase migration process**.

### AWS Cloud Adoption Framework (CAF)
Not a technical tool — a change management framework across six perspectives:

| Perspective | Focus |
|-------------|-------|
| **Business** | Business value, ROI, business case |
| **People** | Org change, training, culture shift |
| **Governance** | Portfolio management, program management |
| **Platform** | Target architecture, provisioning, modern data |
| **Security** | Identity, detection, infrastructure protection |
| **Operations** | Event management, patch, backup, monitoring |

---

## 3. How It Works — The 7 Rs

### 1. Retire
**Decommission and turn off.**

- Typical scope: **10–20% of portfolio**
- Applications with no active users, sunset roadmap, or replaced by other apps
- Process: usage audit (access logs, interviews), dependency check, data archival, shutdown
- Direct cost savings from Day 1 — every retired app is one less license, server, or maintenance burden

### 2. Retain
**Keep on-premises (do nothing now).**

- Mainframe applications that are too risky to move
- Recently upgraded on-premises systems (capital just spent)
- Compliance requirements mandating on-premises (some government regulations)
- Apps dependent on low-latency physical hardware
- "Retain" is not permanent — revisit each cycle
- Typical scope: 20–30% of portfolio

### 3. Rehost (Lift & Shift)
**Move as-is to EC2 with no application changes.**

- Tool: **AWS Application Migration Service (MGN)**
- Fastest migration strategy with minimal risk
- No optimization — same instance types, same OS, same app configuration
- Post-migration optimization can happen separately (right-sizing, then Replatform)
- Business value: data center exit, hardware refresh avoidance, OPEX to CAPEX shift
- **Does not realize cloud-native benefits** — auto-scaling, managed services, etc.

### 4. Relocate
**Move the platform to AWS without changing the application.**

- Distinct from Rehost: you're moving an entire **platform**, not individual VMs
- Tools:
  - **VMware Cloud on AWS**: vSphere VMs migrate via vMotion — zero re-platforming of VMware workloads
  - **EKS Anywhere**: migrate on-premises Kubernetes clusters to EKS
- Used when: VMware contract renewal approaching, data center lease expiring, but app changes not funded
- Application sees no difference — same VMware APIs, same VM management

### 5. Repurchase (Drop & Shop)
**Move to SaaS — replace the application entirely.**

- Examples:
  | Old On-Prem | SaaS Replacement |
  |-------------|-----------------|
  | Custom CRM | Salesforce |
  | On-prem HR system | Workday |
  | Email server | Microsoft 365 / Google Workspace |
  | On-prem ERP | SAP S/4HANA Cloud |
  | On-prem LMS | Cornerstone, Docebo |

- Net infrastructure goes to zero — no servers, no patching, no upgrades
- **Migration effort**: data migration + user training + integration work
- Not appropriate for competitive differentiator applications
- Upfront license/subscription cost may be higher than on-prem

### 6. Replatform (Lift, Tinker & Shift)
**Make targeted cloud optimizations without changing core architecture.**

- The "low-hanging fruit" modernization layer
- Examples:
  | Before | After | Benefit |
  |--------|-------|---------|
  | MySQL on EC2 | Amazon RDS MySQL | Automated backups, HA, patching |
  | App deployed to Linux VMs | AWS Elastic Beanstalk | Auto-scaling, managed platform |
  | Self-managed Kafka | Amazon MSK | Managed brokers, no ZooKeeper ops |
  | Self-managed Redis | ElastiCache | Automated failover, managed upgrades |

- Code may be unchanged — just deployment target changes
- **Moderate effort, good optimization return**
- Risk: some behavior differences between self-managed and managed services (connection pooling, parameter defaults)

### 7. Refactor / Re-architect
**Redesign the application to be cloud-native.**

- Highest effort, highest long-term benefit
- Examples:
  | Before | After |
  |--------|-------|
  | Monolithic Java app | Microservices on ECS/EKS |
  | VM-based batch jobs | AWS Lambda event-driven |
  | Oracle RAC | Aurora Serverless |
  | On-prem data warehouse | Redshift + S3 data lake |
  | Scheduled jobs | Step Functions + EventBridge |

- Requires cloud engineering skills, time, testing
- Justified when: application is a business differentiator, current architecture limits feature velocity, scaling requirements can't be met by Replatform
- Risk: highest — new bugs, new operational model, rework

---

## 4. Key Config & Limits

| Item | Value |
|------|-------|
| Typical Retire % of portfolio | 10–20% |
| Typical Retain % of portfolio | 20–30% |
| MGN replication lag (near-zero RPO) | Seconds |
| MGN cutover downtime | 15–30 minutes |
| Migration Hub — supported discovery connectors | VMware vCenter agentless, AWS agent-based |
| Migration Evaluator report turnaround | 1–2 weeks |
| Application Discovery Service agent OS support | Windows Server 2003+, Linux 2.6.32+ |

---

## 5. Decision Tree

```
START: Application Assessment
│
├─ No active users or scheduled retirement?
│   └─ RETIRE
│
├─ Mainframe / recently upgraded / compliance keeps on-prem?
│   └─ RETAIN
│
├─ SaaS alternative exists and team accepts vendor solution?
│   └─ REPURCHASE (drop & shop)
│
├─ Running on VMware and need zero app changes?
│   └─ RELOCATE (VMware Cloud on AWS / EKS Anywhere)
│
├─ Need fast migration with minimal risk, optimize later?
│   └─ REHOST (MGN lift & shift)
│
├─ Can move to managed service with minimal code change?
│   └─ REPLATFORM (MySQL→RDS, self-managed→Beanstalk/MSK)
│
└─ Business differentiator needing full cloud-native benefits?
    └─ REFACTOR / RE-ARCHITECT
```

**App Characteristics → Strategy:**
```
Low complexity + commodity function → Repurchase or Retire
High migration risk + core business → Rehost first, Refactor later
Containerized already → Relocate (EKS Anywhere) or Replatform
Database-heavy + managed DB fits → Replatform
Microservices opportunity + team skills → Refactor
```

---

## 6. Common Patterns

### Pattern 1: Wave-Based Migration
```
Wave 1: Retire + Repurchase
  → Eliminate 15% of apps, swap CRM to Salesforce
  → Immediate cost reduction, builds team confidence

Wave 2: Rehost non-critical apps
  → Dev/test environments, internal tools
  → MGN, quick wins, data center space freed

Wave 3: Replatform databases and middleware
  → MySQL→RDS, Kafka→MSK, Redis→ElastiCache
  → Operations simplification

Wave 4: Refactor business-critical apps
  → Monolith decomposition, serverless where appropriate
  → Requires skills investment, phased over 12-24 months
```

### Pattern 2: Migration Phases
```
ASSESS (1-3 months):
  Tools: Migration Evaluator (TCO analysis), Application Discovery Service
  Output: Business case, app inventory, dependency map, wave plan

MOBILIZE (2-4 months):
  Tools: Migration Hub, Landing Zone setup, pilot migration
  Output: Operating model, runbooks, tested toolchain, team training

MIGRATE & MODERNIZE (ongoing):
  Tools: MGN, DMS, SCT, Beanstalk, EKS
  Output: Apps running in AWS, optimization in progress
```

### Pattern 3: TCO Analysis
```
On-premises hidden costs to include:
  - Hardware: servers, networking, storage (amortized over 3-5 years)
  - Facilities: power, cooling, rack space, physical security
  - Labor: sysadmin, DBA, network engineer time
  - Software: OS licenses, management tools
  - Downtime: unplanned outages cost per hour

Migration Evaluator: discovers on-prem inventory, models AWS equivalent,
generates TCO comparison PDF (typically shows 20-40% savings over 3 years)
```

### Pattern 4: Application Discovery Service
```
AGENTLESS (VMware vCenter connector):
  - Discovers: VM metadata, CPU/memory utilization, network connections
  - No agent install on VMs
  - Requires vCenter access
  - Best for: large VMware environments, quick assessment

AGENT-BASED (install on each server):
  - Discovers: process-level detail, network connections (which processes talk to which)
  - Works on: bare metal, non-VMware, Linux, Windows
  - Best for: dependency mapping for complex apps

Both export to Migration Hub for centralized tracking
```

---

## 7. Gotchas

| Gotcha | Detail |
|--------|--------|
| **Rehost is fastest but forfeits optimization** | Lift-and-shift to EC2 On-Demand is often MORE expensive than on-premises if you don't right-size and apply RIs/SPs afterward. |
| **Refactor needs cloud-native skills first** | Teams that have never deployed EKS or Lambda will fail a refactor. Invest in training before wave planning. |
| **Don't skip the Assessment phase** | "We know our apps" is always wrong. Dependency mapping reveals undocumented connections that kill migration timelines. |
| **Dependency mapping is critical** | App A may depend on App B via undocumented DB link. Migrating A without B breaks it. |
| **Retain is not "never migrate"** | Retain = not now. Reassess each planning cycle. Mainframe apps often have a viable re-architect path after skills are built. |
| **Repurchase has hidden costs** | Data migration, SSO integration, custom workflow recreation, and retraining can cost more than the old system. |
| **VMware Cloud on AWS is expensive** | Minimum deployment is 2 hosts (~$35K/month). Only justified for specific use cases (data center exit under VMware contract). |
| **MGN != zero downtime** | MGN minimizes downtime but cutover still requires 15-30 minutes. For zero-downtime, layer with DMS CDC or DNS-based strategies. |

---

## 8. Hands-On Lab — Free Tier

**Goal:** Practice migration assessment tools and Migration Hub.

### Step 1: Enable Migration Hub
```
AWS Console → Migration Hub → Enable Migration Hub
  Select home region (can't change later)
  → us-east-1 or us-west-2 recommended

Migration Hub tracks: discovery data, migration tasks, progress
```

### Step 2: Use Migration Evaluator (no cost)
```
Migration Hub → Migration Evaluator → Request Assessment
  → For a real assessment: provide on-prem inventory spreadsheet
  → For lab: use the "quick insights" self-service option
  → Generates: recommended EC2 instances, estimated costs
```

### Step 3: Application Discovery Service — Agentless Import
```
# If you have vCenter access (or simulate with a spreadsheet):
Migration Hub → Discover → Import Data
  Download the import template CSV
  Fill in: server name, CPU cores, RAM GB, disk GB, OS

# This simulates what ADS agentless discovers automatically
# After import, view in: Discover → Servers
```

### Step 4: Create a Migration in Migration Hub
```
Migration Hub → Migrate → Create Application
  Name: "lab-app-01"
  Add servers from discovery

Track status:
  Not Started → In Progress → Completed

# In a real migration, MGN/DMS update this status automatically
```

### Step 5: Map the 7 Rs to a Sample Inventory
```
Create a spreadsheet with 10 fictitious apps:
  App Name | Users | Complexity | Cloud Equivalent | Recommended R

Example:
  legacy-crm     | 50 users | Medium | Salesforce exists    → Repurchase
  mysql-db-01    | N/A      | Low    | RDS drop-in          → Replatform
  mainframe-billing | N/A  | Very High | No equivalent      → Retain
  dev-test-server  | 5 devs | Low  | EC2 lift-and-shift   → Rehost
  custom-erp     | 200 users | High | Refactor to ECS     → Refactor
  legacy-reports | 2 users  | Low   | No one uses it       → Retire
```

---

## Summary Reference Card

```
THE 7 Rs (speed vs cloud benefit tradeoff):

RETIRE    → 10-20% of apps. Just turn them off.
RETAIN    → Mainframe, compliance, recently upgraded. Revisit later.
REHOST    → MGN lift-and-shift. Fast, no optimization, migrate fast.
RELOCATE  → VMware Cloud on AWS / EKS Anywhere. Platform move.
REPURCHASE→ Replace with SaaS. Zero infrastructure.
REPLATFORM→ MySQL→RDS, app→Beanstalk. Managed services, minimal code change.
REFACTOR  → Monolith→microservices. Cloud-native. Most benefit, most effort.

PHASES: Assess (TCO) → Mobilize (pilot) → Migrate & Modernize (waves)
TOOLS:  Migration Evaluator (TCO) | ADS (discovery) | Migration Hub (tracking)
```
