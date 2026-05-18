# equals() & hashCode()

## The Simple Rule
- `equals()` → are these two objects **logically the same**?
- `hashCode()` → give me a **number** to quickly find this object in a bucket

> **If you override one, override both. Always.**

---

## How HashMap Uses Both

```
Step 1: hashCode() → which bucket to go to
Step 2: equals()   → which exact object in that bucket
```

If `hashCode` is wrong → land in wrong bucket → object not found
If `equals` is wrong → can't identify the right object in the bucket

---

## Default Behavior (without override)

```java
new Point(1,2).equals(new Point(1,2))   // false  (different objects in memory)
new Point(1,2).hashCode()               // based on memory address — different every time
```

---

## The Contract (plain english)

**equals must be:**
- `a.equals(a)` → always true
- `a.equals(b)` → same as `b.equals(a)` (no one-sided relationships)
- if `a=b` and `b=c` then `a=c`
- `a.equals(null)` → always false

**hashCode must be:**
- same object → same number every time
- if `a.equals(b)` is true → `a.hashCode() == b.hashCode()` (mandatory)
- if `a.equals(b)` is false → hashCodes *can* still be equal (that's a collision, it's okay)

---

## Correct Override

```java
class Point {
    int x, y;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;                         // same object, shortcut
        if (o == null || getClass() != o.getClass())        // null or wrong type
            return false;
        Point p = (Point) o;
        return x == p.x && y == p.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);                          // combine fields
    }
}
```

---

## What Goes Wrong

### 1. Override equals but NOT hashCode
```java
// equals says (1,2) == (1,2) ✓
// but hashCode gives different numbers for each → different buckets
map.put(new Point(1,2), "A");
map.get(new Point(1,2));  // null — went to wrong bucket
```

### 2. Mutate the key after putting in map
```java
Point p = new Point(1, 2);
map.put(p, "A");
p.x = 99;              // hashCode changed!
map.get(p);            // looks in wrong bucket → null
// entry still exists in map but you can never find it again
```

### 3. hashCode always returns same number
```java
@Override public int hashCode() { return 42; }
// Correct but everything piles into one bucket
// HashMap becomes a linked list → O(n) lookup
```

### 4. equals always returns true
```java
@Override public boolean equals(Object o) { return true; }
// HashSet — every add() looks like a duplicate → set stays size 1
// HashMap — every put() overwrites the same slot
```

### 5. equals true but hashCode different — worst bug
```java
// a.equals(b) == true  but  a.hashCode() != b.hashCode()
map.put(a, "value");
map.get(b);  // b equal to a but different hash → wrong bucket → null
// no exception, just silently broken
```

---

## instanceof vs getClass()

```java
// instanceof problem with subclass
class Animal { String name; }
class Dog extends Animal {}

Animal a = new Animal(); a.name = "Rex";
Dog    d = new Dog();    d.name = "Rex";

a.equals(d)  // true  (d instanceof Animal → yes)
d.equals(a)  // false (a instanceof Dog → no)  ← NOT symmetric!
```

Use `getClass()` for concrete classes — guarantees symmetry.
Use `instanceof` only when you intentionally want subclass equality (rare).

---

## Tricky Coding Questions

### Q1: What is the output?
```java
class Box {
    int val;
    Box(int val) { this.val = val; }
    // no equals/hashCode
}

Set<Box> set = new HashSet<>();
Box b = new Box(1);
set.add(b);
b.val = 99;
set.add(new Box(1));
System.out.println(set.size());
```
<details>
<summary>Answer</summary>

**2** — No override means reference equality. `b` and `new Box(1)` are different objects → both added regardless of val.

</details>

---

### Q2: What is A and B?
```java
class Key {
    int id;
    Key(int id) { this.id = id; }
    @Override public boolean equals(Object o) { return o instanceof Key k && k.id == id; }
    @Override public int hashCode() { return id; }
}

Map<Key, String> map = new HashMap<>();
Key k = new Key(1);
map.put(k, "hello");
k.id = 2;                         // mutated!
System.out.println(map.get(k));   // A
System.out.println(map.size());   // B
```
<details>
<summary>Answer</summary>

**A = null, B = 1**

hashCode now returns 2 → looks in bucket 2 → entry is still in bucket 1 → not found.
Entry exists (size=1) but is permanently orphaned.

</details>

---

### Q3: Find all bugs
```java
class Person {
    String name;
    @Override
    public boolean equals(Object o) {
        Person p = (Person) o;
        return name.equals(p.name);
    }
}
```
<details>
<summary>Answer (4 bugs)</summary>

1. No null check → `equals(null)` throws `ClassCastException`
2. No type check → passing a non-Person throws `ClassCastException`
3. `name` could be null → NPE
4. Missing `hashCode()` → HashMap/HashSet broken

**Fixed:**
```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Person p)) return false;
    return Objects.equals(name, p.name);  // null-safe
}
@Override public int hashCode() { return Objects.hash(name); }
```

</details>

---

### Q4: What is the size?
```java
class Point {
    int x, y;
    Point(int x, int y) { this.x = x; this.y = y; }
    @Override public boolean equals(Object o) {
        if (!(o instanceof Point p)) return false;
        return x == p.x && y == p.y;
    }
    @Override public int hashCode() { return x; }  // only x!
}

Set<Point> set = new HashSet<>();
set.add(new Point(1, 1));
set.add(new Point(1, 2));
System.out.println(set.size());
```
<details>
<summary>Answer</summary>

**2** — Both hash to bucket 1 (same x). Then equals checks y → different → both stored.
Correct result but bad hashCode — all points with same x cause collisions.

</details>

---

### Q5: What is the size?
```java
record User(int id, String name) {}

Set<User> set = new HashSet<>();
set.add(new User(1, "Alice"));
set.add(new User(1, "Alice"));
set.add(new User(1, "Bob"));
System.out.println(set.size());
```
<details>
<summary>Answer</summary>

**2** — Records auto-generate equals/hashCode on ALL fields.
- `User(1,"Alice")` == `User(1,"Alice")` → duplicate, rejected
- `User(1,"Bob")` → different name → different object → added

</details>

---

### Q6: Trace what happens
```java
// objA.hashCode() = 5, objB.hashCode() = 5
// objA.equals(objB) = false
map.put(objA, "A");
map.put(objB, "B");
map.get(objA);  // what does this return and how?
```
<details>
<summary>Answer</summary>

Returns `"A"`.

1. `objA.hashCode()` → 5 → go to bucket 5
2. Bucket 5 has: `[objA→"A"] → [objB→"B"]` (chained)
3. `equals(objA, objA)` → true → return `"A"` ✓

Both entries coexist via chaining. Collision handled correctly.

</details>
