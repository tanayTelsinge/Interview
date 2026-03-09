package DSA.arrays;

public class MaxSumSubArray {
    
    public static void main(String[] args) {
        int[] arr = {-2, 1, -3, 4, -1, 2, 1, -5, 4};

        //max sum = 6
        // Subarray = [4, -1, 2, 1]  (index 3 to 6)

        int maxSum = Integer.MIN_VALUE;
        int currentSum = 0;

        for (int i = 0; i < arr.length; i++) {
            currentSum += arr[i];
            maxSum = Math.max(maxSum, currentSum);
            if (currentSum < 0) currentSum = 0;
        }

        System.out.println(maxSum);

    }
}
