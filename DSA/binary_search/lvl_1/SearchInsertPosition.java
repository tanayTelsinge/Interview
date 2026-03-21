package DSA.binary_search.lvl_1;

// LC 35 - Search Insert Position
// Find index of target, or where it would be inserted to keep sorted order.
// Classic lower bound pattern: find first index where arr[i] >= target
// TC: O(log n), SC: O(1)
public class SearchInsertPosition {

    public int searchInsert(int[] nums, int target) {
        int left = 0, right = nums.length;   // right = n (open boundary) — target may go at end

        while (left < right) {
            int mid = left + (right - left) / 2;

            if (nums[mid] >= target) right = mid;  // mid could be answer, don't exclude
            else left = mid + 1;
        }

        return left;   // left == right == insertion point
    }

    public static void main(String[] args) {
        SearchInsertPosition sol = new SearchInsertPosition();
        System.out.println(sol.searchInsert(new int[]{1, 3, 5, 6}, 5));  // 2 (found)
        System.out.println(sol.searchInsert(new int[]{1, 3, 5, 6}, 2));  // 1 (insert between 1 and 3)
        System.out.println(sol.searchInsert(new int[]{1, 3, 5, 6}, 7));  // 4 (insert at end)
        System.out.println(sol.searchInsert(new int[]{1, 3, 5, 6}, 0));  // 0 (insert at start)
    }
}
