package Streams;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StringFrequency {
    
    public static void main(String[] args) {
        
        String s = "This is the power and this is the jowar";

        var ans = Arrays
        .stream(s.split(" "))
        .collect(
            Collectors.groupingBy(
                Function.identity(),
                Collectors.counting()
            )
        );

        System.out.println(ans);
    }
}
