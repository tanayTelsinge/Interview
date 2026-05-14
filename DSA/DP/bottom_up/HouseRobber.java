package DSA.DP.bottom_up;

// LC 198 - House Robber
// Pattern: DP bottom-up with space optimization
// Intuition: at each house either skip it (take prev1) or rob it (prev2 + curr)
// TC: O(n) — single pass through array  SC: O(1) — only two variables
public class HouseRobber {

    public static void main(String[] args) {
        int[] nums =  {1, 2, 3, 1};
        int ans = rob(nums);
        System.out.println(ans);
    }

    public static int rob(int[] nums) {

        if (nums.length == 1)
            return nums[0];
        int prev2 = nums[0];
        int prev1 = Math.max(nums[0], nums[1]);

        for (int i = 2; i < nums.length; i++) {
            int curr = Math.max(prev1, prev2 + nums[i]);
            prev2 = prev1;
            prev1 = curr;
        }

        return prev1;
    }
}
