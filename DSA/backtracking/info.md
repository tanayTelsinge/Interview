## Backtracking

- Back + tracking (Go back if not valid)
- DFS of state space tree

#### State Space tree
- Node - partial solution
- Root - initial empty state
- Leaf - complete solution or invalid solution


```java
// pseudocode
Backtrack(current_state):
    if current_state is a solution:
        process_solution(current_state)
        return

    for each choice in possible_choices(current_state):
        if is_valid(choice, current_state):
            make(choice)
            Backtrack(new_state)
            undo(choice)  // backtrack
```

```
Complexities:
TC: O(b^d)
where
b = branching factor (number of choices per level)
d = depth of the tree (number of decisions).

SC: O(d) because recursion stack holds one branch at a time