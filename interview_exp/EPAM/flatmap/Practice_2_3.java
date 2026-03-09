package interview_exp.EPAM.flatmap;

import javax.xml.stream.events.Characters;

import java.util.Arrays;
import java.util.List;

public class Practice_2_3 {

    // ## 4. Find All Characters from a List of Strings (distinct, sorted)
    // Input: ["abc", "bcd", "cde"]
    // Output: [a, b, c, d, e]

    public static void main(String[] args) {
        String[] list = { "abc", "bcd", "cde" };

        var a = Arrays.stream(list)
                    .flatMap(s -> s.chars().mapToObj(c -> (char) c)) //combine all and then break in chars
                    .distinct().toList();
        System.out.println(a);
    }
}
