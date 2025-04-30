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

```text
Recursive Patterns with Tricks
├── 1. Linear Recursion
│   ├── Aggregation & Summation
│   │   ├── Trick: Use tail recursion with an accumulator to optimize space.
│   │   ├── Work Problem: Sum of an array.
│   │   └── Tip: Use helper function with accumulator.
│   ├── Sequential Traversal
│   │   ├── Trick: Process one element at a time; consider iteration for deep recursion.
│   │   ├── Work Problem: Reverse a linked list.
│   │   └── Tip: Use recursion to reverse node links.
│   └── Single-path Search
│       ├── Trick: Implement early termination for efficiency.
│       ├── Work Problem: Linear search.
│       └── Tip: Return immediately when element is found.

├── 2. Tree Recursion
│   ├── Fibonacci-like Problems
│   │   ├── Trick: Use memoization to cache results.
│   │   ├── Work Problem: Fibonacci sequence.
│   │   └── Tip: Store results in a dictionary.
│   ├── Combinations & Subsets
│   │   ├── Trick: Use backtracking and prune invalid branches.
│   │   ├── Work Problem: Generate subsets.
│   │   └── Tip: Include/exclude elements recursively.
│   └── Recursive Decision Trees
│       ├── Trick: Reduce search space with inclusion/exclusion.
│       ├── Work Problem: Coin change.
│       └── Tip: Use DP to cache and reuse subproblem solutions.

├── 3. Divide and Conquer
│   ├── Sorting
│   │   ├── Trick: Choose a good pivot to minimize recursive calls.
│   │   ├── Work Problem: Merge sort.
│   │   └── Tip: Use random pivot in quicksort.
│   ├── Searching
│   │   ├── Trick: Binary search on sorted arrays; exponential search for unknown size.
│   │   ├── Work Problem: Binary search.
│   │   └── Tip: Sort input if needed before search.
│   ├── Matrix & Tree Decomposition
│   │   ├── Trick: Break matrix/tree recursively for better performance.
│   │   ├── Work Problem: Matrix multiplication (Strassen’s).
│   │   └── Tip: Balance subtrees for O(log n) height.
│   └── Divide for Optimization
│       ├── Trick: Solve subregions and combine (e.g. closest pair).
│       ├── Work Problem: Closest pair of points.
│       └── Tip: Only compare points within a threshold band.

├── 4. Backtracking
│   ├── Permutations & Combinations
│   │   ├── Trick: Prune invalid paths early.
│   │   ├── Work Problem: All permutations.
│   │   └── Tip: Generate lexicographically for control.
│   ├── Constraint Satisfaction Problems
│   │   ├── Trick: Apply constraints early (e.g. N-Queens column checks).
│   │   ├── Work Problem: N-Queens.
│   │   └── Tip: Backtrack early on conflict.
│   ├── Subset & Combination Sum Problems
│   │   ├── Trick: Prune paths exceeding target sum.
│   │   ├── Work Problem: Combination sum.
│   │   └── Tip: Sort input to detect overflows early.
│   └── Optimization Problems
│       ├── Trick: Use memoization for repeated decisions.
│       ├── Work Problem: Knapsack.
│       └── Tip: Maintain running totals of weight/value recursively.

├── 5. Recursive Search (DFS)
│   ├── Graph Traversal (DFS)
│   │   ├── Trick: Use a visited set to prevent cycles.
│   │   ├── Work Problem: DFS traversal.
│   │   └── Tip: Use stack/queue explicitly if needed.
│   ├── Grid Traversal & Region Problems
│   │   ├── Trick: Store visited cells and use memoization.
│   │   ├── Work Problem: Number of islands.
│   │   └── Tip: Use DFS/BFS to explore connected regions.
│   └── Backtracking for Search
│       ├── Trick: Use Trie/prefix tree for pruning invalid paths.
│       ├── Work Problem: Word search.
│       └── Tip: Abort recursion if prefix doesn't match any valid word.

└── 6. Memoized Recursion (Top-down DP)
    ├── Fibonacci-like Problems
    │   ├── Trick: Store computed results in a map/array.
    │   ├── Work Problem: Fibonacci.
    │   └── Tip: Bottom-up table avoids deep recursion.
    ├── Knapsack-like Problems
    │   ├── Trick: Use DP tables to store optimal subproblem results.
    │   ├── Work Problem: 0/1 Knapsack.
    │   └── Tip: Reduce 2D DP to 1D for space efficiency.
    ├── Grid-based Problems
    │   ├── Trick: Memoize subgrid solutions.
    │   ├── Work Problem: Unique paths in grid.
    │   └── Tip: Prefer bottom-up tabulation to save stack.
    ├── Subset & Partition Problems
    │   ├── Trick: Use bitmask DP for efficient state representation.
    │   ├── Work Problem: Subset sum.
    │   └── Tip: Split problem into solvable partitions.
    └── String Problems
        ├── Trick: Use 2D table to memoize string edits.
        ├── Work Problem: Edit distance.
        └── Tip: Memoize to reduce recomputation.
```
