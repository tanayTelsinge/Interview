# Elasticsearch — Interview Cheat Sheet

---

## What is Elasticsearch?

Distributed, RESTful search and analytics engine built on Apache Lucene.
Stores data as JSON documents and makes them full-text searchable in near real-time.

Used for:
- Full-text search (product search, guest name search in PMS)
- Log aggregation and analysis (ELK stack)
- Analytics and aggregations (booking trends, revenue reports)
- Autocomplete and suggestions

---

## Core Concepts

```
Elasticsearch          Relational DB equivalent
─────────────────────────────────────────────
Index                → Table
Document             → Row
Field                → Column
Mapping              → Schema
Shard                → Partition
```

### Document
JSON object stored in an index. Each document has a unique `_id`.
```json
{
  "_index": "reservations",
  "_id": "1001",
  "_source": {
    "guestName": "John Smith",
    "roomNumber": "204",
    "checkIn": "2025-06-01",
    "checkOut": "2025-06-05",
    "status": "CONFIRMED",
    "totalAmount": 1200.00
  }
}
```

### Index
Collection of documents with similar structure.
One index = one logical dataset (reservations, guests, payments).

### Shard
An index is split into shards — each shard is a self-contained Lucene index.
- Primary shard — original copy, handles writes
- Replica shard — copy for fault tolerance and read scaling

```
Index: reservations (3 primary shards, 1 replica each)

Node 1           Node 2           Node 3
─────────────    ─────────────    ─────────────
P0  R1           P1  R2           P2  R0
                 R0               R1  R2
```

Rule of thumb: num_shards = expected_docs / 20-40 million per shard.

---

## Architecture in a SaaS PMS

```
Spring Boot Service
      │
      │ write (index document)
      ▼
Elasticsearch Cluster (on GKE or Cloud)
      │
      ├── Node 1 (master-eligible + data)
      ├── Node 2 (data)
      └── Node 3 (data)
           │
           │ search query
           ▼
      Results returned to service
```

### Two common patterns for keeping ES in sync with DB:

1. Dual write — service writes to DB and ES in same request
   Risk: partial failure — DB succeeds, ES fails → out of sync

2. Event-driven sync (preferred) — service writes to DB → publishes
   event to Pub/Sub/Kafka → ES sync consumer indexes to Elasticsearch
   Benefit: decoupled, retryable, no data loss

```
Reservation Service
     │ save to Cloud SQL
     │ publish to Pub/Sub
     ▼
ES Sync Consumer
     │ index to Elasticsearch
     ▼
Elasticsearch
```

---

## Spring Boot Integration

### Gradle Dependency
```groovy
// Spring Data Elasticsearch (uses high-level REST client)
implementation 'org.springframework.boot:spring-boot-starter-data-elasticsearch'

// Or Elasticsearch Java client (official, newer)
implementation 'co.elastic.clients:elasticsearch-java:8.13.0'
implementation 'com.fasterxml.jackson.core:jackson-databind:2.17.0'
```

### application.yml
```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
    username: elastic
    password: changeme
    # For cloud (Elastic Cloud):
    # uris: https://my-cluster.es.io:443
    # api-key: base64encodedkey
```

### Document Entity
```java
@Document(indexName = "reservations")
@Setting(shards = 3, replicas = 1)
public class ReservationDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String guestName;       // full-text searchable

    @Field(type = FieldType.Keyword)
    private String status;          // exact match only (CONFIRMED, CANCELLED)

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate checkIn;

    @Field(type = FieldType.Double)
    private Double totalAmount;

    @Field(type = FieldType.Integer)
    private Integer roomNumber;
}
```

### Repository
```java
public interface ReservationSearchRepository
        extends ElasticsearchRepository<ReservationDocument, String> {

    // Spring Data derived query
    List<ReservationDocument> findByGuestNameContaining(String name);

    // Filter by status
    List<ReservationDocument> findByStatus(String status);

    // Range query
    List<ReservationDocument> findByCheckInBetween(LocalDate from, LocalDate to);
}
```

### Custom Query (native)
```java
@Service
@RequiredArgsConstructor
public class ReservationSearchService {

    private final ElasticsearchOperations esOps;

    public List<ReservationDocument> search(String guestName, String status) {
        Criteria criteria = new Criteria("guestName").matches(guestName);
        if (status != null) {
            criteria = criteria.and("status").is(status);
        }
        Query query = new CriteriaQuery(criteria);
        return esOps.search(query, ReservationDocument.class)
                    .stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());
    }
}
```

---

## Query Types

### Match query — full-text search (analysed)
```json
GET /reservations/_search
{
  "query": {
    "match": {
      "guestName": "john smith"   // tokenised, case-insensitive
    }
  }
}
```

### Term query — exact match (not analysed, use for keyword fields)
```json
GET /reservations/_search
{
  "query": {
    "term": {
      "status": "CONFIRMED"
    }
  }
}
```

### Range query
```json
GET /reservations/_search
{
  "query": {
    "range": {
      "checkIn": {
        "gte": "2025-06-01",
        "lte": "2025-06-30"
      }
    }
  }
}
```

### Bool query — combine multiple conditions
```json
GET /reservations/_search
{
  "query": {
    "bool": {
      "must":   [{ "match":  { "guestName": "smith" }}],
      "filter": [{ "term":   { "status": "CONFIRMED" }},
                 { "range":  { "checkIn": { "gte": "2025-06-01" }}}],
      "must_not": [{ "term": { "status": "CANCELLED" }}]
    }
  }
}
```

| Clause | Scoring | Use |
|--------|---------|-----|
| must | yes — affects relevance score | required, ranked by relevance |
| filter | no scoring — cached | required, binary yes/no |
| should | yes | optional, boosts score if matches |
| must_not | no scoring | exclude documents |

Use `filter` over `must` for exact matches — it's faster (cached, no scoring).

### Aggregation — analytics
```json
GET /reservations/_search
{
  "size": 0,
  "aggs": {
    "by_status": {
      "terms": { "field": "status" }
    },
    "total_revenue": {
      "sum": { "field": "totalAmount" }
    },
    "avg_stay": {
      "avg": { "field": "nights" }
    }
  }
}
```

---

## Text vs Keyword field type

| | Text | Keyword |
|--|------|---------|
| Analysed | Yes — tokenised, lowercased | No — stored as-is |
| Use for | Full-text search (names, descriptions) | Exact match, sorting, aggregations |
| Query type | match, match_phrase | term, terms, filter |
| Example | guestName, address, notes | status, country, roomType |

A field can have both — use `fields` mapping:
```json
"guestName": {
  "type": "text",
  "fields": {
    "keyword": { "type": "keyword" }   // for sorting/aggregations
  }
}
```

---

## Inverted Index — how search works internally

Normal DB index: row → values
Inverted index:  term → list of document IDs

```
Document 1: "John Smith checked in"
Document 2: "Smith family reservation"
Document 3: "John Doe booking"

Inverted index:
john    → [doc1, doc3]
smith   → [doc1, doc2]
checked → [doc1]
family  → [doc2]
doe     → [doc3]
```

Search "john smith" → find docs containing "john" AND "smith" → [doc1]
This is why ES is so fast — it doesn't scan documents, it looks up the index.

---

## Relevance Scoring — BM25

ES ranks results by relevance score, not just matched/not matched.

Score is higher when:
- Term appears more times in the document (TF — term frequency)
- Term is rare across all documents (IDF — inverse document frequency)
- Document is shorter (shorter docs with same term count score higher)

BM25 replaced TF-IDF in ES 5.0 — more accurate for longer documents.

---

## Autocomplete / Suggestions

```java
// Mapping
@Field(type = FieldType.Search_As_You_Type)
private String guestName;

// Query — matches prefix and substrings
{
  "query": {
    "multi_match": {
      "query": "joh",
      "type": "bool_prefix",
      "fields": ["guestName", "guestName._2gram", "guestName._3gram"]
    }
  }
}
```

---

## Common Interview Questions

### Q: Elasticsearch vs SQL DB — when to use which?

| | Elasticsearch | SQL (Cloud SQL / Postgres) |
|--|---------------|---------------------------|
| Full-text search | Excellent | Poor (LIKE %% is slow) |
| Exact queries | Good | Excellent |
| ACID transactions | No | Yes |
| Joins | No (denormalise instead) | Yes |
| Aggregations on large data | Fast | Slower |
| Schema flexibility | High | Fixed |
| Source of truth | No — derived store | Yes |

Rule: SQL is source of truth. ES is a read-optimised search index derived from SQL.
Never use ES as your primary database.

---

### Q: How do you keep ES in sync with your DB?

> "We use event-driven sync — on every write to Cloud SQL, the service publishes
> an event to Pub/Sub. An ES sync consumer subscribes, transforms the entity to
> an ES document, and indexes it. This is decoupled and retryable. If ES is down,
> events queue in Pub/Sub and replay when it recovers. The tradeoff is eventual
> consistency — there's a small lag between DB write and search index update, which
> is acceptable for search but not for transactional reads."

---

### Q: What happens when an ES node goes down?

> "Replica shards on other nodes are promoted to primary. Elasticsearch automatically
> reroutes the affected shards within seconds. With 1 replica per shard, the cluster
> survives losing any single node with zero data loss. The cluster status goes yellow
> (replicas unassigned) until a new node joins and shards are re-replicated."

---

### Q: What is the ELK stack?

```
E — Elasticsearch   store and search logs
L — Logstash        ingest, parse, transform logs (or Fluentd/Filebeat)
K — Kibana          visualise, dashboard, query logs

Flow:
App logs → Filebeat (lightweight shipper) → Logstash (parse/enrich) → ES → Kibana
```

In GKE: Fluentd DaemonSet on each node ships pod logs → Elasticsearch.

---

### Q: How do you handle a mapping explosion?

Mapping explosion = too many dynamic fields creating thousands of mappings,
bloating cluster state.
Fix: disable dynamic mapping and define explicit mappings.
```json
PUT /reservations
{
  "mappings": {
    "dynamic": "strict",   // reject unknown fields
    "properties": { ... }
  }
}
```

---

## Gradle dependency versions (Spring Boot 3.x)

```groovy
// build.gradle
plugins {
    id 'org.springframework.boot' version '3.3.2'
    id 'io.spring.dependency-management' version '1.1.6'
}

dependencies {
    // Spring Data Elasticsearch — version managed by Spring Boot BOM
    implementation 'org.springframework.boot:spring-boot-starter-data-elasticsearch'
}
// Managed version: Spring Boot 3.3.x → Spring Data ES 5.3.x → ES client 8.x
// Ensure your ES cluster version matches the client major version (both 8.x)
```
