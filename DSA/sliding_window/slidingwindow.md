# Sliding Window

## Core Idea
Add one element on the right, remove one on the left → **O(n) instead of O(n²)**.

Two variants: **Fixed** (size = k) | **Dynamic** (size based on condition)

---

## 1. Fixed Window
> "subarray/substring of size k"

```java
for (int i = 0; i < arr.length; i++) {
    windowSum += arr[i];
    if (i >= k - 1) {
        maxSum = Math.max(maxSum, windowSum);
        windowSum -= arr[i - (k - 1)];   // left = i - k + 1
    }
}
```

---

## 2. Dynamic Window
> "find smallest/longest subarray where condition holds"

```java
int left = 0, sum = 0, minLen = Integer.MAX_VALUE;
for (int right = 0; right < arr.length; right++) {
    sum += arr[right];
    while (sum >= k) {
        minLen = Math.min(minLen, right - left + 1);
        sum -= arr[left++];
    }
}
return minLen == Integer.MAX_VALUE ? 0 : minLen;
```

**TC: O(n)** — each element touched at most twice (`right` adds, `left` removes). `left` never resets.
**SC: O(1)**

---

## 3. AtMost(K) — Count Subarrays
> "count subarrays with **exactly K** distinct"
**Trick:** `exactly(K) = atMost(K) - atMost(K-1)`

```java
public static int atMostK(int[] arr, int k) {
    int left = 0, count = 0;
    Map<Integer, Integer> freq = new HashMap<>();
    for (int right = 0; right < arr.length; right++) {
        freq.merge(arr[right], 1, Integer::sum);
        while (freq.size() > k) {
            freq.merge(arr[left], -1, Integer::sum);
            if (freq.get(arr[left]) == 0) freq.remove(arr[left]);
            left++;
        }
        count += right - left + 1;   // all valid subarrays ending at right
    }
    return count;
}
```

---

## 4. String Window — need/have
> "minimum window containing all chars of t" (LC 76)

**The trick:** instead of checking the entire map each step, maintain two counters:
- `need` = `needMap.size()` — how many distinct chars must be satisfied
- `have` = how many distinct chars are currently satisfied in the window

A char is "satisfied" when `haveMap.get(c) == needMap.get(c)`.
`have == need` → window is valid in **O(1)**, no map scan needed.

```java
// BUILD need map from t
int need = needMap.size(), have = 0;

// EXPAND right
if (needMap.containsKey(curr)) {
    haveMap.merge(curr, 1, Integer::sum);
    if (haveMap.get(curr).equals(needMap.get(curr))) have++;  // this char is now satisfied
}

// SHRINK left while valid
while (have == need) {
    // record best window here
    if (needMap.containsKey(leftChar)) {
        haveMap.merge(leftChar, -1, Integer::sum);
        if (haveMap.get(leftChar) < needMap.get(leftChar)) have--;  // lost satisfaction
    }
    l++;
}
```

**When to use need/have:**
| Signal | Use need/have |
|---|---|
| Window must contain all chars of another string | yes |
| Frequency of each char must match exactly | yes |
| Just checking distinct count (not frequency) | no — use a single counter |
| Fixed-size window (anagram/permutation) | yes — same trick, skip the while |

**Broader pattern — "Satisfy & Count":**
> Don't validate by scanning — maintain a counter of how many conditions are fully met. Update it only at the exact moment a condition is crossed (up or down). Applies beyond sliding window: meeting rooms, interval scheduling, anagram grouping.
> `O(n²t) brute → O(n×alphabet) naive window → O(n) with need/have`

```java
// Generic Satisfy & Count template
int need = requirementMap.size(), have = 0;
for (each element) {
    // expand: update state
    if (condition crosses threshold upward) have++;

    // check / shrink
    while (have == need) {
        // record answer
        if (condition crosses threshold downward) have--;
        // move left pointer / remove element
    }
}
```

---

## 5. LC 424 — maxFreq Trick
> "max length after replacing ≤k chars"
Use **`if` not `while`** — window only slides, never shrinks (we want maximum).

```
invalid: (windowSize - maxFreq) > k → slide by 1
```

---

## 6. Prefix Sum + HashMap
> "count subarrays with sum = k" (LC 560)

```java
Map<Integer, Integer> prefixCount = new HashMap<>();
prefixCount.put(0, 1);   // ← don't forget this
int sum = 0, count = 0;
for (int num : nums) {
    sum += num;
    count += prefixCount.getOrDefault(sum - k, 0);
    prefixCount.merge(sum, 1, Integer::sum);
}
```

For **divisibility**: store `sum % k`. For **odd count**: convert odd→+1, even→-1.

---

## 7. Reverse Fixed Window (LC 1423)
> "take k cards from ends" = minimize middle window of size `n-k`

```java
return total - minWindowSum(cardPoints, n - k);
```

---

## `while` vs `if` when shrinking

| `while` | `if` |
|---|---|
| Want **minimum** window | Want **maximum** window |
| LC 76, 209, 3 | LC 424 |

## Common Mistakes
1. Fixed window left: `i - k + 1`, not `i - k`
2. Forget `prefixCount.put(0, 1)` → misses subarrays from index 0
3. `maxFreq` in LC 424 never decreases — intentional
4. `freq.size()` = distinct count, not sum

---

## Decision Tree

```
Contiguous subarray/substring?
├── String?
│   ├── Min window with all chars → need/have (LC 76)
│   ├── Permutation/anagram → Fixed + int[26] (LC 567, 438)
│   └── Max length after k replacements → maxFreq + if (LC 424)
└── Array?
    ├── Fixed size k → Fixed Window
    │   └── Take from both ends → Reverse: minimize middle (LC 1423)
    └── Variable size
        ├── Min/max length, sum ≥ target → Dynamic + while
        ├── Count exact condition
        │   ├── Distinct elements → atMost(K) - atMost(K-1)
        │   └── Sum = K / mod K → Prefix Sum + HashMap
        └── Max/Min in every window → Monotonic Deque
```

---

## Problem List

| Pattern | Problems |
|---|---|
| Fixed / Dynamic | LC 209, LC 1423, LC 487 |
| String Window | LC 76, LC 567, LC 438, LC 3, LC 424 |
| AtMost(K) | LC 992, LC 1004, LC 713, LC 904 |
| Prefix Sum | LC 560, LC 523, LC 1248 |
| Monotonic Deque | LC 239, LC 862, LC 1438 |
| Two Heaps | LC 480, LC 295 |

### Level Order
```
L1 (foundation): LC 3 (Longest Substring No Repeat) → LC 209 (Min Size Subarray Sum) → LC 567 (Permutation in String) → LC 11 (Container With Most Water) → LC 560 (Subarray Sum Equals K)
L2 (core FAANG): LC 438 (Find All Anagrams) → LC 424 (Longest Repeating Char Replacement) → LC 76 (Min Window Substring) → LC 1423 (Max Points from Cards) → LC 239 (Sliding Window Maximum)
L3 (high freq):  LC 1004 (Max Consecutive Ones III) → LC 992 (Subarrays with K Different Ints) → LC 523 (Continuous Subarray Sum) → LC 159 (Longest Substr At Most 2 Distinct) → LC 904 (Fruit Into Baskets)
L4 (senior):     LC 295 (Find Median from Data Stream) → LC 480 (Sliding Window Median) → LC 862 (Shortest Subarray Sum ≥ K)
```
