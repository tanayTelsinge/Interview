package interview_exp.EPAM;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Stress {
    
    public static void main(String[] args) {
         String s = "stress";

         var res = s.chars().mapToObj(c -> (char) c)
         .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()))
         .entrySet()
         .stream()
         .filter(e -> e.getValue() == 1)
         .skip(1)
         .findFirst()
         .get()
         .getKey();
         
         System.out.println(res);

    }
}
