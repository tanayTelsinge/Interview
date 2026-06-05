# Amazon ElastiCache — Redis vs Memcached — SAP-C02 Deep Dive

---

## 1. The Problem — Database Bottleneck

Every database has a finite ceiling on how fast it can serve reads:

| DB bottleneck source | Effect |
|---|---|
| Disk I/O for every read | ~1-5ms per query even with SSD indexes |
| CPU for query parsing + execution | Limited parallelism on single nodes |
| Network round-trip | 1-5ms within a VPC |
| Connection limits | RDS MySQL max ~6,000 connections |
| Lock contention on hot rows | Serialized access, queue buildup |

For applications that repeatedly request the same data (product catalog, user session, stock prices, configuration), hitting the database every time is wasteful and slow. Caching solves this by storing computed results in fast in-memory storage so subsequent requests bypass the database entirely.

**ElastiCache** is AWS's fully managed in-memory caching service, offering two engines: **Redis** (rich data structures, persistence, replication) and **Memcached** (pure cache, multi-threaded, simple key-value).

---

## 2. What AWS Built — Core Definition

**Amazon ElastiCache** is a fully managed in-memory data store and cache service that improves application performance by retrieving data from fast, managed in-memory caches instead of relying on slower disk-based databases. It supports two open-source engines:

- **Redis** — Rich data structures, persistence, pub/sub, cluster mode, Multi-AZ with auto-failover
- **Memcached** — Simple multi-threaded key-value cache, no persistence, no replication, pure caching

**ElastiCache Serverless** (launched 2023) — automatically scales capacity, no cluster sizing needed.

---

## 3. How It Works — Internals and Components

### 3.1 Caching Strategies

Choosing the right caching strategy is as important as choosing the cache engine. Each has trade-offs.

#### Strategy 1: Lazy Loading (Cache-Aside)

```
Application
    │
    ├─── 1. Check cache for key
    │
    ├─── Cache HIT → return cached value (fast, no DB)
    │
    └─── Cache MISS:
              │
              ├─ 2. Query database
              ├─ 3. Write result to cache (with TTL)
              └─ 4. Return result to caller
```

```python
def get_user(user_id):
    cached = cache.get(f"user:{user_id}")
    if cached:
        return json.loads(cached)           # Cache HIT

    user = db.query("SELECT * FROM users WHERE id = %s", user_id)  # Cache MISS
    cache.setex(f"user:{user_id}", 3600, json.dumps(user))         # Populate cache
    return user
```

| Pros | Cons |
|---|---|
| Only caches data actually requested | First request is slow (cold cache miss) |
| Cache doesn't fill with unused data | Stale data possible if DB changes |
| Resilient: cache failure just means DB hits | Cache miss storm on cold start / restart |

#### Strategy 2: Write-Through

```
Application write → Cache write (always) + DB write (always)
```

```python
def update_user(user_id, data):
    db.execute("UPDATE users SET ... WHERE id = %s", user_id)  # Write DB
    cache.setex(f"user:{user_id}", 3600, json.dumps(data))     # Write cache
```

| Pros | Cons |
|---|---|
| Cache always has fresh data | Every write hits cache (extra latency) |
| No stale reads | Cache fills with data that may never be read |
| Simple consistency model | Cache churn on write-heavy workloads |

#### Strategy 3: Write-Behind (Write-Back)

```
Application write → Cache write (immediately) → DB write (async, batched)
```

| Pros | Cons |
|---|---|
| Very low write latency | Risk of data loss if cache fails before DB write |
| Batch writes reduce DB load | Complex to implement correctly |
| Good for write-heavy workloads | Eventual consistency between cache and DB |

#### TTL (Time To Live) — Always Use It

- **Always set a TTL** on cached items — without TTL, cache fills up and evicts unpredictably
- TTL creates a self-healing cache: even if data becomes stale, it eventually expires and gets refreshed
- Short TTL = fresher data, more DB load; Long TTL = more DB relief, more staleness
- For immutable data (product images): long TTL (days); For prices: short TTL (minutes)

### 3.2 Redis Deep Dive

#### Redis Data Structures

| Structure | Commands | Use Case |
|---|---|---|
| **String** | GET, SET, INCR, DECR, APPEND | Simple cache, counters, rate limiting |
| **Hash** | HGET, HSET, HMGET, HGETALL | Object caching (user profile fields) |
| **List** | LPUSH, RPUSH, LPOP, LRANGE | Job queues, recent activity feeds |
| **Set** | SADD, SMEMBERS, SINTER, SUNION | Unique tags, follow lists, set operations |
| **Sorted Set** | ZADD, ZRANGE, ZRANGEBYSCORE, ZRANK | Leaderboards, priority queues, time-series |
| **Bitmap** | SETBIT, GETBIT, BITCOUNT | Daily active users, feature flags per user |
| **HyperLogLog** | PFADD, PFCOUNT | Approximate unique count (low memory) |
| **Stream** | XADD, XREAD, XGROUP | Event log, message queue with consumer groups |
| **Geo** | GEOADD, GEODIST, GEORADIUS | Location-based queries |

#### Redis Persistence

| Mode | Description | Durability | Performance impact |
|---|---|---|---|
| **RDB (Redis Database)** | Point-in-time snapshots at intervals | Lower (data between snapshots lost) | Low (background fork) |
| **AOF (Append Only File)** | Log every write command | Higher (configurable fsync) | Medium to high |
| **RDB + AOF** | Both enabled | Highest | Combined overhead |
| **No persistence** | Pure cache, data lost on restart | None | Fastest |

#### Redis Replication Architecture

**Cluster Mode Disabled (Replication Group):**

```
┌─────────────────────────────────────────────────────┐
│  Replication Group (Cluster Mode Disabled)           │
│                                                      │
│  ┌──────────────┐    ┌──────────────┐                │
│  │  Primary      │───▶│  Replica 1   │ AZ-B           │
│  │  (AZ-A)       │    │              │                │
│  │  reads+writes │    └──────────────┘                │
│  └──────────────┘    ┌──────────────┐                │
│                      │  Replica 2   │ AZ-C           │
│                      │              │                │
│                      └──────────────┘                │
│                                                      │
│  Single shard (one keyspace) — all data in one place │
│  Max memory: single node's RAM                       │
└─────────────────────────────────────────────────────┘
```

- Primary handles writes; replicas serve reads
- Multi-AZ: automatic failover to a replica if primary fails (~1 minute with DNS update)
- Up to 5 replicas

**Cluster Mode Enabled (Redis Cluster):**

```
┌──────────────────────────────────────────────────────────────┐
│  Redis Cluster (Cluster Mode Enabled)                         │
│                                                               │
│  Shard 1 (slots 0-5460)      Shard 2 (5461-10922)           │
│  Primary + 2 Replicas         Primary + 2 Replicas           │
│                                                               │
│  Shard 3 (10923-16383)        ...up to 500 shards            │
│  Primary + 2 Replicas                                        │
│                                                               │
│  Total keyspace: 16,384 hash slots                           │
│  Key→slot: CRC16(key) % 16384                               │
└──────────────────────────────────────────────────────────────┘
```

- **Horizontal sharding** across up to **500 nodes**
- Each shard has its own primary + replicas
- Keys are distributed via hash slot (0-16383)
- Supports much larger datasets than single-node
- **Limitation:** Multi-key commands (MGET, transactions, Lua scripts) must have all keys in the same slot — use hash tags `{user}:profile` and `{user}:orders` to force same slot

#### Redis Additional Features

**Pub/Sub:**
```
Publisher → PUBLISH channel "message" → All subscribers on that channel receive it
```
- Real-time messaging between services
- Messages are **NOT persisted** — if subscriber is offline, it misses the message
- For persistent messaging, use Redis Streams instead

**Sorted Sets for Leaderboards:**
```python
# Add player score
redis.zadd("leaderboard:tetris", {"alice": 9500, "bob": 7200, "carol": 12000})

# Get top 3 (descending)
top3 = redis.zrevrange("leaderboard:tetris", 0, 2, withscores=True)
# [('carol', 12000.0), ('alice', 9500.0), ('bob', 7200.0)]

# Get alice's rank
rank = redis.zrevrank("leaderboard:tetris", "alice")  # 1 (0-indexed)
```

**Distributed Locks with SETNX:**
```python
# Acquire lock
lock_acquired = redis.set("lock:resource", "owner_id", nx=True, ex=30)  # 30s TTL
if lock_acquired:
    try:
        # Do work
    finally:
        redis.delete("lock:resource")  # Release lock
```

**Rate Limiting with INCR + EXPIRE:**
```python
key = f"rate:{user_id}:{current_minute}"
count = redis.incr(key)
if count == 1:
    redis.expire(key, 60)  # Set TTL on first increment
if count > 100:
    raise RateLimitExceeded()
```

**Session Store:**
```python
# Store session
redis.setex(f"session:{token}", 3600, json.dumps(session_data))

# Read session
session = json.loads(redis.get(f"session:{token}") or '{}')
```

#### Redis AUTH and Security

```
TLS in-transit: enable transit encryption on cluster creation
Encryption at-rest: enable at-rest encryption on cluster creation (KMS)
Redis AUTH: password authentication for Redis commands
VPC-only: ElastiCache clusters are NOT publicly accessible
```

### 3.3 Memcached Deep Dive

```
┌─────────────────────────────────────────────────────┐
│  Memcached Cluster                                   │
│                                                      │
│  Node 1 (AZ-A)   Node 2 (AZ-B)   Node 3 (AZ-C)    │
│  ─────────────   ─────────────   ─────────────      │
│  Key subset 1    Key subset 2    Key subset 3        │
│                                                      │
│  Client-side consistent hashing distributes keys    │
│  No replication between nodes                       │
└─────────────────────────────────────────────────────┘
```

| Feature | Memcached | Redis |
|---|---|---|
| Data structures | Simple key-value (strings/bytes) | Rich (strings, hashes, lists, sets, sorted sets, streams, geo, etc.) |
| Persistence | None | RDB + AOF |
| Replication | None | Primary + replicas with failover |
| Multi-AZ failover | None (add more nodes) | Yes (automatic) |
| Clustering | Client-side sharding | Native cluster mode |
| Threading | **Multi-threaded** (better CPU utilization) | Single-threaded (v6+ I/O threads) |
| Use case | Pure cache, horizontal scale, simple objects | Cache + data store, pub/sub, sessions, leaderboards |
| Data loss on failure | **Lose all data** on node failure | Replicas survive; persistence can recover |
| Max nodes per cluster | 40 | 500 (cluster mode) |
| Sub-millisecond latency | Yes | Yes |

**When to choose Memcached:**
- Pure cache only — no need for persistence or advanced data types
- Need to scale horizontally by simply adding nodes
- Multi-threaded cache maximizing CPU cores
- Simple string/object cache

**When to choose Redis:**
- Need persistence (survive restarts)
- Need replication and Multi-AZ failover
- Need advanced data types (sorted sets, streams, geospatial)
- Need pub/sub
- Need Lua scripting
- Session store, leaderboards, rate limiting, distributed locks
- Global Datastore (cross-region replication)

### 3.4 ElastiCache Serverless

- Launched 2023
- Automatically scales memory and CPU based on demand — no cluster sizing needed
- Sub-millisecond latency maintained during scaling
- Pay per ElastiCache Processing Units (ECPUs) consumed + GB of data stored
- Supports Redis and Memcached
- Ideal for: unpredictable workloads, dev/test, microservices needing cache without ops overhead

### 3.5 Global Datastore for Redis

```
Primary Region (us-east-1)          Secondary Region (eu-west-1)
┌──────────────────────────┐         ┌──────────────────────────┐
│  Redis Cluster (Primary)  │ ──────▶ │  Redis Cluster (Secondary)│
│  reads + writes           │  async  │  reads only               │
│                           │  repl.  │                           │
└──────────────────────────┘         └──────────────────────────┘
```

- Cross-region Redis replication
- Secondary is read-only; primary handles writes
- Sub-second replication lag
- Secondary can be promoted to primary for DR
- Use case: globally distributed applications needing low-latency reads worldwide

### 3.6 Security Architecture

```
VPC (private subnets only)
│
├── ElastiCache Subnet Group (spans multiple AZs)
│   ├── Redis Primary (AZ-A)
│   └── Redis Replica (AZ-B)
│
├── Security Group: allow port 6379 (Redis) or 11211 (Memcached) from app SG only
│
├── Encryption in transit: TLS (enable at cluster creation)
│
├── Encryption at rest: KMS CMK (enable at cluster creation)
│
└── Redis AUTH: set --auth-token at cluster creation
```

> ElastiCache has **no public endpoint** — it lives inside a VPC. There is no way to access it from the public internet. This is a security feature, not a limitation.

---

## 4. Key Config and Limits

| Parameter | Redis | Memcached |
|---|---|---|
| Max nodes (cluster mode) | 500 nodes, 500 shards | 40 nodes |
| Max replicas per shard | 5 | 0 (no replication) |
| Failover time (Multi-AZ) | ~1 minute (DNS update) | N/A |
| Persistence | RDB + AOF | None |
| Pub/Sub | Yes | No |
| Data structures | 10+ types | Key-value only |
| Threading | Single-threaded (I/O multi in v6+) | Multi-threaded |
| Max item size (default) | 512 MB (string) | 1 MB |
| Hash slots (cluster mode) | 16,384 | N/A |
| TLS in-transit | Yes | Yes |
| Encryption at-rest | Yes | Yes |
| AUTH | Redis AUTH token | No native AUTH |
| Global Datastore | Yes | No |
| Backup/restore | Yes (RDB) | No |

---

## 5. Decision Tree

```
Need an in-memory cache?
│
├─ Pure cache only, no persistence, maximum throughput, multi-threaded?
│   └─ Memcached
│       Note: losing a node = losing all data on that node
│
└─ Any of these needed?
    ├─ Persistence (survive restarts)?
    ├─ Replication + automatic failover?
    ├─ Advanced data types (sorted sets, streams, geo)?
    ├─ Pub/Sub messaging?
    ├─ Distributed locks?
    ├─ Session store?
    ├─ Leaderboards?
    ├─ Cross-region replication?
    └─ → Redis

Redis: Cluster Mode Disabled vs Enabled?
│
├─ Dataset fits in single node memory (<hundreds of GB)?
│   └─ Cluster Mode Disabled (simpler, all multi-key commands work)
│
└─ Need horizontal sharding (dataset > single node)?
    └─ Cluster Mode Enabled
        Note: multi-key commands require same hash slot
              cannot easily resize once created (requires migration)
```

### Caching Strategy Decision

```
What data are you caching?
│
├─ Read-heavy, tolerates slight staleness, saves DB reads?
│   └─ Lazy Loading (Cache-Aside)
│       Add TTL to prevent indefinite staleness
│
├─ Critical that cache is always fresh on every write?
│   └─ Write-Through
│       Also use TTL to avoid stale data from deleted records
│
├─ Write-heavy, can tolerate eventual DB sync, low write latency critical?
│   └─ Write-Behind (Write-Back)
│       Must handle failure recovery; complex to implement
│
└─ All strategies: always set a TTL
   Short TTL (seconds-minutes): prices, inventory, rate limit windows
   Long TTL (hours-days): user profiles, static configuration, product catalog
```

---

## 6. Common Patterns

### Pattern 1: DB Read Cache (Lazy Loading)

```
User Request → App → Check Redis
                          │
                    ┌─────┴──────┐
                    HIT           MISS
                    │             │
              Return cached     Query RDS
                data            Set in Redis (TTL=300s)
                                Return data
```

Benefit: Eliminates 90%+ of DB reads for popular items.

### Pattern 2: Session Store (Multi-AZ Redis)

```
ALB → EC2 instances (stateless)
           │
           └── Session read/write → Redis (Multi-AZ)
                                         │
                                  If EC2 dies, user's session
                                  is preserved in Redis — new
                                  EC2 picks it up seamlessly
```

Config:
```
Redis endpoint: write to primary, read from replica (or primary for consistency)
TTL: session timeout (e.g., 30 minutes inactivity)
```

### Pattern 3: Leaderboard (Sorted Set)

```python
# Game service writes scores
for player, score in game_results:
    redis.zadd(f"leaderboard:{game_id}", {player: score})
    redis.expire(f"leaderboard:{game_id}", 86400)  # 24hr TTL

# API reads top 100
top_100 = redis.zrevrange(f"leaderboard:{game_id}", 0, 99, withscores=True)
```

### Pattern 4: Rate Limiting (INCR + EXPIRE)

```python
def check_rate_limit(user_id: str, limit: int = 100) -> bool:
    key = f"rate:{user_id}:{int(time.time() // 60)}"  # per-minute window
    pipe = redis.pipeline()
    pipe.incr(key)
    pipe.expire(key, 60)
    result = pipe.execute()
    return result[0] <= limit
```

### Pattern 5: Write-Through Cache for Product Catalog

```python
def update_product(product_id, data):
    # Write to DB
    db.execute("UPDATE products SET ... WHERE id = %s", product_id)

    # Write to cache immediately (Write-Through)
    redis.setex(f"product:{product_id}", 3600, json.dumps(data))
    # Now any read from cache gets fresh data
```

### Pattern 6: Pub/Sub for Real-Time Notifications

```python
# Publisher (order service)
redis.publish("order_events", json.dumps({
    "event": "order_shipped",
    "orderId": "ORDER#123",
    "userId": "alice"
}))

# Subscriber (notification service)
pubsub = redis.pubsub()
pubsub.subscribe("order_events")
for message in pubsub.listen():
    if message['type'] == 'message':
        event = json.loads(message['data'])
        send_push_notification(event['userId'], event)
```

---

## 7. Gotchas — Exam Tricks and Production Pitfalls

### Gotcha 1: Redis Failover Takes ~1 Minute (DNS-Based)
- Failover involves electing a new primary and updating the cluster DNS endpoint
- During this ~60 seconds, write requests fail
- Applications must implement retry logic with backoff
- **Exam trap:** "ElastiCache Multi-AZ provides zero-downtime failover" — FALSE, there is ~1 minute of unavailability

### Gotcha 2: Memcached Loses ALL Data on Node Failure
- No replication, no persistence in Memcached
- If a node fails, all keys on that node are gone
- Application must handle cache miss gracefully (fallback to DB)
- Adding a new node doesn't restore old data — starts empty

### Gotcha 3: ElastiCache Is VPC-Only — No Public Internet Access
- Cannot connect to ElastiCache from outside the VPC
- Lambda functions accessing ElastiCache must be in the same VPC
- On-premises access requires VPN or Direct Connect

### Gotcha 4: Cluster Mode Enabled Cannot Be Easily Resized
- Cannot change the number of shards via in-place modification (shard count is fixed at creation for some scenarios)
- Online resharding is possible but operationally complex
- Plan your shard count carefully for cluster mode enabled clusters

### Gotcha 5: Pub/Sub Messages Are Not Persisted
- If a subscriber disconnects and reconnects, it misses all messages sent during the disconnection
- For guaranteed message delivery, use Redis Streams (XADD/XREAD with consumer groups) instead of Pub/Sub

### Gotcha 6: Lazy Loading Results in Cache Miss Storm on Cold Start
- After a cache restart or major deployment, all items are gone → every request is a miss → DB gets hammered
- Mitigation: cache warming (pre-populate cache before taking traffic), circuit breaker patterns

### Gotcha 7: TTL Expiry and Thundering Herd
- When many items expire at the same time (e.g., all set with the same TTL at startup), all requests simultaneously miss → DB flood
- Mitigation: add jitter to TTL values: `TTL = base_TTL + random(0, base_TTL * 0.2)`

### Gotcha 8: Encryption Must Be Enabled at Cluster Creation
- Cannot add TLS or at-rest encryption to an existing cluster
- Must create a new cluster with encryption enabled and migrate data

### Gotcha 9: Redis AUTH Token Is Set at Creation
- Cannot add or change the AUTH token on an existing cluster without replacing it
- Rotate by creating new cluster, migrating, then deleting old

### Gotcha 10: ElastiCache Serverless vs Traditional Clusters
- Serverless auto-scales but costs more per operation at steady-state vs right-sized clusters
- Traditional clusters: you size them, you manage them, but lower cost for predictable load

---

## 8. Hands-On Lab — Free Tier Step-by-Step

### Lab Goal
Create a Redis cluster (cluster mode disabled), connect from EC2, test caching patterns, pub/sub, and sorted sets.

### Prerequisites
- AWS account (ElastiCache free tier: 750 hours of `cache.t3.micro` or `cache.t2.micro` per month for Redis or Memcached)
- EC2 instance in same VPC (for connecting to ElastiCache)

---

### Step 1: Create Security Groups

```bash
# Security group for ElastiCache
aws ec2 create-security-group \
  --group-name elasticache-lab-sg \
  --description "ElastiCache Lab Security Group" \
  --vpc-id vpc-XXXXXXXX

# Security group for EC2 (app tier)
aws ec2 create-security-group \
  --group-name app-lab-sg \
  --description "App Lab Security Group" \
  --vpc-id vpc-XXXXXXXX

# Allow Redis port from app SG only
aws ec2 authorize-security-group-ingress \
  --group-id <elasticache-sg-id> \
  --protocol tcp \
  --port 6379 \
  --source-group <app-sg-id>
```

### Step 2: Create ElastiCache Subnet Group

```bash
aws elasticache create-cache-subnet-group \
  --cache-subnet-group-name lab-subnet-group \
  --cache-subnet-group-description "Lab subnet group" \
  --subnet-ids subnet-XXXXXXXX subnet-YYYYYYYY  # subnets in different AZs
```

### Step 3: Create Redis Cluster (Cluster Mode Disabled)

```bash
aws elasticache create-replication-group \
  --replication-group-id lab-redis \
  --replication-group-description "Lab Redis cluster" \
  --cache-node-type cache.t3.micro \
  --engine redis \
  --engine-version 7.0 \
  --num-cache-clusters 2 \
  --cache-subnet-group-name lab-subnet-group \
  --security-group-ids <elasticache-sg-id> \
  --automatic-failover-enabled \
  --multi-az-enabled \
  --at-rest-encryption-enabled \
  --transit-encryption-enabled \
  --auth-token "MySecureAuthToken123!"

# Wait for cluster to be available
aws elasticache wait replication-group-available \
  --replication-group-id lab-redis
```

Get endpoint:
```bash
aws elasticache describe-replication-groups \
  --replication-group-id lab-redis \
  --query 'ReplicationGroups[0].NodeGroups[0].PrimaryEndpoint'
```

### Step 4: Connect from EC2 and Test Basic Operations

```bash
# On EC2 instance — install redis-cli
sudo amazon-linux-extras install redis6 -y
# or
sudo yum install redis -y

# Connect with TLS and AUTH
redis-cli -h <primary-endpoint> -p 6379 \
  --tls --no-auth-warning \
  -a "MySecureAuthToken123!"

# Basic operations
127.0.0.1:6379> PING
PONG

127.0.0.1:6379> SET user:alice '{"name":"Alice","email":"alice@example.com"}'
OK

127.0.0.1:6379> GET user:alice
"{\"name\":\"Alice\",\"email\":\"alice@example.com\"}"

127.0.0.1:6379> EXPIRE user:alice 300
(integer) 1

127.0.0.1:6379> TTL user:alice
(integer) 298
```

### Step 5: Test Sorted Set (Leaderboard)

```bash
# Add player scores
127.0.0.1:6379> ZADD leaderboard:tetris 9500 alice
127.0.0.1:6379> ZADD leaderboard:tetris 7200 bob
127.0.0.1:6379> ZADD leaderboard:tetris 12000 carol
127.0.0.1:6379> ZADD leaderboard:tetris 8500 dave

# Get top 3 (high score first)
127.0.0.1:6379> ZREVRANGE leaderboard:tetris 0 2 WITHSCORES
1) "carol"
2) "12000"
3) "alice"
4) "9500"
5) "dave"
6) "8500"

# Get alice's rank (0-indexed from top)
127.0.0.1:6379> ZREVRANK leaderboard:tetris alice
(integer) 1

# Get players with score between 8000 and 10000
127.0.0.1:6379> ZRANGEBYSCORE leaderboard:tetris 8000 10000 WITHSCORES
```

### Step 6: Test Pub/Sub

```bash
# Terminal 1 — Subscriber
redis-cli -h <primary-endpoint> -p 6379 --tls -a "MySecureAuthToken123!"
127.0.0.1:6379> SUBSCRIBE order_events
# (waiting for messages)

# Terminal 2 — Publisher
redis-cli -h <primary-endpoint> -p 6379 --tls -a "MySecureAuthToken123!"
127.0.0.1:6379> PUBLISH order_events '{"event":"order_shipped","orderId":"123"}'
(integer) 1  # number of subscribers that received the message

# Terminal 1 will show:
# 1) "message"
# 2) "order_events"
# 3) "{\"event\":\"order_shipped\",\"orderId\":\"123\"}"
```

### Step 7: Test Rate Limiting Pattern

```bash
# Simulate rate limiting: max 3 requests per minute
127.0.0.1:6379> SET rate:user123:$(date +%s | cut -c1-8) 0
127.0.0.1:6379> INCR rate:user123:$(date +%s | cut -c1-8)
(integer) 1
127.0.0.1:6379> INCR rate:user123:$(date +%s | cut -c1-8)
(integer) 2
127.0.0.1:6379> INCR rate:user123:$(date +%s | cut -c1-8)
(integer) 3
# At count 3 = rate limit reached, app would reject next request
```

### Step 8: Test Hash (Object Cache)

```bash
# Store object fields as hash
127.0.0.1:6379> HSET product:p1 name "Laptop" price 999.99 stock 50
(integer) 3

# Get specific fields
127.0.0.1:6379> HGET product:p1 price
"999.99"

# Get all fields
127.0.0.1:6379> HGETALL product:p1
1) "name"
2) "Laptop"
3) "price"
4) "999.99"
5) "stock"
6) "50"

# Update one field without fetching entire object
127.0.0.1:6379> HINCRBY product:p1 stock -1
(integer) 49
```

### Step 9: Test Multi-AZ Failover

```bash
# Identify current primary
aws elasticache describe-replication-groups \
  --replication-group-id lab-redis \
  --query 'ReplicationGroups[0].NodeGroups[0].NodeGroupMembers[?CurrentRole==`primary`].PreferredAvailabilityZone'

# Trigger failover (simulates primary failure)
aws elasticache test-failover \
  --replication-group-id lab-redis \
  --node-group-id 0001

# Watch for ~60 seconds — connection will drop, then reconnect to new primary
# Your application should handle this with retry logic
```

### Step 10: Cleanup

```bash
# Delete replication group (this deletes all nodes)
aws elasticache delete-replication-group \
  --replication-group-id lab-redis \
  --no-retain-primary-cluster

# Delete subnet group
aws elasticache delete-cache-subnet-group \
  --cache-subnet-group-name lab-subnet-group

# Delete security group
aws ec2 delete-security-group --group-id <elasticache-sg-id>
```

---

### Key Observations from Lab

1. **TLS is required to connect with AUTH** — `transit-encryption-enabled` must be set at creation
2. **Failover takes ~60 seconds** — your application must implement retry/reconnect logic (most Redis clients handle this automatically with connection pooling)
3. **Sorted sets are O(log N)** — extremely fast for leaderboard operations regardless of dataset size
4. **Pub/Sub is fire-and-forget** — messages sent while you were disconnected in Step 6 are gone permanently
5. **Hash vs String** — Hash is more memory-efficient for objects (fewer keys, binary-optimized storage for small hashes)
6. **Cluster endpoint stays the same** after failover — only the node pointed to by the DNS entry changes
