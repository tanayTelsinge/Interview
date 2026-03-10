import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Practice {

    //This is my life and this LiFe is good
    //Give no of time word occurs in a sentence
    public static void main(String[] args) {
        String s = "This is my life and this LiFe is good";

        var res = Arrays.stream(s.split(" "))
                    .map(e -> e.toLowerCase())
                    .collect(
                        Collectors.groupingBy(Function.identity(), Collectors.counting())
                    );

        System.out.println(res);
    }
}