package DSA.sliding_window.lvl3;

import java.util.HashMap;
import java.util.Map;

// hard - sliding window (need/have pattern)
//
// Pattern — "Satisfy & Count":
//   Instead of scanning the map each step to check validity,
//   maintain `have` = count of chars whose frequency is fully satisfied.
//   have == need → O(1) validity check, no map scan needed.
//
// Use this pattern when:
//   - window must contain all chars of another string
//   - each char must meet a frequency threshold (not just presence)
//   - you need O(1) validity on a frequency map
//
// Core flow:
//   expand right → if char needed: update haveMap, if freq exactly met → have++
//   while have==need → record window → shrink left → if freq drops below need → have--
//
// Reuse: LC 30 (words), LC 438 (fixed window), LC 727 (subsequence variant)
//
// Optimization story:
//   O(n²t) brute → O(n×alphabet) naive window → O(n) with need/have
//
// TC: O(s + t) | SC: O(alphabet) → O(1) in practice
public class MinWindowSubString {

    public static void main(String[] args) {
        System.out.println(minWindow("ADOBECODEBANC", "ABC"));
    }

    public static String minWindow(String s, String t) {
        Map<Character, Integer> needMap = new HashMap<>();
        Map<Character, Integer> haveMap = new HashMap<>();

        for (char c : t.toCharArray())
            needMap.put(c, needMap.getOrDefault(c, 0) + 1);

        int need = needMap.size(), have = 0;
        int minLength = Integer.MAX_VALUE, l = 0;
        int[] res = new int[2];

        for (int r = 0; r < s.length(); r++) {
            char curr = s.charAt(r);
            if (needMap.containsKey(curr)) {
                haveMap.put(curr, haveMap.getOrDefault(curr, 0) + 1);
                if (haveMap.get(curr).equals(needMap.get(curr)))
                    have++;
            }

            while (have == need) {
                if (r - l + 1 < minLength) {
                    minLength = r - l + 1;
                    res[0] = l;
                    res[1] = r;
                }
                char leftChar = s.charAt(l);
                if (needMap.containsKey(leftChar)) {
                    haveMap.put(leftChar, haveMap.get(leftChar) - 1);
                    if (haveMap.get(leftChar) < needMap.get(leftChar))
                        have--;
                }
                l++;
            }
        }

        return minLength == Integer.MAX_VALUE ? "" : s.substring(res[0], res[1] + 1);
    }
}
