package Streams;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FindAllUniqueWords {

    public static void main(String[] args) {
        String[] arr = {
                "This is @autowired here",
                "Another  annotation",
                "@autowired @qualifier together"
        };

        var res = Arrays.stream(arr)
                  .flatMap(s -> Arrays.stream(s.split(" ")))
                  .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                  .entrySet()
                  .stream()
                  .filter(e -> e.getValue() == 1 && e.getKey().startsWith("@"))
                  .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

                  

        System.out.println(res);
    }
}
