package DSA.binary_search.lvl_1;

public class SearchA2DMatrix {

    //first we find row via binary search (log m)
    //then run normal binary search on that row 
    // TC - log m + log n, SC - (1)

    /*
     * each line - 4 
     */
    public static void main(String[] args) {
        // Example 1: matrix = [[1,3,5,7],[10,11,16,20],[23,30,34,60]], target = 3 → true
        int[][] matrix1 = {{1, 3, 5, 7}, {10, 11, 16, 20}, {23, 30, 34, 60}};
        System.out.println(searchMatrix(matrix1, 3)); // Expected: true

        // Example 2: matrix = [[1,3,5,7],[10,11,16,20],[23,30,34,60]], target = 13 → false
        int[][] matrix2 = {{1, 3, 5, 7}, {10, 11, 16, 20}, {23, 30, 34, 60}};
        System.out.println(searchMatrix(matrix2, 13)); // Expected: false
    }

    public static boolean searchMatrix(int[][] matrix, int target) {

        boolean res = false;

        int top = 0, bottom = matrix.length - 1;
        int left = 0, right = matrix[0].length - 1;
        int row = -1;
        int mid = 0;

        while (mid >= 0 && mid < matrix.length && top <= bottom) {
            mid = top + (bottom - top) / 2;
            if (matrix[mid][left] <= target && matrix[mid][right] >= target) {
                row = mid;
                break;
            } else if (matrix[mid][right] < target) {
                top = mid + 1;
            } else {
                bottom = mid - 1;
            }

        }
        if (row == -1)
            return false;

        res = binarySearch(matrix[row], target);
        return res;
    }

    public static boolean binarySearch(int[] nums, int target) {
        int left = 0, right = nums.length - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] == target)
                return true;
            if (nums[mid] < target)
                left = mid + 1;
            else
                right = mid - 1;
        }
        return false;
    }
}
