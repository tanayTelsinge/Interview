import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Test {

    public static void main(String[] args) {
        
       String str="programming";
       //Count occurence of even index position character using stream api
       
       var res = IntStream.range(0, str.length())
       .filter(i -> i % 2 == 0)
       .mapToObj(i -> str.charAt(i))
       .collect(Collectors.groupingBy(
        Function.identity(),
        LinkedHashMap::new,
        Collectors.counting()
       ));

       System.out.print(res);
    }

  
}