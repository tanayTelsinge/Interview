package disjoint_set;

public class DisjointSetOptimized {
    int[] parent; 
    int[] rank; // Used for union by rank

    DisjointSetOptimized(int size) {
        parent = new int[size];
        rank = new int[size];
        for (int i = 0; i < size; i++) {
            parent[i] = i; // Each element is its own parent initially
            rank[i] = 0;   // Initial rank is 0
        }
    }

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
            parent[parentX] = parentY;  // parent of X becomes Y if rank of Y is higher
        } else if (rank[parentX] > rank[parentY]) {
            parent[parentY] = parentX;
        } else {
            parent[parentY] = parentX;
            rank[parentX]++; // Increase rank if both have same ran
        }
    }

    public static void main(String[] args) {
        
        DisjointSetOptimized ds = new DisjointSetOptimized(10);
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
        System.out.println();
        System.out.println("Find(1): " + ds.find(1)); // Should print the root of the set containing 1
        System.out.println("Find(4): " + ds.find(4)); // Should print the root of the set containing 4

            for(int i = 0; i < ds.parent.length; i++) {
            System.out.print(ds.parent[i] + " ");
        }
    }
}

// 0
// 1
// ├─ 2
// ├─ 3
// ├─ 4
// │  ├─ 5
// │  └─ 6
// 7
// 8
// 9

// 0 1 1 1 4 4 4 7 8 9
