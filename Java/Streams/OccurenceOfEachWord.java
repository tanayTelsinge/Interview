package Streams;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OccurenceOfEachWord {
  
    
    public static void main(String[] args) {
        

        String s = "I am learning Streams API in Java Java";

        var res = Arrays.stream(s.split(" "))
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        System.out.println(res);
    }
}
