package Streams;

import java.util.Arrays;
import java.util.stream.IntStream;

public class SumOfUniqueElements {

    public static void main(String[] args) {
        
        int[] arr = {1,6,7,8,1,1,8,8,7};

        //op 22

        int sum = Arrays.stream(arr)
        .distinct()
        .reduce(0, (a,b) -> a + b);

        System.out.println(sum);

        //or use Intstream

         int s = IntStream.of(arr)
        .sum();

        System.out.println(s);
    }

}