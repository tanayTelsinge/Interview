package DSA.tree.bfs_leetcode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import DSA.tree.core.TreeNode;

public class AverageOfLevels {
        //calculate avg at level
       public List<Double> averageOfLevels(TreeNode root) {
        List<Double> ans = new ArrayList<>();
        if (root == null) return ans;
        Queue<TreeNode> q = new LinkedList<>();
        q.add(root);

        while (!q.isEmpty()) {
            int size = q.size();
            Double sum = 0d;    
            for(int i = 0; i < size; i++) {
                TreeNode curr = q.poll(); 
                if (curr.left != null) q.add(curr.left);
                if (curr.right != null) q.add(curr.right);
                sum += curr.val;
            }
            Double avg = sum / size;
            ans.add(avg);
        }
        return ans;
    }
}
