# Java Features — 17 & 25 Cheatsheet

---

## Java 17 (LTS — Sept 2021)

### 1. Sealed Classes (finalized)
> Control which classes can extend/implement a class.

```java
public sealed class Shape permits Circle, Rectangle, Triangle {}

public final class Circle    extends Shape {}
public final class Rectangle extends Shape {}
public non-sealed class Triangle extends Shape {}  // open for extension
```
`permits` = whitelist. Pairs with `switch` pattern matching.

---

### 2. Pattern Matching for `instanceof` (finalized)
```java
// Before
if (obj instanceof String) {
    String s = (String) obj;
    System.out.println(s.length());
}

// Java 17
if (obj instanceof String s) {
    System.out.println(s.length());  // s scoped here
}
```

---

### 3. Records (finalized — preview in 14/15/16)
> Immutable data carriers. Auto-generates constructor, getters, `equals`, `hashCode`, `toString`.

```java
public record Point(int x, int y) {}

Point p = new Point(3, 4);
p.x();   // getter
```
- Can add compact constructors, custom methods
- Cannot extend classes (implicitly extends `Record`)

---

### 4. Text Blocks (finalized — preview in 13/14/15)
```java
String json = """
        {
            "name": "Alice",
            "age": 30
        }
        """;
```
- Incidental whitespace stripped based on closing `"""`
- No need to escape `"` inside

---

### 5. Switch Expressions (finalized)
```java
int numLetters = switch (day) {
    case MONDAY, FRIDAY, SUNDAY -> 6;
    case TUESDAY                -> 7;
    default -> {
        int len = day.name().length();
        yield len;   // ← yield in block form
    }
};
```

---

### 6. `RandomGenerator` API (new)
```java
RandomGenerator rng = RandomGenerator.of("L64X128MixRandom");
int n = rng.nextInt(100);
```
Unified interface for all random implementations.

---

### 7. Removed / Deprecated
- **RMI Activation** removed
- **Applet API** deprecated for removal
- **Security Manager** deprecated for removal
- **Experimental AOT/JIT** (Graal-based) removed from JDK

---

## Java 25 (LTS — Sept 2025)

### 1. Virtual Threads (finalized in 21, stable in 25)
> Lightweight threads managed by JVM, not OS. Millions possible.

```java
Thread.ofVirtual().start(() -> {
    // blocking I/O here is fine — JVM unmounts while waiting
    String result = httpClient.get(url);
});

// With ExecutorService
try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
    exec.submit(() -> handleRequest(req));
}
```
- **Don't pool** virtual threads — create per task
- Avoid `synchronized` on pinned operations → use `ReentrantLock`

---

### 2. Structured Concurrency (finalized ~Java 25)
> Treat multiple concurrent tasks as a single unit of work.

```java
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    Future<String> user    = scope.fork(() -> fetchUser(id));
    Future<Integer> orders = scope.fork(() -> fetchOrders(id));
    scope.join().throwIfFailed();
    return new Response(user.resultNow(), orders.resultNow());
}
```
- If one task fails → others cancelled automatically
- Scope lifetime = enclosing try block (no leaks)

---

### 3. Scoped Values (finalized ~Java 25)
> Immutable, thread-local-like values scoped to a call tree. Better than `ThreadLocal` for virtual threads.

```java
static final ScopedValue<User> CURRENT_USER = ScopedValue.newInstance();

ScopedValue.where(CURRENT_USER, user).run(() -> {
    processOrder();   // CURRENT_USER.get() works anywhere in call tree
});
```
- Read-only (no `set()`)
- Automatically freed when scope exits

---

### 4. Pattern Matching for `switch` (finalized)
```java
static String format(Object obj) {
    return switch (obj) {
        case Integer i -> "int: " + i;
        case String s  -> "str: " + s;
        case null      -> "null";
        default        -> "other";
    };
}
```

```java
// Guarded patterns
case Integer i when i > 0 -> "positive int";
```

---

### 5. Record Patterns (finalized)
```java
record Point(int x, int y) {}

if (obj instanceof Point(int x, int y)) {
    System.out.println(x + ", " + y);  // destructured directly
}

// In switch
switch (shape) {
    case Circle(double r)         -> Math.PI * r * r;
    case Rectangle(double w, double h) -> w * h;
}
```

---

### 6. String Templates (preview → finalized ~25)
```java
String name = "Alice";
int age = 30;
String msg = STR."Hello \{name}, you are \{age} years old.";
```
- `STR` = built-in template processor
- Custom processors possible (e.g., for SQL, JSON — safe interpolation)

---

### 7. Unnamed Classes & Instance `main` (preview → ~25)
```java
// No class declaration needed for simple scripts
void main() {
    System.out.println("Hello");
}
```
Good for scripting, education, small utilities.

---

### 8. Primitive Types in Patterns (preview ~25)
```java
switch (value) {
    case int i when i < 0  -> "negative";
    case int i             -> "non-negative";
    case double d          -> "double";
}
```
Extends pattern matching to primitives.

---

### 9. Sequenced Collections (finalized Java 21, stable 25)
```java
SequencedCollection<String> list = new ArrayList<>(List.of("a","b","c"));
list.getFirst();   // "a"
list.getLast();    // "c"
list.reversed();   // reversed view
```
Uniform API across `List`, `Deque`, `LinkedHashSet`, `LinkedHashMap`.

---

## Quick Comparison

| Feature | Java 17 | Java 25 |
|---|---|---|
| Records | Finalized | Stable |
| Sealed Classes | Finalized | Stable |
| Pattern instanceof | Finalized | Stable |
| Switch Expressions | Finalized | Stable |
| Text Blocks | Finalized | Stable |
| Virtual Threads | — | Finalized (21→25) |
| Structured Concurrency | — | Finalized |
| Scoped Values | — | Finalized |
| Record Patterns | — | Finalized |
| Switch Pattern Matching | — | Finalized |
| String Templates | — | Finalized |
| Unnamed Classes | — | Finalized |

---

## Interview Hits

- **Records vs Lombok `@Data`** → Records are immutable, no setters, no inheritance
- **Sealed + switch** → exhaustive pattern matching, compiler checks all cases
- **Virtual threads vs platform threads** → M:N mapping, no OS thread per task
- **Scoped Values vs ThreadLocal** → immutable, no memory leaks, works with virtual threads
- **Structured Concurrency** → fork-join with automatic cancellation on failure
