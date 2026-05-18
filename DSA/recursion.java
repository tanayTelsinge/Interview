public class recursion {

    public static void printNameAsc(int n) {
        if(n == 0) {
            return;
        }
        printNameAsc(n - 1);
        System.out.println(n);

    }

    public static void printNameDesc(int n) {
        if(n == 0) {
            return;
        }
        System.out.println(n);
        printNameDesc(n - 1);

    }

    public static int sumOfNNaturalNos(int n) {
        if(n == 0) {
            return 0;
        }
        return n += sumOfNNaturalNos(n - 1);

    }

    public static int factorial(int n) {
        if(n == 1) {
            return n;
        }
        return n * factorial(n - 1);
    } 

    public static void printReverseString(String str) {
        if(str.length() == 0) {
            return;
        }
   
        printReverseString(str.substring(1));
        System.out.print(str.charAt(0));
    }

    public static String reverseString(String str) {
        if (str.isEmpty()) { // Base case: empty string
            return "";
        }
        return reverseString(str.substring(1)) + str.charAt(0); //*** */
    }

    public static boolean isSorted(int[] arr, int index) {
        // Base case: If we reach the last element, the array is sorted
        if (index == arr.length - 1) {
            return true;
        }
        // If the current element is greater than the next, it's not sorted
        if (arr[index] > arr[index + 1]) {
            return false;
        }
        // Recursive call to check the next elements
        return isSorted(arr, index + 1);
    }


    public static void main(String[] args) {
        isSorted(new int[]{1, 2, 3, 4, 5}, 0);
    }
}


