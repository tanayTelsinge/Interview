# Amazon SQS — SAP-C02 Deep Dive

---

## 1. The Problem

Tightly coupled systems fail together. When Service A calls Service B synchronously, a spike in B's processing time causes A to block, timeout, and cascade failures upstream. Before SQS, engineers resorted to custom message brokers or polling databases — brittle, operationally expensive solutions.

**Symptoms of tight coupling:**
- Downstream slowness causes upstream timeouts
- Deployments require coordinated downtime
- Cannot absorb traffic spikes independently
- Retry logic duplicated in every service

SQS decouples producers from consumers by placing a durable, managed queue between them.

---

## 2. What AWS Built

Amazon SQS is a **fully managed message queuing service** that enables asynchronous, decoupled communication between distributed system components. It stores messages durably until a consumer retrieves and deletes them.

Two queue types:
- **Standard Queue** — unlimited throughput, at-least-once delivery, best-effort ordering
- **FIFO Queue** — exactly-once processing, strict ordering, 300–3,000 msg/s

---

## 3. How It Works

### Message Lifecycle

```
Producer                    SQS Queue                     Consumer
   |                            |                              |
   |--SendMessage-------------->|                              |
   |                            | (message stored, invisible=0)|
   |                            |<----ReceiveMessage-----------|
   |                            | (visibility timeout starts)  |
   |                            |--[message body]------------->|
   |                            |  (timeout clock running)     |
   |                   [consumer processes...]                 |
   |                            |<----DeleteMessage------------|
   |                            | (message permanently removed)|
   |                            |                              |
   |                   [if NOT deleted before timeout]         |
   |                            | (message becomes visible     |
   |                            |  again for reprocessing)     |
```

### Key Internal Concepts

**Visibility Timeout**
- Default: **30 seconds** | Min: 0s | Max: **12 hours**
- While a consumer holds a message, it is invisible to other consumers
- Consumer must call `DeleteMessage` before timeout or message reappears
- Use `ChangeMessageVisibility` to extend mid-processing
- **Critical rule:** Visibility timeout MUST be greater than your Lambda/EC2 processing timeout

**Dead Letter Queue (DLQ)**
- A separate queue that receives messages after `maxReceiveCount` failed delivery attempts
- `maxReceiveCount`: 1–1,000 (how many times a message can be received before DLQ)
- **DLQ must be the same type as the source queue** (Standard→Standard, FIFO→FIFO)
- DLQ does NOT retry automatically — it stores failed messages for inspection/replay
- Use **DLQ Redrive** (console or API) to replay messages back to source queue

**Polling Models**

| Feature | Short Polling | Long Polling |
|---|---|---|
| `WaitTimeSeconds` | 0 (default) | 1–20 seconds |
| Returns | Immediately, even if empty | Waits until message arrives or timeout |
| Empty responses | Yes (cost waste) | Minimized |
| Recommended | No | Yes — always prefer |
| API parameter | `WaitTimeSeconds=0` | `WaitTimeSeconds=20` |

**Message Retention**
- Min: **60 seconds (1 min)** | Default: **4 days** | Max: **14 days**
- After retention period, messages are automatically deleted

### Standard vs FIFO

| Feature | Standard | FIFO |
|---|---|---|
| Throughput | **Unlimited** | **300 msg/s** (no batching) / **3,000 msg/s** (batching of 10) |
| Delivery guarantee | **At-least-once** (duplicates possible) | **Exactly-once** (deduplication) |
| Ordering | **Best-effort** (not guaranteed) | **Strict FIFO per MessageGroupId** |
| Deduplication | No | Content-based (SHA-256) or `MessageDeduplicationId` (5 min window) |
| Message groups | No | `MessageGroupId` (parallel ordering groups) |
| Name format | Any valid name | **Must end in `.fifo`** |
| DLQ support | Yes | Yes (FIFO DLQ) |
| Lambda trigger | Yes | Yes |

### FIFO Deep Dive

```
MessageGroupId = "order-123"   → processed strictly in order within this group
MessageGroupId = "order-456"   → processed strictly in order, in PARALLEL with "order-123"
MessageDeduplicationId = "msg-abc-1"  → duplicate within 5 min window is discarded
```

- Multiple `MessageGroupId` values enable parallelism while preserving per-group order
- Single `MessageGroupId` = fully sequential (throughput bottleneck)
- Deduplication ID is idempotency token: same ID within 5 minutes = message discarded

### Delay Queues

- Delay newly sent messages from 0 to **15 minutes** before becoming visible
- Set at queue level (`DelaySeconds`) or per-message (`DelaySeconds` in `SendMessage`)
- **FIFO queues:** per-message delay not supported, queue-level delay only

### Message Size

- Max: **256 KB**
- For larger payloads: use the **SQS Extended Client Library** (Java) + S3
  - Stores payload in S3, sends S3 pointer in SQS message
  - Consumer downloads from S3 automatically
  - Messages up to **2 GB** supported this way

### Lambda Event Source Mapping

```
SQS Queue ──ESM──> Lambda Function
             |
             ├── BatchSize: 1–10,000 (Standard) / 1–10 (FIFO)
             ├── MaximumBatchingWindow: 0–300 seconds
             ├── BisectBatchOnFunctionError: true (bisects batch to isolate bad message)
             ├── ReportBatchItemFailures: partial batch success
             └── FunctionResponseTypes: ["ReportBatchItemFailures"]
```

**Important ESM behaviors:**
- Lambda polls SQS — not a push model
- Failed batch: all messages return to queue unless `ReportBatchItemFailures` is used
- Concurrency: Lambda scales to (queue depth / BatchSize), capped by account concurrency
- `BisectBatchOnFunctionError`: on Lambda error, batch split in half and retried — helps isolate poison pills

### ASG Scaling on Queue Depth

```
SQS Queue → CloudWatch (ApproximateNumberOfMessagesVisible)
                           ↓
                  CloudWatch Alarm
                           ↓
                   ASG Scaling Policy
                  (Target Tracking on custom metric:
                   messages-per-instance = queue_depth / running_instances)
```

Custom metric formula: `ApproximateNumberOfMessagesVisible / RunningInstances`
Target: e.g., 10 messages per instance

### Fan-Out Pattern: SNS → Multiple SQS

```
Publisher
    |
   SNS Topic
    |──────────────────────────────|
    |                              |
  SQS Queue A                 SQS Queue B
(email-service)             (inventory-service)
```

- SNS delivers to all subscribed queues simultaneously
- Each queue processes independently at its own rate
- Failure in one queue does not affect others
- SQS queues must have a resource-based policy allowing SNS to send messages

### Security

| Feature | Details |
|---|---|
| **SSE-KMS** | Server-side encryption with KMS CMK or AWS Managed Key (aws/sqs) |
| **SSE-SQS** | Managed keys, simpler, free |
| **In-transit** | HTTPS enforced via `aws:SecureTransport` condition |
| **VPC Endpoint** | Interface endpoint — keep traffic off public internet |
| **Resource Policy** | Control who can send/receive (cross-account, SNS, etc.) |

---

## 4. Key Config & Limits

| Parameter | Value |
|---|---|
| Message size | Max **256 KB** (Extended Client: 2 GB via S3) |
| Retention period | 60s – **14 days** (default 4 days) |
| Visibility timeout | 0s – **12 hours** (default 30s) |
| Long polling wait | 0 – **20 seconds** |
| Delay queue | 0 – **15 minutes** |
| FIFO throughput | **300 msg/s** baseline / **3,000 msg/s** with batching |
| Standard throughput | **Unlimited** |
| Batch operations | Up to **10 messages** per batch send/receive/delete |
| maxReceiveCount | 1 – 1,000 |
| In-flight messages | Standard: **120,000** / FIFO: **20,000** |
| Queue name length | Max **80 characters** |
| Message groups (FIFO) | Unlimited |
| Dedup window (FIFO) | **5 minutes** |

---

## 5. Decision Tree

### Standard vs FIFO

```
Do you need strict message ordering?
├── YES → Do you need exactly-once processing?
│         ├── YES → FIFO Queue
│         └── YES (implied with ordering) → FIFO Queue
└── NO  → Is throughput > 3,000 msg/s?
          ├── YES → Standard Queue (FIFO cannot handle it)
          └── NO  → Do you care about duplicates?
                    ├── YES → FIFO Queue
                    └── NO  → Standard Queue
```

### SQS vs Kinesis Data Streams

```
Is your use case:
├── Task queue / job queue / decoupling microservices?
│   └── SQS
├── Real-time streaming / event stream analytics?
│   └── Kinesis Data Streams
├── Multiple independent consumers of same data?
│   ├── SQS: Fan-out requires SNS in front
│   └── Kinesis: Multiple consumers natively (Enhanced Fan-Out)
├── Need to replay messages?
│   ├── SQS: No replay (once deleted, gone)
│   └── Kinesis: Yes — retention window replay
├── Ordering required?
│   ├── SQS: FIFO per MessageGroupId
│   └── Kinesis: Per-shard ordering, globally ordered if 1 shard
└── Message size > 256 KB (streaming large records)?
    └── Kinesis (up to 1 MB per record)
```

---

## 6. Common Patterns

### Pattern 1: Decoupled Web → Worker Tier

```
ALB → Web Tier (EC2/ECS)
           |
      SQS Standard Queue
           |
      Worker Tier (EC2 ASG)
           |  ← scales on ApproximateNumberOfMessagesVisible
      DLQ (failed jobs after 3 attempts)
           |
      CloudWatch Alarm → SNS → PagerDuty
```

### Pattern 2: Fan-Out (Order Processing)

```
Order Service
      |
   SNS Topic: order-placed
      |──────────────────────────────────┐
      |                                  |
SQS: inventory-queue            SQS: email-queue
      |                                  |
Lambda: update-inventory        Lambda: send-confirmation
```

### Pattern 3: Lambda + SQS + DLQ Pipeline

```yaml
# SAM/CDK config pattern
SQSQueue:
  VisibilityTimeout: 360   # > Lambda timeout (300s)
  RedrivePolicy:
    deadLetterTargetArn: !GetAtt DLQ.Arn
    maxReceiveCount: 3

LambdaFunction:
  Timeout: 300
  EventSourceMapping:
    BatchSize: 10
    MaximumBatchingWindow: 30
    BisectBatchOnFunctionError: true
    FunctionResponseTypes:
      - ReportBatchItemFailures
```

### Pattern 4: Temporary Queue (Request-Response)

```
Client → SQS Request Queue (with ReplyToQueueUrl in message attributes)
Server → processes, sends response to ReplyToQueueUrl
Client → polls its own temporary response queue
```

Use SQS Temporary Queues (virtual queues) for this — share a single physical queue.

---

## 7. Gotchas

### Exam Traps

| Gotcha | Detail |
|---|---|
| **FIFO 300 TPS limit** | No batching = 300/s; batching of 10 = 3,000/s. Standard has unlimited TPS. |
| **Visibility timeout < Lambda timeout** | Message reappears while Lambda is still processing → duplicate processing |
| **Standard delivers duplicates** | Design ALL consumers to be **idempotent** — always |
| **DLQ doesn't retry** | DLQ is a parking lot. You must manually redrive or fix + redrive |
| **FIFO name must end .fifo** | `my-queue.fifo` — if it doesn't end in `.fifo`, it IS a standard queue |
| **DLQ type must match source** | FIFO source → FIFO DLQ. Standard source → Standard DLQ |
| **Long polling not default** | Default is short polling (empty responses, extra cost). Set `WaitTimeSeconds=20` |
| **In-flight limit** | Standard: 120,000; FIFO: 20,000 in-flight messages max |

### Production Pitfalls

| Pitfall | Solution |
|---|---|
| **Poison pill messages** | Set `maxReceiveCount` → DLQ. Enable `BisectBatchOnFunctionError` for Lambda |
| **Message ordering** | Standard queue makes NO ordering guarantee — use FIFO if order matters |
| **Visibility timeout too short** | Monitor `ApproximateAgeOfOldestMessage` — if rising, extend timeout or scale consumers |
| **Not deleting messages** | Messages reappear after timeout — always delete on success |
| **Large message cost** | Storing 1 MB payloads? Use Extended Client Library with S3 |
| **FIFO single group bottleneck** | Use multiple `MessageGroupId` values for parallelism |
| **Cross-account SNS→SQS** | Queue must have resource policy allowing SNS topic's account to `sqs:SendMessage` |

---

## 8. Hands-On Lab (Free Tier)

### Goal: Standard SQS queue → Lambda consumer with DLQ

**Step 1 — Create DLQ**
1. SQS Console → Create queue
2. Type: Standard
3. Name: `my-app-dlq`
4. All defaults → Create

**Step 2 — Create Main Queue**
1. Create queue → Standard → Name: `my-app-queue`
2. Visibility timeout: `60` seconds
3. Message retention: `4 Days`
4. Receive message wait time: `20` seconds (long polling)
5. Dead-letter queue → Enabled → Select `my-app-dlq` → `maxReceiveCount: 3`
6. Create queue

**Step 3 — Send Test Messages**

```bash
aws sqs send-message \
  --queue-url https://sqs.us-east-1.amazonaws.com/123456789/my-app-queue \
  --message-body '{"orderId": "001", "item": "widget"}'

# Batch send (up to 10)
aws sqs send-message-batch \
  --queue-url https://sqs.us-east-1.amazonaws.com/123456789/my-app-queue \
  --entries '[
    {"Id":"1","MessageBody":"{\"orderId\":\"002\"}"},
    {"Id":"2","MessageBody":"{\"orderId\":\"003\"}"}
  ]'
```

**Step 4 — Create Lambda Consumer**

```python
import json
import boto3

sqs = boto3.client('sqs')

def lambda_handler(event, context):
    successful_ids = []
    failed_ids = []

    for record in event['Records']:
        try:
            body = json.loads(record['body'])
            print(f"Processing order: {body.get('orderId')}")
            # Your business logic here
            successful_ids.append(record['messageId'])
        except Exception as e:
            print(f"Failed: {e}")
            failed_ids.append({
                "itemIdentifier": record['messageId']
            })

    # Partial batch failure reporting
    return {
        "batchItemFailures": failed_ids
    }
```

Lambda Console:
- Runtime: Python 3.12
- Timeout: **30 seconds**
- Add trigger: SQS → `my-app-queue` → Batch size: 10 → Batch window: 10s
- Enable `ReportBatchItemFailures`

**Step 5 — Verify DLQ Behavior**
1. Modify Lambda to throw an error for `orderId = "002"`
2. Send message `{"orderId": "002"}`
3. After 3 receive attempts (with `maxReceiveCount=3`), check DLQ for the message

**Step 6 — Monitor**

```bash
# Check queue depth
aws sqs get-queue-attributes \
  --queue-url https://sqs.us-east-1.amazonaws.com/123456789/my-app-queue \
  --attribute-names ApproximateNumberOfMessages,ApproximateNumberOfMessagesNotVisible

# Check DLQ
aws sqs get-queue-attributes \
  --queue-url https://sqs.us-east-1.amazonaws.com/123456789/my-app-dlq \
  --attribute-names ApproximateNumberOfMessages
```

**Step 7 — Create FIFO Queue**
1. Create queue → FIFO → Name: `my-ordered-queue.fifo`
2. Content-based deduplication: Enabled
3. Send message with `MessageGroupId: "customer-123"`

```bash
aws sqs send-message \
  --queue-url https://sqs.us-east-1.amazonaws.com/123456789/my-ordered-queue.fifo \
  --message-body '{"step": 1}' \
  --message-group-id "customer-123" \
  --message-deduplication-id "step-1-$(date +%s)"
```

**Cleanup:**
```bash
aws sqs delete-queue --queue-url <my-app-queue-url>
aws sqs delete-queue --queue-url <my-app-dlq-url>
aws sqs delete-queue --queue-url <my-ordered-queue-url>
```
