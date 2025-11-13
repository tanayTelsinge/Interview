package DSA.prefix_sum;

public class Prefixsuffix {


    public static void main(String[] args) {
        int[] arr = {3, 1, 4, 2, 5};

        int n = arr.length;
        System.out.println("Original Array: ");
        for (int i = 0; i < n; i++) {
            System.out.print(arr[i] + " ");
        }
        System.out.println();

        // 1. Prefix Min
        int[] prefixMin = new int[n];
        prefixMin[0] = arr[0];
        for (int i = 1; i < n; i++) {
            prefixMin[i] = Math.min(prefixMin[i - 1], arr[i]);
        }

        // 2. Prefix Max
        int[] prefixMax = new int[n];
        prefixMax[0] = arr[0];
        for (int i = 1; i < n; i++) {
            prefixMax[i] = Math.max(prefixMax[i - 1], arr[i]);
        }

        // 3. Suffix Min
        int[] suffixMin = new int[n];
        suffixMin[n - 1] = arr[n - 1];
        for (int i = n - 2; i >= 0; i--) {
            suffixMin[i] = Math.min(suffixMin[i + 1], arr[i]);
        }

        // 4. Suffix Max
        int[] suffixMax = new int[n];
        suffixMax[n - 1] = arr[n - 1];
        for (int i = n - 2; i >= 0; i--) {
            suffixMax[i] = Math.max(suffixMax[i + 1], arr[i]);
        }

        // Print all arrays
        System.out.println("Prefix Min: ");
        printArray(prefixMin);

        System.out.println("Prefix Max: ");
        printArray(prefixMax);

        System.out.println("Suffix Min: ");
        printArray(suffixMin);

        System.out.println("Suffix Max: ");
        printArray(suffixMax);

        // Example: Maximum difference arr[j] - arr[i], j > i
        int maxDiff = Integer.MIN_VALUE;
        for (int i = 0; i < n; i++) {
            maxDiff = Math.max(maxDiff, suffixMax[i] - prefixMin[i]);
        }
        System.out.println("Max difference (j > i): " + maxDiff);
    }

    static void printArray(int[] arr) {
        for (int num : arr) {
            System.out.print(num + " ");
        }
        System.out.println();
    }

}


// Best Time to Buy and Sell Stock 1, 2, 3
// Maximum Difference with Constraints
// Trapping Rain Water
// Product of Array Except Self
// Subarray sum equals by k / divisible by k
// Range Sum Query - Immutable


//3 1 4 2 5

//3 4 8 10 15

//