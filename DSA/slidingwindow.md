Sliding window:

Fixed window template

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

Dynamic window template

```java
  public static int minSubArrayLen(int k, int[] arr) {
        int left = 0, sum = 0, minLen = Integer.MAX_VALUE;

        for (int right = 0; right < arr.length; right++) {
            sum += arr[right];

            while (sum >= k) { // Shrink the window
                minLen = Math.min(minLen, right - left + 1);
                sum -= arr[left++];
            }
        }
        return (minLen == Integer.MAX_VALUE) ? 0 : minLen;
    }
```
