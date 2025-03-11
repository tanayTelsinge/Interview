| **Category** | **Problem** | **Basic Approach** | **Time Complexity** | **Space Complexity** | **Optimum Approach** | **Time Complexity** | **Space Complexity** |
|-------------|------------|-------------------|----------------|----------------|-----------------|----------------|----------------|
| **Two Pointers** | Two Sum (Sorted) | Brute-force checking pairs | O(n²) | O(1) | Two-pointer approach | O(n) | O(1) |
|  | Move Zeroes | Copy non-zero elements to new array | O(n) | O(n) | In-place swap using two pointers | O(n) | O(1) |
|  | Sort Colors | Count 0s, 1s, and 2s, then overwrite | O(n) | O(1) | Dutch National Flag Algorithm | O(n) | O(1) |
|  | Container With Most Water | Check every pair | O(n²) | O(1) | Two-pointer technique | O(n) | O(1) |
| **Sliding Window** | Longest Substring Without Repeating Characters | Brute-force, check all substrings | O(n²) | O(n) | Sliding window with HashSet | O(n) | O(n) |
|  | Maximum Sum Subarray of Size K | Nested loops checking subarrays | O(nK) | O(1) | Sliding window sum update | O(n) | O(1) |
|  | Longest Subarray with Sum ≤ K | Check all subarrays | O(n²) | O(1) | Sliding window sum update | O(n) | O(1) |
|  | Permutation in String | Generate all permutations | O(n*m!) | O(m) | Sliding window frequency count | O(n) | O(1) |
| **Kadane’s Algorithm & Prefix Sum** | Maximum Subarray | Check all subarrays | O(n²) | O(1) | Kadane’s algorithm | O(n) | O(1) |
|  | Subarray Sum Equals K | Nested loops with sum comparison | O(n²) | O(1) | Prefix sum with hash map | O(n) | O(n) |
|  | Product of Array Except Self | Compute product for each index | O(n²) | O(1) | Two-pass prefix & suffix product | O(n) | O(1) |
| **Sorting-Based Problems** | Merge Intervals | Compare every pair | O(n²) | O(n) | Sort and merge | O(n log n) | O(n) |
|  | Meeting Rooms II | Check overlaps for all intervals | O(n²) | O(n) | Sort start & end times | O(n log n) | O(n) |
|  | Kth Largest Element | Sort the array | O(n log n) | O(1) | QuickSelect or Min-Heap | O(n) (avg) / O(n log k) | O(k) |
|  | Top K Frequent Elements | Count and sort elements | O(n log n) | O(n) | Bucket sort or Min-Heap | O(n) | O(n) |
| **Binary Search on Arrays** | Search in Rotated Sorted Array | Linear search | O(n) | O(1) | Modified binary search | O(log n) | O(1) |
|  | Find Minimum in Rotated Sorted Array | Linear scan | O(n) | O(1) | Binary search to find rotation | O(log n) | O(1) |
|  | Median of Two Sorted Arrays | Merge and find median | O(m+n) | O(m+n) | Binary search on partitions | O(log(min(m, n))) | O(1) |
| **Greedy & Heap** | Kth Largest Element (Heap) | Sort and pick kth largest | O(n log n) | O(1) | Min-Heap of size k | O(n log k) | O(k) |
|  | Task Scheduler | Simulate scheduling | O(n log n) | O(n) | Math-based calculation of idle times | O(n) | O(n) |
|  | Hand of Straights | Sort and check groups | O(n log n) | O(n) | Frequency dictionary approach | O(n log n) | O(n) |
| **Matrix-Based Problems** | Spiral Matrix | Use extra matrix for tracking | O(m*n) | O(m*n) | Use boundary pointers | O(m*n) | O(1) |
|  | Rotate Image | Create new matrix and rotate | O(n²) | O(n²) | Transpose and reverse rows in-place | O(n²) | O(1) |
|  | Word Search | DFS brute-force | O(m*n * 4^l) | O(m*n) | DFS with backtracking and pruning | O(m*n) | O(m*n) |
| **Stack-Based Problems** | Next Greater Element | Check every element pair | O(n²) | O(1) | Monotonic stack | O(n) | O(n) |
|  | Largest Rectangle in Histogram | Extend left & right for every bar | O(n²) | O(1) | Monotonic stack to track heights | O(n) | O(n) |
|  | Daily Temperatures | Check next warmer day for each day | O(n²) | O(1) | Monotonic stack | O(n) | O(n) |
| **Bit Manipulation** | Single Number | HashMap to count occurrences | O(n) | O(n) | XOR to cancel out duplicates | O(n) | O(1) |
|  | Missing Number | Compute sum and subtract | O(n) | O(1) | XOR on indices and array elements | O(n) | O(1) |
|  | Reverse Bits | Loop through bits and construct | O(b) (b=32) | O(1) | Bitwise manipulation or lookup table | O(1) | O(1) |
