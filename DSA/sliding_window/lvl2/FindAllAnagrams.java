package DSA.sliding_window.lvl2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FindAllAnagrams {

    //Trick of freq map and sliding window
    public static void main(String[] args) {
        System.out.println(findAnagrams("aa", "bb"));
    }

    public static List<Integer> findAnagrams(String s, String p) {
        int[] freqOne = new int[26];
        int[] freqTwo = new int[26];
        List<Integer> list = new ArrayList<>();

        for(int i = 0; i < p.length(); i++) freqOne[p.charAt(i) - 'a']++;

        int k = p.length();

        for(int i = 0; i < s.length(); i++) {
            freqTwo[s.charAt(i) - 'a']++;

            if (i >= k) {
                freqTwo[s.charAt(i - k) - 'a']--;
            }

            if (Arrays.equals(freqTwo, freqOne)) list.add(i - k + 1);
        }

        return list;

    }
}
