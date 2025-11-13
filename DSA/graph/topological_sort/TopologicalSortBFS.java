import java.util.*;

public class TopologicalSortBFS {
    public static List<Integer> topoSort(int vertices, int[][] edges) {
        // Build adjacency list
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < vertices; i++) {
            adj.add(new ArrayList<>());
        }
        int[] indegree = new int[vertices];

        for (int[] edge : edges) {
            adj.get(edge[1]).add(edge[0]);
            indegree[edge[0]]++;
        }

        // Queue for nodes with indegree 0
        Queue<Integer> queue = new LinkedList<>();
        for (int i = 0; i < vertices; i++) {
            if (indegree[i] == 0) queue.offer(i);
        }

        List<Integer> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            int node = queue.poll();
            result.add(node);

            for (int neighbor : adj.get(node)) {
                indegree[neighbor]--;
                if (indegree[neighbor] == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        // Check for cycle
        if (result.size() != vertices) {
            throw new RuntimeException("Graph has a cycle! Topological sort not possible.");
        }

        return result;
    }

    public static void main(String[] args) {
        int vertices = 6;
        int[][] edges = {
            {5, 2}, {5, 0}, {4, 0}, {4, 1}, {2, 3}, {3, 1}
        };

        System.out.println("Topological Sort (BFS): " + topoSort(vertices, edges));
    }
}
