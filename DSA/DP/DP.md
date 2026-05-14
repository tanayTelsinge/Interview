##  Decision Tree (When to use)
Can the problem be divided into subproblems?
→ No (e.g., Sorting, Max in array) → Don’t use DP
→ Yes (e.g., Fibonacci, Knapsack) → Go to 2

Are subproblems overlapping?
→ No (e.g., Binary Search, Merge Sort) → Use Divide and Conquer
→ Yes (e.g., Fibonacci, Coin Change) → Go to 3

Does the problem have optimal substructure, means solution from optimal solution of subproblem?
→ No (e.g., Naive Traveling Salesman) → Don’t use DP
→ Yes (e.g., Longest Common Subsequence, 0/1 Knapsack) → Use DP!

## Two types
1. Memoization (Top-Down, Recursion)
2. Tabulation (Bottom-up, Loops)

### Why called TD and BU?
- In TD, we go from top to down, eg. in climbing stairs eg. in recursion, we are starting from F(n) and going n-1 n-2.
- In BU, we go from bottom to up, eg in loop, we are starting from 1 + 1 = 2, like normal Fibonacci.

### example with Climbing Stairs problem

- 1st Solution - Memoization, Recursive, Top-Down

```java
class Solution {

    public int climbStairs(int n) {
        int[] dp = new int[n + 1];
        return climbStairsHelper(n, dp);
    }

    public static int climbStairsHelper(int n, int[] dp) {
        if(n <= 1) {
            return 1;
        }
        if(dp[n] > 0) {
            return dp[n];
        }
        dp[n] = climbStairsHelper(n - 1, dp) + climbStairsHelper(n - 2, dp);
        return dp[n];
    }
}
TC - 
```
- 2st Solution - Tabulation, Iterative, Bottom-up
```java
class Solution {
    public int climbStairs(int n) {
        if(n <= 1) {
            return 1;
        }
        int oneStepBefore = 1, twoStepsBefore = 1;
        int current = 1;
        for(int i = 2; i <= n; i++) {
            current = oneStepBefore + twoStepsBefore;
            oneStepBefore = twoStepsBefore;
            twoStepsBefore = current;
        }
        return current;
    }
}
```

| Criteria                | First (Iterative)     | Second (Recursive + Memo)        |
| ----------------------- | --------------------- | -------------------------------- |
| **Time Complexity**     | O(n)                  | O(n)                             |
| **Space Complexity**    | O(1)                  | O(n)                             |
| **Stack Overflow Risk** | None                  | Possible for large `n`           |
| **Performance**         | Slightly better       | Slightly slower due to recursion |


### House Robber (LC 198)

Intuition: at each house you either **skip it** (carry forward `prev1`) or **rob it** (`prev2 + nums[i]`). You only need the last two values — no array needed.

```java
public int rob(int[] nums) {
    if (nums.length == 1) return nums[0];
    int prev2 = nums[0];
    int prev1 = Math.max(nums[0], nums[1]);
    for (int i = 2; i < nums.length; i++) {
        int curr = Math.max(prev1, prev2 + nums[i]);
        prev2 = prev1;
        prev1 = curr;
    }
    return prev1;
}
```

| | |
|---|---|
| **TC** | O(n) |
| **SC** | O(1) — two variables instead of dp array |

**Pattern:** 1D DP → space-optimized with two variables (`prev1`, `prev2`)
**Variants:** House Robber II (circular, LC 213), House Robber III (tree, LC 337)


### Longest Increasing Subsequence (LIS)

