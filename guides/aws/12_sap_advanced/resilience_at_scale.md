# SAP — Resilience Engineering at Scale

## 1. The Problem

At scale, partial failures are not edge cases — they are certainties:

- An AZ fails: can your application serve traffic from the remaining AZs automatically?
- An AWS service is degraded (not down, but slow): does your app cascade into a full outage or degrade gracefully?
- A critical dependency returns 500 errors 10% of the time: does your retry logic make it worse (thundering herd) or better?
- You need to validate that your DR actually works: the day of a real disaster is the wrong time to discover your Aurora Global failover automation has a bug

The challenge is not building systems that work — it's building systems that **fail gracefully** and **recover automatically** at the 99.99% availability threshold.

---

## 2. What AWS Built

| Service | Purpose |
|---------|---------|
| **DynamoDB Global Tables** | Multi-region active-active replication |
| **Aurora Global Database** | Active-passive multi-region with ~1 min RPO |
| **Global Accelerator** | Anycast routing, automatic regional failover |
| **Route53 Application Recovery Controller (ARC)** | Routing controls, safety rules, readiness checks |
| **AWS Fault Injection Service (FIS)** | Controlled chaos engineering experiments |
| **AWS Backup** | Centralized, cross-account backup orchestration |

---

## 3. How It Works

### Resiliency Goals and SLAs

```
Availability → Annual downtime
  99.9%   (three nines)   = 8.76 hours/year
  99.95%                  = 4.38 hours/year
  99.99%  (four nines)    = 52.56 minutes/year
  99.999% (five nines)    = 5.26 minutes/year

To achieve 99.99%:
  Single AZ failure: must recover automatically (multi-AZ)
  Single region failure: must recover in < 52 minutes (multi-region)
  Service degradation: must not cascade (bulkhead, circuit breaker)
```

---

### Multi-Region Active-Active

**Architecture (reads and writes in both regions):**
```
Route53 latency routing:
  us-east-1 → US/EU users
  ap-southeast-1 → APAC users

Each region:
  ALB → ASG (EC2 or ECS)
  DynamoDB Global Tables (multi-master, all regions writeable)
  ElastiCache (session data, region-local)
  CloudFront (CDN, serves content from nearest edge)

Global Accelerator:
  Anycast IPs → routes users to nearest healthy region
  Built-in health checks → automatic failover in < 30 seconds
```

**DynamoDB Global Tables:**
```
Setup:
  Create table in us-east-1 → Add replica in eu-west-1 → Add replica in ap-southeast-1
  Writes to any region replicate to ALL others

Conflict resolution:
  Last-writer-wins: if two regions write same key simultaneously, higher timestamp wins
  No application-level conflict resolution (vs Cassandra's options)

Replication lag: typically < 1 second, but can be seconds
RPO for region failure: seconds (not zero)
RTO for region failure: immediate (other regions already serving writes)

Consider: if user X writes from US and US fails, reads from EU will see X's data
          If the write was in-flight during failure, EU may miss it (< 1s window)
```

**Aurora Global Database:**
```
Primary region (us-east-1): read/write cluster
Secondary regions (eu-west-1, ap-southeast-1): read-only replicas

Replication: < 1 second lag from primary to secondary
Primary failure → promote secondary: ~1 minute (RPO = ~1s, RTO = ~1min)

Active-passive by default:
  All writes go to primary region
  Reads: can read from secondary regions (lower latency for APAC users)

Managed planned failover:
  aws rds failover-global-cluster \
    --global-cluster-identifier my-global-cluster \
    --target-db-cluster-identifier arn:aws:rds:eu-west-1:xxx:cluster:my-cluster

Unmanaged (forced) failover (for actual region failure):
  aws rds remove-from-global-cluster ...  # detach secondary
  → Promotes secondary to standalone writeable cluster
  → OLD primary is still isolated (AWS deletes it after detection)
  → WARNING: forced failover DELETES the old primary cluster data
             (it's already lost in a real region failure scenario)
```

**Stateless Sessions for Active-Active:**
```
Problem: if user's session is in memory on us-east-1 EC2 instance,
         and us-east-1 fails, user must re-login from eu-west-1

Solution: Externalize session state

Option 1: DynamoDB Global Tables
  app_sessions table replicated to all regions
  TTL: session_expiry attribute (auto-delete old sessions)

Option 2: ElastiCache Redis with Global Datastore
  Replicates Redis data across regions
  RPO: seconds

Option 3: JWT (stateless tokens)
  Session data encoded in signed JWT token
  No server-side session state
  Scales to infinite instances, any region reads session from token

Best: JWT for session + DynamoDB for persistent user state
```

**Multi-Region Load Balancing with CloudFront Origin Groups:**
```
CloudFront distribution:
  Primary origin: ALB in us-east-1
  Failover origin: ALB in eu-west-1

Origin group failover criteria:
  HTTP 5xx errors OR timeout → automatically route to secondary origin
  Failover within: seconds (CDN layer)

Route53 latency-based routing:
  *.myapp.com → us-east-1 ALB (for US users)
  *.myapp.com → eu-west-1 ALB (for EU users)

Global Accelerator alternative:
  Static Anycast IPs → AWS global network → nearest healthy region
  Advantage: no DNS TTL delay, true instant failover
```

---

### Multi-Region Active-Passive

**Route53 Failover Routing:**
```
Primary record:
  myapp.com → us-east-1 ALB
  Health check: HTTP check every 10 seconds
  Associate health check: Yes

Secondary record:
  myapp.com → eu-west-1 ALB
  Failover type: Secondary
  No health check needed on secondary

When primary health check fails:
  Route53 automatically routes to secondary
  DNS propagation: 60 seconds (standard TTL)
  Health check failure detection: 10-30 seconds
  Total failover: 60-90 seconds

Health check → SNS → Lambda → trigger DR automation
```

**Aurora Global Failover Automation:**
```python
# Lambda triggered by Route53 health check failure → SNS

import boto3

def lambda_handler(event, context):
    rds = boto3.client('rds', region_name='eu-west-1')
    sns = boto3.client('sns')

    # Step 1: Remove secondary from global cluster (promotes it)
    rds.remove_from_global_cluster(
        GlobalClusterIdentifier='my-global-cluster',
        DbClusterIdentifier='arn:aws:rds:eu-west-1:xxx:cluster:secondary-cluster'
    )
    # Secondary is now an independent writeable cluster

    # Step 2: Update application config
    ssm = boto3.client('ssm', region_name='eu-west-1')
    ssm.put_parameter(
        Name='/myapp/database/primary-endpoint',
        Value='secondary-cluster.cluster-xxx.eu-west-1.rds.amazonaws.com',
        Overwrite=True
    )

    # Step 3: Scale up DR region ASG
    asg = boto3.client('autoscaling', region_name='eu-west-1')
    asg.set_desired_capacity(
        AutoScalingGroupName='myapp-eu-asg',
        DesiredCapacity=10,  # match production capacity
        HonorCooldown=False
    )

    # Step 4: Update Route53 (already done via health check routing)
    # But DNS change for direct record:
    r53 = boto3.client('route53')
    r53.change_resource_record_sets(
        HostedZoneId='ZXXXX',
        ChangeBatch={
            'Changes': [{
                'Action': 'UPSERT',
                'ResourceRecordSet': {
                    'Name': 'db.myapp.com',
                    'Type': 'CNAME',
                    'TTL': 60,
                    'ResourceRecords': [{'Value': 'secondary-cluster.cluster-xxx.eu-west-1.rds.amazonaws.com'}]
                }
            }]
        }
    )

    # Step 5: Validate
    # ... run smoke tests

    sns.publish(
        TopicArn='arn:aws:sns:eu-west-1:xxx:dr-notifications',
        Message='DR Failover COMPLETE: app serving from eu-west-1'
    )
```

---

### Cell-Based Architecture

**Concept — limit blast radius to 1/N of users:**
```
Traditional: one global service → failure affects all users
Cell-based:  divide users into N cells → failure affects 1/N users

Example: 100 cells × 1M users/cell = 100M total users
         One cell fails → 1M affected, 99M continue

Cell isolation:
  - Separate compute (ASG per cell)
  - Separate database (DynamoDB per cell or partition)
  - Separate queue (SQS per cell)
  - Separate monitoring

Cell assignment:
  User ID hash → cell number
  user_cell = hash(user_id) % NUM_CELLS
  Request routed to correct cell at load balancer
```

**Route53 Application Recovery Controller (ARC):**
```
Routing Controls:
  On/Off switches for traffic routing
  "Turn off US-EAST-1 cell without changing DNS records manually"

  Example:
    ROUTING_CONTROL: us-east-1-cell-01
      State: ON (traffic flows)
    ROUTING_CONTROL: eu-west-1-cell-01
      State: OFF (no traffic)

  Toggle: Route53 ARC console → flip switch → DNS propagates within 60 seconds

Safety Rules:
  Prevent accidental all-cells-off scenario:
  MINIMUM_RULE: at least 2 cells must be ON at all times
  ASSERTION_RULE: if cell A OFF, cell B must be ON first

Readiness Checks:
  Continuously validate DR capacity is ready:
  - DR region has enough EC2 instances running
  - DR database is not too far behind primary (replication lag < threshold)
  - DR load balancer is healthy
  - Required IAM roles exist
  ARC reports: Ready or Not Ready per check item
```

---

### AWS Fault Injection Service (FIS)

**Chaos Engineering concept:**
```
Hypothesis: "System remains available when us-east-1b AZ fails"

FIS experiment:
  Target: EC2 instances in us-east-1b
  Action: terminate all instances
  Stop condition: CloudWatch alarm "ErrorRate > 5%"

Result:
  - If system handles it: hypothesis validated ✓
  - If system fails: discovered before real AZ failure ✓

Chaos engineering findings:
  "We discovered our RDS Multi-AZ failover worked, but DNS update
   took 45 seconds, during which health checks failed and customers
   saw errors. We fixed by implementing retry with exponential backoff."
```

**FIS Experiment Structure:**
```json
{
  "targets": {
    "EC2Instances": {
      "resourceType": "aws:ec2:instance",
      "resourceTags": {"Environment": "prod", "Cell": "us-east-1-cell-01"},
      "selectionMode": "PERCENT(50)"
    }
  },
  "actions": {
    "TerminateInstances": {
      "actionId": "aws:ec2:terminate-instances",
      "targets": {"Instances": "EC2Instances"}
    }
  },
  "stopConditions": [{
    "source": "aws:cloudwatch:alarm",
    "value": "arn:aws:cloudwatch:us-east-1:xxx:alarm:error-rate-critical"
  }]
}
```

**Available FIS Actions:**
```
EC2:
  aws:ec2:terminate-instances
  aws:ec2:stop-instances
  aws:ec2:send-spot-instance-interruptions

AZ:
  aws:ec2:disrupt-connectivity (simulate AZ failure subset)

Network:
  aws:network:disrupt-connectivity (packet loss, latency injection)
  aws:network:throttle-connectivity (bandwidth throttling)

Database:
  aws:rds:failover-db-cluster
  aws:rds:reboot-db-instances

Lambda:
  aws:lambda:invocation-add-delay
  aws:lambda:invocation-error (inject errors)

ECS:
  aws:ecs:drain-container-instances
  aws:ecs:stop-task
```

**GameDay process:**
```
Pre-GameDay:
  1. Hypothesis: "System handles X failure scenario"
  2. FIS experiment created and reviewed
  3. Stop conditions set (protect production)
  4. All team members briefed

GameDay:
  5. Inject failure via FIS
  6. Observe: metrics, dashboards, alerts
  7. Team responds as if real incident
  8. FIS stops if stop condition triggered (safety net)

Post-GameDay:
  9. Document findings (what failed, why, how long to detect/recover)
  10. File improvement items
  11. Re-test after fixes to validate improvement
```

---

### Resilience Patterns

**Retry with Exponential Backoff + Jitter:**
```python
import time
import random

def call_with_retry(func, max_attempts=5, base_delay=0.1):
    for attempt in range(max_attempts):
        try:
            return func()
        except TransientError as e:
            if attempt == max_attempts - 1:
                raise
            # Exponential backoff: 0.1s, 0.2s, 0.4s, 0.8s, 1.6s
            # Jitter: randomize to prevent thundering herd
            delay = base_delay * (2 ** attempt) + random.uniform(0, 0.1)
            time.sleep(delay)

# Thundering herd problem (WITHOUT jitter):
# 1000 Lambda functions all fail at once
# All retry after exactly 0.1s → 1000 simultaneous retries → downstream overwhelmed
# All retry after exactly 0.2s → still 1000 simultaneous → still overwhelmed
# WITH jitter: each waits a different random amount → load spread over time
```

**Circuit Breaker:**
```python
class CircuitBreaker:
    def __init__(self, failure_threshold=5, timeout=60):
        self.failures = 0
        self.threshold = failure_threshold
        self.timeout = timeout
        self.state = 'CLOSED'  # CLOSED=normal, OPEN=failing, HALF_OPEN=testing
        self.last_failure_time = None

    def call(self, func):
        if self.state == 'OPEN':
            if time.time() - self.last_failure_time > self.timeout:
                self.state = 'HALF_OPEN'  # Allow one test request
            else:
                raise CircuitOpenError("Circuit breaker is OPEN — fast fail")

        try:
            result = func()
            if self.state == 'HALF_OPEN':
                self.state = 'CLOSED'  # Recovering
                self.failures = 0
            return result
        except Exception as e:
            self.failures += 1
            self.last_failure_time = time.time()
            if self.failures >= self.threshold:
                self.state = 'OPEN'  # Stop calling failing service
            raise

# Use case: Payment service → External bank API
# Bank API is slow/failing → Circuit breaker trips OPEN
# App returns cached result or "payment delayed" instead of waiting 30s per request
# Every 60s: try one request (HALF_OPEN) → if success, back to CLOSED
```

**Bulkhead:**
```
Isolate resources to prevent one consumer from starving others:

Without bulkhead:
  10 Lambda functions share one SQS queue
  1 function processes slow (legacy system down)
  → Fills Lambda concurrent execution limit
  → Other 9 functions can't execute
  → Total service outage

With bulkhead:
  10 Lambda functions each have their own SQS queue + separate concurrency reservation
  1 function reserved concurrency: 100
  Others each: 100 separate reserved slots
  One function blocked → only affects that function's consumers

AWS implementation:
  Lambda reserved concurrency: allocate separate pools per function
  ECS: separate task CPU/memory limits per service
  Fargate: separate cluster per environment
```

**Timeout:**
```python
# NEVER wait forever for a downstream call
import boto3
from botocore.config import Config

# Configure timeouts for all AWS SDK calls:
config = Config(
    connect_timeout=5,   # 5s to establish connection
    read_timeout=30,     # 30s for response
    retries={'max_attempts': 3}
)
client = boto3.client('dynamodb', config=config)

# External HTTP call timeout:
import requests
response = requests.get(
    'https://api.payment-processor.com/charge',
    timeout=(5, 30)  # (connect_timeout, read_timeout)
)
```

**Graceful Degradation:**
```
When DB is unavailable, serve cached/static response instead of error:

def get_product_details(product_id):
    try:
        # Try DynamoDB (primary)
        return dynamodb.get_item(...)
    except Exception as e:
        logger.warning(f"DynamoDB unavailable: {e}")
        # Fallback 1: try ElastiCache (stale but available)
        cached = redis.get(f"product:{product_id}")
        if cached:
            return json.loads(cached)
        # Fallback 2: return static/default response
        return {"product_id": product_id, "status": "details_temporarily_unavailable"}
        # Do NOT: raise exception → 500 → customer sees error
```

---

### Serverless Resilience

**Lambda Async Invocations:**
```
Problem: Lambda async invocation fails (timeout, 500 error)
Default: retries 2 times (0s, 1min, 2min delays) then drops event

Solution: configure destinations
  On failure → SQS Dead Letter Queue
  On failure → SNS notification
  On success → SNS/SQS/EventBridge/Lambda
  On failure → EventBridge event bus

Example configuration:
  Maximum retry attempts: 2
  Maximum event age: 6 hours
  On-failure destination: arn:aws:sqs:us-east-1:xxx:failed-events

Bisect on error:
  If batch of SQS messages fails, bisect batch (send first half as new batch)
  Isolates which specific message is causing failures
  Re-enable: SQS event source mapping → "BisectBatchOnFunctionError": true
```

**SQS + DLQ + Alarm:**
```
SQS Queue (main):
  Message retention: 4 days
  Visibility timeout: 6× Lambda timeout (prevent concurrent processing)
  Redrive policy: MaxReceiveCount=3 → Dead Letter Queue

Dead Letter Queue:
  Message retention: 14 days
  CloudWatch Alarm: ApproximateNumberOfMessagesVisible > 0
    → SNS → PagerDuty → engineer investigates

Process DLQ manually or with another Lambda after root cause fixed
```

**Step Functions Retry + Catch:**
```json
{
  "Type": "Task",
  "Resource": "arn:aws:lambda:us-east-1:xxx:function:ProcessOrder",
  "Retry": [{
    "ErrorEquals": ["Lambda.ServiceException", "Lambda.AWSLambdaException"],
    "IntervalSeconds": 2,
    "MaxAttempts": 3,
    "BackoffRate": 2.0,
    "JitterStrategy": "FULL"
  }],
  "Catch": [{
    "ErrorEquals": ["States.ALL"],
    "Next": "HandleOrderFailure",
    "ResultPath": "$.error"
  }],
  "Next": "SuccessState"
}
```

---

### AWS Backup

**Centralized backup management:**
```
Services supported:
  EC2 (AMI-based), EBS, RDS (all engines), Aurora,
  DynamoDB, EFS, FSx (Windows, Lustre, NetApp, OpenZFS),
  S3, DocumentDB, Neptune, SAP HANA on EC2

Backup Vault:
  Logical container for backups (recovery points)
  KMS encryption on vault

Backup Plans:
  Schedule: every 12 hours, daily, weekly, monthly
  Retention: 35 days, 1 year, 7 years
  Copy to: another region (cross-region backup)
  Copy to: another account (cross-account backup)

Backup Vault Lock (WORM):
  GOVERNANCE mode: org admin can remove the lock (admin accounts only)
  COMPLIANCE mode: NO ONE can delete, not even AWS (immutable)
  Use compliance mode for: regulatory requirements, ransomware protection
```

**Cross-Account Backup:**
```
Source account: application workloads
Backup account: dedicated, separate account

Benefits:
  - Ransomware compromise of source account → backups safe in separate account
  - Compliance: separate backup admin team
  - Different IAM permissions for backup vs restore

Configuration:
  AWS Backup → Create backup vault in destination account
  Grant cross-account access to AWS Backup in source account
  Backup plan in source account → copy to destination account vault
```

---

## 4. Key Config & Limits

| Parameter | Value |
|-----------|-------|
| Route53 health check frequency | Every 10 or 30 seconds |
| Route53 health check failure threshold | 3 consecutive failures to trigger |
| DNS propagation after failover | 60 seconds (with low TTL) |
| Aurora Global forced failover | Deletes old primary |
| Aurora Global promote (managed) | ~1 minute |
| DynamoDB Global Tables replication lag | Typically < 1 second |
| Global Accelerator failover time | < 30 seconds |
| FIS stop conditions | CloudWatch alarm OR timeout |
| AWS Backup vault lock (compliance) | Immutable, even AWS can't remove |
| ARC routing controls | Per-region on/off traffic switches |
| ARC safety rules | Minimum N controls ON at all times |
| Lambda retry (async) | 2 retries by default, configurable 0-2 |
| SQS DLQ MaxReceiveCount | 1-1000 |

---

## 5. Decision Trees

### AZ Failure Response
```
AZ fails → automatic if:
  EC2: Multi-AZ ASG (replace instances in healthy AZs)
  RDS: Multi-AZ deployment (standby promotes in 60-120s)
  Aurora: automatic (writer moves to different AZ)
  ALB: built-in multi-AZ (drop AZ automatically)
  ElastiCache: Multi-AZ cluster mode
  DynamoDB: automatically multi-AZ (no config needed)

Manual if:
  EC2 without ASG → move instance manually
  Single-AZ EFS → no AZ failover possible
```

### Region Failure Response
```
Active-Active (both regions serve traffic)?
└─ DynamoDB Global Tables + Route53 latency/health check
   Global Accelerator → instant failover (no DNS lag)

Active-Passive (one primary, one standby)?
└─ Aurora Global → promote secondary (1 min)
   Route53 failover routing → DNS change (60-90s)
   Lambda automation → scale up DR ASG, update configs

RPO requirement?
  Seconds: DynamoDB Global Tables (active-active), Aurora Global (<1s lag)
  Minutes: S3 replication (~15 min), Aurora (1s lag promoted after 1 min)
  Hours:   Daily backup restore (AWS Backup to second region)
```

### Partial Failure Response
```
Downstream service returning errors:
  Transient (< 30s)? → Retry with exponential backoff + jitter
  Persistent (minutes)? → Circuit breaker → fallback/degraded mode
  Slow (not failing, just slow)? → Timeout → fail fast

Resource exhaustion:
  One consumer starving others? → Bulkhead (separate resource pools)
  Memory/CPU spike one service? → Container resource limits

Data loss risk:
  Lambda async drop? → DLQ + alarm
  SQS message processing failure? → DLQ + redrive policy
  Step Functions failure? → Retry + Catch → compensating transaction
```

---

## 6. Common Patterns

### Pattern 1: Multi-Region Active-Passive with Full Automation
```
Steady state:
  us-east-1: production (10 EC2, 1 Aurora cluster, DynamoDB)
  eu-west-1: DR standby (2 EC2 warm, Aurora Global secondary)
  Aurora Global: replication lag < 1s
  DynamoDB: Global Tables (writes replicated both ways)
  Route53: failover routing, health check every 10s

Failure detected:
  Route53 health check: 3 consecutive failures (30-90 seconds)
  → Triggers SNS → Lambda DR automation

Lambda DR automation (5-10 minutes total):
  Step 1: Verify failure is real (not false positive) — 30 seconds
  Step 2: Remove Aurora secondary from global cluster → promotes to writer
  Step 3: Scale eu-west-1 ASG from 2 → 10 instances
  Step 4: Update SSM Parameter: /app/db/endpoint → eu-west-1 endpoint
  Step 5: Route53 health check triggers DNS failover (already propagating)
  Step 6: Run smoke tests against eu-west-1
  Step 7: SNS: "DR FAILOVER COMPLETE — eu-west-1 serving 100% traffic"

ARC Readiness Checks:
  Continuously validate:
    ✓ eu-west-1 Aurora replication lag < 5s
    ✓ eu-west-1 ASG capacity > 2 instances
    ✓ eu-west-1 ALB health check passing
    ✓ DynamoDB eu-west-1 replication lag < 10s
  If any check fails: alert on-call BEFORE incident, not during
```

### Pattern 2: Cell-Based Architecture with ARC
```
User assignment: hash(user_id) % 10 = cell 0-9

Route53 weighted routing:
  cell-0.myapp.com → us-east-1 cell-0 stack
  cell-1.myapp.com → us-east-1 cell-1 stack
  ...

API Gateway routes user to cell:
  GET /api/users/{userId}/data
  → API GW → Lambda: determine cell → redirect to cell-specific ALB

ARC Routing Controls:
  CELL_0_US_EAST_1: ON
  CELL_0_EU_WEST_1: OFF  (standby)

  Incident on cell-3:
    ARC: flip CELL_3_US_EAST_1 → OFF
         flip CELL_3_EU_WEST_1 → ON
  → 10% of users (cell-3) moved to EU, other 90% unaffected

Safety rule: minimum 8 cells ON at all times (can't accidentally shut down 5+ cells)
```

### Pattern 3: Chaos Engineering GameDay
```
Schedule: quarterly GameDay, production system (with safety guards)

Experiments to run:
  1. Terminate 50% of ASG instances in AZ-1
     Hypothesis: ASG replaces in < 5 minutes, no customer impact
     Stop: ErrorRate > 1%

  2. Aurora failover (RDS failover action via FIS)
     Hypothesis: app reconnects within 60 seconds
     Stop: database connection error rate > 5%

  3. Network packet loss 10% on payment service → third-party API
     Hypothesis: circuit breaker activates, returns cached/degraded response
     Stop: payment service error rate > 2%

  4. DynamoDB throttle table
     Hypothesis: retry with backoff succeeds, no customer-facing errors
     Stop: 500 error rate > 0.5%

Results from last GameDay:
  Finding 1: Aurora failover worked but app logged 10,000 errors in 30s
             (reconnection retry interval was 5s, not 1s with backoff)
             Fix: configure RDS connection pool retry = 1s exponential backoff

  Finding 2: Circuit breaker worked but timeout was 30s (too high)
             Fix: reduce payment API timeout from 30s to 5s
```

---

## 7. Gotchas

| Gotcha | Detail |
|--------|--------|
| **Route53 health check propagation 10-30s** | Health check detects failure in 10-30 seconds (depending on check interval + failure threshold). Then DNS change propagates. Total failover time = 60-120 seconds. Design for this gap. |
| **Aurora Global forced failover deletes old primary** | When you forcibly remove a secondary from a Global Database (for real region failure), the old primary cluster is eventually deleted. Ensure you haven't routed any traffic back to it. |
| **FIS production needs careful stop conditions** | Without a stop condition, an FIS experiment that terminates all instances keeps running. Always set a CloudWatch alarm stop condition in production. Test stop conditions work before running the full experiment. |
| **Active-active is 2× cost** | Running production capacity in two regions simultaneously doubles compute and database costs. Active-passive with pre-warmed DR (2 instances standby) is cheaper. Justify active-active only when RTO < 5 minutes is required. |
| **DynamoDB Global Tables replication lag is seconds** | Writes replicate in < 1 second usually but not always. If your application requires strict read-after-write consistency across regions, it may read stale data. Design reads to be tolerant of seconds of lag. |
| **ARC routing controls are NOT instant DNS** | ARC routing controls update Route53 but DNS still has TTL propagation. Pre-lower TTLs (60s). For instant traffic shifting, use Global Accelerator (no DNS, Anycast routing). |
| **Lambda concurrency limits still apply in DR** | If your primary region Lambda runs 1000 concurrent, your DR region Lambda default limit is also 1000 (account limit, not per-function). Pre-request concurrency limit increase in DR region. |
| **Circuit breaker state is per-instance** | A circuit breaker implemented in memory is per Lambda instance / per EC2 instance. If you have 100 instances, each has independent circuit breaker state. Use DynamoDB or ElastiCache for shared circuit breaker state. |
| **Backup vault lock compliance mode is permanent** | Once a vault is locked in compliance mode, it cannot be unlocked. Test in governance mode first. Minimum lock duration: 7 days. No way to reduce after locking. |

---

## 8. Hands-On Lab — Free Tier

**Goal:** Configure Route53 health check failover and test with FIS.

### Step 1: Deploy Two ALBs (Simulated Regions — Two AZs)
```
# For lab: simulate multi-region with two EC2 instances
# Launch 2 EC2 t2.micro instances:
  Instance 1: us-east-1a — serves "Region: US-EAST-1A"
  Instance 2: us-east-1b — serves "Region: US-EAST-1B"

# Add user data for simple web server:
#!/bin/bash
echo "<h1>Region: US-EAST-1A</h1>" > /var/www/html/index.html
yum install -y httpd && systemctl start httpd && systemctl enable httpd
```

### Step 2: Create Route53 Health Checks
```
Route53 Console → Health checks → Create health check

Primary health check:
  Name: lab-primary-hc
  Type: HTTP
  IP address: <EC2-1 public IP>
  Path: /index.html
  Port: 80
  Request interval: 10 seconds
  Failure threshold: 3
```

### Step 3: Create Route53 Failover Records
```
Route53 → Hosted zone → Create record

Primary record:
  Name: lab.yourdomain.com
  Type: A
  Value: <EC2-1 public IP>
  Routing policy: Failover
  Failover record type: Primary
  Health check: lab-primary-hc
  TTL: 60

Secondary record:
  Name: lab.yourdomain.com
  Type: A
  Value: <EC2-2 public IP>
  Routing policy: Failover
  Failover record type: Secondary
  TTL: 60
```

### Step 4: Test Failover
```bash
# Verify primary is being returned:
dig lab.yourdomain.com

# Stop web server on EC2-1 (simulate failure):
# EC2 Console → select Instance 1 → Actions → Stop

# Wait 30-60 seconds for health check to fail
# Then:
dig lab.yourdomain.com
# Should now return EC2-2 IP

# Verify:
curl http://lab.yourdomain.com
# Should return "Region: US-EAST-1B"
```

### Step 5: Set Up FIS Experiment
```
FIS Console → Experiment templates → Create template

IAM role: Create new (FIS service role with EC2 stop permissions)

Target:
  Resource type: aws:ec2:instance
  Tag: Name=lab-primary

Action:
  aws:ec2:stop-instances
  Target: (your target)
  Duration: PT2M (2 minutes)

Stop conditions:
  Source: none (lab — always runs to completion)

Save and start experiment → observe Route53 failover in real time
```

### Step 6: Implement Exponential Backoff (Code Review)
```python
# Review and test this pattern:
import time
import random

def resilient_request(url, max_retries=5):
    for i in range(max_retries):
        try:
            response = requests.get(url, timeout=5)
            response.raise_for_status()
            return response
        except (requests.RequestException, requests.HTTPError) as e:
            if i == max_retries - 1:
                raise  # Last attempt failed
            # Exponential backoff with full jitter
            sleep_time = random.uniform(0, min(60, 0.1 * 2 ** i))
            print(f"Attempt {i+1} failed: {e}. Retrying in {sleep_time:.2f}s")
            time.sleep(sleep_time)
```

### Step 7: Clean Up
```
Delete FIS experiment template
Delete Route53 health check and records
Terminate both EC2 instances
```

---

## Summary Reference Card

```
AVAILABILITY TARGETS:
  99.9%  = 8.7 hr/year | 99.99% = 52 min/year | 99.999% = 5 min/year

MULTI-REGION ACTIVE-ACTIVE:
  Data: DynamoDB Global Tables (last-writer-wins, seconds lag)
  Routing: Route53 latency + health checks, Global Accelerator (< 30s failover)
  Sessions: stateless JWT or DynamoDB Global Tables
  Cost: 2× (both regions at full capacity)

MULTI-REGION ACTIVE-PASSIVE:
  Data: Aurora Global (< 1s replication lag, 1 min promote RTO)
  Routing: Route53 failover routing (60-90s failover total)
  Automation: Lambda triggers on health check SNS notification
  ARC: readiness checks validate DR capacity continuously

CELL-BASED:
  Blast radius = 1/N users per cell
  ARC routing controls: on/off switches per cell
  ARC safety rules: prevent accidentally killing all cells

FIS (CHAOS ENGINEERING):
  Inject: EC2 terminate, AZ disrupt, RDS failover, network latency
  Stop conditions: CloudWatch alarm threshold
  GameDay: quarterly, document findings, re-test after fixes

RESILIENCE PATTERNS:
  Retry: exponential backoff + jitter (prevent thundering herd)
  Circuit breaker: fast-fail when downstream failing
  Bulkhead: separate resource pools per consumer
  Timeout: never wait forever
  Graceful degradation: serve cached/default on dependency failure

AWS BACKUP:
  Centralized: EC2, RDS, DynamoDB, EFS, FSx, S3, Aurora
  Cross-account + cross-region copies
  Vault Lock COMPLIANCE: immutable even by AWS
```
