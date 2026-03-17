package DSA.two_pointer.opposite_pointers;

import java.util.*;
public class ThreeSum {

    //sort array - skip duplicate check, take first, 2 sum 2 pointer on remaining sorted.
    //TC O(nlogn) for sorting + O(n^2) for 2 sum = O(n^2)
    // sc O(n) for sorting + O(1) for 2 sum = O(n) if we ignore output list space.
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> ans = new ArrayList<>();

        for (int i = 0; i < nums.length - 2; i++) {

            // skip duplicate values for i
            if (i > 0 && nums[i] == nums[i - 1]) continue;

            int l = i + 1, r = nums.length - 1;

            while (l < r) {
                int sum = nums[i] + nums[l] + nums[r];

                if (sum == 0) {
                    ans.add(Arrays.asList(nums[i], nums[l], nums[r]));

                    // skip duplicates on both sides before continuing
                    while (l < r && nums[l] == nums[l + 1]) l++;
                    while (l < r && nums[r] == nums[r - 1]) r--;

                    l++;
                    r--;
                } else if (sum > 0) {
                    r--; // too large, shrink right
                } else {
                    l++; // too small, grow left
                }
            }
        }

        return ans;
    }

    // Test
    public static void main(String[] args) {
        ThreeSum sol = new ThreeSum();
        System.out.println(sol.threeSum(new int[]{-1, 0, 1, 2, -1, -4})); // [[-1,-1,2],[-1,0,1]]
        System.out.println(sol.threeSum(new int[]{0, 0, 0}));             // [[0,0,0]]
        System.out.println(sol.threeSum(new int[]{-2, 0, 0, 2, 2}));      // [[-2,0,2]]
    }
}
