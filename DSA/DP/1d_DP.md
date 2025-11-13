- Coin change problem

    - Must watch - [Jenny video](https://www.youtube.com/watch?v=L27_JpN6Z1Q&list=PLdo5W4Nhv31aBrJE1WS4MR9LRfbmZrAQu&index=2)

- Coin Change II -> - total ways for amount

```java

class Solution {
    public int change(int amount, int[] coins) {
        int n = coins.length;

        int[][] a = new int[n][amount + 1];
        for(int i = 0; i < n; i++) {
            a[i][0] = 1; //if we dont choose any coin, 1 way to get 0 sum. 
        }

        // handle the first row separately (only using coins[0])
        for (int j = 1; j <= amount; j++) {
            if (j % coins[0] == 0) {
                a[0][j] = 1;
            }
        }

        for(int i = 1; i < n; i++) { //coin value
            for(int j = 1; j < amount + 1; j++) { //sum value
                if (coins[i] > j) {
                    a[i][j] = a[i - 1][j]; //cannot include coin
                } else {
                    a[i][j] = a[i - 1][j] + a[i][j - coins[i]]; //a[i - 1][j] exclude, a[i][j - coins[i]] include
                }
            }
        }

        return a[n - 1][amount];
    }
}
```

- Coin Change I -> - minimum coins for amount

```java


```