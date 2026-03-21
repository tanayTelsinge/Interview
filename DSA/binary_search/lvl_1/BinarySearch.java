package DSA.binary_search.lvl_1;

// LC 704 - Binary Search
// TC: O(log n), SC: O(1)
public class BinarySearch {

    public int search(int[] nums, int target) {
        int left = 0, right = nums.length - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;   // safe mid, avoids int overflow

            if (nums[mid] == target) return mid;
            else if (nums[mid] < target) left = mid + 1;
            else right = mid - 1;
        }

        return -1;
    }

    public static void main(String[] args) {
        BinarySearch sol = new BinarySearch();
        System.out.println(sol.search(new int[]{-1, 0, 3, 5, 9, 12}, 9));  // 4
        System.out.println(sol.search(new int[]{-1, 0, 3, 5, 9, 12}, 2));  // -1
        System.out.println(sol.search(new int[]{5}, 5));                    // 0
    }
}
