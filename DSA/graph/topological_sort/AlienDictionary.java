package topological_sort;

import java.util.*;

/*
 * Alien Dictionary
 * https://leetcode.com/problems/alien-dictionary/description/
 * There is a new alien language which uses the latin alphabet. However, the order among letters are unknown to you. You receive a list of non-empty words from the dictionary, where words are sorted lexicographically by the rules of this new language. Derive the order of letters in this language.
 * Example 1:
 * Input:
 * ["wrt","wrf","er","ett","rftt"]
 * Output: "wertf"
 * Example 2:
 * Input:
 * ["z","x"]
 * Output: "zx"
 * Example 3:
 * Input:
 * ["z","x","z"]
 * Output: ""
 * Explanation: The order is invalid, so return "".
 * 
 * Note:
 * You may assume all letters are in lowercase.
 * If the order is invalid, return an empty string.
 * There may be multiple valid order of letters, return any one of them is fine.
 * Constraints:
 * 1 <= words.length <= 100
 * 1 <= words[i].length <= 100
 * words[i] consists of only lowercase English letters.
 * 
 * Intuition:
 * - Topological sort of characters based on given words.
 * - Build graph of char dependencies from adjacent words.
 * - Use Kahn's or DFS based topological sort.
 * - Handle edge cases: prefix words, cycles.
 * - Return any valid order or "" if invalid.
 * Approach:
 * - Build graph:
 *  - For each pair of adjacent words, find first differing char.
 *   - Add directed edge from char in first word to char in second word.
 *  - If second word is prefix of first, return "" (invalid).
 * - Topological Sort (Kahn's or DFS)
 */
public class AlienDictionary {
    public static String alienOrder(String[] words) {
        Map<Character, Set<Character>> adj = new HashMap<>();
        Map<Character, Integer> indegree = new HashMap<>();

        String result = buildGraph(words, adj, indegree);
        if (result.isEmpty()) {
            return "";
        }
        return topologicalSort(adj, indegree);
    }

    private static String buildGraph(String[] words, Map<Character, Set<Character>> adj,
            Map<Character, Integer> indegree) {
        // Initialize graph
        for (String word : words) {
            for (char c : word.toCharArray()) {
                adj.putIfAbsent(c, new HashSet<>());
                indegree.putIfAbsent(c, 0);
            }
        }
        // Build edges
        for (int i = 0; i < words.length - 1; i++) {
            String w1 = words[i];
            String w2 = words[i + 1];
            int minLen = Math.min(w1.length(), w2.length());
            boolean foundDiff = false;
            for (int j = 0; j < minLen; j++) {
                char c1 = w1.charAt(j);
                char c2 = w2.charAt(j);
                if (c1 != c2) {
                    if (!adj.get(c1).contains(c2)) {
                        adj.get(c1).add(c2);
                        indegree.put(c2, indegree.get(c2) + 1);
                    }
                    foundDiff = true;
                    break;
                }
            }
            // Edge case: prefix situation
            if (!foundDiff && w1.length() > w2.length()) {
                return "";
            }
        }
        return "valid"; // Return a non-empty string to indicate valid graph construction
    }

    private static String topologicalSort(Map<Character, Set<Character>> adj, Map<Character, Integer> indegree) {
        StringBuilder order = new StringBuilder();
        Queue<Character> queue = new LinkedList<>();

        // Add all vertices with no incoming edges
        for (char c : indegree.keySet()) {
            if (indegree.get(c) == 0) {
                queue.offer(c);
            }
        }

        // Process until queue is empty
        while (!queue.isEmpty()) {
            char curr = queue.poll();
            order.append(curr);

            // Reduce indegree for all neighbors
            for (char next : adj.get(curr)) {
                indegree.put(next, indegree.get(next) - 1);
                if (indegree.get(next) == 0) {
                    queue.offer(next);
                }
            }
        }

        // Check if we visited all vertices
        if (order.length() != indegree.size()) {
            return ""; // Cycle detected
        }

        return order.toString();
    }

    public static void main(String[] args) {
        String[] words = { "wrt", "wrf", "er", "ett", "rftt" };
        // Expected output: "wertf"
        // String[] words = {"z","x","z"};
        // Expected output: ""
        System.out.println(alienOrder(words));
    }
}
