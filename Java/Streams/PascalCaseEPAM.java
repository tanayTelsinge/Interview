package Streams;

import java.util.Arrays;
import java.util.stream.Collectors;

public class PascalCaseEPAM {
    
    public static void main(String[] args) {
    String sentence = "hello world java";
    //Give Pascal case output and start with # "#HelloWorldJava"

    String ans = Arrays.stream(sentence.split(" "))
    .map(s -> s.substring(0,1).toUpperCase() + s.substring(1))
    .collect(Collectors.joining("", "#", ""));

    System.out.println(ans);
 
    }
}
