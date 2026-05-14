package DSA.arrays;

import java.util.Arrays;

public class ProductOfArrayExceptSelf {
    

    public static void main(String[] args) {
        int[] nums = {1,2,3,4};

        int[] res = productExceptSelf(nums);

        Arrays.stream(res).forEach(System.out::println);
    }

     public int[] productExceptSelf(int[] nums) {
        int n = nums.length;
        if (n == 0 || n == 1) return nums;
        int[] p = new int[n];
        p[0] = nums[0];
        for(int i = 1; i < n; i++) {
            p[i] = p[i - 1] * nums[i]; 
        }

        int[] s = new int[n];
        s[n - 1] = nums[n - 1];
        for(int i = n - 2; i >= 0; i--) {
            s[i] = s[i + 1] * nums[i]; 
        }
        int[] res = new int[n];
        res[0] = s[1];
        res[n - 1] = p[n - 2];
        for(int i = 1; i < n - 1; i++) {
            res[i] = p[i - 1] * s[i + 1];
        }

        return res;
    } 
}
