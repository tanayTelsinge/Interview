Sliding window:



```java
  //template
  public static int maxSumSubArray(int[] arr, int k) {
        int maxSum = Integer.MIN_VALUE, windowSum = 0;

        for (int i = 0; i < arr.length; i++) {
            windowSum += arr[i]; // Expand window

            if (i >= k - 1) { // When window size reaches k
                maxSum = Math.max(maxSum, windowSum);
                windowSum -= arr[i - (k - 1)]; // Slide window
            }
        }
        return maxSum;
    }
```
