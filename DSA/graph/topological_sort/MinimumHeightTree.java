package topological_sort;

import java.util.*;

/*
 * Minimum Height Trees
 * https://leetcode.com/problems/minimum-height-trees/description/
 * For an undirected graph with tree characteristics, we can choose any node as the root.
 *  The result graph is then a rooted tree. Among all possible rooted trees, those with minimum height are called minimum height trees (MHTs).
 *  Given such a graph, write a function to find all the MHTs and return a list of their root labels.
 * 
 */
public class MinimumHeightTree {
    public List<Integer> findMinHeightTrees(int n, int[][] edges) {
        // Handle edge cases
        if (n <= 0) return new ArrayList<>();
        if (n == 1) {
            List<Integer> result = new ArrayList<>();
            result.add(0);
            return result;
        }

        // Build adjacency list representation of the graph
        List<Set<Integer>> adjList = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            adjList.add(new HashSet<>());
        }
        for (int[] edge : edges) {
            adjList.get(edge[0]).add(edge[1]);
            adjList.get(edge[1]).add(edge[0]);
        }

        // Find all leaf nodes (nodes with degree 1)
        List<Integer> leaves = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (adjList.get(i).size() == 1) {
                leaves.add(i);
            }
        }

        // Keep removing leaf nodes until we reach the center(s)
        int remainingNodes = n;
        while (remainingNodes > 2) {
            remainingNodes -= leaves.size();
            List<Integer> newLeaves = new ArrayList<>();
            
            // Process all current leaf nodes
            for (int leaf : leaves) {
                // Get the only neighbor of this leaf
                int neighbor = adjList.get(leaf).iterator().next();
                // Remove this leaf from neighbor's adjacency list
                adjList.get(neighbor).remove(leaf);
                // If neighbor becomes a leaf, add it to new leaves
                if (adjList.get(neighbor).size() == 1) {
                    newLeaves.add(neighbor);
                }
            }
            leaves = newLeaves;
        }

        return leaves;
    }

    public static void main(String[] args) {
        MinimumHeightTree solution = new MinimumHeightTree();
        
        // Test case 1: n = 4, edges = [[1,0],[1,2],[1,3]]
        int[][] edges1 = {{1,0}, {1,2}, {1,3}};
        System.out.println("Test case 1: " + solution.findMinHeightTrees(4, edges1));  // Expected: [1]

        // Test case 2: n = 6, edges = [[3,0],[3,1],[3,2],[3,4],[5,4]]
        int[][] edges2 = {{3,0}, {3,1}, {3,2}, {3,4}, {5,4}};
        System.out.println("Test case 2: " + solution.findMinHeightTrees(6, edges2));  // Expected: [3,4]
    }
}
