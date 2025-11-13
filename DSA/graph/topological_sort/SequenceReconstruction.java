package topological_sort;

import java.util.*;

/*
 * Sequence Reconstruction
 * https://leetcode.com/problems/sequence-reconstruction/description/
 * Check if original sequence can be uniquely reconstructed from sequences.
 * The org sequence is a permutation of the integers from 1 to n.
 * Example 1:
 * Input: org = [1,2,3], seqs = [[1,2],[1,3]]
 * Output: false
 * 
 * Explanation: [1,2,3] is not the only one sequence that can be reconstructed, because [1,3,2] is also valid.
 * Example 2:
 * Input: org = [1,2,3], seqs = [[1,2]]
 * Output: false
 * Explanation: The reconstructed sequence can only be [1,2].
 * Example 3:
 * Input: org = [1,2,3], seqs = [[1,2],[1,3],[2,3]]
 * Output: true
 * Explanation: The sequences [1,2], [1,3], and [2,3] can uniquely reconstruct the original sequence [1,2,3].

 * Intuition:
 * - Topological sort to determine unique order.
 * - Build graph from seqs, then perform Kahn's algorithm.
 */
public class SequenceReconstruction {
    public static boolean sequenceReconstruction(int[] org, List<List<Integer>> seqs) {
        if (org == null || org.length == 0) return false;

        // Step 1: Build the graph
        Map<Integer, Set<Integer>> adj = new HashMap<>();
        Map<Integer, Integer> indegree = new HashMap<>();

       buildGraph(seqs, adj, indegree);
        // Step 2: Topological Sort (Kahn's Algorithm)
        return topologicalSort(org, adj, indegree);
    }

    private static void buildGraph(List<List<Integer>> seqs, Map<Integer, Set<Integer>> adj,
            Map<Integer, Integer> indegree) {
        for (List<Integer> seq : seqs) {
            for (int i = 0; i < seq.size(); i++) {
                indegree.putIfAbsent(seq.get(i), 0);
                adj.putIfAbsent(seq.get(i), new HashSet<>());
                if (i > 0) {
                    int from = seq.get(i - 1);
                    int to = seq.get(i);
                    if (adj.get(from).add(to)) { // Only add edge if not present
                        indegree.put(to, indegree.get(to) + 1);
                    }
                }
            }
        }
    }

    private static boolean topologicalSort(int[] org, Map<Integer, Set<Integer>> adj,
            Map<Integer, Integer> indegree) {
        Queue<Integer> queue = new LinkedList<>();
        for (int node : indegree.keySet()) {
            if (indegree.get(node) == 0) {
                queue.offer(node);
            }
        }

        List<Integer> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            if (queue.size() > 1) {
                return false; // More than one way to reconstruct
            }
            int node = queue.poll();
            result.add(node);

            if (adj.containsKey(node)) {
                for (int neighbor : adj.get(node)) {
                    indegree.put(neighbor, indegree.get(neighbor) - 1);
                    if (indegree.get(neighbor) == 0) {
                        queue.offer(neighbor);
                    }
                }
            }
        }

        if (result.size() != indegree.size()) {
            return false; // Cycle detected or not all nodes included
        }

        // Check if result matches org
        if (result.size() != org.length) {
            return false;
        }
        for (int i = 0; i < org.length; i++) {
            if (result.get(i) != org[i]) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        int[] org = {1, 2, 3};
        List<List<Integer>> seqs = new ArrayList<>();
        seqs.add(Arrays.asList(1, 2));
        seqs.add(Arrays.asList(1, 3));
        // seqs.add(Arrays.asList(2, 3));
        System.out.println(sequenceReconstruction(org, seqs)); // Output: false
        // seqs.add(Arrays.asList
    }
