package Streams;

import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import java.util.function.Function;

public class FirstRepeatingCharacter {
    

    public static void main(String[] args) {
        String s = "asdfaghjklkjhgfdsa";

        var ans = s.chars()
                    .mapToObj(c -> (char) c)
                    .collect(
                        Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting())
                    )
                    .entrySet()
                    .stream()
                    .filter(e -> e.getValue() > 1)
                    .findFirst()
                    .get()
                    .getKey();

        System.out.println(ans);


        var res2 =  s.chars().mapToObj(c -> (char) c)
                .filter(c -> s.indexOf(c) != s.lastIndexOf(c))
                .findFirst()
                .get();

        System.out.println(res2);
        



    }
