Seperate elements (in sets), you want to connect them.

2 operations:
Find(x) → figure out which group x belongs to (returns a representative of the set).
Union(x, y) → merge the sets containing x and y.

Usage
Detecting cycles in graphs.
Kruskal’s Minimum Spanning Tree algorithm.
Dynamic connectivity problems.
checking if two people are in the same friend circle in social networks.

## Optimizations for efficiency:

###  Path compression
- used to flatten the structure of the tree whenever Find is called on it.
- makes future queries faster by making nodes point directly to the root of the set.
- How?
When you call Find(x), traverse up the tree to find the root.
- As you traverse, make each node point directly to the root.
- This reduces the height of the tree, speeding up future operations.

```
0
└── 1
    └── 2
        └── 3
call find(3):
find(3) sees parent[3] = 2, not root → recurse.
find(2) sees parent[2] = 1, not root → recurse.
find(1) sees parent[1] = 0, which is root → return 0.
On the way back:
parent[2] = 0
parent[3] = 0

After path compression:
0
├── 1
├── 2
└── 3

```

### Union by rank
- keeps the tree flat by attaching the smaller tree under the root of the larger tree during union operations.
- Rank is an upper bound on the height of the tree.
- How?
When performing Union(x, y), find the roots of both sets.
Compare their ranks/sizes.
Attach the smaller tree under the root of the larger tree.
If ranks/sizes are equal, choose one as the new root and increment its rank/size

---
### WHen to use?
Ask yourself:
- Can elements belong to sets/groups/components?
- Do I need to merge groups efficiently?
- Do I need to check if two elements are in the same set repeatedly?
- Can the problem be represented as a graph of nodes and edges?
- If most answers are yes, DSU is probably the right tool.

#### What FAANG problems?
- Graph Valid Tree
- Number of Connected Components in an Undirected Graph
- Accounts Merge
- Smallest String With Swaps
- Number of Islands I and II

- Companies: Google, Facebook
- Level: L3–L5

### Code Snippet
```
class DisjointSet {
    int[] parent;
    int[] rank; // or size

    public DisjointSet(int n) {
        parent = new int[n];
        rank = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
            rank[i] = 1; // or size[i] = 1
        }
    }

    public int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]); // Path compression
        }
        return parent[x];
    }

    public void union(int x, int y) {
        int rootX = find(x);
        int rootY = find(y);
        if (rootX != rootY) {
            // Union by rank
            if (rank[rootX] > rank[rootY]) {
                parent[rootY] = rootX;
            } else if (rank[rootX] < rank[rootY]) {
                parent[rootX] = rootY;
            } else {
                parent[rootY] = rootX;
                rank[rootX]++;
            }
        }
    }
}
```

- Cycle Detection
    - Undirected Graph - DSU
    - Directed Graph - Topological Sort (Kahn's Algorithm)

- How cycle detection via DSU in undirected graph?
```
1. Initialize DSU for all vertices.
2. For each edge (u, v):
   a. Find the parent of u and v. (find(u), find(v))
   b. If they have the same parent, a cycle is detected. 
   c. Otherwise, union their sets.

eg. 
0 -- 1
1 -- 2
2 -- 0 //if no cycle this edge won't be there

Initially, each node is its own parent:
0   1   2
After processing edge (0, 1):
find(0) = 0, find(1) = 1 → different sets, so union them.
Union(0, 1):
0
└── 1   2
After processing edge (1, 2):
find(1) = 0 (after path compression), find(2) = 2 → different sets, so union them.
Union(1, 2):
0
└── 1
    └── 2
After processing edge (2, 0):
find(2) = 0 (after path compression), find(0) = 0 → same set, cycle detected.

```