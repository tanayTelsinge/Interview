package DSA.tree.bfs_leetcode;

import DSA.tree.core.TreeNode;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Queue;
import java.util.LinkedList;

public class ZigZagTraversal {
    //take a boolean and reverse for each level
    //more optimized. control addition in level list (add first if reverse, add last if normal). eg. 3,2 or 2,3

    public static void main(String[] args) {
        TreeNode root = TreeNode.getPopulatedTree();

        List<List<Integer>> ans = zigzag(root);

        List<List<Integer>> ansOptimized = zigzagOpt(root);
    }

    public static List<List<Integer>> zigzagOpt(TreeNode root) {
        List<List<Integer>> ans = new ArrayList<>();

        if (root == null) return ans;

        Queue<TreeNode> q = new LinkedList<>();

        q.add(root);
        boolean reverse = false;

        while (!q.isEmpty()) {
            int size = q.size();
            List<Integer> lvl = new ArrayList<>();

            for(int i = 0; i < size; i++) {
                TreeNode curr = q.poll();
                
                if (reverse) {
                    lvl.addFirst(curr.val);  //core
                } else {
                    lvl.addLast(curr.val);
                }
                if (curr.left != null) q.add(curr.left);
                if (curr.right != null) q.add(curr.right);
            }

            ans.add(lvl);
            reverse = !reverse;
        }
        return ans;
    }


     public static List<List<Integer>> zigzag(TreeNode root) {
        List<List<Integer>> ans = new ArrayList<>();

        if (root == null) return ans;

        Queue<TreeNode> q = new LinkedList<>();

        q.add(root);
        boolean reverse = false;

        while (!q.isEmpty()) {
            int size = q.size();
            List<Integer> lvl = new ArrayList<>();

            for(int i = 0; i < size; i++) {
                TreeNode curr = q.poll();
                
                if (curr.left != null) q.add(curr.left);
                if (curr.right != null) q.add(curr.right);
            }
            if (reverse) {
                Collections.reverse(lvl);
            }
            ans.add(lvl);
            reverse = !reverse;
        }
        return ans;
    }
}
