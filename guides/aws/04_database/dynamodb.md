# Amazon DynamoDB — SAP-C02 Deep Dive

---

## 1. The Problem — Why Key-Value / Document at Scale

Relational databases scale **vertically** — when you outgrow one server, you buy a bigger one. This hits a hard ceiling:

| Relational DB Scaling Problem | Impact |
|---|---|
| Single primary writer (for ACID) | All writes bottleneck on one host |
| Schema joins span all rows | Full table scans kill performance at billions of rows |
| Connection-based (TCP sessions) | Max connections ~thousands; Lambda functions = death |
| Storage on one SAN/NAS | Capacity and IOPS ceiling |
| Sharding manually | Huge operational burden, resharding is painful |

For use cases needing **millisecond latency at any scale** (shopping carts, session stores, user profiles, IoT device state, gaming leaderboards), a purpose-built key-value store outperforms a relational engine.

DynamoDB was built to solve Amazon's own Black Friday problem — single-digit millisecond reads/writes at millions of requests per second with zero operational management.

---

## 2. What AWS Built — Core Definition

**Amazon DynamoDB** is a fully managed, serverless, key-value and document NoSQL database. It provides:
- Consistent **single-digit millisecond** response at any scale
- **Automatic sharding** across storage nodes (no manual partitioning)
- **Serverless** — no instances to provision; pay for capacity consumed
- Built-in **replication** across 3 AZs in a region
- Optional **global replication** across multiple regions

Data model: tables contain items (rows); items contain attributes (columns). The only required attributes are those forming the primary key. All other attributes are schema-free.

---

## 3. How It Works — Internals and Components

### 3.1 Data Model

```
Table: Orders
┌─────────────────────────────────────────────────────────────────┐
│  PK (partition key)  SK (sort key)   Attributes                  │
├─────────────────────────────────────────────────────────────────┤
│  USER#alice          ORDER#001        {status: "shipped", ...}  │
│  USER#alice          ORDER#002        {status: "pending", ...}  │
│  USER#bob            ORDER#001        {status: "delivered", ...}│
│  USER#bob            PROFILE#1        {email: "bob@..."}        │
└─────────────────────────────────────────────────────────────────┘
```

- **Schemaless** except for the primary key attributes
- Items can have different sets of attributes (flexible schema)
- Max item size: **400 KB**
- Attribute types: String, Number, Binary, Boolean, Null, List, Map, String Set, Number Set, Binary Set

### 3.2 Primary Key Types

| Type | Structure | Example | Use When |
|---|---|---|---|
| Simple (Hash only) | Partition Key only | `userId` | Each item uniquely identified by one attribute |
| Composite (Hash + Range) | Partition Key + Sort Key | `userId` + `timestamp` | Multiple items per partition; sort/range queries needed |

**Partition Key selection is critical.** DynamoDB uses consistent hashing on the partition key to distribute items across internal storage partitions.

### 3.3 Partitioning and Hot Partition Problem

```
Partition Key → Hash Function → Partition Node

"user_123" → hash(0x3A...) → Partition 1
"user_456" → hash(0x7C...) → Partition 2
"user_789" → hash(0x1F...) → Partition 3
```

- Each partition holds up to **10 GB** of data
- Each partition provides up to **3,000 RCU** and **1,000 WCU**
- DynamoDB automatically splits partitions as data grows

**Hot Partition Problem:**
```
Bad partition key: "status" = ["active", "inactive"]
→ All "active" writes go to 2 partitions
→ Those partitions get throttled even if total table capacity is fine
→ Error: ProvisionedThroughputExceededException

Good partition key: "userId" (high cardinality, uniform distribution)
→ Writes spread evenly across all partitions
→ No single partition becomes a bottleneck
```

Solutions for hot partitions:
- **Write sharding**: append random suffix to partition key (e.g., `status#3`) then aggregate reads
- **Composite key design**: make the PK more specific
- **DAX caching**: absorb hot read spikes

### 3.4 Secondary Indexes

#### Local Secondary Index (LSI)

| Property | Value |
|---|---|
| Alternate key structure | Same partition key, **different sort key** |
| When to create | **Only at table creation** — cannot add later |
| Capacity sharing | Shares RCU/WCU with the base table |
| Consistency | Strongly consistent reads supported |
| Max per table | 5 |
| Item collections | All items with same PK across table + LSI must fit in 10 GB |

```
Base table: PK=userId, SK=orderId
LSI:        PK=userId, SK=orderDate  ← query all orders for a user sorted by date
```

#### Global Secondary Index (GSI)

| Property | Value |
|---|---|
| Alternate key structure | **Different partition key**, optional different sort key |
| When to create | **Any time** — add to existing tables |
| Capacity | **Own separate RCU/WCU** (or on-demand) |
| Consistency | **Eventually consistent only** — no strongly consistent GSI reads |
| Max per table | 20 (default; can request increase) |
| Throttling | GSI can throttle independently from the table |

```
Base table: PK=userId, SK=orderId
GSI:        PK=productId, SK=orderDate  ← query all orders for a product sorted by date
```

> **Critical exam distinction:** LSI = same PK, different SK, at creation, shares capacity, supports strong consistency. GSI = different PK, any time, own capacity, eventual consistency only.

### 3.5 Read Consistency

| Type | Cost | Freshness | When to Use |
|---|---|---|---|
| Eventually Consistent (default) | 1 RCU per 4 KB | May be slightly stale (milliseconds) | Most reads; higher throughput |
| Strongly Consistent | **2 RCU per 4 KB** | Always latest committed data | After a write when you must read your own write |

> Strongly consistent reads cost **2x** the RCU. GSIs never support strongly consistent reads.

### 3.6 Capacity Modes

#### Provisioned Capacity

```
You specify:
  RCU (Read Capacity Units)   — 1 RCU = 1 strongly consistent read of ≤4KB/s
                                       OR 2 eventually consistent reads of ≤4KB/s
  WCU (Write Capacity Units)  — 1 WCU = 1 write of ≤1KB/s

Auto Scaling:
  Set min/max RCU and WCU
  CloudWatch alarms trigger scaling
  Scale up quickly; scale down slowly (limited to 4 decreases per 24hr per table)
```

#### On-Demand Capacity

- Pay per request: `$0.25 per million reads`, `$1.25 per million writes` (approximate)
- No capacity planning needed
- Scales automatically to handle traffic spikes
- More expensive per request at steady-state compared to right-sized provisioned
- **No ProvisionedThroughputExceededException** — requests always succeed (with some throttling limits)

| Comparison | Provisioned + Auto Scaling | On-Demand |
|---|---|---|
| Predictable traffic | Better (lower cost) | Over-priced |
| Spiky / unknown traffic | Risk of throttling gaps | Better (always handles bursts) |
| New tables | Start on-demand | — |
| Cost optimization | Tune RCU/WCU to actual usage | Simpler ops |

### 3.7 RCU/WCU Calculation Formulas

**RCU Formula:**
```
RCU needed = CEILING(item_size_KB / 4) × requests_per_second
  (multiply by 2 if strongly consistent)

Example:
  Item size = 6 KB
  10 eventually consistent reads/sec
  → CEILING(6/4) = 2 RCU per read
  → 2 × 10 = 20 RCU (eventually consistent)
  → 40 RCU (strongly consistent)
```

**WCU Formula:**
```
WCU needed = CEILING(item_size_KB / 1) × writes_per_second

Example:
  Item size = 3.5 KB
  5 writes/sec
  → CEILING(3.5/1) = 4 WCU per write
  → 4 × 5 = 20 WCU
```

**Transactional reads/writes cost 2x:**
```
Transactional read:  2 RCU per 4 KB (same as strongly consistent)
Transactional write: 2 WCU per 1 KB
```

### 3.8 DynamoDB Streams

```
Table write (PUT/UPDATE/DELETE)
        │
        ▼
┌───────────────────────┐
│  DynamoDB Stream       │
│  Ordered change log    │
│  24-hour retention     │
│  Shard-based           │
└───────────┬───────────┘
            │
            ▼ trigger
      Lambda Function
      (process change, fan-out, audit, search indexing)
```

Stream record types:
| View Type | What it contains |
|---|---|
| `KEYS_ONLY` | Only the key attributes of the modified item |
| `NEW_IMAGE` | The entire item after the change |
| `OLD_IMAGE` | The entire item before the change |
| `NEW_AND_OLD_IMAGES` | Both old and new item |

- Ordered within a shard (same partition key items go to same shard)
- Lambda reads from stream via event source mapping
- Common use: replicate to OpenSearch, invalidate cache, trigger workflows, cross-region sync (basis for Global Tables)

### 3.9 Global Tables

```
us-east-1                    eu-west-1                   ap-southeast-1
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│  DynamoDB Table  │◄───────▶│  DynamoDB Table  │◄───────▶│  DynamoDB Table  │
│  (read + write)  │         │  (read + write)  │         │  (read + write)  │
└─────────────────┘         └─────────────────┘         └─────────────────┘
         ▲                           ▲                           ▲
         │                           │                           │
    App (US)                    App (EU)                  App (APAC)
```

- **Active-active** — write to any region, all regions sync
- Built on DynamoDB Streams (each region streams to others)
- Conflict resolution: **last-writer-wins** based on timestamp — no manual conflict resolution
- Typical replication lag: **under 1 second** (sub-second)
- Requires: table must use **on-demand** OR **provisioned with auto scaling enabled** (not static provisioned)
- Use case: globally distributed apps needing local write latency

> **Exam gotcha:** Global Tables requires on-demand or auto-scaling provisioned. Static provisioned capacity is not supported.

### 3.10 DAX — DynamoDB Accelerator

```
Application
     │
     ▼
┌──────────────────────────────┐
│  DAX Cluster (in-memory)      │
│  - Item cache (eventual only) │
│  - Query/Scan cache           │
│  - Write-through to DynamoDB  │
│  - Microsecond reads          │
└──────────────┬───────────────┘
               │ cache miss → read from DynamoDB
               ▼
          DynamoDB Table
```

| Feature | Value |
|---|---|
| Latency | Microseconds (vs single-digit milliseconds for DynamoDB) |
| Cache type | Write-through (writes go to both DAX and DynamoDB) |
| Consistency | **Eventually consistent only** — no strongly consistent reads through DAX |
| Use case | Read-heavy, read-hot-key workloads |
| Not for | Strongly consistent reads, write-heavy workloads, financial balances |
| Cluster size | Minimum 3 nodes (for Multi-AZ) |
| TTL | Per item; configurable |

> **Critical gotcha:** DAX does NOT support strongly consistent reads. If your app needs to read its own writes immediately after writing, bypass DAX and read directly from DynamoDB.

### 3.11 TTL — Time To Live

- Define a TTL attribute (Number type, Unix epoch timestamp in seconds)
- DynamoDB automatically deletes items when current time > TTL value
- Deletion is **eventual** — up to **48 hours** after TTL expiry (not instant)
- Deleted items still appear in reads until actually deleted
- **No WCU consumed** for TTL deletions
- Deleted items appear in DynamoDB Streams (for audit/archive)
- Use case: session data, temporary tokens, shopping cart abandonment, rate limiting windows

```python
import time
item = {
    'userId': 'user123',
    'sessionToken': 'abc...',
    'ttl': int(time.time()) + 3600  # expire in 1 hour
}
table.put_item(Item=item)
```

### 3.12 Transactions

- **ACID transactions** across multiple items and tables
- `TransactWriteItems`: up to **100 items** (was 25 before 2023 increase), max **4 MB** total
- `TransactGetItems`: up to **100 items**, max **4 MB** total
- **Cost: 2x RCU/WCU** vs non-transactional operations
- Idempotency: provide `ClientRequestToken` to prevent duplicate processing
- Use case: banking transfers (debit account A, credit account B atomically)

```python
response = dynamodb.transact_write_items(
    TransactItems=[
        {
            'Update': {
                'TableName': 'Accounts',
                'Key': {'accountId': {'S': 'account-A'}},
                'UpdateExpression': 'SET balance = balance - :amount',
                'ConditionExpression': 'balance >= :amount',
                'ExpressionAttributeValues': {':amount': {'N': '100'}}
            }
        },
        {
            'Update': {
                'TableName': 'Accounts',
                'Key': {'accountId': {'S': 'account-B'}},
                'UpdateExpression': 'SET balance = balance + :amount',
                'ExpressionAttributeValues': {':amount': {'N': '100'}}
            }
        }
    ]
)
```

### 3.13 Single-Table Design

Instead of one table per entity (relational style), single-table design stores all entity types in one table using a generic PK/SK scheme:

```
PK              SK                   Type       Attributes
USER#alice      PROFILE#1            USER       {email, name, ...}
USER#alice      ORDER#2024-001       ORDER      {total, status, ...}
USER#alice      ORDER#2024-002       ORDER      {total, status, ...}
PRODUCT#p1      DETAILS#1            PRODUCT    {name, price, ...}
ORDER#2024-001  ITEM#1               ORDER_ITEM {qty, productId, ...}
```

**Adjacency List Pattern** — relationships stored as items with reverse indexes:

```
Base table: USER#alice → ORDER#001 (user owns order)
GSI:        PK=ORDER#001 → SK=USER#alice (lookup user from order)
```

**GSI Overloading** — same GSI serves multiple access patterns by using different entity types in the GSI PK:

```
GSI PK attribute = "gsi1pk"
For users:    gsi1pk = "EMAIL#alice@example.com"
For products: gsi1pk = "CATEGORY#electronics"
Same GSI handles both access patterns
```

### 3.14 Backup and Restore

| Type | Details |
|---|---|
| On-demand backup | Manual, retained until deleted, no impact on table performance |
| PITR (Point-in-Time Recovery) | Continuous backups; restore to any second in last **35 days** |
| Restore | Creates a **new table** (not in-place); can restore to same or different region |
| Export to S3 | Export table to S3 in DynamoDB JSON or ION format; no RCU consumed |

### 3.15 PartiQL

- SQL-compatible query language for DynamoDB
- Execute SELECT, INSERT, UPDATE, DELETE using SQL-like syntax
- Supports batch and transactional operations
- Does NOT make DynamoDB relational — still partition key based under the hood
- Good for: ad-hoc queries, migration from SQL, familiarity

```sql
SELECT * FROM Orders WHERE userId = 'alice' AND orderId BETWEEN 'ORDER#001' AND 'ORDER#999';
```

---

## 4. Key Config and Limits

| Parameter | Value |
|---|---|
| Max item size | 400 KB |
| Max partition size | 10 GB |
| Partition capacity | 3,000 RCU + 1,000 WCU |
| Max LSIs per table | 5 |
| Max GSIs per table | 20 (default) |
| LSI must be created | At table creation only |
| GSI can be added | Any time |
| Stream retention | 24 hours |
| TTL deletion lag | Up to 48 hours |
| Transaction items max | 100 items, 4 MB |
| Transaction cost | 2x RCU/WCU |
| PITR retention | 35 days |
| Global Tables replication lag | < 1 second |
| DAX minimum nodes | 3 (Multi-AZ) |
| Strongly consistent GSI | Not supported |
| On-Demand capacity switch cooldown | Can switch once per 24hr from on-demand to provisioned |

---

## 5. Decision Tree

```
Need a database?
│
├─ Relational, complex queries, joins, transactions across entities?
│   └─ Use RDS or Aurora
│
├─ Key-value or document access patterns?
│   ├─ Need millisecond latency at any scale?
│   ├─ Unknown schema / flexible attributes?
│   ├─ Serverless / no capacity planning?
│   └─ → DynamoDB
│
└─ DynamoDB — which sub-features?
    │
    ├─ Need to query by non-PK attribute?
    │   ├─ Same PK, different sort key, created at table creation → LSI
    │   └─ Different PK, add any time, eventual consistency → GSI
    │
    ├─ Traffic pattern is predictable → Provisioned + Auto Scaling
    ├─ Traffic pattern is unknown/spiky → On-Demand
    │
    ├─ Read-heavy with hot keys → DAX
    │   (NOT if strongly consistent reads needed)
    │
    ├─ Need to react to changes (event-driven) → DynamoDB Streams + Lambda
    │
    ├─ Multi-region active-active → Global Tables
    │   (requires on-demand or auto-scaling provisioned)
    │
    ├─ Temporary data with auto-expiry → TTL
    │
    └─ Multi-item ACID operations → Transactions (2x cost)
```

### DynamoDB vs RDS Decision

| Factor | Choose DynamoDB | Choose RDS/Aurora |
|---|---|---|
| Access pattern | Known, simple (PK-based) | Complex queries, ad-hoc SQL |
| Scale | Massive (millions TPS) | Moderate (tens of thousands TPS) |
| Data model | Key-value, document | Relational with joins |
| Schema | Flexible, schemaless | Fixed schema with foreign keys |
| Transactions | Single-table or 2-table ACID | Complex multi-table ACID |
| Latency | Sub-millisecond with DAX | Milliseconds |
| Operational overhead | Zero (serverless) | Some (Multi-AZ, parameter tuning) |

---

## 6. Common Patterns

### Pattern 1: Session Store

```
User Login → App generates session token
                  │
                  ▼ PutItem
        DynamoDB (sessionId as PK, TTL = 24hr)
                  │
    Subsequent requests → GetItem(sessionId)
                  │
    Session expired → TTL auto-deletes → user must re-login
```

### Pattern 2: Event-Driven Architecture with Streams

```
DynamoDB Table (orders)
        │
        │ Stream (NEW_AND_OLD_IMAGES)
        ▼
   Lambda Function
        │
        ├── Update OpenSearch index (for search)
        ├── Send SNS notification (order shipped event)
        └── Write to S3 audit log
```

### Pattern 3: Rate Limiting with Atomic Counter

```python
# Increment counter; condition: must be < 100 (rate limit)
response = table.update_item(
    Key={'userId': user_id, 'window': current_minute},
    UpdateExpression='ADD requestCount :inc',
    ConditionExpression='requestCount < :limit',
    ExpressionAttributeValues={':inc': 1, ':limit': 100},
    ReturnValues='UPDATED_NEW'
)
```

### Pattern 4: Leaderboard with GSI

```
Table: GameScores
PK = userId, SK = gameId
Attributes: score, timestamp

GSI:
  PK  = gameId
  SK  = score
  → Query: "top 10 players in game X" = GSI query on gameId, sort by score DESC
```

### Pattern 5: Global Tables for Multi-Region Active-Active

```
us-east-1: Write user profile (userId=alice, email=alice@...)
    ↓ replicated in <1 second
eu-west-1: European users read alice's profile locally
ap-northeast-1: APAC users read alice's profile locally
```

Config requirements:
```
1. Enable Global Tables on the base table
2. Add replica regions
3. Table must use on-demand OR provisioned with auto scaling
4. DynamoDB Streams must be enabled (auto-enabled by Global Tables)
```

---

## 7. Gotchas — Exam Tricks and Production Pitfalls

### Gotcha 1: Hot Partitions — Low Cardinality Partition Keys
- Using `status`, `country`, `boolean_flag` as PK → massive hot partition
- Symptom: `ProvisionedThroughputExceededException` on specific partitions even when total table capacity is sufficient
- Fix: use high-cardinality keys; add randomization suffix; redesign data model

### Gotcha 2: GSI Throttling Is Separate from the Table
- Table writes that update a GSI consume WCU from the GSI's separate capacity
- If GSI WCU is lower than table WCU, the GSI throttles — causing table writes to throttle too
- Always provision GSI capacity proportional to the write patterns that touch that GSI

### Gotcha 3: DAX Does NOT Support Strongly Consistent Reads
- DAX always returns cached data — which is eventually consistent
- If you call `GetItem` with `ConsistentRead=true` through DAX, it bypasses the cache and goes to DynamoDB directly
- Design: use DAX for eventually consistent hot reads; bypass DAX for financial/critical reads

### Gotcha 4: Global Tables Requires On-Demand or Auto Scaling Provisioned
- Static provisioned capacity tables cannot be added to Global Tables
- Common exam trap: question enables Global Tables on a static provisioned table — wrong, needs on-demand or auto scaling

### Gotcha 5: TTL Deletion Is Eventual — Up to 48 Hours
- TTL-expired items may still be returned in reads for up to 48 hours
- Use a filter expression to exclude expired items in your application if exact TTL cutoff matters:
```python
response = table.query(
    ...,
    FilterExpression=Attr('ttl').gte(int(time.time()))
)
```

### Gotcha 6: LSI Cannot Be Added After Table Creation
- You think "I'll add the LSI later" — you cannot
- Must be planned at table creation; if you need one after the fact, recreate the table

### Gotcha 7: GSI Always Eventually Consistent
- Cannot do strongly consistent reads on a GSI — period
- If you need strongly consistent lookup by non-PK attribute, use the base table's PK or redesign

### Gotcha 8: Scan Is Expensive — Avoid in Production
- `Scan` reads every item in the table; consumes RCU for all data scanned even if filtered
- A scan on a 100 GB table = massive RCU cost
- Always use `Query` with a PK; use GSI if needed for alternate access patterns

### Gotcha 9: Item Size Limit Is 400 KB
- Large documents (images, PDFs) don't belong in DynamoDB
- Pattern: store large blobs in S3; store S3 key reference in DynamoDB

### Gotcha 10: Transactions Are 2x Cost and Have Limits
- 100 items max, 4 MB max per transaction
- Consider whether you truly need ACID: often eventual consistency + idempotent operations are sufficient and much cheaper

---

## 8. Hands-On Lab — Free Tier Step-by-Step

### Lab Goal
Create a DynamoDB table with LSI and GSI, load data, test queries, enable Streams, configure DAX, and test TTL.

### Prerequisites
- AWS account (DynamoDB free tier: 25 GB storage, 25 WCU, 25 RCU per month — sufficient for this lab)

---

### Step 1: Create Table with LSI

```bash
aws dynamodb create-table \
  --table-name GameScores \
  --attribute-definitions \
    AttributeName=userId,AttributeType=S \
    AttributeName=gameId,AttributeType=S \
    AttributeName=score,AttributeType=N \
    AttributeName=timestamp,AttributeType=S \
  --key-schema \
    AttributeName=userId,KeyType=HASH \
    AttributeName=gameId,KeyType=RANGE \
  --local-secondary-indexes '[
    {
      "IndexName": "ScoreIndex",
      "KeySchema": [
        {"AttributeName": "userId", "KeyType": "HASH"},
        {"AttributeName": "score", "KeyType": "RANGE"}
      ],
      "Projection": {"ProjectionType": "ALL"}
    }
  ]' \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1
```

Wait for table to be active:
```bash
aws dynamodb wait table-exists --table-name GameScores
```

### Step 2: Add GSI (After Table Creation)

```bash
aws dynamodb update-table \
  --table-name GameScores \
  --attribute-definitions \
    AttributeName=gameId,AttributeType=S \
    AttributeName=score,AttributeType=N \
  --global-secondary-index-updates '[
    {
      "Create": {
        "IndexName": "GameLeaderboard",
        "KeySchema": [
          {"AttributeName": "gameId", "KeyType": "HASH"},
          {"AttributeName": "score", "KeyType": "RANGE"}
        ],
        "Projection": {"ProjectionType": "INCLUDE",
                       "NonKeyAttributes": ["userId", "timestamp"]},
        "BillingMode": "PAY_PER_REQUEST"
      }
    }
  ]'
```

### Step 3: Insert Sample Data

```bash
# Insert multiple items
aws dynamodb batch-write-item --request-items '{
  "GameScores": [
    {"PutRequest": {"Item": {
      "userId": {"S": "alice"},
      "gameId": {"S": "tetris"},
      "score": {"N": "9500"},
      "timestamp": {"S": "2024-01-15T10:00:00Z"},
      "level": {"N": "12"}
    }}},
    {"PutRequest": {"Item": {
      "userId": {"S": "bob"},
      "gameId": {"S": "tetris"},
      "score": {"N": "7200"},
      "timestamp": {"S": "2024-01-15T11:00:00Z"},
      "level": {"N": "9"}
    }}},
    {"PutRequest": {"Item": {
      "userId": {"S": "alice"},
      "gameId": {"S": "pacman"},
      "score": {"N": "15000"},
      "timestamp": {"S": "2024-01-15T12:00:00Z"},
      "level": {"N": "5"}
    }}}
  ]
}'
```

### Step 4: Query Patterns

```bash
# Query 1: All games for alice (base table)
aws dynamodb query \
  --table-name GameScores \
  --key-condition-expression "userId = :uid" \
  --expression-attribute-values '{":uid": {"S": "alice"}}'

# Query 2: Alice's scores > 5000 (LSI: ScoreIndex)
aws dynamodb query \
  --table-name GameScores \
  --index-name ScoreIndex \
  --key-condition-expression "userId = :uid AND score > :min" \
  --expression-attribute-values '{":uid": {"S": "alice"}, ":min": {"N": "5000"}}'

# Query 3: Top scores for Tetris (GSI: GameLeaderboard)
aws dynamodb query \
  --table-name GameScores \
  --index-name GameLeaderboard \
  --key-condition-expression "gameId = :gid" \
  --expression-attribute-values '{":gid": {"S": "tetris"}}' \
  --scan-index-forward false  # descending sort by score
```

### Step 5: Enable DynamoDB Streams

```bash
aws dynamodb update-table \
  --table-name GameScores \
  --stream-specification StreamEnabled=true,StreamViewType=NEW_AND_OLD_IMAGES
```

Get stream ARN:
```bash
STREAM_ARN=$(aws dynamodb describe-table \
  --table-name GameScores \
  --query 'Table.LatestStreamArn' \
  --output text)
echo $STREAM_ARN
```

### Step 6: Add TTL to Items

```bash
# Enable TTL on the table
aws dynamodb update-time-to-live \
  --table-name GameScores \
  --time-to-live-specification Enabled=true,AttributeName=expiresAt

# Insert an item that expires in 60 seconds
EXPIRY=$(( $(date +%s) + 60 ))
aws dynamodb put-item \
  --table-name GameScores \
  --item '{
    "userId": {"S": "testuser"},
    "gameId": {"S": "temp-game"},
    "score": {"N": "100"},
    "expiresAt": {"N": "'$EXPIRY'"}
  }'

# Wait ~2 minutes, then check if item is gone (may take up to 48hr actually)
aws dynamodb get-item \
  --table-name GameScores \
  --key '{"userId": {"S": "testuser"}, "gameId": {"S": "temp-game"}}'
```

### Step 7: Test Capacity Modes Switch

```bash
# Switch from on-demand to provisioned
aws dynamodb update-table \
  --table-name GameScores \
  --billing-mode PROVISIONED \
  --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5

# Enable auto scaling on the provisioned table
aws application-autoscaling register-scalable-target \
  --service-namespace dynamodb \
  --resource-id "table/GameScores" \
  --scalable-dimension "dynamodb:table:ReadCapacityUnits" \
  --min-capacity 5 \
  --max-capacity 100
```

### Step 8: Enable PITR

```bash
aws dynamodb update-continuous-backups \
  --table-name GameScores \
  --point-in-time-recovery-specification PointInTimeRecoveryEnabled=true

# Verify
aws dynamodb describe-continuous-backups --table-name GameScores
```

### Step 9: Cleanup

```bash
aws dynamodb delete-table --table-name GameScores
```

---

### Key Observations from Lab

1. **LSI at creation** — you cannot add the `ScoreIndex` LSI after table creation; plan upfront
2. **GSI after creation** — `GameLeaderboard` GSI was added after the table existed using `update-table`
3. **GSI consistency** — queries on `GameLeaderboard` are eventually consistent; there is no `ConsistentRead` option
4. **TTL is not instant** — the test item may still appear for up to 48 hours after its TTL timestamp passes
5. **Streams are sharded** — records for the same partition key always go to the same shard (ordering guarantee)
6. **On-demand vs provisioned** — switching from provisioned to on-demand can only happen once per 24 hours
