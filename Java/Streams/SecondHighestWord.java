package Streams;

import java.util.Arrays;

public class SecondHighestWord {
 
    public static void main(String[] args) {
        //find 2nd highest length word in sentence
        String s = "I am learning Streams API in Java";
        
        String res = Arrays.stream(s.split(" "))
        .sorted((a,b) -> b.length() - a.length())
        .skip(1)
        .findFirst()
        .get();
    
        System.out.println(res);

       
    }
}
