# Data Analytics — Streaming

## 1. The Problem

Batch processing introduces latency — you get yesterday's data, not today's. Many business decisions require real-time signals:

- Fraud detection: block a transaction *now*, not tomorrow after batch analysis
- Infrastructure monitoring: alert when latency spikes *now*, not in the next hourly report
- Personalization: show a recommendation based on *this session's* behavior
- IoT: respond to a sensor reading *immediately* before equipment fails

Real-time decision latency is the gap between an event happening and a system reacting to it. Batch closes this gap with a fire hose; streaming closes it with a pipe.

---

## 2. What AWS Built

End-to-end streaming architecture:

```
INGEST         PROCESS              DELIVER              VISUALIZE
──────         ───────              ───────              ─────────
Kinesis        Kinesis Analytics    Kinesis Firehose      QuickSight
Data Streams   (Apache Flink)       → S3/Redshift/ES      Dashboards

MSK (Kafka)    Lambda (per-record)  OpenSearch Service   Kibana/
                                    (Elasticsearch)       OS Dashboards

IoT Core       Glue Streaming      DynamoDB              Custom App
               (Spark Streaming)
```

---

## 3. How It Works

### Kinesis Data Streams — Deep Dive

**The Hot Partition Problem:**

```
Bad partition key: user_country ("US", "EU") — 2 unique values
  → Most traffic goes to 1-2 shards → shard throughput limit exceeded
  → Throttling: ProvisionedThroughputExceededException

Good partition key: user_id (millions of unique values)
  → Traffic evenly distributed across all shards
  → No single shard overwhelmed

Fix for unavoidably skewed data:
  Add random suffix: partition_key = user_country + "-" + random(1,100)
  → "US-47", "US-12", "EU-83" → spreads across shards
  → Tradeoff: can't read all "US" events from one shard anymore
```

**Shard Limits:**
```
Per shard:
  Write: 1 MB/sec OR 1,000 records/sec (whichever hit first)
  Read (shared): 2 MB/sec across ALL consumers
  Read (Enhanced Fan-Out): 2 MB/sec PER registered consumer

Shard management:
  Splitting: increase capacity → split 1 shard into 2 (add capacity)
  Merging:   decrease capacity → merge 2 adjacent shards into 1 (reduce cost)

Retention:
  Default: 24 hours
  Extended: up to 365 days (additional cost)
```

**Enhanced Fan-Out (EFO):**
```
Problem: 10 Lambda functions all reading from same stream
  → Each shares the 2 MB/sec read limit
  → With 10 consumers = 200 KB/sec each (not enough for heavy consumers)

Solution: Enhanced Fan-Out
  Each registered consumer gets DEDICATED 2 MB/sec
  Push model: Kinesis pushes to consumers (vs poll model for shared)
  Cost: additional $0.015/shard-hour per registered consumer

Use EFO when: multiple consumers need full throughput independently
```

**KPL (Kinesis Producer Library):**
```
Aggregation:  bundle multiple small records into 1 Kinesis record
  → 100 records × 1 KB each → 1 Kinesis record of 100 KB
  → 99% reduction in PUT request count (cheaper)

Collection:   buffer records locally, batch PUT them
  → Reduce API calls

Tradeoff: adds latency (buffering time before sending)
Not good for: ultra-low latency requirements
Good for:    high-throughput, cost optimization
```

---

### Kinesis Data Firehose — Patterns

**S3 Dynamic Partitioning:**
```json
// Extract fields from record to use as S3 partition keys
{
  "dynamicPartitioning": {
    "enabled": true
  },
  "processingConfiguration": {
    "processors": [{
      "type": "MetadataExtraction",
      "parameters": [{
        "parameterName": "MetadataExtractionQuery",
        "parameterValue": "{country: .country, event_type: .event_type}"
      }, {
        "parameterName": "JsonParsingEngine",
        "parameterValue": "JQ-1.6"
      }]
    }]
  },
  "s3BackupMode": "Disabled",
  "prefix": "data/country=!{partitionKeyFromQuery:country}/type=!{partitionKeyFromQuery:event_type}/year=!{timestamp:yyyy}/month=!{timestamp:MM}/"
}
// Result: s3://bucket/data/country=US/type=click/year=2024/month=01/
// Athena can now query by partition without scanning all data
```

**Redshift Loading via Intermediate S3:**
```
Firehose → S3 (intermediate) → Redshift COPY command (auto-triggered)

Why not direct insert? Redshift COPY is 100x faster than individual INSERTs
Firehose buffers records, writes to S3, triggers Redshift COPY automatically
```

**Lambda Transformation in Firehose:**
```python
def lambda_handler(event, context):
    output = []
    for record in event['records']:
        # Decode
        payload = json.loads(base64.b64decode(record['data']))

        # Transform
        payload['processed_at'] = datetime.utcnow().isoformat()
        payload['amount_usd'] = payload.get('amount', 0) / 100  # cents to dollars

        # Encode and return
        output.append({
            'recordId': record['recordId'],
            'result': 'Ok',
            'data': base64.b64encode(
                json.dumps(payload).encode('utf-8')
            ).decode('utf-8')
        })
    return {'records': output}
```

**Firehose Buffer Settings:**
```
Buffer size:     1 MB – 128 MB (deliver when buffer hits this size)
Buffer interval: 60 sec – 900 sec (deliver after this time regardless of size)
→ Delivery triggered by WHICHEVER limit is hit first
→ MINIMUM 60 seconds — Firehose is NOT for sub-minute latency
```

**Format Conversion (JSON → Parquet/ORC):**
```
Firehose → Lambda (optional transform) → format conversion → S3 (Parquet)

Benefits for Athena:
  JSON: $5/TB scanned (reads all fields)
  Parquet: ~$0.50/TB scanned for typical queries (columnar, compressed)
  Savings: 90% less on Athena queries

Requires: Glue Data Catalog schema for the conversion
```

---

### Kinesis Data Analytics / Apache Flink

**Use Cases:**
- Real-time fraud detection (join events with account history)
- Anomaly detection (compare current metric vs sliding window average)
- Session analytics (group events within a time window per user)
- Real-time leaderboards (continuously updated aggregations)

**Windowing:**
```sql
-- Tumbling window: non-overlapping fixed time segments
SELECT user_id, COUNT(*) as event_count
FROM events
GROUP BY user_id, TUMBLE(event_time, INTERVAL '5' MINUTE);

-- Sliding window: overlapping windows
SELECT user_id, COUNT(*) as event_count
FROM events
GROUP BY user_id, HOP(event_time, INTERVAL '1' MINUTE, INTERVAL '5' MINUTE);
-- HOP(time, slide_interval, window_size)

-- Session window: variable-length, ends after N seconds of inactivity
SELECT user_id, COUNT(*) as session_events
FROM events
GROUP BY user_id, SESSION(event_time, INTERVAL '30' MINUTE);
```

**Flink Checkpointing:**
```
Flink checkpoints state periodically to S3 (or EFS)
If Flink job fails: restarts from last checkpoint, reprocesses records

Tradeoff:
  More frequent checkpoints → more overhead → higher latency
  Less frequent → faster processing → more data to reprocess on failure

Default: checkpoint every 60 seconds (configurable)
```

**Stateful Processing:**
```
Flink maintains state per key across time:
  Example: "count of failed logins per user in last 5 minutes"
  State: HashMap<user_id, List<timestamp>>

Flink manages state in memory + checkpoints to S3
Scales to billions of keys
```

---

### Amazon MSK vs Kinesis

**When to use Kafka (MSK) over Kinesis:**
| Criteria | MSK (Kafka) | Kinesis |
|----------|-------------|---------|
| Open-source Kafka compliance | Yes — same Kafka APIs | No (Kinesis API) |
| Kafka Connect (Debezium, S3 Sink) | Yes — native | No (need Lambda) |
| KSQL / Kafka Streams | Yes | No |
| No vendor lock-in requirement | Yes | No (AWS-proprietary) |
| Message size limit | 1 MB default (configurable up to 10 MB) | 1 MB per record |
| Retention | Configurable (hours to indefinite) | 24 hr – 365 days |
| Throughput scaling | Add brokers/partitions | Add shards |
| Operations | Broker management (MSK manages ZooKeeper) | Fully managed |
| Cost model | Per broker hour | Per shard hour |

**Kinesis vs MSK Comparison:**
| | Kinesis | MSK |
|-|---------|-----|
| Provisioning | Shards | Brokers + partitions |
| Scaling | Shard split/merge | Add brokers, rebalance partitions |
| Consumers | Lambda (event source), KCL, SDK | Any Kafka consumer |
| Serverless | Kinesis: No / Data Streams Serverless: Yes | MSK Serverless: Yes |
| Managed ZooKeeper | N/A | Yes (MSK manages) |
| Ecosystem | AWS-native | Kafka ecosystem |
| Default message retention | 24 hours (up to 365 days) | Configurable |

**MSK Connect (Managed Kafka Connect):**
```
Connectors run on managed workers (you provide JAR/ZIP):

  Debezium Source Connector:
    MySQL/PostgreSQL → MSK (CDC events)
    Use: capture every DB change as Kafka event

  S3 Sink Connector:
    MSK → S3 (data lake ingestion)
    Format: JSON, Avro, Parquet
    Partitioning: by time or by field value

  OpenSearch Sink Connector:
    MSK → OpenSearch (real-time search index)
```

---

### Amazon OpenSearch Service

**What it is:** Managed Elasticsearch (forked at v7.10) + OpenSearch + Kibana/OpenSearch Dashboards.

**Use Cases:**
- Log analytics (application logs, security logs)
- Full-text search (product search, document search)
- Real-time monitoring dashboards
- Security analytics (SIEM)

**Ingest Methods:**
```
CloudWatch Logs → Subscription filter → Lambda → OpenSearch
Kinesis Firehose → OpenSearch (direct delivery destination)
MSK → MSK Connect → OpenSearch Sink Connector
Custom app → OpenSearch REST API (HTTP)
```

**Index State Management (ISM):**
```
Automatically manage index lifecycle:
  Day 0-7:   index in hot storage (SSD)
  Day 7-30:  move to warm storage (HDD, cheaper)
  Day 30-90: move to cold storage (S3-backed, cheapest)
  Day 90:    delete index

ISM policy:
{
  "policy": {
    "states": [
      {"name": "hot", "transitions": [{"state_name": "warm", "conditions": {"min_index_age": "7d"}}]},
      {"name": "warm", "transitions": [{"state_name": "delete", "conditions": {"min_index_age": "90d"}}]},
      {"name": "delete", "actions": [{"delete": {}}]}
    ]
  }
}
```

---

### Amazon QuickSight

**What it is:** Serverless BI and visualization — dashboards, reports, ML-powered insights.

**SPICE (Super-fast Parallel In-memory Calculation Engine):**
```
SPICE = QuickSight's in-memory data store
  - Import data from S3/Redshift/RDS into SPICE
  - Queries run against in-memory cache (fast, no DB load)
  - Capacity: 10 GB per user (purchasable in increments)
  - Refresh: manual or scheduled

Direct query: Query source DB on each dashboard load (no SPICE needed)
  - Use for: real-time data requirement
  - Slower than SPICE; places load on source
```

**ML-Powered Features:**
```
Anomaly Detection: ML detects unusual patterns in metrics
Forecasting:       Time-series forecast with confidence intervals
NL Queries:        Ask "What was revenue in Q1?" in natural language
Auto-narratives:   Auto-generated text summaries of charts
```

**Row-Level Security (RLS):**
```
Restrict what data each user sees:
  Create "RLS dataset" mapping: username → filter values

  username       | region
  john@company.com | us-east
  jane@company.com | eu-west

QuickSight applies: WHERE region = '<user's region>' on every query
```

---

## 4. Key Config & Limits

| Service | Parameter | Value |
|---------|-----------|-------|
| Kinesis | Per-shard write | 1 MB/sec or 1,000 records/sec |
| Kinesis | Per-shard read (shared) | 2 MB/sec total |
| Kinesis | Per-shard read (EFO) | 2 MB/sec per consumer |
| Kinesis | Max record size | 1 MB |
| Kinesis | Default retention | 24 hours |
| Kinesis | Max retention | 365 days |
| Firehose | Min buffer interval | 60 seconds |
| Firehose | Max buffer size | 128 MB |
| MSK | Default max message size | 1 MB (configurable) |
| OpenSearch | Min domain cost | ~$50/month (t3.small.search) |
| QuickSight | SPICE per user | 10 GB |
| Flink (KDA) | Checkpoint interval | Configurable (default 60s) |

---

## 5. Decision Tree

### Kinesis vs MSK
```
Need Kafka APIs or Kafka ecosystem tools (Connect, Streams, KSQL)?
├─ YES → MSK
└─ NO
   └─ Need open-source / no vendor lock-in?
      ├─ YES → MSK
      └─ NO
         └─ Fully AWS-managed, no Kafka expertise on team?
            └─ Kinesis Data Streams
```

### Athena vs Redshift vs OpenSearch for Analytics
```
What type of analysis?
├─ Log search, full-text search, real-time document search → OpenSearch
├─ Ad-hoc SQL on S3, occasional queries → Athena
├─ Complex SQL, high concurrency, loaded structured data → Redshift
└─ Real-time stream processing → Kinesis Analytics (Flink)
```

### Firehose vs Kinesis Streams
```
Need to process/transform records before delivery?
├─ YES, complex stateful processing → Kinesis Streams + Flink
├─ YES, simple transform → Firehose + Lambda
└─ NO, just deliver to S3/Redshift/OpenSearch → Firehose alone
```

---

## 6. Common Patterns

### Pattern 1: IoT Sensor Pipeline
```
IoT Device
  → AWS IoT Core (MQTT)
  → IoT Rule → Kinesis Data Streams
  → Lambda (alert if threshold exceeded)
  → Kinesis Firehose (parallel)
    → S3 (raw archive, Parquet)
    → Athena query (historical analysis)
    → QuickSight dashboard (operational view)
```

### Pattern 2: Application Log Analytics
```
EC2/ECS Applications
  → CloudWatch Logs
  → Subscription filter → Kinesis Data Firehose
  → Lambda transform (parse log fields)
  → S3 (partitioned by app/date/hour)
  → Glue Crawler → Athena (log analysis)

Parallel path for real-time:
  → Firehose → OpenSearch Service
  → Kibana/OpenSearch Dashboards (live log search)
```

### Pattern 3: CDC Database Change Streaming
```
MySQL RDS
  → Debezium (MSK Connect source connector)
  → MSK topic: "orders.changes"
  → MSK Connect sink connector → S3 (data lake)
  → MSK Connect sink connector → Redshift (data warehouse)
  → Flink job → real-time aggregations → DynamoDB (API serving)

Result: DB changes appear in data warehouse within seconds
```

### Pattern 4: Real-Time Fraud Detection
```
Payment event → Kinesis Data Streams
  → Flink (Kinesis Data Analytics)
    State: user's last 10 transactions in sliding window
    Rule: 3+ transactions in 1 minute from different countries?
    → Fraud signal → SNS → block transaction API

  → Firehose (parallel, all transactions)
    → S3 → Athena (fraud investigation queries)
```

### Pattern 5: Complete Real-Time Analytics Platform
```
INGEST:
  App events → Kinesis Data Streams (partitioned by user_id)
  DB changes → MSK (Debezium CDC)

PROCESS:
  Kinesis → Flink → session analysis, aggregations
  MSK → Lambda → simple enrichment

DELIVER:
  Flink → DynamoDB (real-time API serving, e.g., "user's current score")
  Flink → Firehose → S3 Parquet (for batch analysis)
  MSK → S3 Sink → data lake

VISUALIZE:
  S3 → Athena → QuickSight SPICE (daily refresh dashboards)
  OpenSearch + Dashboards (real-time log/event monitoring)
```

---

## 7. Gotchas

| Gotcha | Detail |
|--------|--------|
| **Hot partition = throttling** | Low-cardinality partition keys (country, boolean) send all traffic to 1-2 shards. ProvisionedThroughputExceededException starts. Use high-cardinality keys or random suffix. |
| **OpenSearch minimum cost ~$50/month** | Even a t3.small.search single-node domain costs ~$50/month. No true free tier. Use Athena for log analysis if cost-sensitive. |
| **QuickSight SPICE capacity limits** | Default 10 GB per user. Large datasets exceed this — either buy more SPICE or use direct query mode. |
| **Firehose minimum 60 second buffer** | Firehose is not for sub-minute latency. If you need 5-second delivery, use Kinesis Streams + Lambda. |
| **Flink checkpointing adds latency** | Checkpointing pauses processing briefly. Tune interval based on recovery time vs latency tradeoff. |
| **MSK storage costs accelerate** | Kafka retains messages until TTL. MSK charges per GB-month. Aggressive retention policies drive storage costs if not monitored. |
| **Kinesis EFO has additional cost** | Enhanced Fan-Out adds $0.015/shard-hour per registered consumer. With 10 consumers × 100 shards = 1,000 additional shard-hours/day. |
| **KPL aggregation incompatible with some consumers** | KPL-aggregated records must be de-aggregated with KCL or Kinesis de-aggregation library. Lambda event source doesn't auto-de-aggregate KPL records. |
| **MSK Serverless partition limit** | MSK Serverless has lower throughput limits than provisioned MSK. Check partition count limits before large deployments. |
| **Flink at-least-once vs exactly-once** | Exactly-once requires checkpointing + idempotent sinks. At-least-once is the default. Design consumers to handle duplicate records. |

---

## 8. Hands-On Lab — Free Tier

**Goal:** Build a streaming pipeline: Kinesis Data Streams → Lambda → DynamoDB.

### Step 1: Create Kinesis Data Stream
```
Kinesis Console → Data Streams → Create data stream
  Name: lab-stream
  Capacity mode: Provisioned
  Number of shards: 1

(Kinesis: no free tier — ~$0.015/shard-hour. 1 shard for 1 hour = $0.015)
```

### Step 2: Create DynamoDB Table (target)
```
DynamoDB Console → Create table
  Table name: streaming-events
  Partition key: event_id (String)
  Settings: Free tier (on-demand)
```

### Step 3: Create Lambda Function (processor)
```python
import json
import boto3
import base64
import uuid

dynamodb = boto3.resource('dynamodb')
table = dynamodb.Table('streaming-events')

def lambda_handler(event, context):
    for record in event['Records']:
        # Decode Kinesis record
        payload = base64.b64decode(record['kinesis']['data']).decode('utf-8')
        data = json.loads(payload)

        # Store in DynamoDB
        table.put_item(Item={
            'event_id': str(uuid.uuid4()),
            'sequence_number': record['kinesis']['sequenceNumber'],
            'partition_key': record['kinesis']['partitionKey'],
            'data': payload,
            'timestamp': record['kinesis']['approximateArrivalTimestamp']
        })
        print(f"Processed: {data}")

    return {'statusCode': 200}
```

### Step 4: Configure Lambda Event Source Mapping
```
Lambda Console → (your function) → Add trigger
  Source: Kinesis
  Stream: lab-stream
  Batch size: 10
  Starting position: TRIM_HORIZON (read from beginning)
  → Add trigger
```

### Step 5: Send Test Data to Stream
```python
import boto3
import json
import time

client = boto3.client('kinesis', region_name='us-east-1')

for i in range(10):
    record = {
        'event_type': 'page_view',
        'user_id': f'user-{i % 3}',  # 3 users
        'page': f'/page-{i}',
        'timestamp': time.time()
    }

    response = client.put_record(
        StreamName='lab-stream',
        Data=json.dumps(record),
        PartitionKey=record['user_id']  # user_id as partition key
    )
    print(f"Sent: {record['page']} → shard {response['ShardId']}")
    time.sleep(0.1)
```

### Step 6: Verify in DynamoDB
```
DynamoDB Console → streaming-events → Explore items
→ Should see 10 records with decoded event data

Check Lambda logs:
CloudWatch → Log groups → /aws/lambda/your-function
→ Should see "Processed: {...}" for each record
```

### Step 7: Clean Up
```
# Delete Lambda trigger (event source mapping)
# Delete Lambda function
# Delete Kinesis stream (stops $0.015/hr charge)
# Delete DynamoDB table
```

---

## Summary Reference Card

```
STREAMING SERVICES:
  Kinesis Data Streams: per-shard (1MB/s write, 2MB/s read)
    Hot partition: use high-cardinality partition key
    EFO: dedicated 2MB/s per registered consumer
    KPL: aggregate records to reduce costs (adds latency)

  Kinesis Firehose: delivery to S3/Redshift/OpenSearch
    Min buffer: 60 seconds (not for sub-minute needs)
    Lambda transform: enrich/filter in flight
    Format conversion: JSON → Parquet (saves Athena costs)

  MSK (Kafka): open-source Kafka on AWS
    Use over Kinesis: Kafka ecosystem, KSQL, Connect, no lock-in
    MSK Connect: Debezium (CDC), S3 Sink, OpenSearch Sink

  Kinesis Analytics (Flink): stateful stream processing
    Windowing: tumbling / sliding / session
    Checkpoint: fault tolerance, adds latency

  OpenSearch: log analytics, full-text search (~$50/mo minimum)
  QuickSight: serverless BI, SPICE in-memory cache, ML insights

PIPELINE EXAMPLES:
  IoT: IoT Core → Kinesis → Lambda + Firehose → S3 → Athena
  Logs: CloudWatch → Firehose → S3 + OpenSearch
  CDC: MySQL → MSK Connect (Debezium) → MSK → S3 + Redshift
```
