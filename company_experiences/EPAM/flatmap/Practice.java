package interview_exp.EPAM.flatmap;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Practice {
    // Flatten list of lists
    // Input: [[1, 2, 3], [4, 5], [6, 7, 8, 9]]
    // Output: [1, 2, 3, 4, 5, 6, 7, 8, 9]
    public static void main(String[] args) {
        Integer[][] arr = { { 1, 2, 3 }, { 4, 5 }, { 6, 7, 8, 9 } };
        List<Integer> result = Arrays.stream(arr)
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        System.out.println(result);
    }
}