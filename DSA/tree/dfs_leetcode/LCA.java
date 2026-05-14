package DSA.tree.dfs_leetcode;

import DSA.tree.core.TreeNode;

public class LCA {
    // find lowest common ancestor if we give p and q - two nodes.

    public static void main(String[] args) {
        TreeNode root = new TreeNode(3);
        root.left = new TreeNode(5);
        root.right = new TreeNode(1);
        root.left.left = new TreeNode(6);
        root.left.right = new TreeNode(2);
        root.left.right.left = new TreeNode(7);
        root.left.right.right = new TreeNode(4);

        root.right = new TreeNode(1);
        root.right.left = new TreeNode(0);
        root.right.right = new TreeNode(8);

        TreeNode p = new TreeNode(5);
        TreeNode q = new TreeNode(4);
        TreeNode lca = new LCA().lowestCommonAncestor(root, p, q);
        System.out.println(lca.val);
    }

    public TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {

        if (root == null)
            return root;

        TreeNode l = lowestCommonAncestor(root.left, p, q);
        TreeNode r = lowestCommonAncestor(root.right, p, q);

        if (l != null && r != null) {
            if ((l.val == p.val && r.val == q.val) || (l.val == q.val && r.val == p.val))
                return root;
        }
        if (l != null && (l.val == p.val || l.val == q.val)) {
            return l;
        }
        if (r != null && (r.val == p.val || r.val == q.val)) {
            return r;
        }
        return root;
    }

}
