package DSA.two_pointer.opposite_pointers;

public class ContainerWithMostWater {

    //tc O(n) - we are doing one pass through the array, sc O(1) - we are using constant space to store pointers and max area.
    // core idea - area is bounded by shorter wall, so we move the shorter side towards center to try and find taller wall.
    public int maxArea(int[] height) {
        int l = 0, r = height.length - 1;
        int max = 0;

        while (l < r) {
            int width = r - l;
            int currHeight = Math.min(height[l], height[r]); // water bounded by shorter wall
            max = Math.max(max, width * currHeight);

            // move the shorter side — moving taller side can never improve area
            if (height[l] <= height[r]) {
                l++;
            } else {
                r--;
            }
        }

        return max;
    }

    // Test
    public static void main(String[] args) {
        ContainerWithMostWater sol = new ContainerWithMostWater();
        System.out.println(sol.maxArea(new int[] { 1, 8, 6, 2, 5, 4, 8, 3, 7 })); // 49
        System.out.println(sol.maxArea(new int[] { 1, 1 })); // 1
        System.out.println(sol.maxArea(new int[] { 4, 3, 2, 1, 4 })); // 16
    }
}