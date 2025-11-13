```java
class Solution {
    public List<List<Integer>> subsets(int[] nums) {
        List<List<Integer>> res = new ArrayList<>();
        backtrack(nums, 0, new ArrayList<>(), res);
        return res;
    }

    private void backtrack(int[] nums, int idx, List<Integer> path, List<List<Integer>> res) {
        res.add(new ArrayList<>(path));

        for (int i = idx; i < nums.length; i++) {
            path.add(nums[i]);
            backtrack(nums, i + 1, path, res);
            path.remove(path.size() - 1);
        }
    }
}

```

#### Debug trace
```
Start:
path = []
res = [[]]              // added empty subset

Loop i=0 (choose 1):
path = [1]
res = [[], [1]]

    Recurse idx=1:
    Loop i=1 (choose 2):
    path = [1,2]
    res = [[], [1], [1,2]]

        Recurse idx=2:
        Loop i=2 (choose 3):
        path = [1,2,3]
        res = [[], [1], [1,2], [1,2,3]]

        Backtrack: remove 3
        path = [1,2]

    Backtrack: remove 2
    path = [1]

    Loop i=2 (choose 3):
    path = [1,3]
    res = [[], [1], [1,2], [1,2,3], [1,3]]

    Backtrack: remove 3
    path = [1]

Backtrack: remove 1
path = []

Loop i=1 (choose 2):
path = [2]
res = [[], [1], [1,2], [1,2,3], [1,3], [2]]

    Recurse idx=2:
    Loop i=2 (choose 3):
    path = [2,3]
    res = [[], [1], [1,2], [1,2,3], [1,3], [2], [2,3]]

    Backtrack: remove 3
    path = [2]

Backtrack: remove 2
path = []

Loop i=2 (choose 3):
path = [3]
res = [[], [1], [1,2], [1,2,3], [1,3], [2], [2,3], [3]]

Backtrack: remove 3
path = []

```

```visualize

[]
├── [1]
│   ├── [1,2]
│   │   ├── [1,2,3]
│   │   └── (backtrack)
│   └── [1,3]
│       └── (backtrack)
├── [2]
│   ├── [2,3]
│   │   └── (backtrack)
│   └── (backtrack)
└── [3]
    └── (backtrack)

```