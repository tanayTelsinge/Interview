Sliding window:

Fixed window template

```java
  //template
  public static int maxSumSubArray(int[] arr, int k) {
        int maxSum = Integer.MIN_VALUE, windowSum = 0;

        for (int i = 0; i < arr.length; i++) {
            windowSum += arr[i]; // Expand window

            if (i >= k - 1) { // When window size reaches k
                maxSum = Math.max(maxSum, windowSum);
                windowSum -= arr[i - (k - 1)]; // Slide window
            }
        }
        return maxSum;
    }
```

Dynamic window template

```java
  public static int minSubArrayLen(int k, int[] arr) {
        int left = 0, sum = 0, minLen = Integer.MAX_VALUE;

        for (int right = 0; right < arr.length; right++) {
            sum += arr[right];

            while (sum >= k) { // Shrink the window
                minLen = Math.min(minLen, right - left + 1);
                sum -= arr[left++];
            }
        }
        return (minLen == Integer.MAX_VALUE) ? 0 : minLen;
    }
```

# 📌 **Sliding Window vs. Prefix Sum vs. Other Approaches Decision Table**
This guide will help you classify **subarray problems** in **FAANG-style interviews** within **2 minutes**.  

---

## ✅ **Step 1: Is the problem about contiguous subarrays?**
| Question                      | Answer | Next Step |
|--------------------------------|--------|-----------|
| Are we looking for a subarray (contiguous elements)? | ❌ No  | Use **Dynamic Programming (DP) for subsequences** |
|                                | ✅ Yes  | Go to Step 2 |

---

## ✅ **Step 2: Is the window size fixed or variable?**
| Condition                          | Answer | Approach | Example FAANG Problem |
|------------------------------------|--------|----------|----------------------|
| Is the subarray size `k` **given**? | ✅ Yes  | **Fixed-size Sliding Window** | [Maximum Sum Subarray of Size K (Leetcode 643)](https://leetcode.com/problems/maximum-average-subarray-i/) |
| Does the window size **depend on a condition** (sum ≤, ≥, exact sum, at most/least `goal`)? | ✅ Yes  | **Variable-size Sliding Window (Expand & Shrink)** | [Minimum Size Subarray Sum (Leetcode 209)](https://leetcode.com/problems/minimum-size-subarray-sum/) |
| Are we counting subarrays instead of finding one? | ✅ Yes  | **Prefix Sum + HashMap** | [Subarray Sum Equals K (Leetcode 560)](https://leetcode.com/problems/subarray-sum-equals-k/) |

---

## ✅ **Step 3: Are negative numbers present?**
| Condition                          | Answer | Approach | Example FAANG Problem |
|------------------------------------|--------|----------|----------------------|
| Are all numbers **non-negative**?  | ✅ Yes  | **Use Sliding Window** | [Longest Subarray of 1s After Deleting One Element (Leetcode 1493)](https://leetcode.com/problems/longest-subarray-of-1s-after-deleting-one-element/) |
| Do numbers include **negatives**?  | ✅ Yes  | **Use Prefix Sum + HashMap** | [Subarray Sum Equals K (Leetcode 560)](https://leetcode.com/problems/subarray-sum-equals-k/) |
| Does the problem involve **maximum subarray sum**? | ✅ Yes | **Kadane’s Algorithm** | [Maximum Subarray (Leetcode 53)](https://leetcode.com/problems/maximum-subarray/) |

---

## 🔹 **Final Decision Table Based on FAANG Problems**
| **Problem Type** | **Sliding Window Type?** | **Approach** | **Example FAANG Problem** |
|------------------|------------------|------------|----------------------|
| Find max/min sum of **subarray of size `k`** | **Fixed-size** | **Basic sliding window** | [Max Sum of Subarray of Size K (Leetcode 643)](https://leetcode.com/problems/maximum-average-subarray-i/) |
| Find **smallest/largest subarray** with sum ≥, ≤ `goal` | **Variable-size** | **Expand & shrink sliding window** | [Minimum Size Subarray Sum (Leetcode 209)](https://leetcode.com/problems/minimum-size-subarray-sum/) |
| Find **number of subarrays** with sum = `goal` | ❌ No | **Prefix Sum + HashMap** | [Subarray Sum Equals K (Leetcode 560)](https://leetcode.com/problems/subarray-sum-equals-k/) |
| Find **longest subarray with sum ≤ goal** (Negative numbers exist) | ❌ No | **Prefix Sum + Sliding Window (TreeMap)** | [Longest Well-Performing Interval (Leetcode 1124)](https://leetcode.com/problems/longest-well-performing-interval/) |
| Find **maximum sum of any subarray** | ❌ No | **Kadane’s Algorithm** | [Maximum Subarray (Leetcode 53)](https://leetcode.com/problems/maximum-subarray/) |
| Find **longest substring with at most `k` distinct characters** | ✅ Yes | **Sliding Window + HashMap** | [Longest Substring with At Most K Distinct Characters (Leetcode 340)](https://leetcode.com/problems/longest-substring-with-at-most-k-distinct-characters/) |
| Find **length of longest substring without repeating characters** | ✅ Yes | **Sliding Window + HashSet** | [Longest Substring Without Repeating Characters (Leetcode 3)](https://leetcode.com/problems/longest-substring-without-repeating-characters/) |

---

## 🔥 **Google-Specific Interview Problems**
| **Problem** | **Approach** | **Leetcode Link** |
|------------|------------|----------------------|
| Find subarray with given XOR | **Prefix Sum + HashMap** | [Leetcode 1442](https://leetcode.com/problems/count-triplets-that-can-form-two-arrays-of-equal-xor/) |
| Find max sum of subarray with at most `k` distinct numbers | **Sliding Window + HashMap** | [Leetcode 904](https://leetcode.com/problems/fruit-into-baskets/) |
| Longest substring containing all vowels in order | **Sliding Window + HashMap** | [Leetcode 1839](https://leetcode.com/problems/longest-substring-of-all-vowels-in-order/) |
| Number of subarrays with bounded maximum | **Sliding Window** | [Leetcode 795](https://leetcode.com/problems/number-of-subarrays-with-bounded-maximum/) |
| Find the shortest subarray with sum at least `k` | **Deque (Monotonic Queue) + Prefix Sum** | [Leetcode 862](https://leetcode.com/problems/shortest-subarray-with-sum-at-least-k/) |

---


# 🔥 FAANG Sliding Window Problems - Comprehensive Summary Table

| **Problem** | **Type** | **Expand Condition** | **Shrink Condition** | **Result Update** | **TC / SC** | **Data Structure Used** | **Why This Data Structure?** |
|------------|---------|----------------|----------------|--------------|------------|-------------------|----------------------------|
| **Maximum Sum Subarray of Size K** | Fixed | `sum += arr[right]` | `right >= k-1 → sum -= arr[left++]` | Track max sum | **O(N) / O(1)** | **Array** | Simple sum update avoids recomputation |
| **Maximum of All Subarrays of Size K** | Fixed | Insert in `Deque` | Remove elements out of window | Track max in each window | **O(N) / O(K)** | **Deque (LinkedList)** | Keeps max elements efficiently |
| **First Negative Number in Every Window of Size K** | Fixed | Add negative numbers to `Queue` | `right >= k-1 → remove expired elements` | Track first negative number | **O(N) / O(K)** | **Queue (LinkedList)** | Stores first negative efficiently |
| **Count Occurrences of Anagram (Permutation in a String)** | Fixed | Maintain frequency count | `right >= k-1 → remove left char` | Count valid windows | **O(N) / O(1)** | **HashMap** | Tracks character frequency |
| **Longest Subarray with Sum ≤ K** | Variable | `sum += arr[right]` | `sum > k → sum -= arr[left++]` | Track max window length | **O(N) / O(1)** | **Array** | Prefix sum avoids recomputation |
| **Smallest Subarray with Sum ≥ K** | Variable | `sum += arr[right]` | `sum >= k → sum -= arr[left++]` | Track min window length | **O(N) / O(1)** | **Array** | Efficient shrinking optimizes |
| **Longest Substring Without Repeating Characters** | Variable | `set.add(s[right])` | `set.contains(s[right]) → remove left` | Track max substring length | **O(N) / O(K)** | **HashSet** | Ensures uniqueness efficiently |
| **Longest Subarray with Sum = K** | Variable | `sum += arr[right]` | Check `sum - k` in HashMap | Track max subarray length | **O(N) / O(N)** | **HashMap** | Stores prefix sums for O(1) lookup |
| **Longest Substring with K Distinct Characters** | Variable | Add char to `Map` | `map.size() > k → remove leftmost` | Track max substring length | **O(N) / O(K)** | **HashMap** | Tracks character frequency |
| **Longest Repeating Character Replacement** | Variable | Increase frequency count | `(window size - maxFreq) > k → shrink` | Track max window length | **O(N) / O(1)** | **Array (`int[26]`)** | O(1) updates for char frequency |
| **Binary Subarray with Sum K** | Variable | `sum += arr[right]` | `sum > k → sum -= arr[left++]` | Count valid subarrays | **O(N) / O(N)** | **HashMap** | Tracks prefix sums efficiently |
| **Fruit Into Baskets (Longest Subarray with 2 Distinct Elements)** | Variable | Add fruit types to `Map` | `map.size() > 2 → remove leftmost` | Track max window length | **O(N) / O(2)** | **HashMap** | Tracks fruit types efficiently |
| **Find All Anagrams in a String** | Fixed | Maintain frequency count | `right >= k-1 → remove left char` | Count valid windows | **O(N) / O(1)** | **HashMap** | Tracks character frequency |
| **Sliding Window Median** | Fixed | Insert into `Multiset` | Remove expired elements | Track median | **O(N log K) / O(K)** | **Balanced BST (TreeMap / Heap)** | Ensures efficient median updates |
| **Subarrays with Product Less Than K** | Variable | Multiply product | `product >= k → divide by arr[left++]` | Count valid subarrays | **O(N) / O(1)** | **Count Variable** | Avoids recomputation |
| **Minimum Window Substring** | Variable | Expand to include all chars | Shrink when all chars are included | Track min substring | **O(N) / O(1)** | **HashMap** | Tracks required chars efficiently |
| **Max Consecutive Ones III** | Variable | Expand by adding 1s and 0s | `count(0) > k → move left` | Track max window length | **O(N) / O(1)** | **Count Variable** | Keeps track of `0s` efficiently |
| **Number of Substrings with Exactly K Distinct Characters** | Variable | Expand & maintain count | Use `(atMost(K) - atMost(K-1))` trick | Count valid substrings | **O(N) / O(K)** | **Two-Pointer + HashMap** | Optimized counting approach |

---

## 🔥 Why Use Different Data Structures?

| **Data Structure** | **Why?** |
|----------------|-------------------------------------------|
| **Queue (LinkedList)** | Keeps order in problems like **First Negative Number** |
| **Deque (LinkedList)** | Efficiently finds max in **Maximum in all Subarrays of K** |
| **HashMap** | Tracks character counts in **Anagrams, Distinct Subarrays** |
| **Set (HashSet)** | Ensures uniqueness in **Longest Substring Without Repeating** |
| **Prefix Sum + HashMap** | Finds sum-based conditions efficiently (e.g., **Subarray Sum = K**) |
| **Heap (Priority Queue)** | Used in **Sliding Window Median** for quick median retrieval |
| **Two-Pointer Trick** | Optimizes **counting problems** (e.g., **Substrings with K Distinct**) |

---

1️⃣ When to Use a Queue (FIFO - First In, First Out)?
✅ Use a Queue when:
You need to process elements in the order they appear (FIFO).

You don't need to remove elements from both ends—only from the front.
You are storing elements for sequential processing (e.g., storing negative numbers, indexes, etc.).
🔹 Examples of Problems Using a Queue
Problem	Why Use a Queue?
First Negative Number in Every Window of Size K	Stores negative numbers in order, removes expired ones from the front.
Count Distinct Elements in Every Window of Size K	Stores numbers to track count in a rolling window.
