package DSA.two_pointer.slow_fast;

public class RemoveElement {

    // LC 27 — Remove Element
    //slow points to the pos where we will change, fast goes and finds unique ones.
    public int removeElement(int[] nums, int val) {
        int slow = 0;

        for (int fast = 0; fast < nums.length; fast++) {
            if (nums[fast] != val) {
                nums[slow++] = nums[fast];
            }
        }
        return slow;
    }
}
