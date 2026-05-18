package Streams;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class CitiPractice {
    
    // list of strings - only alphabets - no case
    // vowels and consonants - sort the list in asc order of vowels present in each
    // word.
    // act vowel eating make -> act make eat vow , no of vowels -> act vow eat make
    // aeiou
    // act vowel make eating

    public static void main(String[] args) {
        //act vowel eating make 
        // act vowel make eating

        List<String> list = Arrays.asList("act", "vowel", "make", "eating");

        List<String> l =  list.stream()
                .sorted(Comparator.comparingInt(Main::countVowels))
                .collect(Collectors.toList());
        System.out.println(l);



    }

    public static int countVowels(String s) {
        int count = 0;
        for (char c : s) {
            if ("aeiou".contains(c)) count++;
        }
        return count;
    }
}
