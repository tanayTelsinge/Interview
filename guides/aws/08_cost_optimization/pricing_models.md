# AWS Cost Optimization — Pricing Models & Tools

## 1. The Problem

Cloud bills grow non-linearly. A startup paying $500/month scales to $50,000/month without changing any pricing model. On-Demand pricing is priced for maximum flexibility — AWS charges a premium for that. Without optimization:

- 30–40% of EC2 spend is wasted on idle or over-provisioned instances
- Teams use On-Demand for predictable workloads (pays the flexibility premium for nothing)
- Data transfer costs are invisible until the bill arrives
- No tagging means no accountability — nobody knows which team owns which cost center

The goal: match the pricing model to the workload's commitment and flexibility profile.

---

## 2. What AWS Built

A layered pricing system with five EC2 purchasing models, complementary discount instruments (Reserved Instances and Savings Plans), free and paid cost visibility tools, and ML-based anomaly detection.

---

## 3. How It Works

### EC2 Pricing Models

#### On-Demand
- Billed **per second** (minimum 60 seconds) for Linux/Windows
- No commitment, no upfront cost
- Highest unit price — baseline everything else is compared to this
- Use case: unpredictable workloads, new apps before usage patterns are known, dev/test

#### Reserved Instances (RI)
| Attribute | Standard RI | Convertible RI |
|-----------|-------------|----------------|
| Max discount | **72%** off On-Demand | **54%** off On-Demand |
| Term | 1 or 3 years | 1 or 3 years |
| Can modify? | Instance size within family | Yes — exchange for different family/OS/tenancy |
| Can sell on Marketplace? | **Yes** | **No** |
| Payment options | No Upfront / Partial Upfront / All Upfront | Same |
| Scope | **Zonal** (capacity reservation in AZ) or **Regional** (flexible across AZs) | Regional only |

**Zonal vs Regional RI:**
- Zonal RI provides a **capacity reservation** — guaranteed instance launch in that AZ
- Regional RI provides **flexibility** — discount applies across all AZs in the region, but no capacity reservation
- For SAP-C02: when you need guaranteed capacity, use Zonal; when you want flexibility, use Regional

#### Savings Plans
Commit to a **dollar-per-hour** spend amount, not to specific instance types.

| Type | Discount | Flexibility |
|------|----------|-------------|
| **Compute Savings Plan** | Up to **66%** | Any EC2 family, region, OS, tenancy + Lambda + Fargate |
| **EC2 Instance Savings Plan** | Up to **72%** | Same family + region (any size/OS) |
| **SageMaker Savings Plan** | Up to 64% | SageMaker instances |

```
Example: Commit to $10/hr of EC2/Lambda/Fargate usage.
AWS applies the discount automatically to the cheapest eligible usage first.
Spend above $10/hr reverts to On-Demand rates.
```

**Savings Plans vs Reserved Instances — Decision:**

| Criteria | Savings Plans | Reserved Instances |
|----------|--------------|-------------------|
| Need instance flexibility | Yes — Compute SP covers any family | No — RI locks to family |
| Need capacity reservation | No | Yes — Zonal RI |
| Want to sell unused commitment | No | Yes — Standard RI Marketplace |
| Cover Lambda/Fargate | Compute SP only | No |
| Need max EC2 discount | EC2 Instance SP = RI | Standard RI = EC2 Instance SP (72%) |
| Accounting preference | $/hr commitment | Instance commitment |

#### Spot Instances
- Spare EC2 capacity, up to **90% off** On-Demand
- **2-minute interruption notice** via instance metadata + EventBridge
- AWS can reclaim at any time
- Use cases: fault-tolerant batch, big data, CI/CD, stateless web tier, ML training
- **Spot Fleet**: mixed Spot + On-Demand fleet across multiple instance types/AZs — maintains target capacity by replacing interrupted instances
- **EC2 Fleet**: extends Spot Fleet, adds On-Demand + RI strategies in one API call
- **Spot best practices**: diversify across 5+ instance types, use `capacity-optimized` allocation strategy, use Spot Interruption Handler (save state, drain from ELB)

#### Dedicated Instances vs Dedicated Hosts
| | Dedicated Instance | Dedicated Host |
|-|-------------------|----------------|
| What's dedicated | Instance runs on single-tenant hardware | Entire **physical server** allocated to you |
| BYOL support | No | **Yes** — see socket/core counts for SQL Server, Oracle |
| Visibility | No insight into host | Full visibility: host ID, socket, core placement |
| Billing | Per instance | Per host (hourly or on reservation) |
| Use case | Regulatory — no hardware sharing | **BYOL licensing**, compliance needing physical isolation |
| Idle billing | Only when instance running | **Always billed even when no instances** |

---

## 4. Key Config & Limits

| Parameter | Value |
|-----------|-------|
| On-Demand minimum billing | 60 seconds |
| Spot interruption notice | 2 minutes |
| Standard RI discount | Up to 72% |
| Convertible RI discount | Up to 54% |
| Compute Savings Plan discount | Up to 66% |
| EC2 Instance Savings Plan discount | Up to 72% |
| RI / Savings Plan terms | 1 year or 3 years |
| S3 Intelligent-Tiering monitoring fee | Per 1,000 objects/month |
| S3 Intelligent-Tiering min object size | 128 KB (smaller objects not moved) |
| Cost Explorer forecast look-ahead | Up to 12 months |
| AWS Budgets — free tier | 2 budgets free, then $0.02/day each |
| CUR granularity | Hourly |

---

## 5. Decision Tree — Pricing Model Selection

```
Is the workload fault-tolerant and stateless?
├─ YES → Use SPOT (max savings 90%)
└─ NO
   └─ Is the workload running < 1 year or irregular?
      ├─ YES → Use ON-DEMAND
      └─ NO (predictable, 1+ years)
         └─ Need to cover Lambda or Fargate too?
            ├─ YES → COMPUTE SAVINGS PLAN (66%, max flexibility)
            └─ NO (EC2 only)
               └─ Same instance family for 1-3 years?
                  ├─ YES → EC2 INSTANCE SAVINGS PLAN or STANDARD RI (72%)
                  └─ NO (might change families)
                     └─ Need capacity reservation?
                        ├─ YES → ZONAL STANDARD RI
                        └─ NO → CONVERTIBLE RI (54%) or COMPUTE SP (66%)
```

**Savings Plans vs RI — Quick Decision:**
- If you need to cover Lambda/Fargate → Savings Plans only
- If you need capacity reservation → Zonal RI only
- If you want to resell unused commitment → Standard RI only
- If you want maximum flexibility → Compute Savings Plans

---

## 6. Common Patterns

### Pattern 1: Three-Layer EC2 Cost Strategy
```
Base load (always on)       → Standard RI or EC2 Instance SP  (72% off)
Variable load (predictable) → Compute Savings Plan             (66% off)
Burst / batch               → Spot Fleet                       (90% off)
Truly unpredictable         → On-Demand                        (full price)
```

### Pattern 2: S3 Cost Optimization
```
S3 Storage Classes by cost (cheapest → most expensive for storage):
  Glacier Deep Archive ($0.00099/GB) < Glacier Flexible ($0.004/GB)
  < Glacier Instant ($0.004/GB) < Standard-IA ($0.0125/GB)
  < One Zone-IA ($0.01/GB) < Standard ($0.023/GB)

Note: Lower storage cost = higher retrieval cost + latency

Lifecycle Policy example:
  0 days    → S3 Standard
  30 days   → S3 Standard-IA (transition after 30 days minimum)
  90 days   → S3 Glacier Instant Retrieval
  365 days  → S3 Glacier Deep Archive
  2555 days → Delete

S3 Intelligent-Tiering:
  - AWS moves objects between tiers automatically based on access patterns
  - Frequent → Infrequent (30 days no access) → Archive Instant (90 days)
  - Optional: Archive Access (90-270 days), Deep Archive (180+ days)
  - Monitoring fee per 1,000 objects; objects < 128KB never moved (charged at Standard)
  - Best for: unpredictable access patterns, mixed workloads

Requester Pays:
  - Requester bears data transfer + request costs, not bucket owner
  - Use case: public datasets, cost chargeback to partners
```

### Pattern 3: Data Transfer Cost Reduction
```
FREE:
  - Inbound data transfer (internet → AWS)
  - Same-region, same-AZ traffic between EC2 instances (private IP)
  - CloudFront → S3 (origin fetches)
  - Data transfer within same service same region (S3 to S3)

CHARGED:
  - Outbound to internet: $0.09/GB first 10TB, tiered lower after
  - Cross-AZ within region: $0.01/GB each direction (~$0.02/GB round trip)
  - Cross-region: varies $0.02-$0.08/GB
  - NAT Gateway: $0.045/GB processed (each direction)
  - VPC Peering cross-AZ: $0.01/GB each direction

Cost reduction tactics:
  - Use private IPs for same-AZ communication (avoids cross-AZ charge)
  - Use VPC endpoints (Gateway: free for S3/DynamoDB; Interface: hourly + per GB)
  - CloudFront for outbound (lower data transfer rates, edge caching)
  - Place compute near data (same AZ)
```

### Pattern 4: Cost Management Toolchain
```
VISIBILITY:
  Cost Explorer → visualize spend, filter by service/tag/region
                → 12-month forecast, RI/SP recommendations
  CUR (Cost & Usage Report) → most granular, hourly line items
                            → export to S3, query with Athena
  Trusted Advisor → right-sizing, idle resources, RI coverage

CONTROL:
  AWS Budgets (4 types):
    1. Cost Budget     — alert when $ exceeds threshold
    2. Usage Budget    — alert when usage (GB, hours) exceeds threshold
    3. RI Budget       — alert when RI utilization/coverage drops below %
    4. Savings Plans   — alert when SP utilization/coverage drops below %
  Budget Actions: apply IAM policy or SCP, stop EC2/RDS when budget breached

OPTIMIZATION:
  Compute Optimizer → ML-based right-sizing for EC2, ASG, EBS, Lambda, ECS
                    → requires 14 days of CloudWatch data minimum
                    → recommendations: over-provisioned, under-provisioned, optimal

ANOMALY DETECTION:
  Cost Anomaly Detection → ML model per service/account/tag
                        → detects unexpected spend spikes
                        → sends SNS alert with root cause analysis
```

### Pattern 5: Tagging Strategy
```yaml
# Mandatory tags enforced via Config rules or Tag Policies (Organizations)
Required Tags:
  Environment: prod | staging | dev | sandbox
  Team:        payments | platform | data | security
  CostCenter:  CC-1234
  Project:     project-name
  Owner:       user@company.com

# Activate as Cost Allocation Tags in Billing console (24hr lag)
# Config rule: required-tags — non-compliant resources flagged
# Tag Policies (Organizations): enforce tag key case, allowed values
```

---

## 7. Gotchas

| Gotcha | Detail |
|--------|--------|
| **Savings Plans apply AFTER RIs** | AWS applies RI discounts first, then Savings Plans to remaining usage. Don't double-commit. |
| **Convertible RIs can't be sold** | Standard RIs can be sold on the RI Marketplace; Convertible cannot. |
| **Cross-AZ data transfer costs are invisible** | $0.01/GB each way — a heavily chatty multi-AZ service can add thousands/month. |
| **NAT Gateway charges per GB** | $0.045/GB processed. An EC2 instance with lots of outbound internet traffic is cheaper through a NAT instance if very high volume. |
| **Stopped EC2 still charges EBS** | Stop ≠ free. EC2 compute stops billing, but EBS volumes continue. |
| **Dedicated Host idle billing** | You pay for the host even with zero instances. A Dedicated Instance only bills when the instance is running. |
| **S3 Intelligent-Tiering monitoring fee** | Small objects (< 128 KB) never move tiers but still pay the monitoring fee — net loss for buckets with millions of tiny objects. |
| **Compute Optimizer needs 14 days** | Recommendations are unavailable for new instances until CloudWatch has 14 days of data. |
| **RI Marketplace restrictions** | Can only sell Standard RIs with at least 1 month remaining. RI stays in your account until sold (still providing discount). |
| **Savings Plans discount order** | Compute SP → EC2 Instance SP → On-Demand. AWS picks the cheapest eligible rate. |

---

## 8. Hands-On Lab — Free Tier

**Goal:** Explore Cost Explorer and set up a billing alert.

### Step 1: Enable Cost Allocation Tags
1. Go to **AWS Billing Console** → Cost Allocation Tags
2. Find tags you've applied to resources (e.g., `Environment`, `Team`)
3. Click **Activate** — tags appear in Cost Explorer after 24 hours

### Step 2: Explore Cost Explorer
```
Billing Console → Cost Explorer → Launch Cost Explorer
  - Set date range: Last 3 months
  - Group by: Service
  - Filter: Specific region or account
  - Save as report

Try: Cost Explorer → RI Coverage Report
  Shows % of EC2 hours covered by RIs
  Target: >80% coverage for steady-state instances
```

### Step 3: Create an AWS Budget
```
Billing Console → Budgets → Create Budget

Budget type: Cost Budget
  Name: monthly-total-cost
  Period: Monthly
  Amount: $XX (your expected monthly spend)

Alert 1: 80% of budgeted amount (forecasted)
Alert 2: 100% of actual spend
  → Email notification to your address

Budget Actions (optional):
  → Apply IAM policy restricting new resource creation
```

### Step 4: Enable Cost Anomaly Detection
```
Billing Console → Cost Anomaly Detection → Create Monitor
  Monitor type: AWS services
  Alert threshold: $10 (absolute) or 20% (percentage)
  SNS topic: Create new or use existing

Result: Email alert when spend on any service spikes unexpectedly
```

### Step 5: View Compute Optimizer Recommendations
```
Compute Optimizer Console (enable if first time)
  → EC2 instances → Filter by "Over-provisioned"
  → Check: current vs recommended instance type
  → Compare: estimated monthly savings

Note: Requires 14 days of CloudWatch metrics
```

### Step 6: CUR + Athena (optional, read about costs first)
```sql
-- After enabling CUR export to S3 and setting up Athena integration:
SELECT
  line_item_product_code,
  SUM(line_item_unblended_cost) AS total_cost
FROM "athenacurcfn"."cost_and_usage_report"
WHERE year='2024' AND month='01'
GROUP BY line_item_product_code
ORDER BY total_cost DESC
LIMIT 20;
```

---

## Summary Reference Card

```
PRICING MODELS (cheapest → most expensive):
  Spot (90% off) → RI/SP (72%/66% off) → On-Demand

COMMITMENT INSTRUMENTS:
  Savings Plans: commit $/hr, flexible across families/regions/Lambda/Fargate
  Reserved Instances: commit to instance family, can reserve capacity (Zonal)

COST TOOLS:
  See:     Cost Explorer (visualize) | CUR (raw hourly data)
  Alert:   AWS Budgets (4 types) | Cost Anomaly Detection (ML)
  Right-size: Compute Optimizer (ML) | Trusted Advisor (checks)

FREE DATA TRANSFER: inbound, same-AZ (private IP), S3→CloudFront
CHARGED:           cross-AZ ($0.01/GB), outbound internet ($0.09/GB), NAT GW ($0.045/GB)
```
