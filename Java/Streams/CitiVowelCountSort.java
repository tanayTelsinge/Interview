import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CitiVowelCountSort {
    // list of strings - only alphabets - no case
    // vowels and consonants - sort the list in asc order of vowels present in each
    // word.
    // act vowel eating make -> act make eat vow , no of vowels -> act vow eat make
    // aeiou
    // act vowel make eating
    public static void main(String[] args) {

        List<String> list = Arrays.asList("act", "vowel", "eating", "make");

        // vowel count, sort by it, maintain order.
        Map<String, Integer> map = new LinkedHashMap<>();

        list.stream().forEach(str -> {
            int count = 0;
            for (char c : str.toCharArray()) {
                if (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u') {
                    count++;
                }
            }
            map.put(str, count);
        });
        System.out.println(map);
        var res  = map.entrySet().stream().sorted((a,b) -> a.getValue() - b.getValue()).collect(Collectors.toList());
        System.out.println(res);
    }

    // Optimized solution
    static List<String> sortByVowels(List<String> list) {
        return list.stream()
            .sorted(Comparator.comparingInt(CitiVowelCountSort::vowelCount))
            .collect(Collectors.toList());
    }

    private static int vowelCount(String s) {
        int count = 0;
        for (char c : s.toCharArray())
            if ("aeiou".indexOf(c) >= 0) count++;
        return count;
    }
}