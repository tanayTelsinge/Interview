package interview_exp.EPAM;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Practice_3 {
    
    public static void main(String[] args) {
        //#HelloWorldJava
        //given sentence, make it Pascalcase and start with //#
        //camelCase = helloWorld  //Pascal case = HelloWorld
        String sentence = "hello world java";

        String res = getOutput(sentence);
        System.out.println(res);
    }

    public static String getOutput(String sentence) {
        var ans = Arrays.stream(sentence.split(" "))
                    .map(s -> s.substring(0,1).toUpperCase() + s.substring(1))
                    .collect(Collectors.joining(
                        "","#",""));
        System.out.println(ans);
        return " ";
    }
}

