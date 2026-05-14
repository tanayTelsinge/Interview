package DSA.tree.bfs_leetcode;

import java.util.LinkedList;
import java.util.Queue;

import DSA.tree.core.TreeNode;

public class MinDepth {
    //take a variable and break updating once we find first leaf node (left and right null)
    /**
     * Because BFS explores level by level, so the first leaf node we find = minimum depth.
        DFS would explore deep first and may not give minimum quickly.
         Problem	Use
        Minimum depth	BFS
        Maximum depth	DFS
        Shortest path	BFS
     * }
     * }
     */
    public int minDepth(TreeNode root) {
        if (root == null)
            return 0;
        Queue<TreeNode> q = new LinkedList<>();
        q.add(root);
        int lvl = 0;

        while (!q.isEmpty()) {
            int size = q.size();
            lvl++;
            for (int i = 0; i < size; i++) {
                TreeNode curr = q.poll();
                if (curr.left == null && curr.right == null) {
                    return lvl;
                }
                if (curr.left != null)
                    q.add(curr.left);
                if (curr.right != null)
                    q.add(curr.right);
            }
        }
        return lvl;
    }
}
