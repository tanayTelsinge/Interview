# Binary Search

## Core Idea
Eliminate half the search space each iteration → **O(log n) instead of O(n)**.

Requires: **sorted** data (or a **monotonic condition** — answer space version).

---

## Flow Diagrams

### Classic Binary Search — how pointers move
```
arr = [1, 3, 5, 7, 9, 11]   target = 7
       0  1  2  3  4   5

Step 1:  left=0  right=5  mid=2  arr[2]=5 < 7  → left = mid+1 = 3
Step 2:  left=3  right=5  mid=4  arr[4]=9 > 7  → right = mid-1 = 3
Step 3:  left=3  right=3  mid=3  arr[3]=7 == 7 → return 3 ✓

[1] [3] [5] [7] [9] [11]
 ↑           ↑        ↑
left        mid      right   (step 1)

             ↑   ↑    ↑
            left mid right   (step 2)

             ↑
          left=mid=right      (step 3 → found)
```

### Lower Bound — how `right` closes in on first occurrence
```
arr = [1, 3, 5, 5, 5, 9]   target = 5  (find FIRST 5)
       0  1  2  3  4  5

right = arr.length = 6  (open boundary)

Step 1:  left=0  right=6  mid=3  arr[3]=5 >= 5  → right = mid = 3
Step 2:  left=0  right=3  mid=1  arr[1]=3 < 5   → left = mid+1 = 2
Step 3:  left=2  right=3  mid=2  arr[2]=5 >= 5  → right = mid = 2
Step 4:  left=2  right=2  → exit, return left = 2 ✓

Key: right = mid (not mid-1) so we NEVER exclude the answer
```

### Upper Bound — how we keep going right to find last occurrence
```
arr = [1, 3, 5, 5, 5, 9]   target = 5  (find LAST 5)
       0  1  2  3  4  5

Step 1:  left=0  right=5  mid=2  arr[2]=5 == 5  → ans=2, left = mid+1 = 3
Step 2:  left=3  right=5  mid=4  arr[4]=5 == 5  → ans=4, left = mid+1 = 5
Step 3:  left=5  right=5  mid=5  arr[5]=9 > 5   → right = mid-1 = 4
Step 4:  left=5 > right=4 → exit, return ans = 4 ✓

Key: on match, save ans but keep going RIGHT (left = mid+1)
```

### Answer Space — searching over answer range, not the array
```
piles = [3, 6, 7, 11]  h = 8   (Koko: min speed to eat all in 8 hours)

Answer range: left=1 (min speed)  right=11 (max pile)

Step 1:  mid=6   feasible(6)? hours = 1+1+2+2 = 6 ≤ 8  YES → right = 6
Step 2:  mid=3   feasible(3)? hours = 1+2+3+4 = 10 > 8  NO  → left = 4
Step 3:  mid=5   feasible(5)? hours = 1+2+2+3 = 8 ≤ 8  YES → right = 5
Step 4:  mid=4   feasible(4)? hours = 1+2+2+3 = 8 ≤ 8  YES → right = 4
Step 5:  left=4 == right=4 → return 4 ✓

Key: not searching the array — searching the ANSWER RANGE [1..maxPile]
```

### Rotated Array — which half is always sorted?
```
arr = [4, 5, 6, 7, 0, 1, 2]   target = 0
       0  1  2  3  4  5  6

Step 1:  left=0  right=6  mid=3  arr[3]=7
         arr[left]=4 <= arr[mid]=7  → LEFT half [4,5,6,7] is sorted
         target=0 NOT in [4..7)     → go right: left = mid+1 = 4

Step 2:  left=4  right=6  mid=5  arr[5]=1
         arr[left]=0 <= arr[mid]=1  → LEFT half [0,1] is sorted
         target=0 IN [0..1)         → go left: right = mid-1 = 4

Step 3:  left=4  right=4  mid=4  arr[4]=0 == 0 → return 4 ✓

Rule: arr[left] <= arr[mid] means LEFT is sorted, otherwise RIGHT is sorted
```

---

## 1. Classic Binary Search
> "does target exist?"

```java
int left = 0, right = arr.length - 1;
while (left <= right) {
    int mid = left + (right - left) / 2;   // avoids overflow vs (l+r)/2
    if (arr[mid] == target) return mid;
    else if (arr[mid] < target) left = mid + 1;
    else right = mid - 1;
}
return -1;
```

---

## 2. Lower Bound — First True / Leftmost Position
> "first index where condition is true" (e.g. first occurrence, first ≥ target)

```java
int left = 0, right = arr.length;   // right = n (open), not n-1
while (left < right) {              // NOT <=
    int mid = left + (right - left) / 2;
    if (condition(mid)) right = mid; // mid could be the answer → don't exclude it
    else left = mid + 1;
}
return left;   // left == right == insertion point
```

**Use when:** find first occurrence, search insert position, first bad version.

---

## 3. Upper Bound — Last True / Rightmost Position
> "last index where condition is true" (e.g. last occurrence, last ≤ target)

```java
int left = 0, right = arr.length - 1, ans = -1;
while (left <= right) {
    int mid = left + (right - left) / 2;
    if (arr[mid] == target) {
        ans = mid;
        left = mid + 1;   // keep going right to find last
    } else if (arr[mid] < target) left = mid + 1;
    else right = mid - 1;
}
return ans;
```

---

## 4. Binary Search on Answer Space
> "minimize/maximize X such that condition holds" — search over the *answer*, not the array

```java
int left = minPossible, right = maxPossible;
while (left < right) {
    int mid = left + (right - left) / 2;
    if (feasible(mid)) right = mid;   // mid works → try smaller
    else left = mid + 1;
}
return left;
```

**Key insight:** you're not searching in the array — you search over the *range of possible answers*.
`feasible(mid)` usually involves a linear scan of the array.

**TC: O(n log(range))** where range = maxPossible - minPossible.

Examples: Koko eating bananas, capacity to ship packages, split array largest sum.

---

## 5. Rotated Sorted Array
> "array was sorted then rotated at unknown pivot"

**Trick:** one half is always sorted — figure out which, then decide which half contains target.

```java
while (left <= right) {
    int mid = left + (right - left) / 2;
    if (arr[mid] == target) return mid;

    if (arr[left] <= arr[mid]) {           // LEFT half is sorted
        if (arr[left] <= target && target < arr[mid]) right = mid - 1;
        else left = mid + 1;
    } else {                               // RIGHT half is sorted
        if (arr[mid] < target && target <= arr[right]) left = mid + 1;
        else right = mid - 1;
    }
}
```

**Find minimum in rotated array:** minimum is where the sorted order breaks.
```java
// arr[mid] > arr[right] → min is in right half, else left half (include mid)
if (arr[mid] > arr[right]) left = mid + 1;
else right = mid;
```

---

## 6. Search in 2D Matrix
> "m×n matrix, rows sorted, first element of each row > last of previous row"

**Treat as 1D array:** index `k` → row `k/n`, col `k%n`

```java
int left = 0, right = m * n - 1;
while (left <= right) {
    int mid = left + (right - left) / 2;
    int val = matrix[mid / n][mid % n];
    if (val == target) return true;
    else if (val < target) left = mid + 1;
    else right = mid - 1;
}
```

---

## Template Cheatsheet

| Variant | `right` init | Loop condition | On match |
|---|---|---|---|
| Classic (exact) | `n - 1` | `<=` | `return mid` |
| Lower bound | `n` | `<` | `right = mid` |
| Upper bound | `n - 1` | `<=` | `ans = mid; left = mid+1` |
| Answer space | `maxVal` | `<` | `right = mid` |

---

## `left + (right - left) / 2` — why not `(left + right) / 2`?
`left + right` can overflow `int` when both are large. Always use safe mid.

---

## Common Mistakes

1. **Off-by-one on `right`**: lower bound needs `right = arr.length` (open boundary), classic needs `arr.length - 1`
2. **Infinite loop**: using `right = mid` with `left <= right` → loop never terminates. Pair lower bound template with `left < right`
3. **Wrong half check in rotated**: always compare `arr[left] <= arr[mid]`, not `arr[mid] <= arr[right]` (handles duplicates poorly)
4. **Answer space `feasible`**: must be monotonic — if mid works, everything ≥ mid also works (or vice versa)

---

## Decision Tree

```
Sorted array or sorted condition?
├── Find exact value
│   ├── Standard sorted → Classic BS (LC 704)
│   ├── Rotated sorted → Rotated BS (LC 33, 81, 153)
│   └── 2D sorted matrix → Flatten to 1D (LC 74)
├── Find boundary / position
│   ├── First occurrence / insertion point → Lower Bound (LC 34, 35, 278)
│   └── Last occurrence → Upper Bound (LC 34)
└── Minimize / Maximize answer
    ├── "minimum X such that feasible(X)" → Answer Space BS
    │   ├── Rate/capacity problems → LC 875, LC 1011, LC 1482
    │   └── Split/partition problems → LC 410, LC 1231
    └── Peak / local extrema → LC 162 (modified BS, no sorted requirement)
```

---

## Problem List

| Pattern | Problems |
|---|---|
| Classic | LC 704, LC 35, LC 278 |
| First / Last position | LC 34 |
| Rotated Array | LC 33, LC 81, LC 153, LC 154 |
| 2D Matrix | LC 74, LC 240 |
| Answer Space | LC 875, LC 1011, LC 410, LC 1482 |
| Peak Element | LC 162, LC 852 |

### Level Order
```
L1 (foundation): LC 704 (Binary Search) → LC 35 (Search Insert Position) → LC 34 (First and Last Position) → LC 278 (First Bad Version)
L2 (core FAANG): LC 33 (Search in Rotated Array) → LC 153 (Find Min in Rotated Array) → LC 74 (Search a 2D Matrix) → LC 162 (Find Peak Element)
L3 (answer space): LC 875 (Koko Eating Bananas) → LC 1011 (Capacity to Ship Packages) → LC 1482 (Min Days to Make Bouquets)
L4 (senior): LC 410 (Split Array Largest Sum) → LC 81 (Rotated with Duplicates) → LC 4 (Median of Two Sorted Arrays)
```
