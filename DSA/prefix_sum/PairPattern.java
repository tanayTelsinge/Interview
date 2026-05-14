package DSA.prefix_sum;

import java.util.HashMap;
import java.util.Map;

/*
 * ================================================================
 * TOPIC: PREFIX SUM + PAIR COUNTING
 * ================================================================
 *
 * STEP 1: BRUTE FORCE PAIRS — O(n²)
 * ----------------------------------
 * Check every (i,j) pair with nested loop.
 * Always works but too slow for large inputs.
 *
 *   for i:
 *     for j > i:
 *       if condition(i,j): count++
 *
 * ----------------------------------------------------------------
 *
 * STEP 2: COUNT WHILE YOU BUILD — O(n)
 * -------------------------------------
 * When k-th same element arrives → it pairs with k-1 previous ones.
 * Like people shaking hands: each new person shakes with everyone inside.
 *
 * RULE: query map BEFORE updating it (else you pair with yourself)
 *
 *   for each element:
 *     count += map[element]    ← how many before me match
 *     map[element]++           ← now register myself
 *
 * USE WHEN: "count pairs where f(i) == f(j)"
 *
 * ----------------------------------------------------------------
 *
 * STEP 3: COMPLEMENT COUNTING
 * ----------------------------
 * Sometimes bad pairs are hard to count directly.
 * Count good pairs instead, then subtract.
 *
 *   bad pairs = total pairs - good pairs
 *   total pairs = n*(n-1)/2
 *
 * ----------------------------------------------------------------
 *
 * STEP 4: ALGEBRAIC TRICK (unlocks Step 2 for harder problems)
 * -------------------------------------------------------------
 * When condition involves both i,j and nums[i],nums[j]:
 * rearrange so one side has only i-terms, other has only j-terms.
 * Then apply frequency map on that term.
 *
 *   eg: j - i != nums[j] - nums[i]          (bad pair)
 *    -> nums[j] - j != nums[i] - i
 *    -> good pair: nums[i]-i == nums[j]-j
 *    -> f(i) = nums[i] - i
 *    -> count good pairs with frequency map, subtract from total
 *
 * ----------------------------------------------------------------
 *
 * STEP 5: PREFIX SUM + FREQUENCY MAP
 * ------------------------------------
 * For subarray problems, convert to prefix sum first.
 * Then use frequency map to find pairs of prefix sums.
 *
 *   subarray(i,j) = prefix[j] - prefix[i-1]
 *   if subarray(i,j) == k:
 *   -> prefix[i-1] = prefix[j] - k
 *   -> for each j, look up (prefix[j] - k) in map
 *
 * RULE: always seed map with {0:1} before loop
 *       handles subarrays starting from index 0
 *
 * ================================================================
 * PROGRESSION: 1512 → 2364 → 2574 → 560
 * ================================================================
 */

public class PairPattern {

    public static void main(String[] args) {}

    // ----------------------------------------------------------------
    // LC 1480 - Running Sum of 1D Array
    // PATTERN: basic prefix sum
    // p[i] = p[i-1] + nums[i]
    // ----------------------------------------------------------------
    public int[] runningSum(int[] nums) {
        for (int i = 1; i < nums.length; i++)
            nums[i] += nums[i - 1];
        return nums;
    }

    // ----------------------------------------------------------------
    // LC 1512 - Number of Good Pairs
    // PATTERN: count while you build
    // f(i) = nums[i]
    // ----------------------------------------------------------------
    public int numIdenticalPairs(int[] nums) {
        Map<Integer, Integer> map = new HashMap<>();
        int count = 0;
        for (int num : nums) {
            count += map.getOrDefault(num, 0);
            map.put(num, map.getOrDefault(num, 0) + 1);
        }
        return count;
    }

    // ----------------------------------------------------------------
    // LC 2364 - Count Number of Bad Pairs
    // PATTERN: algebraic trick + count while you build + complement
    // f(i) = nums[i] - i
    // bad = total - good
    // ----------------------------------------------------------------
    public long countBadPairs(int[] nums) {
        Map<Long, Long> map = new HashMap<>();
        long good = 0, n = nums.length;
        for (int i = 0; i < n; i++) {
            long key = nums[i] - i;
            good += map.getOrDefault(key, 0L);
            map.put(key, map.getOrDefault(key, 0L) + 1L);
        }
        return n * (n - 1) / 2 - good;
    }

    // ----------------------------------------------------------------
    // LC 2574 - Left and Right Sum Differences
    // PATTERN: prefix array + suffix array
    // answer[i] = |leftSum[i] - rightSum[i]|
    // ----------------------------------------------------------------
    public int[] leftRightDifference(int[] nums) {
        int n = nums.length;
        int[] pre = new int[n], suf = new int[n];
        pre[0] = nums[0];
        suf[n - 1] = nums[n - 1];
        for (int i = 1; i < n; i++) pre[i] = pre[i - 1] + nums[i];
        for (int i = n - 2; i >= 0; i--) suf[i] = suf[i + 1] + nums[i];
        nums[0] = suf[1];
        nums[n - 1] = pre[n - 2];
        for (int i = 1; i < n - 1; i++) nums[i] = Math.abs(pre[i - 1] - suf[i + 1]);
        return nums;
    }

    // ----------------------------------------------------------------
    // LC 560 - Subarray Sum Equals K
    // PATTERN: prefix sum + frequency map
    // for each j, look up (prefix[j] - k) in map
    // seed map with {0:1} to handle subarrays from index 0
    // ----------------------------------------------------------------
    public int subarraySum(int[] nums, int k) {
        int n = nums.length;
        for (int i = 1; i < n; i++)
            nums[i] += nums[i - 1];

        Map<Integer, Integer> map = new HashMap<>();
        map.put(0, 1);
        int count = 0;
        for (int i = 0; i < n; i++) {
            int diff = nums[i] - k;
            count += map.getOrDefault(diff, 0);
            map.put(nums[i], map.getOrDefault(nums[i], 0) + 1);
        }
        return count;
    }
}