# Amazon CloudWatch — Observability

## 1. The Problem

Without visibility, cloud infrastructure is a black box:

- An EC2 instance shows "running" in the console but the application is out of memory — CloudWatch doesn't collect memory by default
- A Lambda function fails 5% of the time but no one knows until a customer reports it
- A security team needs to detect who changed a production security group — but they need an audit trail, not a graph
- A developer needs to trace a slow API call through 6 microservices to find the bottleneck

Observability requires three pillars: **Metrics** (what's the state?), **Logs** (what happened?), and **Traces** (why did it happen?). CloudWatch provides the first two, plus glue services that connect all three.

---

## 2. What AWS Built

CloudWatch is AWS's native observability platform — a collection of related services sharing a brand name:

| Component | Purpose |
|-----------|---------|
| **CloudWatch Metrics** | Time-series numeric data from AWS services and custom apps |
| **CloudWatch Alarms** | Alert and automate based on metric thresholds |
| **CloudWatch Logs** | Collect, store, search, and analyze log data |
| **CloudWatch Dashboards** | Visualize metrics and logs |
| **CloudWatch Agent** | Collect OS-level metrics and logs from EC2/on-prem |
| **Container Insights** | ECS/EKS task/service/cluster metrics |
| **Lambda Insights** | Enhanced Lambda performance metrics |
| **Synthetics** | Canary scripts for endpoint monitoring |
| **RUM** | Browser-side performance data |
| **Evidently** | Feature flags and A/B testing |
| **ServiceLens** | Unified X-Ray + Logs + Metrics view |

---

## 3. How It Works

### CloudWatch Metrics

**Namespace → Metric → Dimensions:**
```
Namespace: AWS/EC2
  Metric: CPUUtilization
    Dimensions:
      InstanceId = i-1234567890abcdef0  → specific instance
      AutoScalingGroupName = my-asg     → ASG-level rollup

Namespace: AWS/RDS
  Metric: DatabaseConnections
    Dimensions: DBInstanceIdentifier = prod-mysql

Namespace: Custom/MyApp
  Metric: OrdersProcessed
    Dimensions: Environment = prod, Region = us-east-1
```

**Default EC2 Metrics (free, 5-minute resolution):**
```
CPUUtilization        ✓ (default)
NetworkIn             ✓ (default)
NetworkOut            ✓ (default)
DiskReadOps           ✓ (default, instance store only)
DiskWriteOps          ✓ (default, instance store only)
StatusCheckFailed     ✓ (default)

NOT collected by default (need CloudWatch Agent):
  MemoryUtilization   ✗ OS-level metric
  DiskSpaceUtilization ✗ OS-level metric
  Swap usage          ✗ OS-level metric
  Custom app metrics  ✗ application-level
```

**Standard vs High-Resolution Metrics:**
```
Standard:      1-minute granularity (EC2 detailed monitoring enabled)
               5-minute granularity (EC2 basic monitoring, free)
High-Resolution: 1-second granularity (custom metrics only)

Cost: High-resolution custom metrics cost more per metric per month
      Standard: $0.30/metric/month
      High-resolution: $0.30/metric/month + more storage for 1s data

Retention:
  1-second resolution:  3 hours
  60-second resolution: 15 days
  5-minute resolution:  63 days
  1-hour resolution:    15 months
```

**PutMetricData API (custom metrics):**
```python
import boto3
cloudwatch = boto3.client('cloudwatch')

cloudwatch.put_metric_data(
    Namespace='MyApp/Orders',
    MetricData=[{
        'MetricName': 'OrderProcessingTime',
        'Dimensions': [
            {'Name': 'Environment', 'Value': 'prod'},
            {'Name': 'Region', 'Value': 'us-east-1'}
        ],
        'Value': 245.3,   # milliseconds
        'Unit': 'Milliseconds'
    }]
)
```

**Metric Math:**
```
Expression: (Errors / Requests) * 100
Gives: Error rate percentage — not stored as a metric, computed at query time

Use cases:
  ErrorRate = (m1 / m2) * 100     # errors divided by total requests
  AvgLatency = m1 / m2             # total latency / request count
  p99 + p50 combined chart        # overlay percentiles
```

---

### CloudWatch Alarms

**Alarm States:**
```
OK:                  Metric is within threshold
ALARM:               Metric breached threshold
INSUFFICIENT_DATA:   Not enough data to evaluate (new metric, gaps)
```

**Alarm Configuration:**
```
Period:            Evaluation window (must be >= metric resolution)
Evaluation Periods: How many consecutive periods to check
Datapoints to Alarm: How many periods must breach threshold
                     (e.g., 3 out of 5 periods → reduces noise)
```

**Alarm Actions:**
```
SNS notification      → email, SMS, Lambda, HTTP endpoint
EC2 action            → Stop, Terminate, Reboot, Recover
Auto Scaling action   → Scale out / scale in
SSM OpsCenter         → Create OpsItem
Systems Manager       → Start SSM Automation
```

**Composite Alarms:**
```json
{
  "AlarmRule": "ALARM(HighCPU) AND ALARM(HighLatency)",
  "AlarmName": "ServiceDegraded-Composite"
}

// Reduce alert noise: only alarm when BOTH CPU is high AND latency is high
// Use: AND / OR logical operators across multiple alarms
```

**Anomaly Detection:**
```
CloudWatch trains ML model on metric history
Creates "expected band" (upper/lower bounds based on time-of-day, day-of-week patterns)
Alarm when metric goes outside expected band

Use for: metrics with natural seasonality (web traffic, DB connections)
vs static threshold which would false-alarm at predictable peaks
```

---

### CloudWatch Logs

**Structure:**
```
Log Group: /aws/lambda/my-function
  Log Stream: 2024/01/15/[$LATEST]abc123
    Log Events:
      timestamp: 1705334400000
      message: "START RequestId: xxx"
      timestamp: 1705334400100
      message: "Processing order: 12345"
      timestamp: 1705334400200
      message: "END RequestId: xxx"
```

**Retention Settings:**
```
Default: NEVER EXPIRE (costs money indefinitely!)
Configurable: 1 day, 3 days, 5 days, ... 1 week, ... 1 month, ... 5 years, 10 years

Best practice: set retention for every log group
  Production: 90 days (compliance) or 1 year (PCI, HIPAA)
  Dev/test: 7-30 days
  Lambda: 14 days

Automate: use EventBridge to trigger Lambda when new log group created → set retention
```

**Metric Filters:**
```
Extract metrics from log events — count errors, extract latency values

Example: count HTTP 500 errors from application logs
Filter pattern: [ip, user, timestamp, request, statusCode=5*, size]
              or: { $.statusCode = 500 }  (JSON logs)

Creates metric: MyApp/HTTP500Count
Then create alarm on that metric

Important: metric filters do NOT backfill historical data
           Only counts matching events from when filter was created
```

**Subscription Filters (real-time streaming):**
```
Log Group → Subscription Filter → Destination

Destinations:
  Lambda              → process/transform logs in real-time
  Kinesis Data Streams → aggregate across accounts
  Kinesis Firehose    → deliver to S3/OpenSearch
  OpenSearch Service  → real-time log search

Cross-account log aggregation:
  All accounts → Kinesis Data Streams in central account
  → Lambda → central S3 bucket or OpenSearch
```

**CloudWatch Logs Insights:**
```sql
-- Query language for log analysis
-- Cost: $0.005 per GB scanned

-- Top 10 slowest Lambda invocations:
fields @timestamp, @duration, @requestId
| filter @type = "REPORT"
| sort @duration desc
| limit 10

-- Error rate by hour:
filter @message like /ERROR/
| stats count(*) as error_count by bin(1h)

-- Parse custom log format:
parse @message "* * * [*] \"* HTTP/1.1\" * *" as ip, user, timestamp, request, status, size
| stats count(*) by status

-- Lambda cold starts:
filter @message like /Init Duration/
| parse @message "Init Duration: * ms" as init_duration
| stats avg(init_duration) as avg_cold_start by bin(5m)
```

---

### CloudWatch Agent

**What it collects (not available by default):**
```
OS Metrics:
  - Memory utilization (%)
  - Disk space utilization (%)
  - Swap utilization (%)
  - TCP connections
  - Process count

Custom application logs:
  - Any log file on the OS
  - Windows Event Log
  - Statsd / CollectD metrics

```

**Agent Configuration:**
```json
{
  "metrics": {
    "metrics_collected": {
      "mem": {
        "measurement": ["mem_used_percent"],
        "metrics_collection_interval": 60
      },
      "disk": {
        "measurement": ["used_percent"],
        "resources": ["/", "/data"],
        "metrics_collection_interval": 60
      }
    },
    "append_dimensions": {
      "InstanceId": "${aws:InstanceId}",
      "Environment": "prod"
    }
  },
  "logs": {
    "logs_collected": {
      "files": {
        "collect_list": [{
          "file_path": "/var/log/app/application.log",
          "log_group_name": "/myapp/application",
          "log_stream_name": "{instance_id}",
          "retention_in_days": 30
        }]
      }
    }
  }
}
```

**Install via SSM (fleet-wide):**
```bash
# SSM Run Command to install and configure agent on all instances:
aws ssm send-command \
  --document-name "AWS-ConfigureAWSPackage" \
  --parameters '{"action":["Install"],"name":["AmazonCloudWatchAgent"]}' \
  --targets '[{"Key":"tag:Environment","Values":["prod"]}]'
```

---

### Observability Products at a Glance

**Container Insights:**
```
ECS: task-level CPU, memory, network, disk
EKS: cluster, node, pod, container level metrics + Kubernetes events
     Requires: CloudWatch agent as DaemonSet on EKS nodes

Provides: pre-built dashboards for container environments
```

**Lambda Insights:**
```
Enhanced metrics beyond default Lambda metrics:
  - Memory used (not just configured)
  - CPU time
  - Network I/O
  - Cold start rate
  - Init duration trend

Enable per function: add Lambda layer + execution role permission
```

**CloudWatch Synthetics (Canaries):**
```
Purpose: proactive endpoint monitoring (before users report issues)
How: scheduled Puppeteer/Selenium scripts run from CloudWatch
  - Check: does the login page load?
  - Check: does the checkout flow complete?
  - Check: does the API return 200 with correct schema?

Runs every: 1 minute, 5 minutes, 1 hour, etc.
Alerts on: non-200 response, page load > threshold, element not found
```

**RUM (Real User Monitoring):**
```
JavaScript snippet embedded in web app
Captures real browser performance data:
  - Page load time
  - Core Web Vitals (LCP, FID, CLS)
  - JavaScript errors
  - API call latency from browser perspective

vs Synthetics: RUM = real users, Synthetics = simulated probes
```

**Evidently (Feature Flags + Experiments):**
```
Feature flags: enable/disable features without deployment
A/B testing: route % of traffic to variant B, measure impact on metrics

Use case:
  - Launch new checkout flow to 10% of users
  - Measure: conversion rate, error rate, latency
  - If improvement → increase to 50% → 100%
  - If regression → roll back immediately
```

**ServiceLens:**
```
Unified view: CloudWatch metrics + logs + X-Ray traces
For a given API endpoint:
  - Request rate, error rate, latency (CloudWatch metrics)
  - Recent error logs (CloudWatch Logs)
  - Trace map showing which services are slow (X-Ray)

Requires: X-Ray tracing enabled on services
```

---

## 4. Key Config & Limits

| Parameter | Value |
|-----------|-------|
| EC2 basic monitoring | 5-minute intervals, free |
| EC2 detailed monitoring | 1-minute intervals, extra cost |
| Custom metric — high resolution | 1-second granularity |
| Metric retention (1s) | 3 hours |
| Metric retention (1min) | 15 days |
| Metric retention (1hr) | 15 months |
| Log retention default | Never expire (set it!) |
| CloudWatch Agent — memory metric | Not default, requires agent |
| Alarm evaluation minimum period | 10 seconds (high-res), 60 seconds (standard) |
| Logs Insights cost | $0.005/GB scanned |
| CloudWatch Logs — export to S3 | Manual or scheduled (not real-time) |
| Subscription filter — max per log group | 2 |
| Custom metrics cost | $0.30/metric/month (first 10,000) |

---

## 5. Decision Tree

```
WHAT HAPPENED? (audit, API call history)
└─ CloudTrail (not CloudWatch)

WHY IS IT SLOW? (trace through microservices)
└─ AWS X-Ray / ServiceLens

WHAT IS THE CURRENT STATE? (numeric metric, threshold alert)
└─ CloudWatch Metrics + Alarms

WHAT DID THE APPLICATION LOG? (text logs, error messages)
└─ CloudWatch Logs

IS MY ENDPOINT UP? (proactive health check)
└─ CloudWatch Synthetics

WHAT IS THE REAL USER EXPERIENCE? (browser performance)
└─ CloudWatch RUM

IS THIS METRIC BEHAVING NORMALLY? (seasonality-aware)
└─ CloudWatch Anomaly Detection

CloudWatch vs CloudTrail vs X-Ray:
  CloudWatch: metrics + logs → "what is the state NOW"
  CloudTrail: API audit trail → "who did what and when"
  X-Ray:      distributed traces → "which service is causing this latency"
```

---

## 6. Common Patterns

### Pattern 1: EC2 Full Observability Stack
```
1. Enable Detailed Monitoring (1-min metrics)
2. Install CloudWatch Agent → collects memory, disk, app logs
3. Create Alarms:
   - CPUUtilization > 80% for 5 minutes → SNS
   - mem_used_percent > 85% → SNS
   - disk_used_percent (/) > 90% → SNS
4. Log group: /ec2/application.log, retention: 30 days
5. Metric filter on log group: count ERROR messages
6. Dashboard: CPU + Memory + Disk + Error Rate combined
```

### Pattern 2: Lambda Error Rate Monitoring
```
Default Lambda metrics:
  Errors, Duration, Throttles, ConcurrentExecutions, Invocations

Alarm: Error rate > 1%
  Metric Math: (Errors / Invocations) * 100 > 1
  Action: SNS → PagerDuty

Alarm: Throttles > 0
  Action: Ticket creation

Log Insights query (run when debugging):
  filter @type = "REPORT"
  | stats avg(@duration) as avg_ms, max(@duration) as max_ms,
          percentile(@duration, 99) as p99 by bin(5m)
```

### Pattern 3: Cross-Account Log Aggregation
```
(Each member account)
  CloudWatch Logs → Subscription Filter → Kinesis Data Streams (security account)

(Security/Central account)
  Kinesis Data Streams → Lambda → S3 (central log archive)
                       → OpenSearch (real-time SIEM)

Benefit: logs from 50 accounts in one place, immutable S3 archive
```

### Pattern 4: Auto-Set Log Retention (Automation)
```python
# Lambda triggered by EventBridge when new log group created:
import boto3

logs = boto3.client('logs')

def lambda_handler(event, context):
    log_group_name = event['detail']['requestParameters']['logGroupName']

    # Set 90-day retention for all new log groups
    logs.put_retention_policy(
        logGroupName=log_group_name,
        retentionInDays=90
    )
    print(f"Set 90-day retention for {log_group_name}")
```

---

## 7. Gotchas

| Gotcha | Detail |
|--------|--------|
| **EC2 memory not default** | CloudWatch doesn't know about OS memory — it can't see inside the instance hypervisor. Install CloudWatch Agent to collect `mem_used_percent`. |
| **Log retention defaults to "never expire"** | Every log group ever created retains logs indefinitely unless you set a retention policy. Automate with EventBridge rule on log group creation. |
| **High-resolution metrics cost more** | 1-second custom metrics use more storage than 1-minute. Only use high-resolution for metrics where second-level granularity matters. |
| **Metric filters don't backfill** | If you create a metric filter today, it counts matching log lines from today forward. It does not count historical log events. |
| **Logs Insights charges per GB scanned** | Don't run broad queries over 90-day date ranges in production. Narrow time range + use log group filtering to reduce cost. |
| **Two subscription filters max per log group** | You can only have 2 subscription filters per log group. Choose destinations carefully. |
| **Alarm INSUFFICIENT_DATA can be misleading** | A new alarm in INSUFFICIENT_DATA is not "OK" — it means no data yet. Some automation triggers on INSUFFICIENT_DATA by mistake. |
| **CloudWatch Agent must be configured AND started** | Installing the agent is not enough. You must run `amazon-cloudwatch-agent-ctl -a start` after providing the config file. |
| **Composite alarms don't support SNS directly** | Composite alarms can trigger other alarms' actions but don't have their own SNS action. The child alarm actions fire. |

---

## 8. Hands-On Lab — Free Tier

**Goal:** Set up full EC2 observability with CloudWatch Agent and create alarms.

### Step 1: Launch EC2 (Free Tier)
```
EC2 → Launch instance
  AMI: Amazon Linux 2023
  Type: t2.micro
  IAM Role: Create new role with policies:
    - CloudWatchAgentServerPolicy
    - AmazonSSMManagedInstanceCore
  Enable detailed monitoring: Yes (1-min metrics)
```

### Step 2: Install CloudWatch Agent via SSM
```
SSM Console → Run Command → AWS-ConfigureAWSPackage
  Action: Install
  Name: AmazonCloudWatchAgent
  Target: your EC2 instance

Or via EC2 console → Connect → Session Manager:
  sudo yum install -y amazon-cloudwatch-agent
```

### Step 3: Create Agent Configuration
```bash
# On the EC2 instance (via Session Manager):
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-config-wizard

# Or manually create config:
sudo cat > /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json << 'EOF'
{
  "metrics": {
    "metrics_collected": {
      "mem": {"measurement": ["mem_used_percent"], "metrics_collection_interval": 60},
      "disk": {"measurement": ["used_percent"], "resources": ["/"], "metrics_collection_interval": 60}
    }
  },
  "logs": {
    "logs_collected": {
      "files": {
        "collect_list": [{
          "file_path": "/var/log/messages",
          "log_group_name": "/lab/ec2-messages",
          "log_stream_name": "{instance_id}",
          "retention_in_days": 7
        }]
      }
    }
  }
}
EOF

# Start agent:
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
  -a fetch-config \
  -m ec2 \
  -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json \
  -s
```

### Step 4: Verify Metrics in Console
```
CloudWatch → Metrics → CWAgent → InstanceId
  → Should see: mem_used_percent, disk_used_percent

Wait 2-3 minutes for first data points
```

### Step 5: Create Memory Alarm
```
CloudWatch → Alarms → Create alarm
  Metric: CWAgent → InstanceId → mem_used_percent
  Threshold: Greater than 80
  Period: 1 minute
  Evaluation periods: 3 (alarm after 3 consecutive minutes above 80%)
  Action: Send notification to new SNS topic → email address
  Alarm name: lab-high-memory

→ Confirm SNS email subscription
```

### Step 6: Create a Dashboard
```
CloudWatch → Dashboards → Create dashboard: "lab-ec2"
  + Widget: Line chart
    Metrics:
      - AWS/EC2 → CPUUtilization (your instance)
      - CWAgent → mem_used_percent (your instance)
      - CWAgent → disk_used_percent (your instance)
  + Widget: Number (current memory %)
  + Widget: Logs table (recent /var/log/messages)
```

### Step 7: Query Logs
```
CloudWatch → Logs Insights
  Log group: /lab/ec2-messages
  Time: Last 30 minutes
  Query:
    fields @timestamp, @message
    | filter @message like /error/ or @message like /warning/
    | sort @timestamp desc
    | limit 50
```

### Step 8: Clean Up
```
# Delete dashboard, alarms, log groups
# Stop and terminate EC2 instance
# (Detailed monitoring stops automatically when instance is terminated)
```

---

## Summary Reference Card

```
CLOUDWATCH COMPONENTS:
  Metrics:    Time-series numbers. EC2 default: CPU/Network/Disk (NOT memory/disk%)
              High-res: 1s. Standard: 1min. Custom: PutMetricData API.
  Alarms:     States: OK/ALARM/INSUFFICIENT_DATA
              Actions: SNS, EC2 action, ASG scaling, SSM
              Composite alarms: AND/OR logic across multiple alarms
  Logs:       Log groups → streams → events
              Retention: NEVER EXPIRE by default — always set it
              Metric filters: extract metrics from logs (no backfill)
              Subscription filters: stream to Lambda/Kinesis/Firehose (max 2 per group)
              Logs Insights: $0.005/GB scanned
  Agent:      Required for memory %, disk %, swap, custom app logs
  Synthetics: Scheduled canary scripts (proactive endpoint monitoring)

CLOUDWATCH vs CLOUDTRAIL vs X-RAY:
  CloudWatch: metrics + logs = what is happening
  CloudTrail: API audit trail = who did what
  X-Ray:      distributed traces = why is it slow
```
