package DSA.tree.core;

import java.util.Queue;
import java.util.LinkedList;

public class Tree {
    
    //        1
   //      2       3
   //   4    5  6     7
    public static void main(String[] args) {
        TreeNode root = TreeNode.getPopulatedTree();

        bfs(root);

        System.out.println("-------------");
        dfsInorder(root);
        //dfsPreOrder(root);
        //dfsPostOrder(root);
        
        // dfsInorderIterative(root);
        // dfsPreorderIterative(root);
        // dfsPostorderIterative(root);



    }

    public static void dfsInorder(TreeNode root) {
        //L R Ri
        if (root == null) return;

        dfsInorder(root.left);
        System.out.print(root.val + " ");
        dfsInorder(root.right);
    }

    public static void bfs(TreeNode root) {
        Queue<TreeNode> q = new LinkedList<>();
        q.add(root);

        while (!q.isEmpty()) {
            TreeNode currNode = q.poll();

            if (currNode.left != null) {
                q.add(currNode.left);
            }

            if (currNode.right != null) {
                q.add(currNode.right);
            }

            System.out.print(currNode.val + " -> ");
        }
    }
}
