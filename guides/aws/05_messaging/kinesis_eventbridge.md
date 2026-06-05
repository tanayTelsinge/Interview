# Kinesis + EventBridge — SAP-C02 Deep Dive

---

## 1. The Problem

**Kinesis problem statement:** Millions of IoT sensors, clickstream events, application logs, and financial transactions arrive continuously. Batch processing introduces unacceptable latency. You need to process records in real time, replay historical data for ML retraining, and support multiple consumers reading the same stream independently.

**EventBridge problem statement:** Microservices emit dozens of different event types. SQS/SNS fan-out works for simple cases, but you need content-based routing, schema validation, integration with 200+ SaaS partners, and scheduled tasks — all managed centrally without code changes in publishers.

---

## 2. What AWS Built

**Kinesis Family:**
- **Kinesis Data Streams (KDS):** Real-time durable data stream — produce, store, consume
- **Kinesis Data Firehose:** Zero-code delivery pipeline to storage/analytics destinations
- **Amazon Managed Service for Apache Flink (Kinesis Analytics):** Stateful stream processing
- **MSK (Managed Streaming for Apache Kafka):** Fully managed Kafka

**EventBridge:**
- Serverless event bus with content-based routing, schema registry, and SaaS integrations

---

## 3. How It Works — Kinesis Data Streams

### Architecture

```
Producers                     KDS Shards                    Consumers
(SDK/Agent/Firehose            ┌─────────┐
 /IoT/MSK Connect)             │ Shard 0 │──────────────> Lambda (1 invocation/shard)
         |              hash   │ Shard 1 │──────────────> KCL Application
  PartitionKey─────────────>  │ Shard 2 │──────────────> Kinesis Firehose
                               └─────────┘──────────────> Flink (KDA)
                               (records ordered
                                within each shard)
```

### Shard Fundamentals

| Metric | Value |
|---|---|
| **Write capacity** | 1 MB/s OR 1,000 records/s per shard |
| **Read capacity (Classic)** | 2 MB/s per shard **shared** across all consumers |
| **Read capacity (Enhanced Fan-Out)** | 2 MB/s per shard **per consumer** (push model) |
| **Record size** | Max **1 MB** |
| **Retention** | Default 24 hrs | Extended: up to **365 days** |
| **Ordering** | Strict within a shard; no cross-shard ordering |

### Shard Capacity Math

```
Need to ingest 5 MB/s, 500 records/s:
  Shards needed = max(5 MB/s ÷ 1 MB/s, 500 ÷ 1000) = 5 shards

With 3 Classic consumers reading at 2 MB/s from 5 shards:
  Total read = 5 shards × 2 MB/s = 10 MB/s shared across 3 consumers
  Per consumer = 10/3 ≈ 3.3 MB/s — OK if within limits

With Enhanced Fan-Out (3 consumers):
  Each consumer gets 5 × 2 MB/s = 10 MB/s independently
```

### Capacity Modes

| Feature | Provisioned | On-Demand |
|---|---|---|
| Pricing | Per shard-hour ($0.015/shard/hr) | Pay per GB in/out |
| Scaling | Manual `UpdateShardCount` (reshard) | Auto-scales based on throughput |
| Best for | Predictable workloads | Variable/unknown workloads |
| Max shards | Soft limit, request increase | Manages automatically |

### Partition Key → Shard Hashing

```
Record → MD5(PartitionKey) → hash value → mapped to shard range

Good partition key: userId, deviceId (high cardinality)
Bad partition key:  "fixed-key" (all records → 1 shard = HOT PARTITION)
Bad partition key:  date like "2024-01-01" (all traffic → 1 shard per day)

Fix hot partition: append random suffix, use composite key, or reshard
```

### Consumer Types

**Classic (GetRecords — polling pull model):**
- Shared 2 MB/s per shard across all consumers
- Lambda: 1 concurrent invocation per shard (batching up to 10,000 records or 6 MB)
- KCL: One worker per shard (lease-based coordination via DynamoDB)

**Enhanced Fan-Out (SubscribeToShard — push model):**
- **2 MB/s per consumer per shard** — fully independent bandwidth
- Push-based: records delivered within 70 ms (vs 200 ms polling)
- Extra cost: ~$0.015 per consumer shard-hour + $0.013 per GB
- Register consumer: `RegisterStreamConsumer` API

### KCL (Kinesis Client Library)

- Runs as a fleet of workers; each worker claims shard leases
- Leases stored in **DynamoDB table** (one row per shard)
- Checkpoints progress in DynamoDB (sequence number)
- Scale workers by adding instances; KCL redistributes leases
- Available for Java, Python, Node.js, .NET, Go

### Lambda Event Source Mapping for KDS

```
KDS Shard ──ESM──> Lambda
           |
           ├── StartingPosition: TRIM_HORIZON / LATEST / AT_TIMESTAMP
           ├── BatchSize: 1–10,000 records
           ├── BisectBatchOnFunctionError: true (binary search for bad record)
           ├── MaximumRetryAttempts: 0–10,000 (-1 = forever)
           ├── MaximumRecordAgeInSeconds: skip old records
           ├── DestinationConfig: OnFailure → SQS/SNS
           └── ParallelizationFactor: 1–10 (Lambda invocations per shard)
```

`ParallelizationFactor`: up to 10 concurrent Lambda invocations per shard, keys within a shard stay ordered.

---

## 4. How It Works — Kinesis Data Firehose

### Architecture

```
Sources                    Firehose Stream              Destinations
(KDS / Direct PUT /         ┌──────────────┐
 MSK / CloudWatch /         │  Buffer:     │──────────> S3
 IoT / EventBridge)─────>  │  Size: 1-128MB│──────────> Amazon Redshift
                            │  Time: 60-900s│──────────> OpenSearch Service
                            │              │──────────> Splunk
                            │  Lambda      │──────────> HTTP endpoint
                            │  Transform   │──────────> Snowflake / others
                            └──────────────┘
                            (auto-scaling, no shards)
```

### Key Characteristics

| Feature | Value |
|---|---|
| **Managed by** | Fully serverless — no capacity provisioning |
| **Latency** | **Minimum 60 seconds** (buffering before delivery) |
| **Buffer size** | 1 MB – 128 MB |
| **Buffer interval** | 60 seconds – 900 seconds |
| **Delivery guarantee** | **At-least-once** (no replay capability) |
| **Lambda transform** | Invoke Lambda per batch for format conversion/filtering |
| **S3 backup** | All records or only failed transforms |
| **Compression** | GZIP, Snappy, Zip, Hadoop-compatible GZIP |
| **Encryption** | SSE-KMS |

**Delivery triggers whichever comes first:** buffer size OR buffer interval.

### Firehose vs Data Streams

| Feature | Kinesis Data Streams | Kinesis Firehose |
|---|---|---|
| Consumers | Any custom consumer | Only predefined destinations |
| Latency | Real-time (milliseconds) | Near real-time (min 60s) |
| Replay | Yes (within retention window) | No |
| Scaling | Manual shards or On-Demand | Automatic |
| Code required | Yes (consumer code) | No (zero code) |
| Delivery guarantee | At-least-once | At-least-once |
| Use case | Real-time processing | Load to storage/analytics |

---

## 5. How It Works — Amazon Managed Service for Apache Flink

- Managed Apache Flink runtime (formerly Kinesis Data Analytics)
- Sources: KDS, MSK, S3
- Sinks: KDS, Firehose, S3, DynamoDB, others
- **Exactly-once** semantics with checkpointing
- Stateful processing: windows (tumbling, sliding, session), aggregations, joins
- Scales automatically; pay per KPU (Kinesis Processing Unit)

---

## 6. How It Works — Amazon MSK

### When to Choose MSK Over KDS

| Choose MSK | Choose KDS |
|---|---|
| Need Kafka ecosystem (Kafka Connect, MirrorMaker, Schema Registry) | AWS-native integration preferred |
| Open-source portability / avoid vendor lock-in | No Kafka expertise on team |
| Compliance requires Kafka | Cost optimization (KDS simpler pricing) |
| Using Kafka Streams or ksqlDB | Managed scaling preferred |
| > 1 MB record sizes | Records ≤ 1 MB |
| Long retention (months) | Days retention sufficient |

### MSK Options

| Feature | MSK Provisioned | MSK Serverless |
|---|---|---|
| Capacity | Choose broker instance type | Automatic |
| Storage | Choose EBS per broker | Automatic |
| Cost | Broker hours + storage | Per partition-hour + GB |
| Multi-AZ | 2 or 3 AZs | Automatic |
| Control | Full Kafka config | Limited config |

### MSK Connect
- Kafka Connect managed service on MSK
- Run source/sink connectors (S3, OpenSearch, RDS, etc.)
- Auto-scaling workers

---

## 7. How It Works — Amazon EventBridge

### Event Buses

| Bus Type | Source | Use Case |
|---|---|---|
| **Default** | All AWS services | EC2 state changes, S3, CodePipeline, etc. |
| **Custom** | Your applications | Microservice events |
| **Partner** | SaaS (Salesforce, Zendesk, GitHub, Shopify, etc.) | SaaS integration without polling |

### Rules

Each rule on a bus has:
1. **Event pattern** — JSON pattern matching (content-based routing)
2. **Schedule** — cron or rate expression (e.g., `rate(5 minutes)`)
3. **Targets** — up to **5 targets per rule**

```json
// Event pattern: match EC2 instance stopping in us-east-1
{
  "source": ["aws.ec2"],
  "detail-type": ["EC2 Instance State-change Notification"],
  "detail": {
    "state": ["stopped", "stopping"],
    "instance-id": [{"prefix": "i-"}]
  },
  "region": ["us-east-1"]
}
```

**Target types:** Lambda, SQS, SNS, Kinesis, Step Functions, ECS, CodePipeline, API Gateway, EventBridge bus (cross-account), HTTP endpoint, and more.

### Schema Registry

- Auto-discovers event schemas from events on the bus
- Generate code bindings for Java, Python, TypeScript
- Validate events against schema
- Versioned schemas

### EventBridge Pipes

Point-to-point event processing with built-in source/target integration:

```
Source ──filter──> Enrichment ──transform──> Target
(SQS/KDS/DynamoDB  (optional Lambda/       (Lambda/SQS/SNS/
 /MSK/Kafka)        Step Functions/          Step Functions/
                    API Gateway)             API Gateway/etc.)
```

- Reduces boilerplate glue code
- Built-in filtering before enrichment (cost savings)
- Single pipe per source-target pair

---

## 8. Key Config & Limits

### Kinesis Data Streams

| Parameter | Value |
|---|---|
| Write per shard | 1 MB/s or 1,000 records/s |
| Read per shard (Classic) | 2 MB/s shared |
| Read per shard (EFO) | 2 MB/s per registered consumer |
| Max record size | **1 MB** |
| Retention | 24 hrs (default) – **365 days** |
| Shard count max | Default 500 (soft limit) per region |
| GetRecords calls | 5 per second per shard |
| PutRecords batch | Up to 500 records or 5 MB |

### Kinesis Firehose

| Parameter | Value |
|---|---|
| Buffer size | 1 MB – 128 MB |
| Buffer interval | **60 s – 900 s** (minimum 60s) |
| Lambda transform timeout | 5 minutes |
| Record size max | 1 MB |
| Delivery retry | Up to 24 hours |

### EventBridge

| Parameter | Value |
|---|---|
| Targets per rule | **5** |
| Rules per event bus | 300 (default, adjustable) |
| Event size | Max **256 KB** |
| Schedule granularity | 1-minute minimum |
| Cross-region/account | Yes (via bus-to-bus target) |

---

## 9. Decision Tree

### Kinesis vs Firehose vs SQS vs MSK

```
What is your use case?

├── Task queue / decoupled microservices / job processing?
│   └── SQS

├── Real-time stream processing (analytics, ML, dashboards)?
│   └── Kinesis Data Streams (+ Flink/Lambda for processing)

├── Load streaming data to S3/Redshift/OpenSearch with no code?
│   └── Kinesis Firehose

├── Need replay of historical stream data?
│   └── Kinesis Data Streams (NOT Firehose)

├── Need Kafka ecosystem / open-source / connectors?
│   └── MSK

├── Record size > 1 MB?
│   └── MSK (Kafka supports up to 1 MB default, configurable higher)

└── Multiple independent consumers of same stream?
    ├── KDS with Enhanced Fan-Out (2 MB/s per consumer)
    └── MSK (consumer groups)
```

### SNS vs EventBridge

```
├── Simple pub/sub fan-out to SQS/Lambda/Email/SMS/Mobile?
│   └── SNS

├── Complex content-based routing with JSON pattern matching?
│   └── EventBridge

├── SaaS partner events (Salesforce, Zendesk, GitHub)?
│   └── EventBridge (partner event buses)

├── Schema registry + code binding generation?
│   └── EventBridge

├── Scheduled tasks (cron)?
│   └── EventBridge (Scheduler or Rules)

└── Mobile push notifications?
    └── SNS (APNs/FCM/GCM support)
```

---

## 10. Common Patterns

### Pattern 1: Real-Time Clickstream Pipeline

```
Web App → KDS (10 shards, 5 MB/s)
               |
        ┌──────┴───────┐
        |              |
    Lambda (EFO)    Firehose (EFO)
    Real-time        |
    alerts           S3 (raw data lake)
    (fraud check)    |
                  Glue Crawler → Athena queries
```

### Pattern 2: IoT Telemetry with Fan-Out

```
IoT Devices → IoT Core → KDS
                           |──── Lambda (EFO consumer 1): real-time anomaly
                           |──── KDA Flink (EFO consumer 2): 5-min aggregations → DynamoDB
                           └──── Firehose (Classic consumer): S3 archive
```

### Pattern 3: EventBridge-Driven Microservices

```
Order Service → EventBridge (custom bus: order-events)
                      |
          ┌───────────┼────────────────┐
          |           |                |
    Rule: ORDER_PLACED  Rule: ORDER_SHIPPED  Rule: PAYMENT_FAILED
          |           |                |
       Lambda:      SQS:            Step Functions:
     inventory    shipping-queue    fraud-workflow
     update
```

### Pattern 4: SaaS → EventBridge → Lambda

```
Salesforce (Lead Created) → EventBridge Partner Bus
                                   |
                          Rule: lead-source = "web"
                                   |
                               Lambda: enrich + insert DynamoDB
```

### Pattern 5: KDS → Firehose Delivery (No EFO)

```
KDS Stream → Firehose (as consumer, Classic)
                  |
          Lambda Transform (convert JSON → Parquet)
                  |
              S3 (Parquet files in s3://datalake/year=X/month=Y/day=Z/)
                  |
          Glue + Athena for ad-hoc SQL queries
```

---

## 11. Gotchas

### Kinesis

| Gotcha | Detail |
|---|---|
| **Hot partition** | High-cardinality partition key critical. Never use constant or low-cardinality key |
| **Firehose min 60s latency** | Not real-time. If you need < 60s delivery, use KDS + Lambda |
| **Enhanced Fan-Out costs extra** | ~$0.015/shard-hour per consumer + data transfer. Justify with throughput needs |
| **Classic GetRecords 5 calls/s limit** | With multiple consumers polling same shard, throttling occurs — use EFO |
| **Lambda 1 invocation per shard** | Parallelism = number of shards (unless ParallelizationFactor > 1) |
| **Resharding takes time** | Splitting/merging shards not instant; plan capacity ahead |
| **MSK broker storage costs** | Kafka retains all data until retention period; storage costs accumulate |
| **Firehose no replay** | Data delivered to S3/Redshift is final — no re-consuming of stream |
| **Record ordering** | Only guaranteed within a shard. Cross-shard ordering not supported |

### EventBridge

| Gotcha | Detail |
|---|---|
| **All events cost money** | Charged per event even if no rule matches — filter at producer or use custom bus |
| **5 targets per rule** | Need more? Fan out to SNS/SQS and subscribe many consumers |
| **Default bus AWS events only** | Custom app events must go to custom bus |
| **Cross-region delivery** | Requires bus-to-bus target across regions — additional cost |
| **EventBridge Pipes — 1:1 only** | One source → one target per pipe; not fan-out |
| **Schedule minimum 1 minute** | For sub-minute scheduling, use other approaches |

---

## 12. Hands-On Lab (Free Tier)

### Goal: Kinesis Data Stream → Lambda consumer + Firehose → S3

**Step 1 — Create KDS Stream**

```bash
aws kinesis create-stream \
  --stream-name my-data-stream \
  --shard-count 1
```

**Step 2 — Create S3 Bucket for Firehose**

```bash
aws s3 mb s3://my-firehose-delivery-$(aws sts get-caller-identity --query Account --output text)
```

**Step 3 — Create Firehose Delivery Stream**

Console: Kinesis → Firehose → Create delivery stream
- Source: Kinesis Data Stream → `my-data-stream`
- Destination: Amazon S3 → your bucket
- Buffer: 1 MB / 60 seconds
- Prefix: `year=!{timestamp:yyyy}/month=!{timestamp:MM}/day=!{timestamp:dd}/`
- Error prefix: `errors/!{firehose:error-output-type}/`

**Step 4 — Lambda Consumer**

```python
import base64
import json

def lambda_handler(event, context):
    for record in event['Records']:
        # KDS records are base64 encoded
        payload = base64.b64decode(record['kinesis']['data']).decode('utf-8')
        data = json.loads(payload)

        shard_id = record['eventID'].split(':')[0]
        seq_num = record['kinesis']['sequenceNumber']
        partition_key = record['kinesis']['partitionKey']

        print(f"Shard: {shard_id} | Partition: {partition_key} | Data: {data}")

    return {'statusCode': 200}
```

Lambda trigger: KDS → `my-data-stream` → Batch size: 100 → Starting position: LATEST

**Step 5 — Produce Test Records**

```bash
# Single record
aws kinesis put-record \
  --stream-name my-data-stream \
  --data '{"userId":"user-123","event":"page_view","page":"/home"}' \
  --partition-key "user-123"

# Batch records (different partition keys = good distribution)
aws kinesis put-records \
  --stream-name my-data-stream \
  --records '[
    {"Data":"{\"userId\":\"u1\",\"event\":\"click\"}","PartitionKey":"u1"},
    {"Data":"{\"userId\":\"u2\",\"event\":\"purchase\"}","PartitionKey":"u2"},
    {"Data":"{\"userId\":\"u3\",\"event\":\"logout\"}","PartitionKey":"u3"}
  ]'
```

**Step 6 — Check Lambda Logs**

```bash
aws logs tail /aws/lambda/my-kinesis-consumer --follow
```

**Step 7 — Verify Firehose S3 Delivery** (wait 60-120 seconds)

```bash
aws s3 ls s3://my-firehose-delivery-ACCOUNT/ --recursive
```

**Step 8 — EventBridge Quick Test**

```bash
# Create custom event bus
aws events create-event-bus --name my-app-bus

# Create rule
aws events put-rule \
  --name process-orders \
  --event-bus-name my-app-bus \
  --event-pattern '{"source":["my-app"],"detail-type":["ORDER_PLACED"]}' \
  --state ENABLED

# Add Lambda target (replace with your Lambda ARN)
aws events put-targets \
  --rule process-orders \
  --event-bus-name my-app-bus \
  --targets 'Id=1,Arn=arn:aws:lambda:us-east-1:ACCOUNT:function:my-function'

# Send test event
aws events put-events \
  --entries '[{
    "Source": "my-app",
    "DetailType": "ORDER_PLACED",
    "Detail": "{\"orderId\":\"001\",\"amount\":99.99}",
    "EventBusName": "my-app-bus"
  }]'
```

**Cleanup:**

```bash
aws kinesis delete-stream --stream-name my-data-stream
aws events delete-rule --name process-orders --event-bus-name my-app-bus
aws events delete-event-bus --name my-app-bus
aws s3 rb s3://my-firehose-delivery-ACCOUNT --force
```
