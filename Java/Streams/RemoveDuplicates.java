package Streams;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

public class RemoveDuplicates {
    

    public static void main(String[] args) {
        
        //Remove duplicates from the string and return in the same order
        String s = "dabcadefg";

        s.chars().mapToObj(c -> (char) c)
        .distinct().forEach(System.out::print);

        System.out.println();
        //or
        Arrays.stream(s.split("")).distinct().forEach(System.out::print);

        System.out.println();

        //if want to return
         String s1 = s.chars().mapToObj(c -> String.valueOf((char) c)).distinct().collect(Collectors.joining());
        //if want to return
         String s2 = Arrays.stream(s.split("")).distinct().collect(Collectors.joining());
         System.out.println(s2);

         //if without stream or distinct 

         StringBuilder sb = new StringBuilder();

         HashSet<Character> set = new HashSet<>();

         for(char c : s.toCharArray()) {
            if (!set.contains(c)) {
                sb.append(c);
                set.add(c);
            }
         }

         System.out.println(sb);

    }
}
