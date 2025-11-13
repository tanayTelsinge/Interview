# Backtracking Types Summary

| Type | Goal | Example | Stop Condition | Tree Traversal | Pruning Type |
|------|------|---------|----------------|----------------|--------------|
| Decision | Find if any solution exists | Subset Sum | Stop after one valid solution | Partial | Feasibility |
| Optimization | Find best solution | Knapsack | After full search | Full | Feasibility + Optimality |
| Constraint Satisfaction | Meet all constraints | N-Queens, Sudoku | Stop after one or all valid solutions | Partial or Full | Feasibility |
| Enumeration | List all valid solutions | Permutations | After exploring all branches | Full | Minimal pruning |