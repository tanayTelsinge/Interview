package DSA.tree.dfs_leetcode.dfs_backtracking_tree;

import DSA.tree.core.TreeNode;

import java.util.ArrayList;
import java.util.List;

public class PathSumTwo {
    //return all paths as per targetSum -> List<List<Integer>> result
    //Intuition - as multiple paths, are backtracking.
    //Q What to add (path), When to return (leaf and sum == root.val add path in result)
    //- Note *** - result.add(path) wrong, if u remove from path, it removes from result too, same reference.
    // hence new ArrayList<>(path).
    //Note *** path.add(root.val) before leaf check, because last leaf node value as consider in path.
    public static void main(String[] args) {
        TreeNode root = new TreeNode(5);
        root.left = new TreeNode(4);
        root.right = new TreeNode(8);
        root.left.left = new TreeNode(11);
        root.left.left.right = new TreeNode(2);

        root.right.right = new TreeNode(4);
        root.right.right.left = new TreeNode(5);

        int targetSum = 22;
        List<List<Integer>> result = new PathSumTwo().pathSum(root, targetSum);

        System.out.println(result);
    }

    public List<List<Integer>> pathSum(TreeNode root, int targetSum) {
        List<List<Integer>>  result = new ArrayList<>();
        List<Integer> path = new ArrayList<>();

        dfs(root, result, path, targetSum);
        return result;
    }

    public void dfs(TreeNode root, List<List<Integer>> result, List<Integer> path, int targetSum) {
        if (root == null) return;

        path.add(root.val);

        if (root.left == null && root.right == null) {
            if (targetSum == root.val) {
                result.add(new ArrayList<>(path));
            }
        }
        
        dfs(root.left, result, path, targetSum - root.val);
        dfs(root.right, result, path, targetSum - root.val);
        path.remove(path.size() - 1);
    
    }
}
