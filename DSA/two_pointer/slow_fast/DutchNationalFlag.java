package DSA.two_pointer.slow_fast;

public class DutchNationalFlag {
    
    public void sortColors(int[] nums) {
        int l = 0, r = nums.length - 1;
        int i = 0;
        while (i <= r) {
            if (nums[i] == 0) {
                swap(nums, l, i);
                l++;
                i++; //as l is behind i safe to ++
            } else if (nums[i] == 2) {
                swap(nums, r, i);
                r--;
                //as r is ahead of i, possible some other val so need to check so no i++
                // eg. i at 2 r at 1, 1 comes back at swap.
            } else if (nums[i] == 1) {
                i++;
            }
        }
    }

    public static void swap(int[] nums, int l, int r) {
        int temp = nums[l];
        nums[l] = nums[r];
        nums[r] = temp;
    }
}
