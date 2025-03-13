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

🔥 **This is the ultimate sliding window problem list for FAANG interviews!** 🚀  
Would you like **detailed code solutions** for any specific problem?
