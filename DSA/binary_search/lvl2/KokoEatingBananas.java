package DSA.binary_search.lvl2;

// LC 875 - Koko Eating Bananas
// Binary search on ANSWER SPACE (eating speed k), not on the array.
// feasible(k): can Koko eat all piles within h hours at speed k?
//   hours needed for pile p at speed k = ceil(p/k) = (p + k - 1) / k
// Minimize k such that feasible(k) is true.
// TC: O(n log(maxPile)), SC: O(1)
public class KokoEatingBananas {

    public int minEatingSpeed(int[] piles, int h) {
        int left = 1, right = getMax(piles);

        while (left < right) {
            int mid = left + (right - left) / 2;
            if (feasible(piles, mid, h)) right = mid;  // mid works → try slower
            else left = mid + 1;
        }

        return left;
    }

    private boolean feasible(int[] piles, int speed, int h) {
        int hours = 0;
        for (int pile : piles) {
            hours += (pile + speed - 1) / speed;   // ceil(pile / speed)
        }
        return hours <= h;
    }

    private int getMax(int[] piles) {
        int max = 0;
        for (int p : piles) max = Math.max(max, p);
        return max;
    }

    public static void main(String[] args) {
        KokoEatingBananas sol = new KokoEatingBananas();
        System.out.println(sol.minEatingSpeed(new int[]{3, 6, 7, 11}, 8));    // 4
        System.out.println(sol.minEatingSpeed(new int[]{30, 11, 23, 4, 20}, 5)); // 30
        System.out.println(sol.minEatingSpeed(new int[]{30, 11, 23, 4, 20}, 6)); // 23
    }
}
