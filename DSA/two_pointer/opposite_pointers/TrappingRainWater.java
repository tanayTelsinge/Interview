package DSA.two_pointer.opposite_pointers;

public class TrappingRainWater {

    public static void main(String[] args) {

    }

    //we only need min of left or right, 
    //once we know min, we subtract height from it.
    public int trapOptimized(int[] height) {
        int n = height.length;
        int l = 0, r = n - 1;
        int leftMax = 0, rightMax = 0;
        int sum = 0;
        while (l < r) {
            if (height[l] <= height[r]) {
                leftMax = Math.max(leftMax, height[l]);
                sum += leftMax - height[l];
                l++;
            } else {
                rightMax = Math.max(rightMax, height[r]);
                sum += rightMax - height[r];
                r--;
            }
        }
        return sum;
    }

    // prefix and suffix array
    // core idea - min (left max wall, right Max wall) - height of current.
    // big o (n) but two pointer use and big 0 (1) possible
    public int trap(int[] height) {
        int n = height.length;
        int[] leftMax = populateLeftMax(height);
        int[] rightMax = populateRightMax(height);
        int sum = 0;
        for (int i = 0; i < n; i++) {
            int ht = Math.min(leftMax[i], rightMax[i]) - height[i];
            sum += ht;
        }
        return sum;
    }

    public static int[] populateLeftMax(int[] nums) {
        int n = nums.length;
        int[] res = new int[n];
        res[0] = nums[0];

        for (int i = 1; i < n; i++) {
            res[i] = Math.max(nums[i], res[i - 1]);
        }
        return res;
    }

    public static int[] populateRightMax(int[] nums) {
        int n = nums.length;
        int[] res = new int[n];
        res[n - 1] = nums[n - 1];

        for (int i = n - 2; i >= 0; i--) {
            res[i] = Math.max(nums[i], res[i + 1]);
        }
        return res;
    }
}
