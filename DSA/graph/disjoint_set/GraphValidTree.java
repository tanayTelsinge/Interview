package disjoint_set;

import java.util.ArrayList;

/*
        You are given an undirected graph of N nodes (numbered from 0 to N-1) and M edges. 
        Return 1 if the graph is a tree, else return 0.
 *  Input:
    N = 4, M = 3
    G = [[0, 1], [1, 2], [1, 3]]
    Output: 
    1
    Explanation: 
    Every node is reachable and the graph has no loops, so it is a tree

 */
public class GraphValidTree {
    
    int[] parent;
    int[] rank;
    
    public int find(int x) {
        if(parent[x] != x) {
            parent[x] = find(parent[x]);
        }
        return parent[x];
    }
    
    void union(int x, int y) {
        int rootX = find(x);
        int rootY = find(y);
        
        if (rank[rootX] < rank[rootY]) {
            parent[rootX] = rootY;
        } else if (rank[rootY] < rank[rootX]) {
            parent[rootY] = rootX;
        } else {
            parent[rootY] = rootX;
            rank[rootX]++;
        }
    }
    
    public boolean detectCycle(int V, ArrayList<ArrayList<Integer>> edges) {
        for(ArrayList<Integer> edge : edges) {
            int u = edge.get(0);
            int v = edge.get(1);
            
            int parentU = find(u);
            int parentV = find(v);
            
            if (parentU == parentV) return true;
            union(u,v);
        }
        return false;
    }
    
    public boolean allConnected(int n) {
        int root = find(0);
        for (int i = 1; i < n; i++) {
            if (find(i) != root) return false;
        }
        return true;
    }
    public boolean isTree(int n, int m, ArrayList<ArrayList<Integer>> edges) {
        // code here
        parent = new int[n];
        rank = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
        
        boolean hasCycle = detectCycle(n, edges);
        boolean areAllConnected = allConnected(n);
        return !hasCycle && areAllConnected;
    }
}