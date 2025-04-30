#### Recursion can be solved by multiple approaches:
1. IBH (Induction - Base - Hypothesis) -> check for nearest case. (for print 1 - N, height of tree etc.)
   - check what are ip - op
   - Hypothesis decide - what will function do? check for smaller input.
   - Base - what will be base condition.
   - Induction - core logic.
3. Recursion tree (for subset, subsequences etc)

#### IBH
![image](https://github.com/user-attachments/assets/c2c53bdf-380e-4fbe-89ff-9a9249617e40)
- check video aditya verma - https://www.youtube.com/watch?v=aqLTbtWh40E&list=PL_z_8CaSLPWeT1ffjiImo0sYTcnLzo-wY&index=5

# 🌳 Recursive Patterns with Tricks

## ├── 1. Linear Recursion
│   ├── **Aggregation & Summation**
│   │   ├── **Trick**: For problems like sum, product, or counting elements, use **tail recursion** to optimize space complexity (eliminating the need for additional stack frames).
│   │   ├── **Work Problem**: Calculate the sum of an array.
│   │   └── **Tip**: Convert the problem to a helper function that carries an additional accumulator (e.g., `acc`) to avoid recomputing intermediate values.
│   ├── **Sequential Traversal**
│   │   ├── **Trick**: If you're traversing an array or linked list recursively, break down the problem by processing one element at a time and then moving forward. For better space complexity, use an iterative approach if recursion depth is too large.
│   │   ├── **Work Problem**: Reverse a linked list.
│   │   └── **Tip**: Use the recursive call as the point to swap the next element with the current one, reducing complexity.
│   └── **Single-path Search**
│       ├── **Trick**: Implement **early termination** if the base case is found before reaching the end. This helps save time when searching for an element in an array.
│       ├── **Work Problem**: Linear Search.
│       └── **Tip**: When searching for an element, use a condition to immediately return when the element is found to avoid unnecessary recursion.

## ├── 2. Tree Recursion
│   ├── **Fibonacci-like Problems**
│   │   ├── **Trick**: Use **memoization** to store previously computed values in a hash table to avoid redundant calculations.
│   │   ├── **Work Problem**: Fibonacci sequence.
│   │   └── **Tip**: Instead of recomputing each Fibonacci number from scratch, store the results in a dictionary or array, and look them up as needed.
│   ├── **Combinations & Subsets**
│   │   ├── **Trick**: Use a **backtracking** approach to build subsets or combinations incrementally. **Prune** the search space early if conditions are violated (e.g., the sum exceeds a target).
│   │   ├── **Work Problem**: Generate all subsets of a set.
│   │   └── **Tip**: Leverage the fact that for each element, you either include it or exclude it, and recursively solve the problem for the next element.
│   └── **Recursive Decision Trees**
│       ├── **Trick**: For **binary decision problems**, reduce the problem space by splitting it into two or more smaller subproblems at each step (e.g., in the knapsack problem, try to include or exclude an item).
│       ├── **Work Problem**: Coin change problem.
│       └── **Tip**: Use dynamic programming (memoization or tabulation) in addition to recursion to keep track of already solved subproblems and improve efficiency.

## ├── 3. Divide and Conquer
│   ├── **Sorting**
│   │   ├── **Trick**: In **Merge Sort** or **Quick Sort**, the partition step (splitting the array) is key. Ensure that the pivot or midpoint is chosen wisely to minimize the number of recursive calls.
│   │   ├── **Work Problem**: Merge Sort.
│   │   └── **Tip**: For Quick Sort, choose a **random pivot** to avoid worst-case time complexity (O(n^2)) when the input is sorted or nearly sorted.
│   ├── **Searching**
│   │   ├── **Trick**: **Binary Search** is efficient for sorted arrays. Always ensure the array is sorted before applying binary search, or implement **exponential search** when the size of the array is unknown.
│   │   ├── **Work Problem**: Binary Search.
│   │   └── **Tip**: If the array is not sorted, you can use **quick sort** or **merge sort** to sort it first.
│   ├── **Matrix & Tree Decomposition**
│   │   ├── **Trick**: For **Strassen’s Matrix Multiplication**, break the matrix down into smaller subproblems and solve recursively to get a better time complexity (O(n^log7)).
│   │   ├── **Work Problem**: Matrix Multiplication.
│   │   └── **Tip**: For tree balancing, recursively balance left and right subtrees to maintain an O(log n) height.
│   └── **Divide for Optimization**
│       ├── **Trick**: For problems like finding the **closest pair of points**, break the points down into smaller subsets, find the closest pairs recursively, and combine the results.
│       ├── **Work Problem**: Closest Pair of Points.
│       └── **Tip**: When combining results, ensure that you only compare points within a specific distance to avoid unnecessary checks.

## ├── 4. Backtracking
│   ├── **Permutations & Combinations**
│   │   ├── **Trick**: When generating **permutations** or **combinations**, try to **prune** the search tree when it’s clear that a path will not lead to a valid solution (e.g., if the sum exceeds the target).
│   │   ├── **Work Problem**: Generate all permutations.
│   │   └── **Tip**: Use **lexicographical order** to generate permutations more efficiently.
│   ├── **Constraint Satisfaction Problems**
│   │   ├── **Trick**: For problems like the **N-Queens**, apply constraints early to eliminate invalid choices (e.g., avoid placing a queen in a column that’s already occupied).
│   │   ├── **Work Problem**: Solve N-Queens.
│   │   └── **Tip**: Use **backtracking** with **pruning** to explore possible configurations and **terminate early** when you find an invalid solution.
│   ├── **Subset & Combination Sum Problems**
│   │   ├── **Trick**: Use **pruning** in combination sum problems to skip over paths that are not promising (e.g., when the sum exceeds the target).
│   │   ├── **Work Problem**: Combination Sum.
│   │   └── **Tip**: Sort the input array to improve performance by allowing you to stop early when the current sum exceeds the target.
│   └── **Optimization Problems**
│       ├── **Trick**: When solving optimization problems (e.g., knapsack), consider whether you need to use **memoization** to store intermediate solutions and avoid redundant computation.
│       ├── **Work Problem**: Knapsack problem.
│       └── **Tip**: Solve by recursively choosing to include or exclude an item while maintaining a running total of the value and weight.

## ├── 5. Recursive Search (DFS)
│   ├── **Graph Traversal (DFS)**
│   │   ├── **Trick**: When implementing **DFS**, use a **visited set** to avoid revisiting nodes. For large graphs, consider using **iterative DFS** to avoid stack overflow.
│   │   ├── **Work Problem**: DFS Traversal.
│   │   └── **Tip**: Use **stack** or **queue** data structures explicitly in your implementation for better control.
│   ├── **Grid Traversal & Region Problems**
│   │   ├── **Trick**: In problems like **Flood Fill**, store visited cells in a separate data structure to prevent revisiting them. Consider **memoization** if the problem has overlapping subproblems.
│   │   ├── **Work Problem**: Number of Islands.
│   │   └── **Tip**: Use **DFS** or **BFS** to find all connected components or regions.
│   └── **Backtracking for Search**
│       ├── **Trick**: For problems like **word search**, ensure that you only continue if the current path can form a valid word (use a **prefix tree (Trie)** to optimize).
│       ├── **Work Problem**: Word Search.
│       └── **Tip**: Prune paths early if the current word cannot be completed.

## └── 6. Memoized Recursion (Top-down DP)
    ├── **Fibonacci-like Problems**
    │   ├── **Trick**: For **Fibonacci** and other sequence problems, store previously computed values to avoid recomputing them. Use a dictionary or array for efficient lookups.
    │   ├── **Work Problem**: Fibonacci sequence.
    │   └── **Tip**: If using a bottom-up approach, fill a DP table iteratively to avoid excessive recursion depth.
    ├── **Knapsack-like Problems**
    │   ├── **Trick**: For **knapsack problems**, build a DP table where each cell represents the optimal solution for a subproblem. This avoids recalculating the same subproblems.
    │   ├── **Work Problem**: 0/1 Knapsack.
    │   └── **Tip**: Use **iterative DP** to optimize memory usage (usually 2D DP can be reduced to 1D).
    ├── **Grid-based Problems**
    │   ├── **Trick**: Use a **memoization table** to store results of subproblems in problems like finding unique paths in a grid.
    │   ├── **Work Problem**: Unique Paths in a Grid.
    │   └── **Tip**: For grid-based DP problems, use the **tabulation** technique for bottom-up computation to avoid deep recursion.
    ├── **Subset & Partition Problems**
    │   ├── **Trick**: Use a **bitmask** to represent subsets efficiently in DP problems.
    │   ├── **Work Problem**: Subset Sum Problem.
    │   └── **Tip**: For partition problems, try dividing the problem into smaller subproblems that can be solved with smaller states.
    └── **String Problems**
        ├── **Trick**: For string problems like **Edit Distance**, use a **2D table** (DP table) to store intermediate results.
        ├── **Work Problem**: Edit Distance.
        └── **Tip**: Use **memoization** to optimize space complexity and avoid unnecessary recalculations.

---

### Summary of Key Tips:
1. **Memoization** is crucial for optimizing overlapping subproblems.
2. **Pruning** can help you skip invalid paths early.
3. **Tail recursion** and iterative solutions can often save space in deep recursive calls.
4. **Sorting inputs
