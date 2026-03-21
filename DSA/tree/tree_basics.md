# Tree

## Core Idea
Hierarchical structure. Most problems = **DFS (recursion)** or **BFS (queue)**.
Think: "what does each node need from its children?" → return it up.

---

## Terminology
| Term | Meaning |
|---|---|
| Height | longest path from node to leaf |
| Depth | distance from root to node |
| Diameter | longest path between any two nodes (may not pass root) |
| Full BT | every node has 0 or 2 children |
| Complete BT | all levels full except last, filled left to right |
| Perfect BT | all internal nodes have 2 children, all leaves same level |
| BST | left < node < right (for all subtrees) |

---

## 1. DFS Traversals

```java
void inorder(TreeNode root) {   // Left → Root → Right  (BST = sorted)
    if (root == null) return;
    inorder(root.left);
    process(root.val);
    inorder(root.right);
}

void preorder(TreeNode root) {  // Root → Left → Right  (serialize/clone)
    if (root == null) return;
    process(root.val);
    preorder(root.left);
    preorder(root.right);
}

void postorder(TreeNode root) { // Left → Right → Root  (delete, height)
    if (root == null) return;
    postorder(root.left);
    postorder(root.right);
    process(root.val);
}
```

---

## 1b. DFS Traversals — Iterative

```java
// Inorder: Left → Root → Right  (use explicit stack)
List<Integer> inorderIterative(TreeNode root) {
    List<Integer> res = new ArrayList<>();
    Deque<TreeNode> stack = new ArrayDeque<>();
    TreeNode cur = root;
    while (cur != null || !stack.isEmpty()) {
        while (cur != null) { stack.push(cur); cur = cur.left; }  // go left
        cur = stack.pop();
        res.add(cur.val);     // process
        cur = cur.right;      // go right
    }
    return res;
}

// Preorder: Root → Left → Right  (push right first so left is processed first)
List<Integer> preorderIterative(TreeNode root) {
    List<Integer> res = new ArrayList<>();
    if (root == null) return res;
    Deque<TreeNode> stack = new ArrayDeque<>();
    stack.push(root);
    while (!stack.isEmpty()) {
        TreeNode node = stack.pop();
        res.add(node.val);                             // process
        if (node.right != null) stack.push(node.right); // right first (LIFO)
        if (node.left  != null) stack.push(node.left);
    }
    return res;
}

// Postorder: Left → Right → Root  (reverse of "Root → Right → Left")
List<Integer> postorderIterative(TreeNode root) {
    LinkedList<Integer> res = new LinkedList<>();
    if (root == null) return res;
    Deque<TreeNode> stack = new ArrayDeque<>();
    stack.push(root);
    while (!stack.isEmpty()) {
        TreeNode node = stack.pop();
        res.addFirst(node.val);                        // prepend (reverse)
        if (node.left  != null) stack.push(node.left);
        if (node.right != null) stack.push(node.right);
    }
    return res;
}
```

**Pattern summary:**
| Traversal | Stack trick |
|---|---|
| Inorder | chase left, pop & process, go right |
| Preorder | push right then left (LIFO = left first) |
| Postorder | mirror of preorder (Root→R→L), then reverse result |

---

## 2. BFS — Level Order

```java
Queue<TreeNode> q = new LinkedList<>();
q.offer(root);
while (!q.isEmpty()) {
    int size = q.size();          // ← snapshot level size
    for (int i = 0; i < size; i++) {
        TreeNode node = q.poll();
        process(node.val);
        if (node.left != null)  q.offer(node.left);
        if (node.right != null) q.offer(node.right);
    }
}
```

---

## 3. Height / Diameter Pattern

```java
int diameter = 0;

int height(TreeNode node) {
    if (node == null) return 0;
    int left  = height(node.left);
    int right = height(node.right);
    diameter = Math.max(diameter, left + right);  // update global
    return 1 + Math.max(left, right);             // return to parent
}
```
**Key:** diameter updated at every node; height returned up. Same pattern for any "local max passed up" problem.

---

## 4. Path Sum Pattern

```java
// Does a root-to-leaf path with sum = target exist?
boolean hasPath(TreeNode node, int remaining) {
    if (node == null) return false;
    if (node.left == null && node.right == null)
        return node.val == remaining;
    return hasPath(node.left,  remaining - node.val)
        || hasPath(node.right, remaining - node.val);
}
```

---

## 5. BST Operations

```java
// Search
TreeNode search(TreeNode root, int val) {
    if (root == null || root.val == val) return root;
    return val < root.val ? search(root.left, val) : search(root.right, val);
}

// Insert
TreeNode insert(TreeNode root, int val) {
    if (root == null) return new TreeNode(val);
    if (val < root.val) root.left  = insert(root.left,  val);
    else                root.right = insert(root.right, val);
    return root;
}

// Inorder successor (for delete)
TreeNode minNode(TreeNode node) {
    while (node.left != null) node = node.left;
    return node;
}
```

**BST inorder = sorted array** — use this for kth smallest, range queries.

---

## 6. LCA (Lowest Common Ancestor)

```java
// Binary Tree (LC 236)
TreeNode lca(TreeNode root, TreeNode p, TreeNode q) {
    if (root == null || root == p || root == q) return root;
    TreeNode left  = lca(root.left,  p, q);
    TreeNode right = lca(root.right, p, q);
    if (left != null && right != null) return root;  // split here
    return left != null ? left : right;
}

// BST (LC 235) — use BST property
TreeNode lcaBST(TreeNode root, TreeNode p, TreeNode q) {
    if (p.val < root.val && q.val < root.val) return lcaBST(root.left,  p, q);
    if (p.val > root.val && q.val > root.val) return lcaBST(root.right, p, q);
    return root;
}
```

---

## 7. Construct Tree from Traversals

| Given | Can Rebuild? | Key Trick |
|---|---|---|
| Preorder + Inorder | Yes | preorder[0] = root; find in inorder → splits left/right |
| Postorder + Inorder | Yes | postorder[last] = root |
| Preorder + Postorder | No (unique only if full BT) | — |
| Inorder only | No | — |

```java
// Preorder + Inorder (LC 105)
TreeNode build(int[] pre, int[] in, int preL, int inL, int inR, Map<Integer,Integer> idxMap) {
    if (preL > pre.length - 1 || inL > inR) return null;
    TreeNode root = new TreeNode(pre[preL]);
    int mid = idxMap.get(pre[preL]);
    int leftSize = mid - inL;
    root.left  = build(pre, in, preL + 1,           inL,     mid - 1, idxMap);
    root.right = build(pre, in, preL + leftSize + 1, mid + 1, inR,    idxMap);
    return root;
}
```

---

## Common Mistakes
1. Forgetting `null` base case → NPE
2. Diameter: don't return `left + right`, return `1 + max(left, right)`
3. BST delete: replace with inorder **successor** (min of right subtree)
4. Level order: snapshot `q.size()` before the inner loop, not inside
5. Path sum: check leaf `(left == null && right == null)`, not just `node != null`

---

## Decision Tree

```
Tree problem?
├── Process level by level?          → BFS + Queue
├── Need sorted order / BST?         → Inorder DFS
├── Build / Clone / Serialize?       → Preorder DFS
├── Height / Depth / Diameter?       → Postorder, return value up
├── Path (root-to-leaf)?             → DFS, subtract val
├── Path (any node to any node)?     → Postorder + global max (like diameter)
├── Common ancestor?
│   ├── Binary Tree                  → LCA LC 236
│   └── BST                         → LCA LC 235 (use BST property)
├── BST validation / search?         → Use min/max bounds
└── Reconstruct tree?                → Preorder+Inorder → LC 105
```

---

## Problem List

| Pattern | Problems |
|---|---|
| BFS / Level Order | LC 102, LC 103, LC 107, LC 116, LC 117, LC 994 |
| Height / Depth | LC 104, LC 111, LC 110, LC 1448 |
| Diameter / Path | LC 543, LC 124, LC 687, LC 112, LC 113, LC 437 |
| BST | LC 98, LC 230, LC 235, LC 450, LC 108, LC 99 |
| LCA | LC 236, LC 235, LC 1644, LC 1650 |
| Construct | LC 105, LC 106, LC 889, LC 297 |
| Trie | LC 208, LC 211, LC 212, LC 648, LC 421 |

---

### Level Order
```
L1 (foundation): LC 104 (Max Depth) → LC 102 (Level Order) → LC 226 (Invert Tree) → LC 112 (Path Sum) → LC 98 (Validate BST)
L2 (core FAANG): LC 543 (Diameter) → LC 236 (LCA Binary Tree) → LC 105 (Build from Pre+In) → LC 230 (Kth Smallest BST) → LC 103 (Zigzag Level Order)
L3 (high freq):  LC 124 (Max Path Sum) → LC 437 (Path Sum III) → LC 297 (Serialize/Deserialize) → LC 450 (Delete BST Node) → LC 208 (Implement Trie)
L4 (senior):     LC 212 (Word Search II) → LC 99 (Recover BST) → LC 1483 (Kth Ancestor) → LC 968 (Camera Cover)
```
