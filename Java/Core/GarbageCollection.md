# Garbage Collection — Java Cheatsheet

## Core Idea
GC automatically frees heap memory for objects with no live references.
You never call `free()` — JVM decides **when** and **what** to collect.

---

## Heap Structure

```
Heap
├── Young Generation         ← new objects allocated here
│   ├── Eden Space           ← all new objects start here
│   ├── Survivor S0          ← objects that survived 1+ GC
│   └── Survivor S1
└── Old Generation (Tenured) ← long-lived objects promoted here
```

**Minor GC** — collects Young Gen (fast, frequent)
**Major/Full GC** — collects Old Gen (slow, stop-the-world)

---

## Object Lifecycle

```
new Obj() → Eden → [Minor GC] → S0/S1 (age++) → [age ≥ threshold] → Old Gen → Full GC → gone
```

- Promotion threshold default: **age 15**
- Large objects skip Young Gen → go directly to Old Gen

---

## GC Algorithms

| GC | Flag | Best For | Pause |
|---|---|---|---|
| Serial | `-XX:+UseSerialGC` | single-core, small heap | high (STW) |
| Parallel (Throughput) | `-XX:+UseParallelGC` | batch jobs, throughput | medium STW |
| G1 (default Java 9+) | `-XX:+UseG1GC` | balanced latency+throughput | low |
| ZGC | `-XX:+UseZGC` | low-latency, large heap | <1ms (Java 15+ prod) |
| Shenandoah | `-XX:+UseShenandoahGC` | low-latency | <1ms |

**Default:** G1GC (Java 9+), ZGC becoming default in newer versions.

---

## G1GC — How It Works

- Heap split into equal-sized **regions** (~2048 regions)
- Regions dynamically assigned as Eden / Survivor / Old / Humongous
- Collects regions with **most garbage first** (hence G1)
- Targets pause time: `-XX:MaxGCPauseMillis=200` (default)

```
Regions: [E][E][S][O][O][H][E][O]...
          ↑ Eden  ↑ Survivor  ↑ Old  ↑ Humongous (large obj)
```

---

## ZGC — How It Works

- Concurrent — does most work **while app runs**
- Uses **colored pointers** (load barriers) to track object state
- Pause only for root scanning: **<1ms** regardless of heap size
- Good for heaps from 8MB to 16TB
- `-XX:+UseZGC -Xmx16g`

---

## Key JVM Flags

```bash
-Xms512m                    # initial heap size
-Xmx4g                      # max heap size
-XX:NewRatio=3              # Old:Young ratio (3:1)
-XX:SurvivorRatio=8         # Eden:Survivor ratio (8:1:1)
-XX:MaxTenuringThreshold=15 # age before promotion
-XX:+PrintGCDetails         # verbose GC log
-Xlog:gc*                   # modern GC logging (Java 9+)
-XX:MaxGCPauseMillis=200    # G1 pause target
```

---

## GC Roots (what keeps objects alive)

- Local variables on stack
- Static fields
- Active threads
- JNI references

If no GC root → object is **unreachable** → eligible for collection.

---

## Reference Types

| Type | GC Behavior | Use Case |
|---|---|---|
| `StrongReference` | never collected if reachable | normal `Object obj = new Object()` |
| `SoftReference<T>` | collected only when OOM | memory-sensitive cache |
| `WeakReference<T>` | collected on next GC | `WeakHashMap`, listeners |
| `PhantomReference<T>` | collected, enqueued for cleanup | post-mortem cleanup |

```java
WeakReference<MyObj> ref = new WeakReference<>(new MyObj());
MyObj obj = ref.get();  // null if GC'd
```

---

## finalize() vs Cleaner

- `finalize()` — **deprecated** (Java 9), unpredictable, avoid
- `Cleaner` (Java 9+) — preferred alternative

```java
Cleaner cleaner = Cleaner.create();
cleaner.register(obj, () -> System.out.println("cleanup"));
```

---

## Common GC Problems

| Symptom | Cause | Fix |
|---|---|---|
| Frequent Full GC | Old Gen filling up | increase `-Xmx`, check for memory leaks |
| `OutOfMemoryError: Java heap space` | heap exhausted | increase heap or fix leak |
| `OutOfMemoryError: GC overhead limit` | >98% time in GC, <2% freed | memory leak, tune heap |
| High latency spikes | STW pause | switch to ZGC/Shenandoah |
| Humongous allocations | large objects bypass Young Gen | reduce allocation size |

---

## Quick Decision

```
Need throughput (batch jobs)?    → ParallelGC
Need balanced (web servers)?     → G1GC (default)
Need ultra-low latency (<1ms)?   → ZGC or Shenandoah
Small app / microservice?        → Serial or G1 with small heap
```
