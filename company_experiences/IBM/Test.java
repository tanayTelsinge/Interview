import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Test {

    public static void main(String[] args) {
        String str = "Java is great and Java is fun";
        var res = Arrays.stream(str.split(" "))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .filter(e -> e.getValue() > 1)
                .map(e -> e.getKey())
                .collect(Collectors.toList());

        System.out.println(res);

        String s = "abcabcbb";

        int l = 0;
        Set<Character> set = new HashSet<>();
        int sIdx = 0, eIdx = 0;
        int max = 0;
        for (int r = 0; r < s.length(); r++) {
            char rChar = s.charAt(r);
            while (set.contains(rChar)) {
                char lChar = s.charAt(l);
                set.remove(lChar);
                l++;
            }
            set.add(rChar);
            if (set.size() > max) {
                max = set.size();
                sIdx = l;
                eIdx = r;
            }
        }
        System.out.println(s.substring(sIdx, eIdx + 1));
    }
}

// class Main {
// public static void main(String[] args) {
// String str = "Java is great and Java is fun";

// var res = Arrays.stream(str.split(" "))
// .map(s -> s.toLowerCase())
// .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
// .entrySet()
// .filter(e -> e.getValue() > 1)
// .keySet();
// .collect(Collectors.toList());
// }
// }