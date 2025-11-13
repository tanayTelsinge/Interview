class Solution {
    int[] parent;
    int[] rank;

    public int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]);
        }
        return parent[x];
    }

    public void union(int x, int y) {
        int rootX = find(x);
        int rootY = find(y);

        if (rootX == rootY) return;

        if (rank[rootX] < rank[rootY]) {
            parent[rootX] = rootY;
        } else if (rank[rootX] > rank[rootY]) {
            parent[rootY] = rootX;
        } else {
             parent[rootY] = rootX;
             rank[rootX]++;
        }
    }
    public int[] findRedundantConnection(int[][] edges) {
        int V = edges.length;
        parent = new int[V + 1];
        rank = new int[V + 1];
        for (int i = 1; i <= V; i++) parent[i] = i;
        for(int[] edge : edges) {
            int parentX = find(edge[0]);
            int parentY = find(edge[1]);
            if (parentX == parentY) {
                return edge;
            }
            union(edge[0], edge[1]);
        }
        return new int[]{};
    }
}