package DSA.tree.dfs_leetcode;

import DSA.tree.core.TreeNode;

public class MaxDepthOfBinaryTree {

    public static void main(String[] args) {

        TreeNode root = TreeNode.getPopulatedTree();

        System.out.println(maxDepth(root));
    }

    public static int maxDepth(TreeNode root) {
        if (root == null)
            return 0;

        int left = maxDepth(root.left);
        int right = maxDepth(root.right);
        return 1 + Math.max(left, right);

    }
}
