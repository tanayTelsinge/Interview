package disjoint_set;

public class DisjoinSet {
    
    public int[] parent;

    DisjoinSet(int size) {
        parent = new int[size];

        for (int i = 0; i < size; i++) {
            parent[i] = i; // Each element is its own parent initially
        }
    }

    int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]); // Path compression
        }
        return parent[x];
    }

    public void union(int x, int y) {
        int parentX = find(x);
        int parentY = find(y);

        if (parentX != parentY) {
            parent[parentY] = parentX; // Attach Y's root to X's root
        }
    }


    public static void main(String[] args) {
        DisjoinSet ds = new DisjoinSet(10);
        for(int i = 0; i < ds.parent.length; i++) {
            System.out.print(ds.parent[i] + " ");
        }
        System.out.println();
        ds.union(1, 2);
        ds.union(2, 3);
        ds.union(4, 5);
        ds.union(5, 6);
        ds.union(3, 6); // Connect the two sets

        for(int i = 0; i < ds.parent.length; i++) {
            System.out.print(ds.parent[i] + " ");
        }
        System.out.println("Find(1): " + ds.find(1)); // Should print the root of the set containing 1
        System.out.println("Find(4): " + ds.find(4)); // Should print
    }
}
