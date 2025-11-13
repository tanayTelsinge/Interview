package disjoint_set;

import java.util.*;

class CycleDetection {
    int[] parent; 
    int[] rank;

    int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]); // Path compression
        }
        return parent[x];
    }
    
    void union(int x, int y) {
        int parentX = find(x);
        int parentY = find(y);

        if (parentX == parentY) return; // Already in the same set

        // Union by rank
        if (rank[parentX] < rank[parentY]) {
            parent[parentX] = parentY;
        } else if (rank[parentX] > rank[parentY]) {
            parent[parentY] = parentX;
        } else {
            parent[parentY] = parentX;
            rank[parentX]++; 
        }
    }

    // Function to detect cycle using DSU in an undirected graph.
    public boolean detectCycle(int V, ArrayList<ArrayList<Integer>> adj) {
        // Initialize DSU here instead of constructor
        parent = new int[V];
        rank = new int[V];
        for (int i = 0; i < V; i++) {
            parent[i] = i;
            rank[i] = 0;
        }

        for (int u = 0; u < V; u++) {
            for (Integer v : adj.get(u)) {
                if (u < v) { // avoid double-processing edges
                    int parentU = find(u);
                    int parentV = find(v);
                    
                    if (parentU == parentV) {
                        return true; // cycle found
                    }
                    union(u, v);
                }
            }
        }
        return false; // no cycle
    }
}

