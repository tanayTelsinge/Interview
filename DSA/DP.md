## Two types
1. Memoization (Top-Down, Recursion)
2. Tabulation (Bottom-up, Loops)

### Why called TD and BU?
- In TD, we go from top to down, eg. in climbing stairs eg. in recursion, we are starting from F(n) and going n-1 n-2.
- In BU, we go from bottom to up, eg in loop, we are starting from 1 + 1 = 2, like normal Fibonacci.

### example with Climbing Stairs problem

-- 1st Solution - Memoization, Recursive, Top-Down

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
| **Readability**         | Simple, but iterative | Intuitive for recursion lovers   |
| **Stack Overflow Risk** | None                  | Possible for large `n`           |
| **Performance**         | Slightly better       | Slightly slower due to recursion |

