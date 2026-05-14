# Binary Search

## Core Idea
Eliminate half the search space each iteration → O(log n).
Requires: sorted data or monotonic condition.

---

## Patterns

### 1. Classic — find exact value
```java
int l = 0, r = arr.length - 1;
while (l <= r) {
    int mid = l + (r - l) / 2;
    if (arr[mid] == target) return mid;
    else if (arr[mid] < target) l = mid + 1;
    else r = mid - 1;
}
return -1;
```

### 2. First True — leftmost / minimum valid answer
> [F, F, F, T, T, T] →    first T
```java
int l = 0, r = n;          // r = n (open boundary)
while (l < r) {
    int mid = l + (r - l) / 2;
    if (condition(mid)) r = mid;
    else l = mid + 1;
}
return l;
```
Use: first occurrence, search insert position, first bad version, **minimize answer (Koko)**.

### 3. Last True — rightmost / maximum valid answer
> [T, T, T, F, F, F] → find last T
```java
int l = 0, r = n - 1;
while (l < r) {
    int mid = l + (r - l + 1) / 2;   // ceiling — avoids infinite loop when l=mid
    if (condition(mid)) l = mid;
    else r = mid - 1;
}
return l;
```
Use: last occurrence, maximize answer.

### 4. Rotated Array
```java
while (l <= r) {
    int mid = l + (r - l) / 2;
    if (arr[mid] == target) return mid;
    if (arr[l] <= arr[mid]) {                           // left half sorted
        if (arr[l] <= target && target < arr[mid]) r = mid - 1;
        else l = mid + 1;
    } else {                                            // right half sorted
        if (arr[mid] < target && target <= arr[r]) l = mid + 1;
        else r = mid - 1;
    }
}
```

### 5. 2D Matrix — treat as 1D
Matrix is fully sorted end-to-end (last of row i < first of row i+1).
So flatten mentally: binary search on indices `0 to m*n-1`, convert back to 2D:
- `mid / n` → row (every n indices = one row)
- `mid % n` → column within that row

```
matrix = [[1,3,5,7],     n=4
          [10,11,16,20],
          [23,30,34,60]]

index 5 → row = 5/4 = 1, col = 5%4 = 1 → matrix[1][1] = 11
index 8 → row = 8/4 = 2, col = 8%4 = 0 → matrix[2][0] = 23
```

```java
int l = 0, r = m * n - 1;
while (l <= r) {
    int mid = l + (r - l) / 2;
    int val = matrix[mid / n][mid % n];
    if (val == target) return true;
    else if (val < target) l = mid + 1;
    else r = mid - 1;
}
```

---

## Cheatsheet

| Pattern | loop | on true | on false | mid |
|---|---|---|---|---|
| Classic | `l <= r` | `return mid` | move l or r | floor |
| First True | `l < r` | `r = mid` | `l = mid+1` | floor |
| Last True | `l < r` | `l = mid` | `r = mid-1` | ceiling (+1) |

**Why ceiling for Last True?** — `l = mid` with floor mid causes infinite loop when `l=0, r=1`.

**Why `(p + k - 1) / k` for ceiling division?**
Java `/` always floors: `6/5 = 1` but Koko needs `ceil(6/5) = 2` (needs 2 hours for pile of 6 at speed 5).
Formula: `(p + k - 1) / k`
```
p=6, k=5 → (6+4)/5 = 10/5 = 2 ✓
p=10, k=5 → (10+4)/5 = 14/5 = 2 ✓  (exact, no rounding needed)
p=11, k=5 → (11+4)/5 = 15/5 = 3 ✓
```

---

## Decision Tree
```
Find exact value      → Classic
First occurrence      → First True
Last occurrence       → Last True
Minimize answer       → First True (answer space)
Maximize answer       → Last True (answer space)
Rotated array         → Rotated pattern
2D matrix             → Flatten to 1D
```

---

## Problems

| Level | Problems |
|---|---|
| L1 | LC 704, LC 35, LC 34, LC 278 |
| L2 | LC 33, LC 153, LC 74, LC 162 |
| L3 | LC 875, LC 1011, LC 1482 |
| L4 | LC 410, LC 81, LC 4 |
