# Amazon SNS — SAP-C02 Deep Dive

---

## 1. The Problem

A single event (e.g., "order placed") needs to trigger multiple independent downstream actions: send an email receipt, update inventory, notify the shipping system, and log to analytics. Implementing direct point-to-point calls creates a rigid mesh: each service must know about every other service, and adding a new subscriber requires modifying the publisher.

**Symptoms of one-to-many coupling:**
- Publisher code lists every subscriber explicitly
- Adding a consumer requires deploying the publisher
- One slow subscriber can block others
- No fan-out without custom broker logic

SNS solves this with a **publish-subscribe (pub/sub)** model: publish once, deliver to many.

---

## 2. What AWS Built

Amazon SNS is a **fully managed pub/sub messaging and mobile notification service**. Publishers send messages to an **SNS Topic**; SNS then fans those messages out to all subscribed endpoints simultaneously and independently.

---

## 3. How It Works

### Core Architecture

```
Publisher (any AWS service, SDK, CLI)
          |
       SNS Topic
          |
    ┌─────┼──────┬──────┬──────────┬──────────┐
    |     |      |      |          |           |
  SQS  Lambda  HTTPS  Email     Kinesis     Mobile
Queue  Func   endpoint  /SMS    Firehose     Push
```

Each subscription is independent — SNS delivers to all of them concurrently.

### Subscription Types

| Protocol | Use Case | Notes |
|---|---|---|
| **SQS** | Durable fan-out, decoupled processing | Must have resource policy allowing SNS |
| **Lambda** | Real-time processing, no queue needed | Async invoke; errors go to Lambda DLQ |
| **HTTP/HTTPS** | Webhooks to external services | Must confirm subscription via GET to SubscribeURL |
| **Email** | Human notification | Plaintext; must confirm via email link |
| **Email-JSON** | Structured email | Full JSON envelope; must confirm |
| **SMS** | Text messages | Regional availability varies; has monthly spend limit |
| **Mobile Push** | APNs (iOS), GCM/FCM (Android) | Via Platform Application + device token |
| **Kinesis Firehose** | High-volume data to S3/Redshift/OpenSearch | SNS→Firehose→destination |

### SNS FIFO Topics

- Like SQS FIFO: **strict ordering**, **exactly-once delivery**, **deduplication**
- **Only SQS FIFO subscribers supported** — no Lambda, HTTP, Email, SMS
- Throughput: same limits as SQS FIFO (300 TPS / 3,000 TPS with batching)
- Name must end in `.fifo`
- Use when downstream systems require ordered, deduplicated events

```
SNS FIFO Topic: order-events.fifo
    |
    ├── SQS FIFO: inventory-queue.fifo  (MessageGroupId preserved)
    └── SQS FIFO: billing-queue.fifo
```

### Message Filtering

**Without filtering:** every subscriber receives every message → wasteful.

**With filter policies:** SNS evaluates message attributes before delivery, only routing matching messages.

```json
// Message published with attributes:
{
  "MessageAttributes": {
    "eventType": {"DataType": "String", "StringValue": "ORDER_PLACED"},
    "region": {"DataType": "String", "StringValue": "us-east"},
    "amount": {"DataType": "Number", "StringValue": "150"}
  }
}

// Subscription filter policy (on SQS subscription for inventory service):
{
  "eventType": ["ORDER_PLACED", "ORDER_UPDATED"],
  "region": ["us-east", "us-west"],
  "amount": [{"numeric": [">=", 100]}]
}
```

**Filter policy scope:**
- `MessageAttributes` (default) — filter on message attributes
- `MessageBody` — filter on JSON message body (newer feature)

**Benefits:**
- Reduces cost (you pay per delivery, filtering prevents unnecessary deliveries)
- Decouples routing logic from publisher

### Message Attributes

Up to **10 message attributes** per message. Each attribute has:
- Name (string)
- DataType: `String`, `Number`, `Binary`, `String.Array`
- Value

Used for routing (filter policies) and metadata.

### DLQ for Subscriptions

- Each **subscription** (not the topic) can have its own DLQ (SQS queue)
- Catches messages that couldn't be delivered (HTTP endpoint down, Lambda errors, etc.)
- SQS DLQ for subscription must be in the same AWS account and region

```
SNS Topic
    |
  [HTTP subscription]──failure──> SQS DLQ
```

### Mobile Push Architecture

```
Mobile App
    |
  Register device → get device token (APNs/FCM)
    |
  Store token in your backend
    |
  Create Platform Application (SNS Console)
    |  (configure APNs certificate or FCM server key)
    |
  Create Platform Endpoint (per device token)
    |
  Publish to endpoint → SNS → APNs/FCM → Device
  OR
  Subscribe endpoint to SNS Topic for broadcast push
```

**Platforms supported:** APNs (Apple iOS/macOS), FCM/GCM (Android), ADM (Amazon), Baidu, Windows (WNS/MPNS)

### Message Size

- Max message size: **256 KB** (same as SQS)
- For larger payloads: store in S3, publish S3 URL in SNS message

### Cross-Account Topic Policy

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {"AWS": "arn:aws:iam::ACCOUNT-B:root"},
    "Action": "sns:Publish",
    "Resource": "arn:aws:sns:us-east-1:ACCOUNT-A:my-topic"
  }]
}
```

### Encryption (SSE)

- SSE with AWS KMS: encrypts messages at rest within SNS
- KMS key must allow SNS service principal in key policy
- In-transit: HTTPS always

---

## 4. Key Config & Limits

| Parameter | Value |
|---|---|
| Message size | Max **256 KB** |
| Subscriptions per topic | **12,500,000** |
| Topics per account | **100,000** |
| Message retention (no delivery) | **23 days** (for SQS/Lambda retries); HTTP up to 23 days with backoff |
| Filter policy attributes | Max **5** attribute conditions per filter policy |
| Message attribute entries | Max **10** per message |
| SMS monthly spend limit | Default **$1.00** (request increase) |
| SNS FIFO throughput | 300 TPS (3,000 with batching) |
| HTTP retry policy | Up to 100,015 retries over 23 days |
| Delivery delay | No delay queues on SNS (use SQS Delay Queue on subscriber) |

---

## 5. Decision Tree

### SNS vs SQS vs EventBridge

```
What is your primary need?

├── One-to-many fan-out (single event → multiple subscribers)?
│   └── SNS (+ SQS per subscriber for durability)

├── One-to-one task queue / worker decoupling?
│   └── SQS

├── Complex event routing based on event content/patterns?
│   └── EventBridge (content-based routing, schema registry, 5 targets/rule)

├── Events from AWS services (CloudWatch, EC2 state, S3)?
│   ├── CloudWatch Alarms → SNS (notifications)
│   └── Operational events → EventBridge (richer routing)

├── Events from SaaS partners (Salesforce, Zendesk)?
│   └── EventBridge (partner event buses)

├── Ordered delivery to multiple consumers?
│   └── SNS FIFO → SQS FIFO subscribers

└── Do you need message replay?
    ├── SNS: No replay
    ├── SQS: No replay (DLQ redrive is manual)
    └── Kinesis: Yes (retention-window replay)
```

---

## 6. Common Patterns

### Pattern 1: Fan-Out (Most Common)

```
Order Service
      |
   SNS: order-events
      |─────────────────────────────────────────────┐
      |                          |                   |
SQS: inventory-queue    SQS: email-queue    SQS: analytics-queue
      |                          |                   |
Lambda: update-stock     Lambda: send-email   Kinesis Firehose
                                                      |
                                                     S3
```

Each SQS queue has its own failure handling, scaling, and retry policy.

### Pattern 2: CloudWatch Alarm → SNS → Multi-Channel Notification

```
CloudWatch Alarm (CPU > 80%)
      |
   SNS Topic: ops-alerts
      |──────────────────┐
      |                  |
  SQS: incident     Lambda: send-slack-message
  (for ticketing)   (calls Slack webhook)
      |
  Lambda: create-jira-ticket
```

### Pattern 3: Mobile Push with Topic Broadcast

```
Marketing Campaign Event
      |
   SNS Topic: promotions
      |
  [Subscribed platform endpoints for all users]
      |
  APNs → iOS devices
  FCM  → Android devices
```

### Pattern 4: SNS Filter Policy for Event Routing

```
E-Commerce Platform publishes all events to one SNS topic
      |
   SNS Topic: all-events
      |─────────────────────────────────────────────┐
      |                                              |
SQS: inventory-queue                    SQS: fraud-queue
FilterPolicy:                           FilterPolicy:
  eventType: [ORDER_PLACED]               eventType: [PAYMENT_FAILED, CHARGEBACK]
  amount: >= 0                            amount: >= 500
```

### Pattern 5: Kinesis Firehose via SNS

```
IoT Sensor Data → SNS Topic → Kinesis Firehose → S3 Data Lake
                           ↓
                      Lambda (real-time alert for anomalies)
```

---

## 7. Gotchas

### Exam Traps

| Gotcha | Detail |
|---|---|
| **SNS is NOT ordered** | Use SNS FIFO topic if ordering required; standard SNS has no ordering guarantee |
| **HTTP subscribers must confirm** | SNS sends a `SubscriptionConfirmation` GET request; endpoint must respond to activate |
| **SMS regional limits** | Not all regions support SMS; US default spend limit is $1/month — request increase |
| **256 KB message size limit** | Same as SQS; use S3 + pointer for large payloads |
| **Filter policies reduce cost** | Only paying for deliveries to matched subscribers |
| **SNS FIFO only allows SQS FIFO** | Cannot subscribe Lambda/HTTP/Email to SNS FIFO topic |
| **No delivery to email in prod** | Email subscriptions require manual confirmation — not suitable for automated workflows |
| **Cross-account SQS subscription** | SQS queue needs resource policy allowing SNS to `sqs:SendMessage` |
| **Message not stored** | SNS does not persist messages; if delivery fails and no retry succeeds, message is lost (unless SQS subscriber with DLQ) |

### Production Pitfalls

| Pitfall | Solution |
|---|---|
| **No durability on failed HTTP delivery** | Add SQS subscription as buffer; process from SQS |
| **Device tokens expire** | Handle `InvalidRegistrationToken` errors; remove stale endpoints |
| **Fan-out without SQS** | Direct Lambda subscription loses messages if Lambda throttled; add SQS between SNS and Lambda |
| **Overly broad filter policies** | Start restrictive; widen as needed — wrong filters silently drop messages |
| **SNS delivery failures invisible** | Enable delivery status logging (CloudWatch Logs) for each protocol |

---

## 8. Hands-On Lab (Free Tier)

### Goal: SNS Fan-Out to SQS + Email with Filter Policies

**Step 1 — Create SQS Queues (subscribers)**

```bash
# Inventory queue
aws sqs create-queue --queue-name inventory-queue

# Orders queue
aws sqs create-queue --queue-name orders-all-queue
```

**Step 2 — Create SNS Topic**

```bash
aws sns create-topic --name order-events
# Note the TopicArn in output
```

**Step 3 — Add Resource Policy to SQS (allow SNS)**

```bash
QUEUE_URL=$(aws sqs get-queue-url --queue-name inventory-queue --query QueueUrl --output text)
QUEUE_ARN=$(aws sqs get-queue-attributes --queue-url $QUEUE_URL \
  --attribute-names QueueArn --query Attributes.QueueArn --output text)

aws sqs set-queue-attributes \
  --queue-url $QUEUE_URL \
  --attributes '{
    "Policy": "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"Service\":\"sns.amazonaws.com\"},\"Action\":\"sqs:SendMessage\",\"Resource\":\"'"$QUEUE_ARN"'\"}]}"
  }'
```

**Step 4 — Subscribe SQS to SNS with Filter Policy**

```bash
TOPIC_ARN="arn:aws:sns:us-east-1:123456789:order-events"

# Inventory queue: only ORDER_PLACED events
aws sns subscribe \
  --topic-arn $TOPIC_ARN \
  --protocol sqs \
  --notification-endpoint $QUEUE_ARN \
  --attributes '{
    "FilterPolicy": "{\"eventType\":[\"ORDER_PLACED\"]}"
  }'

# Subscribe email (for all events)
aws sns subscribe \
  --topic-arn $TOPIC_ARN \
  --protocol email \
  --notification-endpoint your@email.com
# Check email and click confirmation link!
```

**Step 5 — Publish Test Messages**

```bash
# This should reach inventory queue + email
aws sns publish \
  --topic-arn $TOPIC_ARN \
  --message '{"orderId": "001", "item": "widget", "quantity": 5}' \
  --message-attributes '{
    "eventType": {"DataType": "String", "StringValue": "ORDER_PLACED"},
    "amount": {"DataType": "Number", "StringValue": "99.99"}
  }'

# This should reach email ONLY (not inventory queue - eventType doesn't match)
aws sns publish \
  --topic-arn $TOPIC_ARN \
  --message '{"orderId": "001", "status": "shipped"}' \
  --message-attributes '{
    "eventType": {"DataType": "String", "StringValue": "ORDER_SHIPPED"}
  }'
```

**Step 6 — Verify Filter Routing**

```bash
# Check inventory queue - should have 1 message (ORDER_PLACED only)
aws sqs receive-message \
  --queue-url $QUEUE_URL \
  --wait-time-seconds 5
```

**Step 7 — Enable Delivery Logging**

In SNS Console → Topic → Edit → Delivery status logging:
- Choose SQS → IAM role (create new) → Success sample rate: 100%
- View logs in CloudWatch Logs group `/aws/sns/...`

**Step 8 — Create SNS FIFO Topic**

```bash
aws sns create-topic \
  --name order-events.fifo \
  --attributes FifoTopic=true,ContentBasedDeduplication=true

# Subscribe SQS FIFO queue only
aws sqs create-queue \
  --queue-name inventory-ordered.fifo \
  --attributes FifoQueue=true,ContentBasedDeduplication=true
```

**Cleanup:**

```bash
aws sns delete-topic --topic-arn $TOPIC_ARN
aws sqs delete-queue --queue-url $QUEUE_URL
```
