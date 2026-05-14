package DSA.tree.dfs_leetcode;

import DSA.tree.core.TreeNode;

public class HardMaxPathSum {
    // 124. Binary Tree Maximum Path Sum
    // TC: O(n)  SC: O(h) — h=logn balanced, h=n skewed
    // Intuition: see note.txt -> Hard Max Path Sum section

    /*
     * 1. Get left, right result
     * 2. Clamp negatives to 0 (skip bad paths)
     * 3. Track global: max = max(max, l + r + node.val)  [arch through this node]
     * 4. Return: node.val + max(l, r)                    [single arm for parent]
     */
    public static int max = Integer.MIN_VALUE;

    public static void main(String[] args) {
        TreeNode root1 = new TreeNode(-10);
        root1.left = new TreeNode(9);
        root1.right = new TreeNode(20);
        root1.right.left = new TreeNode(15);
        root1.right.right = new TreeNode(7);

        System.out.println(new HardMaxPathSum().maxPathSum(root1)); // 42
    }

    public int maxPathSum(TreeNode root) {
        maxPathSumTemp(root);
        return max;
    }

    public int maxPathSumTemp(TreeNode root) {
        if (root == null) return 0;

        int l = Math.max(maxPathSumTemp(root.left), 0);
        int r = Math.max(maxPathSumTemp(root.right), 0);

        max = Math.max(l + r + root.val, max);

        return root.val + Math.max(l, r);
    }

}
