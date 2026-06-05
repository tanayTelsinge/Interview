# AWS Lambda — Serverless Compute

## The Problem
Not all workloads need a server running 24/7. Consider:
- An image resize function triggered when a user uploads a photo
- A nightly report generation job
- An API that gets 3 requests per day most of the time, then 50,000 on launch day
- A webhook handler that processes events from Stripe or GitHub

For these workloads, maintaining EC2 instances means:
- Paying for idle compute (server running waiting for events)
- Managing OS patches, scaling configuration, health checks
- Over-provisioning for peak load while wasting money at base load
- Complex auto-scaling that still has minimum capacity costs

---

## What AWS Built
Lambda runs your code in response to events — **you upload the function, AWS runs it**. You pay only for the milliseconds your code executes. No server management, no idle cost, automatic scaling from 0 to thousands of concurrent executions.

---

## How It Works

### Execution Environment
When Lambda receives an event:
1. AWS provisions an execution environment (micro-VM using Firecracker)
2. Loads your function code (from zip or container image)
3. Runs the **init phase**: loads runtime, executes initialization code (outside handler)
4. Runs the **handler** with the event payload
5. Environment stays warm for a period — reused for subsequent invocations

```
┌────────────────────────────────────────┐
│     Lambda Execution Environment       │
│  ┌──────────────────────────────────┐  │
│  │  Init phase (cold start)         │  │
│  │  - Load runtime (Node, Python..) │  │
│  │  - Execute init code             │  │
│  │  - Connect to DB, load config    │  │
│  └──────────────────────────────────┘  │
│  ┌──────────────────────────────────┐  │
│  │  Handler invocation              │  │
│  │  - Receive event                 │  │
│  │  - Execute handler function      │  │
│  │  - Return response               │  │
│  └──────────────────────────────────┘  │
└────────────────────────────────────────┘
         Reused for ~5-15 min (warm)
```

### Cold Start vs Warm Start
- **Cold start:** First invocation (or after idle period). Environment provisioned from scratch. Adds 100ms–1s latency depending on runtime and package size.
- **Warm start:** Subsequent invocations while environment is active. Near-instant.

**Cold start by runtime (approximate):**
```
Go, Rust         → ~1-50ms (fastest)
Node.js, Python  → ~100-300ms
Java, C# (.NET)  → ~500ms-2s (JVM/CLR initialization)
```

**Optimization:**
- Keep init code minimal (connect to DB lazily or in init, not in handler)
- Use smaller deployment packages
- Use Provisioned Concurrency to eliminate cold starts

---

## Invocation Models

### Synchronous (Request-Response)
Caller waits for Lambda to complete and return response.
```
API Gateway → Lambda → returns response to API Gateway → to client
ALB → Lambda → returns response
CLI: aws lambda invoke --function-name myFn --payload '{}' response.json
```

### Asynchronous (Fire and Forget)
Caller sends event, Lambda processes it independently. Retries twice on failure.
```
S3 (object created) → Lambda (async)
SNS → Lambda (async)
EventBridge → Lambda (async)
```
- **Retry behavior:** On failure, retries 2 times (total 3 attempts) with 1-min then 2-min delays
- **Destinations:** Configure success/failure routing to SQS, SNS, EventBridge, or another Lambda

### Poll-Based (Event Source Mapping)
Lambda polls a stream/queue and processes batches.
```
SQS → Lambda (polls queue, processes batch of up to 10,000 messages)
Kinesis → Lambda (polls stream, processes batch per shard)
DynamoDB Streams → Lambda (polls stream)
MSK/Kafka → Lambda (polls topic)
```

---

## Triggers (Event Sources)

| Source | Model | Notes |
|---|---|---|
| API Gateway / ALB | Synchronous | HTTP requests → Lambda responses |
| S3 | Asynchronous | Object events (created, deleted) |
| DynamoDB Streams | Poll-based | Item-level changes |
| SQS | Poll-based | Queue messages, batch processing |
| SNS | Asynchronous | Topic notifications |
| EventBridge | Asynchronous | Scheduled + event-driven |
| Kinesis | Poll-based | Real-time stream processing |
| Cognito | Synchronous | Auth flow customization (pre/post triggers) |
| CloudFront | Synchronous | Lambda@Edge for request/response manipulation |
| IoT | Asynchronous | IoT rule actions |

---

## Concurrency

### Reserved Concurrency
- **Guarantees** a specific number of concurrent executions for a function
- **Also limits** the function to that maximum (throttle above it)
- Use: protect downstream resources (limit DB connections from Lambda)
```bash
aws lambda put-function-concurrency \
  --function-name myFunction \
  --reserved-concurrent-executions 100
```

### Provisioned Concurrency
- Pre-warm N execution environments
- **Eliminates cold starts** for those N instances
- Billed for provisioned time + invocations
- Use: latency-sensitive APIs, after burst warmup

```bash
aws lambda put-provisioned-concurrency-config \
  --function-name myFunction \
  --qualifier myAlias \
  --provisioned-concurrent-executions 10
```

### Burst Concurrency Limits
- Account-level: 1,000 concurrent executions default (request increase)
- Burst limit: 500-3,000 per region (initial burst)
- Beyond burst: scales by +500 executions per minute

---

## Lambda Limits

| Parameter | Limit |
|---|---|
| Max execution timeout | 15 minutes |
| Max memory | 10,240 MB (10 GB) |
| Min memory | 128 MB |
| /tmp storage | 10,240 MB (10 GB) |
| Deployment package (zip, direct) | 50 MB zipped / 250 MB unzipped |
| Container image size | 10 GB |
| Environment variables | 4 KB total |
| Concurrent executions (default) | 1,000 per account per region |
| Layers per function | 5 |
| Max layers total size | 250 MB |
| Invocation payload (sync) | 6 MB request + 6 MB response |
| Invocation payload (async) | 256 KB |

---

## Lambda Layers
Reusable packages shared across functions. Separate from function code.
```
Use cases:
  - Shared libraries (boto3 upgrade, NumPy, pandas)
  - Common utilities (logging, auth helpers)
  - Lambda Insights extension layer

Layer storage:
  - Up to 5 layers per function
  - Total unzipped size ≤ 250 MB (including function code)
  - Layers are versioned and immutable
```

---

## Lambda@Edge vs CloudFront Functions

| | Lambda@Edge | CloudFront Functions |
|---|---|---|
| **Location** | Runs at regional edge caches | Runs at 400+ PoPs (true edge) |
| **Triggers** | Viewer Request/Response, Origin Request/Response | Viewer Request/Response only |
| **Latency** | ~1ms added (regional) | Sub-millisecond |
| **Runtime** | Node.js, Python | JavaScript (ES5.1) |
| **Max execution time** | 5s (viewer) / 30s (origin) | 1ms |
| **Memory** | 128MB–10GB | 2 MB |
| **Cost** | Higher | ~1/6th of Lambda@Edge |
| **Use case** | Complex logic, auth, A/B testing with origin calls | Simple rewrites, redirects, header manipulation |

---

## Lambda Destinations
Route async invocation results to a target:
```
On Success → SQS / SNS / EventBridge / Another Lambda
On Failure → SQS / SNS / EventBridge / Another Lambda (better than DLQ)

DLQ (Dead Letter Queue) → only for failures, less info than Destinations
```

---

## Lambda in a VPC
By default, Lambda runs outside your VPC (in AWS-managed network). To access private VPC resources (RDS in private subnet, ElastiCache):

1. Configure Lambda with VPC, subnets, and security group
2. Lambda creates an **ENI (Elastic Network Interface)** in your subnets
3. Lambda can now reach private VPC resources

```
Important: VPC Lambda has NO internet access by default
To call external APIs or AWS services from VPC Lambda:
  → Add NAT Gateway in public subnet + route table entry
  → OR use VPC Interface Endpoints for AWS services
```

**ENI scaling:** Multiple concurrent Lambda invocations share ENIs (AWS manages the ENI pool). ENI creation was previously slow (cold start issue) — now managed by Hyperplane ENI for fast warm-up.

---

## Decision Tree: Lambda vs Other Compute

```
How long does the task run?
│
├── < 15 minutes?
│   └── Is it event-driven or invoked on demand?
│       ├── YES → Lambda (perfect fit)
│       └── NO (always-on service) → consider ECS/EC2
│
└── > 15 minutes?
    ├── Containerized? → ECS Fargate or EC2
    └── Batch job? → AWS Batch or Step Functions + Lambda chain

What is the concurrency pattern?
│
├── Sudden spikes to thousands? → Lambda (instant scale)
├── Steady-state load? → EC2/Fargate (more cost-efficient)
└── GPU workloads? → EC2 (Lambda has no GPU support)

What is the latency requirement?
│
├── Sub-10ms response time? → Lambda with Provisioned Concurrency
├── <100ms acceptable? → Lambda (warm starts)
└── Cold starts unacceptable for all traffic? → Fargate or Provisioned Concurrency
```

---

## Common Patterns

### API Backend (Serverless Web App)
```
CloudFront → API Gateway → Lambda → DynamoDB
                           ↓ (async tasks)
                           SQS → Lambda workers
```

### Event Processing Pipeline
```
S3 (file uploaded) → Lambda (transform) → DynamoDB (write result)
                   → SNS (notify) → Lambda (thumbnail) + SQS (audit)
```

### Scheduled Job
```
EventBridge Rule (cron: 0 9 * * ? *) → Lambda (daily report)
```

### Fan-Out Processing
```
SNS → Lambda (email notification)
    → Lambda (push notification)
    → Lambda (analytics event)
```

### SQS-Based Worker with DLQ
```
App → SQS Queue (max receive 3) → Lambda
                                    ↓ on 3 failures
                               DLQ → AlertLambda → SNS (page on-call)
```

---

## Gotchas

1. **Lambda in VPC needs NAT for internet:** Private subnet Lambda → NAT Gateway → internet. Without NAT, no internet access. This adds cost (~$0.045/hr + data processing).

2. **SQS visibility timeout must be > Lambda timeout:** If Lambda takes 10s and visibility timeout is 5s, SQS will re-deliver the message to another Lambda while the first is still processing — double processing.

3. **Cold starts in Java/C# are severe:** JVM startup can add 1-3s. Use:
   - GraalVM native compilation (reduces cold start dramatically)
   - Provisioned Concurrency for critical paths
   - SnapStart (Lambda SnapStart for Java — snapshots initialized execution environment)

4. **Async retry = 3 attempts total:** Lambda retries async invocations 2 times. If your function has side effects (sends emails, charges cards), ensure **idempotency** — processing the same event twice should not create duplicate effects.

5. **Memory ↑ = CPU ↑ = cost optimization possible:** Lambda allocates CPU proportional to memory. A function running 5s at 128MB might run in 1s at 1024MB — and cost LESS overall (1s × 1024MB < 5s × 128MB). Use Lambda Power Tuning.

6. **/tmp is shared across warm invocations:** Files written to /tmp persist across warm invocations of the same environment. Potential data leakage if you store sensitive data there.

7. **10GB container image but not for fast starts:** Large container images increase cold start time significantly. Keep images lean.

---

## Hands-On Lab (Free Tier)

**Goal:** Create Lambda function, trigger from S3, test with SQS.

```python
# Function code (Python 3.12)
import json
import boto3

def lambda_handler(event, context):
    # Log the event
    print(json.dumps(event))

    # If S3 trigger
    if 'Records' in event:
        for record in event['Records']:
            if record.get('eventSource') == 'aws:s3':
                bucket = record['s3']['bucket']['name']
                key = record['s3']['object']['key']
                print(f"New file: s3://{bucket}/{key}")

    return {
        'statusCode': 200,
        'body': json.dumps('Hello from Lambda!')
    }
```

```bash
# 1. Create Lambda function (free tier: 1M requests/month)
aws lambda create-function \
  --function-name MyFirstLambda \
  --runtime python3.12 \
  --role arn:aws:iam::ACCOUNT_ID:role/lambda-basic-role \
  --handler lambda_function.lambda_handler \
  --zip-file fileb://function.zip

# 2. Test invoke
aws lambda invoke \
  --function-name MyFirstLambda \
  --payload '{"key": "value"}' \
  --cli-binary-format raw-in-base64-out \
  response.json
cat response.json

# 3. Add S3 trigger (via Console is easier)
# Console → Lambda → Add trigger → S3 → select bucket → All object create events

# 4. Upload a file to the bucket and check CloudWatch Logs
aws s3 cp test.txt s3://your-bucket/test.txt
# Console → CloudWatch → Log groups → /aws/lambda/MyFirstLambda → check latest stream

# 5. Check cold start in logs
# Look for INIT_START entry in CloudWatch Logs — that's your cold start

# 6. Test concurrency limit
# Console → Lambda → Configuration → Concurrency → Reserve 5 concurrent executions
# Invoke 10 times simultaneously — some will throttle (429)
```
