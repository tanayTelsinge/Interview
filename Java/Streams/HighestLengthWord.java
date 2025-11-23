package Streams;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

public class HighestLengthWord {

    //Given a sentence highest length word
    public static void main(String[] args) {
        String s = "I am learning Streams API in Java";

        String[] strArray = s.split(" ");

        Optional<String> maxLengthOpt = Arrays.stream(strArray).max(Comparator.comparing(String::length));

        if (maxLengthOpt.isPresent()) {
        System.out.print(maxLengthOpt.get());
        }

        //or 
        String ans2 = Arrays.stream(s.split(" ")).max(Comparator.comparing(String::length)).get();

        System.out.println( " ans 2 : "  + ans2);
    }
}