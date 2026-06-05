# High Availability Patterns — SAP-C02 Deep Dive

---

## 1. The Problem

Every component in a system is a potential single point of failure (SPOF). A single EC2 instance, a single AZ, a single database — any one failure brings the entire system down. The engineering challenge is not eliminating all failure (impossible), but designing systems that continue operating correctly when individual components fail.

**Common SPOFs in naive architectures:**
- Single EC2 instance with Elastic IP — instance fails = complete outage
- Single AZ deployment — AZ power/networking failure = outage
- EBS-backed state on one instance — instance fails = data inaccessible
- Single RDS instance — DB fails = complete outage
- Hardcoded session state in app memory — instance dies = user logged out

High availability patterns systematically eliminate each SPOF while managing cost.

---

## 2. Multi-AZ Architecture

### The Baseline: 3 AZ Design

```
                    Route53 / CloudFront
                          |
                  Application Load Balancer
                 (spans all 3 AZs natively)
                    /         |        \
              AZ-a          AZ-b         AZ-c
               |              |            |
          EC2 (ASG)      EC2 (ASG)    EC2 (ASG)
               |              |            |
          ─────────────────────────────────────
                     Private subnet
               |              |            |
          ─────────────────────────────────────
                    RDS Multi-AZ
                  (primary in one AZ)
                  (standby in another AZ)
                  (automatic failover ~60s)
```

### Service HA Characteristics

| Service | HA Mechanism | Notes |
|---|---|---|
| **S3** | 11 nines durability, multi-AZ by default | No single-AZ risk; use versioning + MFA delete |
| **EFS** | Multi-AZ by default (Regional) | EFS One Zone is single-AZ (lower cost) |
| **EBS** | Single AZ — NOT HA | Volume tied to one AZ; snapshot = cross-AZ recovery |
| **RDS Multi-AZ** | Synchronous standby in second AZ, auto-failover | ~60 second failover; read replica ≠ Multi-AZ |
| **Aurora** | 6-way replication across 3 AZs, auto-failover < 30s | Writer + up to 15 readers |
| **ElastiCache** | Multi-AZ replica + automatic failover | Redis: primary + replica; cluster mode for sharding |
| **ALB/NLB** | Spans multiple AZs natively | Cross-zone load balancing recommended |
| **DynamoDB** | Multi-AZ by default | Sync replication across 3 AZs in region |

### EBS vs EFS vs S3 for HA

```
EC2 Auto Scaling Group (multiple AZs)
    |
    ├── EBS: mounted to ONE instance in ONE AZ
    │   → NOT suitable for ASG with multiple AZs
    │   → Use for: single-instance databases, high-performance local storage
    │
    ├── EFS: mounted to ANY instance in ANY AZ (same region)
    │   → Suitable for shared file storage across ASG
    │   → Use for: shared content, logs, WordPress, config files
    │
    └── S3: accessible from any AZ/region
        → Suitable for: static assets, user uploads, config
        → Latency higher than EBS/EFS
```

---

## 3. Auto Scaling Deep Dive

### Launch Templates vs Launch Configurations

| Feature | Launch Template | Launch Configuration |
|---|---|---|
| **Status** | **Preferred (current)** | Legacy (no new features) |
| Versioning | Yes (multiple versions) | No |
| Mixed instance types | Yes (spot + on-demand) | No |
| T2/T3 unlimited | Yes | No |
| Placement groups | Yes | No |
| Elastic inference | Yes | No |
| Dedicated hosts | Yes | No |

Always use **Launch Templates** for new deployments.

### Scaling Policies

#### 1. Target Tracking Scaling (Recommended)

```
Target: CPU utilization = 50%
ASG adds/removes instances to maintain the target metric

Pros: Simple, AWS manages the math, most common
Cons: Scale-in is conservative (default 15-min cooldown)

Common targets:
  - ASGAverageCPUUtilization: 50–70%
  - ALBRequestCountPerTarget: e.g., 1000 req/instance
  - ASGAverageNetworkIn/Out: bytes/second
  - Custom metric: SQS messages per instance
```

#### 2. Step Scaling

```
Alarm: CPU > 70% for 2 minutes → Add 2 instances
Alarm: CPU > 85% for 1 minute → Add 5 instances
Alarm: CPU < 30% for 10 minutes → Remove 1 instance

Pros: Fine-grained control per threshold
Cons: Must define each step manually
```

#### 3. Scheduled Scaling

```
# Scale up before business hours
aws autoscaling put-scheduled-update-group-action \
  --auto-scaling-group-name my-asg \
  --scheduled-action-name scale-up-morning \
  --recurrence "0 8 * * MON-FRI" \
  --min-size 5 \
  --max-size 20 \
  --desired-capacity 10

# Scale down at night
aws autoscaling put-scheduled-update-group-action \
  --auto-scaling-group-name my-asg \
  --scheduled-action-name scale-down-night \
  --recurrence "0 20 * * *" \
  --desired-capacity 2
```

**Key: Scheduled scaling acts BEFORE expected load (proactive), not reactively.**

#### 4. Predictive Scaling

```
Predictive Scaling (uses ML):
  Analyzes historical CloudWatch metrics
  → Predicts future load
  → Pre-scales 1 hour before predicted peak
  → Runs alongside reactive scaling

Enable:
  ForecastOnly mode: test predictions without scaling
  ForecastAndScale mode: autonomous scaling
```

### Scaling Cooldown

- Default cooldown: **300 seconds** (5 minutes)
- During cooldown, ASG does not launch/terminate additional instances
- Target tracking has separate scale-in/scale-out cooldowns
- Reduce cooldown for fast-responding apps; increase for slow-starting apps

### Lifecycle Hooks

```
Scale Out Event
      |
   EC2 Pending state
      |
   [Lifecycle Hook: EC2_INSTANCE_LAUNCHING]
      |
   Custom action: (up to 1 hour wait)
   - Download config from S3
   - Register with service discovery
   - Run health check
   - Warm up application cache
      |
   CONTINUE signal → EC2 goes InService
   ABANDON signal → EC2 terminated (if setup failed)

Scale In Event
      |
   EC2 Terminating:Wait state
      |
   [Lifecycle Hook: EC2_INSTANCE_TERMINATING]
      |
   Custom action:
   - Deregister from service discovery
   - Drain active connections
   - Upload logs to S3
   - Complete in-flight work
      |
   CONTINUE signal → EC2 terminated
```

```bash
# Complete lifecycle action from within the EC2 instance
aws autoscaling complete-lifecycle-action \
  --lifecycle-hook-name my-launch-hook \
  --auto-scaling-group-name my-asg \
  --lifecycle-action-result CONTINUE \
  --instance-id i-1234567890
```

### Warm Pools

Pre-initialize instances to reduce launch latency:

```
Warm Pool: 5 instances (stopped or running, pre-configured)
      |
Scale-out event: need 3 more instances
      |
Instead of launching from scratch (3-5 min):
  Start instances from warm pool (~30-60 seconds)
      |
Instances are already configured, just need to start

Use case: Apps with long initialization times (large model loading, complex config)
```

### Instance Refresh (Rolling AMI Updates)

```
# Update Launch Template to new AMI version
# Then trigger instance refresh

aws autoscaling start-instance-refresh \
  --auto-scaling-group-name my-asg \
  --preferences MinHealthyPercentage=90,InstanceWarmup=120

# AWS replaces instances gradually:
# - Launch new instance with new AMI
# - Wait for it to pass health check
# - Terminate old instance
# - Repeat until all replaced
```

**MinHealthyPercentage:** Keep at least 90% healthy during refresh.
**InstanceWarmup:** Wait 120 seconds after new instance healthy before proceeding.

### Health Checks — Always Use ELB for Web Apps

| Health Check Type | Detects | Best For |
|---|---|---|
| **EC2 (default)** | Instance stopped/impaired at AWS level | Background workers |
| **ELB** | Application-level health (app returns 200) | **Web apps — always use this** |
| **Custom** | Via lifecycle hooks or health API | Complex multi-component apps |

```bash
# Enable ELB health checks on ASG
aws autoscaling update-auto-scaling-group \
  --auto-scaling-group-name my-asg \
  --health-check-type ELB \
  --health-check-grace-period 300
```

**Health check grace period:** How long to wait before starting health checks on new instances (default 300s, set to your startup time).

### Termination Policies

Order of evaluation for scale-in:

```
1. Find AZ with most instances (balance AZs)
2. Apply termination policy:
   - OldestLaunchTemplate: terminate oldest launch config
   - OldestInstance: terminate oldest instance
   - NewestInstance: terminate newest (test new configs)
   - ClosestToNextInstanceHour: minimize billing (default)
   - Default: balance AZs, then oldest launch config, then closest billing hour
```

---

## 4. Stateless Design

### Why Statefulness Kills HA

```
Stateful EC2 instances:
  User logs in → session stored in memory on Instance A
  User next request → routes to Instance B
  Instance B has no session → user logged out (broken)

  OR: sticky sessions (session affinity):
    All user traffic pinned to one instance
    Instance A dies → user session gone anyway
    Sticky sessions prevent autoscaling benefits
```

### Externalize Session State

```
Option 1: ElastiCache Redis
  User request → App reads/writes session to Redis
  Any instance can serve any user
  Redis cluster: Multi-AZ with automatic failover

Option 2: DynamoDB
  Session data in DynamoDB table
  TTL attribute for automatic session expiry
  Global Tables for multi-region session sharing

Option 3: JWT (Stateless tokens)
  Session data encoded in signed JWT
  No backend storage needed
  Validate signature on each request
  Revocation challenge (use token blacklist in Redis)
```

```python
# DynamoDB session pattern
import boto3
from datetime import datetime, timedelta

dynamodb = boto3.resource('dynamodb')
table = dynamodb.Table('sessions')

def save_session(session_id, user_data):
    table.put_item(Item={
        'sessionId': session_id,
        'userId': user_data['userId'],
        'data': user_data,
        'ttl': int((datetime.now() + timedelta(hours=24)).timestamp())
    })

def get_session(session_id):
    response = table.get_item(Key={'sessionId': session_id})
    return response.get('Item')
```

---

## 5. Circuit Breaker Pattern

### Problem

Downstream service is slow → your app waits → threads pile up → your app runs out of threads → your app becomes slow → upstream services wait → cascade failure.

### Circuit Breaker States

```
CLOSED (normal):
  Requests pass through
  Track failure count
  If failures > threshold → open circuit

OPEN (failing):
  Requests fail immediately (no waiting)
  Return fallback response
  After timeout → try half-open

HALF-OPEN (testing):
  Allow limited requests through
  If success → close circuit
  If failure → back to open

Implementation: AWS App Mesh, Istio, or AWS SDK retry config
```

---

## 6. Blue/Green Deployments

### Using Route53 Weighted Routing

```
Blue environment (current): Route53 weight=100
Green environment (new):    Route53 weight=0

Deployment:
1. Deploy new version to green environment
2. Shift 10% traffic: blue=90, green=10
3. Monitor error rates + latency
4. Shift 50%: blue=50, green=50
5. All clear: blue=0, green=100
6. Decommission blue (or keep as fallback)

Rollback: set green=0, blue=100 instantly
```

### Using ALB Target Group Swap

```
ALB Listener Rule:
  Default: → Blue Target Group (current version)

Deployment:
  1. Create Green Target Group with new instances
  2. Run tests against green directly (via rule with weight or direct endpoint)
  3. Swap: ALB default action → Green Target Group
  4. Blue Target Group remains (instant rollback available)
  5. After confidence: drain + delete Blue
```

### Using CodeDeploy Blue/Green

```
CodeDeploy Blue/Green (ECS):
  1. New task definition (green)
  2. CodeDeploy starts green tasks
  3. Shifts ALB traffic: CanaryInterval or Linear100PercentEvery1Minute
  4. On success: terminate blue tasks
  5. On failure (CloudWatch alarm): rollback to blue
```

---

## 7. Canary Deployments

### Route53 Weighted Canary

```
5% canary:
  Route53: Production → weight=95, Canary → weight=5

Monitor for 30 minutes:
  - Error rate (CloudWatch)
  - P99 latency
  - Business metrics (orders/min)

If good: increase to 20% → 50% → 100%
If bad: set canary weight=0 (instant rollback)
```

### Lambda Aliases for Canary

```bash
# Deploy new Lambda version
aws lambda publish-version --function-name my-function

# Update alias with weighted routing
aws lambda update-alias \
  --function-name my-function \
  --name production \
  --function-version 5 \
  --routing-config AdditionalVersionWeights={"4": 0.05}
# 5% → version 4 (canary), 95% → version 5 (new)

# After validation: promote
aws lambda update-alias \
  --function-name my-function \
  --name production \
  --function-version 5
# 100% → version 5
```

### CodeDeploy Canary for Lambda/ECS

```yaml
# appspec.yml for Lambda canary
version: 0.0
Resources:
  - myLambdaFunction:
      Type: AWS::Lambda::Function
      Properties:
        Name: my-function
        Alias: production
        CurrentVersion: 4
        TargetVersion: 5

Hooks:
  BeforeAllowTraffic: validateNewVersion
  AfterAllowTraffic: validateLiveTraffic
```

Deployment configurations:
- `LambdaCanary10Percent5Minutes`: 10% for 5 min, then 100%
- `LambdaLinear10PercentEvery1Minute`: +10% per minute
- `LambdaAllAtOnce`: immediate full switch (no canary)

---

## 8. Multi-Region HA

### Route53 Policies for Multi-Region

| Policy | Use Case |
|---|---|
| **Latency** | Route to lowest latency region for active/active |
| **Failover** | Primary/secondary — automatic failover on health check |
| **Weighted** | Blue/green across regions, canary rollout |
| **Geolocation** | Route EU users to EU, US users to US (data residency) |
| **Geoproximity** | Route by physical distance with adjustable bias |
| **Multi-value answer** | Return up to 8 IPs with health checks (basic load distribution) |

### Health Check Hierarchy

```
Route53 Health Check
      ↓ (checks ALB endpoint)
ALB Health Check
      ↓ (checks Target Group)
Target Group Health Check
      ↓ (checks EC2 /health endpoint)
EC2 Instance

If EC2 app fails:
  Target Group: UnHealthy → removes from rotation
  ALB: routes to healthy instances in other AZs
  Route53: sees ALB responding → no DNS failover needed

If ALL instances in ALB are unhealthy:
  ALB: returns 503
  Route53: health check of ALB endpoint fails → DNS failover to DR region
```

### CloudFront for Multi-Region HA

```
User → CloudFront (edge cache hit, no origin hit) → 99.9% cache availability

Cache miss → Origin Group:
  Primary: ALB in us-east-1
  Secondary: ALB in us-west-2 (automatic failover on 4xx/5xx/503)

CloudFront Origin Failover:
  - Primary returns 5xx → failover to secondary automatically
  - Faster than DNS TTL-based failover
  - No client DNS caching issue
```

---

## 9. Health Check Hierarchy in Practice

```
Correct HA Stack:
                    DNS (Route53)
                         |
                    CloudFront
                    (edge HA + origin failover)
                         |
                  Application Load Balancer
                  (multi-AZ, built-in HA)
                         |
              ┌──────────┼──────────┐
           AZ-a         AZ-b       AZ-c
             |            |          |
          EC2 (ASG)   EC2 (ASG)  EC2 (ASG)
          (ELB health  check enabled)
             |
          Application
          (sessions in ElastiCache/DynamoDB)
             |
          RDS Aurora
          (Multi-AZ, auto-failover)
             |
          ElastiCache Redis
          (Multi-AZ, automatic failover)

Each layer independently detects failure and recovers
without requiring the layer above to do anything
```

---

## 10. Key Config & Limits

| Parameter | Value |
|---|---|
| ASG launch grace period | Default 300s — set to app startup time |
| ASG health check grace period | Default 300s for ELB health checks |
| Target tracking scale-in cooldown | Default **300s** (15 min was old default) |
| Target tracking scale-out cooldown | Default **0s** (immediate scale out) |
| ALB deregistration delay | Default **300s** — tune down for fast deploys |
| ALB connection draining | Same as deregistration delay |
| Lifecycle hook timeout | 1 hour (3,600 seconds) |
| Warm pool max size | Up to 1,000 instances |
| Route53 health check interval | 10s or 30s |
| Route53 health check failure threshold | 1–10 consecutive checks |
| CloudFront origin failover error codes | Configurable: 500, 502, 503, 504 (4xx optional) |
| RDS Multi-AZ failover | ~60 seconds (synchronous standby) |
| Aurora failover | ~30 seconds (promotes read replica) |

---

## 11. Decision Tree — HA Architecture for Given SLA

```
What is the SLA?

├── 99.9% (8.7 hours/year downtime = ~43 min/month)?
│   └── Single-region multi-AZ:
│       ALB + ASG across 3 AZs + RDS Multi-AZ
│       ElastiCache Multi-AZ
│       EFS (multi-AZ) or S3 for shared state

├── 99.95% (~4.4 hours/year)?
│   └── Single-region multi-AZ + CloudFront
│       + warm standby DR
│       + automated failover runbook

├── 99.99% (~53 minutes/year)?
│   └── Multi-region active/passive (warm standby)
│       Aurora Global + DynamoDB Global Tables
│       Route53 failover with 60s TTL + CloudFront

└── 99.999% (~5 minutes/year)?
    └── Multi-region active/active
        Route53 latency routing
        Aurora Global (writer in each region or active/active logic)
        DynamoDB Global Tables
        Zero-state app tier (sessions in DynamoDB/ElastiCache Global)
```

---

## 12. Gotchas

### Exam Traps

| Gotcha | Detail |
|---|---|
| **ELB health check not default on ASG** | Default = EC2 health check (only detects instance failure, not app failure). Always set to ELB for web apps |
| **ALB deregistration delay 300s** | Default is 5 minutes — during deployment, traffic drains slowly. Tune to 30s for faster deploys |
| **Scheduled scaling must be proactive** | Add buffer (e.g., schedule for 30 min before event, not when load starts) |
| **Target tracking scales in slowly** | Scale-in default cooldown 300s. Intentionally conservative to avoid flapping |
| **Launch Configuration = deprecated** | Use Launch Templates. LC lacks mixed instances, versioning |
| **EBS = single AZ** | Cannot attach EBS from AZ-a to EC2 in AZ-b. Never use for ASG shared storage |
| **Instance refresh does not drain connections** | Use `ALBConnectionDraining` (via lifecycle hook) before refresh replaces instances |
| **Lifecycle hook requires heartbeat** | If custom action doesn't send heartbeat, hook times out and defaults to CONTINUE or ABANDON based on setting |
| **Warm pool incurs cost** | Pre-initialized instances (even stopped) incur EBS/EIP costs. Size appropriately |
| **Aurora failover promotes replica** | Old writer remains running as reader. App reconnection to new writer endpoint required (use cluster endpoint, not instance endpoint) |

### Production Pitfalls

| Pitfall | Solution |
|---|---|
| **Sticky sessions with ASG** | Externalize sessions (Redis/DynamoDB). Sticky sessions defeat auto-scaling |
| **Instance refresh during peak** | Set `MinHealthyPercentage=100` with enough capacity headroom, or schedule refresh in off-hours |
| **ALB cross-zone load balancing off** | Enable cross-zone for even distribution; default on for ALB, off for NLB |
| **Route53 TTL too high** | Set to 60s during maintenance windows for fast recovery |
| **Health check grace period too short** | App starts slow → health check fails → instance replaced in loop. Set grace period = startup time + buffer |
| **Canary deployment without monitoring** | Wire CloudWatch alarms → CodeDeploy rollback trigger. No manual canary monitoring |

---

## 13. Hands-On Lab (Free Tier)

### Goal: Multi-AZ ASG with ELB health checks + lifecycle hooks + target tracking scaling

**Step 1 — Create Launch Template**

Console: EC2 → Launch Templates → Create launch template
- AMI: Amazon Linux 2023
- Instance type: t3.micro
- Security group: web-sg (allow 80 from ALB SG)
- User data:

```bash
#!/bin/bash
yum update -y
yum install -y httpd
systemctl start httpd
systemctl enable httpd
INSTANCE_ID=$(curl -s http://169.254.169.254/latest/meta-data/instance-id)
AZ=$(curl -s http://169.254.169.254/latest/meta-data/placement/availability-zone)
echo "<h1>Instance: $INSTANCE_ID in $AZ</h1>" > /var/www/html/index.html
echo "OK" > /var/www/html/health
```

**Step 2 — Create ALB + Target Group**

```bash
# Create target group
aws elbv2 create-target-group \
  --name my-web-tg \
  --protocol HTTP \
  --port 80 \
  --vpc-id vpc-XXXXXXXX \
  --health-check-path /health \
  --health-check-interval-seconds 15 \
  --healthy-threshold-count 2 \
  --unhealthy-threshold-count 2

# Create ALB
aws elbv2 create-load-balancer \
  --name my-web-alb \
  --subnets subnet-AZ1 subnet-AZ2 subnet-AZ3 \
  --security-groups alb-sg

# Create listener
aws elbv2 create-listener \
  --load-balancer-arn ALB-ARN \
  --protocol HTTP \
  --port 80 \
  --default-actions Type=forward,TargetGroupArn=TG-ARN
```

**Step 3 — Create ASG with ELB Health Check**

```bash
aws autoscaling create-auto-scaling-group \
  --auto-scaling-group-name my-web-asg \
  --launch-template LaunchTemplateId=lt-XXXXXX,Version='$Latest' \
  --min-size 1 \
  --max-size 6 \
  --desired-capacity 3 \
  --vpc-zone-identifier "subnet-AZ1,subnet-AZ2,subnet-AZ3" \
  --target-group-arns TG-ARN \
  --health-check-type ELB \
  --health-check-grace-period 120
```

**Step 4 — Add Target Tracking Policy**

```bash
aws autoscaling put-scaling-policy \
  --auto-scaling-group-name my-web-asg \
  --policy-name cpu-target-tracking \
  --policy-type TargetTrackingScaling \
  --target-tracking-configuration '{
    "PredefinedMetricSpecification": {
      "PredefinedMetricType": "ASGAverageCPUUtilization"
    },
    "TargetValue": 50.0,
    "ScaleInCooldown": 300,
    "ScaleOutCooldown": 60
  }'
```

**Step 5 — Add Lifecycle Hook**

```bash
# Launch hook — wait for instance to register with service discovery
aws autoscaling put-lifecycle-hook \
  --lifecycle-hook-name launch-hook \
  --auto-scaling-group-name my-web-asg \
  --lifecycle-transition autoscaling:EC2_INSTANCE_LAUNCHING \
  --heartbeat-timeout 120 \
  --default-result CONTINUE

# Terminate hook — drain connections
aws autoscaling put-lifecycle-hook \
  --lifecycle-hook-name terminate-hook \
  --auto-scaling-group-name my-web-asg \
  --lifecycle-transition autoscaling:EC2_INSTANCE_TERMINATING \
  --heartbeat-timeout 60 \
  --default-result CONTINUE
```

**Step 6 — Set ALB Deregistration Delay**

```bash
aws elbv2 modify-target-group-attributes \
  --target-group-arn TG-ARN \
  --attributes Key=deregistration_delay.timeout_seconds,Value=30
```

**Step 7 — Test Scaling**

```bash
# Generate CPU load to trigger scale-out
# SSH to one of the EC2 instances and run:
stress --cpu 4 --timeout 300

# Watch ASG activity
aws autoscaling describe-scaling-activities \
  --auto-scaling-group-name my-web-asg \
  --query 'Activities[0:5].{Status:StatusCode,Cause:Cause,Start:StartTime}'
```

**Step 8 — Instance Refresh Test**

```bash
# Modify Launch Template (bump version — simulate AMI update)
# Then trigger refresh:
aws autoscaling start-instance-refresh \
  --auto-scaling-group-name my-web-asg \
  --preferences '{
    "MinHealthyPercentage": 90,
    "InstanceWarmup": 120
  }'

# Monitor
aws autoscaling describe-instance-refreshes \
  --auto-scaling-group-name my-web-asg
```

**Cleanup:**

```bash
aws autoscaling delete-auto-scaling-group \
  --auto-scaling-group-name my-web-asg \
  --force-delete

aws elbv2 delete-load-balancer --load-balancer-arn ALB-ARN
aws elbv2 delete-target-group --target-group-arn TG-ARN
aws ec2 delete-launch-template --launch-template-id lt-XXXXXX
```
