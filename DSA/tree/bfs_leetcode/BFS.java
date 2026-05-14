package DSA.tree.bfs_leetcode;

import DSA.tree.core.TreeNode;

import java.util.ArrayList;
import java.util.List;
import java.util.*;

//LC 102
public class BFS {

    public static void main(String[] args) {
        TreeNode root = TreeNode.getPopulatedTree();

        List<List<Integer>> list = bfs(root);
    }

    //TC - 0(n) 
    public static List<List<Integer>> bfs(TreeNode root) {
        List<List<Integer>> ans = new ArrayList<>();

        Queue<TreeNode> q = new LinkedList<>();
        q.add(root);

        while (!q.isEmpty()) {

            int size = q.size();
            TreeNode curr = q.poll();
            List<Integer> l = new ArrayList<>();

            while (size > 0) {
                if (curr.left != null) q.add(curr.left);
                if (curr.right != null) q.add(curr.right);
                l.add(curr.val);
            }
            ans.add(l);
        }
        return ans;
    }
    
}
