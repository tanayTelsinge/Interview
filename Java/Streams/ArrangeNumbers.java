package Streams;

import java.util.Arrays;
import java.util.Collections;

public class ArrangeNumbers {
    

    public static void main(String[] args) {
        int[] arr = {1,2,3,4,5};

        //o/p 54321 and 12345 arrange lowest and hgihest

        //boxed is imp else reverseOrder not work
        Arrays.stream(arr)
        .boxed()
        .sorted(Collections.reverseOrder())
        .forEach(System.out::print);
                   
        //or mapToObj

        Arrays.stream(arr)
        .mapToObj(i -> i)
        .sorted(Collections.reverseOrder())
        .forEach(System.out::print);
    }
}
