package Streams;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FirstNonRepeatingCharacter {
    
    public static void main(String[] args) {
        
        String s = "stress";
        //t

        var ans = s.chars()
                    .mapToObj(c -> (char) c)
                    .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()))
                    .entrySet()
                    .stream()
                    .filter((entry) -> entry.getValue() == 1)
                    .findFirst();

        if (ans.isPresent()) System.out.println(ans.get().getKey());

        String s1 = "Hello WorldH";

        var ans2 = s1.chars()
                    .mapToObj(c-> (char) c)
                    .filter(c -> c != ' ')
                    .collect(Collectors.groupingBy(c-> c, LinkedHashMap::new, Collectors.counting()))
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue() == 1)
                    .findFirst();

        System.out.println(ans2.get().getKey());

        String s3 = "stress";

        var ans3 = Arrays.stream(s3.split(""))
        .filter(c -> s3.indexOf(c) == s3.lastIndexOf(c))
        .findFirst();

        //if index of (first) and last index is same, single occurence.

        System.out.println(ans3.get());
    }
}
