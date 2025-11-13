# Prefix & Difference Tricks Cheatsheet (Interview Priority + Companies)

---

## What & Why Prefix Sum?

Prefix Sum is a technique where we precompute the cumulative sum (or another operation like XOR, min, max) of an array.

- Why?  
  Instead of recalculating sums for every query (O(n)), prefix sums allow us to answer in O(1) after O(n) preprocessing.

- What does it solve?  
  Problems that ask about:
  - Subarray sums
  - Range queries
  - Counting frequencies
  - Interval overlaps
  - Dynamic updates (extended to difference arrays, BIT, Segment Tree)

- Example:  
  arr = [2, 4, 5]  
  prefix = [0, 2, 6, 11]  
  sum(1,2) = prefix[3] - prefix[1] = 9 → (4+5)

---

## Tier 1: Must Know

### 1. Prefix Sum
- Use: Fast range sum queries.
- Formula:  
  `prefix[i] = prefix[i-1] + arr[i]`  
   
- Example: arr=[2,4,5], prefix=[0,2,6,11]  
  sum(1,2)=9 → (4+5)
- Problems: 303, 560, 724
- 560 (LeetCode) - Subarray Sum Equals K
```
###### prefix sum version of Two Sum

- Two sum
- for each num x, we check x - k exists in map, if yes return result (indexes). 
- for each prefix sum x, check if x - k exists in map, if yes return result (but result can be multiple so store frequencies, because subarrays).

nums = [1 1 1] k = 2

map [0 : 1] 

num = 1
ps - k = 1 - 2 = -1 not exists default 0
map [0:1, 1:1]

num = 1
ps - k = 2 - 2 = 0 exists 
count += 1 => 1
map [0:1, 1:1, 2:1]

num = 1
ps - k = 3 - 2  = 1 exists
count += 1 => 2
map [0:1, 1:2, 2:1]

return count => 2
#### Intuition:
- From the current prefix sum, removing k => whether an earlier prefix sum existed that would leave behind a valid subarray
```
- Homework : Subarray sum divisible by k , Hashmap + prefix sum + mod handling (negative mod)

- Companies: Amazon, Google, Microsoft  
- Level: L3–L5

---

### 2.Range Difference Array (Imos Method)
- Why Use: Apply range updates efficiently.
- eg. You want to add +X to all elements in range [L,R] multiple times.
- Naive O(n) per update → O(n*m) for multiple updates.
```
for (int i = l; i <= r; i++) {
    arr[i] += val;
}
```
- Difference array allows O(1) per update → O(n+m) total.
- Formula:  
  `diff[L]+=X`  
  `diff[R+1]-=X`  
  `final = prefix(diff)`

```java
        int[] arr = {1, 2, 3, 4, 5}; // original array
        int n = arr.length;
        int[] diff = new int[n];

        // Step 1: build the difference array
        diff[0] = arr[0];
        for (int i = 1; i < n; i++) {
            diff[i] = arr[i] - arr[i - 1];
        }
        // arr = [1, 1, 1, 1, 1]

        // Step 2: perform multiple range updates
        // Add 10 to range [1, 3]
        rangeAdd(diff, 1, 3, 10);
        //arr = [1, 11, 11, 11, 1]

        // Add 5 to range [2, 4]
        rangeAdd(diff, 2, 4, 5);
        // arr = [1, 11, 16, 16, 6]

        // Step 3: rebuild the final array
        arr[0] = diff[0];
        for (int i = 1; i < n; i++) {
            arr[i] = arr[i - 1] + diff[i];
        }
        // arr = [1, 12, 17, 21, 6]

        // Print the final array
        for (int num : arr) {
            System.out.print(num + " ");
        }

         // Function to perform O(1) range update on the difference array
        static void rangeAdd(int[] diff, int l, int r, int val) {
            diff[l] += val; // start adding from l
            if (r + 1 < diff.length) {
                diff[r + 1] -= val; // stop adding after r
                // if we don't do this, diff spreads to the right indefinitely
            }
        }


```
- Problems: 370, 1109, 1094
- Companies: Amazon, Microsoft, Bloomberg  
- Level: L3–L4

---

### 3. Prefix XOR
- Use: Subarray XOR queries.
- Formula:  
  `prefix[i] = prefix[i-1] ^ arr[i]`  
  `xor(L,R) = prefix[R] ^ prefix[L-1]`
- Example: arr=[3,5,2], prefix=[0,3,6,4]  
  xor(1,2)=6 → (3^5)
- Problems: 1310, 1442
- Companies: Google, Facebook  
- Level: L3–L5

---

### 4. Prefix Min/Max
- Why Imp? - Helps answer range queries quickly without sorting or looping each time.
```
Prefix Max: at each index i, store the maximum value from start to i.

prefixMax[0] = arr[0];
for (int i = 1; i < n; i++) {
    prefixMax[i] = Math.max(prefixMax[i-1], arr[i]);
}

Prefix Min: same idea, but store the minimum.
Suffix similarly.

```
- Use: Maximum Difference with Constraints, Trapping water, product except self.
- Formula:  
  `left[i] = max(left[i-1], arr[i])`  
  `right[i] = max(right[i+1], arr[i])`
- Example: arr=[2,1,3]  
  left=[2,2,3], right=[3,3,3]  
  trapped at idx=1 → min(2,3)-1=1
- Often combined with sliding window or stack for harder problems.

```
Problem tricks:
- Product except self: use prefix and suffix products.
- Trapping rain water: use prefix max and suffix max to find water above each bar.
- Maximum Difference with Constraints 
- Best time to buy and sell stock: use prefix min to track lowest price up to each day.
- Best time to buy and sell stock with cooldown: use prefix max to track best profit up to each day.
- Best time to buy and sell stock II: use prefix max to accumulate profits from every upward slope.
- Best time to buy and sell stock III: use two prefix max arrays to track profits from two transactions.
- Best time to buy and sell stock IV: DP + prefix max to optimize multiple transactions.
- Longest mountain in array: use prefix max and suffix max to find peaks.
- Sliding window maximum: use deque + prefix max to maintain max in current window.
- Maximum product subarray: use prefix and suffix products to handle negatives.
- Maximum sum circular subarray: use prefix sums to find max subarray that wraps around.
```
- Problems: 42, 238
- Companies: Amazon, Google, Microsoft  
- Level: L3–L5

---

### 5. Prefix Modulo
- Use: Find subarrays divisible by k.
- Formula:  
  `prefix[i] = (prefix[i-1]+arr[i]) % k`  
  If remainder repeats → divisible subarray
- Example: arr=[2,3,1], k=3  
  prefix mods=[0,2,2,0] → remainder repeats → valid subarray
- Problems: 974, 523
- Companies: Google, Facebook, Amazon  
- Level: L3–L4

---

### 6. Sweep Line (Event Marking)
- Use: Overlapping intervals, max concurrency.
- Formula:  
  `events[L]+=1`  
  `events[R]-=1`  
  `active = prefix(events)`
- Example: Meetings=[(1,3),(2,4)]  
  active=[0,1,2,1,0] → max=2 rooms
- Problems: 253, 1094, 732
- Companies: Google, Microsoft, Facebook, Amazon  
- Level: L3–L5

---

## Tier 2: Strong Bonus

### 7. 2D Prefix Sum
- Use: Submatrix sums.
- Formula:  
  `pref[i][j] = grid[i][j] + pref[i-1][j] + pref[i][j-1] - pref[i-1][j-1]`
- Example: grid=[[1,2],[3,4]], pref=[[1,3],[4,10]]  
  sum((1,1),(2,2))=10
- Problem: 304
- Companies: Amazon, Microsoft  
- Level: L3–L4

---

### 8. 2D Difference Array
- Use: Submatrix updates.
- Formula:  
    `diff[x1][y1]+=val`  
    `diff[x1][y2+1]-=val`  
    `diff[x2+1][y1]-=val`  
    `diff[x2+1][y2+1]+=val`  
    `final = prefix(prefix(diff))`
- Example: grid=[[0,0],[0,0]]
    Add +2 to submatrix (0,0)-(1,1) → final=[[2,2],[2,2]]
- Problem: 370
- Companies: Amazon, Microsoft
- Level: L3–L4
---


### 9. Prefix Frequency
- Use: Count of letters/numbers in subarray.
- Formula:  
`freq[i][c] = freq[i-1][c] + (arr[i]==c)`  
`count(L,R,c) = freq[R][c] - freq[L-1][c]`
- Example: arr=[a,b,a,c], char='a'  
freq=[0,1,1,2,2] → count(2,3,'a')=1
- Problems: 304 extended, anagram queries
- Companies: Google, Facebook  
- Level: L3–L5

---

### 10. Prefix Product
- Use: Product except self.
- Formula:  
`prefix[i]=prefix[i-1]*arr[i]`  
`suffix[i]=suffix[i+1]*arr[i]`
- Example: arr=[2,3,4]  
product except self at idx=1 → prefix[0]*suffix[2]=12
- Problem: 238
- Companies: Amazon, Google  
- Level: L3–L5

---

## Tier 3: Advanced / Rare

### 11. Prefix Hash (Rolling Hash)
- Use: Substring equality checks.
- Problem: 1044
- Companies: Google, Microsoft  
- Level: L4–L5

### 12. Prefix Bitmask
- Use: Track char sets.
- Problem: 1915
- Companies: Google, Facebook  
- Level: L4

### 13. Fenwick Tree (BIT)
- Use: Dynamic range queries.
- Problem: 307
- Companies: Amazon, Microsoft  
- Level: L4–L5

### 14. Segment Tree
- Use: General range queries + updates.
- Problems: 307, 732
- Companies: Amazon, Microsoft, Google  
- Level: L4–L5

### 15. Sparse Table
- Use: Static RMQ (min/max).
- Problem: Rare in interviews, CP focus
- Companies: Rare in interviews, mostly competitive programming
- Level: L4–L5