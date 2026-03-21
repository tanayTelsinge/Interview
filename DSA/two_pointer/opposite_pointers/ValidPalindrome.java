package DSA.two_pointer.opposite_pointers;

public class ValidPalindrome {
    public boolean isPalindrome(String s) {

        int l = 0, r = s.length() - 1;
        String temp = s.toLowerCase();
        while (l < r) {
            while (l < r && !isCharacterOrDigit(temp.charAt(l)))
                l++;
            while (l < r && !isCharacterOrDigit(temp.charAt(r)))
                r--;
            if (temp.charAt(l) != temp.charAt(r))
                return false;
            l++;
            r--;
        }
        return true;
    }

    public static boolean isCharacterOrDigit(char c) {
        return ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9'));
    }
}
