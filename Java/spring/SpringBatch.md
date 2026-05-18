# Spring Batch

## What is it?
Framework for processing **large volumes of data** — read, process, write — with retry, skip, restart built in.

---

## Architecture

```
Job
└── Step (one or many, sequential or parallel)
    ├── Tasklet approach   → single unit of work
    └── Chunk approach     → read → process → write in batches

JobLauncher  → launches a Job
JobRepository → stores metadata (job status, step status) in DB
JobInstance  → one run of a Job with given parameters
JobExecution → actual attempt (can have multiple executions per instance on retry)
StepExecution → execution metadata per step
```

```
┌─────────────┐       ┌──────────────────────────────────────┐
│ JobLauncher │──────▶│              Job                     │
└─────────────┘       │  ┌────────┐  ┌────────┐  ┌────────┐ │
                      │  │ Step 1 │→ │ Step 2 │→ │ Step 3 │ │
                      │  └────────┘  └────────┘  └────────┘ │
                      └──────────────────────────────────────┘
                                        │
                                 JobRepository (DB)
```

---

## 1. Tasklet Approach

One step = one custom action. No read-process-write cycle.

**Use when:** delete temp files, call an API once, send email, DDL operations.

```java
@Bean
public Step cleanupStep(StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory.get("cleanupStep")
        .tasklet((contribution, chunkContext) -> {
            // do your one-time work
            Files.delete(Path.of("/tmp/data.csv"));
            return RepeatStatus.FINISHED;  // or CONTINUABLE to loop
        })
        .build();
}
```

---

## 2. Chunk-Oriented Processing

Read N items → process each → write all N as a batch. Repeat until done.

```
[ ItemReader ] --item--> [ ItemProcessor ] --item--> [ ItemWriter ]
      ↑                                                     |
      |_____________ commit after chunk size ______________|
```

```java
@Bean
public Step processStep(StepBuilderFactory factory,
                         ItemReader<CsvRecord> reader,
                         ItemProcessor<CsvRecord, User> processor,
                         ItemWriter<User> writer) {
    return factory.get("processStep")
        .<CsvRecord, User>chunk(500)          // chunk size = 500
        .reader(reader)
        .processor(processor)
        .writer(writer)
        .faultTolerant()
        .skip(ParseException.class).skipLimit(100)   // skip bad records
        .retry(DeadlockLoserDataAccessException.class).retryLimit(3)
        .build();
}
```

### Built-in Readers/Writers

| Reader | Writer |
|---|---|
| `FlatFileItemReader` (CSV) | `JdbcBatchItemWriter` |
| `JdbcCursorItemReader` | `FlatFileItemWriter` |
| `JpaCursorItemReader` | `JpaItemWriter` |
| `KafkaItemReader` | `KafkaItemWriter` |
| `JsonItemReader` | `MongoItemWriter` |

---

## CSV → DB: FlatFileItemReader + JdbcBatchItemWriter

```java
// Reader
@Bean
public FlatFileItemReader<CsvRecord> reader() {
    return new FlatFileItemReaderBuilder<CsvRecord>()
        .name("csvReader")
        .resource(new FileSystemResource("data.csv"))
        .delimited().names("id", "name", "email")
        .targetType(CsvRecord.class)
        .build();
}

// Processor (transform/validate)
@Bean
public ItemProcessor<CsvRecord, User> processor() {
    return item -> {
        if (item.getEmail() == null) return null;  // null = skip this item
        return new User(item.getId(), item.getName(), item.getEmail());
    };
}

// Writer — single batch INSERT per chunk
@Bean
public JdbcBatchItemWriter<User> writer(DataSource ds) {
    return new JdbcBatchItemWriterBuilder<User>()
        .dataSource(ds)
        .sql("INSERT INTO users (id, name, email) VALUES (:id, :name, :email)")
        .beanMapped()
        .build();
}
```

---

## Tasklet vs Chunk

| | Tasklet | Chunk |
|---|---|---|
| Use for | One-time actions | Large data processing |
| Restart | Re-runs whole tasklet | Restarts from last commit point |
| Retry/Skip | Manual | Built-in |
| Transaction | Single | Per chunk |

---

## Parallel Processing Options

### Partitioning (partition large file by range)
```java
// Master step splits work, slave steps run in parallel
@Bean
public Step masterStep() {
    return stepBuilderFactory.get("masterStep")
        .partitioner("slaveStep", partitioner())   // split into N partitions
        .step(slaveStep())
        .gridSize(10)                               // 10 parallel workers
        .taskExecutor(taskExecutor())
        .build();
}
```

### Parallel Steps
```java
Flow flow1 = new FlowBuilder<Flow>("flow1").start(step1()).build();
Flow flow2 = new FlowBuilder<Flow>("flow2").start(step2()).build();

new FlowBuilder<Flow>("parallelFlow")
    .split(taskExecutor())
    .add(flow1, flow2)
    .build();
```

### Multi-threaded Step (simplest)
```java
.chunk(500)
.taskExecutor(new SimpleAsyncTaskExecutor())
.throttleLimit(4)   // 4 threads, each processing chunks
```
⚠️ Reader must be thread-safe — use `SynchronizedItemStreamReader` wrapper.

---

## Interview Questions

**Q: What is a JobInstance vs JobExecution?**
→ JobInstance = logical run (Job + parameters). JobExecution = one actual attempt. One instance can have multiple executions (on retry/restart).

**Q: How does restart work in chunk processing?**
→ Spring Batch stores the last committed chunk's offset in JobRepository. On restart, it skips already-committed chunks and resumes from the failure point.

**Q: How do you skip bad records?**
→ `.faultTolerant().skip(Exception.class).skipLimit(n)` — processor returns null or reader skips the line. Spring records skipped items in `skip_count`.

**Q: chunk size — how do you choose?**
→ Balance between memory (larger chunk = more objects in memory) and DB round trips (smaller chunk = more commits). Typical: 100–1000. Test with your data size.

**Q: How to run steps in parallel?**
→ Split Flow with `taskExecutor`, or use Partitioning for data-parallel execution (each partition handles a range of data).

**Q: What is ItemProcessor returning null?**
→ That item is filtered out — not passed to the writer. Use this for conditional skipping without throwing exceptions.

**Q: Where does Spring Batch store job metadata?**
→ In `JobRepository` — backed by a DB (tables: `BATCH_JOB_INSTANCE`, `BATCH_JOB_EXECUTION`, `BATCH_STEP_EXECUTION`, etc.). Can use in-memory for tests.

---

---

# Large CSV → DB: All Approaches in Microservice Architecture

## The Problem
- File: millions of rows
- Goal: persist to DB fast and reliably
- Constraints: memory, DB connection pool, throughput, failure recovery

---

## Approach 1: Spring Batch (chunk-based)
Best for: **scheduled batch jobs, restart/retry needed, audit trail**

- FlatFileItemReader streams line by line (no full load into memory)
- JdbcBatchItemWriter does bulk INSERT per chunk
- Partitioning for parallelism
- Built-in skip, retry, restart from checkpoint

**Bottleneck:** single service, single DB connection pool

---

## Approach 2: Streaming + Message Queue (Kafka/RabbitMQ)
Best for: **real-time ingestion, decoupled processing, high throughput**

```
CSV Upload Service
      │
      ▼
Parse CSV line by line
      │
      ▼
Publish each row (or batch of rows) → Kafka topic
      │
      ▼
Consumer Microservice(s) → bulk INSERT to DB
```

- Producer streams CSV, never loads full file in memory
- Multiple consumer instances = horizontal scale
- Kafka offset = natural checkpoint (restart from last committed offset)
- Consumer batches messages before writing (e.g., collect 500, INSERT once)

```java
// Consumer — collect and batch insert
@KafkaListener(topics = "csv-records", batch = true)
public void consume(List<CsvRecord> records) {
    jdbcTemplate.batchUpdate(SQL, records, 500, (ps, r) -> { ... });
}
```

---

## Approach 3: Async + Thread Pool (simple, in-process)
Best for: **moderate file sizes, no external dependencies**

```java
ExecutorService pool = Executors.newFixedThreadPool(8);
List<Future<?>> futures = new ArrayList<>();

try (BufferedReader br = Files.newBufferedReader(path)) {
    List<String> batch = new ArrayList<>();
    String line;
    while ((line = br.readLine()) != null) {
        batch.add(line);
        if (batch.size() == 500) {
            List<String> chunk = new ArrayList<>(batch);
            futures.add(pool.submit(() -> bulkInsert(chunk)));
            batch.clear();
        }
    }
    // handle remainder
}
futures.forEach(f -> f.get());  // wait for all
```

⚠️ DB connection pool must be sized to match thread count.

---

## Approach 4: COPY / LOAD DATA INFILE (DB-native bulk load)
Best for: **maximum raw throughput, simple use case**

```sql
-- PostgreSQL
COPY users(id, name, email) FROM '/path/data.csv' DELIMITER ',' CSV HEADER;

-- MySQL
LOAD DATA INFILE '/path/data.csv'
INTO TABLE users
FIELDS TERMINATED BY ','
IGNORE 1 ROWS;
```

Or via JDBC (PostgreSQL CopyManager):
```java
CopyManager cm = new CopyManager((BaseConnection) conn);
cm.copyIn("COPY users FROM STDIN WITH CSV HEADER", fileReader);
```

**Fastest possible.** But: no transformation, no per-row validation, DB needs file access (or stream via JDBC).

---

## Approach 5: Pre-signed URL + Cloud Storage + Lambda/Worker
Best for: **very large files (GBs), cloud-native, serverless**

```
Client uploads CSV → S3 / GCS (pre-signed URL)
      │
      ▼
S3 Event → triggers Lambda / Cloud Function / Worker Service
      │
      ▼
Worker streams S3 object line by line → Kafka or direct DB insert
```

- File never touches your app server
- S3 streaming = memory efficient
- Worker can be autoscaled

---

## Approach 6: Reactive Streaming (WebFlux + R2DBC)
Best for: **non-blocking, high concurrency, reactive stack**

```java
Flux.fromStream(Files.lines(path))
    .skip(1)                                          // skip header
    .map(this::parseLine)
    .buffer(500)                                      // batch of 500
    .flatMap(batch -> r2dbcTemplate.batchInsert(batch), 4)  // 4 concurrent
    .subscribe();
```

No thread blocking — good for async pipelines. Requires R2DBC (reactive DB driver).

---

## Approach 7: Bulk Insert Optimization (applies to all above)

Regardless of approach, DB write speed depends on:

```sql
-- 1. Use batch INSERT not individual inserts
INSERT INTO users VALUES (1,'a'), (2,'b'), ... (500,'z');  -- one round trip

-- 2. Disable indexes during load, rebuild after
ALTER TABLE users DISABLE KEYS;
LOAD DATA ...;
ALTER TABLE users ENABLE KEYS;

-- 3. Wrap in single transaction per batch
-- 4. Use UPSERT to handle duplicates
INSERT INTO users ... ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name;
```

JDBC settings:
```properties
spring.datasource.hikari.maximum-pool-size=20
rewriteBatchedStatements=true   # MySQL — rewrites to multi-value INSERT
```

---

## Comparison

| Approach | Throughput | Complexity | Restart | Best For |
|---|---|---|---|---|
| Spring Batch | Medium | Medium | Built-in | Scheduled jobs, audit |
| Kafka + Consumer | High | High | Kafka offset | Real-time, scale-out |
| Thread Pool | Medium | Low | Manual | Simple, moderate size |
| DB COPY/LOAD | Highest | Low | Manual | Max speed, no transform |
| S3 + Worker | High | High | S3 + offset | Cloud-native, huge files |
| Reactive (R2DBC) | High | High | Manual | Reactive stack |

---

## Decision Guide

```
File size < 100k rows?         → Simple Thread Pool or Spring Batch
Need retry/skip/audit?         → Spring Batch
Need real-time / event-driven? → Kafka + Consumer
Need maximum raw speed?        → DB COPY / LOAD DATA INFILE
File too large for server?     → S3 upload + Worker
Already on reactive stack?     → WebFlux + R2DBC
```
