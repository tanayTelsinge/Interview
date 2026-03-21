package DSA.two_pointer.slow_fast;

public class RemoveDuplicates {
    
    //LC 26
    //[1,1,2] remove duplicates return k = unique no. of elements
    public int removeDuplicates(int[] nums) {
        int slow = 0;

        for(int fast = 0; fast < nums.length; fast++) {
            if (nums[fast] != nums[slow]) {
                nums[++slow] = nums[fast];
            }
        }

        return slow + 1;
    }
}
