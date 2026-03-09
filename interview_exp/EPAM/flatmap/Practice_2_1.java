package interview_exp.EPAM.flatmap;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Practice_2_1 {

//     ## 2. Get All Unique Words from a List of Sentences
// ```
// Input:  ["hello world", "world of java", "java is great"]
// Output: [hello, world, of, java, is, great]  // distinct
// ```

public static void main(String[] args) {
    List<String> sentences = List.of("hello world", "world of java", "java is great");
    var result = sentences.stream()
                    .flatMap(e -> Arrays.stream(e.split(" "))) //think what stream to apply, split returns [] so we need Array.stream
                    .distinct().collect(Collectors.toList());
    System.out.println(result);
}
    
}
