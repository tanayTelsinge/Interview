import DSA.tree.core.TreeNode;

public class DiameterOfBinaryTree {
    public int diameter = 0; 

    public int diameterOfBinaryTree(TreeNode root) {
       maxDepth(root);
       return diameter;
    }

    public int maxDepth(TreeNode root) {
        if (root == null) return 0;
        int left = maxDepth(root.left);
        int right = maxDepth(root.right);

        diameter = Math.max(left + right, diameter);
        return 1 + Math.max(left, right);
    }