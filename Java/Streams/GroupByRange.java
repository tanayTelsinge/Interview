package Streams;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.function.Function;

public class GroupByRange {
    

    public static void main(String[] args) {
        
        int[] arr = {2,3,10,14,20,24,30,34,40,44,50,54};

        Arrays.stream(arr)
        .boxed()
        .collect(
            Collectors.groupingBy(Function.identity())
        );
    }
}
