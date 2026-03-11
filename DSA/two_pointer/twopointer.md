# Two Pointer Template & Decision Guide

## Core Idea

Two pointers eliminate the need for nested loops by moving two indices intelligently.
Instead of checking every pair O(n²), you move pointers based on a condition — O(n).

Three variants:
- **Opposite ends** — left starts at 0, right at end, they converge
- **Same direction (fast/slow)** — both start at 0, one moves faster
- **Sliding window** — both move right (covered in slidingwindow.md)

---

## 1. Opposite Ends — Sorted Array

> Use when: array is sorted and you need pairs/triplets summing to a target

```
left → → → → ← ← ← ← right
Move left if sum too small, move right if sum too big
```

```java
// LC 167 — Two Sum II (sorted input)
public int[] twoSum(int[] numbers, int target) {
    int left = 0, right = numbers.length - 1;

    while (left < right) {
        int sum = numbers[left] + numbers[right];

        if (sum == target)      return new int[]{left + 1, right + 1};
        else if (sum < target)  left++;   // need bigger sum
        else                    right--;  // need smaller sum
    }
    return new int[]{-1, -1};
}
```

**Key invariant:** array must be sorted. If unsorted, sort first.

---

## 2. Three Sum — Sorted + Two Pointers

> Use when: "find all triplets summing to target" (LC 15, 16, 18)

**Pattern:** Fix one element, run two-pointer on the rest. Skip duplicates explicitly.

```java
// LC 15 — 3Sum
public List<List<Integer>> threeSum(int[] nums) {
    Arrays.sort(nums);
    List<List<Integer>> result = new ArrayList<>();

    for (int i = 0; i < nums.length - 2; i++) {
        if (i > 0 && nums[i] == nums[i - 1]) continue;   // skip duplicate i

        int left = i + 1, right = nums.length - 1;
        while (left < right) {
            int sum = nums[i] + nums[left] + nums[right];

            if (sum == 0) {
                result.add(Arrays.asList(nums[i], nums[left], nums[right]));
                while (left < right && nums[left] == nums[left + 1]) left++;   // skip dup left
                while (left < right && nums[right] == nums[right - 1]) right--; // skip dup right
                left++; right--;
            } else if (sum < 0) left++;
            else right--;
        }
    }
    return result;
}
```

**Duplicate skipping rule:** skip `nums[i]` at outer loop, skip `nums[left]` and `nums[right]` after finding a valid triplet.

---

## 3. Fast / Slow Pointers — In-place Operations

> Use when: remove elements, find middle, detect cycles without extra space

**Pattern:** slow marks the "write position", fast scans ahead.

```java
// LC 26 — Remove Duplicates from Sorted Array
public int removeDuplicates(int[] nums) {
    int slow = 0;

    for (int fast = 1; fast < nums.length; fast++) {
        if (nums[fast] != nums[slow]) {
            nums[++slow] = nums[fast];   // write new unique value
        }
    }
    return slow + 1;   // length of deduplicated array
}
```

```java
// LC 27 — Remove Element
public int removeElement(int[] nums, int val) {
    int slow = 0;

    for (int fast = 0; fast < nums.length; fast++) {
        if (nums[fast] != val) {
            nums[slow++] = nums[fast];
        }
    }
    return slow;
}
```

**Mental model:** slow = last valid position, fast = current candidate.

---

## 4. Fast / Slow — Linked List Cycle Detection

> Use when: detect cycle, find middle, find k-th from end in linked list

```java
// LC 141 — Linked List Cycle
public boolean hasCycle(ListNode head) {
    ListNode slow = head, fast = head;

    while (fast != null && fast.next != null) {
        slow = slow.next;
        fast = fast.next.next;   // fast moves 2x
        if (slow == fast) return true;
    }
    return false;
}
```

```java
// LC 876 — Middle of Linked List
public ListNode middleNode(ListNode head) {
    ListNode slow = head, fast = head;

    while (fast != null && fast.next != null) {
        slow = slow.next;
        fast = fast.next.next;
    }
    return slow;   // slow is at middle when fast reaches end
}
```

**Why it works:** fast travels 2x speed. When fast reaches end, slow is at the midpoint.

---

## 5. Three Pointers — Dutch National Flag / Sort Colors

> Use when: partition array into 3 groups in one pass (LC 75)

```
[0...low-1] = 0s  |  [low...mid-1] = 1s  |  [high+1...n-1] = 2s
                        mid scans forward
```

```java
// LC 75 — Sort Colors
public void sortColors(int[] nums) {
    int low = 0, mid = 0, high = nums.length - 1;

    while (mid <= high) {
        if (nums[mid] == 0) {
            swap(nums, low++, mid++);   // 0 goes to front
        } else if (nums[mid] == 1) {
            mid++;                       // 1 stays in place
        } else {
            swap(nums, mid, high--);    // 2 goes to back (don't advance mid)
        }
    }
}

private void swap(int[] nums, int i, int j) {
    int tmp = nums[i]; nums[i] = nums[j]; nums[j] = tmp;
}
```

**Why not advance `mid` when swapping with `high`?** The element swapped from `high` is unknown — it needs to be inspected next.

---

## 6. Trapping Rain Water

> Classic hard — uses two-pointer with running max from both sides

```
maxLeft[i]  = max height to the left of i  (including i)
maxRight[i] = max height to the right of i (including i)
water[i]    = min(maxLeft, maxRight) - height[i]
```

```java
// LC 42 — Trapping Rain Water
public int trap(int[] height) {
    int left = 0, right = height.length - 1;
    int maxLeft = 0, maxRight = 0, water = 0;

    while (left < right) {
        if (height[left] <= height[right]) {
            maxLeft = Math.max(maxLeft, height[left]);
            water += maxLeft - height[left];   // water above left bar
            left++;
        } else {
            maxRight = Math.max(maxRight, height[right]);
            water += maxRight - height[right]; // water above right bar
            right--;
        }
    }
    return water;
}
```

**Key insight:** move the pointer with the smaller height — the water it can hold is bounded by its own side's max.

---

## 7. Palindrome — Two Pointer Verify

> Use when: check or find palindromes (LC 125, 680)

```java
// LC 125 — Valid Palindrome
public boolean isPalindrome(String s) {
    int left = 0, right = s.length() - 1;

    while (left < right) {
        while (left < right && !Character.isAlphanumeric(s.charAt(left)))  left++;
        while (left < right && !Character.isAlphanumeric(s.charAt(right))) right--;

        if (Character.toLowerCase(s.charAt(left)) != Character.toLowerCase(s.charAt(right)))
            return false;

        left++; right--;
    }
    return true;
}
```

```java
// LC 680 — Valid Palindrome II (allow one deletion)
public boolean validPalindrome(String s) {
    int left = 0, right = s.length() - 1;

    while (left < right) {
        if (s.charAt(left) != s.charAt(right)) {
            return isPalin(s, left + 1, right) || isPalin(s, left, right - 1);
        }
        left++; right--;
    }
    return true;
}

private boolean isPalin(String s, int l, int r) {
    while (l < r) {
        if (s.charAt(l++) != s.charAt(r--)) return false;
    }
    return true;
}
```

---

## Key Pitfalls

### Sorting requirement
Opposite-end two pointer **only works on sorted arrays**. Always check: is the input sorted? If not, `Arrays.sort()` first (O(n log n) — usually fine).

### Duplicate skipping in 3Sum
- Skip outer `i` at the **start** of the loop iteration: `if (i > 0 && nums[i] == nums[i-1]) continue`
- Skip inner `left/right` **after** recording the result, not before
- Missing either one causes duplicate triplets in output

### Fast/slow: null check order
Always check `fast != null && fast.next != null` — in that order. Reversing causes NullPointerException.

### Dutch flag: don't advance mid after swap with high
When `nums[mid] == 2`, you swap with `high` and decrement `high` but **do not** increment `mid` — the swapped value is uninspected.

---

## Decision Tree: Two Pointer Problems

```
Array / String problem, need O(n)?
│
├── Input is a LINKED LIST?
│   ├── Detect cycle → fast/slow (Floyd's)
│   ├── Find middle → fast/slow
│   └── Find k-th from end → gap of k between fast and slow
│
└── Input is an ARRAY / STRING?
    │
    ├── Sorted array, find pairs/triplets summing to target?
    │   ├── One pair → Opposite ends two pointer
    │   └── Triplets → Fix one + opposite ends (skip duplicates)
    │
    ├── Remove/filter elements in-place?
    │   └── Fast/slow — slow = write head, fast = read head
    │
    ├── Partition into groups (0s, 1s, 2s)?
    │   └── Three pointers — Dutch national flag
    │
    ├── Trapping water / max area?
    │   └── Opposite ends, move smaller pointer
    │
    └── Palindrome check / manipulation?
        └── Opposite ends, skip non-alnum, handle one deletion
```

---

## Problem Sets by Pattern

### Opposite Ends — Sorted Array
| Problem | LC | Difficulty | Company |
|---|---|---|---|
| Two Sum II | [167](https://leetcode.com/problems/two-sum-ii-input-array-is-sorted/) | Easy | All |
| 3Sum | [15](https://leetcode.com/problems/3sum/) | Medium | Facebook/Meta, Google |
| 3Sum Closest | [16](https://leetcode.com/problems/3sum-closest/) | Medium | Amazon |
| 4Sum | [18](https://leetcode.com/problems/4sum/) | Medium | Amazon |
| Container With Most Water | [11](https://leetcode.com/problems/container-with-most-water/) | Medium | All |
| Trapping Rain Water | [42](https://leetcode.com/problems/trapping-rain-water/) | Hard | Google, Amazon, Meta |

### Fast / Slow — Arrays
| Problem | LC | Difficulty | Company |
|---|---|---|---|
| Remove Duplicates from Sorted Array | [26](https://leetcode.com/problems/remove-duplicates-from-sorted-array/) | Easy | All |
| Remove Element | [27](https://leetcode.com/problems/remove-element/) | Easy | All |
| Move Zeroes | [283](https://leetcode.com/problems/move-zeroes/) | Easy | Facebook/Meta |
| Remove Duplicates II (allow ≤2) | [80](https://leetcode.com/problems/remove-duplicates-from-sorted-array-ii/) | Medium | Google |
| Sort Colors | [75](https://leetcode.com/problems/sort-colors/) | Medium | All |

### Fast / Slow — Linked List
| Problem | LC | Difficulty | Company |
|---|---|---|---|
| Linked List Cycle | [141](https://leetcode.com/problems/linked-list-cycle/) | Easy | All |
| Linked List Cycle II (find entry) | [142](https://leetcode.com/problems/linked-list-cycle-ii/) | Medium | Amazon, Google |
| Middle of Linked List | [876](https://leetcode.com/problems/middle-of-the-linked-list/) | Easy | All |
| Remove N-th Node from End | [19](https://leetcode.com/problems/remove-nth-node-from-end-of-list/) | Medium | All |

### Palindrome
| Problem | LC | Difficulty | Company |
|---|---|---|---|
| Valid Palindrome | [125](https://leetcode.com/problems/valid-palindrome/) | Easy | Meta, Apple |
| Valid Palindrome II | [680](https://leetcode.com/problems/valid-palindrome-ii/) | Easy | Meta favorite |
| Palindromic Substrings | [647](https://leetcode.com/problems/palindromic-substrings/) | Medium | Google |
| Longest Palindromic Substring | [5](https://leetcode.com/problems/longest-palindromic-substring/) | Medium | All |

---

## FAANG Study Guide — Level by Level

### Level 1 — Foundation

> Goal: understand the two-pointer movement. Every problem here is < 20 lines.

| # | Problem | Pattern | Key learning |
|---|---|---|---|
| 1 | LC 167 — Two Sum II | Opposite ends | Move based on sum vs target |
| 2 | LC 26 — Remove Duplicates | Fast/slow | Slow = write pointer |
| 3 | LC 141 — Linked List Cycle | Fast/slow | fast moves 2x, meet = cycle |
| 4 | LC 125 — Valid Palindrome | Opposite ends | Skip non-alnum, compare |
| 5 | LC 283 — Move Zeroes | Fast/slow | Swap zeros to back |

Checkpoint: Can you write each in < 8 min without hints?

---

### Level 2 — Core FAANG

> Goal: the problems that appear in 70% of two-pointer interview rounds.

| # | Problem | Pattern | Key learning |
|---|---|---|---|
| 6 | LC 15 — 3Sum | Fix + opposite ends | Duplicate skipping — the tricky part |
| 7 | LC 11 — Container With Most Water | Opposite ends | Move the smaller height pointer |
| 8 | LC 42 — Trapping Rain Water | Opposite ends + running max | Most asked hard at FAANG |
| 9 | LC 75 — Sort Colors | Three pointers | Dutch flag, don't advance mid on swap-high |
| 10 | LC 19 — Remove Nth from End | Fast/slow with gap k | Advance fast by k first |

Checkpoint: LC 42 and LC 15 are the bosses. Master these before moving on.

---

### Level 3 — High Frequency

> Goal: variants and follow-ups that appear in harder rounds.

| # | Problem | Pattern | Key learning |
|---|---|---|---|
| 11 | LC 16 — 3Sum Closest | Fix + opposite ends | Track minimum diff, not exact match |
| 12 | LC 80 — Remove Duplicates II | Fast/slow | Allow up to 2 copies: `nums[fast] != nums[slow-2]` |
| 13 | LC 142 — Linked List Cycle II | Fast/slow + math | After meeting, reset one to head — they meet at entry |
| 14 | LC 680 — Valid Palindrome II | Opposite ends | On mismatch try skip-left OR skip-right |
| 15 | LC 647 — Palindromic Substrings | Expand from center | Two-pointer outward, odd + even length |

---

### Level 4 — Senior / L5+

> Goal: hard problems that require combining two-pointer with other techniques.

| # | Problem | Pattern | Key learning |
|---|---|---|---|
| 16 | LC 18 — 4Sum | Fix two + opposite ends | Double fix loop + two pointer inside |
| 17 | LC 5 — Longest Palindromic Substring | Expand from center | Expand both odd (i,i) and even (i,i+1) centers |
| 18 | LC 881 — Boats to Save People | Greedy + opposite ends | Pair heaviest with lightest if possible |

---

### Recommended Order (visual)

```
L1: LC 167 → LC 26 → LC 141 → LC 125 → LC 283
              ↓
L2: LC 15 → LC 11 → LC 42 → LC 75 → LC 19
              ↓
L3: LC 16 → LC 80 → LC 142 → LC 680 → LC 647
              ↓
L4: LC 18 → LC 5 → LC 881   (senior only)
```

### What each company focuses on

| Company | Focus | Must-do |
|---|---|---|
| **Google** | Hard + tricky edge cases | LC 42, LC 15, LC 142, LC 647 |
| **Meta** | Palindrome + 3Sum variants | LC 15, LC 125, LC 680, LC 11 |
| **Amazon** | In-place array + linked list | LC 26, LC 141, LC 19, LC 75 |
| **Apple** | Clean code on medium problems | LC 167, LC 125, LC 15, LC 11 |
| **Fintech** | Sorted pair/triplet sums | LC 167, LC 15, LC 16, LC 11 |

---

## Why Two Pointer vs Other Approaches?

| Scenario | Brute Force | Two Pointer | Why TP wins |
|---|---|---|---|
| Pair sum in sorted array | O(n²) | O(n) | Sorted order lets you move directionally |
| Remove elements in-place | O(n) extra space | O(1) space | Write-head doesn't need extra array |
| Cycle in linked list | O(n) HashSet | O(1) space | Fast/slow meet without storing visited |
| Trap water | O(n) extra arrays | O(1) space | Running max from both sides is enough |
