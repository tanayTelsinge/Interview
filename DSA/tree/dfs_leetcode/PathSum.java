package DSA.tree.dfs_leetcode;

import DSA.tree.core.TreeNode;

public class PathSum {

    public static void main(String[] args) {

        TreeNode root = TreeNode.getPopulatedTree();

        boolean ans = hasPathSum(root, 12);
        System.out.println(ans);

    }
   // Given the root of a binary tree and an integer targetSum,
     //return true if the tree has a root-to-leaf path such that adding up all the values along the path equals targetSum.
    public static boolean hasPathSum(TreeNode root, int targetSum) {
         if (root == null) return false;
        
        if (targetSum == root.val && root.left == null && root.right == null) {
            return true;
        } 
        boolean left = hasPathSum(root.left, targetSum - root.val);
        boolean right = hasPathSum(root.right, targetSum - root.val);
        return left || right;
    }
}
