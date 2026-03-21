package DSA.sliding_window.lvl_1;

import java.util.Arrays;

public class PermutationInString {

    //trick is of frequency array (its just hashmap easy version [] used for some count)
    //sliding window is used because we need to ensure contiguous array.
    //ab - boa no bao fine.
    //populate two freq arrays 1 for s1 and 2nd for s2, sliding window shrink if size >= k.
    public static void main(String[] args) {
        String s1 = "ab";
        String s2 = "eidbaooo";
        // check if permutation of s1 is in s2
        boolean res = checkInclusion(s1, s2);
        System.out.println(res);
    }

    public static boolean checkInclusion(String s1, String s2) {
        int[] freqOne = new int[26];
        int[] freqTwo = new int[26];

        for (int i = 0; i < s1.length(); i++)
            freqOne[s1.charAt(i) - 'a']++;
            int k = s1.length();

        for (int i = 0; i < s2.length(); i++) {
            freqTwo[s2.charAt(i) - 'a']++;

            if (i >= k) {
                freqTwo[s2.charAt(i - k) - 'a']--;
            }
            boolean check = Arrays.equals(freqOne, freqTwo);
            if (check) return true;
        }
        return false;
    }

}
