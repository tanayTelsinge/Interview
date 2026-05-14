package DSA.graph.topological_sort;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));
        String[] firstLine = bf.readLine().trim().split(" ");
        int n = Integer.parseInt(firstLine[0]);
        long k = Long.parseLong(firstLine[1]);

        String[] tokens = bf.readLine().trim().split(" ");
        long[] a = new long[n];
        for (int i = 0; i < n; i++) {
            a[i] = Long.parseLong(tokens[i]);
        }

        long lo = 1, hi = 2_000_000_000_000_000_000L;
        while (lo < hi) {
            long mid = lo + (hi - lo + 1) / 2;
            if (canAchieve(mid, a, n, k)) lo = mid;
            else hi = mid - 1;
        }
        System.out.println(lo);
    }

    static boolean canAchieve(long m, long[] a, int n, long k) {
        long ops = 0;
        for (int i = 0; i < n; i++) {
            if (a[i] < m) {
                long deficit = m - a[i];
                long idx = i + 1;
                ops += (deficit + idx - 1) / idx; // ceil(deficit / idx)
                if (ops > k) return false;        // early exit to avoid overflow
            }
        }
        return true;
    }
}