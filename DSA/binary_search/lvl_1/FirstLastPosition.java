package DSA.binary_search.lvl_1;

import java.util.Arrays;

// LC 34 - Find First and Last Position of Element in Sorted Array
// Two binary searches: one for leftmost (lower bound), one for rightmost (upper bound)
// TC: O(log n), SC: O(1)
public class FirstLastPosition {

    public int[] searchRange(int[] nums, int target) {
        return new int[]{findFirst(nums, target), findLast(nums, target)};
    }

    // Lower bound: first index where nums[i] == target
    private int findFirst(int[] nums, int target) {
        int left = 0, right = nums.length;

        while (left < right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] >= target) right = mid;
            else left = mid + 1;
        }

        // left is the insertion point — check it's actually the target
        return (left < nums.length && nums[left] == target) ? left : -1;
    }

    // Upper bound: last index where nums[i] == target
    private int findLast(int[] nums, int target) {
        int left = 0, right = nums.length;

        while (left < right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] > target) right = mid;  // strictly greater → target's last is to the left
            else left = mid + 1;
        }

        // left-1 is the last position <= target — check it's exactly target
        return (left > 0 && nums[left - 1] == target) ? left - 1 : -1;
    }

    public static void main(String[] args) {
        FirstLastPosition sol = new FirstLastPosition();
        System.out.println(Arrays.toString(sol.searchRange(new int[]{5, 7, 7, 8, 8, 10}, 8)));  // [3, 4]
        System.out.println(Arrays.toString(sol.searchRange(new int[]{5, 7, 7, 8, 8, 10}, 6)));  // [-1, -1]
        System.out.println(Arrays.toString(sol.searchRange(new int[]{}, 0)));                   // [-1, -1]
        System.out.println(Arrays.toString(sol.searchRange(new int[]{1}, 1)));                  // [0, 0]
    }
}
