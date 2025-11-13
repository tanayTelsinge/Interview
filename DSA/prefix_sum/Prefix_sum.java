package DSA.prefix_sum;

public class Prefix_sum {
    
    public static void main(String[] args) {
        
        int[] arr = {3, 1, 4, 2, 5};
        int n = arr.length;
        //Maximum difference with constraints
        int minSoFar = arr[0];
        int maxDiff = Integer.MIN_VALUE; // or 0 if no negative differences allowed

        for (int j = 1; j < n; j++) {
            maxDiff = Math.max(maxDiff, arr[j] - minSoFar);
            minSoFar = Math.min(minSoFar, arr[j]);
        }

        //best time to buy and sell stock
        int minPrice = arr[0];
        int maxProfit = 0;
        for (int price : arr) {
            minPrice = Math.min(minPrice, price);
            maxProfit = Math.max(maxProfit, price - minPrice);
        }


        //best time to buy and sell stock II
        //how prefix sum is used here?
        int totalProfit = 0;
        for (int i = 1; i < n; i++) {
            int diff = arr[i] - arr[i - 1];
            if (diff > 0) {
                totalProfit += diff;
            }
        }

        //best time to buy and sell stock 3
        //how prefix sum is used here?
        Intuition:
        - Two transaction - max Profit
        - Imagine 2 set of peak and down.
        - for 1st tx - we do prefix min, done.
        - For 2nd tx - we dont know where first peak end, so we do suffix.
                
        

        //trapping rain water

        int[] prefixMax = new int[n];
        prefixMax[0] = arr[0];
        for (int i = 1; i < n; i++) {
            prefixMax[i] = Math.max(prefixMax[i - 1], arr[i]);
        }

        int[] suffixMax = new int[n];
        suffixMax[n - 1] = arr[n - 1];
        for (int i = n - 2; i >= 0; i--) {
            suffixMax[i] = Math.max(suffixMax[i + 1], arr[i]);
        }
        int trappedWater = 0;
        for (int i = 0; i < n; i++) {
            trappedWater += Math.min(prefixMax[i], suffixMax[i]) - arr[i];
        }

        //product except self
        int[] prefixProduct = new int[n];
        int[] suffixProduct = new int[n];
        prefixProduct[0] = 1;
        for (int i = 1; i < n; i++) {
            prefixProduct[i] = prefixProduct[i - 1] * arr[i - 1];
        }
        suffixProduct[n - 1] = 1;
        for (int i = n - 2; i >= 0; i--) {
            suffixProduct[i] = suffixProduct[i + 1] * arr[i + 1];
        }
        int[] result = new int[n];
        for (int i = 0; i < n; i++) {
            result[i] = prefixProduct[i] * suffixProduct[i];
        }

    }
}
