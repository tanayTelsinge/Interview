package DSA.sliding_window.lvl_1;

import java.util.HashSet;
import java.util.Set;

public class LongestSubstringWithoutRepeatingCharacter {

    public static int lengthOfLongestSubstring(String s) {
        int slow = 0, max = 0;
        Set<Character> set = new HashSet<>();

        for(int fast = 0; fast < s.length(); fast++) {
            char c = s.charAt(fast);
            while (set.contains(c)) {
                char removingChar = s.charAt(slow++);
                set.remove(removingChar);
            }
            set.add(c);
            max = Math.max(max, fast - slow + 1);
        }

        return max;
    }

    public static void main(String[] args) {
        String s = "abcabcbb";
        System.out.println(lengthOfLongestSubstring(s));
    }
    
}
