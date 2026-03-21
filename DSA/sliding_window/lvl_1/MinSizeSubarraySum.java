package DSA.sliding_window.lvl_1;

public class MinSizeSubarraySum {
    
    // Given an array of positive integers nums and a positive integer target, 
    // return the minimal length of a subarray whose sum is greater than or equal to target.
    //  If there is no such subarray, return 0 instead.
    public static void main(String[] args) {
        
        int[] nums = {2, 3, 1, 2, 4, 3};
        int target = 7;
        System.out.println(minSubArrayLen(target, nums));


    }

    //took 17 min48s 
    public static int minSubArrayLen(int target, int[] nums) {
        int min = Integer.MAX_VALUE, left = 0, sum = 0;
        for(int right = 0; right < nums.length; right++) {
            sum += nums[right];
            while (sum >= target) {
            min = Math.min(min,right - left + 1);
            sum -= nums[left++];
            }
        }
        return min == Integer.MAX_VALUE ? 0 : min; //forgot this, if finally its MAX_VALUE then sum not going up the target return 0
    }
}
