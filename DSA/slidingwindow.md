# Sliding Window Template & Decision Guide

## Fixed Window (Size K)

```java
public static int maxSumSubArray(int[] arr, int k) {
    int maxSum = Integer.MIN_VALUE, windowSum = 0;
    for (int i = 0; i < arr.length; i++) {
        windowSum += arr[i]; // Add current element to window
        if (i >= k - 1) { // Once window size reaches k
            maxSum = Math.max(maxSum, windowSum); // Update max sum
            windowSum -= arr[i - (k - 1)]; // Remove the leftmost element
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
        sum += arr[right]; // Expand window by adding right element
        while (sum >= k) { // Contract window when condition met
            minLen = Math.min(minLen, right - left + 1); // Update min length
            sum -= arr[left++]; // Shrink window from left
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
        freq.put(arr[right], freq.getOrDefault(arr[right], 0) + 1); // Add to frequency map
        while (freq.size() > k) { // Maintain at most k distinct
            freq.put(arr[left], freq.get(arr[left]) - 1);
            if (freq.get(arr[left]) == 0) freq.remove(arr[left]);
            left++; // Shrink window
        }
        count += right - left + 1; // Count valid subarrays
    }
    return count;
}
```

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
## Decision Tree: Subarray Problems

- **Contiguous Required?** → ❌ No → **DP (Subsequences)**
- ✅ Yes → Move to Next
- **Fixed or Variable Size?**
  - 🔹 Fixed (`k` given)? → **Fixed Sliding Window**
  - 🔹 Variable (condition-based)? → Move to Next
- **Condition Type?**
  - 🔸 Sum ≥, ≤, exact? → **Expand & Shrink (Two-Pointer)**
  - 🔸 Count subarrays with condition? → Move to Next
- **AtMost(K) Trick Applicable? like asked in distinct k or atmost k odd **
  - ✅ Yes → `atMost(K) - atMost(K-1)`
  - ❌ No → **Prefix Sum + HashMap**

## AtMost(K) Applicable Problems

| Problem Type                       | Leetcode Link                                                                              |
| ---------------------------------- | ------------------------------------------------------------------------------------------ |
| Subarrays with at most K distinct  | [340](https://leetcode.com/problems/longest-substring-with-at-most-k-distinct-characters/) |
| Subarrays with exactly K distinct  | [992](https://leetcode.com/problems/subarrays-with-k-different-integers/)                  |
| Max Consecutive Ones with ≤K flips | [1004](https://leetcode.com/problems/max-consecutive-ones-iii/)                            |
| Subarrays with Product < K         | [713](https://leetcode.com/problems/subarray-product-less-than-k/)                         |

## Prefix HashMap Applicable Problems

| Problem Type                       | Leetcode Link                                                            |
| ---------------------------------- | ------------------------------------------------------------------------ |
| Subarray Sum Equals K              | [560](https://leetcode.com/problems/subarray-sum-equals-k/)              |
| Continuous Subarray Sum            | [523](https://leetcode.com/problems/continuous-subarray-sum/)            |
| Count Number of Nice Subarrays     | [1248](https://leetcode.com/problems/count-number-of-nice-subarrays/)    |
| Maximum Size Subarray Sum Equals K | [325](https://leetcode.com/problems/maximum-size-subarray-sum-equals-k/) |

## Fixed & Dynamic Sliding Window (Without Prefix & HashMap)

| Problem Type                           | Leetcode Link                                                                         |
| -------------------------------------- | ------------------------------------------------------------------------------------- |
| Longest Substring Without Repeating    | [3](https://leetcode.com/problems/longest-substring-without-repeating-characters/)   |
| Longest Substring with At Most 2 Distinct | [159](https://leetcode.com/problems/longest-substring-with-at-most-two-distinct-characters/) |
| Minimum Window Substring               | [76](https://leetcode.com/problems/minimum-window-substring/)                         |
| Longest Repeating Character Replacement | [424](https://leetcode.com/problems/longest-repeating-character-replacement/)         |

### Monotonic Queue (Deque)
| Problem Type                        | Leetcode Link                                                                        |
| ----------------------------------- | ----------------------------------------------------------------------------------- |
| Sliding Window Maximum              | [239](https://leetcode.com/problems/sliding-window-maximum/)                       |
| Shortest Subarray with Sum at Least K | [862](https://leetcode.com/problems/shortest-subarray-with-sum-at-least-k/)       |

### Heap (Priority Queue)
| Problem Type                        | Leetcode Link                                                                      |
| ----------------------------------- | --------------------------------------------------------------------------------- |
| Sliding Window Median               | [480](https://leetcode.com/problems/sliding-window-median/)                      |
| Find Median from Data Stream        | [295](https://leetcode.com/problems/find-median-from-data-stream/)                |

### Expand & Shrink (Two Pointers)
| Problem Type                        | Leetcode Link                                                                      |
| ----------------------------------- | --------------------------------------------------------------------------------- |
| Container With Most Water           | [11](https://leetcode.com/problems/container-with-most-water/)                    |
| Two Sum Less Than K                 | [1099](https://leetcode.com/problems/two-sum-less-than-k/)                        |

## Why Different DS?

| Structure                | Usage                                   |
| ------------------------ | --------------------------------------- |
| **HashMap**              | Count frequencies (Anagrams, Distinct)  |
| **Deque**                | Max/Min in Sliding Window (Max Sliding) |
| **Prefix Sum + HashMap** | Subarray sum-related (Sum = K)          |
| **Set**                  | Unique elements (Longest No Repeat)     |
| **Heap**                 | Median in Window (Sliding Median)       |


