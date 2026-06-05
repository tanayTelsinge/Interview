# Disaster Recovery Strategies — SAP-C02 Deep Dive

---

## 1. The Problem

Failures are not a matter of "if" — they are a matter of "when" and "how bad". Data centers flood, regions experience outages, software bugs corrupt databases, and operators accidentally delete production data. The business question is not "will we fail?" but "how quickly can we recover, and how much data can we afford to lose?"

**Business impact of downtime:**
- E-commerce: ~$200,000/hour (average large retailer)
- Financial services: ~$5,000,000/hour
- Healthcare: patient safety implications beyond cost
- SaaS: contractual SLA violations (SLA credits + churn)

Two numbers define your recovery requirements: RTO and RPO.

---

## 2. RTO and RPO Defined

### Recovery Time Objective (RTO)

**The maximum acceptable time to restore service after a disaster.**

```
Disaster Occurs          Service Restored
     |                         |
     |<───────── RTO ─────────>|
     |                         |
  T=0:00                    T=4:00 (RTO = 4 hours)
```

RTO examples:
- "We can tolerate up to 4 hours of downtime before customers churn"
- "Regulatory requirement: system must be available within 1 hour"
- "Our SLA guarantees 99.9% uptime (8.7 hrs/year max downtime)"

### Recovery Point Objective (RPO)

**The maximum acceptable data loss measured in time.**

```
Last Backup      Disaster Occurs
     |                |
     |<─── RPO ──────>|
     |                |
  T=6:00 AM       T=8:00 AM  (RPO = 2 hours of data lost)
```

RPO examples:
- "We can lose up to 1 hour of orders (reprocess manually)"
- "Zero data loss tolerance (RPO=0) for financial transactions"
- "24-hour RPO acceptable for audit logs"

### RTO vs RPO vs Strategy Selection

```
         RPO near zero?
         /           \
        YES            NO
        |               |
   Aurora Global/    S3 CRR/
   DynamoDB Global   Snapshots
        |               |
   RTO near zero?   RTO hours?
        |               |
       YES              YES
        |               |
  Multi-Site       Backup &
  Active/Active     Restore
        |
       NO (RTO minutes)
        |
   Pilot Light /
   Warm Standby
```

---

## 3. The Four DR Strategies

### Strategy Comparison Matrix

| Strategy | RTO | RPO | Cost | Use Case |
|---|---|---|---|---|
| **Backup & Restore** | Hours | Hours | $ | Non-critical systems, archives |
| **Pilot Light** | 10s of minutes | Minutes | $$ | Core services, tolerate some downtime |
| **Warm Standby** | Minutes | Seconds–Minutes | $$$ | Important workloads, limited budget for active/active |
| **Multi-Site Active/Active** | Near zero (~seconds) | Near zero | $$$$ | Business-critical, zero tolerance |

---

## 4. Strategy 1: Backup & Restore

### How It Works

```
Primary Region (us-east-1)               DR Region (us-west-2)
     |                                          |
  Production EC2/RDS               S3 bucket (backups/AMIs/snapshots)
     |                                          |
     |─── Daily snapshots ───────────────────→  |
     |─── S3 CRR (S3 data) ──────────────────→  |
     |─── AMIs copied ────────────────────────→  |
                                                |
                                   [Disaster strikes primary]
                                                |
                              Launch from AMIs + restore snapshots
                              RTO: 1-4 hours (ami launch + data restore)
```

### Key Tools

- **RDS automated backups:** retained 1–35 days, copied cross-region manually or via AWS Backup
- **EBS snapshots:** incremental, copied to DR region via CLI or AWS Backup
- **AMIs:** copy to DR region with `aws ec2 copy-image`
- **S3 Cross-Region Replication (CRR):** replicate S3 objects to DR bucket
- **AWS Backup:** centralized backup service for RDS, EBS, EFS, DynamoDB, EC2

### AWS Backup Vault Lock

Immutable backup vault — prevent deletion of backups even by root account (WORM compliance).

### Cost Profile

- Pay for S3 storage of backups
- No running compute costs in DR region
- No data transfer costs for kept backups
- Restoration time = compute launch + data restore = **hours**

---

## 5. Strategy 2: Pilot Light

### How It Works

```
Primary Region                           DR Region (Pilot Light)
     |                                          |
  [Full stack running]            [Core DB running, rest stopped]
  App tier (running)                DB replica (RUNNING — must be live)
  DB tier (running)                App tier (stopped/no instances)
  Load balancer (active)           Load balancer (configured, inactive)
  Route53 (points here)            Route53 health check (failover)
     |
     |── RDS cross-region replica ──────────────→ RDS replica (running)
     |── DynamoDB Global Tables ────────────────→ DynamoDB (synchronized)
                                                          |
                                         [Disaster strikes primary]
                                                          |
                              1. Promote RDS replica to standalone
                              2. Launch app tier from pre-baked AMIs
                              3. Update Route53 to point to DR region
                              4. Scale ASG to minimum viable capacity
                              RTO: 10-30 minutes
```

### Activation Runbook (Pilot Light)

```
Step 1: Promote RDS read replica to primary (1-5 min)
  aws rds promote-read-replica --db-instance-identifier dr-replica

Step 2: Launch app tier from AMIs (5-10 min)
  aws autoscaling update-auto-scaling-group \
    --auto-scaling-group-name dr-app-asg \
    --min-size 2 --max-size 10 --desired-capacity 2

Step 3: Update DNS (1-2 min TTL propagation)
  aws route53 change-resource-record-sets ... (or auto via health check)

Step 4: Verify application health (2-5 min)
  curl https://dr.example.com/health

Total RTO: ~15-25 minutes
```

### Cost Profile

- Running cost: RDS replica + storage
- No app compute in DR region (only during incident)
- Activation cost: EC2 instances only during DR event

---

## 6. Strategy 3: Warm Standby

### How It Works

```
Primary Region                      DR Region (Warm Standby)
     |                                      |
  Full stack (full capacity)      Scaled-down copy (running, ~10-25% capacity)
  App: 10 instances                App: 2 instances (running but small)
  DB: Multi-AZ primary             DB: Cross-region replica (running)
  ALB: Active, serving traffic     ALB: Active, serving health checks
  Route53: Health check → here     Route53: Health check → failover ready
     |
     |── DB replication ───────────────────→ DB replica (in sync, seconds lag)
                                                      |
                                     [Primary unhealthy]
                                          Route53 health check fails
                                                      |
                              1. Route53 fails over DNS to DR ALB
                              2. Promote DB replica (if needed)
                              3. ASG scales up to full capacity
                              4. (automation handles 2+3)
                              RTO: 2-10 minutes
```

### Key Automation

```yaml
# CloudWatch alarm triggers ASG scale-up on failover event
# Lifecycle hook to warm up instances before traffic

# EventBridge rule: Route53 health check fails → Lambda
# Lambda:
#   1. Promote RDS replica
#   2. Set ASG desired capacity = production values
#   3. Send SNS notification

# DNS TTL should be 60 seconds (not default 300)
# Lower TTL = faster failover, higher Route53 API cost
```

---

## 7. Strategy 4: Multi-Site Active/Active

### How It Works

```
us-east-1                              us-west-2
     |                                      |
Full capacity production          Full capacity production
10 EC2 instances                  10 EC2 instances
Aurora primary                    Aurora global cluster secondary
DynamoDB Global Table             DynamoDB Global Table
     |                                      |
     |── Route53 Latency routing ───────────|
     |   (US East users → east)             |
     |   (US West users → west)             |
     |                                      |
     |── Aurora Global: 1 sec replication ──|
     |── DynamoDB: sub-second replication ──|
                                            |
                            [Region outage — us-east-1]
                                            |
                            Route53 health check detects failure
                            All traffic routes to us-west-2 (< 60 seconds)
                            Aurora secondary promoted to primary (< 1 min)
                            RTO: near zero (seconds to 1 minute)
                            RPO: near zero (< 1 second of data loss possible)
```

### Data Replication Options

| Service | Replication Type | RPO |
|---|---|---|
| **Aurora Global Database** | Async (< 1 second lag) | < 1 second |
| **DynamoDB Global Tables** | Multi-master active/active | < 1 second |
| **S3 Cross-Region Replication** | Async (seconds to minutes) | Seconds–minutes |
| **RDS Cross-Region Read Replica** | Async (seconds to minutes) | Seconds–minutes |
| **ElastiCache Global Datastore** | Async | Seconds |

### Aurora Global Database Failover

```
Normal operation:
  Primary: us-east-1 (read/write)
  Secondary: us-west-2 (read-only) ← 1 secondary write endpoint per region

Planned failover (managed switchover):
  aws rds switchover-global-cluster \
    --global-cluster-identifier my-global-cluster \
    --target-db-cluster-identifier arn:aws:rds:us-west-2:...

Unplanned failover (disaster):
  aws rds failover-global-cluster \
    --global-cluster-identifier my-global-cluster \
    --target-db-cluster-identifier arn:aws:rds:us-west-2:...

  ⚠ Old primary's cluster is DETACHED (not demoted — it becomes standalone read-only)
  ⚠ Must manually re-add old primary as secondary after it recovers
```

---

## 8. Route53 Health Checks + DNS Failover

### Health Check Types

| Type | What It Monitors |
|---|---|
| **Endpoint health check** | HTTP/HTTPS/TCP to IP/domain |
| **Calculated health check** | Combines multiple health checks (AND/OR) |
| **CloudWatch alarm health check** | Pass/fail based on CloudWatch alarm state |

### Failover Routing

```
Route53 Hosted Zone: example.com
  ├── A record (PRIMARY): ALB in us-east-1
  │   └── Health check: /health endpoint
  └── A record (SECONDARY): ALB in us-west-2
      └── Health check: /health endpoint
      └── (only serves traffic if PRIMARY health check fails)
```

### DNS TTL Considerations

```
TTL = 300 seconds (5 minutes, default):
  User cached DNS for 5 min after primary fails
  Failover perceived time = DNS detection + TTL = 30s + 300s = 5.5 minutes

TTL = 60 seconds:
  Failover perceived time = 30s + 60s = 90 seconds
  Cost: 5× more Route53 queries

Recommendation: Set TTL to 60s BEFORE a disaster is likely
  (cannot change TTL faster than current TTL expires)
```

---

## 9. AWS Services per DR Strategy

| AWS Service | Backup & Restore | Pilot Light | Warm Standby | Active/Active |
|---|---|---|---|---|
| **EC2** | AMIs in DR region | AMIs ready, not launched | Scaled-down ASG running | Full ASG both regions |
| **RDS** | Snapshots in DR region | Read replica (running) | Read replica (running) | Aurora Global |
| **DynamoDB** | PITR + backups | Global Tables | Global Tables | Global Tables |
| **S3** | CRR | CRR | CRR | CRR |
| **Route53** | Manual DNS change | Failover routing | Failover routing | Latency/weighted routing |
| **ELB** | Create on recovery | Pre-configured in DR | Running in DR | Active in both regions |
| **EFS** | EFS backup to S3 | EFS replication | EFS replication | EFS per-region |
| **ElastiCache** | Not needed | Not needed | Scaled-down in DR | Global Datastore |

---

## 10. AWS Fault Injection Simulator (FIS)

### What It Does

Managed **chaos engineering** service. Run controlled fault injection experiments to validate DR runbooks and measure actual RTO/RPO.

### Experiment Types

| Action | Effect |
|---|---|
| `aws:ec2:terminate-instances` | Terminate EC2 instances |
| `aws:rds:failover-db-cluster` | Trigger RDS Multi-AZ failover |
| `aws:ecs:stop-task` | Stop ECS tasks |
| `aws:eks:terminate-nodegroup-instances` | Remove EKS nodes |
| `aws:fis:inject-api-internal-error` | Simulate AWS API errors |
| `aws:fis:inject-api-throttle-error` | Simulate API throttling |
| `aws:network:disrupt-connectivity` | Block network traffic |

### FIS Experiment Template

```json
{
  "description": "Test RDS failover recovery time",
  "targets": {
    "MyRDSCluster": {
      "resourceType": "aws:rds:cluster",
      "resourceArns": ["arn:aws:rds:..."],
      "selectionMode": "ALL"
    }
  },
  "actions": {
    "FailoverRDS": {
      "actionId": "aws:rds:failover-db-cluster",
      "targets": {"Clusters": "MyRDSCluster"}
    }
  },
  "stopConditions": [{
    "source": "aws:cloudwatch:alarm",
    "value": "arn:aws:cloudwatch:...:alarm:DR-Abort-Alarm"
  }],
  "roleArn": "arn:aws:iam::...:role/FISRole"
}
```

---

## 11. DR Testing Runbooks

### Monthly DR Test Checklist

```
Pre-test:
  [ ] Notify stakeholders (scheduled maintenance window)
  [ ] Set Route53 TTLs to 60 seconds 24 hours before test
  [ ] Take manual RDS snapshot before test
  [ ] Verify monitoring dashboards working

Test execution:
  [ ] Simulate disaster (FIS or manual)
  [ ] Start timer (RTO clock)
  [ ] Execute runbook steps
  [ ] Measure actual time for each step
  [ ] Verify application health in DR region

Post-test:
  [ ] Document actual RTO vs target
  [ ] Document any data loss (RPO gap)
  [ ] Failback to primary
  [ ] Update runbooks with lessons learned
  [ ] File action items for gaps
```

---

## 12. Key Config & Limits

| Parameter | Value |
|---|---|
| RDS automated backup retention | 1–35 days |
| RDS cross-region snapshot copy | Manual or AWS Backup |
| Aurora Global DB replication lag | Typically **< 1 second** |
| DynamoDB Global Tables replication lag | Typically **< 1 second** |
| Route53 health check interval | 10s or 30s |
| Route53 failover DNS propagation | With low TTL: **< 60 seconds** |
| S3 CRR replication time | Typically minutes; S3 RTC guarantees **15 min** for 99.99% |
| RDS replica promotion | **2–5 minutes** (estimate) |
| EC2 instance launch | **2–4 minutes** (from AMI) |
| Aurora Global failover | Typically **< 1 minute** for managed failover |

---

## 13. Decision Tree — Which DR Strategy?

```
What is your RTO requirement?

├── < 1 minute (near-zero)?
│   └── Multi-Site Active/Active (Route53 latency, Aurora Global, DynamoDB Global)

├── 1-15 minutes?
│   ├── Warm Standby (scaled-down stack, auto-failover with Route53)
│   └── or Pilot Light with good automation

├── 15-60 minutes?
│   └── Pilot Light (core DB running, rest activated on demand)

└── Hours acceptable?
    └── Backup & Restore

Now cross-check with RPO:

├── RPO = 0 (no data loss)?
│   └── Must use Aurora Global / DynamoDB Global Tables + Active/Active

├── RPO < 5 minutes?
│   └── Warm Standby with cross-region DB replica

├── RPO hours?
│   └── Backup & Restore with S3 CRR / RDS snapshots

Now consider cost:

├── Budget available for full second region?
│   └── Active/Active or Warm Standby

├── Limited DR budget?
│   └── Pilot Light (only DB running in DR region)

└── Minimal cost?
    └── Backup & Restore
```

---

## 14. Gotchas

### Exam Traps

| Gotcha | Detail |
|---|---|
| **DNS TTL must be low for fast failover** | High TTL = clients cache old DNS → long failover. Set 60s before expected incidents |
| **Aurora Global failover removes old primary** | Old primary is DETACHED and becomes standalone. Must manually re-add as secondary after recovery |
| **RTO includes DNS propagation** | Even if your system is up in DR, clients using old DNS still see the failure |
| **Warm standby needs scale-up automation** | Pre-configure ASG with desired capacity = 0 in DR; automation sets it on failover |
| **RDS Read Replica promotion = new standalone** | After promotion, it's a fresh primary. Old primary's data stops syncing. No automatic re-sync |
| **S3 CRR doesn't replicate existing objects** | CRR only replicates NEW objects after enabling. Use S3 Batch Replication for existing |
| **DynamoDB Global Tables requires on-demand or provisioned in each region** | Cost in every active region, not just primary |
| **Pilot Light DB must be running** | The whole point of pilot light is DB is already up. If DB is also stopped = Backup & Restore |
| **FIS stop conditions** | Always configure a stop condition (CloudWatch alarm) to abort chaos experiments gone wrong |

### Production Pitfalls

| Pitfall | Solution |
|---|---|
| **Untested DR runbook** | Run FIS experiments quarterly. Actual failover drill annually |
| **DR region dependencies** | Ensure DR region has sufficient service limits (EC2 vCPU, RDS instance limits) |
| **AMIs out of date** | Automate AMI baking pipeline (EC2 Image Builder). DR AMIs must be current |
| **Secrets not in DR region** | Secrets Manager cross-region replication or Parameter Store per-region |
| **IAM roles missing in DR region** | IAM is global, but test the entire stack works in DR before disaster |
| **Monitoring not watching DR** | CloudWatch dashboards and alarms must exist in DR region too |

---

## 15. Hands-On Lab (Free Tier)

### Goal: Implement Pilot Light DR — RDS read replica + Route53 failover

**Step 1 — Primary Region Setup (us-east-1)**

```bash
# Create RDS MySQL in primary region
aws rds create-db-instance \
  --db-instance-identifier primary-db \
  --db-instance-class db.t3.micro \
  --engine mysql \
  --master-username admin \
  --master-user-password SecurePass123! \
  --allocated-storage 20 \
  --backup-retention-period 7 \
  --region us-east-1
```

**Step 2 — Create Cross-Region Read Replica (DR Region)**

```bash
# Wait for primary to be available, then create replica
aws rds create-db-instance-read-replica \
  --db-instance-identifier dr-replica \
  --source-db-instance-identifier \
    arn:aws:rds:us-east-1:ACCOUNT:db:primary-db \
  --db-instance-class db.t3.micro \
  --region us-west-2
```

**Step 3 — Set Low DNS TTL**

```bash
# In Route53, set primary A record TTL to 60 seconds
aws route53 change-resource-record-sets \
  --hosted-zone-id ZONE-ID \
  --change-batch '{
    "Changes": [{
      "Action": "UPSERT",
      "ResourceRecordSet": {
        "Name": "app.example.com",
        "Type": "A",
        "Failover": "PRIMARY",
        "TTL": 60,
        "HealthCheckId": "PRIMARY-HC-ID",
        "AliasTarget": {
          "HostedZoneId": "ALB-ZONE-ID",
          "DNSName": "primary-alb.us-east-1.elb.amazonaws.com",
          "EvaluateTargetHealth": true
        }
      }
    }]
  }'
```

**Step 4 — Simulate Failover**

```bash
# Simulate: primary becomes unhealthy (disable health check target)
# Then promote replica:
aws rds promote-read-replica \
  --db-instance-identifier dr-replica \
  --region us-west-2

# Wait for promotion (2-5 minutes)
aws rds wait db-instance-available \
  --db-instance-identifier dr-replica \
  --region us-west-2

echo "Replica promoted — now a standalone primary in us-west-2"
echo "Update app config to point to new primary endpoint"
```

**Step 5 — Measure RTO**

```bash
# Start timer when "disaster" begins
START=$(date +%s)

# ... complete all steps ...

END=$(date +%s)
echo "RTO: $((END - START)) seconds"
```

**Step 6 — Test FIS (requires EC2)**

```bash
# Create FIS experiment for EC2 termination
aws fis create-experiment-template \
  --description "Terminate random EC2 instance" \
  --targets '{"MyInstances": {"resourceType": "aws:ec2:instance","resourceTags": {"Environment": "DR-Test"},"selectionMode": "COUNT(1)"}}' \
  --actions '{"TerminateInstance": {"actionId": "aws:ec2:terminate-instances","targets": {"Instances": "MyInstances"}}}' \
  --stop-conditions '[{"source": "aws:cloudwatch:alarm","value": "arn:aws:cloudwatch:...ABORT-ALARM"}]' \
  --role-arn arn:aws:iam::ACCOUNT:role/FISRole

# Start experiment
aws fis start-experiment --experiment-template-id EXP-TEMPLATE-ID
```

**Cleanup:**

```bash
aws rds delete-db-instance --db-instance-identifier dr-replica --skip-final-snapshot --region us-west-2
aws rds delete-db-instance --db-instance-identifier primary-db --skip-final-snapshot --region us-east-1
```
