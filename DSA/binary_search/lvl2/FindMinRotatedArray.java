package DSA.binary_search.lvl2;

// LC 153 - Find Minimum in Rotated Sorted Array
// Key insight: compare arr[mid] with arr[right].
//   arr[mid] > arr[right] → min is in right half (rotation break point is right of mid)
//   arr[mid] < arr[right] → mid could be min, or min is in left half
// TC: O(log n), SC: O(1)
public class FindMinRotatedArray {

    public int findMin(int[] nums) {
        int left = 0, right = nums.length - 1;

        while (left < right) {
            int mid = left + (right - left) / 2;

            if (nums[mid] > nums[right]) left = mid + 1;  // min is to the right of mid
            else right = mid;                             // mid is a candidate for min
        }

        return nums[left];   // left == right == index of minimum
    }

    public static void main(String[] args) {
        FindMinRotatedArray sol = new FindMinRotatedArray();
        System.out.println(sol.findMin(new int[]{3, 4, 5, 1, 2}));     // 1
        System.out.println(sol.findMin(new int[]{4, 5, 6, 7, 0, 1, 2})); // 0
        System.out.println(sol.findMin(new int[]{11, 13, 15, 17}));     // 11 (not rotated)
        System.out.println(sol.findMin(new int[]{2, 1}));               // 1
    }
}
