package DSA.binary_search.lvl2;

// LC 33 - Search in Rotated Sorted Array
// Key insight: one half is always sorted — determine which, then check if target is in it.
// TC: O(log n), SC: O(1)
public class SearchRotatedArray {

    public int search(int[] nums, int target) {
        int left = 0, right = nums.length - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;

            if (nums[mid] == target) return mid;

            if (nums[left] <= nums[mid]) {          // LEFT half [left..mid] is sorted
                if (nums[left] <= target && target < nums[mid]) right = mid - 1;  // target in sorted left
                else left = mid + 1;                // target in right
            } else {                                // RIGHT half [mid..right] is sorted
                if (nums[mid] < target && target <= nums[right]) left = mid + 1;  // target in sorted right
                else right = mid - 1;               // target in left
            }
        }

        return -1;
    }

    public static void main(String[] args) {
        SearchRotatedArray sol = new SearchRotatedArray();
        System.out.println(sol.search(new int[]{4, 5, 6, 7, 0, 1, 2}, 0));  // 4
        System.out.println(sol.search(new int[]{4, 5, 6, 7, 0, 1, 2}, 3));  // -1
        System.out.println(sol.search(new int[]{1}, 0));                     // -1
        System.out.println(sol.search(new int[]{3, 1}, 1));                  // 1
    }
}
