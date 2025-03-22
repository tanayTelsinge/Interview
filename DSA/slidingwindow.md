# Sliding Window Template & Decision Guide

## Fixed Window (Size K)

```java
public static int maxSumSubArray(int[] arr, int k) {
    int maxSum = Integer.MIN_VALUE, windowSum = 0;
    for (int i = 0; i < arr.length; i++) {
        windowSum += arr[i];
        if (i >= k - 1) {
            maxSum = Math.max(maxSum, windowSum);
            windowSum -= arr[i - (k - 1)];
        }
    }
    return maxSum;
}
```

## Dynamic Window (Variable Size)

```java
public static int minSubArrayLen(int k, int[] arr) {
    int left = 0, sum = 0, minLen = Integer.MAX_VALUE;
    for (int right = 0; right < arr.length; right++) {
        sum += arr[right];
        while (sum >= k) {
            minLen = Math.min(minLen, right - left + 1);
            sum -= arr[left++];
        }
    }
    return (minLen == Integer.MAX_VALUE) ? 0 : minLen;
}
```

## AtMost(K) Template

```java
public static int atMostK(int[] arr, int k) {
    int left = 0, count = 0;
    Map<Integer, Integer> freq = new HashMap<>();
    for (int right = 0; right < arr.length; right++) {
        freq.put(arr[right], freq.getOrDefault(arr[right], 0) + 1);
        while (freq.size() > k) {
            freq.put(arr[left], freq.get(arr[left]) - 1);
            if (freq.get(arr[left]) == 0) freq.remove(arr[left]);
            left++;
        }
        count += right - left + 1;
    }
    return count;
}
```

## Decision Tree: Subarray Problems

- **Contiguous Required?**

  - ❌ No → **DP (Subsequences)**
  - ✅ Yes → Move to Next

- **Fixed or Variable Size?**

  - 🔹 Fixed (`k` given)? → **Fixed Sliding Window**
  - 🔹 Variable (condition-based)? → Move to Next

- **Condition Type?**

  - 🔸 Sum ≥, ≤, exact? → **Expand & Shrink (Two-Pointer)**
  - 🔸 Count subarrays with condition? → Move to Next

- **AtMost(K) Trick Applicable?**

  - ✅ Yes → `atMost(K) - atMost(K-1)`
  - ❌ No → **Prefix Sum + HashMap**

## Common Patterns & Examples

| Approach                  | Problem Type                         | Leetcode Link                                                                              |
| ------------------------- | ------------------------------------ | ------------------------------------------------------------------------------------------ |
| Fixed Sliding Window      | Max/Min Sum of Size K                | [643](https://leetcode.com/problems/maximum-average-subarray-i/)                           |
| Expand & Shrink           | Smallest/Largest Subarray (Sum Cond) | [209](https://leetcode.com/problems/minimum-size-subarray-sum/)                            |
| Prefix Sum + HashMap      | Count Subarrays with Sum K           | [560](https://leetcode.com/problems/subarray-sum-equals-k/)                                |
| Sliding Window + HashMap  | Longest Subarray ≤ K Distinct        | [340](https://leetcode.com/problems/longest-substring-with-at-most-k-distinct-characters/) |
| `atMost(K) - atMost(K-1)` | Exactly K Distinct Elements          | [992](https://leetcode.com/problems/subarrays-with-k-different-integers/)                  |
| Expand & Shrink           | Max Consecutive Ones (≤K Zeros)      | [1004](https://leetcode.com/problems/max-consecutive-ones-iii/)                            |
| Two Pointers              | Subarrays with Product < K           | [713](https://leetcode.com/problems/subarray-product-less-than-k/)                         |
| Sliding Window            | Minimum Window Substring             | [76](https://leetcode.com/problems/minimum-window-substring/)                              |

## AtMost(K) Applicable Problems

| Problem Type                       | Leetcode Link                                                                              |
| ---------------------------------- | ------------------------------------------------------------------------------------------ |
| Subarrays with at most K distinct  | [340](https://leetcode.com/problems/longest-substring-with-at-most-k-distinct-characters/) |
| Subarrays with exactly K distinct  | [992](https://leetcode.com/problems/subarrays-with-k-different-integers/)                  |
| Max Consecutive Ones with ≤K flips | [1004](https://leetcode.com/problems/max-consecutive-ones-iii/)                            |
| Subarrays with Product < K         | [713](https://leetcode.com/problems/subarray-product-less-than-k/)                         |

## Why Different DS?

| Structure                | Usage                                   |
| ------------------------ | --------------------------------------- |
| **HashMap**              | Count frequencies (Anagrams, Distinct)  |
| **Deque**                | Max/Min in Sliding Window (Max Sliding) |
| **Prefix Sum + HashMap** | Subarray sum-related (Sum = K)          |
| **Set**                  | Unique elements (Longest No Repeat)     |
| **Heap**                 | Median in Window (Sliding Median)       |

This version is **shorter**, **structured**, and keeps key insights. Let me know if you need more tweaks!

