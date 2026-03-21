# Quartz Scheduler

## What & Why

`@Scheduled` in Spring works fine for single instance. The moment you have **multiple instances of your service**, every instance runs the job → **duplicate execution**.

> **Instance = Pod** in Kubernetes. Each pod runs its own JVM.
> ```
> Deployment (replicas: 3)
>   ├── Pod 1  (JVM → runs @Scheduled)
>   ├── Pod 2  (JVM → runs @Scheduled)   ← all 3 fire independently
>   └── Pod 3  (JVM → runs @Scheduled)
> ```
> Same problem applies to EC2 instances, Docker containers, or any multiple JVM setup.

Quartz solves this by using a **shared DB** as a lock — only one instance runs the job at a time.

```
Without Quartz (3 instances):          With Quartz (3 instances):
Instance 1 → runs job ✓               Instance 1 → acquires DB lock → runs job ✓
Instance 2 → runs job ✓  (duplicate)  Instance 2 → sees lock → skips
Instance 3 → runs job ✓  (duplicate)  Instance 3 → sees lock → skips
```

---

## Architecture

```
┌─────────────────────────────────────┐
│            Scheduler                │  ← brain, coordinates everything
│                                     │
│   JobDetail ──── Trigger            │
│   (what to run)  (when to run)      │
│                                     │
│   JobStore (DB / RAM)               │  ← persists jobs & triggers
│   ThreadPool                        │  ← worker threads that execute jobs
└─────────────────────────────────────┘
```

### Key Components

| Component | What it does |
|---|---|
| `Scheduler` | Main interface — schedule, pause, resume, delete jobs |
| `Job` | Interface with `execute()` — your actual logic |
| `JobDetail` | Metadata about the job (name, group, class, data) |
| `Trigger` | When/how often to fire (Cron or Simple) |
| `JobStore` | Where jobs/triggers are stored — RAM or DB |
| `ThreadPool` | Runs jobs concurrently |

---

## Two Trigger Types

```java
// 1. CronTrigger — cron expression
Trigger cron = TriggerBuilder.newTrigger()
    .withSchedule(CronScheduleBuilder.cronSchedule("0 0 9 * * ?"))  // 9am daily
    .build();

// 2. SimpleTrigger — fixed interval / repeat count
Trigger simple = TriggerBuilder.newTrigger()
    .withSchedule(SimpleScheduleBuilder.repeatSecondlyForever(30))  // every 30s
    .build();
```

---

## Two JobStore Modes

| | RAMJobStore | JDBCJobStore |
|---|---|---|
| Storage | In memory | Database |
| Survives restart | No | Yes |
| Clustered | No | Yes |
| Config | Default | Needs Quartz DB tables |

For **clustering** always use JDBCJobStore — that's the whole point.

---

## Spring Boot Setup

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-quartz</artifactId>
</dependency>
```

```properties
spring.quartz.job-store-type=jdbc          # use DB
spring.quartz.jdbc.initialize-schema=always # create quartz tables
spring.quartz.properties.org.quartz.jobStore.isClustered=true
spring.quartz.properties.org.quartz.scheduler.instanceId=AUTO
```

```java
@Component
public class MyJob implements Job {
    @Override
    public void execute(JobExecutionContext context) {
        // your logic — Spring beans can be injected via SpringBeanJobFactory
        System.out.println("Running at " + new Date());
    }
}
```

---

## Clustering — How the Lock Works

```
All instances share same DB (QRTZ_* tables)
      │
      ▼
When trigger fires → instance tries to acquire row-level lock on QRTZ_LOCKS table
      │
      ├── Lock acquired → run job
      └── Lock not acquired → another instance is running it → skip
```

No code change needed — just configure `isClustered=true` and same DB.

---

## Misfire Policy
What happens if job was supposed to run but scheduler was down?

```java
CronScheduleBuilder.cronSchedule("0 0 9 * * ?")
    .withMisfireHandlingInstructionDoNothing()    // skip missed runs
    .withMisfireHandlingInstructionFireAndProceed() // run once immediately
    .withMisfireHandlingInstructionIgnoreMisfires() // run all missed
```

---

## Quartz vs Alternatives

### @Scheduled (Spring)
```java
@Scheduled(cron = "0 0 9 * * ?")
public void run() { ... }
```
- Simple, no setup
- ❌ Runs on every instance — no clustering

### ShedLock
```java
@Scheduled(cron = "0 0 9 * * ?")
@SchedulerLock(name = "myJob", lockAtMostFor = "10m")
public void run() { ... }
```
- Adds a DB/Redis lock on top of `@Scheduled`
- ✅ Prevents duplicate runs with almost zero setup
- ❌ No persistence, no UI, no misfire handling
- **Best lightweight alternative to Quartz**

### db-scheduler
- Simple single-table DB scheduler
- Good for microservices — minimal footprint
- No XML, no 11 tables like Quartz

### Temporal / Conductor (workflow orchestration)
- For long-running, multi-step workflows
- Overkill for simple cron jobs

### Cloud-native options
| Option | Platform |
|---|---|
| AWS EventBridge Scheduler | AWS Lambda / ECS |
| GCP Cloud Scheduler | GCP Cloud Run |
| Kubernetes CronJob | K8s |

For microservices on cloud — **prefer cloud scheduler** over running Quartz in your service.

---

## When to Use What

```
Single instance app?
└── @Scheduled  ← simplest, done

Multiple instances, just need "run once"?
└── ShedLock + @Scheduled  ← easiest, minimal setup

Need persistence, misfire handling, job management UI?
└── Quartz with JDBCJobStore

Running on Kubernetes?
└── K8s CronJob  ← let the platform handle it

Complex multi-step workflows?
└── Temporal / Conductor
```

---

## Interview Questions

**Q: Why not just use @Scheduled in a clustered app?**
→ Every instance fires the job independently — same job runs N times in parallel.

**Q: How does Quartz prevent duplicate runs in a cluster?**
→ All nodes share a DB. When a trigger fires, the node tries to acquire a row lock on `QRTZ_LOCKS`. Only one succeeds — others skip.

**Q: RAMJobStore vs JDBCJobStore?**
→ RAMJobStore is fast but loses all jobs on restart and can't cluster. JDBCJobStore persists to DB — required for clustering and restart recovery.

**Q: What is a misfire?**
→ A scheduled trigger that was missed (app was down). Misfire policy decides: skip it, run once now, or run all missed.

**Q: ShedLock vs Quartz?**
→ ShedLock is just a lock wrapper on `@Scheduled` — dead simple, no tables, no UI. Quartz is a full scheduler — persistence, clustering, misfire, dynamic job creation. Choose based on complexity needed.
