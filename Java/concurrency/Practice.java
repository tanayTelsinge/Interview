public class Practice {

    public static void main(String[] args) {

        Thread t1 = new Thread(() -> {
            System.out.println("Test" + Thread.currentThread().getName());
        });

        t1.start();

        // Reverse a string without inbuilt functions
        String input = "hello";
        char[] arr = input.toCharArray();

        int left = 0, right = input.length() - 1;

        while (left < right) {
            char temp = arr[left];
            arr[left] = arr[right];
            arr[right] = temp;
            left++;
            right--;
        }

        String s1 = new String(arr);
        System.out.println(s1);
    }
}
