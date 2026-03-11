# Sliding Window Template & Decision Guide

## Core Idea

A sliding window avoids recomputing from scratch on every step.
Instead, you **add one element on the right** and **remove one on the left** — O(n) instead of O(n²).

Two variants:
- **Fixed window** — size is always `k`
- **Dynamic window** — size grows/shrinks based on a condition

---

## 1. Fixed Window (Size K)

> Use when: problem says "subarray/substring of size k"

```
Visualise on arr = [2,1,5,1,3,2], k=3:
  [2,1,5] → sum=8
    [1,5,1] → sum=7
      [5,1,3] → sum=9  ← max
        [1,3,2] → sum=6
```

```java
public static int maxSumSubArray(int[] arr, int k) {
    int maxSum = Integer.MIN_VALUE;
    int windowSum = 0;

    for (int i = 0; i < arr.length; i++) {
        windowSum += arr[i];                          // expand right

        if (i >= k - 1) {                             // window is full
            maxSum = Math.max(maxSum, windowSum);
            windowSum -= arr[i - (k - 1)];            // shrink left
        }
    }
    return maxSum;
}
```

**Key formula:** left = `i - (k - 1)` — the element that just fell out of the window.

---

## 2. Dynamic Window (Variable Size)

> Use when: "find smallest/longest subarray where condition holds"

**Pattern:** right always moves forward; left shrinks only when condition is violated.

```
right expands → condition met → record answer → left shrinks → repeat
```

```java
// Example: minimum length subarray with sum >= k
public static int minSubArrayLen(int k, int[] arr) {
    int left = 0, sum = 0;
    int minLen = Integer.MAX_VALUE;

    for (int right = 0; right < arr.length; right++) {
        sum += arr[right];                            // expand

        while (sum >= k) {                            // condition met → try to shrink
            minLen = Math.min(minLen, right - left + 1);
            sum -= arr[left++];                       // shrink
        }
    }
    return (minLen == Integer.MAX_VALUE) ? 0 : minLen;
}
```

**Window size formula:** `right - left + 1`

---

## 3. AtMost(K) Template

> Use when: "count subarrays with **exactly K** distinct / odd / etc."

**Trick:** `exactly(K)` = `atMost(K) - atMost(K-1)`

Why `count += right - left + 1`?
Every new `right` forms `(right - left + 1)` valid subarrays ending at `right`.

```java
public static int atMostK(int[] arr, int k) {
    int left = 0, count = 0;
    Map<Integer, Integer> freq = new HashMap<>();

    for (int right = 0; right < arr.length; right++) {
        freq.merge(arr[right], 1, Integer::sum);      // add right to window

        while (freq.size() > k) {                     // violated → shrink
            freq.merge(arr[left], -1, Integer::sum);
            if (freq.get(arr[left]) == 0) freq.remove(arr[left]);
            left++;
        }

        count += right - left + 1;                    // all valid subarrays ending at right
    }
    return count;
}
```

## 4. String Character Window — need/have Pattern

> Use when: "find minimum window in s containing all chars of t" (LC 76, 567, 438)

**Pattern:** Track `formed` (how many chars are satisfied) vs `required` (total distinct chars needed).
Window is valid only when `formed == required`.

```java
// LC 76 — Minimum Window Substring
public String minWindow(String s, String t) {
    int[] need = new int[128];
    int[] have = new int[128];

    for (char c : t.toCharArray()) need[c]++;

    int required = 0;
    for (int n : need) if (n > 0) required++;   // distinct chars we must satisfy

    int formed = 0, left = 0;
    int minLen = Integer.MAX_VALUE, start = 0;

    for (int right = 0; right < s.length(); right++) {
        char c = s.charAt(right);
        have[c]++;
        if (need[c] > 0 && have[c] == need[c]) formed++;   // one more char satisfied

        while (formed == required) {                         // valid → try to shrink
            if (right - left + 1 < minLen) {
                minLen = right - left + 1;
                start = left;
            }
            char l = s.charAt(left++);
            have[l]--;
            if (need[l] > 0 && have[l] < need[l]) formed--; // lost a satisfied char
        }
    }
    return minLen == Integer.MAX_VALUE ? "" : s.substring(start, start + minLen);
}
```

**Key insight:** `formed` only increments when `have[c]` hits exactly `need[c]` (not above). This avoids recounting extras.

---

## 5. Fixed String Window — char array (Anagram / Permutation)

> Use when: "does s2 contain a permutation of s1?" or "find all anagrams" (LC 567, 438)

**Optimization:** For lowercase-only problems, `int[26]` is faster than HashMap.

```java
// LC 567 — Permutation in String
public boolean checkInclusion(String s1, String s2) {
    if (s1.length() > s2.length()) return false;

    int[] need = new int[26];
    int[] have = new int[26];
    for (char c : s1.toCharArray()) need[c - 'a']++;

    int k = s1.length();
    for (int i = 0; i < s2.length(); i++) {
        have[s2.charAt(i) - 'a']++;                     // expand right

        if (i >= k) have[s2.charAt(i - k) - 'a']--;    // shrink left (fixed window)

        if (Arrays.equals(need, have)) return true;      // anagram found
    }
    return false;
}
```

For **Find All Anagrams (LC 438):** same code, collect `left` index when `Arrays.equals` is true.

---

## 6. LC 424 — maxFreq Trick (Character Replacement)

> Use when: "max length substring after replacing at most k characters"

**The non-obvious trick:** You don't need to shrink aggressively. Just keep `if` (not `while`) — never reduce the window size, only slide it.

```
invalid condition: (windowSize - maxFreq) > k
  → replacements needed > k → must shrink by 1
```

```java
// LC 424 — Longest Repeating Character Replacement
public int characterReplacement(String s, int k) {
    int[] freq = new int[26];
    int left = 0, maxFreq = 0;

    for (int right = 0; right < s.length(); right++) {
        freq[s.charAt(right) - 'A']++;
        maxFreq = Math.max(maxFreq, freq[s.charAt(right) - 'A']);

        if (right - left + 1 - maxFreq > k) {           // invalid → slide (not shrink)
            freq[s.charAt(left) - 'A']--;
            left++;
        }
    }
    return s.length() - left;   // window never shrinks, only slides
}
```

**Why `if` not `while`?** We want the *maximum* window ever seen. Once window reaches size X, we only care if it can grow bigger — no point shrinking below X.

---

## 7. Prefix Sum + HashMap

> Use when: "count subarrays with sum = k" or "divisible by k" (LC 560, 523, 1248)

**Core insight:** `sum[right] - sum[left] = k` → `sum[left] = sum[right] - k`
So for each `right`, check how many previous prefixes equal `currentSum - k`.

```java
// LC 560 — Subarray Sum Equals K
public int subarraySum(int[] nums, int k) {
    Map<Integer, Integer> prefixCount = new HashMap<>();
    prefixCount.put(0, 1);   // empty prefix (sum=0 seen once)

    int sum = 0, count = 0;
    for (int num : nums) {
        sum += num;
        count += prefixCount.getOrDefault(sum - k, 0);  // subarrays ending here with sum=k
        prefixCount.merge(sum, 1, Integer::sum);
    }
    return count;
}
```

**For divisibility (LC 523):** store `sum % k` in map instead of `sum`.
**For odd count (LC 1248):** convert to `+1/-1` array (odd→+1, even→-1), then find subarrays with sum = k.

---

## 8. Max Points from Cards — Reverse Fixed Window (LC 1423)

> FAANG/Fintech favorite — disguised as a card game but it's a fixed window problem.

**Insight:** Taking k cards from ends = removing a **contiguous subarray of size (n-k)** from the middle.
→ maximize cards taken = minimize the middle window sum.

```java
public int maxScore(int[] cardPoints, int k) {
    int total = 0;
    for (int p : cardPoints) total += p;

    int n = cardPoints.length;
    int windowSize = n - k;       // the middle part we DON'T take
    int windowSum = 0, minSum = Integer.MAX_VALUE;

    for (int i = 0; i < n; i++) {
        windowSum += cardPoints[i];
        if (i >= windowSize - 1) {
            minSum = Math.min(minSum, windowSum);
            windowSum -= cardPoints[i - (windowSize - 1)];
        }
    }
    return total - minSum;
}
```

---

## Key Pitfalls

### `while` vs `if` when shrinking

| Use `while` | Use `if` |
|---|---|
| Want **minimum** window (keep shrinking until invalid) | Want **maximum** window (only slide, never shrink) |
| LC 76, 209, 3 | LC 424, and most "longest" problems with `if` variant |

### Common mistakes

1. **Off-by-one on fixed window left:** left = `i - k + 1`, not `i - k`
2. **Forgetting `prefixCount.put(0, 1)`** in prefix sum → misses subarrays starting at index 0
3. **`maxFreq` in LC 424 never decreases** — this is intentional, the window only slides
4. **`freq.size()` vs sum check in atMost(K)** — `size()` = distinct count, don't confuse with sum

---

## Monotonic Queue (Deque) Template

```java
class MonotonicQueue {
    Deque<Integer> deque = new LinkedList<>();
    public void push(int num) {
        while (!deque.isEmpty() && deque.peekLast() < num) {
            deque.pollLast(); // Maintain decreasing order
        }
        deque.addLast(num);
    }
    public void pop(int num) {
        if (!deque.isEmpty() && deque.peekFirst() == num) {
            deque.pollFirst(); // Remove if it was the max element
        }
    }
    public int max() {
        return deque.peekFirst(); // Max element always at front
    }
}
```

## Heap (Priority Queue) Template

```java
class MedianFinder {
    private PriorityQueue<Integer> small = new PriorityQueue<>(Collections.reverseOrder()); // Max heap
    private PriorityQueue<Integer> large = new PriorityQueue<>(); // Min heap
    
    public void addNum(int num) {
        small.add(num);
        large.add(small.poll()); // Balance heaps
        if (small.size() < large.size()) {
            small.add(large.poll()); // Ensure max heap has equal/more elements
        }
    }
    
    public double findMedian() {
        return small.size() > large.size() ? small.peek() : (small.peek() + large.peek()) / 2.0;
    }
}
```

## Expand & Shrink (Two Pointers) Template

```java
public static int maxArea(int[] height) {
    int left = 0, right = height.length - 1, maxArea = 0;
    while (left < right) {
        maxArea = Math.max(maxArea, (right - left) * Math.min(height[left], height[right])); // Compute area
        if (height[left] < height[right]) left++; // Move pointer with smaller height
        else right--;
    }
    return maxArea;
}
```
## Decision Tree: Subarray / Substring Problems

```
Is the problem about contiguous subarray/substring?
│
├── NO → DP (subsequences, knapsack)
│
└── YES
    │
    ├── Input is a STRING?
    │   │
    │   ├── Find min/max window containing all chars of t → need/have pattern (LC 76)
    │   ├── Does permutation / anagram exist? → Fixed window + int[26] (LC 567, 438)
    │   └── Max length after ≤k replacements → maxFreq trick with `if` shrink (LC 424)
    │
    └── Input is an ARRAY?
        │
        ├── Fixed size k given → Fixed Sliding Window
        │   └── Take from both ends? → Reverse: minimize middle window (LC 1423)
        │
        └── Variable size (condition-based)
            │
            ├── Find min/max LENGTH where sum ≥/≤ target → Dynamic Window with `while`
            │
            ├── COUNT subarrays with exact condition
            │   │
            │   ├── Condition = distinct / atmost k elements → atMost(K) - atMost(K-1)
            │   │
            │   └── Condition = sum = K / divisible by K → Prefix Sum + HashMap
            │
            └── Max/Min VALUE in every window of size k → Monotonic Deque
```

## Problem Sets by Pattern

### String Window (need/have + char array)
| Problem | LC | Difficulty | Tag |
|---|---|---|---|
| Minimum Window Substring | [76](https://leetcode.com/problems/minimum-window-substring/) | Hard | need/have |
| Permutation in String | [567](https://leetcode.com/problems/permutation-in-string/) | Medium | int[26] fixed |
| Find All Anagrams in String | [438](https://leetcode.com/problems/find-all-anagrams-in-a-string/) | Medium | int[26] fixed |
| Longest Substring Without Repeating | [3](https://leetcode.com/problems/longest-substring-without-repeating-characters/) | Medium | dynamic + set |
| Longest Substring with At Most 2 Distinct | [159](https://leetcode.com/problems/longest-substring-with-at-most-two-distinct-characters/) | Medium | dynamic + map |
| Longest Repeating Character Replacement | [424](https://leetcode.com/problems/longest-repeating-character-replacement/) | Medium | maxFreq + if |

### AtMost(K) — exactly(K) = atMost(K) - atMost(K-1)
| Problem | LC | Difficulty |
|---|---|---|
| Longest Substring with At Most K Distinct | [340](https://leetcode.com/problems/longest-substring-with-at-most-k-distinct-characters/) | Medium |
| Subarrays with K Different Integers | [992](https://leetcode.com/problems/subarrays-with-k-different-integers/) | Hard |
| Max Consecutive Ones III (≤K flips) | [1004](https://leetcode.com/problems/max-consecutive-ones-iii/) | Medium |
| Subarray Product Less Than K | [713](https://leetcode.com/problems/subarray-product-less-than-k/) | Medium |
| Fruit Into Baskets | [904](https://leetcode.com/problems/fruit-into-baskets/) | Medium |

### Prefix Sum + HashMap — sum = K / divisible
| Problem | LC | Difficulty |
|---|---|---|
| Subarray Sum Equals K | [560](https://leetcode.com/problems/subarray-sum-equals-k/) | Medium |
| Continuous Subarray Sum (divisible) | [523](https://leetcode.com/problems/continuous-subarray-sum/) | Medium |
| Count Number of Nice Subarrays | [1248](https://leetcode.com/problems/count-number-of-nice-subarrays/) | Medium |
| Maximum Size Subarray Sum Equals K | [325](https://leetcode.com/problems/maximum-size-subarray-sum-equals-k/) | Medium |

### Fixed & Dynamic (Arrays)
| Problem | LC | Difficulty |
|---|---|---|
| Maximum Sum Subarray of Size K | classic | Easy |
| Minimum Size Subarray Sum | [209](https://leetcode.com/problems/minimum-size-subarray-sum/) | Medium |
| Max Points from Cards | [1423](https://leetcode.com/problems/maximum-points-you-can-obtain-from-cards/) | Medium |
| Max Consecutive Ones II | [487](https://leetcode.com/problems/max-consecutive-ones-ii/) | Medium |

### Monotonic Deque
| Problem | LC | Difficulty |
|---|---|---|
| Sliding Window Maximum | [239](https://leetcode.com/problems/sliding-window-maximum/) | Hard |
| Shortest Subarray with Sum ≥ K | [862](https://leetcode.com/problems/shortest-subarray-with-sum-at-least-k/) | Hard |
| Longest Continuous Subarray with AbsDiff ≤ Limit | [1438](https://leetcode.com/problems/longest-continuous-subarray-with-absolute-diff-less-than-or-equal-to-limit/) | Medium |

### Two Heaps
| Problem | LC | Difficulty |
|---|---|---|
| Sliding Window Median | [480](https://leetcode.com/problems/sliding-window-median/) | Hard |
| Find Median from Data Stream | [295](https://leetcode.com/problems/find-median-from-data-stream/) | Hard |

### Expand & Shrink (Two Pointers)
| Problem | LC | Difficulty |
|---|---|---|
| Container With Most Water | [11](https://leetcode.com/problems/container-with-most-water/) | Medium |
| Two Sum Less Than K | [1099](https://leetcode.com/problems/two-sum-less-than-k/) | Easy |

---

## Fintech / Finance-Specific Patterns

Fintech interviews (Stripe, Robinhood, Bloomberg, Jane Street, Citadel) emphasize:
- Running balances and transaction windows
- Rate limiting / throttling (fixed window counter)
- Price/metric aggregations over time

### Running Balance — Max Profit Window
> "Find max profit in a sliding window of transactions" → same as max subarray sum in window

Use **Kadane's variant** for max subarray, or **fixed window** for fixed time horizon.

```java
// Max profit from stock in any window of size k
public int maxProfitWindow(int[] prices, int k) {
    int windowSum = 0, maxProfit = 0;
    for (int i = 0; i < prices.length; i++) {
        if (i > 0) windowSum += prices[i] - prices[i - 1];   // daily change
        if (i >= k) windowSum -= prices[i - k] - prices[i - k - 1];
        maxProfit = Math.max(maxProfit, windowSum);
    }
    return maxProfit;
}
```

### Rate Limiting — Fixed Window Counter
> "Allow at most k requests in any window of t seconds"

```java
// Sliding window log approach — O(n) per check
class RateLimiter {
    private final Deque<Long> log = new ArrayDeque<>();
    private final int limit;
    private final long windowMs;

    public RateLimiter(int limit, long windowMs) {
        this.limit = limit;
        this.windowMs = windowMs;
    }

    public boolean allow(long timestamp) {
        while (!log.isEmpty() && timestamp - log.peekFirst() >= windowMs) {
            log.pollFirst();                            // evict old requests
        }
        if (log.size() < limit) {
            log.addLast(timestamp);
            return true;
        }
        return false;
    }
}
```

### Moving Average — Fixed Window
> "Compute average of last k data points" (LC 346, common fintech screen)

```java
class MovingAverage {
    private final int[] window;
    private int sum = 0, count = 0, idx = 0;

    public MovingAverage(int size) { window = new int[size]; }

    public double next(int val) {
        sum -= window[idx];          // remove oldest
        window[idx] = val;
        sum += val;
        idx = (idx + 1) % window.length;
        count = Math.min(count + 1, window.length);
        return (double) sum / count;
    }
}
```

## Why Different DS?

| Structure                | When to use                                      | Example |
| ------------------------ | ------------------------------------------------ | ------- |
| **int[26] array**        | Lowercase-only char frequency (faster than map)  | LC 567, 438 |
| **int[128] array**       | Any ASCII char frequency                         | LC 76 |
| **HashMap**              | Arbitrary key frequencies (numbers, Unicode)     | LC 992, 560 |
| **Set**                  | Just presence, not count (unique elements)       | LC 3 |
| **Deque**                | Max/Min in window, monotonic order               | LC 239, 862 |
| **Prefix Sum + HashMap** | Sum = K, divisible by K, cumulative conditions   | LC 560, 523 |
| **Two Heaps**            | Median tracking in window                        | LC 480, 295 |
| **Circular array**       | Moving average, ring buffer                      | LC 346 |

---

## FAANG Study Guide — Level by Level

### Level 1 — Foundation (Do these first, no exceptions)

> Goal: build the core mental model. Every other problem is a variant of these.

| # | Problem | Pattern | Key learning |
|---|---|---|---|
| 1 | LC 3 — Longest Substring Without Repeating | Dynamic window + Set | Basic expand/shrink loop |
| 2 | LC 209 — Minimum Size Subarray Sum | Dynamic window | `while` shrink, `right - left + 1` |
| 3 | LC 567 — Permutation in String | Fixed window + int[26] | char array comparison, fixed slide |
| 4 | LC 11 — Container With Most Water | Two pointers | Move the smaller pointer |
| 5 | LC 560 — Subarray Sum Equals K | Prefix sum + HashMap | `prefixCount.put(0,1)` trick |

Checkpoint: Can you write each from scratch in < 10 min? Move on only when yes.

---

### Level 2 — Core FAANG (Tier 1 problems)

> Goal: handle the patterns that appear in 80% of FAANG sliding window rounds.

| # | Problem | Pattern | Key learning |
|---|---|---|---|
| 6 | LC 438 — Find All Anagrams | Fixed window + int[26] | Same as 567, collect all start indices |
| 7 | LC 424 — Longest Repeating Character Replacement | maxFreq + `if` shrink | Window only slides, never shrinks |
| 8 | LC 76 — Minimum Window Substring | need/have (formed/required) | Hardest string template — master this |
| 9 | LC 1423 — Max Points from Cards | Reverse fixed window | total - min middle window |
| 10 | LC 239 — Sliding Window Maximum | Monotonic deque | Decreasing deque, evict stale front |

Checkpoint: LC 76 is the boss of Level 2. If you can do it cold, you're ready for Level 3.

---

### Level 3 — High Frequency (Tier 2 problems)

> Goal: cover the patterns that differentiate strong from average candidates.

| # | Problem | Pattern | Key learning |
|---|---|---|---|
| 11 | LC 1004 — Max Consecutive Ones III | atMost(K) with `if` | Flip 0s → at most K zeros in window |
| 12 | LC 992 — Subarrays with K Different Integers | exactly(K) = atMost(K) - atMost(K-1) | The subtraction trick |
| 13 | LC 523 — Continuous Subarray Sum | Prefix sum + mod | `sum % k` in map, check `i - map[mod] >= 2` |
| 14 | LC 159 — Longest Substring with At Most 2 Distinct | Dynamic window + HashMap | Generalizes to K distinct |
| 15 | LC 904 — Fruit Into Baskets | atMost(2) distinct | Same as 159, different story |

---

### Level 4 — Senior / L5+ (Tier 3 problems)

> Goal: hard problems for senior rounds at Google / Amazon. Skip if targeting L3-L4.

| # | Problem | Pattern | Key learning |
|---|---|---|---|
| 16 | LC 295 — Find Median from Data Stream | Two heaps | Max heap (small) + min heap (large), balance sizes |
| 17 | LC 480 — Sliding Window Median | Two heaps + removal | Lazy deletion from heap |
| 18 | LC 862 — Shortest Subarray with Sum ≥ K | Prefix sum + monotonic deque | Handles negatives unlike dynamic window |

---

### Recommended Order (visual)

```
L1: LC 3 → LC 209 → LC 567 → LC 11 → LC 560
              ↓
L2: LC 438 → LC 424 → LC 76 → LC 1423 → LC 239
              ↓
L3: LC 1004 → LC 992 → LC 523 → LC 159 → LC 904
              ↓
L4: LC 295 → LC 480 → LC 862   (senior only)
```

### What each company focuses on

| Company | Focus | Must-do |
|---|---|---|
| **Google** | Hard + deque + two-heap | LC 239, LC 76, LC 862, LC 480 |
| **Meta** | String window + anagram | LC 76, LC 567, LC 438, LC 3 |
| **Amazon** | OA-style + prefix sum | LC 1423, LC 560, LC 209, LC 1004 |
| **Apple** | Medium difficulty, clean code | LC 3, LC 424, LC 567, LC 11 |
| **Fintech** (Stripe, Bloomberg) | Running sums, rate limiting, moving avg | LC 560, LC 523, Moving Average (LC 346) |
