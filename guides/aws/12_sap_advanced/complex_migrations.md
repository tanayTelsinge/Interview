# SAP — Complex Large-Scale Migrations

## 1. The Problem

Migrating 200+ applications is an order-of-magnitude harder than migrating 10:

- **Dependencies are unknown:** Nobody has a complete dependency map. App A fails after migration because it had an undocumented database link to App B that wasn't migrated yet.
- **Parallel execution is required:** Sequential migration at 2 apps/week = 2 years. Need 10 parallel "lanes" running simultaneously.
- **Zero-downtime requirements clash with migration velocity:** Finance won't accept 4-hour maintenance windows. But fast migrations usually mean more downtime.
- **Mainframes and SAP exist:** These aren't just "lift EC2 and swap." They require specialized migration paths.
- **Post-migration optimization is often skipped:** Teams migrate, check "done," and leave right-sized On-Demand instances running the same workload for 2 years.

Enterprise migration at scale requires a **factory model** — repeatable, parallel, documented.

---

## 2. What AWS Built

| Service/Pattern | Purpose |
|-----------------|---------|
| **Migration Hub** | Central tracking dashboard across all migration tools |
| **Application Discovery Service (ADS)** | Automated inventory and dependency mapping |
| **Migration Hub Strategy Recommendations** | Analysis tool for .NET/Java app modernization paths |
| **MGN** | Continuous server replication for near-zero downtime cutover |
| **DMS + CDC** | Near-zero downtime database migration |
| **AWS Mainframe Modernization** | COBOL/PL1 replatform or refactor to Java |
| **AWS Launch Wizard for SAP** | Automated SAP HANA and S/4HANA deployment |
| **VMware Cloud on AWS** | Relocate VMware workloads without re-platforming |

---

## 3. How It Works

### Migration Factory Model

**The factory concept — parallel streams:**
```
Traditional migration: Serial
  App 1 (4 weeks) → App 2 (4 weeks) → App 3 ... → 200 apps = 800 weeks

Factory migration: Parallel lanes
  Lane 1: Wave 1 (20 apps, 4 weeks) → Wave 5 (20 apps)
  Lane 2: Wave 2 (20 apps, 4 weeks) → Wave 6 ...
  Lane 3: Wave 3 ...
  → 200 apps in 20 weeks (10× faster)

Each lane has: discovery analyst, migration engineer, testing engineer, app owner liaison
```

**Wave Planning Criteria:**
```
Dependency analysis → group apps that share dependencies in same wave
  - Database dependencies (App A reads from App B's DB → same wave)
  - Network dependencies (App A calls App B's API → same wave)
  - Independent apps → different waves (no ordering constraint)

Risk assessment → start with lower-risk apps:
  Wave 1: Dev/test environments (low risk, builds team experience)
  Wave 2: Non-customer-facing internal tools
  Wave 3: Less critical production apps
  Wave N: Mission-critical, customer-facing, complex dependencies

Archetype-based runbooks:
  Web app (Apache/Nginx + MySQL) → Runbook A
  Java microservice (Tomcat + Oracle) → Runbook B
  .NET Framework app (Windows + SQL Server) → Runbook C
  Each runbook: step-by-step validated procedure for that archetype
```

---

### Dependency Mapping

**Application Discovery Service:**
```
AGENTLESS (VMware vCenter connector):
  - Connect to vCenter API
  - Discovers: VMs, CPU/memory utilization, network connections
  - Does NOT see: process-level connections (which app ports are used)
  - Best for: large VMware environments, quick overview
  - Network connections: detected at VM level (not process level)

AGENT-BASED (install on each server):
  - Agent discovers: running processes, network connections PER PROCESS
  - Can see: "httpd on server-1 connects to mysql on server-3:3306"
  - Supports: Linux (2.6.32+), Windows Server (2003+)
  - Best for: detailed dependency mapping before migration

Both export to: AWS Migration Hub (central view)

Discovery outputs:
  - Server inventory CSV (CPU, RAM, disk, OS)
  - Network connection map (server A → server B)
  - Application grouping suggestions
```

**Migration Hub Strategy Recommendations:**
```
For .NET and Java applications specifically:
  - Install lightweight data collector on each server
  - Analyzes: binary files, config files, deployed artifacts
  - Outputs: recommended migration strategy per app

Strategies it suggests:
  Rehost:     Simple lift-and-shift
  Replatform: Move to managed service (.NET on Elastic Beanstalk, Java on ECS)
  Refactor:   Microservices decomposition
  Repackage:  Containerize without code changes (app → Docker → ECS/EKS)
  Retire:     No active usage detected
  Retain:     Complex dependencies detected, keep on-premises

Runtime analysis:
  Detects: which Java frameworks (Spring, Struts), .NET versions
  Identifies: components ready for containers vs needing OS-level dependencies
```

**VPC Flow Logs for Network Dependency:**
```
After Rehost to EC2: enable VPC Flow Logs
Analyze with Athena to discover which services talk to which:

SELECT srcaddr, dstaddr, dstport, protocol, SUM(bytes) as total_bytes
FROM vpc_flow_logs
WHERE start >= to_unixtime(now() - interval '7' day)
GROUP BY srcaddr, dstaddr, dstport, protocol
HAVING total_bytes > 1000000  -- only significant connections
ORDER BY total_bytes DESC;

Result: network dependency map you might not have had on-premises
Useful for: discovering unknown integrations, planning security group rules
```

---

### Zero-Downtime Migration Patterns

**Pattern 1: DMS CDC for Databases**
```
Phase 1: Schema migration (weekend maintenance window)
  - SCT converts schema
  - Apply to target Aurora
  - Validate schema: no errors, all objects created

Phase 2: Full Load (business hours, non-disruptive)
  - DMS Full Load: copy all existing data (hours to days)
  - Source DB continues serving production
  - DMS applies CDC changes as the load runs

Phase 3: CDC Sync (ongoing, pre-cutover)
  - Full Load completes
  - DMS now in CDC-only mode: lag < 10 seconds typically
  - Continue until cutover window

Phase 4: Cutover (minimal window)
  - App maintenance page: 5 minutes
  - Let DMS lag reach 0
  - Change app DB connection string
  - Validate: first transactions on new DB
  - Remove maintenance page
  Total downtime: 5-15 minutes
```

**Pattern 2: MGN for Servers with DNS Cutover**
```
Pre-migration (weeks before):
  1. TTL reduction: lower DNS TTL from 3600 → 60 seconds
     (Must do 24+ hours before cutover to ensure cache expiry)
  2. MGN replication: continuous, lag = seconds

Cutover day:
  3. Low-traffic window: 2 AM Sunday
  4. Stop application gracefully: drain connections, close DB connections
  5. Trigger MGN final sync: wait for lag = 0 (seconds)
  6. Launch MGN cutover instance
  7. Update Route53 record: point to new EC2 IP
  8. DNS propagates within 60 seconds (TTL was pre-lowered)
  9. Validate application

Downtime: 5-10 minutes (stop → sync → launch → DNS → validate)
```

**Pattern 3: Strangler Fig for Incremental Migration**
```
Monolithic on-premises app → gradual migration to cloud-native

Week 1: Route53 weighted routing
  90% traffic → On-premises (legacy)
  10% traffic → AWS (migrated user-service microservice)

Month 3: After validating stability
  50% → On-premises
  50% → AWS (more services migrated)

Month 6:
  0%  → On-premises
  100% → AWS (all services migrated)
  Decommission on-premises

DNS-based cutover: no code changes needed
  Route53 weighted routing: adjust weights gradually
  Health checks: auto-remove failed endpoint
  Canary: 5% to new → validate → increase

Each migration increment: small blast radius
```

**Pattern 4: Blue-Green Database Migration**
```
Blue: original Oracle DB (production)
Green: new Aurora PostgreSQL (migration target)

DMS CDC: Oracle (Blue) → Aurora (Green)
  CDC keeps Green in sync with Blue

Cutover:
  1. Stop writes to Blue
  2. Verify Green CDC lag = 0
  3. Promote Green to production
  4. Update app connection string
  5. Keep Blue read-only for N days (rollback option)

If Green has issues:
  Revert: change connection string back to Blue
  Blue still has all data (CDC is one-directional, not bidirectional)

Rollback window: however long you keep Blue running
```

---

### Mainframe Migration

**AWS Mainframe Modernization Service:**
```
Modernization paths:
  REPLATFORM:
    - COBOL/PL1 source code runs on managed runtime (AWS-managed)
    - Micro Focus Enterprise Server on AWS
    - No code rewrite — same COBOL, new infrastructure
    - Faster migration, lower risk, still COBOL
    - OS: z/OS, IBM i applications

  REFACTOR:
    - Automated conversion: COBOL → Java microservices
    - Tool: Blu Age (acquired by AWS)
    - Converts: COBOL programs → Java + Spring Boot
    - Converts: VSAM/JCL → modern DB + orchestration
    - Result: cloud-native Java application
    - Higher risk, longer timeline, best long-term outcome
```

**Micro Focus COBOL on AWS:**
```
  Micro Focus Enterprise Server:
    - Run COBOL applications unchanged on EC2 or containers
    - CICS/JES2 transaction processing emulation
    - DB2 → Amazon RDS (schema migration + DMS)

  Blu Age (COBOL refactoring):
    - Parse COBOL source → generate Java
    - VSAM → Amazon DynamoDB or RDS
    - JCL jobs → AWS Batch or Step Functions
    - COBOL programs → Spring Boot services on ECS
    - BMS maps → React.js frontend
    - Typical timeline: 12-24 months for large mainframe
```

**Data migration from mainframe:**
```
  VSAM → S3: AWS DataSync or custom extract programs
  IBM Db2 → Aurora/RDS: DMS + SCT
  IMS → DynamoDB: custom migration scripts (AWS provides samples)
  Sequential datasets → S3: mainframe FTP or DataSync
```

---

### SAP on AWS

**SAP HANA Instance Types:**
```
Memory-optimized instances for HANA in-memory DB:
  x1e.32xlarge: 3.9 TB RAM (large HANA production)
  x1.32xlarge:  2 TB RAM
  r6i.24xlarge: 768 GB RAM (medium HANA)

Storage:
  HANA data/log volumes → gp3 or io2 EBS
  HANA backup → S3 via AWS Backint Agent for SAP HANA

SAP Launch Wizard:
  Guided deployment of SAP HANA, S/4HANA, NetWeaver
  Pre-validates: instance type, storage layout, network config
  Creates: SAP-certified architecture automatically
```

**SAP HANA High Availability:**
```
Primary technique: HANA System Replication (HSR)
  Primary HANA → synchronous replication → Secondary HANA
  Secondary: standby, ready for failover in < 1 minute

AWS HA pattern:
  AZ-1: Primary EC2 (HANA Primary)
  AZ-2: Secondary EC2 (HANA Secondary)
  Cluster: Pacemaker + STONITH for automatic failover
  STONITH: fences failed node (prevents split-brain)
  Overlay IP: moved by Pacemaker to surviving node

  Route53 Health Check + Elastic IP → auto-DNS failover alternative

SAP Backint Agent:
  HANA backup directly to S3 (no intermediate storage needed)
  Better than: filesystem backup → copy to S3
  Supports: full, incremental, differential backups
  Schedule: HANA Studio or DBA Cockpit
```

---

### VMware Cloud on AWS (Relocate Strategy)

```
When to use: data center exit under VMware contract, no re-platforming budget

Architecture:
  AWS-managed vSphere cluster on dedicated bare metal AWS hosts
  vCenter: AWS-managed
  vMotion: live migrate VMs from on-prem vSphere → AWS vSphere (no downtime)

Pricing:
  Minimum: 2 hosts = i3.metal instances dedicated
  Cost: ~$35,000/month for 2-host cluster (dedicated hosts)
  Very expensive — only justified for:
    Short-term: pending renegotiation, waiting for refactor
    VMware contract: avoid break fees
    Compliance: VMware-specific certification requirements

Post-VMC: gradually Replatform/Refactor off VMware toward native AWS services
```

---

### Post-Migration Optimization

```
Day 1 post-migration: app works, team declares victory

30 days later: optimize

Compute Optimizer (after 14 days minimum CloudWatch data):
  Check: over-provisioned instances
  Action: right-size based on actual CPU/memory utilization
  Typical savings: 20-40% of compute costs

Trusted Advisor:
  Idle EC2 instances: low CPU < 10% for 14 days
  Underutilized EBS: low IO
  Unused Elastic IPs

Migration Hub Refactor Spaces:
  Incremental modernization from migrated monolith → microservices
  Add strangler-fig layer above existing app
  Route % of traffic to new microservice, leave rest to monolith
  Gradually expand without big-bang rewrite
```

---

## 4. Key Config & Limits

| Parameter | Value |
|-----------|-------|
| MGN cutover downtime | 15–30 minutes |
| MGN replication RPO | Seconds |
| DMS CDC typical lag | Seconds to low minutes |
| DNS TTL reduction timing | Change 24+ hours before cutover |
| Route53 weighted routing granularity | 0-255 per record |
| VMware Cloud on AWS minimum hosts | 2 |
| VMware Cloud on AWS cost | ~$35,000+/month |
| SAP HANA HSR failover time | < 1 minute (with Pacemaker) |
| HANA instance RAM max | 24 TB (u-24tb1.metal) |
| Blu Age conversion | Automated but requires manual validation |
| Compute Optimizer minimum data | 14 days |

---

## 5. Decision Trees

### Migration Order — Wave Planning
```
App characteristics → wave assignment:

No dependencies, dev/test? → Wave 1 (build confidence)
Dependencies well-mapped, internal users? → Wave 2-3
Complex dependencies, external users? → Wave 4-5
Mission-critical, real-time, customer-facing? → Last wave (most practice)
Mainframe/SAP → Separate parallel track (different skills)
```

### Zero-Downtime Strategy by App Type
```
Stateless web/app tier + RDS database?
└─ MGN (server) + DMS CDC (database)
   → 15-30 min downtime at cutover

Stateless web tier + DynamoDB?
└─ MGN + DynamoDB table copy → minimal downtime

Stateful database (heterogeneous)?
└─ SCT + DMS CDC → 5-15 min downtime (wait for CDC lag=0)

Monolith with deep integration?
└─ Strangler Fig (Route53 weighted) → weeks, gradual

VMware VMs, no code changes needed?
└─ VMware Cloud on AWS (vMotion → zero downtime) or MGN
```

---

## 6. Common Patterns

### Pattern 1: 12-Week Migration Factory Kickoff
```
Week 1-2: ASSESS
  - Deploy ADS agents on all servers
  - Run Migration Hub Strategy Recommendations
  - Generate: app inventory, dependency map, strategy per app
  - Output: application portfolio scored and categorized

Week 3-4: MOBILIZE
  - Set up: Landing Zone, IAM Identity Center, network connectivity (DX or VPN)
  - Define: runbooks per archetype
  - Pilot migration: 3 non-critical apps
  - Validate: MGN process, DMS process, DNS cutover, monitoring

Week 5-12: MIGRATE (waves)
  - Wave 1 (Week 5-6): 20 rehost apps (non-critical)
  - Wave 2 (Week 7-8): 20 replatform apps (MySQL→RDS, etc.)
  - Wave 3 (Week 9-10): 20 more apps
  - Wave 4 (Week 11-12): critical apps + first mainframe module
```

### Pattern 2: DNS Cutover with Zero Downtime Validation
```
T-72hrs: Lower DNS TTL for all app FQDNs
  Route53: change TTL from 3600 → 60 seconds

T-0 (cutover window: 2 AM):
  2:00 - App team: drain load balancer, stop new connections
  2:02 - MGN: trigger final sync for all servers in wave
  2:05 - DMS: verify CDC lag = 0 for all databases
  2:08 - Health check: new EC2 instances passing ALB health checks
  2:10 - Route53: update A records to new ALB/EC2 IPs
         (60-second TTL ensures propagation within 2 minutes)
  2:12 - Smoke tests: automated test suite confirms functionality
  2:15 - Success → cutover complete

Rollback (if issues at 2:12):
  2:12 - Revert Route53 to old IPs (< 60 second propagation)
  2:13 - Traffic back to original servers (still running)
  2:14 - Investigate → plan for next window
```

---

## 7. Gotchas

| Gotcha | Detail |
|--------|--------|
| **MGN cutover 15-30 min + app restart time** | MGN cutover launches the instance but app startup time is additional. Factor application warm-up (JVM startup, cache warming, connection pool initialization) into downtime estimate. |
| **DMS CDC requires supplemental logging BEFORE full load** | If you start DMS Full Load without enabling supplemental logging first, CDC will fail to capture changes that occurred during the load. Enable supplemental logging on the source DB before the first DMS task. |
| **Dependency mapping reveals more than expected** | Teams that say "we know all our dependencies" typically discover 20-30% more connections than documented. Always run ADS for 14+ days before finalizing wave plans. |
| **DNS TTL change must happen 24 hours before cutover** | Old TTL determines how long cached DNS records persist. If you change TTL at midnight for a 1 AM cutover, clients may still hit the old server for up to the OLD TTL duration after your DNS change. Change TTL 24+ hours in advance. |
| **VMware Cloud is expensive (~$35K/month minimum)** | Many organizations discover VMware Cloud on AWS mid-project as an "easy" option. At $35K+/month minimum, it's only financially viable for specific situations with clear exit strategy. |
| **Strangler Fig requires feature parity validation** | As you route traffic to the new service, ensure the new implementation handles all edge cases the old code handled. Subtle business logic differences cause intermittent failures. |
| **Mainframe testing takes longer than expected** | COBOL programs have decades of business logic. Automated conversion via Blu Age generates functional but not always semantically identical code. Plan 3-6 months of regression testing. |
| **SAP licensing requires SAP approval** | Moving SAP to AWS requires SAP to update licensing. Contact SAP BEFORE migration — some license models have cloud-specific requirements. AWS has an SAP on AWS team to help. |
| **Post-migration right-sizing needs 14 days** | Compute Optimizer needs 14 days of CloudWatch data. Don't right-size on Day 1 — you might hit workload peaks that weren't in the first day's data. |

---

## 8. Hands-On Lab — Free Tier

**Goal:** Simulate migration discovery and planning with Migration Hub and ADS.

### Step 1: Set Up Migration Hub
```
Migration Hub Console → Set home region (choose once, can't change)
  → us-east-1 recommended

Migration Hub → Get started → Import migration data
  (We'll use import instead of deploying real ADS agents)
```

### Step 2: Import Discovery Data (Simulate ADS)
```
Migration Hub → Discover → Import → Download template

Fill in the import template CSV:
ExternalId, ServerName, IPAddresses, MACAddresses, CPUType, TotalNumberofCores, TotalNumberofCPUs, TotalRAMinMB, TotalDiskSizeInGB, OperatingSystem

Example rows:
web-01,web-server-01,10.0.1.10,aa:bb:cc:dd:ee:ff,Intel Xeon,4,1,8192,200,Ubuntu 20.04
db-01,db-server-01,10.0.1.20,aa:bb:cc:dd:ee:01,Intel Xeon,8,2,65536,2000,RHEL 8
app-01,app-server-01,10.0.1.30,aa:bb:cc:dd:ee:02,Intel Xeon,4,1,16384,500,Windows Server 2019

Upload CSV → Migration Hub imports servers
```

### Step 3: Create Applications in Migration Hub
```
Migration Hub → Discover → Servers → select web-01, app-01, db-01

Actions → Create application
  Name: "sample-webapp"
  Servers: web-01, app-01, db-01

Migration Hub now tracks this app as a unit
```

### Step 4: Track Migration Progress
```
Migration Hub → Migrate → Applications → sample-webapp
  Status: Not Started

Update status (simulating migration progress):
  Actions → Update status → In Progress
  → Change to Completed when done

This is what MGN/DMS update automatically in real migrations
```

### Step 5: Explore Migration Hub Strategy Recommendations
```
Migration Hub → Assess → Strategy Recommendations
  (Note: requires actual servers with the collector installed for real analysis)

Review the strategy recommendation categories:
  Rehost, Replatform, Repackage, Refactor, Retire, Retain

This tool:
  - Analyzes .NET and Java app configurations
  - Checks for common anti-patterns (hardcoded IPs, tight OS dependencies)
  - Recommends migration strategy per app
```

### Step 6: Practice DNS TTL Planning
```bash
# Check current TTL for your domain:
dig +nocmd your-domain.com A +noall +answer

# If you have Route53:
# Go to Route53 → your hosted zone → record
# Change TTL: 3600 → 60 (do this 24 hours before any lab cutover)
# This simulates the TTL reduction step in real migrations
```

---

## Summary Reference Card

```
MIGRATION FACTORY:
  Parallel lanes: 5-10 teams running simultaneously
  Wave planning: by dependency + risk (start with low-risk)
  Runbooks per archetype: standardized procedures reduce errors

DEPENDENCY MAPPING:
  ADS Agentless: VMware vCenter, VM-level network connections
  ADS Agent-based: process-level connections, non-VMware
  Strategy Recommendations: .NET/Java analysis, per-app strategy suggestion
  VPC Flow Logs: post-migration dependency discovery

ZERO-DOWNTIME PATTERNS:
  DMS CDC: Full Load + CDC → cutover when lag=0 (5-15 min downtime)
  MGN: continuous replication → test cutover → final cutover (15-30 min)
  DNS cutover: pre-lower TTL 24hrs before, Route53 weighted routing
  Strangler Fig: gradual traffic shift via Route53 weighted routing

SPECIALIZED MIGRATIONS:
  Mainframe: AWS Mainframe Modernization → Replatform (Micro Focus) or Refactor (Blu Age)
  SAP: r/x EC2 families, Launch Wizard, HANA System Replication + Pacemaker
  VMware: VMware Cloud on AWS (vMotion, zero re-platform, expensive ~$35K/mo)

POST-MIGRATION:
  Compute Optimizer (14 days data needed): right-size EC2/EBS/Lambda
  Trusted Advisor: idle resources, underutilized
  Migration Hub Refactor Spaces: incremental monolith → microservices
```
