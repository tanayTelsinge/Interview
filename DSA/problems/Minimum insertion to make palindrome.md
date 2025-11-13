give problem description only
# Minimum insertion to make palindrome
Given a string `str`, the task is to find the minimum number of characters to be inserted to convert it to a palindrome.

### Examples:
```
Input: str = "ab"
Output: 1
Insert 'a' at the end of the string to make it "aba".
take string toto

Input: str = "toto".
Output: 1
Insert 't' at the end of the string to make it "totot".

Input: str = "abcd"
Output: 3
Insert 'dcb' at the beginning of the string to make it "dcbabcd".

#core logic or approach
The idea is to find the length of the longest palindromic subsequence (LPS) in the given string. The minimum number of insertions required to make the string a palindrome will
be the difference between the length of the string and the length of the LPS.
```

### Core idea

```declarative
min insertions = n - LCS(s, reverse(s))
where:
n = length of the string

LPS(s) = length of the Longest Palindromic Subsequence of s
```

### Code:
```java
public class MinInsertionsToMakePalindrome {
    public static int minInsertions(String str) {
        int n = str.length();
        int[][] dp = new int[n][n];

        // Fill the dp array
        for (int len = 1; len <= n; len++) {
            for (int i = 0; i <= n - len; i++) {
                int j = i + len - 1;
                if (i == j) {
                    dp[i][j] = 1;
                } else if (str.charAt(i) == str.charAt(j)) {
                    dp[i][j] = 2 + dp[i + 1][j - 1];
                } else {
                    dp[i][j] = Math.max(dp[i + 1][j], dp[i][j - 1]);
                }
            }
        }

        // The minimum insertions required
        return n - dp[0][n - 1];
    }

    public static void main(String[] args) {
        String str = "abcd";
        System.out.println("Minimum insertions to make palindrome: " + minInsertions(str));
    }
}
```
