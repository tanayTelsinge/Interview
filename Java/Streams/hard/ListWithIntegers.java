package Streams.hard;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ListWithIntegers {

    public static void main(String[] args) {

        String[] s = { "abc", "123", "xys", "342" };

        var res = Arrays.stream(s)
                .filter(str -> Character.isDigit(str.charAt(0)))
                .collect(Collectors.toList());
        System.out.println(res);

        var res2 = Arrays.stream(s)
                .filter(str -> str.chars().mapToObj(c -> (char) c).allMatch(Character::isDigit))
                .collect(Collectors.toList());

        System.out.println(res);
        System.out.println(res2);
    }
}
