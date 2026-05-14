package Streams;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.List;

public class FindDuplicatesIBM {

    public static void main(String[] args) {
        List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5, 5, 6, 6);

        var res = nums.stream()
                .collect(Collectors
                        .groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .filter(e -> e.getValue() > 1)
                .map(e -> e.getKey())
                .collect(Collectors.toSet());

        System.out.println(res);
    }
}
